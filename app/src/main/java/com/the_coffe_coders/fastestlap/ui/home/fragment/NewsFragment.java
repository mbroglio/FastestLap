package com.the_coffe_coders.fastestlap.ui.home.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.adapter.NewsRecyclerAdapter;
import com.the_coffe_coders.fastestlap.domain.news.News;
import com.the_coffe_coders.fastestlap.source.news.NewsFetcher;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.notification.AppNotificationManager;
import com.the_coffe_coders.fastestlap.util.service.NetworkUtils;
import com.the_coffe_coders.fastestlap.util.ui.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NewsFragment extends Fragment {

    private static final String TAG = "NewsFragment";
    private TextView newsMenu;
    private MaterialSwitch languageFeedSwitch;
    private Boolean languageFeed;
    private RecyclerView newsRecyclerView;
    private NewsRecyclerAdapter newsAdapter;
    private final Set<String> expandedNewsIds = new HashSet<>();
    private int defaultIndex;
    private int loadingCounter = 0;
    private LoadingScreen loadingScreen;

    // Cache news data to avoid re-fetching
    private List<News> cachedEnglishNews = null;
    private List<News> cachedItalianNews = null;
    private int cachedEnglishSourceIndex = -1;

    public NewsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(getLayoutResource(), container, false);

        newsMenu = view.findViewById(R.id.news_menu_layout);
        languageFeedSwitch = view.findViewById(R.id.language_feed_switch);

        newsRecyclerView = view.findViewById(R.id.news_recycler_view);
        TextView noConnectionText = view.findViewById(R.id.no_connection_text);

        setupLoadingScreen(view);

        Context ctx = requireContext();
        boolean hasPref = AppNotificationManager.getInstance().hasSavedNewsSourcePreference(ctx);
        if (hasPref) {
            languageFeed = AppNotificationManager.getInstance().isSavedNewsLanguageEnglish(ctx);
            defaultIndex = AppNotificationManager.getInstance().getSavedNewsSourceIndex(ctx);
        } else {
            languageFeed = isAppLanguageEnglish();
            defaultIndex = 0;
            String defaultSource = languageFeed ? Constants.DEFAULT_ENG_SOURCE : Constants.DEFAULT_ITA_SOURCE;
            AppNotificationManager.getInstance().saveNewsSourcePreference(ctx, languageFeed, defaultIndex, defaultSource);
        }

        // Setup RecyclerView once
        newsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        List<News> initialNews = getCachedNews(languageFeed, defaultIndex);
        newsAdapter = new NewsRecyclerAdapter(
                initialNews != null ? initialNews : new ArrayList<>(),
                requireContext(),
                null,
                0,
                expandedNewsIds
        );
        newsRecyclerView.setAdapter(newsAdapter);

        // Setup header controls once
        if (newsMenu != null) {
            newsMenu.setOnClickListener(v -> {
                boolean isEnglish = languageFeedSwitch != null && languageFeedSwitch.isChecked();
                showSourcesDialog(isEnglish);
            });
        }

        if (languageFeedSwitch != null) {
            languageFeedSwitch.setChecked(languageFeed);
            languageFeedSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (languageFeed != null && languageFeed == isChecked) {
                    return;
                }
                languageFeed = isChecked;
                defaultIndex = 0;
                String sourceName = isChecked ? Constants.DEFAULT_ENG_SOURCE : Constants.DEFAULT_ITA_SOURCE;
                AppNotificationManager.getInstance().saveNewsSourcePreference(requireContext(), isChecked, defaultIndex, sourceName);

                if (newsAdapter != null) {
                    newsAdapter.clearExpandedStates();
                }
                newsRecyclerView.scrollToPosition(0);
                loadNews(isChecked, defaultIndex, false);
            });
        }

        NetworkUtils networkUtils = new NetworkUtils(requireContext());
        networkUtils.observe(getViewLifecycleOwner(), isConnected -> {
            Log.i(TAG, "network: " + isConnected);
            if (isConnected) {
                newsRecyclerView.setVisibility(View.VISIBLE);
                noConnectionText.setVisibility(View.GONE);

                // If no news loaded yet, load with loading screen; if already loaded, silent background refresh
                if (newsAdapter == null || newsAdapter.getItemCount() == 0) {
                    loadNews(languageFeed, defaultIndex, false);
                } else {
                    loadNews(languageFeed, defaultIndex, true);
                }
            } else {
                if (newsAdapter == null || newsAdapter.getItemCount() == 0) {
                    newsRecyclerView.setVisibility(View.GONE);
                    noConnectionText.setVisibility(View.VISIBLE);
                }
                loadingScreen.hideLoadingScreen();
            }
        });

        return view;
    }

    protected int getLayoutResource() {
        return R.layout.fragment_news;
    }

    private void setupLoadingScreen(View view) {
        loadingScreen = new LoadingScreen(view, getContext(), null, newsRecyclerView);
    }

    private synchronized void decrementLoadingCounter() {
        loadingCounter--;
        if (loadingCounter <= 0) {
            loadingCounter = 0;
            loadingScreen.hideLoadingScreen();
        }
    }

    private List<News> getCachedNews(boolean isEnglish, int sourceIndex) {
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

    private void loadNews(boolean isEnglish, int value, boolean isSilent) {
        // Check if we have cached data and it's not a silent refresh
        List<News> cached = getCachedNews(isEnglish, value);
        if (cached != null && !cached.isEmpty() && !isSilent) {
            Log.d(TAG, "Using cached news data");
            preloadNewsImagesAndDisplay(cached);
            return;
        }

        if (!isSilent) {
            loadingCounter = 1;
            loadingScreen.showLoadingScreen(false);
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            List<News> newsList = null;
            try {
                if (isEnglish) {
                    newsList = NewsFetcher.fetchNewsEngSources(value);
                    defaultIndex = value;
                    cachedEnglishNews = newsList;
                    cachedEnglishSourceIndex = value;
                } else {
                    newsList = NewsFetcher.fetchNewsItSources();
                    defaultIndex = 0;
                    cachedItalianNews = newsList;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching news", e);
            }

            List<News> finalNewsList = newsList;
            handler.post(() -> {
                if (!isAdded()) {
                    return;
                }
                if (finalNewsList != null && !finalNewsList.isEmpty()) {
                    preloadNewsImagesAndDisplay(finalNewsList);
                } else {
                    if (!isSilent) {
                        Toast.makeText(getContext(), R.string.feed_error, Toast.LENGTH_SHORT).show();
                        decrementLoadingCounter();
                    }
                }
            });
        });
    }

    private void preloadNewsImagesAndDisplay(List<News> newsList) {
        if (newsList == null || newsList.isEmpty() || !isAdded()) {
            return;
        }

        displayNews(newsList);

        java.util.List<String> imageUrls = new java.util.ArrayList<>();
        for (int i = 0; i < Math.min(7, newsList.size()); i++) {
            String img = newsList.get(i).getImageUrl();
            if (img != null && !img.isEmpty()) {
                imageUrls.add(img);
            }
        }
        if (!imageUrls.isEmpty() && getContext() != null) {
            UIUtils.preloadImagesInParallel(getContext(), imageUrls.toArray(new String[0]), null);
        }
    }

    private boolean isAppLanguageEnglish() {
        try {
            androidx.core.os.LocaleListCompat appLocales = AppCompatDelegate.getApplicationLocales();
            String langTag = appLocales.toLanguageTags();
            if (langTag != null && !langTag.isEmpty()) {
                return langTag.toLowerCase(java.util.Locale.ROOT).startsWith("en");
            }
            if (getContext() != null) {
                String systemLang = getContext().getResources().getConfiguration().getLocales().get(0).getLanguage();
                return systemLang != null && systemLang.toLowerCase(java.util.Locale.ROOT).startsWith("en");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking app language: " + e.getMessage());
        }
        return false;
    }

    private void displayNews(List<News> newsList) {
        if (!isAdded()) {
            return;
        }
        if (newsAdapter == null) {
            newsAdapter = new NewsRecyclerAdapter(new ArrayList<>(newsList), requireContext(), null, 0, expandedNewsIds);
            newsRecyclerView.setAdapter(newsAdapter);
        } else {
            newsAdapter.updateNewsList(newsList);
        }
        loadingScreen.hideLoadingScreen();
    }

    private void showSourcesDialog(boolean isEnglish) {
        final List<String> sources = isEnglish
                ? Constants.ENGLISH_NEWS_SOURCES
                : Constants.ITALIAN_NEWS_SOURCES;

        ListView listView = new ListView(requireContext());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_multiple_choice,
                sources
        );
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        if (defaultIndex >= 0 && defaultIndex < sources.size()) {
            listView.setItemChecked(defaultIndex, true);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(listView)
                .create();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            for (int i = 0; i < sources.size(); i++) {
                listView.setItemChecked(i, i == position);
            }

            defaultIndex = position;
            String sourceName = sources.get(position);
            AppNotificationManager.getInstance().saveNewsSourcePreference(requireContext(), languageFeedSwitch.isChecked(), position, sourceName);

            if (newsAdapter != null) {
                newsAdapter.clearExpandedStates();
            }
            newsRecyclerView.scrollToPosition(0);

            try {
                loadNews(languageFeedSwitch.isChecked(), position, false);
            } catch (Exception e) {
                Toast.makeText(requireContext(),
                        R.string.feed_error, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Errore durante il recupero del feed RSS", e);
            }

            dialog.dismiss();
        });

        dialog.show();
    }
}
