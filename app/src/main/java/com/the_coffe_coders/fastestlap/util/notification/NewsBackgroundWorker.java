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

import java.util.List;

/**
 * Background worker executed periodically by WorkManager (even when app is killed/closed)
 * to fetch the latest F1 news and trigger a local notification via AppNotificationManager if new articles exist.
 */
public class NewsBackgroundWorker extends Worker {

    private static final String TAG = "NewsBackgroundWorker";
    private static final String PREF_NAME = "fastestlap_notification_prefs";
    private static final String KEY_LAST_NOTIFIED_LINK = "last_notified_news_link";

    public NewsBackgroundWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.i(TAG, "NewsBackgroundWorker started execution in background.");
        try {
            boolean isItalian = "it".equalsIgnoreCase(java.util.Locale.getDefault().getLanguage());
            List<News> newsList;
            if (isItalian) {
                newsList = NewsFetcher.fetchNewsItSources();
                if (newsList == null || newsList.isEmpty()) {
                    newsList = NewsFetcher.fetchNewsEngSources(0);
                }
            } else {
                newsList = NewsFetcher.fetchNewsEngSources(0);
                if (newsList == null || newsList.isEmpty()) {
                    newsList = NewsFetcher.fetchNewsItSources();
                }
            }


            if (newsList.isEmpty()) {
                Log.i(TAG, "No news fetched in background.");
                return Result.success();
            }

            News latestNews = newsList.get(0);
            String latestLink = latestNews.getLink();

            SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String lastNotifiedLink = prefs.getString(KEY_LAST_NOTIFIED_LINK, "");

            // If this is a brand new article link we haven't notified yet
            if (latestLink != null && !latestLink.equals(lastNotifiedLink)) {
                // Save new link
                prefs.edit().putString(KEY_LAST_NOTIFIED_LINK, latestLink).apply();

                // Clean description HTML tags for notification summary
                String rawDescription = latestNews.getDescription() != null ? latestNews.getDescription() : "";
                String cleanDescription = Html.fromHtml(rawDescription, Html.FROM_HTML_MODE_LEGACY).toString().trim();

                // Post notification via centralized AppNotificationManager with image URL
                AppNotificationManager.getInstance().showNewsNotification(
                        getApplicationContext(),
                        latestNews.getTitle(),
                        cleanDescription,
                        latestLink,
                        latestNews.getImageUrl()
                );


                Log.i(TAG, "New article found and notification triggered: " + latestNews.getTitle());
            } else {
                Log.i(TAG, "No new articles since last background check.");
            }



            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error in NewsBackgroundWorker: " + e.getMessage(), e);
            return Result.retry();
        }
    }
}
