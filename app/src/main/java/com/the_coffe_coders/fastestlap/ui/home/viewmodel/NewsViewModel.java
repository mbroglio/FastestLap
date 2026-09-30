package com.the_coffe_coders.fastestlap.ui.home.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.the_coffe_coders.fastestlap.domain.news.F1News;
import com.the_coffe_coders.fastestlap.domain.news.JuniorNews;
import com.the_coffe_coders.fastestlap.domain.news.News;
import com.the_coffe_coders.fastestlap.source.news.NewsFetcher;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NewsViewModel extends ViewModel {
    private static final String TAG = "NewsViewModel";

    private final MutableLiveData<List<News>> newsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isLoadingMoreLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> errorLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> hasMoreJuniorNewsLiveData = new MutableLiveData<>(false);

    // Cache news data to avoid unnecessary network fetches
    private static List<News> cachedEnglishNews = null;
    private static List<News> cachedItalianNews = null;
    private static int cachedEnglishSourceIndex = -1;
    private static final Map<String, List<News>> cachedJuniorNews = new HashMap<>();
    private static final Map<String, Integer> juniorCurrentPageMap = new HashMap<>();

    private final ExecutorService executor;

    public NewsViewModel() {
        this(Executors.newSingleThreadExecutor());
    }

    public NewsViewModel(ExecutorService executor) {
        this.executor = executor != null ? executor : Executors.newSingleThreadExecutor();
    }

    public LiveData<List<News>> getNewsLiveData() {
        return newsLiveData;
    }

    public LiveData<Boolean> getIsLoadingLiveData() {
        return isLoadingLiveData;
    }

    public LiveData<Boolean> getIsLoadingMoreLiveData() {
        return isLoadingMoreLiveData;
    }

    public LiveData<Boolean> getHasMoreJuniorNewsLiveData() {
        return hasMoreJuniorNewsLiveData;
    }

    public LiveData<Boolean> getErrorLiveData() {
        return errorLiveData;
    }

    public void consumeError() {
        errorLiveData.setValue(false);
    }

    public List<News> getCachedNews(boolean isEnglish, int sourceIndex) {
        if (isEnglish) {
            if (sourceIndex == cachedEnglishSourceIndex && cachedEnglishNews != null) {
                return cachedEnglishNews;
            }
        } else {
            if (cachedItalianNews != null) {
                return cachedItalianNews;
            }
        }
        return null;
    }

    public List<News> getCachedJuniorNews(String seriesId) {
        if (seriesId == null) return null;
        String key = normalizeJuniorSeries(seriesId);
        return cachedJuniorNews.get(key);
    }

    public List<News> getCachedJuniorNews(int series) {
        return getCachedJuniorNews(series == 1 ? "f3" : "f2");
    }

    private static String normalizeJuniorSeries(String series) {
        if (series == null) return "f2";
        String s = series.trim().toLowerCase(Locale.ROOT);
        return (s.equals("f3") || s.equals("formula3")) ? "f3" : "f2";
    }

    public void loadNews(boolean isEnglish, int value, boolean isSilent) {
        List<News> cached = getCachedNews(isEnglish, value);
        if (cached != null && !cached.isEmpty() && !isSilent) {
            Log.d(TAG, "Using cached news data");
            newsLiveData.setValue(cached);
            return;
        }

        if (!isSilent) {
            isLoadingLiveData.setValue(true);
        }

        executor.execute(() -> {
            List<News> rawList = null;
            try {
                if (isEnglish) {
                    rawList = NewsFetcher.fetchNewsEngSources(value);
                } else {
                    rawList = NewsFetcher.fetchNewsItSources();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching news from NewsFetcher", e);
            }

            List<News> cleanedList = cleanNews(rawList);

            if (!cleanedList.isEmpty()) {
                if (isEnglish) {
                    cachedEnglishNews = cleanedList;
                    cachedEnglishSourceIndex = value;
                } else {
                    cachedItalianNews = cleanedList;
                }
                newsLiveData.postValue(cleanedList);
            } else {
                if (!isSilent) {
                    errorLiveData.postValue(true);
                }
            }

            if (!isSilent) {
                isLoadingLiveData.postValue(false);
            }
        });
    }

    public void loadJuniorNews(String seriesId, boolean isSilent) {
        loadJuniorNews(seriesId, 1, isSilent);
    }

    public void loadJuniorNews(int series, boolean isSilent) {
        loadJuniorNews(series == 1 ? "f3" : "f2", 1, isSilent);
    }

    public void loadJuniorNews(String seriesId, int maxPages, boolean isSilent) {
        String normalizedSeries = normalizeJuniorSeries(seriesId);

        List<News> cached = getCachedJuniorNews(normalizedSeries);
        if (cached != null && !cached.isEmpty() && !isSilent) {
            Log.d(TAG, "Using cached junior news data for " + normalizedSeries);
            hasMoreJuniorNewsLiveData.setValue(true);
            newsLiveData.setValue(cached);
            return;
        }

        if (!isSilent) {
            isLoadingLiveData.setValue(true);
        }

        executor.execute(() -> {
            List<News> rawList = null;
            try {
                rawList = NewsFetcher.fetchJuniorNews(normalizedSeries, maxPages);
            } catch (Exception e) {
                Log.e(TAG, "Error fetching junior news for " + normalizedSeries, e);
            }

            List<News> cleanedList = cleanNews(rawList);

            if (!cleanedList.isEmpty()) {
                juniorCurrentPageMap.put(normalizedSeries, Math.max(1, maxPages));
                cachedJuniorNews.put(normalizedSeries, cleanedList);
                hasMoreJuniorNewsLiveData.postValue(true);
                newsLiveData.postValue(cleanedList);
            } else {
                hasMoreJuniorNewsLiveData.postValue(false);
                if (!isSilent) {
                    errorLiveData.postValue(true);
                }
            }

            if (!isSilent) {
                isLoadingLiveData.postValue(false);
            }
        });
    }

    public void loadMoreJuniorNews(String seriesId) {
        String normalizedSeries = normalizeJuniorSeries(seriesId);
        int nextPage = juniorCurrentPageMap.getOrDefault(normalizedSeries, 1) + 1;
        loadMoreJuniorNews(normalizedSeries, nextPage);
    }

    public void loadMoreJuniorNews(String seriesId, int page) {
        String normalizedSeries = normalizeJuniorSeries(seriesId);

        if (Boolean.TRUE.equals(isLoadingMoreLiveData.getValue())) {
            return;
        }

        isLoadingMoreLiveData.setValue(true);

        executor.execute(() -> {
            List<News> rawList = null;
            try {
                rawList = NewsFetcher.fetchJuniorNewsPage(normalizedSeries, page);
            } catch (Exception e) {
                Log.e(TAG, "Error fetching junior news page " + page + " for " + normalizedSeries, e);
            }

            List<News> cleanedList = cleanNews(rawList);

            if (!cleanedList.isEmpty()) {
                List<News> current = cachedJuniorNews.get(normalizedSeries);
                if (current == null) {
                    current = new ArrayList<>();
                }
                List<News> combined = new ArrayList<>(current);
                java.util.Set<String> existingLinks = new java.util.HashSet<>();
                for (News n : combined) {
                    if (n.getLink() != null) {
                        existingLinks.add(n.getLink().trim());
                    }
                }

                int addedCount = 0;
                for (News n : cleanedList) {
                    String link = n.getLink() != null ? n.getLink().trim() : "";
                    if (!link.isEmpty() && existingLinks.add(link)) {
                        combined.add(n);
                        addedCount++;
                    }
                }

                if (addedCount > 0) {
                    juniorCurrentPageMap.put(normalizedSeries, page);
                    cachedJuniorNews.put(normalizedSeries, combined);
                    hasMoreJuniorNewsLiveData.postValue(true);
                    newsLiveData.postValue(combined);
                } else {
                    hasMoreJuniorNewsLiveData.postValue(false);
                }
            } else {
                hasMoreJuniorNewsLiveData.postValue(false);
            }

            isLoadingMoreLiveData.postValue(false);
        });
    }

    /**
     * Cleans and sanitizes the news items retrieved from the source.
     * Filters out null or invalid entries, trims texts and formats descriptions.
     */
    private List<News> cleanNews(List<News> rawList) {
        if (rawList == null) {
            return Collections.emptyList();
        }

        List<News> cleaned = new ArrayList<>();
        for (News item : rawList) {
            if (item == null) continue;

            String title = item.getTitle();
            String link = item.getLink();

            if (title == null || title.trim().isEmpty() || link == null || link.trim().isEmpty()) {
                continue;
            }

            String cleanTitle = title.trim();
            String cleanLink = link.trim();
            String imageUrl = item.getImageUrl() != null ? item.getImageUrl().trim() : null;

            if (item instanceof F1News) {
                F1News f1Item = (F1News) item;
                String desc = f1Item.getDescription();
                if (desc != null && !desc.trim().isEmpty()) {
                    desc = UIUtils.formatXmlText(desc.trim());
                } else {
                    desc = "";
                }
                String date = f1Item.getDate() != null ? f1Item.getDate().trim() : "";
                String category = f1Item.getCategory() != null ? f1Item.getCategory().trim() : "";

                cleaned.add(new F1News(cleanTitle, cleanLink, desc, date, category, imageUrl));
            } else if (item instanceof JuniorNews) {
                JuniorNews jItem = (JuniorNews) item;
                String pubAgo = jItem.getPublishedAgo() != null ? jItem.getPublishedAgo().trim() : "";
                String seriesId = jItem.getSeriesId() != null ? jItem.getSeriesId().trim() : null;
                cleaned.add(new JuniorNews(cleanTitle, cleanLink, pubAgo, imageUrl, seriesId));
            } else {
                cleaned.add(item);
            }
        }
        return cleaned;
    }

    public void clearCache() {
        cachedEnglishNews = null;
        cachedItalianNews = null;
        cachedEnglishSourceIndex = -1;
        cachedJuniorNews.clear();
        juniorCurrentPageMap.clear();
        hasMoreJuniorNewsLiveData.postValue(false);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
