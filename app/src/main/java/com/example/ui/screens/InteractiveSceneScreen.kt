package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.InvestigationLocation
import com.example.model.SceneHotspot
import com.example.ui.Screen
import com.example.ui.TheAlibiUiState
import com.example.ui.TheAlibiViewModel
import com.example.ui.theme.*

@Composable
fun InteractiveSceneScreen(
    uiState: TheAlibiUiState,
    viewModel: TheAlibiViewModel,
    modifier: Modifier = Modifier
) {
    val casePkg = uiState.activeCase ?: return
    val locations = casePkg.locations.ifEmpty {
        listOf(
            InvestigationLocation(
                id = "loc_study",
                name = casePkg.crimeScene.roomName,
                description = casePkg.crimeScene.description,
                atmosphereSnippet = "Rain lashes the heavy leaded panes of the study.",
                isPrimaryCrimeScene = true,
                hotspots = casePkg.crimeScene.hotspots
            )
        )
    }

    val currentLocation = locations.find { it.id == uiState.activeLocationId } ?: locations.first()
    val activeHotspots = currentLocation.hotspots

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(NoirBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (uiState.selectedHotspot != null) 260.dp else 12.dp)
        ) {
            // Location Switcher Tabs
            Text(
                text = "MANOR LOCATIONS",
                color = GoldAccent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(locations, key = { it.id }) { loc ->
                    val isSelected = loc.id == currentLocation.id
                    Surface(
                        color = if (isSelected) GoldAccent else NoirCard,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .border(1.dp, if (isSelected) GoldLight else NoirBorder, RoundedCornerShape(8.dp))
                            .clickable { viewModel.setActiveLocation(loc.id) }
                            .testTag("location_tab_${loc.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (loc.isPrimaryCrimeScene) Icons.Default.Warning else Icons.Default.MeetingRoom,
                                contentDescription = null,
                                tint = if (isSelected) NoirBackground else if (loc.isPrimaryCrimeScene) WarningRed else GoldMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = loc.name,
                                color = if (isSelected) NoirBackground else PaperText,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Location Header & Action Shortcuts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentLocation.name.uppercase(),
                        color = PaperText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                    Text(
                        text = currentLocation.atmosphereSnippet,
                        color = MutedText,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Objectives Shortcut
                    Surface(
                        color = NoirSecondary,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .border(1.dp, GoldMuted, RoundedCornerShape(8.dp))
                            .clickable { viewModel.navigateTo(Screen.OBJECTIVE_CENTER) }
                            .testTag("open_objectives_shortcut")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Checklist, contentDescription = null, tint = GoldLight, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${uiState.primaryObjective?.progress ?: 0}%",
                                color = GoldLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Private Chat Shortcut
                    Surface(
                        color = NoirSecondary,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .border(1.dp, NoirBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                val firstOther = uiState.currentRoom?.players?.firstOrNull { it.playerId != uiState.myPlayerId }
                                    ?: uiState.activeCase?.characters?.firstOrNull { it.id != uiState.assignedCharacter?.id }
                                if (firstOther != null) {
                                    val otherId = if (firstOther is com.example.model.PlayerInfo) firstOther.playerId else (firstOther as com.example.model.CharacterDossier).id
                                    val otherName = if (firstOther is com.example.model.PlayerInfo) firstOther.playerName else (firstOther as com.example.model.CharacterDossier).name
                                    viewModel.openPrivateChat(otherId, otherName)
                                } else {
                                    viewModel.openPrivateChat("player_allies", "Investigator Peers")
                                }
                            }
                            .testTag("open_chat_shortcut")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.ChatBubble, contentDescription = null, tint = PaperText, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${uiState.privateMessages.size}",
                                color = PaperText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive Scene Image Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, GoldMuted, RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_crime_scene),
                    contentDescription = currentLocation.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Hotspots Overlay on image
                activeHotspots.forEach { hotspot ->
                    val isInspected = hotspot.id in uiState.inspectedHotspotIds
                    val isSelected = uiState.selectedHotspot?.id == hotspot.id

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = (hotspot.normalizedX * 260).dp,
                                top = (hotspot.normalizedY * 300).dp
                            )
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) GoldAccent else if (isInspected) ClueFound else HotspotGlow,
                            modifier = Modifier
                                .size(38.dp)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.5.dp,
                                    color = if (isSelected) PaperText else GoldLight,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.selectHotspot(hotspot) }
                                .testTag("hotspot_${hotspot.id}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isInspected) Icons.Default.Check else Icons.Default.Search,
                                    contentDescription = hotspot.title,
                                    tint = if (isSelected) NoirBackground else PaperText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Inspectable Objects Horizontal Carousel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SEARCH CLUES IN ROOM",
                    color = MutedText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${activeHotspots.count { it.id in uiState.inspectedHotspotIds }} / ${activeHotspots.size} Found",
                    color = GoldLight,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(activeHotspots, key = { it.id }) { spot ->
                    val isInspected = spot.id in uiState.inspectedHotspotIds
                    val isSelected = uiState.selectedHotspot?.id == spot.id

                    Surface(
                        color = if (isSelected) GoldAccent else NoirCard,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = if (isSelected) GoldLight else NoirBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { viewModel.selectHotspot(spot) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (isInspected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isSelected) NoirBackground else if (isInspected) ClueFound else DimText,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = spot.title,
                                color = if (isSelected) NoirBackground else PaperText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Inspection Bottom Sheet
        AnimatedVisibility(
            visible = uiState.selectedHotspot != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val spot = uiState.selectedHotspot
            if (spot != null) {
                val associatedEvidence = casePkg.evidence.find { it.id == spot.associatedEvidenceId }
                val isEvidenceCollected = spot.associatedEvidenceId in uiState.discoveredEvidenceIds

                Card(
                    colors = CardDefaults.cardColors(containerColor = NoirCard),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, GoldAccent, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = spot.title,
                                color = GoldAccent,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            )
                            IconButton(
                                onClick = { viewModel.selectHotspot(null) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = PaperText)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = spot.inspectableDescription,
                            color = PaperText,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        if (associatedEvidence != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = NoirSecondary,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = null,
                                            tint = GoldLight,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = "FORENSIC LOG: ${associatedEvidence.name}", color = GoldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = associatedEvidence.detailedInspection, color = PaperText, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "What it proves: ${associatedEvidence.whatItProves}", color = MutedText, fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (associatedEvidence != null && !isEvidenceCollected) {
                                Button(
                                    onClick = {
                                        viewModel.collectEvidence(associatedEvidence.id)
                                        viewModel.selectHotspot(null)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = NoirBackground),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("collect_evidence_button")
                                ) {
                                    Icon(imageVector = Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "COLLECT CLUE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.selectHotspot(null) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NoirSecondary, contentColor = PaperText),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                ) {
                                    Text(text = if (isEvidenceCollected) "Clue Logged In Dossier" else "Done Inspecting", fontSize = 12.sp)
                                }
                            }

                            if (associatedEvidence != null) {
                                OutlinedButton(
                                    onClick = {
                                        val firstOther = uiState.currentRoom?.players?.firstOrNull { it.playerId != uiState.myPlayerId }
                                        val targetId = firstOther?.playerId ?: "player_allies"
                                        val targetName = firstOther?.playerName ?: "Investigator Peers"
                                        viewModel.openPrivateChat(targetId, targetName)
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldLight),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldMuted),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "SHARE", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
