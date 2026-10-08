/*
 * metrics-dashboard.js — EduAI usage dashboard (single self-contained HTML).
 * ------------------------------------------------------------------------------------------------
 * Lives in Eduapp (next to .tools/firebase-ci-token.txt + firebase-cli). Supersedes the thin
 * metrics-retention-dau.js console report for the Chart.js HTML surface.
 *
 * Firestore collections written by ProgressAnalyticsSessionSyncManager:
 *   users                       : { email, createdAt, appName }
 *   sessions/<u>/records        : { sessionStartTime|sessionDate, sessionEndTime, durationMillis }
 *   progress/<u>/records        : { itemType, status, language, completedAt, lastAccessedAt, … }
 *   chapterprogress/<u>/records : { chapterId, language, overallPercentage, status, … }
 *   streak/<u> (or /records)    : { streakCount, … }
 *   analytics/<u>/events        : { eventType, screenName, interactionType, entryTime }
 *
 * Real progress.itemType values (ProgressEntity / ProgressEventTracker):
 *   CONCEPT, MATH_AGENT, SCIENCE_AGENT, REVISION_AGENT, SIMULATION, SIMULATION_AGENT
 *
 * AnalyticsFirestoreMirror.ENABLED = false → click/funnel rows are usually empty (GA4 holds them).
 * DAU / retention / engagement come from sessions + progress + chapterprogress.
 *
 *   node scripts/metrics-dashboard.js                 # console + reports/dashboard.html
 *   node scripts/metrics-dashboard.js --html out.html
 *   node scripts/metrics-dashboard.js --days=30
 *   node scripts/metrics-dashboard.js --utc
 *   node scripts/metrics-dashboard.js --include-internal   # keep team emails (default: exclude)
 *   node scripts/metrics-dashboard.js --mock              # synthetic preview, no token
 *
 * Requires .tools/firebase-ci-token.txt except in --mock mode.
 */

const fs = require("fs");
const path = require("path");

const PROJECT = "eduai-e090e";
const APP_NAME = "eduai_app";
const MOCK = process.argv.includes("--mock");
const INCLUDE_INTERNAL = process.argv.includes("--include-internal");

/** Team accounts — excluded from published metrics unless --include-internal */
const INTERNAL_EMAILS = new Set([
  "jeecounsela@gmail.com",
  "nkb.rgp@gmail.com",
  "check@padaams.in",
]);

/** Matches app orphan-session cleanup / Python dashboard */
const MAX_REPORTED_SESSION_MS = 45 * 60 * 1000;

const STUDY_TYPES = new Set(["CONCEPT", "MATH_AGENT", "REVISION_AGENT", "SCIENCE_AGENT"]);
const SIM_TYPES = new Set(["SIMULATION", "SIMULATION_AGENT"]);
const KNOWN_TYPES = [
  "CONCEPT",
  "MATH_AGENT",
  "SCIENCE_AGENT",
  "REVISION_AGENT",
  "SIMULATION",
  "SIMULATION_AGENT",
];

const DAY_MS = 86400000;
const IST_OFFSET_MS = 5.5 * 3600000;

// ---- day helpers ------------------------------------------------------------------------------
function parseToMs(v) {
  if (v == null) return null;
  if (typeof v === "number") return v;
  if (typeof v === "string") {
    if (/^\d{4}-\d{2}-\d{2}$/.test(v)) return Date.parse(v + "T12:00:00.000Z");
    const n = Date.parse(v);
    return Number.isNaN(n) ? null : n;
  }
  return null;
}
const toDayUTC = (ms) => (ms == null ? null : new Date(ms).toISOString().slice(0, 10));
const toDayIST = (ms) =>
  ms == null ? null : new Date(ms + IST_OFFSET_MS).toISOString().slice(0, 10);

/** Start-of-day epoch ms for a YYYY-MM-DD string in the report timezone. */
function dayStartMs(day, tz) {
  if (!day) return NaN;
  return tz === "UTC"
    ? Date.parse(day + "T00:00:00.000Z")
    : Date.parse(day + "T00:00:00.000+05:30");
}

