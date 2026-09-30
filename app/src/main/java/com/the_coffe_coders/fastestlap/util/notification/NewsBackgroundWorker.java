package com.the_coffe_coders.fastestlap.util.notification;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Html;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.the_coffe_coders.fastestlap.domain.news.News;
import com.the_coffe_coders.fastestlap.source.news.NewsFetcher;
import com.the_coffe_coders.fastestlap.util.Constants;

import java.util.List;

/**
 * Background worker executed periodically by WorkManager (even when app is killed/closed)
 * to fetch the latest F1 news and trigger a local notification via AppNotificationManager if new articles exist.
 */
public class NewsBackgroundWorker extends Worker {

    private static final String TAG = "NewsBackgroundWorker";
    private static final String PREF_NAME = "fastestlap_notification_prefs";
    private static final String KEY_LAST_NOTIFIED_LINK = "last_notified_news_link";
    private static final String KEY_LAST_NOTIFIED_F2_LINK = "last_notified_f2_news_link";
    private static final String KEY_LAST_NOTIFIED_F3_LINK = "last_notified_f3_news_link";

    public NewsBackgroundWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.i(TAG, "NewsBackgroundWorker started execution in background.");
        SharedPreferences notifPrefs = getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences userPrefs = getApplicationContext().getSharedPreferences(Constants.SHARED_PREFERENCES_FILENAME, Context.MODE_PRIVATE);

        // 1. Check F1 News
        try {
            checkF1News(notifPrefs);
        } catch (Exception e) {
            Log.e(TAG, "Error checking F1 news: " + e.getMessage(), e);
        }

        // 2. Check F2 News (if user enabled notifications for F2 news)
        boolean f2MasterEnabled = userPrefs.getBoolean(Constants.PREF_NOTIF_F2_ENABLED, false);
        boolean f2NewsEnabled = userPrefs.getBoolean(Constants.PREF_NOTIF_F2_NEWS, true);
        if (f2MasterEnabled && f2NewsEnabled) {
            try {
                checkJuniorNews("f2", KEY_LAST_NOTIFIED_F2_LINK, notifPrefs);
            } catch (Exception e) {
                Log.e(TAG, "Error checking F2 news: " + e.getMessage(), e);
            }
        } else {
            Log.d(TAG, "F2 news notifications disabled in preferences.");
        }

        // 3. Check F3 News (if user enabled notifications for F3 news)
        boolean f3MasterEnabled = userPrefs.getBoolean(Constants.PREF_NOTIF_F3_ENABLED, false);
        boolean f3NewsEnabled = userPrefs.getBoolean(Constants.PREF_NOTIF_F3_NEWS, true);
        if (f3MasterEnabled && f3NewsEnabled) {
            try {
                checkJuniorNews("f3", KEY_LAST_NOTIFIED_F3_LINK, notifPrefs);
            } catch (Exception e) {
                Log.e(TAG, "Error checking F3 news: " + e.getMessage(), e);
            }
        } else {
            Log.d(TAG, "F3 news notifications disabled in preferences.");
        }

