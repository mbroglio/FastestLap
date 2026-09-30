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
import com.the_coffe_coders.fastestlap.domain.junior.standings.JuniorDriverStandings;
import com.the_coffe_coders.fastestlap.ui.junior.viewmodel.JuniorCategoryViewModel;
import com.the_coffe_coders.fastestlap.ui.junior.viewmodel.JuniorCategoryViewModelFactory;
import com.the_coffe_coders.fastestlap.util.ui.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

public class JuniorDriverStandingsFragment extends Fragment {
    private static final String TAG = "JuniorDriverStandingsFragment";

    private View view;
    private int categoryType = 0; // 0: F2, 1: F3
    private JuniorCategoryViewModel juniorCategoryViewModel;
    private SwipeRefreshLayout driverStandingsLayout;
    private RecyclerView driverStandingsRecyclerView;
    private TextView contentNotAvailableLayout;
    private LoadingScreen loadingScreen;

    public JuniorDriverStandingsFragment() {
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
        view = inflater.inflate(R.layout.fragment_junior_driver_standings, container, false);
        setupFragment();
        return view;
    }

    private void setupFragment() {
        driverStandingsLayout = view.findViewById(R.id.driver_standings_layout);
        UIUtils.applyWindowInsets(driverStandingsLayout);

        loadingScreen = new LoadingScreen(view, getContext(), null, driverStandingsLayout);
        loadingScreen.showLoadingScreen(false);

        driverStandingsRecyclerView = view.findViewById(R.id.driver_standings_recycler_view);
        contentNotAvailableLayout = view.findViewById(R.id.content_not_available_layout);

        juniorCategoryViewModel = new ViewModelProvider(
                this,
                new JuniorCategoryViewModelFactory(requireActivity().getApplication())
        ).get(JuniorCategoryViewModel.class);

        driverStandingsLayout.setOnRefreshListener(() -> {
            if (juniorCategoryViewModel != null) {
                juniorCategoryViewModel.refreshDriverStandings(categoryType);
            }
            fetchDriverStandings();
        });

        fetchDriverStandings();
    }

    private void fetchDriverStandings() {
        MutableLiveData<Result> standingsLiveData = juniorCategoryViewModel.getDriverStandings(categoryType);
        if (standingsLiveData == null) {
            showContentNotAvailable();
            return;
        }

        standingsLiveData.observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.Loading) {
                return;
            }

            if (driverStandingsLayout != null) {
                driverStandingsLayout.setRefreshing(false);
            }

            if (result != null && result.isSuccess()) {
                JuniorDriverStandings driverStandings = ((Result.JuniorDriverStandingsSuccess) result).getData();
                if (driverStandings != null && driverStandings.getDriverStandings() != null && !driverStandings.getDriverStandings().isEmpty()) {
                    showContent();
                    driverStandingsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
                    JuniorStandingsRecyclerAdapter adapter = new JuniorStandingsRecyclerAdapter(requireContext(), driverStandings);
                    driverStandingsRecyclerView.setAdapter(adapter);
                } else {
                    Log.w(TAG, "Driver standings is null or empty");
                    showContentNotAvailable();
                }
            } else {
                Log.e(TAG, "Driver standings fetch failed");
                showContentNotAvailable();
            }
        });
    }

    private void showContent() {
        if (driverStandingsRecyclerView != null) {
            driverStandingsRecyclerView.setVisibility(View.VISIBLE);
        }
        if (contentNotAvailableLayout != null) {
            contentNotAvailableLayout.setVisibility(View.GONE);
        }
        if (loadingScreen != null) {
            loadingScreen.hideLoadingScreen();
        }
    }

    private void showContentNotAvailable() {
        if (driverStandingsRecyclerView != null) {
            driverStandingsRecyclerView.setVisibility(View.GONE);
        }
        if (contentNotAvailableLayout != null) {
            contentNotAvailableLayout.setVisibility(View.VISIBLE);
        }
        if (loadingScreen != null) {
            loadingScreen.hideLoadingScreen();
        }
    }
}
