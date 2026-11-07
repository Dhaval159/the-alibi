package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.Screen
import com.example.ui.TheAlibiUiState
import com.example.ui.TheAlibiViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateRoomScreen(
    uiState: TheAlibiUiState,
    viewModel: TheAlibiViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var hostName by remember { mutableStateOf("Detective Vance") }
    var selectedTheme by remember { mutableStateOf(uiState.roomMysteryTheme) }
    var selectedDiff by remember { mutableStateOf(uiState.roomMysteryDifficulty) }
    var selectedAtmo by remember { mutableStateOf(uiState.roomAtmosphere) }
    var customPrompt by remember { mutableStateOf(uiState.roomCustomPrompt) }

    val themes = listOf(
        "Mansion inheritance mystery",
        "Hotel mystery",
        "Private island mystery",
        "Corporate office mystery",
        "Museum mystery",
        "Luxury train mystery",
        "University mystery",
        "Remote research facility"
    )

    val difficulties = listOf("Easy", "Medium", "Hard", "Expert")

    val atmospheres = listOf(
        "Classic detective mystery",
        "Psychological mystery",
        "Suspense thriller",
        "Locked-room mystery",
        "Conspiracy mystery"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NoirBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(Screen.HOME) },
                modifier = Modifier.testTag("create_room_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = GoldAccent
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "CREATE MULTIPLAYER ROOM",
                    color = GoldAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    text = "Host a new case lobby with a 6-digit code",
                    color = MutedText,
                    fontSize = 12.sp
                )
            }
        }

        HorizontalDivider(color = NoirBorder, thickness = 1.dp)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Notice Card
            Card(
                colors = CardDefaults.cardColors(containerColor = NoirCard),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GoldMuted.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Information",
                        tint = GoldLight,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "ROOM-BASED ADAPTIVE MYSTERY",
                            color = PaperText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "The mystery case will be generated AFTER your players join the lobby. The case engine adapts character rosters, deduction chains, and clue placements to the exact human player count (4 to 10 players).",
                            color = MutedText,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Host Name Input
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "YOUR DETECTIVE ALIAS",
                    color = GoldAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                OutlinedTextField(
                    value = hostName,
                    onValueChange = { hostName = it },
                    placeholder = { Text("Enter your name (e.g., Detective Vance)", color = DimText) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PaperText,
                        unfocusedTextColor = PaperText,
                        focusedBorderColor = GoldAccent,
                        unfocusedBorderColor = NoirBorder,
                        focusedContainerColor = NoirCard,
                        unfocusedContainerColor = NoirCard
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("host_name_input")
                )
            }

            // Theme Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SELECT MYSTERY THEME",
                    color = GoldAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    themes.forEach { theme ->
                        val isSelected = theme == selectedTheme
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) GoldAccent else NoirCard)
                                .border(
                                    1.dp,
                                    if (isSelected) GoldLight else NoirBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedTheme = theme }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("theme_$theme")
                        ) {
                            Text(
                                text = theme,
                                color = if (isSelected) NoirBackground else PaperText,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Difficulty Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "DEDUCTION DIFFICULTY",
                    color = GoldAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    difficulties.forEach { diff ->
                        val isSelected = diff == selectedDiff
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) GoldAccent else NoirCard)
                                .border(
                                    1.dp,
                                    if (isSelected) GoldLight else NoirBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedDiff = diff }
                                .padding(vertical = 10.dp)
                                .testTag("diff_$diff"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = diff,
                                color = if (isSelected) NoirBackground else PaperText,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Visual Atmosphere Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "VISUAL ATMOSPHERE",
                    color = GoldAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    atmospheres.forEach { atmo ->
                        val isSelected = atmo == selectedAtmo
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) GoldAccent else NoirCard)
                                .border(
                                    1.dp,
                                    if (isSelected) GoldLight else NoirBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedAtmo = atmo }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("atmo_$atmo")
                        ) {
                            Text(
                                text = atmo,
                                color = if (isSelected) NoirBackground else PaperText,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Custom Instructions (Optional)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "CUSTOM CASE DIRECTIVES (OPTIONAL)",
                    color = GoldAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                OutlinedTextField(
                    value = customPrompt,
                    onValueChange = { customPrompt = it },
                    placeholder = {
                        Text(
                            "e.g., Set in an art gallery blackout during a storm; include an antique dagger.",
                            color = DimText,
                            fontSize = 12.sp
                        )
                    },
                    minLines = 2,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PaperText,
                        unfocusedTextColor = PaperText,
                        focusedBorderColor = GoldAccent,
                        unfocusedBorderColor = NoirBorder,
                        focusedContainerColor = NoirCard,
                        unfocusedContainerColor = NoirCard
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_directives_input")
                )
            }
        }

        // Bottom Action Bar
        Surface(
            color = NoirSecondary,
            border = BorderStroke(1.dp, NoirBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    viewModel.updateRoomConfig(
                        theme = selectedTheme,
                        diff = selectedDiff,
                        atmo = selectedAtmo,
                        prompt = customPrompt
                    )
                    viewModel.createRoom(hostName)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldAccent,
                    contentColor = NoirBackground
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp)
                    .testTag("create_room_submit_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MeetingRoom,
                    contentDescription = null,
                    tint = NoirBackground
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CREATE ROOM & OPEN LOBBY",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
        }
    }
}
