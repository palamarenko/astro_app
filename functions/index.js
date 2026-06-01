const { onRequest }  = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { defineSecret } = require("firebase-functions/params");
const { initializeApp }  = require("firebase-admin/app");
const { getMessaging }   = require("firebase-admin/messaging");
const { getFirestore }   = require("firebase-admin/firestore");

initializeApp();

const ADMIN_SECRET   = defineSecret("ADMIN_SECRET");
const CLAUDE_API_KEY = defineSecret("CLAUDE_API_KEY");

const SCHEDULE_DOC       = "admin_config/push_schedule";
const GEN_SCHEDULE_DOC   = "admin_config/gen_schedule";
const PROMPT_DOC_PREFIX  = "admin_config/prompt_";   // + "daily" | "weekly" | "monthly"
const FIREBASE_DB_URL    = "https://zodiac-b23ce-default-rtdb.europe-west1.firebasedatabase.app";

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

const LANGUAGES = ["ru", "uk", "en", "es", "de", "fr"];

// Default prompt template. Variables: {date}, {lang}
// The signs list is injected automatically by the function.
// Style instructions stored in Firestore — only the creative/tone part.
// The technical wrapper (signs list, JSON schema, date, language) is always hardcoded.
const LANG_QUALITY =
  "Language quality:\n" +
  "— Ukrainian: write in authentic literary Ukrainian. Avoid russicisms and calques from Russian " +
  "(e.g. use 'тому що' not 'потому що', 'тільки' not 'только', 'будь-який' not 'любий' in the sense of 'any'). " +
  "Use native Ukrainian vocabulary, idioms and phrasing. The text must feel natural to a native Ukrainian speaker, " +
  "not like a word-for-word translation from Russian.\n" +
  "— Russian: use expressive literary Russian.\n" +
  "— English: use poetic but accessible English.\n" +
  "— Spanish: write in natural, warm Latin-American Spanish. Avoid overly formal Castilian constructions. " +
  "Use smooth, conversational phrasing that feels native and approachable.\n" +
  "— German: write in clear, warm, modern German. Avoid bureaucratic or overly formal tone. " +
  "Use natural sentence structures and accessible vocabulary, as if speaking to a friend.\n" +
  "— French: write in elegant, natural French. Avoid anglicisms and overly complex constructions. " +
  "The tone should feel warm and literary, like a trusted advisor speaking to you directly.";

const DEFAULT_STYLES = {
  daily:
    "Style: warm, clear, easy to read — like advice from a trusted friend, not a mystical oracle.\n" +
    "Avoid heavy metaphors, flowery language, and vague cosmic imagery. Write in plain, natural sentences.\n" +
    "Each sign text: 6-8 short sentences. Include at least one concrete prediction or practical tip for the day " +
    "(what to expect, what to do, what to avoid). Reflect score levels: " +
    "low (50-65) — mention a specific challenge or thing to be careful about, " +
    "medium (66-80) — note steady but real progress in one area, " +
    "high (81-100) — name a concrete opportunity or win coming their way.\n" +
    "Keyword: 1-2 simple, vivid words capturing the day's theme.\n" +
    LANG_QUALITY,

  weekly:
    "Style: warm, clear, easy to read — like advice from a trusted friend, not a mystical oracle.\n" +
    "Avoid heavy metaphors, flowery language, and vague cosmic imagery. Write in plain, natural sentences.\n" +
    "Each sign text: 3-4 short sentences covering the most important theme of the week. " +
    "Include at least one concrete prediction or actionable tip (what to focus on, what to watch out for). " +
    "Reflect score levels: low (50-65) — name a specific tension or obstacle to expect this week, " +
    "medium (66-80) — highlight one area where steady effort will pay off, " +
    "high (81-100) — point to a clear opportunity or highlight of the week.\n" +
    "IMPORTANT: Do NOT mention week numbers (e.g. 'week 32' or 'W32'). " +
    "Instead refer to the time naturally, e.g. 'the second week of August', 'mid-July'.\n" +
    "Keyword: 1-2 simple, vivid words capturing the week's theme.\n" +
    LANG_QUALITY,

  monthly:
    "Style: warm, clear, easy to read — like advice from a trusted friend, not a mystical oracle.\n" +
    "Avoid heavy metaphors, flowery language, and vague cosmic imagery. Write in plain, natural sentences.\n" +
    "Each sign text: 3-4 short sentences about the main theme of the month. " +
    "Name the 1-2 life areas most in focus (love, work, finances, health, self-development) and give one clear, " +
    "specific prediction or piece of guidance for the month. Reflect score levels: " +
    "low (50-65) — be direct about what will be difficult and why patience matters, " +
    "medium (66-80) — describe what consistent effort will bring, " +
    "high (81-100) — describe the specific breakthrough or reward waiting this month.\n" +
    "Keyword: 1-2 simple, vivid words capturing the month's theme.\n" +
    LANG_QUALITY,
};

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

