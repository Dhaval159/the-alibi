package com.thealibi.server.gemini

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import java.util.Base64

class GeminiClient(
    private val apiKey: String = System.getenv("GEMINI_API_KEY") ?: "",
    private val textModel: String = System.getenv("GEMINI_TEXT_MODEL") ?: "gemini-2.5-flash",
    private val imageModel: String = System.getenv("GEMINI_IMAGE_MODEL") ?: "imagen-3.0-generate-002"
) {
    private val logger = LoggerFactory.getLogger(GeminiClient::class.java)

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }
    }

    val isConfigured: Boolean
        get() = apiKey.isNotBlank()

    suspend fun generateStructuredText(
        systemInstruction: String,
        prompt: String,
        responseSchema: JsonObject? = null
    ): String {
        if (!isConfigured) {
            throw IllegalStateException("GEMINI_API_KEY environment variable is not configured on the server")
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$textModel:generateContent?key=$apiKey"

        val contents = listOf(
            buildJsonObject {
                put("role", "user")
                put("parts", buildJsonArray {
                    add(buildJsonObject { put("text", prompt) })
                })
            }
        )

        val generationConfig = buildJsonObject {
            put("temperature", 0.7)
            put("responseMimeType", "application/json")
            if (responseSchema != null) {
                put("responseSchema", responseSchema)
            }
        }

        val systemInstructionObj = buildJsonObject {
            put("parts", buildJsonArray {
                add(buildJsonObject { put("text", systemInstruction) })
            })
        }

        val requestBody = buildJsonObject {
            put("contents", JsonArray(contents))
            put("generationConfig", generationConfig)
            put("systemInstruction", systemInstructionObj)
        }

        logger.info("Calling Gemini API model=$textModel")

        val response: HttpResponse = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        if (!response.status.isSuccess()) {
            val errBody = response.bodyAsText()
            logger.error("Gemini API error status=${response.status}: $errBody")
            throw RuntimeException("Gemini API returned ${response.status}: $errBody")
        }

        val responseJson: JsonObject = response.body()
        val candidates = responseJson["candidates"]?.jsonArray
        if (candidates.isNullOrEmpty()) {
            throw RuntimeException("Gemini API returned no candidates")
        }

        val parts = candidates[0].jsonObject["content"]?.jsonObject?.get("parts")?.jsonArray
        val text = parts?.getOrNull(0)?.jsonObject?.get("text")?.jsonPrimitive?.content
            ?: throw RuntimeException("Empty response text from Gemini API")

        return text
    }

    suspend fun generateImage(prompt: String): ByteArray? {
        if (!isConfigured) {
            logger.warn("Skipping real image generation - GEMINI_API_KEY is not set")
            return null
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$imageModel:predict?key=$apiKey"
        val requestBody = buildJsonObject {
            put("instances", buildJsonArray {
                add(buildJsonObject {
                    put("prompt", prompt)
                })
            })
            put("parameters", buildJsonObject {
                put("sampleCount", 1)
                put("aspectRatio", "1:1")
            })
        }

        return try {
            val response: HttpResponse = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            if (response.status.isSuccess()) {
                val responseJson: JsonObject = response.body()
                val predictions = responseJson["predictions"]?.jsonArray
                val b64 = predictions?.getOrNull(0)?.jsonObject?.get("bytesBase64Encoded")?.jsonPrimitive?.content
                if (b64 != null) {
                    Base64.getDecoder().decode(b64)
                } else null
            } else {
                logger.warn("Image generation returned ${response.status}: ${response.bodyAsText()}")
                null
            }
        } catch (e: Exception) {
            logger.warn("Image generation failed: ${e.message}")
            null
        }
    }
}
