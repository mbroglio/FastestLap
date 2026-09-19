package com.the_coffe_coders.fastestlap.util.notification;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Firebase Cloud Messaging (FCM) Service responsible for handling push notifications:
 * - Automatically receives remote push messages whether the app is in the foreground,
 *   background, or terminated.
 * - Handles device registration token refresh events (onNewToken).
 * - Routes incoming payloads (news, session alerts, general broadcasts) to the appropriate
 *   specialized notification managers.
 */
public class FastestLapFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FastestLapFCM";

    /**
     * Called when a new FCM registration token is generated (e.g. initial install, app restore).
     * The token is logged and cached locally in SharedPreferences.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.i(TAG, "==================================================");
        Log.i(TAG, "🔥 [FCM TOKEN GENERATO]: " + token);
        Log.i(TAG, "==================================================");

        AppNotificationManager.getInstance().saveFcmToken(this, token);
    }

    /**
     * Called when an incoming FCM message is received.
     * Handles both Notification payloads and custom Data payloads.
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.i(TAG, "==================================================");
        Log.i(TAG, "📩 [FCM MESSAGGIO RICEVUTO]");
        Log.i(TAG, "   Da: " + remoteMessage.getFrom());
        Log.i(TAG, "==================================================");

        // Turn on the device screen immediately upon push delivery
        AppNotificationManager.getInstance().wakeUpScreen(this);

        String title = null;
        String body = null;
        String imageUrl = null;

        // 1. Extract from Notification payload (if present)
        RemoteMessage.Notification notification = remoteMessage.getNotification();
        if (notification != null) {
            title = notification.getTitle();
            body = notification.getBody();
            if (notification.getImageUrl() != null) {
                imageUrl = notification.getImageUrl().toString();
            }
        }

        // 2. Extract from Data payload (overrides or complements notification payload)
        Map<String, String> data = remoteMessage.getData();
        if (data != null && !data.isEmpty()) {
            if (title == null || title.trim().isEmpty()) {
                title = data.get("title");
            }
            if (body == null || body.trim().isEmpty()) {
                body = data.get("body");
                if (body == null || body.trim().isEmpty()) {
                    body = data.get("message");
                }
            }
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                imageUrl = data.get("imageUrl");
                if (imageUrl == null || imageUrl.trim().isEmpty()) {
                    imageUrl = data.get("image");
                }
            }
        }

        if (title == null || title.trim().isEmpty()) {
            title = "FastestLap";
        }
        if (body == null) {
            body = "";
        }

        // 3. Determine payload routing (Session reminder, News alert, or General announcement)
        String from = remoteMessage.getFrom() != null ? remoteMessage.getFrom().toLowerCase() : "";
        String channelId = notification != null ? notification.getChannelId() : null;
        if (channelId == null && data != null) {
            channelId = data.get("channelId");
        }

        String type = (data != null) ? data.get("type") : null;
        if (type == null && data != null) {
            type = data.get("category");
        }

        boolean isSession = "session".equalsIgnoreCase(type)
                || "sessions".equalsIgnoreCase(type)
                || from.contains("sessions")
                || AppNotificationManager.CHANNEL_SESSIONS_ID.equals(channelId)
                || (data != null && (data.containsKey("raceName") || data.containsKey("sessionName")));

        boolean isNews = !isSession && ("news".equalsIgnoreCase(type)
                || from.contains("news")
                || AppNotificationManager.CHANNEL_NEWS_ID.equals(channelId)
                || (data != null && (data.containsKey("newsUrl") || data.containsKey("url"))));

        // If not explicitly flagged, perform contextual keyword analysis on title and body
        if (!isSession && !isNews) {
            String lowerContent = ((title != null ? title : "") + " " + (body != null ? body : "")).toLowerCase();
            if (lowerContent.contains("qualifiche") || lowerContent.contains("qualifying")
                    || lowerContent.contains("prove libere") || lowerContent.contains("fp1")
                    || lowerContent.contains("fp2") || lowerContent.contains("fp3")
                    || lowerContent.contains("sprint") || lowerContent.contains("gp ")
                    || lowerContent.contains("gran premio") || lowerContent.contains("grand prix")) {
                isSession = true;
            } else if (lowerContent.contains("notizia") || lowerContent.contains("news") || imageUrl != null) {
                isNews = true;
            }
        }

        if (isSession) {
            String raceName = (data != null) ? data.get("raceName") : null;
            String sessionName = (data != null) ? data.get("sessionName") : null;
            String sessionTime = (data != null) ? data.get("sessionTime") : null;

            if (raceName == null || raceName.trim().isEmpty()) raceName = title;
            if (sessionName == null || sessionName.trim().isEmpty()) sessionName = (body != null && !body.trim().isEmpty()) ? body : "Session";
            if (sessionTime == null) sessionTime = "";

            AppNotificationManager.getInstance().showSessionNotification(
                    this,
                    raceName,
                    sessionName,
                    sessionTime
            );

        } else if (isNews) {
            String newsUrl = (data != null) ? data.get("newsUrl") : null;
            if (newsUrl == null || newsUrl.trim().isEmpty()) {
                newsUrl = (data != null) ? data.get("url") : null;
            }

            AppNotificationManager.getInstance().showNewsNotification(
                    this,
                    title,
                    body,
                    newsUrl,
                    imageUrl
            );

        } else {
            // General push notification from Firebase Console or admin broadcast
            AppNotificationManager.getInstance().showGeneralNotification(
                    this,
                    title,
                    body,
                    imageUrl,
                    data
            );
        }
    }
}
