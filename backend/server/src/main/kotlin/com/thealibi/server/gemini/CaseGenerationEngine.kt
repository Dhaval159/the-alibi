package com.thealibi.server.gemini

import com.thealibi.server.models.*
import com.thealibi.server.validation.CaseValidator
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.UUID

class CaseGenerationEngine(
    private val geminiClient: GeminiClient,
    private val validator: CaseValidator = CaseValidator()
) {
    private val logger = LoggerFactory.getLogger(CaseGenerationEngine::class.java)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    suspend fun generateMysteryCase(
        request: CaseGenerationRequest,
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): CasePackage {
        onProgress(10, "Designing the mystery setting and victim profile...")

        val systemInstruction = """
            You are the master mystery architect for THE ALIBI, a premier social deduction and detective investigation game.
            You produce complete, playable, airtight detective mysteries with rich lore, complex character webs, and logical deduction chains.
            Every mystery must satisfy:
            1. Exactly ONE human character is the designated culprit.
            2. Culprit must have Motive, Opportunity, and Means.
            3. Multiple independent clues (at least 3 distinct deduction paths) prove the culprit's guilt.
            4. Innocent suspects must have believable secrets, alibis, and mild contradictions that can be cleared with proper investigation.
            5. Crime scene must include inspectable hotspots.
            6. All character IDs, evidence IDs, and timestamps must be unique and consistent.
            7. Return clean, strictly formatted JSON matching the CasePackage schema.
        """.trimIndent()

        val prompt = """
            Generate a full playable detective mystery case.
            Theme: ${request.theme}
            Difficulty: ${request.difficulty}
            Target Player Count: ${request.playerCount} (must support configurations from 4 to 10 players)
            Atmosphere: ${request.atmosphere}
            Custom Directives: ${request.customPrompt.ifBlank { "Create an intricate and memorable case." }}

            Ensure:
            - 8 to 10 characters total
            - 10 to 14 evidence items
            - A chronological timeline (discovered and hidden)
            - A secretSolution containing the culpritId, motive, opportunity, method, and explanation of all contradictions.
            - playerCountConfigs for player counts 4, 5, 6, 7, 8, 9, 10 where the culprit is ALWAYS among the assigned human characters (never an NPC).
            - 3 to 5 deduction paths.
            - Return JSON matching the CasePackage model.
        """.trimIndent()

        onProgress(30, "Consulting Gemini mystery architect for structured case data...")

        var rawJson: String
        try {
            rawJson = geminiClient.generateStructuredText(
                systemInstruction = systemInstruction,
                prompt = prompt
            )
        } catch (e: Exception) {
            logger.warn("Gemini API call failed, generating procedural master mystery fallback: ${e.message}")
            return generateProceduralMasterCase(request)
        }

        onProgress(50, "Parsing and structuring mystery evidence, timeline, and suspects...")

        var casePackage = try {
            json.decodeFromString<CasePackage>(rawJson)
        } catch (e: Exception) {
            logger.warn("Direct parse error: ${e.message}. Attempting targeted repair...")
            repairWithGemini(rawJson, listOf("JSON parse error: ${e.message}"))
                ?: generateProceduralMasterCase(request)
        }

        onProgress(65, "Checking deduction logic and consistency with deterministic validator...")

        var validation = validator.validate(casePackage)
        var repairAttempts = 0
        while (!validation.isValid && repairAttempts < 2) {
            repairAttempts++
            onProgress(70, "Refining mystery contradictions (Repair pass $repairAttempts)...")
            logger.info("Validation errors found: ${validation.errors}. Sending repair request...")
            val repaired = repairWithGemini(json.encodeToString(CasePackage.serializer(), casePackage), validation.errors)
            if (repaired != null) {
                casePackage = repaired
                validation = validator.validate(casePackage)
            } else {
                break
            }
        }

        if (!validation.isValid) {
            logger.warn("Validation still failed after repairs: ${validation.errors}. Using verified master mystery.")
            casePackage = generateProceduralMasterCase(request)
        }

        onProgress(85, "Finalizing mystery dossier and player count configurations...")
        return casePackage
    }

    private suspend fun repairWithGemini(
        brokenJson: String,
        errors: List<String>
    ): CasePackage? {
        val repairPrompt = """
            The following CasePackage JSON has validation errors:
            ${errors.joinToString("\n- ")}

            Please fix these exact errors and return the corrected, valid JSON:
            $brokenJson
        """.trimIndent()

        return try {
            val response = geminiClient.generateStructuredText(
                systemInstruction = "You are a JSON schema and mystery repair engine. Output only the corrected JSON.",
                prompt = repairPrompt
            )
            json.decodeFromString<CasePackage>(response)
        } catch (e: Exception) {
            logger.warn("Repair failed: ${e.message}")
            null
        }
    }

    /**
     * High-fidelity, verified, procedural mystery builder guaranteed to pass 100% of deterministic validation checks.
     */
    fun generateProceduralMasterCase(request: CaseGenerationRequest): CasePackage {
        val caseId = "case_${UUID.randomUUID().toString().take(8)}"
        val victim = VictimProfile(
            name = "Lord Reginald Blackwood",
            age = 62,
            occupation = "Patriarch & Industrialist",
            background = "Magnate of Blackwood Mining & Antiquities, rumored to be rewriting his will.",
            causeOfDeath = "Poisoning by rare belladonna alkaloid in vintage scotch",
            locationFound = "Private library study, seated at mahogany desk"
        )

        val characters = listOf(
            CharacterProfile(
                id = "char_edward_vance",
                name = "Edward Vance",
                age = 38,
                occupation = "Estranged Nephew & Antiquities Dealer",
                publicBio = "Nephew of Lord Blackwood who recently returned from Europe under heavy gambling debts.",
                personality = "Charming, nervous when pressed, meticulous dresser",
                publicMotive = "Stands to inherit Blackwood Manor under the original will, but was being disinherited.",
                relationshipWithVictim = "Nephew and debtor",
                privateSecret = "Secretly snuck into the library at 23:15 to steal the newly drafted will and substituted the poison decanter.",
                personalAlibi = "Claims he was walking in the south conservatory smoking a cigar.",
                claimedLocation = "South Conservatory",
                actualActions = "Entered study via secret garden corridor, laced Lord Blackwood's private vintage scotch with belladonna, took the new will draft.",
                knownInformation = "Saw Dr. Finch arguing with Lord Blackwood earlier at 21:00.",
                hiddenInformation = "Still has the brass key to the study garden door in his silver cigarette case.",
                possibleContradictions = listOf("Claims he was in the conservatory, but mud matching the south terrace was found on his patent leather shoes."),
                personalObjective = "Conceal the stolen will draft and shift suspicion onto Dr. Finch or Beatrice.",
                interrogationResponses = mapOf(
                    "Motive" to listOf(
                        InterrogationResponse(
                            question = "Why did you return to Blackwood Manor so suddenly?",
                            answer = "My uncle summoned me to discuss our antiquities investments. We had our disagreements, but I held him in the highest regard."
                        )
                    ),
                    "Opportunity" to listOf(
                        InterrogationResponse(
                            question = "Where were you between 23:00 and 23:45?",
                            answer = "I was strolling through the conservatory. The night air calms my nerves. I met nobody."
                        )
                    ),
                    "Evidence confrontation" to listOf(
                        InterrogationResponse(
                            question = "We found red clay from the study terrace on your shoes, Mr. Vance.",
                            answer = "Ah... well, I might have stepped near the terrace doors briefly when listening to the storm, but I never crossed the threshold!",
                            requiredEvidenceToTrigger = "ev_muddy_footprints",
                            revealsContradiction = true
                        )
                    )
                )
            ),
            CharacterProfile(
                id = "char_beatrice_thorne",
                name = "Lady Beatrice Thorne",
                age = 54,
                occupation = "Sister-in-Law & Manor Administrator",
                publicBio = "Oversees the estate affairs and charitable foundations funded by Lord Blackwood.",
                personality = "Stern, regal, uncompromising",
                publicMotive = "Resented Reginald's financial audits into the charity accounts.",
                relationshipWithVictim = "Sister-in-law",
                privateSecret = "Had embezzled 10,000 pounds from the estate trust to pay her brother's debts.",
                personalAlibi = "Reviewing estate ledgers in the drawing room.",
                claimedLocation = "Drawing Room",
                actualActions = "Burned accounting receipts in the drawing room hearth at 22:30.",
                knownInformation = "Saw Edward pacing outside the study corridor at 23:10.",
                hiddenInformation = "Left a monogrammed handkerchief in the drawing room.",
                possibleContradictions = listOf("Claims she never left the drawing room, but the butler saw her in the wine cellar at 22:00."),
                personalObjective = "Protect the charity ledgers and clear her name of murder."
            ),
            CharacterProfile(
                id = "char_dr_julian_finch",
                name = "Dr. Julian Finch",
                age = 49,
                occupation = "Personal Physician",
                publicBio = "Longtime family doctor who managed Lord Blackwood's failing cardiovascular health.",
                personality = "Analytical, guarded, defensive about medical records",
                publicMotive = "Lord Blackwood threatened to report medical malpractice regarding experimental treatments.",
                relationshipWithVictim = "Physician and confidant",
                privateSecret = "Prescribed heavy digitalis to Blackwood, fearing an inquest into previous dosages.",
                personalAlibi = "In the guest quarters reading medical journals.",
                claimedLocation = "Guest Wing Room 4",
                actualActions = "Visited study at 21:30 to plead with Blackwood, left peacefully.",
                knownInformation = "Noticed the scotch decanter had an unusual bitter almond scent when he visited earlier.",
                hiddenInformation = "Carries a vial of belladonna extract in his medical kit for angina.",
                possibleContradictions = listOf("Claims he retired to bed at 22:00, but a servant saw his silhouette in the gallery at 23:20.")
            ),
            CharacterProfile(
                id = "char_clara_montgomery",
                name = "Clara Montgomery",
                age = 31,
                occupation = "Private Secretary & Archivist",
                publicBio = "Brilliant archivist responsible for typing Lord Blackwood's letters and legal codicils.",
                personality = "Quiet, perceptive, observant",
                publicMotive = "Blackwood knew of her alias and past criminal record for document forgery.",
                relationshipWithVictim = "Secretary",
                privateSecret = "Forged a letter of recommendation using Blackwood's wax seal earlier that afternoon.",
                personalAlibi = "Typing transcripts in the library annex.",
                claimedLocation = "Library Annex",
                actualActions = "Overheard raised voices between Blackwood and Edward at 23:05.",
                knownInformation = "Heard a drawer in the secret safe snap shut around 23:25.",
                hiddenInformation = "Has a duplicate brass key to the library catalog."
            ),
            CharacterProfile(
                id = "char_arthur_pendleton",
                name = "Arthur Pendleton",
                age = 65,
                occupation = "Head Butler",
                publicBio = "Served Blackwood Hall for over thirty-five years. Knows every secret passage.",
                personality = "Impeccably formal, watchful, fiercely loyal to estate tradition",
                publicMotive = "Distressed that Blackwood was planning to sell the ancestral estate to overseas buyers.",
                relationshipWithVictim = "Head of household staff",
                privateSecret = "Eavesdropped on all family meetings from the pantry service speaking tube.",
                personalAlibi = "Securing the front gates and bolting the cellar doors.",
                claimedLocation = "Ground Floor Service Corridor",
                actualActions = "Saw someone in a dark trench coat slipping through the terrace doors at 23:30.",
                knownInformation = "Verified that the study window latch was broken from the inside.",
                hiddenInformation = "Possesses the master key ring for the manor."
            ),
            CharacterProfile(
                id = "char_madame_vivienne",
                name = "Vivienne Leclair",
                age = 42,
                occupation = "French Opera Singer & Socialite Guest",
                publicBio = "Distinguished guest visiting the estate for the autumn gala.",
                personality = "Dramatic, observant, fond of gossip",
                publicMotive = "Blackwood held scandalous love letters from her youth.",
                relationshipWithVictim = "Former romantic acquaintance",
                privateSecret = "Was searching the library desk for her old correspondence at 20:30.",
                personalAlibi = "Playing piano in the music salon.",
                claimedLocation = "Music Salon",
                actualActions = "Played piano to provide an alibi while watching guests move through the foyer.",
                knownInformation = "Saw Edward Vance holding a small glass vial in the hallway earlier."
            ),
            CharacterProfile(
                id = "char_inspector_kane",
                name = "Detective Inspector Thomas Kane",
                age = 51,
                occupation = "Scotland Yard Investigator (Visiting)",
                publicBio = "Off-duty detective visiting Lord Blackwood regarding security at the estate.",
                personality = "Methodical, cynical, authoritative",
                publicMotive = "None (Primary investigator/observer role).",
                relationshipWithVictim = "Official investigator",
                privateSecret = "Suspended from active duty in London pending internal review.",
                personalAlibi = "Examining the perimeter upon hearing the alarm.",
                claimedLocation = "Main Foyer",
                actualActions = "Secured the crime scene and gathered initial statements."
            ),
            CharacterProfile(
                id = "char_marcus_greene",
                name = "Marcus Greene",
                age = 28,
                occupation = "Estate Chauffeur & Mechanic",
                publicBio = "Maintains the motor fleet and generators for Blackwood Hall.",
                personality = "Blunt, hard-working, watchful",
                publicMotive = "Owed money by Edward Vance for transport services.",
                relationshipWithVictim = "Employee",
                privateSecret = "Prepared Edward's car for an unannounced midnight departure.",
                personalAlibi = "Tinkering in the carriage house workshop.",
                claimedLocation = "Carriage House",
                actualActions = "Heard Edward's car engine turn over briefly at 23:40."
            ),
            CharacterProfile(
                id = "char_evelyn_reed",
                name = "Evelyn Reed",
                age = 27,
                occupation = "Estranged Goddaughter & Painter",
                publicBio = "Talented bohemian painter painting Lord Blackwood's official memorial portrait.",
                personality = "Quiet, observing, sensitive to room tensions",
                publicMotive = "Cut off financially after refusing to marry Lord Blackwood's chosen suitor.",
                relationshipWithVictim = "Goddaughter and resident artist",
                privateSecret = "Secretly sketched Edward Vance pacing on the terrace at 23:18 with a small vial.",
                personalAlibi = "In the second-floor studio cleaning paintbrushes.",
                claimedLocation = "Second-floor art studio",
                actualActions = "Watched the garden terrace from her studio window during the storm.",
                knownInformation = "Her watercolor sketch confirms Edward on the terrace at 23:18.",
                hiddenInformation = "Hid the sketchbook under her mattress fearing for her life.",
                possibleContradictions = listOf("Claims she was asleep, but oil paint on her smock was still wet at midnight.")
            ),
            CharacterProfile(
                id = "char_prof_sterling",
                name = "Professor Alistair Sterling",
                age = 58,
                occupation = "Cambridge Antiquities Historian",
                publicBio = "Fellow scholar cataloging Lord Blackwood's private collection of Renaissance manuscripts.",
                personality = "Pedantic, eccentric, obsessive collector",
                publicMotive = "Accused by Blackwood of smuggling estate artifacts to private buyers.",
                relationshipWithVictim = "Colleague and guest",
                privateSecret = "Discovered an ancient botanical codex detailing belladonna preparation in the study library.",
                personalAlibi = "Deciphering Latin manuscripts in the west reading nook.",
                claimedLocation = "West Reading Nook",
                actualActions = "Observed Dr. Finch and Edward arguing near the decanter table earlier.",
                knownInformation = "Identified the decanter residue as Atropa belladonna extract.",
                hiddenInformation = "Carried a pocket magnifying loupe smudged with dried scotch.",
                possibleContradictions = listOf("Claims he never approached the desk, but his fountain pen was found on Blackwood's blotting paper.")
            )
        )

        val evidence = listOf(
            EvidenceItem(
                id = "ev_poisoned_decanter",
                name = "Cut-Crystal Scotch Decanter",
                category = "WeaponTrace",
                discoveryLocation = "Study writing desk",
                discoveryPhase = "Initial",
                description = "Antique crystal decanter containing vintage Glenmorangie, tainted with white powdery residue.",
                detailedInspection = "Chemical smell of bitter belladonna. Only two glasses poured. A single fingerprint smudge on the stopper.",
                linkedCharacterIds = listOf("char_edward_vance", "char_dr_julian_finch"),
                whatItProves = "The poison was delivered orally through the scotch between 23:00 and 23:30.",
                isMisleading = false,
                timelineRelationship = "Placed on desk before 23:15.",
                relevanceToSolution = "Direct murder weapon delivery method."
            ),
            EvidenceItem(
                id = "ev_torn_will_draft",
                name = "Torn Parchment with Blackwood Seal",
                category = "Document",
                discoveryLocation = "Grate of study fireplace",
                discoveryPhase = "SceneInspection",
                description = "Scorched corner of legal parchment naming a sole heir, signed just hours before the murder.",
                detailedInspection = "Words legible: '...hereby revoke all previous bequests to Edward Vance and convey all estate properties to...'",
                linkedCharacterIds = listOf("char_edward_vance"),
                whatItProves = "Edward Vance had immediate motive to kill before the new will was registered with the solicitor tomorrow morning.",
                isMisleading = false,
                timelineRelationship = "Drafted at 19:00, burned partially around 23:20.",
                relevanceToSolution = "Core motive for the murder."
            ),
            EvidenceItem(
                id = "ev_muddy_footprints",
                name = "Terrace Clay Footprints",
                category = "Forensic",
                discoveryLocation = "French doors leading to garden terrace",
                discoveryPhase = "SceneInspection",
                description = "Faint prints of expensive Italian leather brogues stained with red clay.",
                detailedInspection = "Shoe size 10, distinctive diamond tread pattern matching bespoke London shoemaker.",
                linkedCharacterIds = listOf("char_edward_vance"),
                whatItProves = "Edward Vance entered the study from the outside terrace despite claiming he stayed inside the conservatory.",
                isMisleading = false,
                timelineRelationship = "Made between 23:10 and 23:35 during rainfall.",
                relevanceToSolution = "Directly breaks Edward Vance's alibi."
            ),
            EvidenceItem(
                id = "ev_silver_cigarette_case",
                name = "Engraved Silver Cigarette Case",
                category = "PersonalItem",
                discoveryLocation = "Found on terrace garden steps",
                discoveryPhase = "SceneInspection",
                description = "Silver case engraved with monogram 'E.V.', contains three Egyptian cigarettes and a small key.",
                detailedInspection = "Contains a secret compartment with traces of white belladonna powder and a small brass key.",
                linkedCharacterIds = listOf("char_edward_vance"),
                whatItProves = "Edward carried the poison and the key to the study garden door.",
                isMisleading = false,
                timelineRelationship = "Dropped during hasty escape around 23:32.",
                relevanceToSolution = "Physical link connecting culprit to both means and opportunity."
            ),
            EvidenceItem(
                id = "ev_broken_pocket_watch",
                name = "Lord Blackwood's Gold Pocket Watch",
                category = "PersonalItem",
                discoveryLocation = "Study rug beside desk",
                discoveryPhase = "Initial",
                description = "Gold pocket watch stopped at 23:22 with cracked crystal.",
                detailedInspection = "Watch crystal was cracked violently, establishing the precise moment of Lord Blackwood's struggle.",
                linkedCharacterIds = emptyList(),
                whatItProves = "Incident occurred at approximately 23:20 to 23:25.",
                isMisleading = false,
                timelineRelationship = "23:22 GMT.",
                relevanceToSolution = "Anchors the exact timeline of the crime."
            ),
            EvidenceItem(
                id = "ev_opened_wall_safe",
                name = "Open Wall Safe Behind Oil Painting",
                category = "Environmental",
                discoveryLocation = "Study wall behind tilted family portrait",
                discoveryPhase = "SceneInspection",
                description = "Heavy brass dial safe left ajar. Velvet jewel box intact, but legal folder empty.",
                detailedInspection = "Safe was opened with combination dial, not forced. Only the original will folder is missing.",
                linkedCharacterIds = listOf("char_edward_vance", "char_clara_montgomery"),
                whatItProves = "The thief had the combination or was given it by someone who knew it.",
                isMisleading = false,
                timelineRelationship = "Accessed at 23:25.",
                relevanceToSolution = "Proves theft was targeted at inheritance, not burglary."
            ),
            EvidenceItem(
                id = "ev_burnt_charity_receipts",
                name = "Half-Burned Account Receipts",
                category = "Document",
                discoveryLocation = "Drawing room fireplace",
                discoveryPhase = "SceneInspection",
                description = "Ledger carbon slips showing transfers to Thorne Overseas Trust.",
                detailedInspection = "Reveals Lady Beatrice's embezzlement, but completely unrelated to the poison.",
                linkedCharacterIds = listOf("char_beatrice_thorne"),
                whatItProves = "Lady Beatrice was hiding financial misconduct, creating a false suspicion.",
                isMisleading = true,
                timelineRelationship = "Burned at 22:30.",
                relevanceToSolution = "Red herring motive for Lady Beatrice."
            ),
            EvidenceItem(
                id = "ev_medical_kit",
                name = "Dr. Finch's Leather Medical Bag",
                category = "Forensic",
                discoveryLocation = "Guest Room 4",
                discoveryPhase = "SceneInspection",
                description = "Physician's kit containing surgical tools, morphine, and digitalis vials.",
                detailedInspection = "Digitalis vial is full; no belladonna vial is present in doctor's official kit.",
                linkedCharacterIds = listOf("char_dr_julian_finch"),
                whatItProves = "Dr. Finch did not use his medical kit poisons for the murder.",
                isMisleading = true,
                timelineRelationship = "Inspected post-incident.",
                relevanceToSolution = "Clears Dr. Finch from suspicion."
            ),
            EvidenceItem(
                id = "ev_secret_passage_latch",
                name = "Concealed Bookshelf Latch",
                category = "Environmental",
                discoveryLocation = "Study library west bookshelf",
                discoveryPhase = "SceneInspection",
                description = "Hidden spring mechanism behind volume IV of Blackstone's Commentaries.",
                detailedInspection = "Connects the study directly to the south terrace garden path.",
                linkedCharacterIds = listOf("char_arthur_pendleton", "char_edward_vance"),
                whatItProves = "Allows access to the study without passing through the monitored main hall.",
                isMisleading = false,
                timelineRelationship = "Used by killer at 23:15.",
                relevanceToSolution = "Explains how the culprit entered and exited unseen."
            ),
            EvidenceItem(
                id = "ev_telegram_from_monte_carlo",
                name = "Urgent Telegram from Monte Carlo",
                category = "Communication",
                discoveryLocation = "Edward Vance's coat pocket",
                discoveryPhase = "SceneInspection",
                description = "Dated three days ago: 'DEBTS OF 40,000 POUNDS DUE MIDNIGHT OCT 25 OR ASSETS SEIZED.'",
                detailedInspection = "Proves extreme immediate financial pressure on Edward Vance.",
                linkedCharacterIds = listOf("char_edward_vance"),
                whatItProves = "Edward faced ruin within 24 hours unless he secured the estate inheritance.",
                isMisleading = false,
                timelineRelationship = "Received October 21.",
                relevanceToSolution = "Corroborates urgency of motive."
            )
        )

        val timeline = listOf(
            TimelineEvent("tl_1", "19:00", "Will Rewritten", "Lord Blackwood meets with legal counsel to draft new will disinheriting Edward Vance.", listOf("char_edward_vance"), false, true),
            TimelineEvent("tl_2", "21:00", "Dr. Finch Heated Consultation", "Dr. Finch argues with Blackwood over medical treatment in the study.", listOf("char_dr_julian_finch"), false, true),
            TimelineEvent("tl_3", "22:00", "Lady Beatrice Cellar Visit", "Lady Beatrice seen near the wine cellar, later burning papers in drawing room.", listOf("char_beatrice_thorne"), false, true),
            TimelineEvent("tl_4", "23:00", "Edward Vance Alibi Window", "Edward claims he walked alone in the south conservatory during the rainstorm.", listOf("char_edward_vance"), false, true),
            TimelineEvent("tl_5", "23:15", "Study Garden Entry", "Edward slips through the secret passage into the study, poisons the scotch decanter.", listOf("char_edward_vance"), true, false),
            TimelineEvent("tl_6", "23:22", "Fatal Toast & Struggle", "Lord Blackwood drinks poisoned scotch, watch shatters in collapse, safe emptied.", listOf("char_edward_vance"), true, false),
            TimelineEvent("tl_7", "23:45", "Discovery of the Body", "Butler Arthur Pendleton enters with midnight tea, discovers Lord Blackwood and sounds alarm.", listOf("char_arthur_pendleton"), false, true)
        )

        val crimeScene = CrimeSceneData(
            roomName = "Lord Blackwood's Private Library Study",
            description = "A gothic oak-paneled study with high bookshelves, crackling hearth, and heavy velvet curtains overlooking the terrace.",
            hotspots = listOf(
                SceneHotspot("hs_desk", "Mahogany Writing Desk", 0.35f, 0.65f, 0.10f, "Scattered documents, spilled scotch glass, and the crystal decanter.", "ev_poisoned_decanter"),
                SceneHotspot("hs_hearth", "Stone Fireplace Hearth", 0.72f, 0.60f, 0.09f, "Embers glow beneath iron grate with scorched parchment scraps.", "ev_torn_will_draft"),
                SceneHotspot("hs_safe", "Tilted Portrait Wall Safe", 0.74f, 0.32f, 0.08f, "An oil painting pushed aside to reveal an open brass wall safe.", "ev_opened_wall_safe"),
                SceneHotspot("hs_terrace", "Terrace Garden French Doors", 0.20f, 0.45f, 0.09f, "Rain-streaked glass doors with latch unlocked and faint clay smudges.", "ev_muddy_footprints"),
                SceneHotspot("hs_floor", "Antique Persian Floor Rug", 0.50f, 0.80f, 0.08f, "Beside the overturned leather chair lies a shattered gold pocket watch.", "ev_broken_pocket_watch"),
                SceneHotspot("hs_bookshelf", "West Bookshelf Secret Latch", 0.88f, 0.40f, 0.07f, "A false book spine concealing a brass lever to the garden passage.", "ev_secret_passage_latch")
            )
        )

        val deductionPaths = listOf(
            DeductionPath(
                id = "dp_motive",
                title = "The Inheritance Urgency",
                requiredEvidenceIds = listOf("ev_torn_will_draft", "ev_telegram_from_monte_carlo"),
                contradictionDescription = "Edward claimed he was on peaceful terms with his uncle, but telegram proved catastrophic debt and torn will proved imminent disinheritance.",
                conclusion = "Edward Vance had an overwhelming, time-critical financial motive to prevent the new will from taking legal effect."
            ),
            DeductionPath(
                id = "dp_opportunity",
                title = "The Broken Alibi & Terrace Route",
                requiredEvidenceIds = listOf("ev_muddy_footprints", "ev_secret_passage_latch", "ev_broken_pocket_watch"),
                contradictionDescription = "Edward swore he remained inside the conservatory, but red clay matching the garden terrace passage was traced to his shoes at 23:22.",
                conclusion = "Edward Vance was present in the study at the exact moment of Lord Blackwood's death."
            ),
            DeductionPath(
                id = "dp_means",
                title = "The Poison & Secret Key",
                requiredEvidenceIds = listOf("ev_poisoned_decanter", "ev_silver_cigarette_case"),
                contradictionDescription = "Edward claimed no knowledge of poison, but his personal cigarette case contained belladonna residue and the terrace key.",
                conclusion = "Edward Vance possessed both the lethal belladonna agent and the mechanical access to poison the decanter."
            )
        )

        val culpritId = "char_edward_vance"

        // Generate adaptive configurations for human player counts 4 through 10
        val allCharIds = characters.map { it.id }
        val nonCulprits = allCharIds.filter { it != culpritId }

        val configs = mutableMapOf<Int, PlayerCountAssignment>()
        for (count in 4..10) {
            val assignedHumans = mutableListOf(culpritId)
            assignedHumans.addAll(nonCulprits.take(count - 1))
            val npcs = allCharIds.filter { it !in assignedHumans }
            configs[count] = PlayerCountAssignment(
                humanPlayerCount = count,
                assignedHumanCharacterIds = assignedHumans,
                npcCharacterIds = npcs,
                culpritCharacterId = culpritId
            )
        }

        val secretSolution = SecretSolution(
            culpritId = culpritId,
            motive = "Desperate 40,000 pound gambling debts due within 24 hours, combined with Lord Blackwood's sudden decision to disinherit him in favor of charity.",
            opportunity = "Utilized the secret west bookshelf passage from the garden terrace at 23:15, bypassing the main hallway and servants.",
            method = "Administered pure belladonna alkaloid powder into Lord Blackwood's private vintage scotch decanter, knowing he drank a nightcap at 23:20.",
            hiddenTimeline = timeline.filter { it.isHiddenSequence },
            misleadingStrategy = "Attempted to frame Dr. Finch by leaving medical doubts, burned the new will draft, and fabricated a false conservatory walking alibi.",
            smokingGunEvidenceIds = listOf("ev_silver_cigarette_case", "ev_muddy_footprints", "ev_torn_will_draft"),
            contradictionExplanations = mapOf(
                "Conservatory Walk" to "Mud on his shoes proved he crossed the rainy terrace, not the dry indoor conservatory.",
                "Friendly Relationship" to "Torn will draft and Monte Carlo telegram proved bitter enmity and urgent debt."
            ),
            misleadingEvidenceExplanations = mapOf(
                "ev_burnt_charity_receipts" to "Lady Beatrice was hiding financial embezzlement from the estate charity, completely unconnected to the homicide.",
                "ev_medical_kit" to "Dr. Finch carried legitimate cardiac medicines; his digitalis stock was untouched."
            ),
            finalVerdictExplanation = "Edward Vance is guilty of the murder of Lord Reginald Blackwood. Driven by imminent financial ruin from Monte Carlo gambling syndicates, Edward entered the study via the concealed terrace passage, laced the scotch decanter with belladonna, tore the new will draft, and fled. His alibi collapsed when forensic soil matches, the recovered cigarette case with poison residue, and his urgent telegram were confronted against his testimony."
        )

        return CasePackage(
            identity = CaseIdentity(
                caseId = caseId,
                title = "The Cipher at Blackwood Hall",
                subtitle = "A deadly poison, an altered will, and eight suspects behind locked doors.",
                classification = "CLASSIFIED DOSSIER - LEVEL 4",
                setting = "Blackwood Hall, Northumberland, England",
                dateTime = "October 24, 1928, 23:45 GMT",
                difficulty = request.difficulty,
                estimatedDurationMinutes = "7-10 minutes",
                cinematicIntro = "Rain lashes the leaded glass windows of Blackwood Hall. The storm has severed the telegraph wires. When the grandfather clock strikes midnight, Lord Blackwood sits motionless at his mahogany desk. A crystal glass lies shattered on the Persian rug. The newly revised will is gone.",
                incidentSummary = "Lord Reginald Blackwood was poisoned in his private library study between 23:15 and 23:30. Eight individuals were inside the manor. One is the killer.",
                investigationObjective = "Uncover who poisoned Lord Blackwood, reconstruct the timeline, expose the broken alibi, and accuse the true culprit.",
                posterAssetId = "img_case_poster"
            ),
            victim = victim,
            characters = characters,
            evidence = evidence,
            timeline = timeline,
            crimeScene = crimeScene,
            deductionPaths = deductionPaths,
            playerCountConfigs = configs,
            secretSolution = secretSolution
        )
    }
}
