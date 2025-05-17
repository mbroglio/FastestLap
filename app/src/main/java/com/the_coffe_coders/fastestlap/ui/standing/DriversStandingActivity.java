package com.the_coffe_coders.fastestlap.ui.standing;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.adapter.DriversStandingRecyclerAdapter;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandings;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandingsElement;
import com.the_coffe_coders.fastestlap.repository.driver.JolpicaDriverRepository;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.ConstructorViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.ConstructorViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.DriverViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.DriverViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.standing.viewmodel.DriverStandingsViewModel;
import com.the_coffe_coders.fastestlap.ui.standing.viewmodel.DriverStandingsViewModelFactory;
import com.the_coffe_coders.fastestlap.util.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.UIUtils;

import java.util.List;

public class DriversStandingActivity extends AppCompatActivity {

    private static final String TAG = "DriverCardActivity";
    private DriverStandings driverStandings;
    private LoadingScreen loadingScreen;
    private DriverViewModel driverViewModel;
    private DriverStandingsViewModel driverStandingsViewModel;
    private ConstructorViewModel constructorViewModel;
    private SwipeRefreshLayout driverStandingLayout;
    private String driverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_drivers_standing);

        driverId = getIntent().getStringExtra("DRIVER_ID");
        driverStandingLayout = findViewById(R.id.driver_standing_layout);

        loadingScreen = new LoadingScreen(getWindow().getDecorView(), this, driverStandingLayout, null);

        start();

    }

    private void start() {
        Log.i(TAG, "STARTING DRIVER STANDINGS ACTIVITY");

        loadingScreen.showLoadingScreen(false);

        initializeViewModels();

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);

        UIUtils.applyWindowInsets(toolbar);

        toolbar.setNavigationOnClickListener(v -> {
            UIUtils.navigateToHomePage(this);
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                UIUtils.navigateToHomePage(DriversStandingActivity.this);
            }
        });

        UIUtils.applyWindowInsets(driverStandingLayout);

        driverStandingLayout.setOnRefreshListener(() -> {
            start();
            driverStandingLayout.setRefreshing(false);
        });

        setupPage();
    }

    private void initializeViewModels() {
        driverStandingsViewModel = new ViewModelProvider(this, new DriverStandingsViewModelFactory(getApplication())).get(DriverStandingsViewModel.class);
        driverViewModel = new ViewModelProvider(this, new DriverViewModelFactory(getApplication())).get(DriverViewModel.class);
        constructorViewModel = new ViewModelProvider(this, new ConstructorViewModelFactory()).get(ConstructorViewModel.class);
    }

    private void setupPage() {
        MutableLiveData<Result> livedata = driverStandingsViewModel.getDriverStandingsLiveData();//TODO get last update from shared preferences

        livedata.observe(this, result -> {
            if (result instanceof Result.Loading) {
                return;
            }
            if (result.isSuccess()) {
                Log.i(TAG, "DRIVER STANDINGS SUCCESS");
                driverStandings = ((Result.DriverStandingsSuccess) result).getData();

                if (driverStandings == null) {
                    Log.i(TAG, "DRIVER STANDINGS NULL");
                    UIUtils.navigateToHomePage(this);
                } else {
                    List<DriverStandingsElement> driverList = driverStandings.getDriverStandingsElements();

                    RecyclerView driversStandingRecyclerView = findViewById(R.id.drivers_standing_recycler_view);
                    driversStandingRecyclerView.setLayoutManager(new LinearLayoutManager(this));

                    if (driverList.isEmpty()) {
                        Log.i(TAG, "DRIVER STANDINGS EMPTY");
                        MutableLiveData<Result> drivers = fetchDriversList();
                        drivers.observe(this, driverResult -> {
                            if (driverResult instanceof Result.Loading) {
                                return;
                            }
                            if (driverResult.isSuccess()) {
                                List<Driver> driverList2 = ((Result.DriversSuccess) driverResult).getData();

                                DriversStandingRecyclerAdapter driversStandingAdapter = new DriversStandingRecyclerAdapter(this, null, driverList2, driverId, driverViewModel, constructorViewModel, this, loadingScreen);
                                driversStandingRecyclerView.setAdapter(driversStandingAdapter);

                                for (int i = 0; i < driversStandingAdapter.getItemCount(); i++) {
                                    driversStandingAdapter.onBindViewHolder(
                                            driversStandingAdapter.createViewHolder(driversStandingRecyclerView, driversStandingAdapter.getItemViewType(i)), i);
                                }
                            }
                        });

                    } else {
                        Log.i(TAG, "DRIVER STANDINGS NOT EMPTY");

                        DriversStandingRecyclerAdapter driversStandingAdapter = new DriversStandingRecyclerAdapter(this, driverList, null, driverId, driverViewModel, constructorViewModel, this, loadingScreen);
                        driversStandingRecyclerView.setAdapter(driversStandingAdapter);

                        for (int i = 0; i < driversStandingAdapter.getItemCount(); i++) {
                            driversStandingAdapter.onBindViewHolder(
                                    driversStandingAdapter.createViewHolder(driversStandingRecyclerView, driversStandingAdapter.getItemViewType(i)), i);
                        }
                    }
                }
            } else {
                Log.i(TAG, "DRIVER STANDINGS ERROR");
                UIUtils.navigateToHomePage(this);
            }
        });
    }

    private MutableLiveData<Result> fetchDriversList() {
        MutableLiveData<Result> drivers = new MutableLiveData<>();
        JolpicaDriverRepository driverRepository = new JolpicaDriverRepository();
        driverRepository.getDrivers().thenAccept(drivers::postValue);
        return drivers;
    }

}