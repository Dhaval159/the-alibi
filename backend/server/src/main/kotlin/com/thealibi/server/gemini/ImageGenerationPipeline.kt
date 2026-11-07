package com.thealibi.server.gemini

import com.thealibi.server.models.CasePackage
import org.slf4j.LoggerFactory
import java.io.File

class ImageGenerationPipeline(
    private val geminiClient: GeminiClient,
    private val baseStorageDir: File = File("generated_cases")
) {
    private val logger = LoggerFactory.getLogger(ImageGenerationPipeline::class.java)

    data class AssetGenerationProgress(
        val stage: String,
        val completedCount: Int,
        val totalCount: Int
    )

    suspend fun generateCaseAssets(
        casePackage: CasePackage,
        onProgress: (AssetGenerationProgress) -> Unit = {}
    ): CasePackage {
        val caseId = casePackage.identity.caseId
        val caseFolder = File(baseStorageDir, caseId).apply { mkdirs() }
        val posterFolder = File(caseFolder, "poster").apply { mkdirs() }
        val charFolder = File(caseFolder, "characters").apply { mkdirs() }
        val sceneFolder = File(caseFolder, "locations").apply { mkdirs() }
        val evidenceFolder = File(caseFolder, "evidence").apply { mkdirs() }

        val totalAssets = 1 + casePackage.characters.size + 1 + casePackage.evidence.take(4).size
        var completed = 0

        val caseStyleGuide = "Cinematic noir detective thriller, dramatic low-key lighting, 1920s gothic atmosphere, oil painting richness, deep shadows, gold and charcoal palette"

        // 1. Case Poster
        onProgress(AssetGenerationProgress("Generating cinematic case poster", completed, totalAssets))
        val posterPrompt = "Case poster: ${casePackage.identity.title}, ${casePackage.identity.setting}, $caseStyleGuide, atmospheric"
        val posterBytes = geminiClient.generateImage(posterPrompt)
        if (posterBytes != null) {
            File(posterFolder, "poster.jpg").writeBytes(posterBytes)
        }
        completed++

        // 2. Character Portraits
        val updatedCharacters = casePackage.characters.map { char ->
            onProgress(AssetGenerationProgress("Generating portrait for ${char.name}", completed, totalAssets))
            val charPrompt = "Detective casefile photograph portrait of ${char.name}, age ${char.age}, ${char.occupation}, personality: ${char.personality}, $caseStyleGuide, professional studio headshot"
            val charBytes = geminiClient.generateImage(charPrompt)
            val assetId = if (charBytes != null) {
                val file = File(charFolder, "${char.id}.jpg")
                file.writeBytes(charBytes)
                file.name
            } else {
                null
            }
            completed++
            char.copy(portraitAssetId = assetId, portraitPrompt = charPrompt)
        }

        // 3. Crime Scene
        onProgress(AssetGenerationProgress("Illustrating crime scene", completed, totalAssets))
        val scenePrompt = "Crime scene investigation: ${casePackage.crimeScene.roomName}, ${casePackage.crimeScene.description}, $caseStyleGuide, wide interior view"
        val sceneBytes = geminiClient.generateImage(scenePrompt)
        val sceneAssetId = if (sceneBytes != null) {
            val file = File(sceneFolder, "crime_scene.jpg")
            file.writeBytes(sceneBytes)
            file.name
        } else {
            null
        }
        completed++

        // 4. Key Evidence Items (top 4 critical items)
        val updatedEvidence = casePackage.evidence.mapIndexed { index, ev ->
            if (index < 4) {
                onProgress(AssetGenerationProgress("Generating evidence asset: ${ev.name}", completed, totalAssets))
                val evPrompt = "Detective evidence photograph: ${ev.name}, ${ev.detailedInspection}, macro forensic focus on dark wooden table, $caseStyleGuide"
                val evBytes = geminiClient.generateImage(evPrompt)
                val evAssetId = if (evBytes != null) {
                    val file = File(evidenceFolder, "${ev.id}.jpg")
                    file.writeBytes(evBytes)
                    file.name
                } else null
                completed++
                ev.copy(assetId = evAssetId, imagePrompt = evPrompt)
            } else {
                ev
            }
        }

        onProgress(AssetGenerationProgress("Case assets generation completed", completed, totalAssets))

        return casePackage.copy(
            identity = casePackage.identity.copy(posterAssetId = "poster.jpg"),
            characters = updatedCharacters,
            crimeScene = casePackage.crimeScene.copy(sceneAssetId = sceneAssetId),
            evidence = updatedEvidence
        )
    }

    fun getAssetFile(caseId: String, assetId: String): File? {
        val caseFolder = File(baseStorageDir, caseId)
        val possibleFolders = listOf("poster", "characters", "locations", "evidence")
        for (folder in possibleFolders) {
            val file = File(File(caseFolder, folder), assetId)
            if (file.exists()) return file
        }
        return null
    }
}
