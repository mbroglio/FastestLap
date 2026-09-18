const axios = require("axios");
const cheerio = require("cheerio");

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
                    body: description
                },
                data: {
                    type: "news",
                    sourceId: source.id,
                    sourceName: source.name,
                    title: title,
                    body: description,
                    newsUrl: link,
                    imageUrl: imageUrl
                },
                android: {
                    priority: "high",
                    notification: {
                        channelId: "fastestlap_news_v3",
                        icon: "ic_notification",
                        color: "#9D0006",
                        sound: "team_radio"
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
 * Checks upcoming F1 race weekend sessions.
 * Sends an FCM push reminder to topic "sessions" 15-20 minutes before a session starts.
 */
async function executeSessionCheckAndPush(db, messaging) {
    console.log("Starting F1 Session schedule check for push reminders...");

    let raceData;
    try {
        const response = await axios.get(JOLPICA_NEXT_RACE_URL, { timeout: 10000 });
        const races = response.data?.MRData?.RaceTable?.Races;
        if (!races || races.length === 0) {
            console.log("No upcoming race data returned from Jolpica API.");
            return { sent: 0, reason: "no_upcoming_race" };
        }
        raceData = races[0];
    } catch (err) {
        console.error("Failed to fetch next race schedule from Jolpica:", err.message);
        return { sent: 0, error: err.message };
    }

    const raceName = raceData.raceName || "Gran Premio";
    const season = raceData.season;
    const round = raceData.round;

    // Collect all sessions for this race weekend
    const sessions = [];

    function addSession(name, dateStr, timeStr) {
        if (!dateStr) return;
        const iso = timeStr ? `${dateStr}T${timeStr}` : `${dateStr}T00:00:00Z`;
        const timestamp = new Date(iso).getTime();
        if (!isNaN(timestamp)) {
            sessions.push({ name, startTime: timestamp, iso });
        }
    }

    // Standard practice sessions
    if (raceData.FirstPractice) addSession("Prove Libere 1 (FP1)", raceData.FirstPractice.date, raceData.FirstPractice.time);
    if (raceData.SecondPractice) addSession("Prove Libere 2 (FP2)", raceData.SecondPractice.date, raceData.SecondPractice.time);
    if (raceData.ThirdPractice) addSession("Prove Libere 3 (FP3)", raceData.ThirdPractice.date, raceData.ThirdPractice.time);

    // Sprint format sessions
    if (raceData.SprintQualifying) addSession("Sprint Shootout", raceData.SprintQualifying.date, raceData.SprintQualifying.time);
    if (raceData.Sprint) addSession("Gara Sprint", raceData.Sprint.date, raceData.Sprint.time);

    // Qualifying and Main Race
    if (raceData.Qualifying) addSession("Qualifiche", raceData.Qualifying.date, raceData.Qualifying.time);
    if (raceData.date) addSession("Gara", raceData.date, raceData.time);

    const now = Date.now();
    const sentSessionsRef = db.ref(DB_PATHS.sent_sessions);
    const sentSessions = [];

    for (const s of sessions) {
        const sessionStartTime = s.startTime;
        const diffMinutes = (sessionStartTime - now) / (1000 * 60);

        // Window: session starts in 5 to 25 minutes (centered at ~15-20 minutes before)
        if (diffMinutes >= 5 && diffMinutes <= 25) {
            const sessionKey = `${season}_${round}_${s.name.replace(/\s+/g, "_").toLowerCase()}`;

            const checkSnap = await sentSessionsRef.child(sessionKey).once("value");
            if (!checkSnap.exists()) {
                await sentSessionsRef.child(sessionKey).set({
                    raceName,
                    sessionName: s.name,
                    startTime: s.iso,
                    sentAt: new Date().toISOString()
                });

                // Format time in Europe/Rome (CET/CEST)
                const timeStr = new Date(sessionStartTime).toLocaleTimeString("it-IT", {
                    hour: "2-digit",
                    minute: "2-digit",
                    timeZone: "Europe/Rome"
                });

                const payload = {
                    topic: "sessions",
                    notification: {
                        title: `🏁 ${raceName}`,
                        body: `${s.name} sta per iniziare! (ore ${timeStr})`
                    },
                    data: {
                        type: "session",
                        raceName: raceName,
                        sessionName: s.name,
                        sessionTime: timeStr
                    },
                    android: {
                        priority: "high",
                        notification: {
                            channelId: "fastestlap_sessions_v3",
                            icon: "ic_notification",
                            color: "#9D0006",
                            sound: "team_radio"
                        }
                    }
                };

                const response = await messaging.send(payload);
                console.log(`🔥 [FCM PUSH SESSION SENT]: ${raceName} - ${s.name} (ore ${timeStr})`);
                sentSessions.push({ session: s.name, time: timeStr, messageId: response });
            } else {
                console.log(`Session reminder for ${s.name} was already sent previously.`);
            }
        }
    }

    return { sent: sentSessions.length, details: sentSessions };
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
            raceName: isSession ? "Gran Premio di Test" : "",
            sessionName: isSession ? "Qualifiche" : "",
            sessionTime: isSession ? "15:00" : "",
            newsUrl: !isSession ? "https://www.formula1.com" : ""
        },
        android: {
            priority: "high",
            notification: {
                channelId: isSession ? "fastestlap_sessions_v3" : "fastestlap_news_v3",
                icon: "ic_notification",
                color: "#9D0006",
                sound: "team_radio"
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
