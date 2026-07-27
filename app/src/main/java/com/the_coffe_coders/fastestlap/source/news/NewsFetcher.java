package com.the_coffe_coders.fastestlap.source.news;

import static com.the_coffe_coders.fastestlap.util.Constants.AUTOSPORT_RSS_URL;
import static com.the_coffe_coders.fastestlap.util.Constants.CRASH_RSS_URL;
import static com.the_coffe_coders.fastestlap.util.Constants.MOTORSPORT_RSS_URL;

import android.util.Log;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.the_coffe_coders.fastestlap.domain.news.News;

import java.util.ArrayList;
import java.util.List;


public class NewsFetcher {

    public static List<News> fetchNewsEngSources(int value) {
        List<News> newsList = new ArrayList<>();
        switch (value) {
            case 0:
                newsList.addAll(fetchF1News(AUTOSPORT_RSS_URL));
                break;
            case 1:
                newsList.addAll(fetchF1News(CRASH_RSS_URL));
                break;
        }
        return newsList;
    }

    public static List<News> fetchNewsItSources() {
        List<News> newsList = new ArrayList<>();
        newsList.addAll(fetchF1News(MOTORSPORT_RSS_URL));
        return newsList;
    }

    private static List<News> fetchF1News(String sourceUrl) {
        List<News> newsList = new ArrayList<>();
        try {
            okhttp3.OkHttpClient client = com.the_coffe_coders.fastestlap.util.ServiceLocator.getInstance().getOkHttpClient();
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(sourceUrl)
                    .header("User-Agent", "Mozilla/5.0 FastestLapApp")
                    .build();

            try (okhttp3.Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    try (java.io.InputStream is = response.body().byteStream();
                         XmlReader reader = new XmlReader(is)) {
                        SyndFeed feed = new SyndFeedInput().build(reader);
                        for (SyndEntry entry : feed.getEntries()) {
                            String title = entry.getTitle();
                            String link = entry.getLink();
                            String description = (entry.getDescription() != null) ? entry.getDescription().getValue() : "";
                            String date = (entry.getPublishedDate() != null) ? entry.getPublishedDate().toString() : "";
                            String category = entry.getCategories().isEmpty() ? "" : entry.getCategories().get(0).getName();
                            String imageUrl = null;

                            if (entry.getEnclosures() != null && !entry.getEnclosures().isEmpty()) {
                                imageUrl = entry.getEnclosures().get(0).getUrl();
                            }

                            newsList.add(new News(title, link, description, date, category, imageUrl));
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("NewsFetcher", "Error fetching news from " + sourceUrl + ": " + e.getMessage());
        }
        return newsList;
    }

}
