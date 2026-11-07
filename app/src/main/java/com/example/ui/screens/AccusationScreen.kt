package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

@Composable
fun AccusationScreen(
    uiState: TheAlibiUiState,
    viewModel: TheAlibiViewModel,
    modifier: Modifier = Modifier
) {
    val casePkg = uiState.activeCase ?: return
    var showConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NoirBackground)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .widthIn(max = 600.dp)
    ) {
        // Header
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "FORMAL ACCUSATION",
                color = WarningRedBright,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                text = "Name the Culprit",
                color = PaperText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
            )
            Text(
                text = "Review your deductions carefully. An incorrect accusation allows the killer to escape.",
                color = MutedText,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            // 1. Accuse Suspect
            item {
                Text(
                    text = "1. SELECT THE ACCUSED CULPRIT",
                    color = GoldLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(casePkg.characters) { suspect ->
                        val isSelected = uiState.selectedAccusedCharacterId == suspect.id
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) GoldAccent else NoirCard),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .width(120.dp)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) PaperText else NoirBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.selectAccusedSuspect(suspect.id) }
                                .testTag("accuse_suspect_${suspect.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) NoirCard else NoirSecondary)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (isSelected) GoldLight else PaperText,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = suspect.name,
                                    color = if (isSelected) NoirBackground else PaperText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = suspect.occupation.take(14),
                                    color = if (isSelected) NoirBackground.copy(alpha = 0.8f) else MutedText,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // 2. Select Motive
            item {
                Text(
                    text = "2. IDENTIFY THE LETHAL MOTIVE",
                    color = GoldLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                val motiveOptions = listOf(
                    "Desperate debt & altered will disinheritance",
                    "Covering up systematic charity trust embezzlement",
                    "Blackmail over forged medical and legal records",
                    "Revenge over ancestral estate sale to overseas buyers"
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    motiveOptions.forEach { motive ->
                        val isSelected = uiState.selectedAccusationMotive == motive
                        Surface(
                            color = if (isSelected) GoldAccent else NoirCard,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, if (isSelected) GoldLight else NoirBorder, RoundedCornerShape(10.dp))
                                .clickable { viewModel.selectAccusationMotive(motive) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.selectAccusationMotive(motive) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = NoirBackground,
                                        unselectedColor = GoldMuted
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = motive,
                                    color = if (isSelected) NoirBackground else PaperText,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // 3. Select Smoking Gun Evidence
            item {
                Text(
                    text = "3. PRESENT THE SMOKING GUN CLUE",
                    color = GoldLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    casePkg.evidence.filter { it.id in uiState.discoveredEvidenceIds }.forEach { ev ->
                        val isSelected = uiState.selectedSmokingGunEvidenceId == ev.id
                        Surface(
                            color = if (isSelected) WarningRed else NoirCard,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, if (isSelected) WarningRedBright else NoirBorder, RoundedCornerShape(10.dp))
                                .clickable { viewModel.selectSmokingGunEvidence(ev.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isSelected) PaperText else DimText
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = ev.name,
                                        color = PaperText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = ev.whatItProves,
                                        color = if (isSelected) PaperText.copy(alpha = 0.8f) else MutedText,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val canSubmit = uiState.selectedAccusedCharacterId != null &&
                uiState.selectedAccusationMotive.isNotBlank() &&
                uiState.selectedSmokingGunEvidenceId != null

        Button(
            onClick = { showConfirmDialog = true },
            enabled = canSubmit,
            colors = ButtonDefaults.buttonColors(
                containerColor = WarningRed,
                contentColor = PaperText,
                disabledContainerColor = NoirSecondary,
                disabledContentColor = DimText
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("submit_accusation_button")
        ) {
            Icon(imageVector = Icons.Default.Gavel, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "DELIVER FINAL ACCUSATION", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        // Confirmation Modal Dialog
        if (showConfirmDialog) {
            val accusedChar = casePkg.characters.find { it.id == uiState.selectedAccusedCharacterId }
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                containerColor = NoirCard,
                shape = RoundedCornerShape(16.dp),
                title = { Text(text = "Confirm Verdict Submission", color = GoldAccent, fontSize = 18.sp, fontFamily = FontFamily.Serif) },
                text = {
                    Text(
                        text = "You are formally charging ${accusedChar?.name ?: "the suspect"} with the first-degree murder of Lord Reginald Blackwood.\n\nAre you prepared to close the case?",
                        color = PaperText,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showConfirmDialog = false
                            viewModel.submitAccusation()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WarningRed, contentColor = PaperText)
                    ) {
                        Text("Charge Suspect", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) {
                        Text("Review Evidence", color = PaperText)
                    }
                }
            )
        }
    }
}
