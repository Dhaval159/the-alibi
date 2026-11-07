package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PlayerInfo
import com.example.model.RoomState
import com.example.ui.Screen
import com.example.ui.TheAlibiUiState
import com.example.ui.TheAlibiViewModel
import com.example.ui.theme.*

@Composable
fun LobbyScreen(
    uiState: TheAlibiUiState,
    viewModel: TheAlibiViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val room = uiState.currentRoom
    val isHost = uiState.isHost
    val playerCount = room?.players?.size ?: 0
    val canGenerate = isHost && playerCount in 4..10 && room?.state == RoomState.WAITING_FOR_PLAYERS

    // Confirmation Modal for Host
    if (uiState.showGenerateConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setGenerateConfirmationDialog(false) },
            containerColor = NoirCard,
            titleContentColor = GoldAccent,
            textContentColor = PaperText,
            shape = RoundedCornerShape(12.dp),
            title = {
                Text(
                    text = "LOCK ROSTER & GENERATE",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    fontFamily = FontFamily.Serif
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Generate a mystery for $playerCount players? Once generation begins, the room roster will be locked.",
                        fontSize = 14.sp,
                        color = PaperText,
                        lineHeight = 20.sp
                    )
                    Text(
                        text = "• Exactly $playerCount human character dossiers will be created.\n• 1 secret culprit and ${playerCount - 1} innocent suspects.\n• Each investigator will receive private directives.",
                        fontSize = 12.sp,
                        color = MutedText,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.startRoomMysteryGeneration() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAccent,
                        contentColor = NoirBackground
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.testTag("confirm_generation_button")
                ) {
                    Text("COMMENCE GENERATION", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.setGenerateConfirmationDialog(false) },
                    border = BorderStroke(1.dp, NoirBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MutedText),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.testTag("cancel_generation_button")
                ) {
                    Text("CANCEL", fontSize = 12.sp)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NoirBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { viewModel.leaveRoom() },
                modifier = Modifier.testTag("leave_room_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Leave Room",
                    tint = GoldAccent
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "THE ALIBI LOBBY",
                    color = GoldAccent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    text = if (isHost) "You are the Room Host" else "Joined as Investigator",
                    color = MutedText,
                    fontSize = 11.sp
                )
            }

            // Connection Status Dot
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(NoirCard)
                    .border(1.dp, NoirBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(ClueFound)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "LIVE",
                    color = PaperText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        HorizontalDivider(color = NoirBorder, thickness = 1.dp)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Room Code Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NoirCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ROOM ACCESS CODE",
                            color = MutedText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = room?.roomCode ?: "------",
                            color = GoldLight,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 6.sp,
                            modifier = Modifier.testTag("room_code_display")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val clip = ClipData.newPlainText("The Alibi Room Code", room?.roomCode ?: "")
                                clipboard?.setPrimaryClip(clip)
                            },
                            border = BorderStroke(1.dp, GoldMuted),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("copy_room_code_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Code",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "COPY ROOM CODE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }

            // Players Roster Header & Count
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PLAYERS: $playerCount / 10",
                            color = GoldAccent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.testTag("lobby_player_count")
                        )
                        Text(
                            text = if (playerCount < 4) {
                                "Need ${4 - playerCount} more player(s) to commence"
                            } else {
                                "Roster quorum reached • Ready for case generation"
                            },
                            color = if (playerCount < 4) WarningRedBright else ClueFound,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Quorum badge
                    Surface(
                        color = if (playerCount in 4..10) ClueFound.copy(alpha = 0.2f) else WarningRed.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, if (playerCount in 4..10) ClueFound else WarningRed),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "MIN 4 • MAX 10",
                            color = if (playerCount in 4..10) ClueFound else WarningRedBright,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Player List Items
            val playersList = room?.players ?: emptyList()
            items(playersList) { player ->
                PlayerRosterCard(
                    player = player,
                    isLocalPlayer = player.playerId == uiState.myPlayerId,
                    canRemove = isHost && !player.isHost,
                    onRemove = { viewModel.removeMockInvestigator(player.playerId) }
                )
            }

            // Local Multi-Investigator Test Mode Controls
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NoirSecondary),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, NoirBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "LOCAL INVESTIGATOR TESTING",
                                color = GoldAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = "Add simulated co-investigators to test exact player-count mystery generation (4 to 10 players) directly on this device.",
                            color = MutedText,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.addMockInvestigator() },
                                enabled = playerCount < 10,
                                border = BorderStroke(1.dp, GoldMuted),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("add_mock_investigator_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ Add Investigator", fontSize = 11.sp)
                            }
                            if (playerCount > 1 && isHost) {
                                OutlinedButton(
                                    onClick = {
                                        val lastNonHost = playersList.lastOrNull { !it.isHost }
                                        if (lastNonHost != null) {
                                            viewModel.removeMockInvestigator(lastNonHost.playerId)
                                        }
                                    },
                                    border = BorderStroke(1.dp, NoirBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningRedBright),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.testTag("remove_mock_investigator_button")
                                ) {
                                    Text("- Remove", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Action Bar
        Surface(
            color = NoirSecondary,
            border = BorderStroke(1.dp, NoirBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isHost) {
                    Button(
                        onClick = { viewModel.setGenerateConfirmationDialog(true) },
                        enabled = canGenerate,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldAccent,
                            contentColor = NoirBackground,
                            disabledContainerColor = NoirCard,
                            disabledContentColor = DimText
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("generate_mystery_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (canGenerate) NoirBackground else DimText
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (playerCount < 4) {
                                "GENERATE MYSTERY (MIN 4 PLAYERS)"
                            } else {
                                "GENERATE MYSTERY FOR $playerCount PLAYERS"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NoirCard),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, NoirBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = GoldAccent,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "WAITING FOR HOST",
                                    color = PaperText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "The host will initiate case generation once all $playerCount players have gathered.",
                                    color = MutedText,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerRosterCard(
    player: PlayerInfo,
    isLocalPlayer: Boolean,
    canRemove: Boolean,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isLocalPlayer) NoirCard else NoirSecondary
        ),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            if (player.isHost) GoldAccent.copy(alpha = 0.5f) else NoirBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Avatar Circle
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (player.isHost) GoldAccent else NoirBorder),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (player.isHost) Icons.Default.Stars else Icons.Default.Person,
                        contentDescription = null,
                        tint = if (player.isHost) NoirBackground else PaperText,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = player.playerName,
                            color = PaperText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (isLocalPlayer) {
                            Text(
                                text = "(You)",
                                color = GoldAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Connection Dot
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (player.isConnected) ClueFound else DimText)
                        )
                        Text(
                            text = if (player.isConnected) "Connected" else "Disconnected",
                            color = if (player.isConnected) MutedText else DimText,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (player.isHost) {
                    Surface(
                        color = GoldAccent.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, GoldAccent),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "HOST",
                            color = GoldAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    color = if (player.isReady) ClueFound.copy(alpha = 0.2f) else NoirBorder,
                    border = BorderStroke(1.dp, if (player.isReady) ClueFound else DimText),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (player.isReady) "READY" else "WAITING",
                        color = if (player.isReady) ClueFound else DimText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (canRemove) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove Investigator",
                            tint = DimText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
