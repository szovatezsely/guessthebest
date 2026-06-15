package io.adroit.guessthebest

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.sql.Connection
import java.sql.DriverManager

/** Shape of a question in the seed file (resources/questions.json). */
@Serializable
data class SeedQuestion(
    val category: String,
    val difficulty: String,
    val text: String,
    val answers: List<String>,
    val correctIndex: Int,
)

/**
 * Owns the SQLite connection and schema. The database file (guessthebest.db) is created in the
 * working directory on first run and seeded from resources/questions.json if empty.
 */
object Database {
    private const val URL = "jdbc:sqlite:guessthebest.db"

    fun connection(): Connection = DriverManager.getConnection(URL)

    fun init() {
        connection().use { conn ->
            conn.createStatement().use { st ->
                st.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS questions (
                        id            INTEGER PRIMARY KEY AUTOINCREMENT,
                        category      TEXT    NOT NULL,
                        difficulty    TEXT    NOT NULL,
                        text          TEXT    NOT NULL,
                        answer_a      TEXT    NOT NULL,
                        answer_b      TEXT    NOT NULL,
                        answer_c      TEXT    NOT NULL,
                        answer_d      TEXT    NOT NULL,
                        correct_index INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }

            val count = conn.createStatement().use { st ->
                st.executeQuery("SELECT COUNT(*) FROM questions").use { rs ->
                    rs.next()
                    rs.getInt(1)
                }
            }

            if (count == 0) seed(conn)
        }
    }

    private fun seed(conn: Connection) {
        val stream = this::class.java.classLoader.getResourceAsStream("questions.json")
            ?: error("questions.json not found on the classpath")
        val raw = stream.bufferedReader().use { it.readText() }
        val questions = Json { ignoreUnknownKeys = true }.decodeFromString<List<SeedQuestion>>(raw)

        val sql = """
            INSERT INTO questions
                (category, difficulty, text, answer_a, answer_b, answer_c, answer_d, correct_index)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        conn.prepareStatement(sql).use { ps ->
            for (q in questions) {
                require(q.answers.size == 4) { "Question must have exactly 4 answers: ${q.text}" }
                require(q.correctIndex in 0..3) { "correctIndex must be 0-3: ${q.text}" }
                ps.setString(1, q.category)
                ps.setString(2, q.difficulty)
                ps.setString(3, q.text)
                ps.setString(4, q.answers[0])
                ps.setString(5, q.answers[1])
                ps.setString(6, q.answers[2])
                ps.setString(7, q.answers[3])
                ps.setInt(8, q.correctIndex)
                ps.addBatch()
            }
            ps.executeBatch()
        }
        println("Seeded ${questions.size} questions into SQLite.")
    }
}
