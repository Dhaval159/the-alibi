package com.example

import com.example.data.GameRepository
import com.example.data.TheAlibiRepository
import com.example.ui.Screen
import com.example.ui.TheAlibiViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TheAlibiViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: GameRepository
    private lateinit var viewModel: TheAlibiViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = TheAlibiRepository()
        viewModel = TheAlibiViewModel(repository = repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `viewModel can be constructed using repository abstraction and factory`() {
        assertNotNull(viewModel)
        val factory = TheAlibiViewModel.Factory(repository)
        val created = factory.create(TheAlibiViewModel::class.java)
        assertNotNull(created)
        assertEquals(repository, created.repository)
    }

    @Test
    fun `room creation works through ViewModel and updates UI state`() {
        assertEquals(Screen.HOME, viewModel.uiState.value.currentScreen)

        viewModel.createRoom("Master Detective")

        assertEquals(Screen.LOBBY, viewModel.uiState.value.currentScreen)
        assertNotNull(viewModel.uiState.value.currentRoom)
        assertEquals(1, viewModel.uiState.value.currentRoom?.playerCount)
        assertTrue(viewModel.uiState.value.isHost)
    }

    @Test
    fun `two repository instances have isolated stores by default`() {
        // First repository creates a room
        viewModel.createRoom("Host Player 1")
        val roomCode1 = viewModel.uiState.value.currentRoom!!.roomCode

        // Create a second repository with its own isolated store
        val isolatedRepo = TheAlibiRepository()
        val isolatedViewModel = TheAlibiViewModel(repository = isolatedRepo)

        // Second repository should NOT see the first room
        assertEquals(Screen.HOME, isolatedViewModel.uiState.value.currentScreen)
        assertNull(isolatedViewModel.uiState.value.currentRoom)

        // Second repository creates its own room
        isolatedViewModel.createRoom("Host Player 2")
        val roomCode2 = isolatedViewModel.uiState.value.currentRoom!!.roomCode

        // Rooms should be different
        assertNotEquals(roomCode1, roomCode2)

        // Each repository should have its own player count of 1
        assertEquals(1, viewModel.uiState.value.currentRoom?.playerCount)
        assertEquals(1, isolatedViewModel.uiState.value.currentRoom?.playerCount)
    }

    @Test
    fun `room joining works through ViewModel with shared local store`() {
        // Shared local store for multi-instance simulation
        val sharedStore = com.example.data.LocalRoomStore()

        val hostRepo = TheAlibiRepository(roomStore = sharedStore)
        val hostViewModel = TheAlibiViewModel(repository = hostRepo)
        hostViewModel.createRoom("Host Player")
        val roomCode = hostViewModel.uiState.value.currentRoom!!.roomCode

        // Second player joins using their own viewModel with explicitly shared store
        val secondPlayerRepo: GameRepository = TheAlibiRepository(roomStore = sharedStore)
        val secondViewModel = TheAlibiViewModel(repository = secondPlayerRepo)

        secondViewModel.joinRoom(roomCode, "Joining Player")

        assertEquals(Screen.LOBBY, secondViewModel.uiState.value.currentScreen)
        assertNotNull(secondViewModel.uiState.value.currentRoom)
        assertEquals(2, secondViewModel.uiState.value.currentRoom?.playerCount)
        assertFalse(secondViewModel.uiState.value.isHost)
    }

    @Test
    fun `readiness changes work through ViewModel`() {
        viewModel.createRoom("Ready Check Host")

        viewModel.togglePlayerReady(false)
        val players = viewModel.uiState.value.currentRoom?.players
        assertFalse(players?.first()?.isReady == true)

        viewModel.togglePlayerReady(true)
        val playersReady = viewModel.uiState.value.currentRoom?.players
        assertTrue(playersReady?.first()?.isReady == true)
    }

    @Test
    fun `repository failures are handled and surface to toast message`() {
        // Attempt to join nonexistent room
        viewModel.joinRoom("999999", "Lost Detective")

        // Should not navigate to lobby
        assertNotEquals(Screen.LOBBY, viewModel.uiState.value.currentScreen)
        // Should produce an error toast
        assertNotNull(viewModel.uiState.value.toastMessage)
        assertTrue(viewModel.uiState.value.toastMessage?.contains("not found") == true)
    }

    @Test
    fun `existing gameplay behavior remains intact`() {
        // Active Case startup - verify case library exists (may be empty or generated)
        val activeCase = viewModel.uiState.value.activeCase
        // Case may be null initially; if present, verify it's valid
        assertTrue(activeCase == null || activeCase.identity.caseId.isNotBlank())

        // Navigation
        viewModel.navigateTo(Screen.CASE_LIBRARY)
        assertEquals(Screen.CASE_LIBRARY, viewModel.uiState.value.currentScreen)

        // Dossier conceal toggle
        assertFalse(viewModel.uiState.value.isDossierConcealed)
        viewModel.toggleConcealDossier()
        assertTrue(viewModel.uiState.value.isDossierConcealed)

        // Hotspot selection - use first available hotspot or crime scene hotspot
        val activeCase2 = viewModel.uiState.value.activeCase
        val hotspot = if (activeCase2 != null && activeCase2.crimeScene?.hotspots?.isNotEmpty() == true) {
            activeCase2.crimeScene?.hotspots?.firstOrNull()
        } else null
        viewModel.selectHotspot(hotspot)
        assertTrue(viewModel.uiState.value.inspectedHotspotIds.isNotEmpty() || hotspot == null)
    }
}

