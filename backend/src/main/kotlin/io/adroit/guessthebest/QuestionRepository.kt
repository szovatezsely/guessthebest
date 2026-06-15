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
