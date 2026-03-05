package com.the_coffe_coders.fastestlap.ui.junior.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.adapter.junior.JuniorResultsRecyclerAdapter;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.junior.result.JuniorResult;
import com.the_coffe_coders.fastestlap.ui.junior.viewmodel.JuniorCategoryViewModel;
import com.the_coffe_coders.fastestlap.ui.junior.viewmodel.JuniorCategoryViewModelFactory;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

public class JuniorResultsFragment extends Fragment {
    private static final String TAG = "JuniorResultsActivity";

    private View view;

    private int categoryType;
    private JuniorCategoryViewModel juniorCategoryViewModel;
    private SwipeRefreshLayout resultsLayout;
    private RecyclerView resultsrRecyclerView;
    private TextView contentNotAvailableLayout;
    private JuniorResultsRecyclerAdapter juniorResultsAdapter;


    public JuniorResultsFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            categoryType = getArguments().getInt("CATEGORY_TYPE");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_junior_results, container, false);
        /*
        resultsLayout = view.findViewById(R.id.results_layout);
        UIUtils.applyWindowInsets(resultsLayout);
*/
        setupFragment();

        return view;
    }

    private void setupFragment() {

        setToolbar();

        SwipeRefreshLayout layout = view.findViewById(R.id.results_layout);
        UIUtils.applyWindowInsets(layout);

        resultsrRecyclerView = view.findViewById(R.id.results_recycler_view);
        contentNotAvailableLayout = view.findViewById(R.id.content_not_available_layout);

        layout.setOnRefreshListener(() -> {
            setupFragment();
            layout.setRefreshing(false);
        });

        initializeViewModels();

        fetchResults();
    }

    private void setToolbar() {
        MaterialToolbar toolbar = view.findViewById(R.id.topAppBar);
        AppBarLayout appBarLayout = view.findViewById(R.id.top_bar_layout);

        // If fragment has its own toolbar (standalone mode)
        if (toolbar != null && appBarLayout != null) {
            toolbar.setNavigationOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

            if (categoryType == 0) {
                toolbar.setBackgroundColor(requireActivity().getColor(R.color.formula_2));
                appBarLayout.setBackgroundColor(requireActivity().getColor(R.color.formula_2));
            } else {
                toolbar.setBackgroundColor(requireActivity().getColor(R.color.app_primary_red));
                appBarLayout.setBackgroundColor(requireActivity().getColor(R.color.app_primary_red));
            }

            UIUtils.applyWindowInsets(toolbar);
        }
        // If using activity's toolbar (Navigation Component mode)
        else if (requireActivity() instanceof AppCompatActivity) {
            androidx.appcompat.widget.Toolbar activityToolbar =
                    requireActivity().findViewById(R.id.topAppBar);
            AppBarLayout activityAppBarLayout =
                    requireActivity().findViewById(R.id.top_bar_layout);

            if (activityToolbar != null && activityAppBarLayout != null) {
                if (categoryType == 0) {
                    activityToolbar.setBackgroundColor(requireActivity().getColor(R.color.formula_2));
                    activityAppBarLayout.setBackgroundColor(requireActivity().getColor(R.color.formula_2));
                } else {
                    activityToolbar.setBackgroundColor(requireActivity().getColor(R.color.app_primary_red));
                    activityAppBarLayout.setBackgroundColor(requireActivity().getColor(R.color.app_primary_red));
                }
            }
        }
    }

    private void initializeViewModels() {
        juniorCategoryViewModel = new ViewModelProvider(this, new JuniorCategoryViewModelFactory(requireActivity().getApplication())).get(JuniorCategoryViewModel.class);
    }

    private void fetchResults() {
        MutableLiveData<Result> resultsLiveData = juniorCategoryViewModel.getResults(categoryType);
        resultsLiveData.observe(requireActivity(), result -> {
            if (result != null) {
                if (result instanceof Result.Loading) {
                    return;
                }
                if (result.isSuccess()) {
                    showResults();
                    Log.i(TAG, "Results fetched successfully");

                    JuniorResult juniorResult = ((Result.JuniorResultSuccess) result).getData();

                    if (juniorResult != null) {
                        Log.i(TAG, "Junior result: " + juniorResult.getSeries());
                        Log.i(TAG, "Junior result: " + juniorResult);

                        resultsrRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

                        juniorResultsAdapter = new JuniorResultsRecyclerAdapter(requireContext(), juniorResult, requireActivity().getSupportFragmentManager(), categoryType);
                        resultsrRecyclerView.setAdapter(juniorResultsAdapter);

                        for (int i = 0; i < juniorResultsAdapter.getItemCount(); i++) {
                            juniorResultsAdapter.onBindViewHolder(
                                    juniorResultsAdapter.createViewHolder(resultsrRecyclerView, juniorResultsAdapter.getItemViewType(i)), i);
                        }
                    } else {
                        showContentNotAvailable();
                        Log.e(TAG, "Junior result is null");
                    }
                } else {
                    Log.e(TAG, "1 Results fetch failed");
                    showContentNotAvailable();
                }
            } else {
                Log.e(TAG, "2 Results fetch failed");
                showContentNotAvailable();
            }
        });

    }

    public void showResults() {
        resultsrRecyclerView.setVisibility(View.VISIBLE);
        contentNotAvailableLayout.setVisibility(View.GONE);
    }

    public void showContentNotAvailable() {
        resultsrRecyclerView.setVisibility(View.GONE);
        contentNotAvailableLayout.setVisibility(View.VISIBLE);
    }
}