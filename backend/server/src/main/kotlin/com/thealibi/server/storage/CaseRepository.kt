package com.thealibi.server.storage

import com.thealibi.server.models.*
import com.thealibi.server.validation.CaseValidator
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class CaseRepository(
    private val validator: CaseValidator = CaseValidator()
) {
    private val cases = ConcurrentHashMap<String, CasePackage>()
    private val jobs = ConcurrentHashMap<String, GenerationJobStatus>()

    fun saveCase(casePackage: CasePackage) {
        cases[casePackage.identity.caseId] = casePackage
    }

    fun getCase(caseId: String): CasePackage? = cases[caseId]

    fun getPublicCase(caseId: String): CasePackage? {
        val fullCase = cases[caseId] ?: return null
        return validator.createPublicCase(fullCase)
    }

    fun getAllPublicCases(): List<CasePreview> {
        return cases.values.map { casePkg ->
            CasePreview(
                caseId = casePkg.identity.caseId,
                title = casePkg.identity.title,
                subtitle = casePkg.identity.subtitle,
                setting = casePkg.identity.setting,
                difficulty = casePkg.identity.difficulty,
                estimatedDuration = casePkg.identity.estimatedDurationMinutes,
                victimName = casePkg.victim.name,
                characterCount = casePkg.characters.size,
                evidenceCount = casePkg.evidence.size,
                posterAssetId = casePkg.identity.posterAssetId
            )
        }
    }

    fun deleteCase(caseId: String): Boolean {
        val removed = cases.remove(caseId) != null
        if (removed) {
            val dir = File("generated_cases", caseId)
            if (dir.exists()) dir.deleteRecursively()
        }
        return removed
    }

    fun saveJob(job: GenerationJobStatus) {
        jobs[job.jobId] = job
    }

    fun getJob(jobId: String): GenerationJobStatus? = jobs[jobId]

    fun getSecretSolution(caseId: String): SecretSolution? {
        return cases[caseId]?.secretSolution
    }
}
