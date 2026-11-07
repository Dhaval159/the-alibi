package com.example.ui.screens

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
import com.example.model.CaseGenerationConfig
import com.example.ui.Screen
import com.example.ui.TheAlibiUiState
import com.example.ui.TheAlibiViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CaseGeneratorScreen(
    uiState: TheAlibiUiState,
    viewModel: TheAlibiViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var currentConfig by remember { mutableStateOf(uiState.generationConfig) }

    val themes = listOf(
        "Mansion inheritance mystery",
        "Hotel mystery",
        "Private island mystery",
        "Corporate office mystery",
        "Museum mystery",
        "Luxury train mystery",
        "University mystery",
        "Remote research facility",
        "Custom setting"
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
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .widthIn(max = 600.dp)
    ) {
        // Back header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(Screen.HOME) },
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NoirSecondary)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = PaperText
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "CLASSIFIED GENERATOR",
                    color = GoldAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Draft New Mystery",
                    color = PaperText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section 1: Mystery Theme
        Text(
            text = "1. SELECT SETTING & THEME",
            color = GoldLight,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            themes.forEach { theme ->
                val isSelected = currentConfig.theme == theme
                Surface(
                    color = if (isSelected) GoldAccent else NoirCard,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = if (isSelected) GoldLight else NoirBorder,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            currentConfig = currentConfig.copy(theme = theme)
                            viewModel.updateGenerationConfig(currentConfig)
                        }
                ) {
                    Text(
                        text = theme,
                        color = if (isSelected) NoirBackground else PaperText,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section 2: Atmosphere
        Text(
            text = "2. MYSTERY ATMOSPHERE",
            color = GoldLight,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            atmospheres.forEach { atmo ->
                val isSelected = currentConfig.atmosphere == atmo
                Surface(
                    color = if (isSelected) GoldMuted else NoirCard,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = if (isSelected) GoldAccent else NoirBorder,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            currentConfig = currentConfig.copy(atmosphere = atmo)
                            viewModel.updateGenerationConfig(currentConfig)
                        }
                ) {
                    Text(
                        text = atmo,
                        color = PaperText,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section 3: Difficulty & Human Players
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Difficulty column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "3. DIFFICULTY",
                    color = GoldLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    difficulties.forEach { diff ->
                        val isSelected = currentConfig.difficulty == diff
                        Surface(
                            color = if (isSelected) GoldAccent else NoirCard,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) GoldLight else NoirBorder,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    currentConfig = currentConfig.copy(difficulty = diff)
                                    viewModel.updateGenerationConfig(currentConfig)
                                }
                        ) {
                            Text(
                                text = diff,
                                color = if (isSelected) NoirBackground else PaperText,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
                            )
                        }
                    }
                }
            }

            // Player count column (4 to 10)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "4. HUMAN PLAYERS",
                    color = GoldLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = NoirCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NoirBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${currentConfig.playerCount} Players",
                            color = GoldLight,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                        Text(
                            text = "Supported: 4 to 10 players",
                            color = MutedText,
                            fontSize = 10.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Slider(
                            value = currentConfig.playerCount.toFloat(),
                            onValueChange = {
                                currentConfig = currentConfig.copy(playerCount = it.toInt())
                                viewModel.updateGenerationConfig(currentConfig)
                            },
                            valueRange = 4f..10f,
                            steps = 5,
                            colors = SliderDefaults.colors(
                                thumbColor = GoldAccent,
                                activeTrackColor = GoldAccent,
                                inactiveTrackColor = NoirBorder
                            ),
                            modifier = Modifier.testTag("player_count_slider")
                        )

                        Text(
                            text = "Guarantees 1 human culprit + innocent suspects & NPC witnesses",
                            color = DimText,
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section 5: Optional Custom Prompt
        Text(
            text = "5. CUSTOM DIRECTIVE (OPTIONAL)",
            color = GoldLight,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = currentConfig.customPrompt,
            onValueChange = {
                currentConfig = currentConfig.copy(customPrompt = it)
                viewModel.updateGenerationConfig(currentConfig)
            },
            placeholder = { Text("e.g., A snowy Swiss ski chalet where an heiress vanishes...", color = DimText, fontSize = 13.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = NoirCard,
                unfocusedContainerColor = NoirCard,
                focusedBorderColor = GoldAccent,
                unfocusedBorderColor = NoirBorder,
                focusedTextColor = PaperText,
                unfocusedTextColor = PaperText
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("custom_prompt_input")
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Submit Button
        Button(
            onClick = {
                viewModel.startCaseGeneration()
            },
            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = NoirBackground),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("start_case_generation_submit")
        ) {
            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "COMMENCE GENERATION",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