        return Result.success();
    }

    private void checkF1News(SharedPreferences notifPrefs) {
        String sourceId = AppNotificationManager.getInstance().getSavedNewsSourceId(getApplicationContext());
        List<News> newsList;
        if (Constants.NEWS_SOURCE_ID_AUTOSPORT.equalsIgnoreCase(sourceId)) {
            newsList = NewsFetcher.fetchNewsEngSources(0);
            if (newsList == null || newsList.isEmpty()) {
                newsList = NewsFetcher.fetchNewsItSources();
            }
        } else if (Constants.NEWS_SOURCE_ID_CRASH.equalsIgnoreCase(sourceId)) {
            newsList = NewsFetcher.fetchNewsEngSources(1);
            if (newsList == null || newsList.isEmpty()) {
                newsList = NewsFetcher.fetchNewsItSources();
            }
        } else {
            newsList = NewsFetcher.fetchNewsItSources();
            if (newsList == null || newsList.isEmpty()) {
                newsList = NewsFetcher.fetchNewsEngSources(0);
            }
        }

        if (newsList == null || newsList.isEmpty()) {
            Log.i(TAG, "No F1 news fetched in background.");
            return;
        }

        News latestNews = newsList.get(0);
        String latestLink = latestNews.getLink();
        if (latestLink == null || latestLink.trim().isEmpty()) {
            return;
        }

        String lastNotifiedLink = notifPrefs.getString(KEY_LAST_NOTIFIED_LINK, "");

        // If this is the very first execution, initialize baseline link without notifying
        if (lastNotifiedLink.isEmpty()) {
            Log.i(TAG, "First background check initialized with F1 baseline article: " + latestLink);
            notifPrefs.edit().putString(KEY_LAST_NOTIFIED_LINK, latestLink).apply();
            return;
        }

        // If this is a brand new article link we haven't notified yet
        if (!latestLink.equals(lastNotifiedLink)) {
            notifPrefs.edit().putString(KEY_LAST_NOTIFIED_LINK, latestLink).apply();

            String rawDescription = latestNews.getDescription() != null ? latestNews.getDescription() : "";
            String cleanDescription = Html.fromHtml(rawDescription, Html.FROM_HTML_MODE_LEGACY).toString().trim();

            AppNotificationManager.getInstance().showNewsNotification(
                    getApplicationContext(),
                    latestNews.getTitle(),
                    cleanDescription,
                    latestLink,
                    latestNews.getImageUrl()
            );

            Log.i(TAG, "New F1 article found and notification triggered: " + latestNews.getTitle());
        } else {
            Log.i(TAG, "No new F1 articles since last background check.");
        }
    }

    private void checkJuniorNews(String seriesId, String prefKey, SharedPreferences notifPrefs) {
        String seriesName = seriesId.equalsIgnoreCase("f3") ? "Formula 3" : "Formula 2";
        String seriesTag = seriesId.equalsIgnoreCase("f3") ? "[F3]" : "[F2]";
        Log.i(TAG, "Checking " + seriesName + " news in background...");

        List<News> newsList = NewsFetcher.fetchJuniorNewsPage(seriesId, 1);
        if (newsList == null || newsList.isEmpty()) {
            Log.i(TAG, "No " + seriesName + " news fetched in background.");
            return;
        }

        News latestNews = newsList.get(0);
        String latestLink = latestNews.getLink();
        if (latestLink == null || latestLink.trim().isEmpty()) {
            return;
        }

        String lastNotifiedLink = notifPrefs.getString(prefKey, "");

        // If this is the very first execution, initialize baseline link without notifying
        if (lastNotifiedLink.isEmpty()) {
            Log.i(TAG, "First background check initialized with " + seriesName + " baseline article: " + latestLink);
            notifPrefs.edit().putString(prefKey, latestLink).apply();
            return;
        }

        // If this is a brand new article link we haven't notified yet
        if (!latestLink.equals(lastNotifiedLink)) {
            notifPrefs.edit().putString(prefKey, latestLink).apply();

            String title = seriesTag + " " + latestNews.getTitle();
            String summary;
            if (latestNews.getDescription() != null && !latestNews.getDescription().trim().isEmpty()) {
                summary = Html.fromHtml(latestNews.getDescription(), Html.FROM_HTML_MODE_LEGACY).toString().trim();
            } else {
                String formattedDate = latestNews.getFormattedDate(getApplicationContext());
                summary = (formattedDate != null && !formattedDate.trim().isEmpty())
                        ? seriesName + " • " + formattedDate
                        : seriesName;
            }

            AppNotificationManager.getInstance().showNewsNotification(
                    getApplicationContext(),
                    title,
                    summary,
                    latestLink,
                    latestNews.getImageUrl()
            );

            Log.i(TAG, "New " + seriesName + " article found and notification triggered: " + latestNews.getTitle());
        } else {
            Log.i(TAG, "No new " + seriesName + " articles since last background check.");
        }
    }
}
