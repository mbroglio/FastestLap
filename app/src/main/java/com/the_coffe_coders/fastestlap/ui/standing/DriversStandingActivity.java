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
import com.the_coffe_coders.fastestlap.adapter.f1.DriversStandingRecyclerAdapter;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.f1.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.f1.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.f1.standing.DriverStandings;
import com.the_coffe_coders.fastestlap.domain.f1.standing.DriverStandingsElement;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.ConstructorViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.ConstructorViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.DriverViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.DriverViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.standing.viewmodel.DriverStandingsViewModel;
import com.the_coffe_coders.fastestlap.ui.standing.viewmodel.DriverStandingsViewModelFactory;
import com.the_coffe_coders.fastestlap.util.ui.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import java.util.List;

public class DriversStandingActivity extends AppCompatActivity {

    private static final String TAG = "DriverCardActivity";
    private LoadingScreen loadingScreen;
    private DriverViewModel driverViewModel;
    private DriverStandingsViewModel driverStandingsViewModel;
    private ConstructorViewModel constructorViewModel;
    private SwipeRefreshLayout driverStandingLayout;
    private RecyclerView driversStandingRecyclerView;
    private TextView standingsNotAvailableTextView;
    private DriversStandingRecyclerAdapter driversStandingAdapter;

    private String driverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_drivers_standing);

        driverId = getIntent().getStringExtra("DRIVER_ID");
        driverStandingLayout = findViewById(R.id.driver_standing_layout);
        UIUtils.applyWindowInsets(driverStandingLayout);

        loadingScreen = new LoadingScreen(getWindow().getDecorView(), this, driverStandingLayout, null);

        start();

    }

    private void start() {
        loadingScreen.showLoadingScreen(true);

        initializeViewModels();

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        UIUtils.applyWindowInsets(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        driverStandingLayout.setOnRefreshListener(() -> {
            start();
            driverStandingLayout.setRefreshing(false);
        });

        setupPage();
    }

    private void initializeViewModels() {
        driverStandingsViewModel = new ViewModelProvider(this, new DriverStandingsViewModelFactory(getApplication())).get(DriverStandingsViewModel.class);
        driverViewModel = new ViewModelProvider(this, new DriverViewModelFactory(getApplication())).get(DriverViewModel.class);
        constructorViewModel = new ViewModelProvider(this, new ConstructorViewModelFactory(getApplication())).get(ConstructorViewModel.class);
    }

    private void setupPage() {
        driversStandingRecyclerView = findViewById(R.id.drivers_standing_recycler_view);
        standingsNotAvailableTextView = findViewById(R.id.drivers_standing_not_available);

        MutableLiveData<Result> livedata = driverStandingsViewModel.getDriverStandingsLiveData();

        livedata.observe(this, result -> {
            if (result instanceof Result.Loading) {
                return;
            }
            if (result.isSuccess()) {
                Log.i(TAG, "DRIVER STANDINGS SUCCESS");
                DriverStandings driverStandings;
                try {
                    driverStandings = ((Result.DriverStandingsSuccess) result).getData();

                    Log.i(TAG, driverStandings.toString());

                    show(standingsNotAvailableTextView, driversStandingRecyclerView);
                    List<DriverStandingsElement> driverStandingList = driverStandings.getDriverStandingsElements();

                    driversStandingRecyclerView.setLayoutManager(new LinearLayoutManager(this));

                    driversStandingAdapter = new DriversStandingRecyclerAdapter(this, driverStandingList, null, driverId, driverViewModel, constructorViewModel, this, loadingScreen);
                    driversStandingRecyclerView.setAdapter(driversStandingAdapter);

                    for (DriverStandingsElement element : driverStandingList) {
                        String id = element.getDriver().getDriverId();
                        driverViewModel.getDriver(id).observe(this, dResult -> {
                            if (dResult instanceof Result.Loading) return;
                            if (dResult.isSuccess()) {
                                Driver d = ((Result.DriverSuccess) dResult).getData();
                                UIUtils.preloadImage(this, d.getDriver_half_pic_url());
                                if (d.getTeam_id() != null) {
                                    constructorViewModel.getSelectedConstructor(d.getTeam_id()).observe(this, cResult -> {
                                        if (cResult instanceof Result.Loading) return;
                                        if (cResult.isSuccess()) {
                                            Constructor c = ((Result.ConstructorSuccess) cResult).getData();
                                            UIUtils.preloadImage(this, c.getTeam_logo_minimal_url());
                                        }
                                    });
                                }
                            }
                        });
                    }
                } catch (ClassCastException e) {
                    setupPageForDriverList();
                }
            } else {
                Log.i(TAG, "DRIVER STANDINGS ERROR");
                setupPageForDriverList();
            }
        });
    }

    private void setupPageForDriverList() {
        MutableLiveData<Result> livedata = driverStandingsViewModel.getDriverListLiveData();

        livedata.observe(this, result -> {
            if (result instanceof Result.Loading) {
                return;
            }
            if (result.isSuccess()) {
                Log.i(TAG, "DRIVER LIST SUCCESS");
                List<Driver> driverList = ((Result.DriversSuccess) result).getData();

                Log.i(TAG, driverList.toString());

                show(standingsNotAvailableTextView, driversStandingRecyclerView);

                driversStandingRecyclerView.setLayoutManager(new LinearLayoutManager(this));

                driversStandingAdapter = new DriversStandingRecyclerAdapter(this, null, driverList, driverId, driverViewModel, constructorViewModel, this, loadingScreen);
                driversStandingRecyclerView.setAdapter(driversStandingAdapter);

                for (Driver d : driverList) {
                    UIUtils.preloadImage(this, d.getDriver_half_pic_url());
                    if (d.getTeam_id() != null) {
                        constructorViewModel.getSelectedConstructor(d.getTeam_id()).observe(this, cResult -> {
                            if (cResult instanceof Result.Loading) return;
                            if (cResult.isSuccess()) {
                                Constructor c = ((Result.ConstructorSuccess) cResult).getData();
                                UIUtils.preloadImage(this, c.getTeam_logo_minimal_url());
                            }
                        });
                    }
                }
            } else {
                Log.i(TAG, "DRIVER LIST ERROR");
                show(driversStandingRecyclerView, standingsNotAvailableTextView);
            }
        });
    }

    private void show(View goneView, View visibleView) {
        goneView.setVisibility(View.GONE);
        visibleView.setVisibility(View.VISIBLE);
    }
}