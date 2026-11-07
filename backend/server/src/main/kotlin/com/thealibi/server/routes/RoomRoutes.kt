package com.thealibi.server.routes

import com.thealibi.server.gemini.CaseGenerationEngine
import com.thealibi.server.gemini.ImageGenerationPipeline
import com.thealibi.server.models.*
import com.thealibi.server.storage.CaseRepository
import com.thealibi.server.storage.RoomRepository
import com.thealibi.server.validation.CaseValidator
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import java.util.UUID

fun Route.roomRoutes(
    roomRepository: RoomRepository,
    caseRepository: CaseRepository,
    engine: CaseGenerationEngine,
    imagePipeline: ImageGenerationPipeline,
    validator: CaseValidator
) {
    route("/rooms") {

        // POST /rooms: Create a new multiplayer room
        post {
            val req = try {
                call.receive<CreateRoomRequest>()
            } catch (e: Exception) {
                CreateRoomRequest()
            }

            val config = CaseGenerationRequest(
                theme = req.theme,
                difficulty = req.difficulty,
                atmosphere = req.atmosphere,
                customPrompt = req.customPrompt
            )

            val (room, hostSession) = roomRepository.createRoom(req.hostPlayerName, config)
            val snapshot = roomRepository.getPublicSnapshot(room.roomCode)!!

            val response = CreateRoomResponse(
                roomId = room.roomId,
                roomCode = room.roomCode,
                playerId = hostSession.playerId,
                sessionToken = hostSession.sessionToken,
                snapshot = snapshot
            )
            call.respond(HttpStatusCode.Created, response)
        }

        // POST /rooms/{code}/join: Join an existing room
        post("/{code}/join") {
            val code = call.parameters["code"]?.trim()
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing room code"))

            val req = try {
                call.receive<JoinRoomRequest>()
            } catch (e: Exception) {
                JoinRoomRequest(roomCode = code, playerName = "Investigator")
            }

            val result = roomRepository.joinRoom(code, req.playerName)
            result.fold(
                onSuccess = { (room, session) ->
                    val snapshot = roomRepository.getPublicSnapshot(code)!!
                    val response = JoinRoomResponse(
                        roomId = room.roomId,
                        roomCode = room.roomCode,
                        playerId = session.playerId,
                        sessionToken = session.sessionToken,
                        snapshot = snapshot
                    )
                    call.respond(HttpStatusCode.OK, response)
                },
                onFailure = { err ->
                    val status = when (err) {
                        is IllegalArgumentException -> HttpStatusCode.NotFound
                        is IllegalStateException -> HttpStatusCode.Conflict
                        else -> HttpStatusCode.BadRequest
                    }
                    call.respond(status, mapOf("error" to (err.message ?: "Failed to join room")))
                }
            )
        }

        // GET /rooms/{code}: Get sanitized public room snapshot
        get("/{code}") {
            val code = call.parameters["code"]?.trim()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing room code"))

            val snapshot = roomRepository.getPublicSnapshot(code)
            if (snapshot == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Room $code not found"))
                return@get
            }
            call.respond(snapshot)
        }

        // POST /rooms/{code}/ready: Toggle ready status
        post("/{code}/ready") {
            val code = call.parameters["code"]?.trim()
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing room code"))
            val playerId = call.request.headers["X-Player-Id"]
                ?: call.parameters["playerId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing X-Player-Id header"))

            roomRepository.setPlayerReady(code, playerId, true)
            val snapshot = roomRepository.getPublicSnapshot(code)
            call.respond(HttpStatusCode.OK, snapshot ?: mapOf("status" to "updated"))
        }

        // POST /rooms/{code}/generate: Host initiates mystery generation
        post("/{code}/generate") {
            val code = call.parameters["code"]?.trim()
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing room code"))

            val req = try {
                call.receive<GenerateMysteryRoomRequest>()
            } catch (e: Exception) {
                val pId = call.request.headers["X-Player-Id"] ?: ""
                val tok = call.request.headers["X-Session-Token"] ?: ""
                GenerateMysteryRoomRequest(playerId = pId, sessionToken = tok)
            }

            val room = roomRepository.getRoomByCode(code)
            if (room == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Room $code not found"))
                return@post
            }

            // If generation job already exists, return existing job status (idempotent)
            if (room.generationJobId != null && room.state != RoomState.WAITING_FOR_PLAYERS) {
                val existingStatus = caseRepository.getJob(room.generationJobId!!)
                call.respond(HttpStatusCode.Accepted, mapOf(
                    "jobId" to room.generationJobId,
                    "status" to (existingStatus?.status ?: room.state.name),
                    "playerCount" to (room.frozenPlayerCount ?: room.players.size),
                    "message" to "Generation already initiated for room $code"
                ))
                return@post
            }

            val updatedConfig = if (req.theme != null || req.difficulty != null || req.atmosphere != null) {
                CaseGenerationRequest(
                    theme = req.theme ?: room.generationConfig.theme,
                    difficulty = req.difficulty ?: room.generationConfig.difficulty,
                    atmosphere = req.atmosphere ?: room.generationConfig.atmosphere,
                    customPrompt = req.customPrompt ?: room.generationConfig.customPrompt
                )
            } else null

            val freezeResult = roomRepository.freezeRosterForGeneration(
                roomCode = code,
                requestingPlayerId = req.playerId,
                sessionToken = req.sessionToken,
                updatedConfig = updatedConfig
            )

            freezeResult.fold(
                onSuccess = { (frozenRoom, exactCount) ->
                    val jobId = "job_room_${UUID.randomUUID().toString().take(8)}"
                    roomRepository.setGenerationJob(code, jobId)

                    val initialStatus = GenerationJobStatus(
                        jobId = jobId,
                        status = "PENDING",
                        progressPercent = 5,
                        currentStage = "Freezing player roster ($exactCount players)..."
                    )
                    caseRepository.saveJob(initialStatus)

                    // Execute generation asynchronously
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            caseRepository.saveJob(
                                initialStatus.copy(
                                    status = "GENERATING_CASE",
                                    progressPercent = 15,
                                    currentStage = "Architecting a mystery for $exactCount investigators..."
                                )
                            )

                            val caseRequest = frozenRoom.generationConfig.copy(playerCount = exactCount)
                            val generatedCase = engine.generateMysteryCase(caseRequest) { progress, stage ->
                                caseRepository.saveJob(
                                    initialStatus.copy(
                                        status = "GENERATING_CASE",
                                        progressPercent = progress,
                                        currentStage = stage
                                    )
                                )
                            }

                            caseRepository.saveJob(
                                initialStatus.copy(
                                    status = "VALIDATING_CASE",
                                    progressPercent = 65,
                                    currentStage = "Validating mystery rules for $exactCount players..."
                                )
                            )
                            roomRepository.updateRoomState(code, RoomState.VALIDATING_CASE)

                            val validationResult = validator.validateForPlayerCount(generatedCase, exactCount)
                            if (!validationResult.isValid) {
                                throw IllegalStateException("Generated mystery failed validation: ${validationResult.errors.joinToString(", ")}")
                            }

                            caseRepository.saveJob(
                                initialStatus.copy(
                                    status = "GENERATING_ASSETS",
                                    progressPercent = 75,
                                    currentStage = "Generating crime scene and $exactCount suspect portraits..."
                                )
                            )
                            roomRepository.updateRoomState(code, RoomState.GENERATING_ASSETS)

                            val finalizedCase = imagePipeline.generateCaseAssets(generatedCase) { assetProgress ->
                                val percent = 75 + ((assetProgress.completedCount.toDouble() / assetProgress.totalCount.coerceAtLeast(1)) * 20).toInt()
                                caseRepository.saveJob(
                                    initialStatus.copy(
                                        status = "GENERATING_ASSETS",
                                        progressPercent = percent,
                                        currentStage = assetProgress.stage
                                    )
                                )
                            }

                            caseRepository.saveCase(finalizedCase)

                            // Assign characters to all frozen players
                            roomRepository.updateRoomState(code, RoomState.ASSIGNING_CHARACTERS)
                            caseRepository.saveJob(
                                initialStatus.copy(
                                    status = "ASSIGNING_CHARACTERS",
                                    progressPercent = 95,
                                    currentStage = "Assigning secret character identities to $exactCount players..."
                                )
                            )

                            roomRepository.assignCharacters(code, finalizedCase)

                            caseRepository.saveJob(
                                GenerationJobStatus(
                                    jobId = jobId,
                                    status = "COMPLETED",
                                    progressPercent = 100,
                                    currentStage = "Mystery ready! Transitioning to case briefing.",
                                    caseId = finalizedCase.identity.caseId
                                )
                            )
                            roomRepository.updateRoomState(code, RoomState.CASE_BRIEFING)
                        } catch (e: Exception) {
                            caseRepository.saveJob(
                                GenerationJobStatus(
                                    jobId = jobId,
                                    status = "FAILED",
                                    progressPercent = 0,
                                    currentStage = "Generation failed: ${e.message}",
                                    errorMessage = e.message
                                )
                            )
                            roomRepository.updateRoomState(code, RoomState.WAITING_FOR_PLAYERS, e.message)
                        }
                    }

                    call.respond(HttpStatusCode.Accepted, mapOf(
                        "jobId" to jobId,
                        "roomCode" to code,
                        "playerCount" to exactCount,
                        "status" to "GENERATING_CASE"
                    ))
                },
                onFailure = { err ->
                    val status = when (err) {
                        is IllegalAccessException -> HttpStatusCode.Forbidden
                        is IllegalStateException -> HttpStatusCode.BadRequest
                        else -> HttpStatusCode.BadRequest
                    }
                    call.respond(status, mapOf("error" to (err.message ?: "Failed to initiate generation")))
                }
            )
        }

        // GET /rooms/{code}/generation-status: Check room generation status
        get("/{code}/generation-status") {
            val code = call.parameters["code"]?.trim()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing room code"))

            val room = roomRepository.getRoomByCode(code)
            if (room == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Room $code not found"))
                return@get
            }

            val jobId = room.generationJobId
            if (jobId == null) {
                call.respond(HttpStatusCode.OK, mapOf("status" to room.state.name, "progressPercent" to 0))
                return@get
            }

            val status = caseRepository.getJob(jobId)
            if (status != null) {
                call.respond(status)
            } else {
                call.respond(HttpStatusCode.OK, mapOf("status" to room.state.name))
            }
        }

        // GET /rooms/{code}/my-character: Private assigned character for caller
        get("/{code}/my-character") {
            val code = call.parameters["code"]?.trim()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing room code"))

            val playerId = call.request.headers["X-Player-Id"]
                ?: call.parameters["playerId"]
                ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Missing X-Player-Id header"))

            val sessionToken = call.request.headers["X-Session-Token"]
                ?: call.parameters["sessionToken"]
                ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Missing X-Session-Token header"))

            val result = roomRepository.getAssignedCharacter(code, playerId, sessionToken)
            result.fold(
                onSuccess = { myChar ->
                    call.respond(HttpStatusCode.OK, myChar)
                },
                onFailure = { err ->
                    val status = when (err) {
                        is IllegalAccessException -> HttpStatusCode.Forbidden
                        is IllegalArgumentException -> HttpStatusCode.NotFound
                        else -> HttpStatusCode.BadRequest
                    }
                    call.respond(status, mapOf("error" to (err.message ?: "Failed to retrieve character")))
                }
            )
        }

        // POST /rooms/{code}/transition: Host transitions room phase
        post("/{code}/transition") {
            val code = call.parameters["code"]?.trim()
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing room code"))

            val targetStateStr = call.parameters["toState"] ?: "INVESTIGATION"
            val targetState = try {
                RoomState.valueOf(targetStateStr)
            } catch (e: Exception) {
                RoomState.INVESTIGATION
            }

            roomRepository.updateRoomState(code, targetState)
            val snapshot = roomRepository.getPublicSnapshot(code)
            call.respond(HttpStatusCode.OK, snapshot ?: mapOf("status" to "updated"))
        }

        // POST /rooms/{code}/leave: Disconnect / leave room
        post("/{code}/leave") {
            val code = call.parameters["code"]?.trim()
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing room code"))
            val playerId = call.request.headers["X-Player-Id"] ?: call.parameters["playerId"] ?: ""

            roomRepository.disconnectPlayer(code, playerId)
            call.respond(HttpStatusCode.OK, mapOf("status" to "left"))
        }

        // GET /rooms/{code}/timer: Get server-synchronized countdown timer
        get("/{code}/timer") {
            val code = call.parameters["code"]?.trim()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing room code"))
            val timer = roomRepository.getTimerState(code)
            if (timer != null) {
                call.respond(HttpStatusCode.OK, timer)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Timer not running"))
            }
        }

        // POST /rooms/{code}/messages: Send private message
        post("/{code}/messages") {
            val code = call.parameters["code"]?.trim()
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing room code"))
            val req = call.receive<SendMessageRequest>()
            val result = roomRepository.sendPrivateMessage(code, req)
            result.fold(
                onSuccess = { msg -> call.respond(HttpStatusCode.Created, msg) },
                onFailure = { err -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to (err.message ?: "Failed to send message"))) }
            )
        }

        // GET /rooms/{code}/messages: Retrieve private message history
        get("/{code}/messages") {
            val code = call.parameters["code"]?.trim()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing room code"))
            val playerId = call.request.headers["X-Player-Id"] ?: call.parameters["playerId"] ?: ""
            val sessionToken = call.request.headers["X-Session-Token"] ?: call.parameters["sessionToken"] ?: ""
            val result = roomRepository.getPrivateMessages(code, playerId, sessionToken)
            result.fold(
                onSuccess = { msgs -> call.respond(HttpStatusCode.OK, msgs) },
                onFailure = { err -> call.respond(HttpStatusCode.Forbidden, mapOf("error" to (err.message ?: "Unauthorized"))) }
            )
        }

        // POST /rooms/{code}/deals: Propose a deal
        post("/{code}/deals") {
            val code = call.parameters["code"]?.trim()
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing room code"))
            val req = call.receive<ProposeDealRequest>()
            val result = roomRepository.proposeDeal(code, req)
            result.fold(
                onSuccess = { deal -> call.respond(HttpStatusCode.Created, deal) },
                onFailure = { err -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to (err.message ?: "Deal error"))) }
            )
        }

        // POST /rooms/{code}/deals/respond: Accept or reject deal
        post("/{code}/deals/respond") {
            val code = call.parameters["code"]?.trim()
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing room code"))
            val req = call.receive<RespondDealRequest>()
            val result = roomRepository.respondToDeal(code, req)
            result.fold(
                onSuccess = { deal -> call.respond(HttpStatusCode.OK, deal) },
                onFailure = { err -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to (err.message ?: "Deal response error"))) }
            )
        }

        // POST /rooms/{code}/evidence/{evidenceId}/discover: Mark evidence discovered
        post("/{code}/evidence/{evidenceId}/discover") {
            val code = call.parameters["code"]?.trim()
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing room code"))
            val evidenceId = call.parameters["evidenceId"] ?: ""
            val playerId = call.request.headers["X-Player-Id"] ?: ""
            val makePublic = call.parameters["public"]?.toBoolean() ?: true

            val publicList = roomRepository.recordEvidenceDiscovery(code, playerId, evidenceId, makePublic)
            call.respond(HttpStatusCode.OK, mapOf("discovered" to evidenceId, "publicEvidence" to publicList.toList()))
        }

        // POST /rooms/{code}/twist/trigger: Trigger mid-game twist
        post("/{code}/twist/trigger") {
            val code = call.parameters["code"]?.trim()
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing room code"))
            val result = roomRepository.triggerMidGameTwist(code)
            result.fold(
                onSuccess = { twist -> call.respond(HttpStatusCode.OK, twist) },
                onFailure = { err -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to (err.message ?: "Failed to trigger twist"))) }
            )
        }

        // POST /rooms/{code}/vote: Submit secret ballot
        post("/{code}/vote") {
            val code = call.parameters["code"]?.trim()
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing room code"))
            val req = call.receive<SubmitVoteRequest>()
            val result = roomRepository.submitBallotVote(code, req)
            result.fold(
                onSuccess = { call.respond(HttpStatusCode.OK, mapOf("status" to "vote_recorded")) },
                onFailure = { err -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to (err.message ?: "Failed to record vote"))) }
            )
        }

        // GET /rooms/{code}/resolution: Final case resolution summary & scores
        get("/{code}/resolution") {
            val code = call.parameters["code"]?.trim()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing room code"))
            val resolution = roomRepository.getResolutionSummary(code)
            if (resolution != null) {
                call.respond(HttpStatusCode.OK, resolution)
            } else {
                val tallyResult = roomRepository.tallyAndResolveCase(code)
                tallyResult.fold(
                    onSuccess = { summary -> call.respond(HttpStatusCode.OK, summary) },
                    onFailure = { err -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to (err.message ?: "Resolution error"))) }
                )
            }
        }

        // WEBSOCKET /rooms/{code}/ws: Real-time room events
        webSocket("/{code}/ws") {
            val code = call.parameters["code"]?.trim() ?: run {
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Missing room code"))
                return@webSocket
            }
            val playerId = call.request.queryParameters["playerId"] ?: run {
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Missing playerId"))
                return@webSocket
            }

            roomRepository.registerWebSocket(code, playerId, this)
            try {
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        // Keep-alive or client heartbeat
                    }
                }
            } catch (_: Exception) {
            } finally {
                roomRepository.unregisterWebSocket(code, playerId)
            }
        }
    }
}
