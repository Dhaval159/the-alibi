package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.Screen
import com.example.ui.TheAlibiUiState
import com.example.ui.TheAlibiViewModel
import com.example.ui.theme.*

@Composable
fun JoinRoomScreen(
    uiState: TheAlibiUiState,
    viewModel: TheAlibiViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var roomCode by remember { mutableStateOf("") }
    var playerName by remember { mutableStateOf("Detective Guest") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NoirBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(Screen.HOME) },
                modifier = Modifier.testTag("join_room_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = GoldAccent
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "JOIN CASE ROOM",
                    color = GoldAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    text = "Enter a 6-digit room code to join an investigation",
                    color = MutedText,
                    fontSize = 12.sp
                )
            }
        }

        HorizontalDivider(color = NoirBorder, thickness = 1.dp)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Room Icon Badge
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(NoirCard, RoundedCornerShape(36.dp))
                    .border(2.dp, GoldAccent, RoundedCornerShape(36.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = "ENTER 6-DIGIT CODE",
                color = PaperText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Serif
            )

            // Room Code Input
            OutlinedTextField(
                value = roomCode,
                onValueChange = {
                    if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                        roomCode = it
                    }
                },
                placeholder = {
                    Text(
                        "000000",
                        color = DimText,
                        fontSize = 28.sp,
                        letterSpacing = 6.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 28.sp,
                    letterSpacing = 6.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = GoldAccent,
                    fontFamily = FontFamily.Monospace
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = GoldAccent,
                    unfocusedTextColor = GoldAccent,
                    focusedBorderColor = GoldAccent,
                    unfocusedBorderColor = NoirBorder,
                    focusedContainerColor = NoirCard,
                    unfocusedContainerColor = NoirCard
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("room_code_input")
            )

            // Player Name Input
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "YOUR INVESTIGATOR NAME",
                    color = GoldAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                OutlinedTextField(
                    value = playerName,
                    onValueChange = { playerName = it },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = GoldMuted
                        )
                    },
                    placeholder = { Text("e.g. Detective Holmes", color = DimText) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PaperText,
                        unfocusedTextColor = PaperText,
                        focusedBorderColor = GoldAccent,
                        unfocusedBorderColor = NoirBorder,
                        focusedContainerColor = NoirCard,
                        unfocusedContainerColor = NoirCard
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("join_player_name_input")
                )
            }

            // Info Card
            Card(
                colors = CardDefaults.cardColors(containerColor = NoirCard),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NoirBorder, RoundedCornerShape(8.dp))
            ) {
                Text(
                    text = "You will wait in the lobby until the host locks the roster and initiates case generation. Once generation starts, each investigator will receive a unique character dossier.",
                    color = MutedText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (roomCode.length == 6) {
                        viewModel.joinRoom(roomCode, playerName)
                    }
                },
                enabled = roomCode.length == 6 && playerName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldAccent,
                    contentColor = NoirBackground,
                    disabledContainerColor = NoirCard,
                    disabledContentColor = DimText
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_join_room_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MeetingRoom,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ENTER LOBBY",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
