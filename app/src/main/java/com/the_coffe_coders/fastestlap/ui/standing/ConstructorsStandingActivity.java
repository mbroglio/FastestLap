package com.the_coffe_coders.fastestlap.ui.standing;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.adapter.ConstructorStandingsRecyclerAdapter;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandings;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandingsElement;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.ConstructorViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.ConstructorViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.DriverViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.DriverViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.standing.viewmodel.ConstructorStandingsViewModel;
import com.the_coffe_coders.fastestlap.ui.standing.viewmodel.ConstructorStandingsViewModelFactory;
import com.the_coffe_coders.fastestlap.util.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.UIUtils;

import java.util.List;

public class ConstructorsStandingActivity extends AppCompatActivity {
    private static final String TAG = "TeamCardActivity";
    private final boolean constructorToProcess = true;
    LoadingScreen loadingScreen;

    private DriverViewModel driverViewModel;
    private ConstructorViewModel constructorViewModel;
    private SwipeRefreshLayout teamStandingLayout;
    private ConstructorStandings constructorStandings;
    private String constructorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_constructors_standing);

        constructorId = getIntent().getStringExtra("TEAM_ID");
        Log.i(TAG, "Constructor ID: " + constructorId);

        start();
    }

    private void start() {
        constructorViewModel = new ViewModelProvider(this, new ConstructorViewModelFactory(getApplication())).get(ConstructorViewModel.class);
        driverViewModel = new ViewModelProvider(this, new DriverViewModelFactory(getApplication())).get(DriverViewModel.class);

        teamStandingLayout = findViewById(R.id.team_standing_layout);
        loadingScreen = new LoadingScreen(getWindow().getDecorView(), this, teamStandingLayout, null);
        loadingScreen.showLoadingScreen(false);
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        UIUtils.applyWindowInsets(toolbar);

        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        UIUtils.applyWindowInsets(teamStandingLayout);

        teamStandingLayout.setOnRefreshListener(() -> {
            start();
            teamStandingLayout.setRefreshing(false);
        });

        setupPage();
    }

    private void setupPage() {
        ConstructorStandingsViewModel constructorStandingsViewModel = new ViewModelProvider(this, new ConstructorStandingsViewModelFactory(getApplication())).get(ConstructorStandingsViewModel.class);
        MutableLiveData<Result> liveData = constructorStandingsViewModel.getConstructorStandings();
        Log.i(TAG, "Constructor Standings: " + liveData);
        liveData.observe(this, result -> {
            if (result instanceof Result.Loading) {
                Log.i(TAG, "Constructor Standings LOADING");
                return;
            }
            if (result.isSuccess()) {
                constructorStandings = ((Result.ConstructorStandingsSuccess) result).getData();

                if (constructorStandings == null) {
                    Log.i(TAG, "Constructor Standings is null");
                    UIUtils.navigateToHomePage(this);
                } else {
                    List<ConstructorStandingsElement> constructorList = constructorStandings.getConstructorStandings();

                    RecyclerView constructorsStandingRecyclerView = findViewById(R.id.constructors_standing_recycler_view);
                    constructorsStandingRecyclerView.setLayoutManager(new LinearLayoutManager(this));

                    if (constructorList.isEmpty()) {
                        Log.i(TAG, "Constructor Standings is empty");
                    } else {
                        Log.i(TAG, "Constructor Standings is not empty");

                        ConstructorStandingsRecyclerAdapter constructorsStandingAdapter = new ConstructorStandingsRecyclerAdapter(this, constructorId, constructorList, driverViewModel, constructorViewModel, this, loadingScreen);
                        constructorsStandingRecyclerView.setAdapter(constructorsStandingAdapter);

                        for (int i = 0; i < constructorsStandingAdapter.getItemCount(); i++) {
                            constructorsStandingAdapter.onBindViewHolder(
                                    constructorsStandingAdapter.createViewHolder(constructorsStandingRecyclerView, constructorsStandingAdapter.getItemViewType(i)), i);
                        }

                    }
                }
            } else if (result instanceof Result.Error) {
                Result.Error error = (Result.Error) result;
                Log.e(TAG, "Error: " + error.getMessage());
                UIUtils.navigateToHomePage(this);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}