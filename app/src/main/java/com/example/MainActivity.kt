package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.Screen
import com.example.ui.TheAlibiUiState
import com.example.ui.TheAlibiViewModel
import com.example.ui.screens.*
import com.example.ui.theme.*

import com.example.data.GameRepositoryFactory
import com.example.data.firebase.FirebaseAuthRepository
import com.example.model.GameMode
import com.example.ui.screens.GoogleSignInScreen

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: TheAlibiViewModel by viewModels {
        TheAlibiViewModel.Factory(
            repository = GameRepositoryFactory.createRepository(GameMode.OFFLINE_DEMO)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val authRepo = FirebaseAuthRepository(this)

        // Asynchronously sign in anonymously in the background.
        // If an authenticated session already exists, it is reused immediately.
        lifecycleScope.launch {
            authRepo.signInAnonymously()
        }

        setContent {
            TheAlibiTheme {
                val authIdentity by authRepo.authIdentity.collectAsStateWithLifecycle()

                // Propagate authenticated identity to repository if available
                LaunchedEffect(authIdentity) {
                    authIdentity?.let { identity ->
                        val repo = viewModel.repository
                        if (repo is com.example.data.TheAlibiRepository) {
                            repo.setAuthIdentity(identity)
                        } else if (repo is com.example.data.UnavailableFirebaseGameRepository) {
                            repo.setAuthIdentity(identity)
                        }
                    }
                }

                TheAlibiApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun TheAlibiApp(viewModel: TheAlibiViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // BackHandler based on current screen
    BackHandler(enabled = uiState.currentScreen != Screen.HOME) {
        when (uiState.currentScreen) {
            Screen.CREATE_ROOM -> viewModel.navigateTo(Screen.HOME)
            Screen.JOIN_ROOM -> viewModel.navigateTo(Screen.HOME)
            Screen.LOBBY -> viewModel.leaveRoom()
            Screen.CASE_GENERATOR -> viewModel.navigateTo(Screen.HOME)
            Screen.GENERATION_PROGRESS -> viewModel.cancelGeneration()
            Screen.GENERATED_PREVIEW -> viewModel.navigateTo(Screen.HOME)
            Screen.CASE_LIBRARY -> viewModel.navigateTo(Screen.HOME)
            Screen.CASE_OPENING -> viewModel.navigateTo(Screen.HOME)
            Screen.CHARACTER_REVEAL -> viewModel.navigateTo(Screen.CASE_OPENING)
            Screen.INTERROGATION -> viewModel.navigateTo(Screen.SUSPECT_GALLERY)
            Screen.OBJECTIVE_CENTER -> viewModel.navigateTo(Screen.INTERACTIVE_SCENE)
            Screen.PRIVATE_CHAT -> viewModel.navigateTo(Screen.INTERACTIVE_SCENE)
            Screen.DEBATE -> viewModel.navigateTo(Screen.INTERACTIVE_SCENE)
            Screen.INTERACTIVE_SCENE,
            Screen.EVIDENCE_GALLERY,
            Screen.SUSPECT_GALLERY,
            Screen.INVESTIGATION_BOARD,
            Screen.TIMELINE,
            Screen.ACCUSATION -> viewModel.navigateTo(Screen.HOME)
            Screen.RESOLUTION -> viewModel.navigateTo(Screen.HOME)
            Screen.HOME -> Unit
        }
    }

    val isInvestigationMode = uiState.currentScreen in listOf(
        Screen.INTERACTIVE_SCENE,
        Screen.EVIDENCE_GALLERY,
        Screen.SUSPECT_GALLERY,
        Screen.INTERROGATION,
        Screen.INVESTIGATION_BOARD,
        Screen.TIMELINE,
        Screen.OBJECTIVE_CENTER,
        Screen.PRIVATE_CHAT,
        Screen.DEBATE,
        Screen.ACCUSATION
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(NoirBackground),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            if (isInvestigationMode && uiState.currentScreen != Screen.PRIVATE_CHAT && uiState.currentScreen != Screen.OBJECTIVE_CENTER && uiState.currentScreen != Screen.DEBATE) {
                InvestigationTopBar(
                    uiState = uiState,
                    onHomeClick = { viewModel.navigateTo(Screen.HOME) },
                    onDossierClick = { viewModel.navigateTo(Screen.CHARACTER_REVEAL) },
                    onObjectivesClick = { viewModel.navigateTo(Screen.OBJECTIVE_CENTER) },
                    onAudioClick = { viewModel.toggleSoundSettingsDialog(true) }
                )
            }
        },
        bottomBar = {
            if (isInvestigationMode &&
                uiState.currentScreen != Screen.INTERROGATION &&
                uiState.currentScreen != Screen.PRIVATE_CHAT &&
                uiState.currentScreen != Screen.OBJECTIVE_CENTER &&
                uiState.currentScreen != Screen.DEBATE
            ) {
                InvestigationBottomNavBar(
                    currentScreen = uiState.currentScreen,
                    onNavigate = { viewModel.navigateTo(it) },
                    evidenceCount = uiState.discoveredEvidenceIds.size
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(NoirBackground)
        ) {
            when (uiState.currentScreen) {
                Screen.HOME -> HomeScreen(uiState = uiState, viewModel = viewModel)
                Screen.CREATE_ROOM -> CreateRoomScreen(uiState = uiState, viewModel = viewModel)
                Screen.JOIN_ROOM -> JoinRoomScreen(uiState = uiState, viewModel = viewModel)
                Screen.LOBBY -> LobbyScreen(uiState = uiState, viewModel = viewModel)
                Screen.CASE_GENERATOR -> CaseGeneratorScreen(uiState = uiState, viewModel = viewModel)
                Screen.GENERATION_PROGRESS -> CaseGenerationProgressScreen(uiState = uiState, viewModel = viewModel)
                Screen.GENERATED_PREVIEW -> GeneratedCasePreviewScreen(uiState = uiState, viewModel = viewModel)
                Screen.CASE_LIBRARY -> CaseLibraryScreen(uiState = uiState, viewModel = viewModel)
                Screen.CASE_OPENING -> CaseOpeningScreen(uiState = uiState, viewModel = viewModel)
                Screen.CHARACTER_REVEAL -> CharacterRevealScreen(uiState = uiState, viewModel = viewModel)
                Screen.INTERACTIVE_SCENE -> InteractiveSceneScreen(uiState = uiState, viewModel = viewModel)
                Screen.EVIDENCE_GALLERY -> EvidenceGalleryScreen(uiState = uiState, viewModel = viewModel)
                Screen.SUSPECT_GALLERY -> SuspectGalleryScreen(uiState = uiState, viewModel = viewModel)
                Screen.INTERROGATION -> InterrogationScreen(uiState = uiState, viewModel = viewModel)
                Screen.INVESTIGATION_BOARD -> InvestigationBoardScreen(uiState = uiState, viewModel = viewModel)
                Screen.TIMELINE -> TimelineScreen(uiState = uiState, viewModel = viewModel)
                Screen.OBJECTIVE_CENTER -> ObjectiveCenterScreen(uiState = uiState, viewModel = viewModel)
                Screen.PRIVATE_CHAT -> PrivateChatScreen(uiState = uiState, viewModel = viewModel)
                Screen.DEBATE -> DebateScreen(uiState = uiState, viewModel = viewModel)
                Screen.ACCUSATION -> AccusationScreen(uiState = uiState, viewModel = viewModel)
                Screen.RESOLUTION -> ResolutionScreen(uiState = uiState, viewModel = viewModel)
            }

            // Mid-Game Twist Overlay
            if (uiState.showTwistDialog && uiState.activeTwist != null) {
                MidGameTwistDialog(
                    twist = uiState.activeTwist!!,
                    onDismiss = { viewModel.dismissTwistDialog() }
                )
            }

            // Audio & Immersion Settings Dialog
            if (uiState.soundSettingsOpen) {
                AudioSettingsDialog(
                    uiState = uiState,
                    onToggleMusic = { viewModel.toggleMusic() },
                    onToggleSfx = { viewModel.toggleSfx() },
                    onToggleNarration = { viewModel.toggleNarration() },
                    onDismiss = { viewModel.toggleSoundSettingsDialog(false) }
                )
            }
        }
    }
}

@Composable
fun InvestigationTopBar(
    uiState: TheAlibiUiState,
    onHomeClick: () -> Unit,
    onDossierClick: () -> Unit,
    onObjectivesClick: () -> Unit,
    onAudioClick: () -> Unit
) {
    Surface(
        color = NoirCard,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NoirBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onHomeClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(imageVector = Icons.Default.Home, contentDescription = "Home", tint = GoldAccent)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = uiState.activeCase?.identity?.title ?: "THE ALIBI",
                    color = PaperText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    maxLines = 1
                )
                Text(
                    text = "Role: ${uiState.assignedCharacter?.name ?: "Investigator"}",
                    color = GoldLight,
                    fontSize = 11.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Audio Toggle
                IconButton(
                    onClick = onAudioClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.isSfxEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = "Audio Settings",
                        tint = GoldLight
                    )
                }

                // Objectives
                IconButton(
                    onClick = onObjectivesClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Checklist, contentDescription = "Objectives", tint = GoldLight)
                }

                // Secret Dossier
                Surface(
                    color = NoirSecondary,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .border(1.dp, GoldMuted, RoundedCornerShape(8.dp))
                        .testTag("secret_dossier_tab_button")
                ) {
                    IconButton(
                        onClick = onDossierClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Badge, contentDescription = "Dossier", tint = GoldAccent)
                    }
                }
            }
        }
    }
}