async function getStyleInstructions(db, period) {
  const p = (period === "weekly" || period === "monthly") ? period : "daily";
  const defaultStyle = DEFAULT_STYLES[p];
  try {
    const doc = await db.doc(PROMPT_DOC_PREFIX + p).get();
    return (doc.exists && doc.data().prompt) ? doc.data().prompt : defaultStyle;
  } catch (e) {
    return defaultStyle;
  }
}

async function saveGenerationLog(entry) {
  try {
    const url = FIREBASE_DB_URL + "/generation_logs/" + entry.id + ".json";
    await fetch(url, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(entry),
    });
  } catch (e) {
    console.warn("saveGenerationLog failed: " + e.message);
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
  const langName = lang === "uk" ? "Ukrainian"
                 : lang === "en" ? "English"
                 : lang === "es" ? "Spanish"
                 : lang === "de" ? "German"
                 : lang === "fr" ? "French"
                 : "Russian";
  const signsDesc = buildSignsDesc();
  const periodWord = period === "weekly" ? "weekly" : period === "monthly" ? "monthly" : "daily";
  const periodDesc = period === "weekly" ? "week (" + dateKey + ")"
                   : period === "monthly" ? "month (" + dateKey + ")"
                   : "day (" + dateKey + ")";
  const prompt =
    "Generate " + periodWord + " horoscopes for all 12 zodiac signs for the " + periodDesc + ".\n" +
    "Language: " + langName + ".\n\n" +
    styleInstructions + "\n\n" +
    "Signs and their traits:\n" + signsDesc + "\n\n" +
    "Respond ONLY with valid JSON object, no markdown, no extra text:\n" +
    "{\"aries\":{\"text\":\"...\",\"keyword\":\"1-2 words\",\"love\":72,\"career\":85,\"health\":60,\"energy\":78}," +
    "\"taurus\":{...},\"gemini\":{...},\"cancer\":{...},\"leo\":{...},\"virgo\":{...}," +
    "\"libra\":{...},\"scorpio\":{...},\"sagittarius\":{...},\"capricorn\":{...}," +
    "\"aquarius\":{...},\"pisces\":{...}}";

  const raw = await callClaude(prompt, apiKey, 6000);
  const parsed = JSON.parse(extractJson(raw));

  // Validate all 12 signs are present
  const missing = SIGNS.filter(function(s) { return !parsed[s.id]; });
  if (missing.length > 0) {
    throw new Error("Missing signs: " + missing.map(function(s) { return s.id; }).join(", "));
  }
  return parsed; // { aries: {text,keyword,love,career,health,energy}, ... }
}

