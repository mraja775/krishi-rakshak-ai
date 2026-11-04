// Security rules tests. Run: cd tests/rules && npm install && npm test
import {test, before, after, beforeEach} from "node:test";
import {readFileSync} from "node:fs";
import {
  initializeTestEnvironment, assertSucceeds, assertFails,
} from "@firebase/rules-unit-testing";
import {doc, getDoc, setDoc, updateDoc, deleteDoc, getDocs, collection}
  from "firebase/firestore";
import {ref, uploadBytes, getBytes} from "firebase/storage";

const PROJECT = "demo-krishi-rakshak";
const ALICE = "alice";
const BOB = "bob";
const ALICE_PHONE = "+919800000001";
const BUCKET = `${PROJECT}.appspot.com`;

let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId: PROJECT,
    firestore: {rules: readFileSync("../../firestore.rules", "utf8"),
      host: "127.0.0.1", port: 8080},
    storage: {rules: readFileSync("../../storage.rules", "utf8"),
      host: "127.0.0.1", port: 9199},
  });
});
after(async () => env?.cleanup());
beforeEach(async () => {
  await env.clearFirestore();
  await env.clearStorage();
});

const alice = () => env.authenticatedContext(ALICE,
  {phone_number: ALICE_PHONE});
const bob = () => env.authenticatedContext(BOB,
  {phone_number: "+919800000002"});
const anon = () => env.unauthenticatedContext();

const imageUrl = (uid) =>
  `https://firebasestorage.googleapis.com/v0/b/${BUCKET}/o/images%2F${uid}%2F123_compressed_x.jpg?alt=media&token=abc`;

// Exactly what the Android app writes (AnalysisRepositoryImpl.createAnalysis).
const pendingAnalysis = (id, uid) => ({
  id, userId: uid, imageUrl: imageUrl(uid), plantType: null,
  confidence: 0.0, recommendations: [], timestamp: Date.now(),
  status: "PENDING",
});

// Exactly what the Android app writes (AuthRepositoryImpl.saveUserToFirestore).
const userDoc = () => ({
  phoneNumber: ALICE_PHONE, name: null,
  createdAt: Date.now(), lastLogin: Date.now(),
});

async function seed(fn) {
  await env.withSecurityRulesDisabled(async (ctx) => fn(ctx.firestore()));
}

// ---------- users ----------
test("users: owner can create/read/update own profile (app flow)", async () => {
  const db = alice().firestore();
  await assertSucceeds(setDoc(doc(db, "users", ALICE), userDoc()));
  await assertSucceeds(getDoc(doc(db, "users", ALICE)));
  await assertSucceeds(updateDoc(doc(db, "users", ALICE), {name: "Ravi"}));
  await assertSucceeds(updateDoc(doc(db, "users", ALICE),
    {lastLogin: Date.now()}));
  await assertSucceeds(deleteDoc(doc(db, "users", ALICE)));
});

test("users: UNAUTHENTICATED cannot read or write (was open before)", async () => {
  await seed((db) => setDoc(doc(db, "users", ALICE), userDoc()));
  const db = anon().firestore();
  await assertFails(getDoc(doc(db, "users", ALICE)));
  await assertFails(getDocs(collection(db, "users")));
  await assertFails(setDoc(doc(db, "users", ALICE), userDoc()));
});

test("users: other user cannot read/write my profile", async () => {
  await seed((db) => setDoc(doc(db, "users", ALICE), userDoc()));
  const db = bob().firestore();
  await assertFails(getDoc(doc(db, "users", ALICE)));
  await assertFails(updateDoc(doc(db, "users", ALICE), {name: "x"}));
});

test("users: cannot spoof phone number or add extra fields", async () => {
  const db = alice().firestore();
  await assertFails(setDoc(doc(db, "users", ALICE),
    {...userDoc(), phoneNumber: "+910000000000"}));
  await assertFails(setDoc(doc(db, "users", ALICE),
    {...userDoc(), isAdmin: true}));
});

// ---------- analyses ----------
test("analyses: owner can create PENDING, read and delete (app flow)", async () => {
  const db = alice().firestore();
  await assertSucceeds(setDoc(doc(db, "analyses", "a1"),
    pendingAnalysis("a1", ALICE)));
  await assertSucceeds(getDoc(doc(db, "analyses", "a1")));
  await assertSucceeds(deleteDoc(doc(db, "analyses", "a1")));
});

