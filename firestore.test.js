const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = "demo-no-project";
let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync("firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8085
    }
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

test("Unauthenticated access rejected", async () => {
  const unauthedDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthedDb.doc("users/alice/cloud_vault/item1").get());
  await assertFails(unauthedDb.doc("users/alice/cloud_vault/item1").set({
    id: "item1",
    userId: "alice"
  }));
});

test("Owner can write and read valid cloud_vault item", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore();
  const validItem = {
    id: "item1",
    userId: "alice",
    title: "Secret Photo",
    mediaType: "PHOTO",
    albumName: "Personal",
    originalFileName: "vacation.jpg",
    fileSizeBytes: 2048,
    encryptionAlgorithm: "AES-256-GCM",
    ivBase64: "dGVzdEl2MTIzNDU2",
    encryptedDataPayload: "ZW5jcnlwdGVkRGF0YQ==",
    checksum: "sha256hash",
    isFavorite: true,
    isDecoy: false,
    createdAt: new Date(),
    updatedAt: new Date()
  };

  await assertSucceeds(aliceDb.doc("users/alice/cloud_vault/item1").set(validItem));
  const snap = await assertSucceeds(aliceDb.doc("users/alice/cloud_vault/item1").get());
  if (!snap.exists) throw new Error("Document should exist");
});

test("Cross-user access rejected", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore();
  const bobDb = testEnv.authenticatedContext("bob").firestore();
  const validItem = {
    id: "item1",
    userId: "alice",
    title: "Secret Photo",
    mediaType: "PHOTO",
    albumName: "Personal",
    originalFileName: "vacation.jpg",
    fileSizeBytes: 2048,
    encryptionAlgorithm: "AES-256-GCM",
    ivBase64: "dGVzdEl2MTIzNDU2",
    encryptedDataPayload: "ZW5jcnlwdGVkRGF0YQ==",
    checksum: "sha256hash",
    isFavorite: false,
    isDecoy: false,
    createdAt: new Date(),
    updatedAt: new Date()
  };

  await assertSucceeds(aliceDb.doc("users/alice/cloud_vault/item1").set(validItem));
  await assertFails(bobDb.doc("users/alice/cloud_vault/item1").get());
  await assertFails(bobDb.doc("users/alice/cloud_vault/item1").delete());
});

test("Invalid schema rejected", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore();
  // Missing required fields
  await assertFails(aliceDb.doc("users/alice/cloud_vault/item2").set({
    id: "item2",
    userId: "alice",
    title: "Incomplete"
  }));
});
