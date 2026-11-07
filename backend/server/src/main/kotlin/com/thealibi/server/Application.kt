package com.thealibi.server

import com.thealibi.server.gemini.CaseGenerationEngine
import com.thealibi.server.gemini.GeminiClient
import com.thealibi.server.gemini.ImageGenerationPipeline
import com.thealibi.server.models.CaseGenerationRequest
import com.thealibi.server.routes.caseRoutes
import com.thealibi.server.routes.roomRoutes
import com.thealibi.server.storage.CaseRepository
import com.thealibi.server.storage.RoomRepository
import com.thealibi.server.validation.CaseValidator
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import java.time.Duration
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val logger = LoggerFactory.getLogger("TheAlibiServer")

    install(CallLogging) {
        level = Level.INFO
    }

    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        anyHost()
    }

    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = true
            encodeDefaults = true
        })
    }

    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error("Unhandled exception: ${cause.message}", cause)
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (cause.message ?: "Internal Server Error")))
        }
    }

    val geminiClient = GeminiClient()
    val validator = CaseValidator()
    val engine = CaseGenerationEngine(geminiClient, validator)
    val imagePipeline = ImageGenerationPipeline(geminiClient)
    val repository = CaseRepository(validator)
    val roomRepository = RoomRepository(repository)

    // Pre-seed authentic CASE 001 ("The Cipher at Blackwood Hall") so it is immediately playable
    val case001 = engine.generateProceduralMasterCase(
        CaseGenerationRequest(
            theme = "Mansion inheritance mystery",
            difficulty = "Medium",
            playerCount = 6,
            atmosphere = "Classic detective mystery"
        )
    )
    repository.saveCase(case001)
    logger.info("The Alibi Server initialized. Pre-seeded CASE 001: ${case001.identity.title}")

    routing {
        get("/") {
            call.respondText("THE ALIBI - Gemini Mystery Backend Server is Online", ContentType.Text.Plain)
        }
        caseRoutes(repository, engine, imagePipeline, validator)
        roomRoutes(roomRepository, repository, engine, imagePipeline, validator)
    }
}
