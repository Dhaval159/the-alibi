package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
fun ResolutionScreen(
    uiState: TheAlibiUiState,
    viewModel: TheAlibiViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val resolution = uiState.gameResolution
    val legacyVerdict = uiState.verdict

    val isConvicted = resolution?.isCulpritConvicted ?: legacyVerdict?.isCorrect ?: true
    val culpritName = resolution?.trueCulpritName ?: legacyVerdict?.trueCulpritName ?: "Edward Vance"
    val motive = resolution?.trueMotive ?: legacyVerdict?.solution?.motive ?: "Desperate debts of 40,000 pounds due at midnight."
    val method = resolution?.trueMethod ?: legacyVerdict?.solution?.method ?: "Laced belladonna into vintage scotch decanter."
    val confession = resolution?.culpritConfession ?: "Edward broke down when confronted with the silver cigarette case and the wet terrace clay: 'I had no choice! Monte Carlo syndicates gave me until midnight.'"
    val misleading = resolution?.misleadingCluesExplanation ?: "Lady Beatrice's burned receipts were to hide charity embezzlement, not murder."

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NoirBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .widthIn(max = 600.dp)
    ) {
        // Verdict Result Banner
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isConvicted) ClueFound.copy(alpha = 0.2f) else WarningRed.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    color = if (isConvicted) ClueFound else WarningRedBright,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (isConvicted) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (isConvicted) ClueFound else WarningRedBright,
                    modifier = Modifier.size(54.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isConvicted) "CASE CLOSED: CONVICTION SECURED" else "MISCARRIAGE OF JUSTICE",
                    color = if (isConvicted) ClueFound else WarningRedBright,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isConvicted) "The Culprit Was Unmasked" else "The Real Killer Slipped Away",
                    color = PaperText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Culprit Reveal Section
        Card(
            colors = CardDefaults.cardColors(containerColor = NoirCard),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GoldAccent, RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(NoirSecondary)
                            .border(1.5.dp, WarningRedBright, CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = WarningRedBright, modifier = Modifier.size(30.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(text = "TRUE GUILTY PARTY", color = WarningRedBright, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(text = culpritName, color = PaperText, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                        Text(text = "Charge Confirmed by Ballots", color = ClueFound, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = NoirBorder)
                Spacer(modifier = Modifier.height(14.dp))

                Text(text = "CRIME METHOD & EXECUTION", color = GoldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = method, color = PaperText, fontSize = 13.sp, lineHeight = 19.sp)

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "TRUE MOTIVE", color = GoldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = motive, color = PaperText, fontSize = 13.sp, lineHeight = 19.sp)

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "CULPRIT CONFESSION STATEMENT", color = GoldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "\"$confession\"", color = MutedText, fontSize = 13.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, lineHeight = 19.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Misleading Clues Explained
        Card(
            colors = CardDefaults.cardColors(containerColor = NoirSecondary),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NoirBorder, RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "DEBUNKING MISLEADING RED HERRINGS",
                    color = GoldAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = misleading,
                    color = PaperText,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Player Scoring & Detective Titles
        if (resolution != null && resolution.playerScores.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = NoirCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GoldMuted, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "INVESTIGATOR SCORING & ACHIEVEMENTS",
                        color = GoldLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    resolution.playerScores.forEach { scoreCard ->
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
                                    Text(text = "${scoreCard.playerName} (${scoreCard.characterName})", color = PaperText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "Titles: ${scoreCard.earnedAchievements.joinToString(" • ")}",
                                        color = GoldLight,
                                        fontSize = 11.sp
                                    )
                                }
                                Surface(
                                    color = GoldAccent.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${scoreCard.totalScore} PTS",
                                        color = GoldAccent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Return to Home
        Button(
            onClick = { viewModel.navigateTo(Screen.HOME) },
            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = NoirBackground),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("return_home_button")
        ) {
            Icon(imageVector = Icons.Default.Home, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "RETURN TO CASE ARCHIVES", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