function normalizeLang(raw) {
  const s = (raw || "en").toLowerCase();
  if (s.startsWith("kn") || s === "kannada") return "kn";
  return "en";
}

function bucketItemType(itemType) {
  const t = (itemType || "OTHER").toUpperCase();
  if (STUDY_TYPES.has(t)) return "STUDY";
  if (SIM_TYPES.has(t)) return "SIMULATION";
  return "OTHER";
}

function resolveClosedSessionMs(startMs, endMs, durationField) {
  if (!startMs) return { ms: null, capped: false, dayClipped: false };
  if (endMs == null && durationField <= 0) return { ms: null, capped: false, dayClipped: false };
  let dur = durationField > 0 ? durationField : 0;
  if (dur <= 0 && endMs && endMs > startMs) dur = endMs - startMs;
  if (dur <= 0) return { ms: null, capped: false, dayClipped: false };
  let capped = false;
  if (dur > MAX_REPORTED_SESSION_MS) {
    dur = MAX_REPORTED_SESSION_MS;
    capped = true;
  }
  // Attribute time only to the IST start day (no spill past midnight) — same as Python dashboard
  const day = toDayIST(startMs);
  let dayClipped = false;
  if (day) {
    const maxInDay = Math.max(0, dayStartMs(day, "IST") + DAY_MS - startMs);
    if (dur > maxInDay) {
      dur = maxInDay;
      dayClipped = true;
    }
  }
  return { ms: dur, capped, dayClipped };
}

function isInternalEmail(email) {
  return INTERNAL_EMAILS.has(String(email || "").toLowerCase());
}

function filterInternal(data, exclude) {
  if (!exclude) {
    return { ...data, excludedInternalUsers: 0 };
  }
  const keep = (email) => !isInternalEmail(email);
  const excludedInternalUsers = data.users.filter((u) => !keep(u.email)).length;
  return {
    users: data.users.filter((u) => keep(u.email)),
    sessions: data.sessions.filter((s) => keep(s.email)),
    progress: data.progress.filter((p) => keep(p.email)),
    chapterprogress: data.chapterprogress.filter((c) => keep(c.email)),
    streaks: data.streaks.filter((s) => keep(s.email)),
    analytics: data.analytics.filter((a) => keep(a.email)),
    excludedInternalUsers,
  };
}

// ---- Firestore REST (real mode) ---------------------------------------------------------------
function makeClient() {
  process.env.NODE_OPTIONS = path.join(__dirname, "fix-firebase-http-agent.js");
  require("./fix-firebase-http-agent.js");
  process.env.FIREBASE_TOKEN = fs
    .readFileSync(path.join(__dirname, "../.tools/firebase-ci-token.txt"), "utf8")
    .trim();
  const auth = require("../.tools/firebase-cli/node_modules/firebase-tools/lib/auth");
  const { Client } = require("../.tools/firebase-cli/node_modules/firebase-tools/lib/apiv2");
  auth.setRefreshToken(process.env.FIREBASE_TOKEN);
  return new Client({ urlPrefix: "https://firestore.googleapis.com", apiVersion: "v1" });
}

function fv(f, key) {
  const v = f?.[key];
  if (!v) return null;
  if (v.stringValue != null) return v.stringValue;
  if (v.integerValue != null) return Number(v.integerValue);
  if (v.doubleValue != null) return Number(v.doubleValue);
  if (v.booleanValue != null) return v.booleanValue;
  return null;
}

const emailFromContainer = (id) => id.replace(`${APP_NAME}_`, "");

async function listDocs(client, pathStr, appScoped) {
  let pageToken;
  const out = [];
  do {
    const res = await client.request({
      method: "GET",
      path: `/projects/${PROJECT}/databases/(default)/documents/${pathStr}`,
      queryParams: { pageSize: "200", showMissing: "true", ...(pageToken ? { pageToken } : {}) },
    });
    for (const d of res.body.documents || []) {
      const id = d.name.split("/").pop();
      if (!appScoped || id.startsWith(`${APP_NAME}_`)) out.push({ id, fields: d.fields || {} });
    }
    pageToken = res.body.nextPageToken;
  } while (pageToken);
  return out;
}

