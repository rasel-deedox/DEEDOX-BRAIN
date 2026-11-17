const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validItem(userId) {
  const now = new Date();
  return {
    userId,
    title: "Jetpack Compose Architecture Guide",
    description: "Official Android docs on state and navigation",
    originalUrl: "https://developer.android.com/compose",
    canonicalUrl: "https://developer.android.com/compose",
    domain: "developer.android.com",
    contentType: "article",
    sourceType: "web",
    isFavorite: true,
    status: "organized",
    folderIds: ["folder-android"],
    tagNames: ["compose", "architecture"],
    isDeviceLocal: false,
    localVersion: 1,
    createdAt: now,
    updatedAt: now,
  };
}

function validFolder(userId) {
  const now = new Date();
  return {
    userId,
    name: "Android Architecture",
    colorHex: "#7C5CFC",
    iconKey: "folder",
    sortOrder: 0,
    isFavorite: false,
    isArchived: false,
    createdAt: now,
    updatedAt: now,
  };
}

function validNote(userId, itemId) {
  const now = new Date();
  return {
    userId,
    itemId,
    body: "Review rememberSaveable patterns",
    isPinned: true,
    localVersion: 1,
    createdAt: now,
    updatedAt: now,
  };
}

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

test("Unauthenticated user cannot read or list items", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("items").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Owner can create, read, list, and update their own library item", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const itemRef = aliceDb.collection("users").doc(ALICE_UID).collection("items").doc("item-1");

  await assertSucceeds(itemRef.set(validItem(ALICE_UID)));
  await assertSucceeds(itemRef.get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("items").get());
  await assertSucceeds(
    itemRef.update({
      title: "Updated Compose Guide",
      localVersion: 2,
      updatedAt: new Date(),
    })
  );
});

test("Cross-user isolation: Bob cannot read, list, write, or delete Alice's items (AC-15)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const aliceItemRef = aliceDb.collection("users").doc(ALICE_UID).collection("items").doc("item-1");
  await assertSucceeds(aliceItemRef.set(validItem(ALICE_UID)));

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("items").doc("item-1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("items").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("items").doc("item-2").set(validItem(BOB_UID))
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("items").doc("item-1").delete());
});

test("Shadow Update & Identity Spoofing: rejects ghost fields and spoofed userId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const itemRef = aliceDb.collection("users").doc(ALICE_UID).collection("items").doc("item-ghost");

  const withGhost = { ...validItem(ALICE_UID), isAdmin: true };
  await assertFails(itemRef.set(withGhost));

  const spoofedOwner = validItem(BOB_UID);
  await assertFails(itemRef.set(spoofedOwner));
});

test("Validation rejects invalid contentType or oversized title", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const itemRef = aliceDb.collection("users").doc(ALICE_UID).collection("items").doc("item-bad");

  await assertFails(itemRef.set({ ...validItem(ALICE_UID), contentType: "executable_binary" }));
  await assertFails(itemRef.set({ ...validItem(ALICE_UID), title: "a".repeat(501) }));
});

test("Owner can create and update folders and notes; cannot mutate immutable userId or itemId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const folderRef = aliceDb.collection("users").doc(ALICE_UID).collection("folders").doc("folder-1");
  await assertSucceeds(folderRef.set(validFolder(ALICE_UID)));

  const noteRef = aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc("note-1");
  await assertSucceeds(noteRef.set(validNote(ALICE_UID, "item-1")));
  await assertFails(noteRef.update({ itemId: "item-other", updatedAt: new Date() }));
});

