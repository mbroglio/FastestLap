package com.the_coffe_coders.fastestlap.ui.junior.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.adapter.junior.JuniorStandingsRecyclerAdapter;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.junior.standings.JuniorConstructorStandings;
import com.the_coffe_coders.fastestlap.ui.junior.viewmodel.JuniorCategoryViewModel;
import com.the_coffe_coders.fastestlap.ui.junior.viewmodel.JuniorCategoryViewModelFactory;
import com.the_coffe_coders.fastestlap.util.ui.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

public class JuniorConstructorStandingsFragment extends Fragment {
    private static final String TAG = "JuniorConstructorStandingsFragment";

    private View view;
    private int categoryType = 0; // 0: F2, 1: F3
    private JuniorCategoryViewModel juniorCategoryViewModel;
    private SwipeRefreshLayout constructorStandingsLayout;
    private RecyclerView constructorStandingsRecyclerView;
    private TextView contentNotAvailableLayout;
    private LoadingScreen loadingScreen;

    public JuniorConstructorStandingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            categoryType = getArguments().getInt("CATEGORY_TYPE", 0);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_junior_constructor_standings, container, false);
        setupFragment();
        return view;
    }

    private void setupFragment() {
        constructorStandingsLayout = view.findViewById(R.id.constructor_standings_layout);
        UIUtils.applyWindowInsets(constructorStandingsLayout);

        loadingScreen = new LoadingScreen(view, getContext(), null, constructorStandingsLayout);
        loadingScreen.showLoadingScreen(false);

        constructorStandingsRecyclerView = view.findViewById(R.id.constructor_standings_recycler_view);
        contentNotAvailableLayout = view.findViewById(R.id.content_not_available_layout);

        juniorCategoryViewModel = new ViewModelProvider(
                this,
                new JuniorCategoryViewModelFactory(requireActivity().getApplication())
        ).get(JuniorCategoryViewModel.class);

        constructorStandingsLayout.setOnRefreshListener(() -> {
            if (juniorCategoryViewModel != null) {
                juniorCategoryViewModel.refreshConstructorStandings(categoryType);
            }
            fetchConstructorStandings();
        });

        fetchConstructorStandings();
    }

    private void fetchConstructorStandings() {
        MutableLiveData<Result> standingsLiveData = juniorCategoryViewModel.getConstructorStandings(categoryType);
        if (standingsLiveData == null) {
            showContentNotAvailable();
            return;
        }

        standingsLiveData.observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.Loading) {
                return;
            }

            if (constructorStandingsLayout != null) {
                constructorStandingsLayout.setRefreshing(false);
            }

            if (result != null && result.isSuccess()) {
                JuniorConstructorStandings constructorStandings = ((Result.JuniorConstructorStandingsSuccess) result).getData();
                if (constructorStandings != null && constructorStandings.getConstructorStandings() != null && !constructorStandings.getConstructorStandings().isEmpty()) {
                    showContent();
                    constructorStandingsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
                    JuniorStandingsRecyclerAdapter adapter = new JuniorStandingsRecyclerAdapter(requireContext(), constructorStandings);
                    constructorStandingsRecyclerView.setAdapter(adapter);
                } else {
                    Log.w(TAG, "Constructor standings is null or empty");
                    showContentNotAvailable();
                }
            } else {
                Log.e(TAG, "Constructor standings fetch failed");
                showContentNotAvailable();
            }
        });
    }

    private void showContent() {
        if (constructorStandingsRecyclerView != null) {
            constructorStandingsRecyclerView.setVisibility(View.VISIBLE);
        }
        if (contentNotAvailableLayout != null) {
            contentNotAvailableLayout.setVisibility(View.GONE);
        }
        if (loadingScreen != null) {
            loadingScreen.hideLoadingScreen();
        }
    }

    private void showContentNotAvailable() {
        if (constructorStandingsRecyclerView != null) {
            constructorStandingsRecyclerView.setVisibility(View.GONE);
        }
        if (contentNotAvailableLayout != null) {
            contentNotAvailableLayout.setVisibility(View.VISIBLE);
        }
        if (loadingScreen != null) {
            loadingScreen.hideLoadingScreen();
        }
    }
}