async function fetchAll(client) {
  const usersRaw = (await listDocs(client, "users")).filter(
    (d) => fv(d.fields, "appName") === APP_NAME
  );
  const users = usersRaw.map((u) => ({
    email: (fv(u.fields, "email") || u.id).toLowerCase(),
    createdAt: parseToMs(fv(u.fields, "createdAt")),
  }));

  const sessions = [];
  for (const c of await listDocs(client, "sessions", true)) {
    const email = emailFromContainer(c.id).toLowerCase();
    for (const s of await listDocs(client, `sessions/${c.id}/records`)) {
      const startMs =
        parseToMs(fv(s.fields, "sessionStartTime")) || parseToMs(fv(s.fields, "sessionDate"));
      const endMs = parseToMs(fv(s.fields, "sessionEndTime"));
      const durationField = Number(fv(s.fields, "durationMillis") || 0);
      const { ms, capped, dayClipped } = resolveClosedSessionMs(startMs, endMs, durationField);
      sessions.push({
        email,
        startMs,
        endMs,
        durationMillis: ms, // null = open/orphan (still counts for DAU via startMs)
        rawDurationMillis: durationField,
        capped,
        dayClipped,
        closed: ms != null,
      });
    }
  }

  const progress = [];
  for (const c of await listDocs(client, "progress", true)) {
    const email = emailFromContainer(c.id).toLowerCase();
    for (const p of await listDocs(client, `progress/${c.id}/records`)) {
      const itemType = (fv(p.fields, "itemType") || "OTHER").toUpperCase();
      progress.push({
        email,
        itemType,
        bucket: bucketItemType(itemType),
        status: (fv(p.fields, "status") || "").toUpperCase(),
        language: normalizeLang(fv(p.fields, "language")),
        completedAt: parseToMs(fv(p.fields, "completedAt")),
        ts: parseToMs(
          fv(p.fields, "lastAccessedAt") || fv(p.fields, "updatedAt") || fv(p.fields, "completedAt")
        ),
      });
    }
  }

  const chapterprogress = [];
  for (const c of await listDocs(client, "chapterprogress", true)) {
    const email = emailFromContainer(c.id).toLowerCase();
    for (const cp of await listDocs(client, `chapterprogress/${c.id}/records`)) {
      chapterprogress.push({
        email,
        chapterId: fv(cp.fields, "chapterId") || "?",
        language: normalizeLang(fv(cp.fields, "language")),
        overall: Number(fv(cp.fields, "overallPercentage") || 0),
        status: (fv(cp.fields, "status") || "").toUpperCase(),
        completedAt: parseToMs(fv(cp.fields, "completedAt")),
        ts: parseToMs(fv(cp.fields, "updatedAt") || fv(cp.fields, "completedAt")),
      });
    }
  }

  const streaks = [];
  for (const c of await listDocs(client, "streak", true)) {
    const email = emailFromContainer(c.id).toLowerCase();
    const top = c.fields || {};
    if (fv(top, "streakCount") != null) {
      streaks.push({ email, streakCount: Number(fv(top, "streakCount")) });
    }
    try {
      for (const s of await listDocs(client, `streak/${c.id}/records`)) {
        const n = fv(s.fields, "streakCount");
        if (n != null) streaks.push({ email, streakCount: Number(n) });
      }
    } catch (_) {
      /* some users are a single doc with no /records */
    }
  }

  const analytics = [];
  for (const c of await listDocs(client, "analytics", true)) {
    const email = emailFromContainer(c.id).toLowerCase();
    for (const e of await listDocs(client, `analytics/${c.id}/events`)) {
      analytics.push({
        email,
        eventType: fv(e.fields, "eventType"),
        screen: fv(e.fields, "screenName"),
        interactionType: fv(e.fields, "interactionType"),
        ts: parseToMs(fv(e.fields, "entryTime")),
      });
    }
  }

  return { users, sessions, progress, chapterprogress, streaks, analytics };
}

