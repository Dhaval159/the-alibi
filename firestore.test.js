const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const HOST_UID = "host_user_1";
const DETECTIVE_BOB = "detective_bob_2";
const DETECTIVE_CLARA = "detective_clara_3";
const INTRUDER_UID = "intruder_user_4";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// --- SECURITY & BOUNDARY TESTS ---

test("Unauthenticated user: cannot read or write rooms", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("rooms").get());
  await assertFails(unauthDb.collection("rooms").doc("ROOM12").set({ roomCode: "ROOM12" }));
});

test("Host can create room and players can read room", async () => {
  const hostDb = testEnv.authenticatedContext(HOST_UID).firestore();
  await assertSucceeds(
    hostDb.collection("rooms").doc("ROOM12").set({
      roomCode: "ROOM12",
      hostUid: HOST_UID,
      caseId: "case_blackwood",
      status: "LOBBY",
      playerCount: 1,
      maxPlayers: 6,
      turnNumber: 0,
      createdAt: new Date(),
    })
  );

  const bobDb = testEnv.authenticatedContext(DETECTIVE_BOB).firestore();
  await assertSucceeds(bobDb.collection("rooms").doc("ROOM12").get());
});

test("Player can join room and read players roster", async () => {
  // Seed room
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("rooms").doc("ROOM12").set({
      roomCode: "ROOM12",
      hostUid: HOST_UID,
      caseId: "case_blackwood",
      status: "LOBBY",
      playerCount: 1,
      maxPlayers: 6,
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(DETECTIVE_BOB).firestore();
  await assertSucceeds(
    bobDb.collection("rooms").doc("ROOM12").collection("players").doc(DETECTIVE_BOB).set({
      uid: DETECTIVE_BOB,
      alias: "Bob Detective",
      isHost: false,
      isReady: true,
      isOnline: true,
      joinedAt: new Date(),
    })
  );

  // Bob can read players
  await assertSucceeds(
    bobDb.collection("rooms").doc("ROOM12").collection("players").get()
  );
});

test("Private Dossier isolation: Detective Bob cannot read Detective Clara's secret dossier", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("rooms").doc("ROOM12").set({
      roomCode: "ROOM12",
      hostUid: HOST_UID,
      caseId: "case_blackwood",
      status: "INVESTIGATION",
      playerCount: 2,
      maxPlayers: 6,
      createdAt: new Date(),
    });
    await context.firestore().collection("rooms").doc("ROOM12").collection("privateDossiers").doc(DETECTIVE_CLARA).set({
      uid: DETECTIVE_CLARA,
      characterId: "char_clara",
      isCulprit: true,
      secretObjective: "Conceal the arsenic vial",
      alibiSecret: "Was in the garden burning letters",
    });
  });

  const bobDb = testEnv.authenticatedContext(DETECTIVE_BOB).firestore();
  // Bob reading Clara's dossier MUST fail
  await assertFails(
    bobDb.collection("rooms").doc("ROOM12").collection("privateDossiers").doc(DETECTIVE_CLARA).get()
  );

  // Clara reading her own dossier MUST succeed
  const claraDb = testEnv.authenticatedContext(DETECTIVE_CLARA).firestore();
  await assertSucceeds(
    claraDb.collection("rooms").doc("ROOM12").collection("privateDossiers").doc(DETECTIVE_CLARA).get()
  );
});

test("Private Chat: Message is isolated between sender and recipient", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("rooms").doc("ROOM12").set({
      roomCode: "ROOM12",
      hostUid: HOST_UID,
      caseId: "case_blackwood",
      status: "DEBATE",
      playerCount: 3,
      maxPlayers: 6,
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(DETECTIVE_BOB).firestore();
  await assertSucceeds(
    bobDb.collection("rooms").doc("ROOM12").collection("messages").doc("msg_1").set({
      senderUid: DETECTIVE_BOB,
      senderAlias: "Bob",
      recipientUid: DETECTIVE_CLARA,
      text: "Do you have the bloody handkerchief?",
      timestamp: new Date(),
    })
  );

  // Clara (recipient) can read
  const claraDb = testEnv.authenticatedContext(DETECTIVE_CLARA).firestore();
  await assertSucceeds(
    claraDb.collection("rooms").doc("ROOM12").collection("messages").doc("msg_1").get()
  );

  // Intruder cannot read private message
  const intruderDb = testEnv.authenticatedContext(INTRUDER_UID).firestore();
  await assertFails(
    intruderDb.collection("rooms").doc("ROOM12").collection("messages").doc("msg_1").get()
  );
});

test("Secret Ballots: Only revealed during RESOLUTION phase", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("rooms").doc("ROOM12").set({
      roomCode: "ROOM12",
      hostUid: HOST_UID,
      caseId: "case_blackwood",
      status: "VOTING",
      playerCount: 2,
      maxPlayers: 6,
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(DETECTIVE_BOB).firestore();
  await assertSucceeds(
    bobDb.collection("rooms").doc("ROOM12").collection("votes").doc(DETECTIVE_BOB).set({
      voterUid: DETECTIVE_BOB,
      accusedCharacterId: "char_clara",
      smokingGunEvidenceId: "ev_arsenic",
      motive: "Inheritance money",
    })
  );

  // During VOTING phase, Clara cannot view Bob's ballot
  const claraDb = testEnv.authenticatedContext(DETECTIVE_CLARA).firestore();
  await assertFails(
    claraDb.collection("rooms").doc("ROOM12").collection("votes").doc(DETECTIVE_BOB).get()
  );

  // Once status is updated to RESOLUTION, Clara can see ballots
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("rooms").doc("ROOM12").update({
      status: "RESOLUTION",
    });
  });

  await assertSucceeds(
    claraDb.collection("rooms").doc("ROOM12").collection("votes").doc(DETECTIVE_BOB).get()
  );
});
