package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.model.CharacterDossier
import com.example.ui.Screen
import com.example.ui.TheAlibiUiState
import com.example.ui.TheAlibiViewModel
import com.example.ui.theme.*

@Composable
fun SuspectGalleryScreen(
    uiState: TheAlibiUiState,
    viewModel: TheAlibiViewModel,
    modifier: Modifier = Modifier
) {
    val casePkg = uiState.activeCase ?: return

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
                    text = "PERSONS OF INTEREST",
                    color = GoldAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Suspect Dossiers",
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
                    text = "${casePkg.characters.size} Suspects",
                    color = GoldLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(casePkg.characters, key = { it.id }) { suspect ->
                val isSuspicious = suspect.id in uiState.suspiciousCharacterIds

                SuspectCardItem(
                    suspect = suspect,
                    isSuspicious = isSuspicious,
                    onToggleSuspicion = {
                        viewModel.toggleSuspectSuspicion(suspect.id)
                    },
                    onInterrogate = {
                        viewModel.openInterrogation(suspect)
                    },
                    onPrivateDispatch = {
                        viewModel.openPrivateChat(suspect.id, suspect.name)
                    }
                )
            }
        }
    }
}

@Composable
fun SuspectCardItem(
    suspect: CharacterDossier,
    isSuspicious: Boolean,
    onToggleSuspicion: () -> Unit,
    onInterrogate: () -> Unit,
    onPrivateDispatch: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NoirCard),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSuspicious) 1.5.dp else 1.dp,
                color = if (isSuspicious) WarningRedBright else NoirBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .testTag("suspect_card_${suspect.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Portrait avatar
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(NoirSecondary)
                        .border(1.5.dp, if (isSuspicious) WarningRedBright else GoldAccent, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (isSuspicious) WarningRedBright else GoldLight,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = suspect.name,
                        color = PaperText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                    Text(
                        text = "${suspect.occupation} • Age ${suspect.age}",
                        color = GoldLight,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Relation: ${suspect.relationshipWithVictim}",
                        color = MutedText,
                        fontSize = 11.sp
                    )
                }

                // Suspicion flag button
                IconButton(
                    onClick = onToggleSuspicion,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isSuspicious) Icons.Default.Warning else Icons.Default.WarningAmber,
                        contentDescription = "Toggle Suspicion",
                        tint = if (isSuspicious) WarningRedBright else DimText
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = suspect.publicBio,
                color = MutedText,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = NoirSecondary,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "CLAIMED ALIBI: ", color = DimText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(text = suspect.personalAlibi.ifBlank { "Unverified" }, color = PaperText, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onInterrogate,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = NoirBackground),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("interrogate_button_${suspect.id}")
                ) {
                    Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "INTERROGATE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onPrivateDispatch,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldLight),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldMuted),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("private_dispatch_button_${suspect.id}")
                ) {
                    Icon(imageVector = Icons.Default.ChatBubble, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "DISPATCH", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
