package com.the_coffe_coders.fastestlap.ui.junior.fragment;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.adapter.NewsRecyclerAdapter;
import com.the_coffe_coders.fastestlap.domain.news.News;
import com.the_coffe_coders.fastestlap.ui.home.fragment.NewsFragment;
import com.the_coffe_coders.fastestlap.ui.junior.Formula3Activity;

import java.util.List;

import lombok.Getter;

@Getter
public class JuniorNewsFragment extends NewsFragment {

    private String seriesId = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        resolveSeriesId();
    }

    private void resolveSeriesId() {
        if (getArguments() != null) {
            if (getArguments().getString("SERIES_ID") != null) {
                seriesId = getArguments().getString("SERIES_ID");
                return;
            }
            if (getArguments().containsKey("CATEGORY_TYPE")) {
                int cat = getArguments().getInt("CATEGORY_TYPE");
                seriesId = (cat == 1) ? "f3" : "f2";
                return;
            }
        }
        if (getActivity() instanceof Formula3Activity) {
            seriesId = "f3";
        } else {
            seriesId = "f2";
        }
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_junior_news;
    }

    @Override
    protected List<News> getInitialNews() {
        if (newsViewModel != null) {
            return newsViewModel.getCachedJuniorNews(seriesId);
        }
        return null;
    }

    @Override
    protected void loadNews(boolean isEnglish, int value, boolean isSilent) {
        if (newsViewModel != null) {
            newsViewModel.loadJuniorNews(seriesId, isSilent);
        }
    }

    @Override
    protected void onAdapterCreated(NewsRecyclerAdapter adapter) {
        super.onAdapterCreated(adapter);
        if (adapter != null) {
            adapter.setOnLoadMoreListener(() -> {
                if (newsViewModel != null) {
                    newsViewModel.loadMoreJuniorNews(seriesId);
                }
            });
            updateLoadMoreState();
        }
    }

    @Override
    protected void onNewsDisplayed(List<News> newsList) {
        super.onNewsDisplayed(newsList);
        updateLoadMoreState();
    }

    private void updateLoadMoreState() {
        if (newsAdapter != null && newsViewModel != null) {
            Boolean hasMore = newsViewModel.getHasMoreJuniorNewsLiveData().getValue();
            boolean shouldEnable = Boolean.TRUE.equals(hasMore) && newsAdapter.getNewsCount() > 0;
            newsAdapter.setLoadMoreEnabled(shouldEnable);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (newsViewModel != null) {
            newsViewModel.getIsLoadingMoreLiveData().observe(getViewLifecycleOwner(), isLoadingMore -> {
                if (newsAdapter != null) {
                    newsAdapter.setLoadingMore(Boolean.TRUE.equals(isLoadingMore));
                }
            });

            newsViewModel.getHasMoreJuniorNewsLiveData().observe(getViewLifecycleOwner(), hasMore -> {
                updateLoadMoreState();
            });
        }
    }
}
