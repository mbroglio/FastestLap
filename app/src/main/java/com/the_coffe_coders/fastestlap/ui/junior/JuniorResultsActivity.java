package com.the_coffe_coders.fastestlap.ui.junior;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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

public class JuniorResultsActivity extends AppCompatActivity {
    private static final String TAG = "JuniorResultsActivity";

    private int categoryType;
    private JuniorCategoryViewModel juniorCategoryViewModel;
    private SwipeRefreshLayout resultsLayout;
    private RecyclerView resultsrRecyclerView;
    private JuniorResultsRecyclerAdapter juniorResultsAdapter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_junior_results);

        categoryType = getIntent().getIntExtra("CATEGORY_TYPE", 0);

        resultsLayout = findViewById(R.id.results_layout);
        UIUtils.applyWindowInsets(resultsLayout);

        setupPage();
    }

    private void setupPage() {

        setToolbar();

        SwipeRefreshLayout layout = findViewById(R.id.results_layout);
        UIUtils.applyWindowInsets(layout);

        resultsrRecyclerView = findViewById(R.id.results_recycler_view);

        layout.setOnRefreshListener(() -> {
            setupPage();
            layout.setRefreshing(false);
        });

        initializeViewModels();

        fetchResults();
    }

    private void setToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        AppBarLayout appBarLayout = findViewById(R.id.top_bar_layout);

        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        if(categoryType == 0){
            toolbar.setBackgroundColor(getColor(R.color.formula_2));
            appBarLayout.setBackgroundColor(getColor(R.color.formula_2));
        }else{
            toolbar.setBackgroundColor(getColor(R.color.ferrari_secondary));
            appBarLayout.setBackgroundColor(getColor(R.color.ferrari_secondary));
        }

        UIUtils.applyWindowInsets(toolbar);
    }

    private void initializeViewModels() {
        juniorCategoryViewModel = new ViewModelProvider(this, new JuniorCategoryViewModelFactory(getApplication())).get(JuniorCategoryViewModel.class);
    }

    private void fetchResults() {
        MutableLiveData<Result> resultsLiveData = juniorCategoryViewModel.getResults(categoryType);
        resultsLiveData.observe(this, result -> {
            if (result != null) {
                if (result instanceof Result.Loading) {
                    return;
                }
                if (result.isSuccess()) {
                    Log.i(TAG, "Results fetched successfully");

                    JuniorResult juniorResult = ((Result.JuniorResultSuccess) result).getData();

                    if (juniorResult != null) {
                        Log.i(TAG, "Junior result: " + juniorResult.getSeries());
                        Log.i(TAG, "Junior result: " + juniorResult);

                        resultsrRecyclerView.setLayoutManager(new LinearLayoutManager(this));

                        juniorResultsAdapter = new JuniorResultsRecyclerAdapter(this, juniorResult, getSupportFragmentManager(), categoryType);
                        resultsrRecyclerView.setAdapter(juniorResultsAdapter);

                        for (int i = 0; i < juniorResultsAdapter.getItemCount(); i++) {
                            juniorResultsAdapter.onBindViewHolder(
                                    juniorResultsAdapter.createViewHolder(resultsrRecyclerView, juniorResultsAdapter.getItemViewType(i)), i);
                        }
                    } else {
                        Log.e(TAG, "Junior result is null");
                        //displaysomethine
                    }

                }

            }
        });

    }
}