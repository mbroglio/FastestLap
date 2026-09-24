const axios = require("axios");
const cheerio = require("cheerio");
const calendarLogic = require("./calendar_logic");

const NEWS_SOURCES_CONFIG = [
    {
        id: "motorsport",
        name: "Motorsport Italy",
        url: "https://it.motorsport.com/rss/f1/news/",
        topic: "news_motorsport",
        language: "it"
    },
    {
        id: "autosport",
        name: "Autosport",
        url: "https://www.autosport.com/rss/f1/news/",
        topic: "news_autosport",
        language: "en"
    },
    {
        id: "crash",
        name: "Crash.net",
        url: "https://www.crash.net/rss/f1",
        topic: "news_crash",
        language: "en"
    }
];

const JOLPICA_NEXT_RACE_URL = "https://api.jolpi.ca/ergast/f1/current/next/?format=json";

const DB_PATHS = {
    last_news_prefix: "app_config/notifications/last_news",
    sent_sessions: "app_config/notifications/sent_sessions"
};

/**
 * Checks for newly published F1 articles on RSS feeds for each supported source
 * (Motorsport IT, Autosport EN, Crash.net EN).
 * When a new article is detected for a specific source, sends an FCM push message
 * directly to that source's dedicated topic (e.g. news_motorsport, news_autosport, news_crash).
 * Only users who have selected that specific source in their app preferences receive the push!
 */
async function executeNewsCheckAndPush(db, messaging) {
    console.log("Starting F1 News check for push notifications across all sources...");
    const results = [];

    for (const source of NEWS_SOURCES_CONFIG) {
        try {
            console.log(`Checking news for source: ${source.name} (${source.url})...`);
            const response = await axios.get(source.url, {
                headers: { "User-Agent": "Mozilla/5.0 FastestLapCloudFunction" },
                timeout: 10000
            });

            if (response.status !== 200 || !response.data) {
                console.warn(`Non-200 response from ${source.name}: ${response.status}`);
                results.push({ source: source.id, updated: false, reason: "http_status_" + response.status });
                continue;
            }

            const $ = cheerio.load(response.data, { xmlMode: true });
            const firstItem = $("item").first();

            if (firstItem.length === 0) {
                console.warn(`No <item> elements found in feed for ${source.name}`);
                results.push({ source: source.id, updated: false, reason: "empty_feed" });
                continue;
            }

            const title = firstItem.find("title").text().trim();
            const link = firstItem.find("link").text().trim();
            let description = firstItem.find("description").text().trim();
            const imageUrl = firstItem.find("enclosure").attr("url")
                || firstItem.find("media\\:content").attr("url")
                || "";

            // Clean HTML formatting inside description
            description = description.replace(/<[^>]*>?/gm, "")
                                     .replace(/&nbsp;/g, " ")
                                     .replace(/\s+/g, " ")
                                     .trim();

            if (description.length > 250) {
                description = description.substring(0, 247) + "...";
            }

            if (!title || !link) {
                console.warn(`Missing title or link in ${source.name}`);
                results.push({ source: source.id, updated: false, reason: "missing_fields" });
                continue;
            }

            // Check if article was already notified for this source
            const lastNotifiedRef = db.ref(`${DB_PATHS.last_news_prefix}/${source.id}`);
            const snapshot = await lastNotifiedRef.once("value");
            const lastNotifiedLink = snapshot.val();

            // Baseline initialization: if database has no record yet for this source
            if (!lastNotifiedLink) {
                console.log(`Baseline news article stored for ${source.name}:`, link);
                await lastNotifiedRef.set(link);
                results.push({ source: source.id, updated: false, reason: "baseline_initialized", article: title });
                continue;
            }

            if (link === lastNotifiedLink) {
                console.log(`No new articles for ${source.name} since last check.`);
                results.push({ source: source.id, updated: false, reason: "up_to_date" });
                continue;
            }

            // New article found! Update database and broadcast push to topic `news_${source.id}`
            await lastNotifiedRef.set(link);
            await db.ref(`${DB_PATHS.last_news_prefix}/${source.id}_updated_at`).set(Date.now());

            const payload = {
                topic: source.topic,
                notification: {
                    title: "🏎️ " + title,
                    body: description,
                    ...(imageUrl ? { imageUrl } : {})
                },
                data: {
                    type: "news",
                    EXTRA_TARGET_TAB: "news",
                    sourceId: source.id,
                    sourceName: source.name,
                    title: "🏎️ " + title,
                    body: description,
                    newsUrl: link,
                    imageUrl: imageUrl || ""
                },
                android: {
                    priority: "high",
                    notification: {
                        channelId: "fastestlap_news_v4",
                        sound: "team_radio",
                        defaultSound: false,
                        priority: "high",
                        visibility: "public"
                    }
                }
            };

            const msgResponse = await messaging.send(payload);
            console.log(`🔥 [FCM PUSH NEWS SENT to ${source.topic}]:`, title, "Message ID:", msgResponse);
            results.push({
                source: source.id,
                topic: source.topic,
                updated: true,
                title: title,
                link: link,
                messageId: msgResponse
            });

        } catch (err) {
            console.error(`Error checking news for source ${source.name}:`, err.message);
            results.push({ source: source.id, updated: false, error: err.message });
        }
    }

    return { timestamp: new Date().toISOString(), results };
}

