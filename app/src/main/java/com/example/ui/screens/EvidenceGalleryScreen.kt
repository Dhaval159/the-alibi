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
import com.example.model.EvidenceItem
import com.example.ui.Screen
import com.example.ui.TheAlibiUiState
import com.example.ui.TheAlibiViewModel
import com.example.ui.theme.*

@Composable
fun EvidenceGalleryScreen(
    uiState: TheAlibiUiState,
    viewModel: TheAlibiViewModel,
    modifier: Modifier = Modifier
) {
    val casePkg = uiState.activeCase ?: return
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var selectedEvidence by remember { mutableStateOf<EvidenceItem?>(null) }
    var noteInputText by remember { mutableStateOf("") }

    val categories = listOf("All", "WeaponTrace", "Document", "Forensic", "PersonalItem", "Environmental", "Communication")

    val filteredList = if (selectedCategoryFilter == "All") {
        casePkg.evidence
    } else {
        casePkg.evidence.filter { it.category == selectedCategoryFilter }
    }

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
                    text = "FORENSIC DOSSIER",
                    color = GoldAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Evidence Gallery",
                    color = PaperText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
            }

            Surface(
                color = NoirCard,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.border(1.dp, NoirBorder, RoundedCornerShape(8.dp))
            ) {
                Text(
                    text = "${uiState.discoveredEvidenceIds.size} / ${casePkg.evidence.size} Logged",
                    color = GoldLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { cat ->
                val isSelected = selectedCategoryFilter == cat
                Surface(
                    color = if (isSelected) GoldAccent else NoirCard,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .border(1.dp, if (isSelected) GoldLight else NoirBorder, RoundedCornerShape(8.dp))
                        .clickable { selectedCategoryFilter = cat }
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

        // Evidence Cards List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(filteredList, key = { it.id }) { item ->
                val isDiscovered = item.id in uiState.discoveredEvidenceIds
                EvidenceCardItem(
                    item = item,
                    isDiscovered = isDiscovered,
                    onClick = {
                        selectedEvidence = item
                    }
                )
            }
        }

        // Detailed Inspection Dialog
        if (selectedEvidence != null) {
            val ev = selectedEvidence!!
            val isDiscovered = ev.id in uiState.discoveredEvidenceIds

            AlertDialog(
                onDismissRequest = { selectedEvidence = null },
                containerColor = NoirCard,
                shape = RoundedCornerShape(16.dp),
                title = {
                    Column {
                        Surface(
                            color = GoldAccent.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = ev.category.uppercase(),
                                color = GoldLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = ev.name,
                            color = PaperText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                    }
                },
                text = {
                    Column {
                        Text(text = "LOCATION: ${ev.discoveryLocation}", color = GoldLight, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = ev.description, color = PaperText, fontSize = 13.sp, lineHeight = 18.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = NoirSecondary,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = "FORENSIC EXAMINATION", color = DimText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = ev.detailedInspection, color = PaperText, fontSize = 12.sp, lineHeight = 17.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "PROVES: ${ev.whatItProves}", color = ClueFound, fontSize = 12.sp, fontWeight = FontWeight.Medium)

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick Note Input
                        OutlinedTextField(
                            value = noteInputText,
                            onValueChange = { noteInputText = it },
                            placeholder = { Text("Log a detective note for this clue...", color = DimText, fontSize = 11.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = PaperText,
                                unfocusedTextColor = PaperText,
                                focusedBorderColor = GoldAccent,
                                unfocusedBorderColor = NoirBorder
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (noteInputText.isNotBlank()) {
                            Button(
                                onClick = {
                                    viewModel.addDeductionNote("${ev.name}: $noteInputText")
                                    noteInputText = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = NoirBackground)
                            ) {
                                Text("Save Note", fontSize = 12.sp)
                            }
                        }
                        if (!isDiscovered) {
                            Button(
                                onClick = {
                                    viewModel.collectEvidence(ev.id)
                                    selectedEvidence = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = NoirBackground)
                            ) {
                                Text("Collect Evidence", fontSize = 12.sp)
                            }
                        }
                        TextButton(onClick = { selectedEvidence = null }) {
                            Text("Close", color = PaperText)
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun EvidenceCardItem(
    item: EvidenceItem,
    isDiscovered: Boolean,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NoirCard),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isDiscovered) GoldMuted else NoirBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .testTag("evidence_item_${item.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (isDiscovered) GoldAccent.copy(alpha = 0.2f) else NoirSecondary,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (item.category) {
                            "WeaponTrace" -> Icons.Default.Shield
                            "Document" -> Icons.Default.Description
                            "Forensic" -> Icons.Default.Biotech
                            "PersonalItem" -> Icons.Default.Watch
                            else -> Icons.Default.Search
                        },
                        contentDescription = null,
                        tint = if (isDiscovered) GoldLight else DimText,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        color = if (isDiscovered) PaperText else MutedText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (isDiscovered) {
                        Surface(color = ClueFound.copy(alpha = 0.3f), shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = "LOGGED",
                                color = ClueFound,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${item.category} • ${item.discoveryLocation}",
                    color = GoldLight,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isDiscovered) item.description else "Undiscovered clue. Inspect crime scene or question suspects to uncover.",
                    color = if (isDiscovered) MutedText else DimText,
                    fontSize = 12.sp,
                    maxLines = 2
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = DimText
            )
        }
    }
}
