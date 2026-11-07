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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.model.InterrogationQnA
import com.example.ui.Screen
import com.example.ui.TheAlibiUiState
import com.example.ui.TheAlibiViewModel
import com.example.ui.theme.*

@Composable
fun InterrogationScreen(
    uiState: TheAlibiUiState,
    viewModel: TheAlibiViewModel,
    modifier: Modifier = Modifier
) {
    val suspect = uiState.activeInterrogationSuspect ?: return
    val categories = listOf("Motive", "Opportunity", "Timeline", "Relationships", "Evidence confrontation", "Alibi", "Suspicious behaviour")
    val currentCategory = uiState.selectedInterrogationCategory
    val history = uiState.interrogationHistory[suspect.id] ?: emptyList()

    // Available questions in the selected category
    val availableQuestions = suspect.interrogationResponses[currentCategory] ?: listOf(
        InterrogationQnA(
            question = "Can you describe your relationship with Lord Blackwood?",
            answer = "We had our dealings, detective, but nothing that would warrant this tragic outcome."
        ),
        InterrogationQnA(
            question = "Did you notice anything unusual before midnight?",
            answer = "Only the heavy storm outside and the tension lingering in the halls."
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NoirBackground)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .widthIn(max = 600.dp)
    ) {
        // Suspect Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(Screen.SUSPECT_GALLERY) },
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NoirSecondary)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = PaperText
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(NoirCard)
                    .border(1.dp, GoldAccent, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = GoldLight,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = suspect.name,
                    color = PaperText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    text = "${suspect.occupation} • Interrogation Room",
                    color = GoldLight,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Interrogation Category Filter Pills
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { cat ->
                val isSelected = currentCategory == cat
                Surface(
                    color = if (isSelected) GoldAccent else NoirCard,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .border(1.dp, if (isSelected) GoldLight else NoirBorder, RoundedCornerShape(8.dp))
                        .clickable { viewModel.setInterrogationCategory(cat) }
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) NoirBackground else PaperText,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Questions and Transcript Area
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            // Section: Choose Question
            item {
                Text(
                    text = "SELECT INQUIRY LINE ($currentCategory)",
                    color = DimText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            items(availableQuestions) { qna ->
                val isAsked = history.any { it.question == qna.question }

                Card(
                    colors = CardDefaults.cardColors(containerColor = NoirCard),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (isAsked) ClueFound else GoldMuted,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            viewModel.askQuestion(qna)
                        }
                        .testTag("question_item")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isAsked) Icons.Default.CheckCircle else Icons.Default.QuestionAnswer,
                            contentDescription = null,
                            tint = if (isAsked) ClueFound else GoldLight,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = qna.question,
                                color = PaperText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (qna.requiredEvidenceToTrigger != null) {
                                Text(
                                    text = "Evidence Confrontation: ${qna.requiredEvidenceToTrigger}",
                                    color = GoldLight,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // Section: Transcript History
            if (history.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "RECORDED TESTIMONY & RESPONSES",
                        color = GoldAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                items(history) { record ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NoirSecondary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (record.revealsContradiction) 1.5.dp else 1.dp,
                                color = if (record.revealsContradiction) ContradictionFound else NoirBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Q: ${record.question}",
                                color = GoldLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "\"${record.answer}\"",
                                color = PaperText,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                            if (record.revealsContradiction) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = WarningRed.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = WarningRedBright, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "CONTRADICTION DETECTED IN TESTIMONY",
                                            color = WarningRedBright,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
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
