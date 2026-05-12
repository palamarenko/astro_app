const { onRequest }    = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const { initializeApp }  = require("firebase-admin/app");
const { getMessaging }   = require("firebase-admin/messaging");

initializeApp();

const ADMIN_SECRET = defineSecret("ADMIN_SECRET");

/**
 * POST /sendDailyHoroscope
 * Header: x-admin-secret: <ADMIN_SECRET>
 */
exports.sendDailyHoroscope = onRequest(
  {
    cors: true,
    invoker: "public",
    secrets: [ADMIN_SECRET],
  },
  async (req, res) => {
    if (req.method !== "POST") {
      res.status(405).json({ error: "Method Not Allowed" });
      return;
    }

    const incoming = req.headers["x-admin-secret"];
    const expected = ADMIN_SECRET.value();

    if (!incoming || incoming !== expected) {
      res.status(401).json({ error: "Unauthorized" });
      return;
    }

    try {
      const type = (req.body && req.body.type) ? req.body.type : "daily_horoscope";
      await getMessaging().send({
        topic: "horoscope_daily",
        data: { type: type },
      });
      res.json({ success: true });
    } catch (e) {
      res.status(500).json({ error: String(e) });
    }
  }
);