// ---- mock data (realistic itemTypes, includes internal emails to prove filter) ----------------
function genMock() {
  const now = Date.now();
  const rnd = (n) => Math.floor(Math.random() * n);
  const pick = (arr) => arr[rnd(arr.length)];
  const users = [];
  const sessions = [];
  const progress = [];
  const chapterprogress = [];
  const streaks = [];

  const typeWeights = [
    ["CONCEPT", 28],
    ["MATH_AGENT", 18],
    ["SCIENCE_AGENT", 16],
    ["REVISION_AGENT", 8],
    ["SIMULATION", 18],
    ["SIMULATION_AGENT", 12],
  ];
  const typePool = [];
  for (const [t, w] of typeWeights) for (let i = 0; i < w; i++) typePool.push(t);

  const N = 48;
  for (let i = 0; i < N; i++) {
    const email =
      i < 3
        ? [...INTERNAL_EMAILS][i]
        : `student${i}@school.example`;
    const created = now - (3 + rnd(28)) * DAY_MS;
    users.push({ email, createdAt: created });
    const activeDays = 1 + rnd(14);
    for (let d = 0; d < activeDays; d++) {
      const t = created + rnd(28) * DAY_MS + rnd(14) * 3600000;
      if (t > now) continue;
      const rawDur = (1 + rnd(90)) * 60000; // some orphans > 45m
      const endMs = t + rawDur;
      const { ms, capped, dayClipped } = resolveClosedSessionMs(t, endMs, rawDur);
      sessions.push({
        email,
        startMs: t,
        endMs,
        durationMillis: ms,
        rawDurationMillis: rawDur,
        capped,
        dayClipped,
        closed: ms != null,
      });
      const lang = Math.random() < 0.38 ? "kn" : "en";
      const itemType = pick(typePool);
      const done = Math.random() < 0.55;
      progress.push({
        email,
        itemType,
        bucket: bucketItemType(itemType),
        status: done ? "COMPLETED" : "IN_PROGRESS",
        language: lang,
        completedAt: done ? t : null,
        ts: t,
      });
      if (Math.random() < 0.45) {
        const subj = Math.random() < 0.55 ? "science" : "math";
        const overall = pick([20, 40, 60, 80, 100]);
        chapterprogress.push({
          email,
          chapterId: `${subj}_${1 + rnd(8)}`,
          language: lang,
          overall,
          status: overall >= 100 ? "COMPLETED" : "IN_PROGRESS",
          completedAt: overall >= 100 ? t : null,
          ts: t,
        });
      }
    }
    streaks.push({ email, streakCount: rnd(21) });
  }
  return { users, sessions, progress, chapterprogress, streaks, analytics: [] };
}