test("analyses: UNAUTHENTICATED cannot update results (was open before)", async () => {
  await seed((db) => setDoc(doc(db, "analyses", "a1"),
    pendingAnalysis("a1", ALICE)));
  const db = anon().firestore();
  await assertFails(updateDoc(doc(db, "analyses", "a1"),
    {status: "COMPLETED", plantType: "Fake"}));
  await assertFails(getDoc(doc(db, "analyses", "a1")));
});

test("analyses: owner cannot forge results or update", async () => {
  const db = alice().firestore();
  await assertFails(setDoc(doc(db, "analyses", "a2"),
    {...pendingAnalysis("a2", ALICE), status: "COMPLETED"}));
  await assertFails(setDoc(doc(db, "analyses", "a3"),
    {...pendingAnalysis("a3", ALICE), plantType: "Tomato"}));
  await assertFails(setDoc(doc(db, "analyses", "a4"),
    {...pendingAnalysis("a4", ALICE), diseaseInfo: {name: "x"}}));
  await seed((d) => setDoc(doc(d, "analyses", "a1"),
    pendingAnalysis("a1", ALICE)));
  await assertFails(updateDoc(doc(db, "analyses", "a1"),
    {status: "COMPLETED"}));
});

test("analyses: cannot create for another user or point at their image", async () => {
  const db = alice().firestore();
  await assertFails(setDoc(doc(db, "analyses", "b1"),
    pendingAnalysis("b1", BOB)));
  await assertFails(setDoc(doc(db, "analyses", "b2"),
    {...pendingAnalysis("b2", ALICE), imageUrl: imageUrl(BOB)}));
  await assertFails(setDoc(doc(db, "analyses", "b3"),
    {...pendingAnalysis("b3", ALICE), id: "other"}));
});

test("analyses: other user cannot read or delete mine", async () => {
  await seed((d) => setDoc(doc(d, "analyses", "a1"),
    pendingAnalysis("a1", ALICE)));
  const db = bob().firestore();
  await assertFails(getDoc(doc(db, "analyses", "a1")));
  await assertFails(deleteDoc(doc(db, "analyses", "a1")));
});

// ---------- reference data ----------
test("crops/diseases/treatments: read-only, nobody can write", async () => {
  for (const c of ["crops", "diseases", "treatments"]) {
    await assertSucceeds(getDoc(doc(alice().firestore(), c, "x")));
    await assertFails(getDoc(doc(anon().firestore(), c, "x")));
    await assertFails(setDoc(doc(anon().firestore(), c, "x"), {a: 1}));
    await assertFails(setDoc(doc(alice().firestore(), c, "x"), {a: 1}));
  }
});

// ---------- storage ----------
const jpeg = (n) => new Uint8Array(n);

test("storage: owner can upload small jpeg and read it (app flow)", async () => {
  const s = alice().storage(`gs://${BUCKET}`);
  const r = ref(s, `images/${ALICE}/123_compressed_x.jpg`);
  await assertSucceeds(uploadBytes(r, jpeg(1000), {contentType: "image/jpeg"}));
  await assertSucceeds(getBytes(r));
});

test("storage: rejects >5MB, non-images, other users' folders, anon", async () => {
  const s = alice().storage(`gs://${BUCKET}`);
  await assertFails(uploadBytes(ref(s, `images/${ALICE}/big.jpg`),
    jpeg(6 * 1024 * 1024), {contentType: "image/jpeg"}));
  await assertFails(uploadBytes(ref(s, `images/${ALICE}/x.html`),
    jpeg(10), {contentType: "text/html"}));
  await assertFails(uploadBytes(ref(s, `images/${BOB}/x.jpg`),
    jpeg(10), {contentType: "image/jpeg"}));
  const a = anon().storage(`gs://${BUCKET}`);
  await assertFails(uploadBytes(ref(a, `public/x.jpg`),
    jpeg(10), {contentType: "image/jpeg"}));
  await assertFails(uploadBytes(ref(a, `analysis-results/${ALICE}/x.json`),
    jpeg(10), {contentType: "application/json"}));
});
