const { onRequest }  = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { defineSecret } = require("firebase-functions/params");
const { initializeApp }  = require("firebase-admin/app");
const { getMessaging }   = require("firebase-admin/messaging");
const { getFirestore }   = require("firebase-admin/firestore");

initializeApp();

const ADMIN_SECRET = defineSecret("ADMIN_SECRET");
const SCHEDULE_DOC = "admin_config/push_schedule";

/**
 * Конвертирует UTC-смещение в имя FCM-топика.
 * Примеры: +3 → "tz_p3", -5 → "tz_n5", 0 → "tz_0"
 */
function offsetToTopic(offset) {
  if (offset === 0) return "tz_0";
  return offset > 0 ? `tz_p${offset}` : `tz_n${Math.abs(offset)}`;
}

/**
 * Единая точка входа для admin-приложения.
 *
 * POST  /adminApi
 * Header: x-admin-secret: <ADMIN_SECRET>
 * Body:  { "action": "sendPush" | "getSchedule" | "setSchedule", ...params }
 *
 * Actions:
 *   sendPush                      → отправляет пуш на топик horoscope_daily прямо сейчас
 *   getSchedule                   → { localHours: [9, 18, ...] }  (локальные часы юзеров)
 *   setSchedule { localHours }    → сохраняет расписание в Firestore
 */
exports.adminApi = onRequest(
  { cors: true, invoker: "public", secrets: [ADMIN_SECRET] },
  async (req, res) => {
    if (req.method !== "POST") {
      res.status(405).json({ error: "Method Not Allowed" });
      return;
    }

    const secret   = req.headers["x-admin-secret"];
    const expected = ADMIN_SECRET.value();
    if (!secret || secret !== expected) {
      res.status(401).json({ error: "Unauthorized" });
      return;
    }

    const action = req.body && req.body.action;
    const db = getFirestore();

    try {
      switch (action) {

        case "sendPush": {
          // Ручная рассылка — всем сразу через общий топик
          const type = (req.body && req.body.type) ? req.body.type : "daily_horoscope";
          await getMessaging().send({
            topic: "horoscope_daily",
            data: { type: type },
          });
          res.json({ success: true });
          break;
        }

        case "getSchedule": {
          const doc = await db.doc(SCHEDULE_DOC).get();
          const localHours = doc.exists ? (doc.data().localHours || []) : [];
          res.json({ localHours: localHours });
          break;
        }

        case "setSchedule": {
          const localHours = (req.body && Array.isArray(req.body.localHours))
            ? req.body.localHours : [];
          await db.doc(SCHEDULE_DOC).set({ localHours: localHours, updatedAt: Date.now() });
          res.json({ success: true });
          break;
        }

        default:
          res.status(400).json({ error: "Unknown action: " + action });
      }
    } catch (e) {
      res.status(500).json({ error: String(e) });
    }
  }
);

/**
 * Запускается каждый час в 00 минут (UTC).
 *
 * Логика: для каждого возможного UTC-смещения (-12..+14) вычисляем
 * какой сейчас локальный час у людей с этим смещением.
 * Если он совпадает с одним из выбранных localHours — шлём пуш в их топик.
 *
 * Результат: юзер в любом часовом поясе получает пуш ровно в своё локальное время.
 */
exports.scheduledPush = onSchedule(
  { schedule: "0 * * * *", timeZone: "UTC" },
  async () => {
    const db = getFirestore();
    const doc = await db.doc(SCHEDULE_DOC).get();
    if (!doc.exists) return;

    const localHours = doc.data().localHours || [];
    if (localHours.length === 0) return;

    const currentUtcHour = new Date().getUTCHours();
    const messaging = getMessaging();
    const sends = [];

    // Перебираем все целочисленные UTC-смещения от -12 до +14
    for (let offset = -12; offset <= 14; offset++) {
      const localHour = ((currentUtcHour + offset) % 24 + 24) % 24;
      if (localHours.includes(localHour)) {
        const topic = offsetToTopic(offset);
        sends.push(
          messaging.send({ topic, data: { type: "daily_horoscope" } })
            .catch(e => console.warn(`Topic ${topic} skipped (no subscribers?): ${e.message}`))
        );
      }
    }

    if (sends.length > 0) {
      await Promise.all(sends);
      console.log(`Scheduled push: UTC ${currentUtcHour} → sent to ${sends.length} timezone topic(s)`);
    } else {
      console.log(`Scheduled push: UTC ${currentUtcHour} → no matching timezones`);
    }
  }
);