// ---- aggregation ------------------------------------------------------------------------------
function aggregate(data, opts) {
  const dayFn = opts.dayFn;
  const tz = opts.tz;
  const now = Date.now();

  const activity = new Map();
  const add = (email, ms) => {
    const day = dayFn(ms);
    if (!day) return;
    if (!activity.has(email)) activity.set(email, new Set());
    activity.get(email).add(day);
  };
  data.sessions.forEach((s) => add(s.email, s.startMs));
  data.progress.forEach((p) => add(p.email, p.ts));
  data.chapterprogress.forEach((c) => add(c.email, c.ts));
  data.analytics.forEach((a) => add(a.email, a.ts));

  const dau = new Map();
  for (const [email, days] of activity) {
    for (const d of days) {
      if (!dau.has(d)) dau.set(d, new Set());
      dau.get(d).add(email);
    }
  }
  const sortedDays = [...dau.keys()].sort();
  const recent = sortedDays.slice(-opts.days);
  const sessionsByDay = new Map();
  data.sessions.forEach((s) => {
    const d = dayFn(s.startMs);
    if (d) sessionsByDay.set(d, (sessionsByDay.get(d) || 0) + 1);
  });

  const firstDay = new Map();
  for (const [email, days] of activity) firstDay.set(email, [...days].sort()[0]);
  const dauDays = recent.map((day) => {
    const set = dau.get(day) || new Set();
    let nu = 0;
    for (const e of set) if (firstDay.get(e) === day) nu++;
    return {
      day,
      users: set.size,
      sessions: sessionsByDay.get(day) || 0,
      newU: nu,
      returning: set.size - nu,
    };
  });

  const todayStr = dayFn(now);
  const within = (email, n) => {
    const days = activity.get(email);
    if (!days) return false;
    for (const d of days) {
      const start = dayStartMs(d, tz);
      if (!Number.isNaN(start) && now - start <= n * DAY_MS) return true;
    }
    return false;
  };
  const emails = [...activity.keys()];
  const wau = emails.filter((e) => within(e, 7)).length;
  const mau = emails.filter((e) => within(e, 30)).length;
  const todayDau = dau.get(todayStr)?.size || 0;

  const closed = data.sessions.filter((s) => s.closed && s.durationMillis > 0);
  const cappedCount = closed.filter((s) => s.capped).length;
  const dayClippedCount = closed.filter((s) => s.dayClipped).length;
  const openSkipped = data.sessions.filter((s) => !s.closed).length;
  const durations = closed.map((s) => s.durationMillis);
  const avgSessionMin = durations.length
    ? durations.reduce((a, b) => a + b, 0) / durations.length / 60000
    : 0;

  // Completions: status must be COMPLETED (do not treat completedAt alone as done)
  const completed = data.progress.filter((p) => p.status === "COMPLETED");
  const byBucket = { STUDY: 0, SIMULATION: 0, OTHER: 0 };
  const byType = Object.fromEntries(KNOWN_TYPES.map((t) => [t, 0]));
  byType.OTHER = 0;
  const byLang = { en: 0, kn: 0 };
  completed.forEach((p) => {
    byBucket[p.bucket] = (byBucket[p.bucket] || 0) + 1;
    if (KNOWN_TYPES.includes(p.itemType)) byType[p.itemType]++;
    else byType.OTHER++;
    byLang[p.language] = (byLang[p.language] || 0) + 1;
  });

  const chapterCompleted = new Map();
  data.chapterprogress.forEach((c) => {
    if (c.status === "COMPLETED" || c.overall >= 100) {
      chapterCompleted.set(c.chapterId, (chapterCompleted.get(c.chapterId) || 0) + 1);
    }
  });
  const topChapters = [...chapterCompleted.entries()].sort((a, b) => b[1] - a[1]).slice(0, 12);

  const buckets = { "0": 0, "1-2": 0, "3-6": 0, "7-13": 0, "14+": 0 };
  const bestByUser = new Map();
  data.streaks.forEach((s) =>
    bestByUser.set(s.email, Math.max(bestByUser.get(s.email) || 0, s.streakCount))
  );
  for (const n of bestByUser.values()) {
    if (n <= 0) buckets["0"]++;
    else if (n <= 2) buckets["1-2"]++;
    else if (n <= 6) buckets["3-6"]++;
    else if (n <= 13) buckets["7-13"]++;
    else buckets["14+"]++;
  }

  const cohortByDay = {};
  data.users.forEach((u) => {
    const d = dayFn(u.createdAt);
    if (!d) return;
    (cohortByDay[d] = cohortByDay[d] || []).push(u.email);
  });
  const cohorts = Object.keys(cohortByDay)
    .sort()
    .slice(-14)
    .map((day) => {
      const em = cohortByDay[day];
      const d0 = dayStartMs(day, tz);
      const ret = (off) => {
        const target = dayFn(d0 + off * DAY_MS);
        const a = em.filter((e) => activity.get(e)?.has(target)).length;
        return em.length ? Math.round((a / em.length) * 100) : 0;
      };
      return { day, n: em.length, d1: ret(1), d7: ret(7), d30: ret(30) };
    });

  const clickCounts = new Map();
  data.analytics.forEach((a) => {
    if (a.eventType === "CLICK") {
      const k = `${a.screen}/${a.interactionType || "?"}`;
      clickCounts.set(k, (clickCounts.get(k) || 0) + 1);
    }
  });
  const topClicks = [...clickCounts.entries()].sort((a, b) => b[1] - a[1]).slice(0, 12);

  return {
    generatedAt: new Date().toISOString(),
    tz,
    mock: opts.mock,
    excludeInternal: opts.excludeInternal,
    excludedInternalUsers: data.excludedInternalUsers || 0,
    totalUsers: data.users.length,
    activeUsers: activity.size,
    todayDau,
    wau,
    mau,
    totalSessions: data.sessions.length,
    closedSessions: closed.length,
    openSkipped,
    cappedSessions: cappedCount,
    dayClippedSessions: dayClippedCount,
    avgSessionMin: +avgSessionMin.toFixed(1),
    totalCompleted: completed.length,
    chaptersCompleted: [...chapterCompleted.values()].reduce((a, b) => a + b, 0),
    dauDays,
    byBucket,
    byType,
    byLang,
    topChapters,
    streakBuckets: buckets,
    cohorts,
    topClicks,
  };
}

