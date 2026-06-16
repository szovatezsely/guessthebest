package io.adroit.guessthebest

/** Read access to the questions table. */
class QuestionRepository {

    /** Returns up to [count] random questions, without revealing the correct answer. */
    fun randomQuestions(count: Int): List<QuestionDto> {
        val limit = count.coerceIn(1, 50)
        Database.connection().use { conn ->
            conn.prepareStatement(
                """
                SELECT id, category, difficulty, text, answer_a, answer_b, answer_c, answer_d
                FROM questions
                ORDER BY RANDOM()
                LIMIT ?
                """.trimIndent(),
            ).use { ps ->
                ps.setInt(1, limit)
                ps.executeQuery().use { rs ->
                    val result = mutableListOf<QuestionDto>()
                    while (rs.next()) {
                        result += QuestionDto(
                            id = rs.getInt("id"),
                            category = rs.getString("category"),
                            difficulty = rs.getString("difficulty"),
                            text = rs.getString("text"),
                            answers = listOf(
                                rs.getString("answer_a"),
                                rs.getString("answer_b"),
                                rs.getString("answer_c"),
                                rs.getString("answer_d"),
                            ),
                        )
                    }
                    return result
                }
            }
        }
    }

    private val difficultyOrder = listOf("könnyű", "közepes", "nehéz")

    /** Topic+difficulty combos (and the raw category/difficulty lists) the client builds a board from. */
    fun meta(): Meta {
        Database.connection().use { conn ->
            val combos = mutableListOf<Cell>()
            val categories = sortedSetOf<String>()
            val difficulties = sortedSetOf<String>()
            conn.createStatement().use { st ->
                st.executeQuery("SELECT DISTINCT category, difficulty FROM questions").use { rs ->
                    while (rs.next()) {
                        val category = rs.getString("category")
                        val difficulty = rs.getString("difficulty")
                        combos += Cell(category, difficulty)
                        categories += category
                        difficulties += difficulty
                    }
                }
            }
            combos.sortWith(compareBy({ it.category }, { difficultyOrder.indexOf(it.difficulty) }))
            return Meta(
                combos = combos,
                categories = categories.toList(),
                difficulties = difficulties.sortedBy { difficultyOrder.indexOf(it) },
            )
        }
    }

    /**
     * Draws one random question (without the correct answer) for a grid cell, skipping ids in
     * [exclude]. Falls back from an exact category+difficulty match to difficulty-only, then to any
     * question, so a cell can always be filled even if a specific pool is exhausted. The returned
     * DTO carries the question's real category/difficulty so the client can keep the card truthful.
     */
    fun drawQuestion(category: String?, difficulty: String?, exclude: Set<Int>): QuestionDto? {
        // Progressively broader filters; first hit wins.
        val filters = buildList {
            if (category != null && difficulty != null) add("category = ? AND difficulty = ?" to listOf(category, difficulty))
            if (difficulty != null) add("difficulty = ?" to listOf(difficulty))
            add("1 = 1" to emptyList())
        }
        Database.connection().use { conn ->
            for ((clause, params) in filters) {
                queryOne(conn, clause, params, exclude)?.let { return it }
            }
            // Last resort: ignore the exclude set rather than fail.
            for ((clause, params) in filters) {
                queryOne(conn, clause, params, emptySet())?.let { return it }
            }
        }
        return null
    }

    private fun queryOne(
        conn: java.sql.Connection,
        clause: String,
        params: List<String>,
        exclude: Set<Int>,
    ): QuestionDto? {
        val excludeClause = if (exclude.isEmpty()) "" else
            " AND id NOT IN (${exclude.joinToString(",") { "?" }})"
        val sql = """
            SELECT id, category, difficulty, text, answer_a, answer_b, answer_c, answer_d
            FROM questions
            WHERE $clause$excludeClause
            ORDER BY RANDOM()
            LIMIT 1
        """.trimIndent()
        conn.prepareStatement(sql).use { ps ->
            var i = 1
            for (p in params) ps.setString(i++, p)
            for (id in exclude) ps.setInt(i++, id)
            ps.executeQuery().use { rs ->
                if (!rs.next()) return null
                return QuestionDto(
                    id = rs.getInt("id"),
                    category = rs.getString("category"),
                    difficulty = rs.getString("difficulty"),
                    text = rs.getString("text"),
                    answers = listOf(
                        rs.getString("answer_a"),
                        rs.getString("answer_b"),
                        rs.getString("answer_c"),
                        rs.getString("answer_d"),
                    ),
                )
            }
        }
    }

    /** Distinct categories present in the database. */
    fun categories(): List<String> {
        Database.connection().use { conn ->
            conn.createStatement().use { st ->
                st.executeQuery("SELECT DISTINCT category FROM questions ORDER BY category").use { rs ->
                    val result = mutableListOf<String>()
                    while (rs.next()) result += rs.getString(1)
                    return result
                }
            }
        }
    }

    /** Verifies a submitted answer server-side. Returns null if the question id is unknown. */
    fun checkAnswer(id: Int, selectedIndex: Int): AnswerResult? {
        Database.connection().use { conn ->
            conn.prepareStatement("SELECT correct_index FROM questions WHERE id = ?").use { ps ->
                ps.setInt(1, id)
                ps.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    val correctIndex = rs.getInt("correct_index")
                    return AnswerResult(correct = selectedIndex == correctIndex, correctIndex = correctIndex)
                }
            }
        }
    }
}