test("Admin vault: owner can store dynamic API keys; unauthenticated and cross-user denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const vaultRef = aliceDb.collection("users").doc(ALICE_UID).collection("admin_vault").doc("api_config");
  const now = new Date();
  await assertSucceeds(
    vaultRef.set({
      userId: ALICE_UID,
      aiProvider: "openrouter",
      openRouterApiKey: "sk-or-v1-example",
      openRouterModel: "google/gemini-2.5-flash",
      supabaseUrl: "https://example.supabase.co",
      customCredentialsJson: "[]",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(vaultRef.get());
  await assertSucceeds(vaultRef.update({ aiProvider: "firebase_ai", updatedAt: new Date() }));

  await assertFails(vaultRef.set({
    userId: ALICE_UID,
    aiProvider: "invalid_provider",
    createdAt: now,
    updatedAt: now,
  }));

  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("admin_vault").doc("api_config").get());

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("admin_vault").doc("api_config").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("admin_vault").doc("api_config").set({
      userId: BOB_UID,
      aiProvider: "openrouter",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Global app_config/ai_runtime: admin writes, all signed-in mobile users read, non-admin cannot overwrite or delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date();

  const aliceGlobalRef = aliceDb.collection("app_config").doc("ai_runtime");
  await assertSucceeds(
    aliceGlobalRef.set({
      adminUid: ALICE_UID,
      aiProvider: "openrouter",
      openRouterApiKey: "sk-or-v1-global-test",
      openRouterModel: "openrouter/free",
      supabaseUrl: "https://example.supabase.co",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Any signed-in mobile user (Bob) can read the updated global key & free model
  await assertSucceeds(bobDb.collection("app_config").doc("ai_runtime").get());

  // Admin Alice can rotate key & model
  await assertSucceeds(
    aliceGlobalRef.update({
      openRouterApiKey: "sk-or-v1-rotated",
      openRouterModel: "meta-llama/llama-3.3-70b-instruct:free",
      updatedAt: new Date(),
    })
  );

  // Non-admin Bob cannot overwrite, spoof adminUid, inject ghost fields, list, or delete
  await assertFails(
    bobDb.collection("app_config").doc("ai_runtime").update({
      openRouterApiKey: "sk-or-hijacked",
      updatedAt: new Date(),
    })
  );
  await assertFails(bobDb.collection("app_config").doc("ai_runtime").delete());
  await assertFails(bobDb.collection("app_config").get());

  // Unauthenticated client cannot read or write
  await assertFails(unauthDb.collection("app_config").doc("ai_runtime").get());

  // Ghost fields / invalid provider rejected even for admin
  await assertFails(
    aliceGlobalRef.update({
      ghostFlag: true,
      updatedAt: new Date(),
    })
  );
  await assertFails(
    aliceGlobalRef.update({
      aiProvider: "paid_unknown",
      updatedAt: new Date(),
    })
  );
});

test("Workspace collections (notes, tasks, reminders, habits): owner CRUD succeeds; cross-user & ghost fields fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date();

  const noteRef = aliceDb.collection("users").doc(ALICE_UID).collection("workspace_notes").doc("wn-1");
  await assertSucceeds(
    noteRef.set({
      userId: ALICE_UID,
      title: "Architecture Block Note",
      blocksJson: "[{\"type\":\"H1\",\"text\":\"Monk Mode\"}]",
      tagsCsv: "focus,architecture",
      isPinned: true,
      audioDurationMs: 0,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(noteRef.update({ isPinned: false, updatedAt: new Date() }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("workspace_notes").doc("wn-1").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("workspace_notes").get());

  const taskRef = aliceDb.collection("users").doc(ALICE_UID).collection("workspace_tasks").doc("wt-1");
  await assertSucceeds(
    taskRef.set({
      userId: ALICE_UID,
      title: "Review PRs",
      notes: "Priority check",
      dueDateEpochDay: 20150,
      priority: "HIGH",
      isCompleted: false,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(taskRef.set({
    userId: ALICE_UID,
    title: "Bad task",
    dueDateEpochDay: 20150,
    priority: "EXTREME",
    isCompleted: false,
    createdAt: now,
    updatedAt: now,
  }));

  const remRef = aliceDb.collection("users").doc(ALICE_UID).collection("reminders").doc("rem-1");
  await assertSucceeds(
    remRef.set({
      userId: ALICE_UID,
      mode: "PERSONAL",
      title: "Deep Work Block",
      priority: "HIGH",
      triggerAtMillis: Date.now() + 60000,
      recurrenceType: "DAILY",
      intervalMinutes: 0,
      ringtoneTitle: "Default Alert",
      isEnabled: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("reminders").doc("rem-1").delete());

  const habitRef = aliceDb.collection("users").doc(ALICE_UID).collection("habits").doc("hab-1");
  await assertSucceeds(
    habitRef.set({
      userId: ALICE_UID,
      title: "2h Monk Focus",
      category: "Focus",
      targetPerWeek: 6,
      completedEpochDays: [20149, 20150],
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(habitRef.set({
    userId: ALICE_UID,
    title: "Ghost Habit",
    category: "Focus",
    targetPerWeek: 7,
    completedEpochDays: [],
    extraGhost: 123,
    createdAt: now,
    updatedAt: now,
  }));
});

test("OS modules (gamification, startups, finance sectors & transactions): owner CRUD succeeds; cross-user & validation fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date();

  const gamRef = aliceDb.collection("users").doc(ALICE_UID).collection("gamification").doc("stats");
  await assertSucceeds(
    gamRef.set({
      userId: ALICE_UID,
      totalXp: 350,
      level: 3,
      rankTitle: "Focus Builder",
      bestStreak: 14,
      totalCompletions: 12,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(gamRef.update({ totalXp: 425, level: 4, updatedAt: new Date() }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("gamification").doc("stats").get());

  const startupRef = aliceDb.collection("users").doc(ALICE_UID).collection("startups").doc("st-1");
  await assertSucceeds(
    startupRef.set({
      userId: ALICE_UID,
      name: "Deedox Studio OS",
      tagline: "Personal AI knowledge operating system",
      stage: "MVP",
      targetPersona: "Solo founders and engineers",
      techStack: "Kotlin, Jetpack Compose, Room, Firestore",
      aiPlanMarkdown: "# Venture Plan",
      phasesJson: "[]",
      sprintTasksJson: "[]",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    startupRef.set({
      userId: ALICE_UID,
      name: "Bad Stage",
      stage: "UNKNOWN",
      phasesJson: "[]",
      sprintTasksJson: "[]",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("startups").doc("st-1").get());

  const secRef = aliceDb.collection("users").doc(ALICE_UID).collection("finance_sectors").doc("sec-saas");
  await assertSucceeds(
    secRef.set({
      userId: ALICE_UID,
      name: "Software & APIs",
      iconKey: "code",
      colorHex: "#D1432A",
      typeAffinity: "EXPENSE",
      isCustom: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("finance_sectors").doc("sec-saas").get());

  const txRef = aliceDb.collection("users").doc(ALICE_UID).collection("finance_transactions").doc("tx-1");
  await assertSucceeds(
    txRef.set({
      userId: ALICE_UID,
      txType: "EXPENSE",
      amountCents: 2499,
      sectorId: "sec-saas",
      sectorName: "Software & APIs",
      paymentMethod: "CARD",
      notes: "OpenRouter credits",
      occurredAtMillis: Date.now(),
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    txRef.set({
      userId: ALICE_UID,
      txType: "EXPENSE",
      amountCents: -500,
      sectorId: "sec-saas",
      sectorName: "Software & APIs",
      paymentMethod: "CARD",
      occurredAtMillis: Date.now(),
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("finance_transactions").doc("tx-1").delete());
});

test("Team collaboration & invite capacity rules: owner creates, invite joins member, non-member denied, capacity enforced", async () => {
  const CHARLIE_UID = "charlie_789";
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = new Date();

  const teamRef = aliceDb.collection("teams").doc("team-alpha");
  await assertSucceeds(
    teamRef.set({
      ownerId: ALICE_UID,
      name: "Alpha Founders",
      description: "Product & launch squad",
      inviteCode: "ALPHA99",
      maxMembers: 2,
      memberUids: [ALICE_UID],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Invite code allows direct single-doc get by signed-in user, never list
  const inviteRef = aliceDb.collection("team_invites").doc("ALPHA99");
  await assertSucceeds(
    inviteRef.set({
      teamId: "team-alpha",
      teamName: "Alpha Founders",
      ownerId: ALICE_UID,
      maxMembers: 2,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(bobDb.collection("team_invites").doc("ALPHA99").get());
  await assertFails(bobDb.collection("team_invites").get());

  // Owner registers member record
  await assertSucceeds(
    teamRef.collection("members").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Alice",
      role: "OWNER",
      isOnline: true,
      lastActiveAtMillis: Date.now(),
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob is NOT a member yet -> denied reading shared docs
  await assertFails(bobDb.collection("teams").doc("team-alpha").collection("shared_docs").get());

  // Bob joins team via invite flow (adds only himself to memberUids)
  await assertSucceeds(
    bobDb.collection("teams").doc("team-alpha").update({
      memberUids: [ALICE_UID, BOB_UID],
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    bobDb.collection("teams").doc("team-alpha").collection("members").doc(BOB_UID).set({
      uid: BOB_UID,
      displayName: "Bob",
      role: "MEMBER",
      isOnline: true,
      lastActiveAtMillis: Date.now(),
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  // Charlie cannot join because maxMembers == 2 is reached
  await assertFails(
    charlieDb.collection("teams").doc("team-alpha").update({
      memberUids: [ALICE_UID, BOB_UID, CHARLIE_UID],
      updatedAt: new Date(),
    })
  );

  // Owner expands team limit to 5 -> Charlie can now join
  await assertSucceeds(teamRef.update({ maxMembers: 5, updatedAt: new Date() }));
  await assertSucceeds(
    charlieDb.collection("teams").doc("team-alpha").update({
      memberUids: [ALICE_UID, BOB_UID, CHARLIE_UID],
      updatedAt: new Date(),
    })
  );

  // Team members collaborate on shared_docs, shared_tasks, and notifications
  await assertSucceeds(
    bobDb.collection("teams").doc("team-alpha").collection("shared_docs").doc("doc-1").set({
      teamId: "team-alpha",
      title: "Launch Spec",
      content: "Deploy MVP on Monday",
      resourceUrl: "https://example.com/spec",
      authorUid: BOB_UID,
      authorName: "Bob",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    bobDb.collection("teams").doc("team-alpha").collection("shared_tasks").doc("stask-1").set({
      teamId: "team-alpha",
      title: "Finalize onboarding",
      assigneeUid: CHARLIE_UID,
      assigneeName: "Charlie",
      priority: "HIGH",
      isCompleted: false,
      createdByUid: BOB_UID,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("teams").doc("team-alpha").collection("notifications").doc("notif-1").set({
      teamId: "team-alpha",
      senderUid: ALICE_UID,
      senderName: "Alice",
      title: "Sprint Kickoff",
      message: "Check the Launch Spec now!",
      createdAtMillis: Date.now(),
      createdAt: new Date(),
    })
  );

  // Non-member Eve still cannot read notifications
  const eveDb = testEnv.authenticatedContext("eve_999").firestore();
  await assertFails(eveDb.collection("teams").doc("team-alpha").collection("notifications").get());

  // Member profile update (roleTag & customStatus)
  await assertSucceeds(
    teamRef.collection("members").doc(BOB_UID).update({
      roleTag: "Lead Developer",
      customStatus: "Working on API",
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    bobDb.collection("teams").doc("team-alpha").collection("members").doc(BOB_UID).update({
      customStatus: "In Focus",
      updatedAt: new Date(),
    })
  );

  // Weekly day-by-day schedules with multi-member tagging and status
  const schedRef = bobDb.collection("teams").doc("team-alpha").collection("schedules").doc("sched-1");
  await assertSucceeds(
    schedRef.set({
      teamId: "team-alpha",
      title: "Build Firebase Auth Bridge",
      description: "Hook up CredentialManager + Firestore snapshot sync",
      dayOfWeek: "Mon",
      dateLabel: "Mon · 10:00 AM - 12:30 PM",
      startTime: "10:00 AM",
      endTime: "12:30 PM",
      assignedTo: [BOB_UID, CHARLIE_UID],
      assignedToNames: ["Bob", "Charlie"],
      status: "in_progress",
      priority: "HIGH",
      createdByUid: BOB_UID,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    charlieDb.collection("teams").doc("team-alpha").collection("schedules").doc("sched-1").update({
      status: "done",
      updatedAt: new Date(),
    })
  );
  await assertFails(
    bobDb.collection("teams").doc("team-alpha").collection("schedules").doc("sched-bad").set({
      teamId: "team-alpha",
      title: "Bad Day",
      dayOfWeek: "Funday",
      assignedTo: [BOB_UID],
      status: "todo",
      priority: "MEDIUM",
      createdByUid: BOB_UID,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
  await assertFails(eveDb.collection("teams").doc("team-alpha").collection("schedules").get());

  // Collaborative Notion-style shared_notes
  const noteRef = aliceDb.collection("teams").doc("team-alpha").collection("shared_notes").doc("snote-1");
  await assertSucceeds(
    noteRef.set({
      teamId: "team-alpha",
      title: "Release Architecture Checklist",
      content: "Verify emulator tests before rollout",
      blocksJson: "[]",
      tagsCsv: "release,architecture",
      resourceUrl: "https://example.com/arch",
      authorUid: ALICE_UID,
      authorName: "Alice",
      lastEditedBy: "Alice",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    bobDb.collection("teams").doc("team-alpha").collection("shared_notes").doc("snote-1").update({
      content: "Emulator tests passing; ready for rollout",
      lastEditedBy: "Bob",
      updatedAt: new Date(),
    })
  );
  await assertFails(eveDb.collection("teams").doc("team-alpha").collection("shared_notes").get());

  // Team Notice Board with @Member tagging
  await assertSucceeds(
    aliceDb.collection("teams").doc("team-alpha").collection("notices").doc("notice-1").set({
      teamId: "team-alpha",
      title: "API Freeze Tonight",
      message: "@Bob please merge final schema PR before 9 PM",
      authorId: ALICE_UID,
      authorName: "Alice",
      taggedUsers: [BOB_UID],
      taggedNames: ["Bob"],
      createdAtMillis: Date.now(),
      createdAt: new Date(),
    })
  );
  await assertFails(eveDb.collection("teams").doc("team-alpha").collection("notices").get());

  // Regular notices cannot be updated, but poll notices allow pollVotesJson voting by team members
  await assertFails(
    bobDb.collection("teams").doc("team-alpha").collection("notices").doc("notice-1").update({
      pollVotesJson: "{\"bob_456\":0}",
    })
  );
  const pollRef = aliceDb.collection("teams").doc("team-alpha").collection("notices").doc("poll-1");
  await assertSucceeds(
    pollRef.set({
      teamId: "team-alpha",
      title: "Release Day?",
      message: "Vote for deploy window",
      authorId: ALICE_UID,
      authorName: "Alice",
      taggedUsers: [],
      taggedNames: [],
      noticeType: "poll",
      pollOptionsJson: "[\"Friday\",\"Monday\"]",
      pollVotesJson: "{}",
      createdAtMillis: Date.now(),
      createdAt: new Date(),
    })
  );
  await assertSucceeds(
    bobDb.collection("teams").doc("team-alpha").collection("notices").doc("poll-1").update({
      pollVotesJson: "{\"bob_456\":1}",
    })
  );
  await assertFails(
    eveDb.collection("teams").doc("team-alpha").collection("notices").doc("poll-1").update({
      pollVotesJson: "{\"eve_999\":0}",
    })
  );

  // Extended 5-stage Kanban schedules (backlog / review) + deadlineMillis + linkedDriveUrl + sourceBusinessId
  await assertSucceeds(
    bobDb.collection("teams").doc("team-alpha").collection("schedules").doc("sched-backlog").set({
      teamId: "team-alpha",
      title: "Client Deal Followup",
      dayOfWeek: "Tue",
      assignedTo: [BOB_UID],
      assignedToNames: ["Bob"],
      status: "backlog",
      priority: "MEDIUM",
      deadlineMillis: Date.now() + 86400000,
      linkedDriveUrl: "https://drive.google.com/file/d/abc/view",
      sourceBusinessId: "biz-1",
      createdByUid: BOB_UID,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    charlieDb.collection("teams").doc("team-alpha").collection("schedules").doc("sched-backlog").update({
      status: "review",
      updatedAt: new Date(),
    })
  );

  // Nested Notion sub-pages (parentId, icon, isPinned)
  await assertSucceeds(
    bobDb.collection("teams").doc("team-alpha").collection("shared_notes").doc("snote-sub").set({
      teamId: "team-alpha",
      title: "API Endpoints Sub-page",
      content: "Nested technical spec",
      blocksJson: "[]",
      parentId: "snote-1",
      icon: "🚀",
      isPinned: true,
      authorUid: BOB_UID,
      authorName: "Bob",
      lastEditedBy: "Bob",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  // Team Activity Feed: members create/list, updates denied, non-members denied
  await assertSucceeds(
    bobDb.collection("teams").doc("team-alpha").collection("activity").doc("act-1").set({
      teamId: "team-alpha",
      actorUid: BOB_UID,
      actorName: "Bob",
      actionType: "BUSINESS_SYNC",
      summary: "Synced Business OS sprint to team",
      targetId: "biz-1",
      createdAtMillis: Date.now(),
      createdAt: new Date(),
    })
  );
  await assertFails(
    bobDb.collection("teams").doc("team-alpha").collection("activity").doc("act-1").update({
      summary: "Tampered",
    })
  );
  await assertFails(eveDb.collection("teams").doc("team-alpha").collection("activity").get());

  // Role promotion & Direct Gmail Invite & Member removal
  // Owner promotes Bob to ADMIN
  await assertSucceeds(
    teamRef.collection("members").doc(BOB_UID).update({
      role: "ADMIN",
      updatedAt: new Date(),
    })
  );
  // Charlie (MEMBER) cannot invite by Gmail, Bob (ADMIN) can
  const DANA_UID = "dana_321";
  const danaDb = testEnv.authenticatedContext(DANA_UID, {
    email: "dana@deedox.io",
    email_verified: true,
  }).firestore();
  await assertFails(
    charlieDb.collection("team_email_invites").doc("inv-bad").set({
      teamId: "team-alpha",
      teamName: "Alpha Founders",
      inviteCode: "ALPHA99",
      invitedEmail: "dana@deedox.io",
      invitedRole: "MEMBER",
      invitedByUid: CHARLIE_UID,
      invitedByName: "Charlie",
      status: "PENDING",
      createdAtMillis: Date.now(),
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    bobDb.collection("team_email_invites").doc("inv-dana").set({
      teamId: "team-alpha",
      teamName: "Alpha Founders",
      inviteCode: "ALPHA99",
      invitedEmail: "dana@deedox.io",
      invitedRole: "MEMBER",
      roleTag: "Designer",
      invitedByUid: BOB_UID,
      invitedByName: "Bob",
      status: "PENDING",
      createdAtMillis: Date.now(),
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
  // Dana queries & reads & accepts her invite; Admin Bob lists team invites; Eve cannot query Dana's invite
  await assertSucceeds(
    danaDb.collection("team_email_invites")
      .where("invitedEmail", "==", "dana@deedox.io")
      .where("status", "==", "PENDING")
      .get()
  );
  await assertSucceeds(
    bobDb.collection("team_email_invites")
      .where("teamId", "==", "team-alpha")
      .get()
  );
  await assertFails(
    eveDb.collection("team_email_invites")
      .where("invitedEmail", "==", "dana@deedox.io")
      .where("status", "==", "PENDING")
      .get()
  );
  await assertSucceeds(danaDb.collection("team_email_invites").doc("inv-dana").get());
  await assertFails(eveDb.collection("team_email_invites").doc("inv-dana").get());
  await assertSucceeds(
    danaDb.collection("team_email_invites").doc("inv-dana").update({
      status: "ACCEPTED",
      updatedAt: new Date(),
    })
  );

  // Admin Bob can remove MEMBER Charlie, but CANNOT remove OWNER Alice
  await assertFails(
    bobDb.collection("teams").doc("team-alpha").update({
      memberUids: [BOB_UID, CHARLIE_UID],
      updatedAt: new Date(),
    })
  );
  await assertFails(
    bobDb.collection("teams").doc("team-alpha").collection("members").doc(ALICE_UID).delete()
  );
  await assertSucceeds(
    bobDb.collection("teams").doc("team-alpha").update({
      memberUids: [ALICE_UID, BOB_UID],
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    bobDb.collection("teams").doc("team-alpha").collection("members").doc(CHARLIE_UID).delete()
  );
});

test("Business OS extended startup project fields (projectType, crmDealsJson, okrsJson, sopDocsJson, sharedTeamId) succeed for owner", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();
  const startupRef = aliceDb.collection("users").doc(ALICE_UID).collection("startups").doc("biz-1");

  await assertSucceeds(
    startupRef.set({
      userId: ALICE_UID,
      name: "Deedox Agency OS",
      tagline: "Productized AI studio",
      stage: "BETA",
      projectType: "AGENCY",
      phasesJson: "[]",
      sprintTasksJson: "[]",
      crmDealsJson: "[{\"id\":\"d1\",\"clientName\":\"Acme\",\"dealValueCents\":250000,\"stage\":\"ACTIVE_CLIENT\"}]",
      okrsJson: "[{\"id\":\"o1\",\"objective\":\"Revenue\",\"keyResult\":\"$10k MRR\",\"currentValue\":4000,\"targetValue\":10000,\"unitLabel\":\"USD\"}]",
      sopDocsJson: "[{\"id\":\"s1\",\"title\":\"Client Onboarding\",\"templateType\":\"SOP\",\"content\":\"Kickoff\"}]",
      sharedTeamId: "team-alpha",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    startupRef.set({
      userId: ALICE_UID,
      name: "Bad Type",
      stage: "BETA",
      projectType: "INVALID_TYPE",
      phasesJson: "[]",
      sprintTasksJson: "[]",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("startups").doc("biz-1").get());
});




