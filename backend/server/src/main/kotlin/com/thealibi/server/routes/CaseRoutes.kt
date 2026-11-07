package com.thealibi.server.routes

import com.thealibi.server.gemini.CaseGenerationEngine
import com.thealibi.server.gemini.ImageGenerationPipeline
import com.thealibi.server.models.CaseGenerationRequest
import com.thealibi.server.models.GenerationJobStatus
import com.thealibi.server.storage.CaseRepository
import com.thealibi.server.validation.CaseValidator
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

fun Route.caseRoutes(
    repository: CaseRepository,
    engine: CaseGenerationEngine,
    imagePipeline: ImageGenerationPipeline,
    validator: CaseValidator
) {
    route("/cases") {

        // POST /cases/generate
        post("/generate") {
            val request = try {
                call.receive<CaseGenerationRequest>()
            } catch (e: Exception) {
                CaseGenerationRequest()
            }

            val jobId = "job_${UUID.randomUUID().toString().take(8)}"
            val initialStatus = GenerationJobStatus(
                jobId = jobId,
                status = "PENDING",
                progressPercent = 5,
                currentStage = "Job queued for mystery generation"
            )
            repository.saveJob(initialStatus)

            // Launch generation job asynchronously
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    repository.saveJob(
                        initialStatus.copy(
                            status = "GENERATING_CASE",
                            progressPercent = 15,
                            currentStage = "Architecting case setting, characters, and solution"
                        )
                    )

                    val casePackage = engine.generateMysteryCase(request) { progress, stage ->
                        repository.saveJob(
                            initialStatus.copy(
                                status = "GENERATING_CASE",
                                progressPercent = progress,
                                currentStage = stage
                            )
                        )
                    }

                    repository.saveJob(
                        initialStatus.copy(
                            status = "GENERATING_ASSETS",
                            progressPercent = 75,
                            currentStage = "Creating visual assets and case portraits"
                        )
                    )

                    val finalizedPackage = imagePipeline.generateCaseAssets(casePackage) { assetProgress ->
                        val percent = 75 + ((assetProgress.completedCount.toDouble() / assetProgress.totalCount.coerceAtLeast(1)) * 20).toInt()
                        repository.saveJob(
                            initialStatus.copy(
                                status = "GENERATING_ASSETS",
                                progressPercent = percent,
                                currentStage = assetProgress.stage
                            )
                        )
                    }

                    repository.saveCase(finalizedPackage)

                    repository.saveJob(
                        GenerationJobStatus(
                            jobId = jobId,
                            status = "COMPLETED",
                            progressPercent = 100,
                            currentStage = "Dossier complete and ready for investigation",
                            caseId = finalizedPackage.identity.caseId
                        )
                    )
                } catch (e: Exception) {
                    repository.saveJob(
                        GenerationJobStatus(
                            jobId = jobId,
                            status = "FAILED",
                            progressPercent = 0,
                            currentStage = "Failed: ${e.message}",
                            errorMessage = e.message
                        )
                    )
                }
            }

            call.respond(HttpStatusCode.Accepted, mapOf("jobId" to jobId))
        }

        // GET /cases/generate/{jobId}
        get("/generate/{jobId}") {
            val jobId = call.parameters["jobId"]
            if (jobId == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing jobId parameter")
                return@get
            }
            val status = repository.getJob(jobId)
            if (status == null) {
                call.respond(HttpStatusCode.NotFound, "Job $jobId not found")
                return@get
            }
            call.respond(status)
        }

        // GET /cases
        get {
            val cases = repository.getAllPublicCases()
            call.respond(cases)
        }

        // GET /cases/{caseId}
        get("/{caseId}") {
            val caseId = call.parameters["caseId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val publicCase = repository.getPublicCase(caseId)
            if (publicCase == null) {
                call.respond(HttpStatusCode.NotFound, "Case $caseId not found")
                return@get
            }
            call.respond(publicCase)
        }

        // GET /cases/{caseId}/preview
        get("/{caseId}/preview") {
            val caseId = call.parameters["caseId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val publicCase = repository.getPublicCase(caseId) ?: return@get call.respond(HttpStatusCode.NotFound)
            val preview = repository.getAllPublicCases().find { it.caseId == caseId }
            if (preview != null) {
                call.respond(preview)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        // GET /cases/{caseId}/assets/{assetId}
        get("/{caseId}/assets/{assetId}") {
            val caseId = call.parameters["caseId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val assetId = call.parameters["assetId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val file = imagePipeline.getAssetFile(caseId, assetId)
            if (file != null && file.exists()) {
                call.respondFile(file)
            } else {
                call.respond(HttpStatusCode.NotFound, "Asset not found")
            }
        }

        // POST /cases/{caseId}/validate
        post("/{caseId}/validate") {
            val caseId = call.parameters["caseId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val fullCase = repository.getCase(caseId) ?: return@post call.respond(HttpStatusCode.NotFound)
            val result = validator.validate(fullCase)
            call.respond(result)
        }

        // DELETE /cases/{caseId}
        delete("/{caseId}") {
            val caseId = call.parameters["caseId"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            val deleted = repository.deleteCase(caseId)
            if (deleted) {
                call.respond(HttpStatusCode.OK, mapOf("deleted" to true))
            } else {
                call.respond(HttpStatusCode.NotFound, "Case $caseId not found")
            }
        }

        // POST /cases/{caseId}/resolve - only available at game end to verify accusation
        post("/{caseId}/resolve") {
            val caseId = call.parameters["caseId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val solution = repository.getSecretSolution(caseId)
            if (solution == null) {
                call.respond(HttpStatusCode.NotFound, "Solution for $caseId not available")
                return@post
            }
            call.respond(solution)
        }
    }
}
