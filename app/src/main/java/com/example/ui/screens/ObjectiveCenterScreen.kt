package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import com.example.model.PersonalObjective
import com.example.ui.Screen
import com.example.ui.TheAlibiUiState
import com.example.ui.TheAlibiViewModel
import com.example.ui.theme.*

@Composable
fun ObjectiveCenterScreen(
    uiState: TheAlibiUiState,
    viewModel: TheAlibiViewModel,
    modifier: Modifier = Modifier
) {
    val myChar = uiState.assignedCharacter
    val isCulprit = uiState.myAssignedCharacter?.isCulprit == true

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
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.INTERACTIVE_SCENE) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldAccent)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "MISSION OBJECTIVE CENTER",
                            color = GoldAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${myChar?.name ?: "Investigator"} — Objectives",
                            color = PaperText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Identity & Role Badge
            item {
                Surface(
                    color = if (isCulprit) WarningRed.copy(alpha = 0.15f) else NoirCard,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (isCulprit) WarningRed else GoldMuted, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isCulprit) Icons.Default.VisibilityOff else Icons.Default.Security,
                            contentDescription = null,
                            tint = if (isCulprit) WarningRed else GoldAccent,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isCulprit) "CLASSIFIED ROLE: THE CULPRIT" else "ASSIGNED ROLE: INNOCENT INVESTIGATOR",
                                color = if (isCulprit) WarningRed else GoldAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isCulprit) "Conceal your murder of Lord Blackwood. Divert suspicion to others." else "Uncover the killer's identity and broken alibi while protecting your private secret.",
                                color = PaperText,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Primary Objective
            item {
                val primary = uiState.primaryObjective ?: PersonalObjective(
                    id = "obj_p",
                    title = if (isCulprit) "Conceal Identity & Shift Blame" else "Identify True Culprit & Gather Smoking Gun",
                    description = if (isCulprit) "Maintain your alibi in the conservatory, conceal the stolen will draft, and accuse Dr. Finch or Beatrice." else "Inspect crime scene hotspots, discover contradictions in suspect testimonies, and vote correctly.",
                    category = com.example.model.ObjectiveCategory.INVESTIGATION,
                    isPrimary = true,
                    points = if (isCulprit) 200 else 150,
                    progress = 40
                )

                ObjectiveCard(
                    objective = primary,
                    badgeLabel = "PRIMARY MISSION (REQUIRED)",
                    badgeColor = GoldAccent,
                    onAdvance = {
                        // Advance primary progress
                        // Through ViewModel inspect or deduction
                    }
                )
            }

            // Secondary Objective
            item {
                val secondary = uiState.secondaryObjective ?: PersonalObjective(
                    id = "obj_s",
                    title = if (isCulprit) "Secure Alibi Witness via Secret Deal" else "Protect Private Secret & Forge Alliance",
                    description = if (isCulprit) "Form a pact with at least one player to attest to your presence during the 23:15 window." else "Keep your personal past hidden while trading evidence with a trusted ally.",
                    category = com.example.model.ObjectiveCategory.NEGOTIATION,
                    isPrimary = false,
                    points = 100,
                    progress = 30
                )

                ObjectiveCard(
                    objective = secondary,
                    badgeLabel = "SECONDARY BONUS (BONUS +100 PTS)",
                    badgeColor = ClueFound,
                    onAdvance = {
                        // Advance secondary progress
                    }
                )
            }

            // Private Secret Protection
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NoirCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NoirBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = WarningRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "CONFIDENTIAL SECRET (PROTECT FROM EXPOSURE)", color = WarningRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = myChar?.privateSecret ?: "No recorded secrets.",
                            color = PaperText,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Active Accords & Deals
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NoirCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NoirBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Handshake, contentDescription = null, tint = GoldLight, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "CONFIDENTIAL DEALS & ALLIANCES", color = GoldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        val deals = uiState.dealProposals
                        if (deals.isEmpty()) {
                            Text(
                                text = "No mutual pacts active. Open Private Dispatch with an investigator to propose an alliance.",
                                color = MutedText,
                                fontSize = 12.sp
                            )
                        } else {
                            deals.forEach { deal ->
                                Surface(
                                    color = NoirSecondary,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = deal.title, color = PaperText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text(text = deal.terms, color = MutedText, fontSize = 11.sp)
                                        }
                                        Surface(
                                            color = if (deal.status == "ACCEPTED") ClueFound.copy(alpha = 0.2f) else WarningRed.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = deal.status,
                                                color = if (deal.status == "ACCEPTED") ClueFound else WarningRed,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ObjectiveCard(
    objective: PersonalObjective,
    badgeLabel: String,
    badgeColor: androidx.compose.ui.graphics.Color,
    onAdvance: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NoirCard),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GoldMuted, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = badgeColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = badgeLabel,
                        color = badgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Text(
                    text = "+${objective.points} PTS",
                    color = GoldLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = objective.title,
                color = PaperText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = objective.description,
                color = MutedText,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "PROGRESS: ${objective.progress}%", color = GoldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                if (objective.isCompleted) {
                    Surface(
                        color = ClueFound.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(text = "COMPLETED", color = ClueFound, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { objective.progress / 100f },
                color = if (objective.isCompleted) ClueFound else GoldAccent,
                trackColor = NoirSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )
        }
    }
}
