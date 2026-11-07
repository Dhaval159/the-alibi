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
import com.example.model.EvidenceItem
import com.example.ui.Screen
import com.example.ui.TheAlibiUiState
import com.example.ui.TheAlibiViewModel
import com.example.ui.theme.*

@Composable
fun PrivateChatScreen(
    uiState: TheAlibiUiState,
    viewModel: TheAlibiViewModel,
    modifier: Modifier = Modifier
) {
    val recipientId = uiState.activeChatRecipientPlayerId ?: "peer"
    val recipientName = uiState.activeChatRecipientName
    val myId = uiState.myPlayerId ?: "player_local"

    val conversation = uiState.privateMessages.filter {
        (it.senderPlayerId == myId && it.recipientPlayerId == recipientId) ||
                (it.senderPlayerId == recipientId && it.recipientPlayerId == myId) ||
                (recipientId == "player_allies")
    }

    var messageText by remember { mutableStateOf("") }
    var showDealDialog by remember { mutableStateOf(false) }
    var selectedAttachedEvidenceId by remember { mutableStateOf<String?>(null) }
    var showEvidenceSelector by remember { mutableStateOf(false) }

    val isChatLocked = uiState.currentRoom?.isChatLocked == true

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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.navigateTo(Screen.INTERACTIVE_SCENE) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldAccent)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "PRIVATE DISPATCH",
                                color = GoldAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = recipientName,
                                color = PaperText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            )
                        }
                    }

                    if (!isChatLocked) {
                        Button(
                            onClick = { showDealDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = NoirBackground),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("propose_deal_button")
                        ) {
                            Icon(Icons.Default.Handshake, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PROPOSE DEAL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(NoirBackground)
        ) {
            if (isChatLocked) {
                Surface(
                    color = WarningRed.copy(alpha = 0.2f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, WarningRed)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = WarningRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Private channels locked. All investigators must speak openly in Group Debate.",
                            color = PaperText,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Message transcript
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (conversation.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.QuestionAnswer, contentDescription = null, tint = DimText, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Confidential Channel Open with $recipientName",
                                    color = MutedText,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Send discrete messages, share discovered clues, or negotiate secret accords.",
                                    color = DimText,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                items(conversation, key = { it.id }) { msg ->
                    val isMe = msg.senderPlayerId == myId

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                    ) {
                        Text(
                            text = if (isMe) "You" else msg.senderCharacterName,
                            color = if (isMe) GoldLight else PaperText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isMe) NoirCard else NoirSecondary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .border(
                                    width = 1.dp,
                                    color = if (msg.messageType == "DEAL_PROPOSAL") GoldAccent else if (isMe) GoldMuted else NoirBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (msg.messageType == "DEAL_PROPOSAL") {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Handshake, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "FORMAL DEAL PROPOSAL", color = GoldAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                Text(
                                    text = msg.text,
                                    color = PaperText,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )

                                if (msg.attachedEvidenceId != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val ev = uiState.activeCase?.evidence?.find { it.id == msg.attachedEvidenceId }
                                    Surface(
                                        color = NoirBackground,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Description, contentDescription = null, tint = GoldLight, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "ATTACHED: ${ev?.name ?: msg.attachedEvidenceId}",
                                                color = GoldLight,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                // If this is a deal proposal directed at me, show accept/reject buttons
                                if (!isMe && msg.dealDetails != null && msg.messageType == "DEAL_PROPOSAL") {
                                    val deal = uiState.dealProposals.find { it.recipientPlayerId == myId && it.status == "PENDING" }
                                    if (deal != null) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { viewModel.respondToDeal(deal.id, true) },
                                                colors = ButtonDefaults.buttonColors(containerColor = ClueFound, contentColor = NoirBackground),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("ACCEPT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            OutlinedButton(
                                                onClick = { viewModel.respondToDeal(deal.id, false) },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningRed),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, WarningRed),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("REJECT", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Attached evidence chip if selected
            if (selectedAttachedEvidenceId != null) {
                val ev = uiState.activeCase?.evidence?.find { it.id == selectedAttachedEvidenceId }
                Surface(
                    color = NoirCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GoldMuted)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sharing: ${ev?.name ?: selectedAttachedEvidenceId}",
                                color = GoldLight,
                                fontSize = 11.sp
                            )
                        }
                        IconButton(
                            onClick = { selectedAttachedEvidenceId = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = PaperText, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Evidence selector drawer if toggled
            if (showEvidenceSelector) {
                Surface(
                    color = NoirSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NoirBorder)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "SELECT CLUE TO ATTACH & SHARE", color = GoldAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        val discoveredList = uiState.activeCase?.evidence?.filter { it.id in uiState.discoveredEvidenceIds } ?: emptyList()
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(discoveredList, key = { it.id }) { item ->
                                Surface(
                                    color = if (selectedAttachedEvidenceId == item.id) GoldAccent else NoirCard,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .border(1.dp, GoldMuted, RoundedCornerShape(6.dp))
                                        .clickable {
                                            selectedAttachedEvidenceId = item.id
                                            showEvidenceSelector = false
                                        }
                                ) {
                                    Text(
                                        text = item.name,
                                        color = if (selectedAttachedEvidenceId == item.id) NoirBackground else PaperText,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Input Bar
            if (!isChatLocked) {
                Surface(
                    color = NoirCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NoirBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showEvidenceSelector = !showEvidenceSelector },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = "Attach Clue", tint = if (selectedAttachedEvidenceId != null) GoldAccent else PaperText)
                        }

                        TextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text("Draft discreet dispatch...", color = DimText, fontSize = 13.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = NoirSecondary,
                                unfocusedContainerColor = NoirSecondary,
                                focusedTextColor = PaperText,
                                unfocusedTextColor = PaperText,
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 40.dp, max = 80.dp)
                                .testTag("private_message_input")
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = {
                                if (messageText.isNotBlank()) {
                                    viewModel.sendPrivateMessage(recipientId, messageText, selectedAttachedEvidenceId)
                                    messageText = ""
                                    selectedAttachedEvidenceId = null
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(GoldAccent, RoundedCornerShape(10.dp))
                                .testTag("send_private_message_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = NoirBackground, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }

    // Deal Proposal Dialog
    if (showDealDialog) {
        val presetDeals = listOf(
            Pair("Alibi Swap Accord", "I will confirm you were in the conservatory if you vouch for my presence in the dining hall."),
            Pair("Clue Exchange Pact", "I will share the study will draft if you disclose Lord Blackwood's prescription records."),
            Pair("Pact of Silence", "Neither of us brings up the half-burned receipts during the group interrogation."),
            Pair("Voting Coalition", "We coordinate our final ballots against the primary suspect discovered in Act I.")
        )
        var customTitle by remember { mutableStateOf(presetDeals[0].first) }
        var customTerms by remember { mutableStateOf(presetDeals[0].second) }

        AlertDialog(
            onDismissRequest = { showDealDialog = false },
            title = {
                Text(text = "PROPOSE MUTUAL ALLIANCE", color = GoldAccent, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Preset Accords:", color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    presetDeals.forEach { (title, terms) ->
                        Surface(
                            color = if (customTitle == title) GoldAccent.copy(alpha = 0.2f) else NoirSecondary,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .border(1.dp, if (customTitle == title) GoldAccent else NoirBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    customTitle = title
                                    customTerms = terms
                                }
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(text = title, color = GoldLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(text = terms, color = PaperText, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "Terms:", color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    TextField(
                        value = customTerms,
                        onValueChange = { customTerms = it },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = NoirSecondary,
                            unfocusedContainerColor = NoirSecondary,
                            focusedTextColor = PaperText,
                            unfocusedTextColor = PaperText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.proposeDeal(recipientId, customTitle, customTerms)
                        showDealDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = NoirBackground)
                ) {
                    Text("TRANSMIT PROPOSAL", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDealDialog = false }) {
                    Text("CANCEL", color = PaperText)
                }
            },
            containerColor = NoirCard
        )
    }
}
