package com.the_coffe_coders.fastestlap.ui.home.fragment;

import android.app.AlertDialog;
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

import android.content.Context;
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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NewsFragment extends Fragment {

    private static final String TAG = "NewsFragment";
    private final String currentLanguage = AppCompatDelegate.getApplicationLocales().toLanguageTags();
    private TextView newsMenu;
    private MaterialSwitch languageFeedSwitch;
    private Boolean languageFeed;
    private RecyclerView newsRecyclerView;
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_news, container, false);

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

        NetworkUtils networkUtils = new NetworkUtils(requireContext());
        networkUtils.observe(getViewLifecycleOwner(), isConnected -> {
            Log.i("HomePageActivity", "network: " + isConnected);
            if (isConnected) {
                newsRecyclerView.setVisibility(View.VISIBLE);
                noConnectionText.setVisibility(View.GONE);

                newsMenuManagement();
                languageFeed(newsRecyclerView);
            } else {
                newsRecyclerView.setVisibility(View.GONE);
                noConnectionText.setVisibility(View.VISIBLE);
                // If offline, ensure loading screen is hidden
                loadingScreen.hideLoadingScreen();
            }
        });

        return view;
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

    private void newsMenuManagement() {
        newsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        if (newsMenu != null && languageFeedSwitch != null) {
            newsMenu.setOnClickListener(v -> {
                boolean isEnglish = languageFeedSwitch.isChecked();
                showSourcesDialog(isEnglish);
            });
        }
    }

    private void languageFeed(RecyclerView recyclerView) {
        languageFeedSwitch.setChecked(languageFeed);

        try {
            loadNews(languageFeed, recyclerView, defaultIndex);
        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    R.string.feed_error, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Errore durante il recupero del feed RSS", e);
        }

        languageFeedSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            try {
                languageFeed = isChecked;
                defaultIndex = 0;
                String sourceName = isChecked ? Constants.DEFAULT_ENG_SOURCE : Constants.DEFAULT_ITA_SOURCE;
                AppNotificationManager.getInstance().saveNewsSourcePreference(requireContext(), isChecked, defaultIndex, sourceName);
                loadNews(isChecked, recyclerView, defaultIndex);
            } catch (Exception e) {
                Toast.makeText(requireContext(),
                        R.string.feed_error, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Errore durante il recupero del feed RSS", e);
            }
        });

    }

    private void loadNews(boolean languageFeed, RecyclerView recyclerView, int value) {
        // Check if we have cached data
        boolean useCache = false;
        List<News> cachedNews = null;

        if (languageFeed) {
            // English news
            if (value == cachedEnglishSourceIndex && cachedEnglishNews != null) {
                useCache = true;
                cachedNews = cachedEnglishNews;
            }
        } else {
            // Italian news
            if (cachedItalianNews != null) {
                useCache = true;
                cachedNews = cachedItalianNews;
            }
        }

        if (useCache) {
            // Use cached data - instant loading!
            Log.d(TAG, "Using cached news data");
            preloadNewsImagesAndDisplay(cachedNews, recyclerView);
            return;
        }

        // No cache available - fetch from network
        loadingCounter = 1;
        loadingScreen.showLoadingScreen(false);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            List<News> newsList = null;
            try {
                if (languageFeed) {
                    newsList = NewsFetcher.fetchNewsEngSources(value);
                    defaultIndex = value;
                    // Cache English news
                    cachedEnglishNews = newsList;
                    cachedEnglishSourceIndex = value;
                } else {
                    newsList = NewsFetcher.fetchNewsItSources();
                    defaultIndex = 0;
                    // Cache Italian news
                    cachedItalianNews = newsList;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching news", e);
            }

            List<News> finalNewsList = newsList;
            handler.post(() -> {
                if (finalNewsList != null && !finalNewsList.isEmpty()) {
                    preloadNewsImagesAndDisplay(finalNewsList, recyclerView);
                } else {
                    Toast.makeText(getContext(), R.string.feed_error, Toast.LENGTH_SHORT).show();
                    decrementLoadingCounter();
                }
            });
        });
    }

    private void preloadNewsImagesAndDisplay(List<News> newsList, RecyclerView recyclerView) {
        if (newsList == null || newsList.isEmpty()) {
            return;
        }

        displayNews(newsList, recyclerView);

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

    private void displayNews(List<News> newsList, RecyclerView recyclerView) {
        NewsRecyclerAdapter adapter = new NewsRecyclerAdapter(newsList, getContext(), null, 0);
        recyclerView.setAdapter(adapter);
        loadingScreen.hideLoadingScreen();
    }

    private void showSourcesDialog(boolean isEnglish) {
        final List<String> sources = isEnglish
                ? Constants.ENGLISH_NEWS_SOURCES
                : Constants.ITALIAN_NEWS_SOURCES;

        // create ListView with multiple choice layout (checkboxes) but single choice mode
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

            try {
                loadNews(languageFeedSwitch.isChecked(), newsRecyclerView, position);
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