// Generate for one date: one Claude request per language, each returning all 12 signs.
// langs — optional array of language codes to generate; defaults to all LANGUAGES.
async function generateForDate(dateKey, apiKey, styleInstructions, period, langs) {
  const periodVal  = period || "daily";
  const targetLangs = langs || LANGUAGES;
  let success = 0, failed = 0;
  const errors = [];

  for (var li = 0; li < targetLangs.length; li++) {
    const lang = targetLangs[li];
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
        const period = (req.body && req.body.period) ? req.body.period : "daily";
        const p = (period === "weekly" || period === "monthly") ? period : "daily";
        const doc = await db.doc(PROMPT_DOC_PREFIX + p).get();
        const prompt = (doc.exists && doc.data().prompt) ? doc.data().prompt : DEFAULT_STYLES[p];
        res.json({ prompt: prompt, isDefault: !doc.exists || !doc.data().prompt });

      } else if (action === "setPrompt") {
        const period = (req.body && req.body.period) ? req.body.period : "daily";
        const p = (period === "weekly" || period === "monthly") ? period : "daily";
        const prompt = (req.body && req.body.prompt) ? req.body.prompt : DEFAULT_STYLES[p];
        await db.doc(PROMPT_DOC_PREFIX + p).set({ prompt: prompt, updatedAt: Date.now() });
        res.json({ success: true });

      } else if (action === "getGenSchedule") {
        const doc = await db.doc(GEN_SCHEDULE_DOC).get();
        res.json({ localHours: doc.exists ? (doc.data().localHours || []) : [] });

      } else if (action === "setGenSchedule") {
        const localHours = (req.body && Array.isArray(req.body.localHours)) ? req.body.localHours : [];
        await db.doc(GEN_SCHEDULE_DOC).set({ localHours: localHours, updatedAt: Date.now() });
        res.json({ success: true });

      } else if (action === "generateHoroscopes") {
        const dateKey = (req.body && req.body.date) ? req.body.date : utcDateKey(1);
        const period  = (req.body && req.body.period) ? req.body.period : "daily";
        const lang    = (req.body && req.body.lang)   ? req.body.lang   : null;
        const styleInstructions = await getStyleInstructions(db, period);
        if (lang) {
          // Генерировать только один язык
          console.log("Manual generateHoroscopes lang=" + lang + " date=" + dateKey + " period=" + period);
          const MAX_RETRIES = 8;
          var lastErr = null;
          var success = 0, failed = 0;
          for (var attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
              const horoscopes = await generateAllForLang(lang, period, dateKey, CLAUDE_API_KEY.value(), styleInstructions);
              await Promise.all(SIGNS.map(function(sign) {
                return saveToDb(lang, period, dateKey, sign.id, horoscopes[sign.id]);
              }));
              success = 12;
              console.log("OK " + dateKey + " " + lang + " (all 12 signs)");
              break;
            } catch (e) {
              lastErr = e;
              console.warn("Attempt " + attempt + "/" + MAX_RETRIES + " failed for " + lang + ": " + e.message);
              if (attempt < MAX_RETRIES) {
                await new Promise(function(r) { setTimeout(r, Math.min(2000 * attempt, 10000)); });
              }
              if (attempt === MAX_RETRIES) {
                failed = 12;
                console.error("FAIL all retries exhausted for " + lang);
              }
            }
          }
          res.json({ success: success, failed: failed, errors: failed > 0 ? [lang + ": " + (lastErr ? lastErr.message : "unknown")] : [], date: dateKey, period: period, lang: lang });
        } else {
          // Генерировать все языки (устаревший режим, для обратной совместимости)
          console.log("Manual generateHoroscopes ALL langs for " + dateKey + " period=" + period);
          const result = await generateForDate(dateKey, CLAUDE_API_KEY.value(), styleInstructions, period);
          res.json({ success: result.success, failed: result.failed, errors: result.errors, date: dateKey, period: period });
        }

      } else {
        res.status(400).json({ error: "Unknown action: " + action });
      }

    } catch (e) {
      res.status(500).json({ error: String(e) });
    }
  }
);

// ─── scheduledGenerateHoroscopes ─────────────────────────────────────────────

// ─── Date key helpers ─────────────────────────────────────────────────────────

/**
 * Week key matching the Kotlin app's computeDateKey logic:
 *   week = (dayOfYear / 7) + 1
 */
function appWeekKey(offsetDays) {
  const d = new Date();
  d.setUTCDate(d.getUTCDate() + (offsetDays || 0));
  d.setUTCHours(0, 0, 0, 0);
  const year = d.getUTCFullYear();
  const start = new Date(Date.UTC(year, 0, 1));
  const dayOfYear = Math.floor((d - start) / 86400000) + 1; // 1-based
  const week = Math.floor(dayOfYear / 7) + 1;
  return year + "-W" + String(week).padStart(2, "0");
}

/** Monthly key: YYYY-MM */
function monthKey(offsetMonths) {
  const d = new Date();
  d.setUTCMonth(d.getUTCMonth() + (offsetMonths || 0), 1);
  return d.getUTCFullYear() + "-" + String(d.getUTCMonth() + 1).padStart(2, "0");
}

// ─── Completion check ─────────────────────────────────────────────────────────

/**
 * Returns true if meta record exists for the given lang/period/dateKey with count >= 12.
 */
async function isLangComplete(lang, period, dateKey) {
  try {
    const url = FIREBASE_DB_URL + "/meta/" + lang + "/" + period + "/" + dateKey + ".json";
    const resp = await fetch(url);
    if (!resp.ok) return false;
    const data = await resp.json();
    return data && data.count >= 12;
  } catch (e) {
    return false;
  }
}

/**
 * Returns array of language codes that are NOT yet fully generated for the given period/dateKey.
 */
async function getMissingLangs(period, dateKey) {
  const checks = await Promise.all(
    LANGUAGES.map(function(lang) {
      return isLangComplete(lang, period, dateKey).then(function(done) {
        return done ? null : lang;
      });
    })
  );
  return checks.filter(function(l) { return l !== null; });
}

// ─── Save horoscope meta (mirrors Kotlin FirebaseService.saveHoroscopeMeta) ──