/**
 * Checks upcoming race weekend sessions across F1, F2, and F3 directly from the Firebase calendar.
 * Sends two alerts per session to the dedicated topic:
 * 1. 30 minutes before the session starts.
 * 2. 5 minutes before the session starts.
 */
async function executeSessionCheckAndPush(db, messaging) {
    const currentYear = new Date().getFullYear();
    console.log(`Starting session check from Firebase calendar for year ${currentYear}...`);

    // Ensure F1 calendar is loaded in RTDB as baseline
    const f1CalRef = db.ref(`${calendarLogic.DB_PATHS.calendar}/f1/${currentYear}`);
    let f1Snapshot = await f1CalRef.once("value");
    if (!f1Snapshot.exists() || !f1Snapshot.val()) {
        console.log("F1 calendar not found on Firebase. Performing initial fetch from Jolpica...");
        await calendarLogic.fetchAndStoreF1Calendar(db, currentYear);
        f1Snapshot = await f1CalRef.once("value");
    }

    const f1Data = f1Snapshot.val() || {};
    const f2Snapshot = await db.ref(`${calendarLogic.DB_PATHS.calendar}/f2/${currentYear}`).once("value");
    const f2Data = f2Snapshot.val() || {};
    const f3Snapshot = await db.ref(`${calendarLogic.DB_PATHS.calendar}/f3/${currentYear}`).once("value");
    const f3Data = f3Snapshot.val() || {};

    // Collect all sessions across all categories
    const allSessions = [];

    function processCategoryRaces(racesMap, category) {
        if (!racesMap) return;
        for (const key of Object.keys(racesMap)) {
            const race = racesMap[key];
            if (!race || !race.sessions) continue;
            const raceName = race.raceName || (race.Circuit?.circuitName || "Grand Prix");
            const round = race.round || key;

            const sessionsList = Array.isArray(race.sessions) ? race.sessions : Object.values(race.sessions);
            for (const s of sessionsList) {
                if (s && s.startTimeMillis) {
                    allSessions.push({
                        ...s,
                        raceName,
                        round,
                        category
                    });
                }
            }
        }
    }

    processCategoryRaces(f1Data, "f1");
    processCategoryRaces(f2Data, "f2");
    processCategoryRaces(f3Data, "f3");

    const now = Date.now();
    const sentSessionsRef = db.ref(DB_PATHS.sent_sessions);
    const sentAlerts = [];

    for (const s of allSessions) {
        const diffMinutes = (s.startTimeMillis - now) / (1000 * 60);

        // Check 30-minute alert window (20m to 35m before start)
        const is30mWindow = diffMinutes >= 20 && diffMinutes <= 35;
        // Check 5-minute alert window (2m to 8m before start)
        const is5mWindow = diffMinutes >= 2 && diffMinutes <= 8;

        if (!is30mWindow && !is5mWindow) {
            continue;
        }

        const alertSuffix = is30mWindow ? "30m" : "5m";
        const sessionKey = `${currentYear}_${s.category}_round${s.round}_${s.id}_${alertSuffix}`;

        const checkSnap = await sentSessionsRef.child(sessionKey).once("value");
        if (checkSnap.exists()) {
            continue;
        }

        // Format time in Europe/Rome (CET/CEST)
        const timeStr = new Date(s.startTimeMillis).toLocaleTimeString("it-IT", {
            hour: "2-digit",
            minute: "2-digit",
            timeZone: "Europe/Rome"
        });

        const categoryPrefix = s.category.toUpperCase() + ": ";
        let title;
        let body;

        if (is30mWindow) {
            title = `🏎️ ${categoryPrefix}${s.raceName}`;
            body = `${s.name} inizia tra 30 minuti! (ore ${timeStr})`;
        } else {
            title = `🚨 ${categoryPrefix}${s.raceName}`;
            body = `${s.name} sta per iniziare tra 5 minuti! (ore ${timeStr})`;
        }

        const targetTopic = s.topic || `session_${s.category}_${s.type || "race"}`;

        const payload = {
            topic: targetTopic,
            notification: {
                title: title,
                body: body
            },
            data: {
                type: "session",
                EXTRA_TARGET_TAB: "sessions",
                category: s.category,
                raceName: s.raceName,
                sessionName: s.name,
                sessionTime: timeStr,
                alertType: alertSuffix,
                startTimeMillis: (s.startTimeMillis || "").toString(),
                title: title,
                body: body
            },
            android: {
                priority: "high",
                notification: {
                    channelId: "fastestlap_sessions_v4",
                    sound: "team_radio",
                    defaultSound: false,
                    priority: "high",
                    visibility: "public"
                }
            }
        };

        try {
            const response = await messaging.send(payload);
            await sentSessionsRef.child(sessionKey).set({
                raceName: s.raceName,
                sessionName: s.name,
                category: s.category,
                startTime: s.iso || new Date(s.startTimeMillis).toISOString(),
                alertType: alertSuffix,
                topic: targetTopic,
                sentAt: new Date().toISOString()
            });

            console.log(`🔥 [FCM PUSH SESSION SENT to ${targetTopic}]: ${title} - ${body} (Message ID: ${response})`);
            sentAlerts.push({
                session: s.name,
                category: s.category,
                alertType: alertSuffix,
                time: timeStr,
                topic: targetTopic,
                messageId: response
            });
        } catch (pushErr) {
            console.error(`Failed to send push for ${sessionKey}:`, pushErr.message);
        }
    }

    return { sent: sentAlerts.length, details: sentAlerts };
}

