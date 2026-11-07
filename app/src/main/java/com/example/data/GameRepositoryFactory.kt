package com.example.data

import com.example.model.GameMode

/**
 * Factory providing explicit selection of the repository implementation.
 *
 * Prevents silent fallback between online and offline modes.
 */
object GameRepositoryFactory {

    private var defaultMode: GameMode = GameMode.OFFLINE_DEMO

    fun setDefaultMode(mode: GameMode) {
        defaultMode = mode
    }

    fun getDefaultMode(): GameMode = defaultMode

    fun createRepository(mode: GameMode = defaultMode): GameRepository {
        return when (mode) {
            GameMode.OFFLINE_DEMO -> TheAlibiRepository()
            GameMode.FIREBASE_ONLINE -> UnavailableFirebaseGameRepository()
        }
    }
}

