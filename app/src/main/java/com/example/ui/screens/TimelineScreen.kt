package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TimelineEvent
import com.example.ui.TheAlibiUiState
import com.example.ui.TheAlibiViewModel
import com.example.ui.theme.*

@Composable
fun TimelineScreen(
    uiState: TheAlibiUiState,
    viewModel: TheAlibiViewModel,
    modifier: Modifier = Modifier
) {
    val casePkg = uiState.activeCase ?: return
    val events = casePkg.timeline.filter { !it.isHiddenSequence }

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
                    text = "CHRONOLOGY OF EVENTS",
                    color = GoldAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Incident Timeline",
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
                    text = "${events.size} Logged Events",
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
            items(events, key = { it.id }) { ev ->
                TimelineCardItem(event = ev, casePkg = casePkg)
            }
        }
    }
}

@Composable
fun TimelineCardItem(
    event: TimelineEvent,
    casePkg: com.example.model.CasePackage
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NoirCard),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NoirBorder, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Time Badge
            Surface(
                color = GoldAccent.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.border(1.dp, GoldMuted, RoundedCornerShape(8.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, tint = GoldLight, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = event.timestamp, color = GoldLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    color = PaperText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.description,
                    color = MutedText,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                if (event.characterIds.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val names = event.characterIds.mapNotNull { id ->
                        casePkg.characters.find { it.id == id }?.name
                    }
                    Text(
                        text = "Linked Persons: ${names.joinToString(", ")}",
                        color = GoldMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