/**
 * Sends an immediate test push notification to a specific topic (defaults to "news_motorsport").
 * Allows quick manual verification via HTTP trigger or testing scripts.
 */
async function sendTestNotification(messaging, topic = "news_motorsport", customTitle = null, customBody = null) {
    const isSession = topic && topic.toLowerCase().includes("session");

    const title = customTitle || (isSession ? "🏁 Test Promemoria Sessione" : "🏎️ Test Notizia F1");
    const body = customBody || (isSession
        ? "Gran Premio d'Italia - Qualifiche stanno per iniziare!"
        : "Questa è una notifica di test inviata automaticamente via Cloud Function.");

    const payload = {
        topic: topic,
        notification: {
            title: title,
            body: body
        },
        data: {
            type: isSession ? "session" : "news",
            EXTRA_TARGET_TAB: isSession ? "sessions" : "news",
            title: title,
            body: body,
            raceName: isSession ? "Gran Premio di Test" : "",
            sessionName: isSession ? "Qualifiche" : "",
            sessionTime: isSession ? "15:00" : "",
            newsUrl: !isSession ? "https://www.formula1.com" : ""
        },
        android: {
            priority: "high",
            notification: {
                channelId: isSession ? "fastestlap_sessions_v4" : "fastestlap_news_v4",
                sound: "team_radio",
                defaultSound: false,
                priority: "high",
                visibility: "public"
            }
        }
    };

    const response = await messaging.send(payload);
    console.log(`Test push sent to topic "${payload.topic}":`, response);
    return { success: true, topic: payload.topic, messageId: response };
}

module.exports = {
    executeNewsCheckAndPush,
    executeSessionCheckAndPush,
    sendTestNotification,
    NEWS_SOURCES_CONFIG
};
