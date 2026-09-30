package com.the_coffe_coders.fastestlap.source.news;

import static com.the_coffe_coders.fastestlap.util.Constants.AUTOSPORT_RSS_URL;
import static com.the_coffe_coders.fastestlap.util.Constants.CRASH_RSS_URL;
import static com.the_coffe_coders.fastestlap.util.Constants.DEFAULT_JUNIOR_NEWS_TAG;
import static com.the_coffe_coders.fastestlap.util.Constants.F2_BASE_URL;
import static com.the_coffe_coders.fastestlap.util.Constants.F3_BASE_URL;
import static com.the_coffe_coders.fastestlap.util.Constants.MOTORSPORT_RSS_URL;

import android.util.Log;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.the_coffe_coders.fastestlap.domain.news.F1News;
import com.the_coffe_coders.fastestlap.domain.news.JuniorNews;
import com.the_coffe_coders.fastestlap.domain.news.News;
import com.the_coffe_coders.fastestlap.util.service.ServiceLocator;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NewsFetcher {

    private static final String TAG = "NewsFetcher";

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

    public static List<News> fetchJuniorNews(int series) {
        return fetchJuniorNews(series == 1 ? "f3" : "f2", 1);
    }

    public static List<News> fetchJuniorNews(int series, int maxPages) {
        return fetchJuniorNews(series == 1 ? "f3" : "f2", maxPages);
    }

    public static List<News> fetchJuniorNews(String series) {
        return fetchJuniorNews(series, 1);
    }

    public static List<News> fetchJuniorNews(String series, int maxPages) {
        return fetchJuniorNews(series, maxPages, null);
    }

    public static List<News> fetchJuniorNewsPage(String series, int page) {
        return fetchJuniorNewsPage(series, page, null);
    }

    public static List<News> fetchJuniorNewsPage(String series, int page, String tag) {
        List<News> newsList = new ArrayList<>();
        boolean isF3 = series != null && (series.equalsIgnoreCase("f3") || series.equalsIgnoreCase("formula3"));
        String baseUrl = isF3 ? F3_BASE_URL : F2_BASE_URL;
        String seriesId = isF3 ? "f3" : "f2";
        int pageNum = Math.max(1, page);

        String pageUrl;
        if (tag != null && !tag.trim().isEmpty() && !tag.trim().equals(DEFAULT_JUNIOR_NEWS_TAG)) {
            String activeTag = tag.trim();
            pageUrl = (pageNum == 1)
                    ? baseUrl + "/en/latest/tags/" + activeTag
                    : baseUrl + "/en/latest/tags/" + activeTag + "/slug_" + activeTag + "/page_" + pageNum;
        } else {
            pageUrl = (pageNum == 1)
                    ? baseUrl + "/en/latest"
                    : baseUrl + "/en/latest/page_" + pageNum;
        }

        okhttp3.OkHttpClient client = ServiceLocator.getInstance().getOkHttpClient();
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(pageUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build();

        try (okhttp3.Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                Log.w(TAG, "Failed to fetch junior news page " + pageNum + ": HTTP " + response.code());
                return newsList;
            }

            String html = response.body().string();
            Document doc = Jsoup.parse(html);
            Elements cardElements = doc.select("ul[class*=\"m9aRzq_grid\"] > li");

            for (Element li : cardElements) {
                Element titleAnchor = li.selectFirst(".ujvI6a_content a[class*=\"ujvI6a_title\"]");
                if (titleAnchor == null) {
                    titleAnchor = li.selectFirst("a");
                }
                String title = (titleAnchor != null) ? titleAnchor.text().trim() : "";
                String href = (titleAnchor != null) ? titleAnchor.attr("href") : "";

                if (title.isEmpty() || href.isEmpty()) {
                    continue;
                }

                String newsUrl = href;
                if (!newsUrl.startsWith("http://") && !newsUrl.startsWith("https://")) {
                    newsUrl = baseUrl + (newsUrl.startsWith("/") ? "" : "/") + newsUrl;
                }

                Element imgElement = li.selectFirst(".ujvI6a_image img");
                if (imgElement == null) {
                    imgElement = li.selectFirst("img");
                }
                String imageUrl = "";
                if (imgElement != null) {
                    imageUrl = imgElement.hasAttr("src") ? imgElement.attr("src") : imgElement.attr("data-src");
                    if (imageUrl.startsWith("//")) {
                        imageUrl = "https:" + imageUrl;
                    }
                }

                String published = "recently";
                String contextAttr = (titleAnchor != null) ? titleAnchor.attr("data-uds-a7s-context") : "";
                if (contextAttr.isEmpty()) {
                    Element aWithContext = li.selectFirst("a[data-uds-a7s-context]");
                    if (aWithContext != null) {
                        contextAttr = aWithContext.attr("data-uds-a7s-context");
                    }
                }

                if (!contextAttr.isEmpty()) {
                    byte[] decoded = decodeBase64(contextAttr);
                    if (decoded != null) {
                        try {
                            String jsonStr = new String(decoded, StandardCharsets.UTF_8);
                            JSONObject obj = new JSONObject(jsonStr);
                            if (obj.has("content_publish_datetime")) {
                                published = formatTimeAgo(obj.getString("content_publish_datetime"));
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }

                newsList.add(new JuniorNews(title, newsUrl, published, imageUrl, seriesId));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scraping junior news from " + pageUrl + ": " + e.getMessage());
        }

        return newsList;
    }

    public static List<News> fetchJuniorNews(String series, int maxPages, String tag) {
        List<News> newsList = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();
        int pages = Math.max(1, maxPages);

        for (int page = 1; page <= pages; page++) {
            List<News> pageItems = fetchJuniorNewsPage(series, page, tag);
            if (pageItems.isEmpty()) {
                break;
            }

            int addedCount = 0;
            for (News item : pageItems) {
                if (item.getLink() != null && seenUrls.add(item.getLink())) {
                    newsList.add(item);
                    addedCount++;
                }
            }

            if (addedCount == 0) {
                break;
            }
        }
        return newsList;
    }

    private static List<News> fetchF1News(String sourceUrl) {
        List<News> newsList = new ArrayList<>();
        try {
            okhttp3.OkHttpClient client = ServiceLocator.getInstance().getOkHttpClient();
            okhttp3.Request request = new okhttp3.Request.Builder().url(sourceUrl).header("User-Agent", "Mozilla/5.0 FastestLapApp").build();

            try (okhttp3.Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    try (InputStream is = response.body().byteStream(); XmlReader reader = new XmlReader(is)) {
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

                            newsList.add(new F1News(title, link, description, date, category, imageUrl));
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching news from " + sourceUrl + ": " + e.getMessage());
        }
        return newsList;
    }

    private static String formatTimeAgo(String isoString) {
        if (isoString == null || isoString.trim().isEmpty()) {
            return "recently";
        }
        try {
            long timeMillis = Instant.parse(isoString).toEpochMilli();
            long now = System.currentTimeMillis();
            long diffSeconds = Math.max(0, (now - timeMillis) / 1000);

            if (diffSeconds < 60) {
                return "just now";
            }
            long diffMinutes = diffSeconds / 60;
            if (diffMinutes < 60) {
                return diffMinutes == 1 ? "1 minute ago" : diffMinutes + " minutes ago";
            }
            long diffHours = diffMinutes / 60;
            if (diffHours < 24) {
                return diffHours == 1 ? "1 hour ago" : diffHours + " hours ago";
            }
            long diffDays = diffHours / 24;
            if (diffDays < 30) {
                return diffDays == 1 ? "1 day ago" : diffDays + " days ago";
            }
            long diffMonths = diffDays / 30;
            if (diffMonths < 12) {
                return diffMonths == 1 ? "1 month ago" : diffMonths + " months ago";
            }
            long diffYears = diffDays / 365;
            return diffYears == 1 ? "1 year ago" : diffYears + " years ago";
        } catch (Exception e) {
            return "recently";
        }
    }

    private static byte[] decodeBase64(String str) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                return Base64.getDecoder().decode(str);
            } else {
                return android.util.Base64.decode(str, android.util.Base64.DEFAULT);
            }
        } catch (Throwable t) {
            try {
                return android.util.Base64.decode(str, android.util.Base64.DEFAULT);
            } catch (Throwable t2) {
                return null;
            }
        }
    }
}