// ---- HTML render ------------------------------------------------------------------------------
const esc = (s) => String(s).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");

function renderHtml(r) {
  const cohortRows = r.cohorts
    .map(
      (c) =>
        `<tr><td>${c.day}</td><td>${c.n}</td><td>${c.d1}%</td><td>${c.d7}%</td><td>${c.d30}%</td></tr>`
    )
    .join("");
  const chapterRows = r.topChapters
    .map(([id, n]) => `<tr><td>${esc(id)}</td><td>${n}</td></tr>`)
    .join("");
  const clickRows = r.topClicks
    .map(([k, n]) => `<tr><td>${esc(k)}</td><td>${n}</td></tr>`)
    .join("");
  const typeRows = Object.entries(r.byType)
    .filter(([, n]) => n > 0)
    .sort((a, b) => b[1] - a[1])
    .map(([t, n]) => `<tr><td>${esc(t)}</td><td>${n}</td></tr>`)
    .join("");
  const J = (o) => JSON.stringify(o);

  const filterNote = r.excludeInternal
    ? `Internal team excluded (${r.excludedInternalUsers} registered). Pass <code>--include-internal</code> to include.`
    : "Internal team included (--include-internal).";

  return `<!DOCTYPE html><html lang="en"><head><meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1"/>
<title>EduAI Metrics Dashboard</title>
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"></script>
<style>
 body{font-family:system-ui,sans-serif;margin:24px;background:#f6f7fb;color:#1a1a2e}
 h1{margin:0 0 2px} .sub{color:#666;margin-bottom:12px;font-size:14px}
 .banner{background:#fff6e0;border:1px solid #f0d488;color:#8a6d0f;padding:8px 14px;border-radius:8px;margin-bottom:12px;font-size:13px}
 .meta{background:#eef2ff;border:1px solid #c7d2fe;color:#3730a3;padding:8px 14px;border-radius:8px;margin-bottom:18px;font-size:13px;line-height:1.45}
 .grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(140px,1fr));gap:12px;margin-bottom:26px}
 .card{background:#fff;border-radius:10px;padding:16px;box-shadow:0 1px 4px rgba(0,0,0,.08)}
 .card .val{font-size:26px;font-weight:700} .card .lbl{font-size:12px;color:#666;margin-top:4px}
 .charts{display:grid;grid-template-columns:repeat(auto-fit,minmax(340px,1fr));gap:18px;margin-bottom:26px}
 .panel{background:#fff;border-radius:10px;padding:16px;box-shadow:0 1px 4px rgba(0,0,0,.08)}
 .panel h2{margin:0 0 12px;font-size:15px}
 table{width:100%;border-collapse:collapse;background:#fff;border-radius:10px;overflow:hidden;box-shadow:0 1px 4px rgba(0,0,0,.08);margin-bottom:24px}
 th,td{padding:9px 13px;text-align:left;border-bottom:1px solid #eee;font-size:14px} th{background:#eef1ff;font-size:12px}
 h2.sec{margin:28px 0 10px;font-size:17px} .note{font-size:12px;color:#666;max-width:820px;line-height:1.5}
</style></head><body>
<h1>EduAI Metrics Dashboard</h1>
<p class="sub">Generated ${esc(r.generatedAt)} · ${esc(r.tz)} · Firebase ${PROJECT}</p>
${r.mock ? '<div class="banner">MOCK DATA — synthetic preview with real itemTypes (CONCEPT, MATH_AGENT, …). Run without <code>--mock</code> for live Firestore numbers.</div>' : ""}
<div class="meta">
 ${filterNote}<br/>
 Sessions: ${r.closedSessions} closed (avg ${r.avgSessionMin}m, capped at 45m: ${r.cappedSessions}, day-clipped: ${r.dayClippedSessions}); ${r.openSkipped} open/orphan skipped for duration.<br/>
 Completions require <code>status=COMPLETED</code>. Study bucket = CONCEPT + agents; Simulation = SIMULATION + SIMULATION_AGENT.
</div>

<div class="grid">
 <div class="card"><div class="val">${r.totalUsers}</div><div class="lbl">Registered users</div></div>
 <div class="card"><div class="val">${r.activeUsers}</div><div class="lbl">Users with activity</div></div>
 <div class="card"><div class="val">${r.todayDau}</div><div class="lbl">DAU today</div></div>
 <div class="card"><div class="val">${r.wau}</div><div class="lbl">WAU (7d)</div></div>
 <div class="card"><div class="val">${r.mau}</div><div class="lbl">MAU (30d)</div></div>
 <div class="card"><div class="val">${r.totalSessions}</div><div class="lbl">Session starts</div></div>
 <div class="card"><div class="val">${r.avgSessionMin}m</div><div class="lbl">Avg closed session</div></div>
 <div class="card"><div class="val">${r.totalCompleted}</div><div class="lbl">Items completed</div></div>
 <div class="card"><div class="val">${r.chaptersCompleted}</div><div class="lbl">Chapter completions</div></div>
</div>

<div class="charts">
 <div class="panel"><h2>DAU — last ${r.dauDays.length} days</h2><canvas id="cDau"></canvas></div>
 <div class="panel"><h2>New vs returning</h2><canvas id="cNvr"></canvas></div>
 <div class="panel"><h2>Completions by bucket</h2><canvas id="cBucket"></canvas></div>
 <div class="panel"><h2>Completions by itemType</h2><canvas id="cType"></canvas></div>
 <div class="panel"><h2>Language split (EN vs KN)</h2><canvas id="cLang"></canvas></div>
 <div class="panel"><h2>Streak distribution</h2><canvas id="cStreak"></canvas></div>
</div>

<h2 class="sec">Completions by itemType</h2>
<table><thead><tr><th>itemType</th><th>Count</th></tr></thead>
<tbody>${typeRows || "<tr><td colspan=2>No completions</td></tr>"}</tbody></table>

<h2 class="sec">Retention by signup cohort</h2>
<table><thead><tr><th>Cohort</th><th>Users</th><th>D1</th><th>D7</th><th>D30</th></tr></thead>
<tbody>${cohortRows || "<tr><td colspan=5>No cohorts</td></tr>"}</tbody></table>

<h2 class="sec">Top completed chapters</h2>
<table><thead><tr><th>chapterId</th><th>Completions</th></tr></thead>
<tbody>${chapterRows || "<tr><td colspan=2>No chapter completions</td></tr>"}</tbody></table>

<h2 class="sec">Top click types (Firestore analytics)</h2>
<p class="note">Usually empty: AnalyticsFirestoreMirror.ENABLED = false — clicks/funnels go to GA4.</p>
<table><thead><tr><th>screen / interaction</th><th>Count</th></tr></thead>
<tbody>${clickRows || "<tr><td colspan=2>No Firestore click events (expected — see GA4)</td></tr>"}</tbody></table>

<p class="note">Re-run from Eduapp: <code>node scripts/metrics-dashboard.js --html reports/dashboard.html</code>
 · <code>--days=N</code> · <code>--utc</code> · <code>--include-internal</code> · <code>--mock</code></p>

<script>
const dau=${J(r.dauDays)}, byBucket=${J(r.byBucket)}, byType=${J(r.byType)}, byLang=${J(r.byLang)}, streak=${J(r.streakBuckets)};
const g=(id)=>document.getElementById(id).getContext('2d');
new Chart(g('cDau'),{type:'line',data:{labels:dau.map(d=>d.day),datasets:[{label:'DAU',data:dau.map(d=>d.users),borderColor:'#4c6ef5',backgroundColor:'rgba(76,110,245,.15)',fill:true,tension:.3}]},options:{plugins:{legend:{display:false}}}});
new Chart(g('cNvr'),{type:'bar',data:{labels:dau.map(d=>d.day),datasets:[{label:'New',data:dau.map(d=>d.newU),backgroundColor:'#38b000'},{label:'Returning',data:dau.map(d=>d.returning),backgroundColor:'#4c6ef5'}]},options:{scales:{x:{stacked:true},y:{stacked:true}}}});
new Chart(g('cBucket'),{type:'bar',data:{labels:Object.keys(byBucket),datasets:[{data:Object.values(byBucket),backgroundColor:['#4c6ef5','#f59f00','#adb5bd']}]},options:{plugins:{legend:{display:false}}}});
const typeLabels=Object.keys(byType).filter(k=>byType[k]>0);
new Chart(g('cType'),{type:'bar',data:{labels:typeLabels,datasets:[{data:typeLabels.map(k=>byType[k]),backgroundColor:['#4c6ef5','#748ffc','#91a7ff','#bac8ff','#f59f00','#fab005','#adb5bd']}]},options:{plugins:{legend:{display:false}}}});
new Chart(g('cLang'),{type:'doughnut',data:{labels:['English','Kannada'],datasets:[{data:[byLang.en||0,byLang.kn||0],backgroundColor:['#4c6ef5','#e8590c']}]}});
new Chart(g('cStreak'),{type:'bar',data:{labels:Object.keys(streak),datasets:[{label:'Users',data:Object.values(streak),backgroundColor:'#7048e8'}]},options:{plugins:{legend:{display:false}}}});
</script>
</body></html>`;
}

