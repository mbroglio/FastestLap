package com.the_coffe_coders.fastestlap.util;

import static com.the_coffe_coders.fastestlap.util.Constants.AUTOSPORT_RSS_URL;
import static com.the_coffe_coders.fastestlap.util.Constants.CRASH_RSS_URL;
import static com.the_coffe_coders.fastestlap.util.Constants.MOTORSPORT_RSS_URL;
import android.util.Log;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.the_coffe_coders.fastestlap.domain.news.News;
import java.io.IOException;
import java.lang.InterruptedException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class NewsFetcher {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

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
        Future<List<News>> future = executor.submit(() -> {
            List<News> newsList = new ArrayList<>();

            try (XmlReader reader = new XmlReader(new URL(sourceUrl))) {
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
                    Log.i("NewsFetcher", "News fetched: " + title + " - " + link + " - " + description + " - " + date + " - " + category + " - " + imageUrl);
                }
            } catch (FeedException | IOException e) {
                throw new RuntimeException(e);
            }
            return newsList;
        });

        try {
            List<News> newsList = future.get();
            Log.i("NewsFetcher", "News fetched: " + newsList.size() + " items");
            return newsList;
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

}
