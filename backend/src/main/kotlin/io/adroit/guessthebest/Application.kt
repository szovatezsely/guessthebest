package io.adroit.guessthebest

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun main() {
    Database.init()
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json { prettyPrint = true; ignoreUnknownKeys = true })
    }
    install(CORS) {
        anyHost() // dev only; tighten before production
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
    }
    install(CallLogging)
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (cause.message ?: "internal error")))
        }
    }

    val repo = QuestionRepository()

    routing {
        get("/api/health") {
            call.respond(mapOf("status" to "ok"))
        }

        // GET /api/questions?count=10 -> random questions (no correct answer revealed)
        get("/api/questions") {
            val count = call.request.queryParameters["count"]?.toIntOrNull() ?: 10
            call.respond(repo.randomQuestions(count))
        }

        get("/api/questions/categories") {
            call.respond(repo.categories())
        }

        // GET /api/meta -> topic+difficulty combos the client builds the 5x5 board from
        get("/api/meta") {
            call.respond(repo.meta())
        }

        // GET /api/question?category=..&difficulty=..&exclude=1,2,3 -> one question (no answer)
        get("/api/question") {
            val category = call.request.queryParameters["category"]
            val difficulty = call.request.queryParameters["difficulty"]
            val exclude = call.request.queryParameters["exclude"]
                ?.split(",")
                ?.mapNotNull { it.trim().toIntOrNull() }
                ?.toSet()
                ?: emptySet()
            val question = repo.drawQuestion(category, difficulty, exclude)
            if (question == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "no question available"))
                return@get
            }
            call.respond(question)
        }

        // POST /api/questions/{id}/answer  body: { "selectedIndex": 2 }
        post("/api/questions/{id}/answer") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid question id"))
                return@post
            }
            val req = call.receive<AnswerRequest>()
            val result = repo.checkAnswer(id, req.selectedIndex)
            if (result == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "question not found"))
                return@post
            }
            call.respond(result)
        }
    }
}
