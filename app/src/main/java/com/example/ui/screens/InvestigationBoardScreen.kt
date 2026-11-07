package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.Screen
import com.example.ui.TheAlibiUiState
import com.example.ui.TheAlibiViewModel
import com.example.ui.theme.*

@Composable
fun InvestigationBoardScreen(
    uiState: TheAlibiUiState,
    viewModel: TheAlibiViewModel,
    modifier: Modifier = Modifier
) {
    val casePkg = uiState.activeCase ?: return
    var noteInput by remember { mutableStateOf("") }
    var showAddConnectionDialog by remember { mutableStateOf(false) }

    var selectedSuspectId by remember { mutableStateOf(casePkg.characters.firstOrNull()?.id ?: "") }
    var selectedEvidenceId by remember { mutableStateOf(casePkg.evidence.firstOrNull()?.id ?: "") }
    var connectionLabel by remember { mutableStateOf("Contradicts alibi") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NoirBackground)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .widthIn(max = 600.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "THEORY & DEDUCTION",
                    color = GoldAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Investigation Board",
                    color = PaperText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
            }

            Button(
                onClick = { showAddConnectionDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = NoirBackground),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(38.dp)
            ) {
                Icon(imageVector = Icons.Default.AddLink, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Connect Clue", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Visual Board Pinboard Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, GoldMuted, RoundedCornerShape(12.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_investigation_board),
                contentDescription = "Investigation Board",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Surface(
                color = NoirBackground.copy(alpha = 0.75f),
                shape = RoundedCornerShape(topStart = 8.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                Text(
                    text = "${uiState.boardConnections.size} Connected Links • ${uiState.deductionNotes.size} Notes",
                    color = GoldLight,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Connected Deduction Threads List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            item {
                Text(
                    text = "ACTIVE DEDUCTION THREADS",
                    color = GoldAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            if (uiState.boardConnections.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NoirCard),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No connections linked yet. Tap 'Connect Clue' to pin a suspect to discovered evidence.",
                            color = MutedText,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(uiState.boardConnections, key = { it.id }) { conn ->
                    val suspect = casePkg.characters.find { it.id == conn.suspectId }
                    val evidence = casePkg.evidence.find { it.id == conn.evidenceId }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = NoirCard),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, NoirBorder, RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Timeline, contentDescription = null, tint = WarningRedBright, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${suspect?.name ?: "Suspect"} ➔ ${evidence?.name ?: "Evidence"}",
                                    color = PaperText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = conn.label,
                                    color = GoldLight,
                                    fontSize = 11.sp
                                )
                            }
                            IconButton(
                                onClick = { viewModel.removeBoardConnection(conn.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = DimText, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Section: Detective Notes Notebook
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "DETECTIVE'S PRIVATE NOTES",
                    color = GoldAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            items(uiState.deductionNotes, key = { it.id }) { note ->
                Surface(
                    color = NoirSecondary,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = note.text, color = PaperText, fontSize = 12.sp, lineHeight = 17.sp)
                        Text(text = note.timestamp, color = DimText, fontSize = 10.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Note Add Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = noteInput,
                onValueChange = { noteInput = it },
                placeholder = { Text("Log observation or theory...", color = DimText, fontSize = 12.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = PaperText,
                    unfocusedTextColor = PaperText,
                    focusedBorderColor = GoldAccent,
                    unfocusedBorderColor = NoirBorder
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (noteInput.isNotBlank()) {
                        viewModel.addDeductionNote(noteInput)
                        noteInput = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = NoirBackground),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text(text = "Add", fontWeight = FontWeight.Bold)
            }
        }

        // Add Connection Modal Dialog
        if (showAddConnectionDialog) {
            AlertDialog(
                onDismissRequest = { showAddConnectionDialog = false },
                containerColor = NoirCard,
                shape = RoundedCornerShape(16.dp),
                title = { Text(text = "Connect Clue to Suspect", color = GoldAccent, fontSize = 18.sp, fontFamily = FontFamily.Serif) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = "Select Suspect:", color = GoldLight, fontSize = 12.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(casePkg.characters) { ch ->
                                val isSelected = selectedSuspectId == ch.id
                                Surface(
                                    color = if (isSelected) GoldAccent else NoirSecondary,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.clickable { selectedSuspectId = ch.id }
                                ) {
                                    Text(
                                        text = ch.name.substringBefore(" "),
                                        color = if (isSelected) NoirBackground else PaperText,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Text(text = "Select Evidence:", color = GoldLight, fontSize = 12.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(casePkg.evidence.filter { it.id in uiState.discoveredEvidenceIds }) { ev ->
                                val isSelected = selectedEvidenceId == ev.id
                                Surface(
                                    color = if (isSelected) GoldAccent else NoirSecondary,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.clickable { selectedEvidenceId = ev.id }
                                ) {
                                    Text(
                                        text = ev.name.take(16),
                                        color = if (isSelected) NoirBackground else PaperText,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Text(text = "Deduction Relationship:", color = GoldLight, fontSize = 12.sp)
                        OutlinedTextField(
                            value = connectionLabel,
                            onValueChange = { connectionLabel = it },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = PaperText,
                                unfocusedTextColor = PaperText,
                                focusedBorderColor = GoldAccent
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.addBoardConnection(selectedSuspectId, selectedEvidenceId, connectionLabel)
                            showAddConnectionDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = NoirBackground)
                    ) {
                        Text("Pin Thread", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddConnectionDialog = false }) {
                        Text("Cancel", color = PaperText)
                    }
                }
            )
        }
    }
}
