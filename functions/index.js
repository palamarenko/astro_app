const { onRequest }  = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { defineSecret } = require("firebase-functions/params");
const { initializeApp }  = require("firebase-admin/app");
const { getMessaging }   = require("firebase-admin/messaging");
const { getFirestore }   = require("firebase-admin/firestore");

initializeApp();

const ADMIN_SECRET   = defineSecret("ADMIN_SECRET");
const CLAUDE_API_KEY = defineSecret("CLAUDE_API_KEY");

const SCHEDULE_DOC    = "admin_config/push_schedule";
const PROMPT_DOC      = "admin_config/horoscope_prompt";
const FIREBASE_DB_URL = "https://zodiac-b23ce-default-rtdb.europe-west1.firebasedatabase.app";

const SIGNS = [
  { id: "aries",       name: "Aries",       element: "Fire",  planet: "Mars"    },
  { id: "taurus",      name: "Taurus",      element: "Earth", planet: "Venus"   },
  { id: "gemini",      name: "Gemini",      element: "Air",   planet: "Mercury" },
  { id: "cancer",      name: "Cancer",      element: "Water", planet: "Moon"    },
  { id: "leo",         name: "Leo",         element: "Fire",  planet: "Sun"     },
  { id: "virgo",       name: "Virgo",       element: "Earth", planet: "Mercury" },
  { id: "libra",       name: "Libra",       element: "Air",   planet: "Venus"   },
  { id: "scorpio",     name: "Scorpio",     element: "Water", planet: "Pluto"   },
  { id: "sagittarius", name: "Sagittarius", element: "Fire",  planet: "Jupiter" },
  { id: "capricorn",   name: "Capricorn",   element: "Earth", planet: "Saturn"  },
  { id: "aquarius",    name: "Aquarius",    element: "Air",   planet: "Uranus"  },
  { id: "pisces",      name: "Pisces",      element: "Water", planet: "Neptune" },
];

const LANGUAGES = ["ru", "uk", "en"];

// Default prompt template. Variables: {date}, {lang}
// The signs list is injected automatically by the function.
// Style instructions stored in Firestore — only the creative/tone part.
// The technical wrapper (signs list, JSON schema, date, language) is always hardcoded.
const DEFAULT_STYLE =
  "Style: poetic, inspiring, mystical.\n" +
  "Each sign text: 3-4 sentences that reflect the score levels — " +
  "low scores (50-65) hint at caution or challenges, " +
  "medium (66-80) suggest steady progress, " +
  "high (81-100) radiate optimism and success.\n" +
  "Keyword: 1-2 evocative words capturing the day's energy for that sign.";

// ─── Helpers ──────────────────────────────────────────────────────────────────

function utcDateKey(offsetDays) {
  const d = new Date();
  d.setUTCDate(d.getUTCDate() + (offsetDays || 0));
  return d.toISOString().split("T")[0];
}