async function saveHoroscopeMeta(lang, period, dateKey) {
  try {
    const url = FIREBASE_DB_URL + "/meta/" + lang + "/" + period + "/" + dateKey + ".json";
    await fetch(url, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ count: 12, generatedAt: Date.now() }),
    });
  } catch (e) {
    console.warn("saveHoroscopeMeta failed: " + e.message);
  }
}

// ─── Generate one period/dateKey with logging ─────────────────────────────────

async function generatePeriodDate(apiKey, db, period, dateKey) {
  // Check each language individually — only generate the missing ones
  const missingLangs = await getMissingLangs(period, dateKey);

  if (missingLangs.length === 0) {
    console.log(period + " " + dateKey + ": all 6 languages complete, skipping");
    return;
  }

  console.log("=== generating " + period + " " + dateKey + " — missing: [" + missingLangs.join(", ") + "] ===");

  const styleInstructions = await getStyleInstructions(db, period);
  const t = Date.now();

  const result = await generateForDate(dateKey, apiKey, styleInstructions, period, missingLangs);
  console.log(period + " " + dateKey + ": ok=" + result.success + " fail=" + result.failed);

  // Save meta only for languages that succeeded
  for (var li = 0; li < missingLangs.length; li++) {
    const lang = missingLangs[li];
    if (await isLangComplete(lang, period, dateKey)) {
      await saveHoroscopeMeta(lang, period, dateKey);
    }
  }

  await saveGenerationLog({
    id: String(t), timestamp: t, period: period, dateKey: dateKey,
    success: result.success, failed: result.failed, durationMs: Date.now() - t,
    triggeredBy: "scheduled",
  });
}

// ─── scheduledGenerateDaily ──────────────────────────────────────────────────
// Каждый час; реальные часы запуска берутся из Firestore (GEN_SCHEDULE_DOC).
// Генерирует daily для завтра + послезавтра (все 6 языков).

exports.scheduledGenerateDaily = onSchedule(
  {
    schedule: "0 * * * *",
    timeZone: "UTC",
    secrets: [CLAUDE_API_KEY],
    timeoutSeconds: 540,
    memory: "512MiB",
  },
  async function() {
    const db             = getFirestore();
    const genDoc         = await db.doc(GEN_SCHEDULE_DOC).get();
    const localHours     = genDoc.exists ? (genDoc.data().localHours || []) : [];
    const currentUtcHour = new Date().getUTCHours();

    if (!localHours.includes(currentUtcHour)) {
      console.log("scheduledGenerateDaily: UTC " + currentUtcHour + " not in schedule, skipping");
      return;
    }

    const apiKey = CLAUDE_API_KEY.value();
    console.log("=== scheduledGenerateDaily: UTC " + currentUtcHour + " ===");

    await generatePeriodDate(apiKey, db, "daily", utcDateKey(1));
    await generatePeriodDate(apiKey, db, "daily", utcDateKey(2));
  }
);

// ─── scheduledGenerateWeekly ─────────────────────────────────────────────────
// Каждое воскресенье в 03:00 UTC.
// Генерирует weekly для текущей недели + следующей (все 6 языков).

exports.scheduledGenerateWeekly = onSchedule(
  {
    schedule: "0 3 * * 0",
    timeZone: "UTC",
    secrets: [CLAUDE_API_KEY],
    timeoutSeconds: 540,
    memory: "512MiB",
  },
  async function() {
    const db     = getFirestore();
    const apiKey = CLAUDE_API_KEY.value();
    console.log("=== scheduledGenerateWeekly ===");

    await generatePeriodDate(apiKey, db, "weekly", appWeekKey(0));
    await generatePeriodDate(apiKey, db, "weekly", appWeekKey(7));
  }
);

// ─── scheduledGenerateMonthly ────────────────────────────────────────────────
// 25-го числа каждого месяца в 03:00 UTC.
// Генерирует monthly для текущего месяца + следующего (все 6 языков).

exports.scheduledGenerateMonthly = onSchedule(
  {
    schedule: "0 3 25 * *",
    timeZone: "UTC",
    secrets: [CLAUDE_API_KEY],
    timeoutSeconds: 540,
    memory: "512MiB",
  },
  async function() {
    const db     = getFirestore();
    const apiKey = CLAUDE_API_KEY.value();
    console.log("=== scheduledGenerateMonthly ===");

    await generatePeriodDate(apiKey, db, "monthly", monthKey(0));
    await generatePeriodDate(apiKey, db, "monthly", monthKey(1));
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