// ---- main -------------------------------------------------------------------------------------
async function main() {
  const args = process.argv.slice(2);
  const tz = args.includes("--utc") ? "UTC" : "IST";
  const dayFn = tz === "UTC" ? toDayUTC : toDayIST;
  const daysArg = args.find((a) => a.startsWith("--days="));
  const days = daysArg ? Number(daysArg.split("=")[1]) : 30;
  const htmlIdx = args.indexOf("--html");
  const htmlOut =
    htmlIdx >= 0 ? args[htmlIdx + 1] : path.join(__dirname, "../reports/dashboard.html");
  const excludeInternal = !INCLUDE_INTERNAL;

  let raw = MOCK ? genMock() : await fetchAll(makeClient());
  const data = filterInternal(raw, excludeInternal);
  const r = aggregate(data, { dayFn, tz, days, mock: MOCK, excludeInternal });

  console.log(`=== EduAI metrics (${tz}${MOCK ? ", MOCK" : ""}${excludeInternal ? ", external-only" : ", +internal"}) ===`);
  console.log(
    `users=${r.totalUsers} active=${r.activeUsers} DAU=${r.todayDau} WAU=${r.wau} MAU=${r.mau} (excludedInternal=${r.excludedInternalUsers})`
  );
  console.log(
    `sessions=${r.totalSessions} closed=${r.closedSessions} openSkipped=${r.openSkipped} capped45m=${r.cappedSessions} dayClipped=${r.dayClippedSessions} avg=${r.avgSessionMin}m`
  );
  console.log(
    `completed=${r.totalCompleted} chapters=${r.chaptersCompleted} byBucket=${JSON.stringify(r.byBucket)}`
  );
  console.log(`byType=${JSON.stringify(r.byType)} byLang=${JSON.stringify(r.byLang)}`);

  const outPath = path.resolve(htmlOut);
  fs.mkdirSync(path.dirname(outPath), { recursive: true });
  fs.writeFileSync(outPath, renderHtml(r), "utf8");
  console.log(`\nHTML dashboard written: ${outPath}`);
}

main().catch((e) => {
  console.error("Error:", e.message);
  process.exit(1);
});