function extractJson(raw) {
  const s = raw.trim();
  if (!s.startsWith("`")) return s;
  return s.replace(/^```json\n?/, "").replace(/^```\n?/, "").replace(/\n?```$/, "").trim();
}

async function callClaude(prompt, apiKey, maxTokens) {
  const resp = await fetch("https://api.anthropic.com/v1/messages", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "x-api-key": apiKey,
      "anthropic-version": "2023-06-01",
    },
    body: JSON.stringify({
      model: "claude-haiku-4-5-20251001",
      max_tokens: maxTokens || 3000,
      messages: [{ role: "user", content: prompt }],
    }),
  });
  if (!resp.ok) throw new Error("Claude " + resp.status + ": " + (await resp.text()));
  const data = await resp.json();
  return data.content[0].text;
}

async function saveToDb(lang, period, dateKey, signId, data) {
  const periodPath = (period === "weekly" || period === "monthly") ? period : "daily";
  const url = FIREBASE_DB_URL + "/horoscopes/" + lang + "/" + periodPath + "/" + dateKey + "/" + signId + ".json";
  const resp = await fetch(url, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });
  if (!resp.ok) throw new Error("Firebase DB " + resp.status);
}

async function getStyleInstructions(db) {
  try {
    const doc = await db.doc(PROMPT_DOC).get();
    return (doc.exists && doc.data().prompt) ? doc.data().prompt : DEFAULT_STYLE;
  } catch (e) {
    return DEFAULT_STYLE;
  }
}

// Build the signs description string for injection into prompt
function buildSignsDesc() {
  return SIGNS.map(function(s) {
    return s.id + " (" + s.name + ", " + s.element + ", " + s.planet + ")";
  }).join("; ");
}

// Generate horoscopes for ALL 12 signs in a single Claude request for one language
async function generateAllForLang(lang, period, dateKey, apiKey, styleInstructions) {
  const langName = lang === "uk" ? "Ukrainian" : lang === "en" ? "English" : "Russian";
  const signsDesc = buildSignsDesc();
  const prompt =
    "Generate daily horoscopes for all 12 zodiac signs for the day (" + dateKey + ").\n" +
    "Language: " + langName + ".\n\n" +
    styleInstructions + "\n\n" +
    "Signs and their traits:\n" + signsDesc + "\n\n" +
    "Respond ONLY with valid JSON object, no markdown, no extra text:\n" +
    "{\"aries\":{\"text\":\"...\",\"keyword\":\"1-2 words\",\"love\":72,\"career\":85,\"health\":60,\"energy\":78}," +
    "\"taurus\":{...},\"gemini\":{...},\"cancer\":{...},\"leo\":{...},\"virgo\":{...}," +
    "\"libra\":{...},\"scorpio\":{...},\"sagittarius\":{...},\"capricorn\":{...}," +
    "\"aquarius\":{...},\"pisces\":{...}}";

  const raw = await callClaude(prompt, apiKey, 3000);
  const parsed = JSON.parse(extractJson(raw));

  // Validate all 12 signs are present
  const missing = SIGNS.filter(function(s) { return !parsed[s.id]; });
  if (missing.length > 0) {
    throw new Error("Missing signs: " + missing.map(function(s) { return s.id; }).join(", "));
  }
  return parsed; // { aries: {text,keyword,love,career,health,energy}, ... }
}

// Generate for one date: 3 Claude requests (one per language), each returning all 12 signs
async function generateForDate(dateKey, apiKey, styleInstructions, period) {
  const periodVal = period || "daily";
  let success = 0, failed = 0;
  const errors = [];

  for (var li = 0; li < LANGUAGES.length; li++) {
    const lang = LANGUAGES[li];
    const MAX_RETRIES = 8;
    var lastErr = null;

    for (var attempt = 1; attempt <= MAX_RETRIES; attempt++) {
      try {
        const horoscopes = await generateAllForLang(lang, periodVal, dateKey, apiKey, styleInstructions);
        // Save all 12 signs in parallel
        await Promise.all(SIGNS.map(function(sign) {
          return saveToDb(lang, period, dateKey, sign.id, horoscopes[sign.id]);
        }));
        success += 12;
        console.log("OK " + dateKey + " " + lang + " (all 12 signs)");
        break;
      } catch (e) {
        lastErr = e;
        console.warn("Attempt " + attempt + "/" + MAX_RETRIES + " failed for " + lang + ": " + e.message);
        if (attempt < MAX_RETRIES) {
          await new Promise(function(r) { setTimeout(r, Math.min(2000 * attempt, 10000)); });
        }
        if (attempt === MAX_RETRIES) {
          failed += 12;
          errors.push(lang + ": " + lastErr.message);
          console.error("FAIL all retries exhausted for " + lang);
        }
      }
    }
  }

  return { success: success, failed: failed, errors: errors };
}

function offsetToTopic(offset) {
  if (offset === 0) return "tz_0";
  return offset > 0 ? "tz_p" + offset : "tz_n" + Math.abs(offset);
}

// ─── adminApi ─────────────────────────────────────────────────────────────────

exports.adminApi = onRequest(
  {
    cors: true,
    invoker: "public",
    secrets: [ADMIN_SECRET, CLAUDE_API_KEY],
    timeoutSeconds: 540,
    memory: "512MiB",
  },
  async function(req, res) {
    if (req.method !== "POST") { res.status(405).json({ error: "Method Not Allowed" }); return; }
    const secret = req.headers["x-admin-secret"];
    if (!secret || secret !== ADMIN_SECRET.value()) { res.status(401).json({ error: "Unauthorized" }); return; }

    const action = req.body && req.body.action;
    const db = getFirestore();
    try {

      if (action === "sendPush") {
        const type = (req.body && req.body.type) ? req.body.type : "daily_horoscope";
        await getMessaging().send({ topic: "horoscope_daily", data: { type: type } });
        res.json({ success: true });

      } else if (action === "getSchedule") {
        const doc = await db.doc(SCHEDULE_DOC).get();
        res.json({ localHours: doc.exists ? (doc.data().localHours || []) : [] });

      } else if (action === "setSchedule") {
        const localHours = (req.body && Array.isArray(req.body.localHours)) ? req.body.localHours : [];
        await db.doc(SCHEDULE_DOC).set({ localHours: localHours, updatedAt: Date.now() });
        res.json({ success: true });

      } else if (action === "getPrompt") {
        const doc = await db.doc(PROMPT_DOC).get();
        const prompt = (doc.exists && doc.data().prompt) ? doc.data().prompt : DEFAULT_STYLE;
        res.json({ prompt: prompt, isDefault: !doc.exists || !doc.data().prompt });

      } else if (action === "setPrompt") {
        const prompt = (req.body && req.body.prompt) ? req.body.prompt : DEFAULT_STYLE;
        await db.doc(PROMPT_DOC).set({ prompt: prompt, updatedAt: Date.now() });
        res.json({ success: true });

      } else if (action === "generateHoroscopes") {
        const dateKey = (req.body && req.body.date) ? req.body.date : utcDateKey(1);
        const period  = (req.body && req.body.period) ? req.body.period : "daily";
        const styleInstructions = await getStyleInstructions(db);
        console.log("Manual generateHoroscopes for " + dateKey + " period=" + period);
        const result = await generateForDate(dateKey, CLAUDE_API_KEY.value(), styleInstructions, period);
        res.json({ success: result.success, failed: result.failed, errors: result.errors, date: dateKey, period: period });

      } else {
        res.status(400).json({ error: "Unknown action: " + action });
      }

    } catch (e) {
      res.status(500).json({ error: String(e) });
    }
  }
);

// ─── scheduledGenerateHoroscopes ─────────────────────────────────────────────

/**
 * Проверяет, что все 12 знаков заполнены хотя бы для одного языка (ru).
 * Если да — считаем дату полностью сгенерированной и пропускаем.
 */
async function horoscopesComplete(dateKey) {
  try {
    const url = FIREBASE_DB_URL + "/horoscopes/ru/daily/" + dateKey + ".json";
    const resp = await fetch(url);
    if (!resp.ok) return false;
    const data = await resp.json();
    if (!data || typeof data !== "object") return false;
    return SIGNS.every(function(s) { return data[s.id] && data[s.id].text; });
  } catch (e) {
    return false;
  }
}

exports.scheduledGenerateHoroscopes = onSchedule(
  {
    schedule: "0 7 * * *",
    timeZone: "UTC",
    secrets: [CLAUDE_API_KEY],
    timeoutSeconds: 540,
    memory: "512MiB",
  },
  async function() {
    const apiKey   = CLAUDE_API_KEY.value();
    const db       = getFirestore();
    const styleTpl = await getStyleInstructions(db);
    const tomorrow = utcDateKey(1);
    const dayAfter = utcDateKey(2);

    console.log("=== scheduledGenerateHoroscopes: " + tomorrow + " + " + dayAfter + " ===");

    if (await horoscopesComplete(tomorrow)) {
      console.log(tomorrow + ": already complete, skipping");
    } else {
      const r1 = await generateForDate(tomorrow, apiKey, styleTpl, "daily");
      console.log(tomorrow + ": ok=" + r1.success + " fail=" + r1.failed);
    }

    if (await horoscopesComplete(dayAfter)) {
      console.log(dayAfter + ": already complete, skipping");
    } else {
      const r2 = await generateForDate(dayAfter, apiKey, styleTpl, "daily");
      console.log(dayAfter + ": ok=" + r2.success + " fail=" + r2.failed);
    }
  }
);

// ─── scheduledPush ────────────────────────────────────────────────────────────

exports.scheduledPush = onSchedule(
  { schedule: "0 * * * *", timeZone: "UTC" },
  async function() {
    const db = getFirestore();
    const doc = await db.doc(SCHEDULE_DOC).get();
    if (!doc.exists) return;
    const localHours = doc.data().localHours || [];
    if (localHours.length === 0) return;
    const currentUtcHour = new Date().getUTCHours();
    const messaging = getMessaging();
    const sends = [];
    for (let offset = -12; offset <= 14; offset++) {
      const localHour = ((currentUtcHour + offset) % 24 + 24) % 24;
      if (localHours.includes(localHour)) {
        const topic = offsetToTopic(offset);
        sends.push(
          messaging.send({ topic: topic, data: { type: "daily_horoscope" } })
            .catch(function(e) { console.warn("Topic " + topic + " skipped: " + e.message); })
        );
      }
    }
    if (sends.length > 0) {
      await Promise.all(sends);
      console.log("Scheduled push: UTC " + currentUtcHour + " -> " + sends.length + " TZ topic(s)");
    } else {
      console.log("Scheduled push: UTC " + currentUtcHour + " -> no matching timezones");
    }
  }
);
