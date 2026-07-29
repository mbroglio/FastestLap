package com.the_coffe_coders.fastestlap.ui.standing;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.adapter.f1.ConstructorStandingsRecyclerAdapter;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.f1.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.f1.standing.ConstructorStandings;
import com.the_coffe_coders.fastestlap.domain.f1.standing.ConstructorStandingsElement;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.ConstructorViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.ConstructorViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.DriverViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.DriverViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.standing.viewmodel.ConstructorStandingsViewModel;
import com.the_coffe_coders.fastestlap.ui.standing.viewmodel.ConstructorStandingsViewModelFactory;
import com.the_coffe_coders.fastestlap.util.ui.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import java.util.List;

public class ConstructorsStandingActivity extends AppCompatActivity {
    private static final String TAG = "TeamCardActivity";
    LoadingScreen loadingScreen;

    private DriverViewModel driverViewModel;
    private ConstructorViewModel constructorViewModel;
    private ConstructorStandingsViewModel constructorStandingsViewModel;

    private ConstructorStandingsRecyclerAdapter constructorsStandingAdapter;
    private SwipeRefreshLayout teamStandingLayout;
    private RecyclerView constructorsStandingRecyclerView;
    private TextView standingsNotAvailableTextView;

    private String constructorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_constructors_standing);

        constructorId = getIntent().getStringExtra("TEAM_ID");
        Log.i(TAG, "Constructor ID: " + constructorId);

        teamStandingLayout = findViewById(R.id.team_standing_layout);
        UIUtils.applyWindowInsets(teamStandingLayout);

        loadingScreen = new LoadingScreen(getWindow().getDecorView(), this, teamStandingLayout, null);

        start();
    }

    private void start() {
        loadingScreen.showLoadingScreen(false);

        initializeViewModels();

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        UIUtils.applyWindowInsets(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        teamStandingLayout.setOnRefreshListener(() -> {
            start();
            teamStandingLayout.setRefreshing(false);
        });

        setupPage();
    }

    private void initializeViewModels() {
        constructorViewModel = new ViewModelProvider(this, new ConstructorViewModelFactory(getApplication())).get(ConstructorViewModel.class);
        driverViewModel = new ViewModelProvider(this, new DriverViewModelFactory(getApplication())).get(DriverViewModel.class);
        constructorStandingsViewModel = new ViewModelProvider(this, new ConstructorStandingsViewModelFactory(getApplication())).get(ConstructorStandingsViewModel.class);

    }

    private void setupPage() {
        constructorsStandingRecyclerView = findViewById(R.id.constructors_standing_recycler_view);
        standingsNotAvailableTextView = findViewById(R.id.constructors_standing_not_available);

        MutableLiveData<Result> liveData = constructorStandingsViewModel.getConstructorStandings();

        liveData.observe(this, result -> {
            if (result instanceof Result.Loading) {
                Log.i(TAG, "Constructor Standings LOADING");
                return;
            }
            if (result.isSuccess()) {
                Log.i(TAG, "CONSTRUCTORS STANDINGS SUCCESS");
                ConstructorStandings constructorStandings;
                try {
                    constructorStandings = ((Result.ConstructorStandingsSuccess) result).getData();

                    List<ConstructorStandingsElement> constructorList = constructorStandings.getConstructorStandings();

                    constructorsStandingRecyclerView.setLayoutManager(new LinearLayoutManager(this));

                    constructorsStandingAdapter = new ConstructorStandingsRecyclerAdapter(this, constructorId, constructorList, null, driverViewModel, constructorViewModel, this, loadingScreen);
                    constructorsStandingRecyclerView.setAdapter(constructorsStandingAdapter);
                } catch (ClassCastException e) {
                    setupPageForConstructorList();
                }
            } else {
                Log.i(TAG, "CONSTRUCTORS STANDINGS ERROR");
                setupPageForConstructorList();
            }
        });
    }

    private void setupPageForConstructorList() {
        MutableLiveData<Result> liveData = constructorStandingsViewModel.getConstructorListLiveData();

        liveData.observe(this, result -> {
            if (result instanceof Result.Loading) {
                return;
            }
            if (result.isSuccess()) {
                Log.i(TAG, "CONSTRUCTORS LIST SUCCESS");
                List<Constructor> constructorList = ((Result.ConstructorsSuccess) result).getData();

                Log.i(TAG, constructorList.toString());

                show(standingsNotAvailableTextView, constructorsStandingRecyclerView);

                constructorsStandingRecyclerView.setLayoutManager(new LinearLayoutManager(this));

                constructorsStandingAdapter = new ConstructorStandingsRecyclerAdapter(this, constructorId, null, constructorList, driverViewModel, constructorViewModel, this, loadingScreen);
                constructorsStandingRecyclerView.setAdapter(constructorsStandingAdapter);

            } else {
                Log.i(TAG, "CONSTRUCTORS LIST ERROR");
                show(constructorsStandingRecyclerView, standingsNotAvailableTextView);
            }
        });


    }

    private void show(View goneView, View visibleView) {
        goneView.setVisibility(View.GONE);
        visibleView.setVisibility(View.VISIBLE);
    }
}