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

import com.google.android.material.materialswitch.MaterialSwitch;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.adapter.NewsRecyclerAdapter;
import com.the_coffe_coders.fastestlap.domain.news.News;
import com.the_coffe_coders.fastestlap.source.news.NewsFetcher;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.NetworkUtils;
import com.the_coffe_coders.fastestlap.util.ui.LoadingScreen;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NewsFragment extends Fragment {

    private final String currentLanguage = AppCompatDelegate.getApplicationLocales().toLanguageTags();
    private TextView newsMenu;
    private MaterialSwitch languageFeedSwitch;
    private Boolean languageFeed;
    private RecyclerView newsRecyclerView;
    private int defaultIndex;
    private static final String TAG = "NewsFragment";
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

        languageFeed = currentLanguage.equals("en-GB");

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
            loadNews(languageFeedSwitch.isChecked(), recyclerView, true, 0);
        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    R.string.feed_error, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Errore durante il recupero del feed RSS", e);
        }

        languageFeedSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            try {
                loadNews(isChecked, recyclerView, true, 0);
                defaultIndex = 0;
            } catch (Exception e) {
                Toast.makeText(requireContext(),
                        R.string.feed_error, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Errore durante il recupero del feed RSS", e);
            }
        });

    }

    private void loadNews(boolean languageFeed, RecyclerView recyclerView, boolean defaultSource, int value) {
        // Check if we have cached data
        boolean useCache = false;
        List<News> cachedNews = null;

        if (languageFeed) {
            // English news
            if (defaultSource && value == cachedEnglishSourceIndex && cachedEnglishNews != null) {
                useCache = true;
                cachedNews = cachedEnglishNews;
            } else if (!defaultSource && value == cachedEnglishSourceIndex && cachedEnglishNews != null) {
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
            displayNews(cachedNews, recyclerView);
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
                    if (defaultSource) {
                        newsList = NewsFetcher.fetchNewsEngSources(0);
                        defaultIndex = 0;
                    } else {
                        newsList = NewsFetcher.fetchNewsEngSources(value);
                        defaultIndex = value;
                    }
                    // Cache English news
                    cachedEnglishNews = newsList;
                    cachedEnglishSourceIndex = defaultSource ? 0 : value;
                } else {
                    newsList = NewsFetcher.fetchNewsItSources();
                    // Cache Italian news
                    cachedItalianNews = newsList;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching news", e);
            }

            List<News> finalNewsList = newsList;
            handler.post(() -> {
                if (finalNewsList != null && !finalNewsList.isEmpty()) {
                    displayNews(finalNewsList, recyclerView);
                } else {
                    Toast.makeText(getContext(), R.string.feed_error, Toast.LENGTH_SHORT).show();
                    decrementLoadingCounter();
                }
            });
        });
    }

    private void displayNews(List<News> newsList, RecyclerView recyclerView) {
        // Reduce items to wait for - only wait for first image to load
        int itemsToWait = 1;
        loadingCounter += itemsToWait;
        NewsRecyclerAdapter adapter = new NewsRecyclerAdapter(newsList, getContext(), this::decrementLoadingCounter, itemsToWait);
        recyclerView.setAdapter(adapter);
        recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                decrementLoadingCounter();
            }
        });
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


        if (defaultIndex >= 0) {
            listView.setItemChecked(defaultIndex, true);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(listView)
                .create();


        listView.setOnItemClickListener((parent, view, position, id) -> {

            for (int i = 0; i < sources.size(); i++) {
                listView.setItemChecked(i, i == position);
            }

            try {
                loadNews(languageFeed, newsRecyclerView, false, position);
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
