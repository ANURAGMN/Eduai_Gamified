#!/usr/bin/env node
/**
 * Collect Firestore snapshot for metrics dashboard using firebase-tools auth
 * (user login / ADC — not FIREBASE_TOKEN CI refresh token).
 *
 * Usage: node scripts/collect-firestore-snapshot.js [out-json]
 */

const fs = require("fs");
const path = require("path");

const PROJECT = "eduai-e090e";
const APP = "eduai_app";
const ROOT = path.resolve(__dirname, "..");
const FIREBASE_TOOLS = path.join(ROOT, ".tools", "firebase-cli", "node_modules", "firebase-tools");

process.chdir(ROOT);
delete process.env.FIREBASE_TOKEN;

const { requireAuth } = require(path.join(FIREBASE_TOOLS, "lib", "requireAuth"));
const { Client } = require(path.join(FIREBASE_TOOLS, "lib", "apiv2"));

const BASE = `projects/${PROJECT}/databases/(default)/documents`;

async function listAll(client, parent, collectionId) {
  const docs = [];
  let pageToken;
  do {
    const qs = new URLSearchParams({ pageSize: "100", showMissing: "true" });
    if (pageToken) qs.set("pageToken", pageToken);
    const suffix = collectionId
      ? `/${parent}/${collectionId}?${qs}`
      : `/${parent}?${qs}`;
    const res = await client.get(suffix);
    if (res.body.documents) docs.push(...res.body.documents);
    pageToken = res.body.nextPageToken;
  } while (pageToken);
  return docs;
}

async function listSub(client, parentPath, sub) {
  return listAll(client, parentPath, sub);
}

function docId(name) {
  return name.split("/").pop();
}

async function main() {
  const outPath =
    process.argv[2] ||
    path.join(ROOT, "reports", "firestore-snapshot.json");

  const options = { project: PROJECT, cwd: ROOT };
  await requireAuth(options);
  const client = new Client({
    urlPrefix: "https://firestore.googleapis.com/v1",
    auth: true,
  });

  console.error("Loading Concept catalog...");
  const concepts = await listAll(client, BASE, "Concept");

  console.error("Loading users...");
  const users = await listAll(client, BASE, "users");

  console.error("Loading session containers...");
  const sessionContainers = (await listAll(client, BASE, "sessions")).filter((d) =>
    docId(d.name).startsWith(`${APP}_`)
  );

  console.error("Loading analytics containers...");
  const analyticsContainers = (await listAll(client, BASE, "analytics")).filter((d) =>
    docId(d.name).startsWith(`${APP}_`)
  );

  console.error("Loading progress containers...");
  const progressContainers = (await listAll(client, BASE, "progress")).filter((d) =>
    docId(d.name).startsWith(`${APP}_`)
  );

  const sessions = {};
  for (const c of sessionContainers) {
    const id = docId(c.name);
    process.stderr.write(`  sessions/${id}\n`);
    sessions[id] = await listSub(client, `sessions/${encodeURIComponent(id)}`, "records");
  }

  const analytics = {};
  for (const c of analyticsContainers) {
    const id = docId(c.name);
    process.stderr.write(`  analytics/${id}\n`);
    analytics[id] = await listSub(client, `analytics/${encodeURIComponent(id)}`, "events");
  }

  const progress = {};
  for (const c of progressContainers) {
    const id = docId(c.name);
    process.stderr.write(`  progress/${id}\n`);
    progress[id] = await listSub(client, `progress/${encodeURIComponent(id)}`, "records");
  }

  const snapshot = {
    collectedAt: new Date().toISOString(),
    project: PROJECT,
    concepts,
    users,
    sessions,
    analytics,
    progress,
  };

  fs.mkdirSync(path.dirname(outPath), { recursive: true });
  fs.writeFileSync(outPath, JSON.stringify(snapshot));
  console.error(`Snapshot written: ${outPath}`);
  console.log(outPath);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
