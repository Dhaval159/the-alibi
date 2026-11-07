package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CharacterDossier
import com.example.model.EvidenceItem
import com.example.ui.Screen
import com.example.ui.TheAlibiUiState
import com.example.ui.TheAlibiViewModel
import com.example.ui.theme.*

@Composable
fun DebateScreen(
    uiState: TheAlibiUiState,
    viewModel: TheAlibiViewModel,
    modifier: Modifier = Modifier
) {
    val casePkg = uiState.activeCase ?: return
    val timerRemaining = uiState.gameTimer?.remainingSeconds ?: 300
    val minutes = timerRemaining / 60
    val seconds = timerRemaining % 60

    var selectedEvidenceDetail by remember { mutableStateOf<EvidenceItem?>(null) }

    Scaffold(
        containerColor = NoirBackground,
        topBar = {
            Surface(
                color = NoirCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NoirBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "ACT II: GROUP INTERROGATION & DEBATE",
                            color = GoldAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Open Floor Deliberation",
                            color = PaperText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                    }

                    // Synchronized Round Timer
                    Surface(
                        color = NoirSecondary,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.border(1.dp, GoldLight, RoundedCornerShape(8.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = String.format("%02d:%02d", minutes, seconds),
                                color = GoldAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Private Channel Lock Warning
            item {
                Surface(
                    color = WarningRed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, WarningRed, RoundedCornerShape(10.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = WarningRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "PRIVATE CHAT LOCKED: Present all theories, accusations, and contradictions in the open forum.",
                            color = PaperText,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Public Evidence Wall
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PUBLIC EVIDENCE REPOSITORY",
                            color = GoldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${uiState.discoveredEvidenceIds.size} Discovered",
                            color = GoldLight,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    val discoveredItems = casePkg.evidence.filter { it.id in uiState.discoveredEvidenceIds }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(discoveredItems, key = { it.id }) { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = NoirCard),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .width(190.dp)
                                    .border(1.dp, if (item.isMisleading) WarningRed.copy(alpha = 0.5f) else GoldMuted, RoundedCornerShape(10.dp))
                                    .clickable { selectedEvidenceDetail = item }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = item.category.uppercase(), color = GoldLight, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        if (item.isMisleading) {
                                            Text(text = "CONTESTED", color = WarningRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = item.name, color = PaperText, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = item.whatItProves, color = MutedText, fontSize = 10.sp, maxLines = 2)
                                }
                            }
                        }
                    }
                }
            }

            // Suspect Alibi & Contradiction Challenge List
            item {
                Text(
                    text = "SUSPECT ALIBIS & CHALLENGES",
                    color = GoldAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            items(casePkg.characters, key = { it.id }) { suspect ->
                val isSuspicious = suspect.id in uiState.suspiciousCharacterIds

                Card(
                    colors = CardDefaults.cardColors(containerColor = NoirCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (isSuspicious) WarningRed else NoirBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = suspect.name,
                                    color = PaperText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Serif
                                )
                                Text(
                                    text = "${suspect.occupation} • ${suspect.claimedLocation}",
                                    color = GoldLight,
                                    fontSize = 11.sp
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = { viewModel.toggleSuspectSuspicion(suspect.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSuspicious) Icons.Default.Flag else Icons.Default.OutlinedFlag,
                                        contentDescription = "Flag",
                                        tint = if (isSuspicious) WarningRed else DimText
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Claimed Alibi: \"${suspect.personalAlibi}\"",
                            color = MutedText,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.startInterrogationWithSuspect(suspect) },
                                colors = ButtonDefaults.buttonColors(containerColor = NoirSecondary, contentColor = GoldLight),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("INTERROGATE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    viewModel.selectBallotAccused(suspect.id)
                                    viewModel.navigateTo(Screen.ACCUSATION)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WarningRed, contentColor = PaperText),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ACCUSE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Proceed to Secret Voting Callout
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { viewModel.advanceToVoting() },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = NoirBackground),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("proceed_to_voting_button")
                ) {
                    Icon(Icons.Default.HowToVote, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CONCLUDE DEBATE & CAST SECRET BALLOT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }

    // Evidence inspection dialog if tapped
    if (selectedEvidenceDetail != null) {
        val ev = selectedEvidenceDetail!!
        AlertDialog(
            onDismissRequest = { selectedEvidenceDetail = null },
            title = {
                Text(text = ev.name, color = GoldAccent, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(text = "Category: ${ev.category} • Location: ${ev.discoveryLocation}", color = GoldLight, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = ev.detailedInspection, color = PaperText, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "What it proves: ${ev.whatItProves}", color = MutedText, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedEvidenceDetail = null },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = NoirBackground)
                ) {
                    Text("CLOSE")
                }
            },
            containerColor = NoirCard
        )
    }
}
