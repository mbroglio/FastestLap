package com.the_coffe_coders.fastestlap.ui.home.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.adapter.NewsRecyclerAdapter;
import com.the_coffe_coders.fastestlap.domain.news.News;
import com.the_coffe_coders.fastestlap.ui.home.viewmodel.NewsViewModel;
import com.the_coffe_coders.fastestlap.ui.home.viewmodel.NewsViewModelFactory;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.notification.AppNotificationManager;
import com.the_coffe_coders.fastestlap.util.service.NetworkUtils;
import com.the_coffe_coders.fastestlap.util.ui.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NewsFragment extends Fragment {

    private static final String TAG = "NewsFragment";
    private TextView newsMenu;
    private MaterialSwitch languageFeedSwitch;
    private Boolean languageFeed;
    private RecyclerView newsRecyclerView;
    protected NewsRecyclerAdapter newsAdapter;
    private final Set<String> expandedNewsIds = new HashSet<>();
    private int defaultIndex;
    private LoadingScreen loadingScreen;
    protected NewsViewModel newsViewModel;

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

        newsViewModel = new ViewModelProvider(this, new NewsViewModelFactory()).get(NewsViewModel.class);

        // Setup RecyclerView once
        newsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        List<News> initialNews = getInitialNews();
        if (initialNews == null || initialNews.isEmpty()) {
            loadingScreen.showLoadingScreen(false);
        }
        newsAdapter = new NewsRecyclerAdapter(
                initialNews != null ? initialNews : new ArrayList<>(),
                requireContext(),
                null,
                0,
                expandedNewsIds
        );
        onAdapterCreated(newsAdapter);
        newsRecyclerView.setAdapter(newsAdapter);

        // Setup observers
        newsViewModel.getNewsLiveData().observe(getViewLifecycleOwner(), newsList -> {
            if (newsList != null && !newsList.isEmpty()) {
                preloadNewsImagesAndDisplay(newsList);
            }
        });

        newsViewModel.getIsLoadingLiveData().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null && isLoading) {
                loadingScreen.showLoadingScreen(false);
            } else {
                loadingScreen.hideLoadingScreen();
            }
        });

        newsViewModel.getErrorLiveData().observe(getViewLifecycleOwner(), isError -> {
            if (isError != null && isError) {
                Toast.makeText(getContext(), R.string.feed_error, Toast.LENGTH_SHORT).show();
                newsViewModel.consumeError();
            }
        });

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
                if (newsAdapter == null || newsAdapter.getNewsCount() == 0) {
                    loadNews(languageFeed, defaultIndex, false);
                } else {
                    loadNews(languageFeed, defaultIndex, true);
                }
            } else {
                if (newsAdapter == null || newsAdapter.getNewsCount() == 0) {
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

    protected List<News> getInitialNews() {
        return newsViewModel != null ? newsViewModel.getCachedNews(languageFeed, defaultIndex) : null;
    }

    private void setupLoadingScreen(View view) {
        loadingScreen = new LoadingScreen(view, getContext(), null, newsRecyclerView);
    }

    protected void loadNews(boolean isEnglish, int value, boolean isSilent) {
        defaultIndex = value;
        if (newsViewModel != null) {
            newsViewModel.loadNews(isEnglish, value, isSilent);
        }
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
        boolean wasEmpty = (newsAdapter == null || newsAdapter.getNewsCount() == 0);
        if (newsAdapter == null) {
            newsAdapter = new NewsRecyclerAdapter(new ArrayList<>(newsList), requireContext(), null, 0, expandedNewsIds);
            onAdapterCreated(newsAdapter);
            newsRecyclerView.setAdapter(newsAdapter);
        } else {
            newsAdapter.updateNewsList(newsList);
        }
        loadingScreen.hideLoadingScreen();
        onNewsDisplayed(newsList);

        if (wasEmpty && !newsList.isEmpty() && newsRecyclerView != null) {
            newsRecyclerView.scrollToPosition(0);
            if (newsRecyclerView.getLayoutManager() instanceof LinearLayoutManager) {
                ((LinearLayoutManager) newsRecyclerView.getLayoutManager()).scrollToPositionWithOffset(0, 0);
            }
            newsRecyclerView.post(() -> {
                if (isAdded() && newsRecyclerView != null) {
                    newsRecyclerView.scrollToPosition(0);
                    if (newsRecyclerView.getLayoutManager() instanceof LinearLayoutManager) {
                        ((LinearLayoutManager) newsRecyclerView.getLayoutManager()).scrollToPositionWithOffset(0, 0);
                    }
                }
            });
            newsRecyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                           int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    v.removeOnLayoutChangeListener(this);
                    if (newsRecyclerView.getLayoutManager() instanceof LinearLayoutManager) {
                        ((LinearLayoutManager) newsRecyclerView.getLayoutManager()).scrollToPositionWithOffset(0, 0);
                    }
                }
            });
        }
    }

    protected void onNewsDisplayed(List<News> newsList) {
        // Subclasses can customize behavior after news is displayed
    }

    protected void onAdapterCreated(NewsRecyclerAdapter adapter) {
        // Subclasses can customize adapter
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
