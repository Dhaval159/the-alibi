package com.thealibi.server.validation

import com.thealibi.server.models.CasePackage

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

class CaseValidator {

    fun validate(casePackage: CasePackage): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // 1. Identity validation
        if (casePackage.identity.caseId.isBlank()) errors.add("Case ID must not be blank")
        if (casePackage.identity.title.isBlank()) errors.add("Case title must not be blank")

        // 2. Character validation
        val characterIds = casePackage.characters.map { it.id }.toSet()
        if (characterIds.size != casePackage.characters.size) {
            errors.add("Duplicate character IDs detected")
        }
        if (casePackage.characters.size < 4) {
            errors.add("Case must have at least 4 characters (found ${casePackage.characters.size})")
        }

        // 3. Evidence validation
        val evidenceIds = casePackage.evidence.map { it.id }.toSet()
        if (evidenceIds.size != casePackage.evidence.size) {
            errors.add("Duplicate evidence IDs detected")
        }
        if (casePackage.evidence.size < 6) {
            errors.add("Case must have at least 6 evidence items (found ${casePackage.evidence.size})")
        }

        // Verify evidence references valid character IDs
        for (item in casePackage.evidence) {
            for (charId in item.linkedCharacterIds) {
                if (charId !in characterIds) {
                    errors.add("Evidence '${item.id}' references unknown character '$charId'")
                }
            }
        }

        // 4. Secret Solution validation
        val solution = casePackage.secretSolution
        if (solution == null) {
            errors.add("Secret solution is missing")
        } else {
            if (solution.culpritId !in characterIds) {
                errors.add("Culprit ID '${solution.culpritId}' does not match any character")
            }
            if (solution.motive.isBlank()) {
                errors.add("Culprit motive is missing")
            }
            if (solution.opportunity.isBlank()) {
                errors.add("Culprit opportunity is missing")
            }
            if (solution.smokingGunEvidenceIds.isEmpty()) {
                errors.add("Solution must have at least one smoking gun evidence item")
            } else {
                for (gunId in solution.smokingGunEvidenceIds) {
                    if (gunId !in evidenceIds) {
                        errors.add("Smoking gun evidence '$gunId' does not exist in evidence roster")
                    }
                }
            }
        }

        // 5. Adaptive Player Count Configuration validation
        if (casePackage.playerCountConfigs.isEmpty()) {
            errors.add("Player count configurations are missing")
        } else {
            for ((count, config) in casePackage.playerCountConfigs) {
                if (config.assignedHumanCharacterIds.size != count) {
                    errors.add("Player count config for $count players has ${config.assignedHumanCharacterIds.size} human characters assigned")
                }
                // Culprit must be assigned to a human player!
                if (solution != null && solution.culpritId !in config.assignedHumanCharacterIds) {
                    errors.add("Player count $count: Culprit '${solution.culpritId}' is not assigned to a human player")
                }
                // No NPC culprit
                if (solution != null && solution.culpritId in config.npcCharacterIds) {
                    errors.add("Player count $count: Culprit '${solution.culpritId}' is marked as an NPC")
                }
                // All assigned IDs must be valid
                for (id in config.assignedHumanCharacterIds + config.npcCharacterIds) {
                    if (id !in characterIds) {
                        errors.add("Player count $count references invalid character '$id'")
                    }
                }
            }
        }

        // 6. Deduction Coverage Validation (At least 3 distinct deduction paths)
        if (casePackage.deductionPaths.size < 3) {
            errors.add("Deduction coverage insufficient: must have at least 3 distinct deduction paths (found ${casePackage.deductionPaths.size})")
        } else {
            for (path in casePackage.deductionPaths) {
                if (path.requiredEvidenceIds.isEmpty()) {
                    errors.add("Deduction path '${path.id}' has no required evidence")
                }
                for (evId in path.requiredEvidenceIds) {
                    if (evId !in evidenceIds) {
                        errors.add("Deduction path '${path.id}' references non-existent evidence '$evId'")
                    }
                }
            }
        }

        // 7. Crime scene hotspots validation
        for (hotspot in casePackage.crimeScene.hotspots) {
            if (hotspot.associatedEvidenceId != null && hotspot.associatedEvidenceId !in evidenceIds) {
                warnings.add("Scene hotspot '${hotspot.id}' references unknown evidence '${hotspot.associatedEvidenceId}'")
            }
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    fun validateForPlayerCount(casePackage: CasePackage, exactPlayerCount: Int): ValidationResult {
        val baseResult = validate(casePackage)
        val errors = baseResult.errors.toMutableList()
        val warnings = baseResult.warnings.toMutableList()

        val config = casePackage.playerCountConfigs[exactPlayerCount]
        if (config == null) {
            errors.add("No configuration found for exact player count $exactPlayerCount")
        } else {
            if (config.assignedHumanCharacterIds.size != exactPlayerCount) {
                errors.add("Expected $exactPlayerCount human characters, but found ${config.assignedHumanCharacterIds.size}")
            }
            val solution = casePackage.secretSolution
            if (solution != null && solution.culpritId !in config.assignedHumanCharacterIds) {
                errors.add("Culprit '${solution.culpritId}' is not assigned to a human player for $exactPlayerCount players")
            }
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    /**
     * Sanitizes a CasePackage for public API transmission.
     * Strips secretSolution and any other player's private secrets.
     */
    fun createPublicCase(casePackage: CasePackage): CasePackage {
        val sanitizedCharacters = casePackage.characters.map { char ->
            char.copy(
                privateSecret = "",
                personalAlibi = "",
                actualActions = "",
                knownInformation = "",
                hiddenInformation = "",
                possibleContradictions = emptyList(),
                personalObjective = ""
            )
        }
        return casePackage.copy(
            characters = sanitizedCharacters,
            secretSolution = null // Never sent over public endpoints!
        )
    }
}
