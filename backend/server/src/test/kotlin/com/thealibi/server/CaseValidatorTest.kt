package com.thealibi.server

import com.thealibi.server.gemini.CaseGenerationEngine
import com.thealibi.server.gemini.GeminiClient
import com.thealibi.server.models.CaseGenerationRequest
import com.thealibi.server.validation.CaseValidator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CaseValidatorTest {

    private val validator = CaseValidator()
    private val engine = CaseGenerationEngine(GeminiClient("fake_key"), validator)

    @Test
    fun `master mystery case passes all deterministic validations`() {
        val masterCase = engine.generateProceduralMasterCase(
            CaseGenerationRequest(theme = "Mansion inheritance", difficulty = "Hard", playerCount = 6)
        )
        val result = validator.validate(masterCase)
        assertTrue(result.isValid, "Master mystery should be valid: ${result.errors}")
        assertEquals(0, result.errors.size)
    }

    @Test
    fun `rejects case with duplicate character IDs`() {
        val masterCase = engine.generateProceduralMasterCase(CaseGenerationRequest())
        val duplicateChars = masterCase.characters + masterCase.characters.first()
        val broken = masterCase.copy(characters = duplicateChars)
        val result = validator.validate(broken)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Duplicate character IDs") })
    }

    @Test
    fun `rejects case where culprit is marked as an NPC in player count config`() {
        val masterCase = engine.generateProceduralMasterCase(CaseGenerationRequest())
        val culpritId = masterCase.secretSolution!!.culpritId
        val brokenConfig = masterCase.playerCountConfigs.toMutableMap()
        val cfg4 = brokenConfig[4]!!
        brokenConfig[4] = cfg4.copy(
            assignedHumanCharacterIds = cfg4.assignedHumanCharacterIds.filter { it != culpritId } + "char_dr_julian_finch",
            npcCharacterIds = cfg4.npcCharacterIds + culpritId
        )
        val broken = masterCase.copy(playerCountConfigs = brokenConfig)
        val result = validator.validate(broken)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Culprit") && it.contains("NPC") })
    }

    @Test
    fun `rejects case with insufficient deduction paths`() {
        val masterCase = engine.generateProceduralMasterCase(CaseGenerationRequest())
        val broken = masterCase.copy(deductionPaths = masterCase.deductionPaths.take(2))
        val result = validator.validate(broken)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Deduction coverage insufficient") })
    }

    @Test
    fun `public case sanitization strips secret solution and private secrets`() {
        val masterCase = engine.generateProceduralMasterCase(CaseGenerationRequest())
        val publicCase = validator.createPublicCase(masterCase)
        assertNull(publicCase.secretSolution, "Secret solution must be stripped in public case")
        for (char in publicCase.characters) {
            assertEquals("", char.privateSecret, "Character private secret must be empty in public case")
            assertEquals("", char.actualActions, "Actual actions must be empty in public case")
        }
    }
}
