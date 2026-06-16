package io.adroit.guessthebest

import kotlinx.serialization.Serializable

/** A quiz question as sent to the client. Note: the correct answer is intentionally NOT included. */
@Serializable
data class QuestionDto(
    val id: Int,
    val category: String,
    val difficulty: String,
    val text: String,
    val answers: List<String>,
)

/** Client submits the index (0-3) of the answer it selected. */
@Serializable
data class AnswerRequest(val selectedIndex: Int)

/** Server's verdict on a submitted answer. */
@Serializable
data class AnswerResult(val correct: Boolean, val correctIndex: Int)

/** A topic + difficulty pairing that has at least one question — used to label grid cells. */
@Serializable
data class Cell(val category: String, val difficulty: String)

/** Building blocks the client uses to assemble the game board. */
@Serializable
data class Meta(
    val combos: List<Cell>,
    val categories: List<String>,
    val difficulties: List<String>,
)