@Composable
fun InvestigationBottomNavBar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    evidenceCount: Int
) {
    NavigationBar(
        containerColor = NoirCard,
        contentColor = PaperText,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NoirBorder)
            .navigationBarsPadding()
    ) {
        InvestigationNavItem(
            screen = Screen.INTERACTIVE_SCENE,
            currentScreen = currentScreen,
            icon = Icons.Default.LocationOn,
            label = "Scene",
            onNavigate = onNavigate
        )
        InvestigationNavItem(
            screen = Screen.EVIDENCE_GALLERY,
            currentScreen = currentScreen,
            icon = Icons.Default.Folder,
            label = "Clues ($evidenceCount)",
            onNavigate = onNavigate
        )
        InvestigationNavItem(
            screen = Screen.SUSPECT_GALLERY,
            currentScreen = currentScreen,
            icon = Icons.Default.People,
            label = "Suspects",
            onNavigate = onNavigate
        )
        InvestigationNavItem(
            screen = Screen.INVESTIGATION_BOARD,
            currentScreen = currentScreen,
            icon = Icons.Default.Dashboard,
            label = "Board",
            onNavigate = onNavigate
        )
        InvestigationNavItem(
            screen = Screen.DEBATE,
            currentScreen = currentScreen,
            icon = Icons.Default.Forum,
            label = "Debate",
            onNavigate = onNavigate
        )
        InvestigationNavItem(
            screen = Screen.ACCUSATION,
            currentScreen = currentScreen,
            icon = Icons.Default.Gavel,
            label = "Accuse",
            onNavigate = onNavigate
        )
    }
}

