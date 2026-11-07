# Firebase security audit

**Audit date:** 2026-11-07  
**Method:** static review of `firestore.rules`, data models, repository writes and `firestore.test.js`. Firebase Emulator Suite tests were not executable here; findings below are rule-level vulnerabilities, not claims of executed exploit tests.

## Overall result

The rules default-deny unmatched paths, but existing matches grant clients extensive trust. The current Firebase repositories are not wired to gameplay, yet any client authenticated to the project can invoke the allowed Firestore operations directly. Firestore rules are not a replacement for trusted server validation. No Cloud Functions or Storage rules were found.

## Findings

### Critical: room membership and cross-room access are not enforced

`rooms/{roomId}` allows any signed-in user to `get` any room when the ID syntax is valid and allows broad signed-in `list`. Player reads are similarly open to any signed-in identity. The rule does not check that the caller is a room member. A caller can enumerate/read room metadata and roster across rooms. Private documents are more restricted, but the room host is explicitly allowed to read all dossiers.

Room creation only checks `hostUid == auth.uid`, code shape/length, timestamps and basic fields. It does not require a corresponding host player, bind room ID to code, constrain allowed keys, or validate code uniqueness atomically.

### Critical: players can self-assign host and gameplay identity

Any signed-in caller may create `players/{uid}` if the UID matches the path, with arbitrary `isHost`, `isReady`, `isOnline`, and optional character assignment. A user can set `isHost: true` inside their player document. Existing users can update their own full player document, including host flag, readiness, online status and assigned character. Host can also update any player. No rule derives host status from immutable `rooms.hostUid`, freezes assignments, checks phase, or validates character uniqueness.

### Critical: room host can arbitrarily alter match state

The room update branch grants the room host any update so long as the resulting document passes `isValidRoom` and host/createdAt stay unchanged. The host can skip phases, manipulate counts, change case ID/turn number, start before players are ready, undo phases, or set `RESOLUTION` early. `playerCount` is not checked against actual player documents.

### Critical: dossiers are writable by their owner

`privateDossiers/{uid}` allows the owner **or room host** to create/update/delete. A player can write their own `isCulprit`, character, and secrets; host can read every dossier and alter roles. A player can also create a dossier for their UID without proving room membership. The private dossier read boundary does not make its contents trustworthy.

### High: evidence discovery/sharing is client-forgeable and public reads are broad

Any signed-in user can read/list all evidence in any room. A user can create evidence for any room if `discoveredByUid` matches their own UID; no membership, phase, allowed-evidence catalog, unlock condition or authoritative share transition is checked. The discoverer can edit all validated fields, including `isSmokingGun` and `isShared`, and delete it.

### High: ballots are mutable and may be read prematurely

Ballot writes require caller UID to equal the ballot path/`voterUid`, but do not require room membership, `VOTING` phase, allowed character/evidence IDs, or a single immutable submission. `allow create, update` explicitly permits changing an existing vote. Any authenticated user can read their own-path ballot even before resolution; all callers can read ballots in `RESOLUTION`, while the host can directly change the room to that phase. `observeBallots()` queries the entire subcollection, which will be denied for ordinary players before resolution under per-document rules; after resolution it may be readable, but phase gating is host-controlled. Duplicate ballots under other document IDs are blocked only if caller also sets path to their UID; there is no canonical server tally or duplicate-vote prevention across identity aliases because Firebase UID identity is the only binding and guest identities can create multiple anonymous UIDs.

### High: private messages can be forged; query denial and membership missing

Create requires sender UID equal auth UID, but caller may claim any sender alias or arbitrary recipient UID and send in any room without membership. The rule restricts message reads based on recipient/sender/public. Unrelated users should fail individual private-message reads; however, this has not been emulator-tested. Public messages can be read by every signed-in user, including nonmembers. Firestore queries must constrain recipient/sender fields for rules to prove list access; a broad collection listener used by `observeMessages()` can fail as a query. Sender alias is spoofable because it is not matched to player record. No rate/abuse guard exists.

### High: deals allow unauthorized state transitions

Participants can update deal fields under a partial immutable-party check. No transition graph, pending-only acceptance, recipient-only accept/reject, membership, phase, offer ownership, or clue ownership validation is enforced. Sender can accept their own deal and parties can rewrite terms/clue references.

### High: case library exposes full case JSON

Public case reads include `caseJson`; rules cannot hide solution fields within a document. A public case document can expose its solution to any signed-in user. Case updates are creator-scoped, but document data and `isPublic` can be set by creator; no server review or field immutability.

### Missing controls

- No Storage rules were present; Storage access posture cannot be verified.
- No App Check enforcement is visible in the rules/config/deployment artifacts.
- No Functions enforce trusted operations.
- No Firebase Emulator Suite execution or recorded test results. `firestore.test.js` is a Node test script requiring `@firebase/rules-unit-testing`, but no package manifest/dependency installation was found.

## Requested attack test matrix

| Attack | Static outcome | Executed emulator result |
|---|---|---|
| Unauthenticated read/write | Denied by signed-in checks/default deny | Not run |
| Cross-room room/roster access | Allowed to any signed-in UID | Not run |
| Read another player's private dossier | Denied to non-owner/non-host by the explicit rule; host can read all | Not run |
| Unauthorized role modification | Allowed: self updates all player fields; dossier owner and host can edit role fields | Not run |
| Premature solution access | No dedicated solution path. Full case JSON may include solution; host can read dossiers | Not run |
| Premature ballot access | Own ballot always readable; host can force resolution, then all ballots readable | Not run |
| Unauthorized vote modification | Existing ballot updates explicitly allowed for its UID | Not run |
| Duplicate votes | Canonical UID path limits normal writes, but mutable updates allowed; no trusted uniqueness/tally | Not run |
| Unrelated private message read | Individual read denied unless public/sender/recipient; query path unverified | Not run |
| Host privilege abuse | Host can update any valid room fields and player/dossier records | Not run |
| Forged player identity | Self may create UID-matched record and set `isHost`/assignments; nonmember reads/writes broadly | Not run |
| Direct client bypass of game rules | Allowed for many room, player, dossier, evidence, deal, vote operations | Not run |

The existing tests do not test these controls comprehensively. In particular, their room creation test uses a six-character string `ROOM12` despite product language describing a six-digit numeric code; test data does not establish numeric-only enforcement. Test claims are not results because execution was unavailable.

## Required corrective design

1. Make critical state server-owned. Clients should call authenticated Functions for room lifecycle, roster freeze/assignment, case generation, evidence state, deal/interrogation transitions, phase/timer, ballots and resolution.
2. Rework rules to require actual room membership on every room-scoped read/write; make host role immutable and derived from room host UID; tightly enumerate mutable fields and valid transitions.
3. Store solution separately from all player-readable data; allow only trusted server reads until resolution. Keep dossiers server-written and owner-readable only; exclude host access unless product explicitly requires it.
4. Make ballots create-only at canonical UID path, in voting phase, member-only, schema-bound; no client read of other ballots before resolution and server-only transition into resolution. Server tally exactly once.
5. Validate evidence against the case catalog and state/phase; represent share state in server-owned records. Enforce message sender alias/room membership and recipient membership; use query shapes compatible with rules.
6. Add Storage rules scoped to authenticated room members and server-controlled asset writes. Add App Check where appropriate.
7. Run emulator tests for every row above, including forged field updates, query behavior, room-join concurrency, host abuse, and direct REST SDK writes. Publish test command and results.