@Composable
fun RowScope.InvestigationNavItem(
    screen: Screen,
    currentScreen: Screen,
    icon: ImageVector,
    label: String,
    onNavigate: (Screen) -> Unit
) {
    val selected = currentScreen == screen
    NavigationBarItem(
        selected = selected,
        onClick = { onNavigate(screen) },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp)
            )
        },
        label = {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = NoirBackground,
            selectedTextColor = GoldAccent,
            indicatorColor = GoldAccent,
            unselectedIconColor = MutedText,
            unselectedTextColor = DimText
        )
    )
}

@Composable
fun AudioSettingsDialog(
    uiState: TheAlibiUiState,
    onToggleMusic: () -> Unit,
    onToggleSfx: () -> Unit,
    onToggleNarration: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "AUDIO & IMMERSION",
                color = GoldAccent,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Configure noir soundscape and atmospheric acoustic cues.",
                    color = MutedText,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                AudioToggleRow(
                    title = "Sound Effects & Audio Cues",
                    description = "Chimes for clue discoveries, secret reveals, and accusation gongs",
                    isEnabled = uiState.isSfxEnabled,
                    onToggle = onToggleSfx
                )

                Spacer(modifier = Modifier.height(10.dp))

                AudioToggleRow(
                    title = "Cinematic Ambient Music",
                    description = "Subtle gramophone noir background chords",
                    isEnabled = uiState.isMusicEnabled,
                    onToggle = onToggleMusic
                )

                Spacer(modifier = Modifier.height(10.dp))

                AudioToggleRow(
                    title = "Case Narration Audio",
                    description = "Spoken detective intros and mid-game alerts",
                    isEnabled = uiState.isNarrationEnabled,
                    onToggle = onToggleNarration
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = NoirBackground)
            ) {
                Text("DONE", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = NoirCard
    )
}

@Composable
fun AudioToggleRow(
    title: String,
    description: String,
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        color = NoirSecondary,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = PaperText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(text = description, color = MutedText, fontSize = 10.sp)
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NoirBackground,
                    checkedTrackColor = GoldAccent,
                    uncheckedThumbColor = DimText,
                    uncheckedTrackColor = NoirCard
                )
            )
        }
    }
}
