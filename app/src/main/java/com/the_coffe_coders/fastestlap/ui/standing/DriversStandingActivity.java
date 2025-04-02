package com.the_coffe_coders.fastestlap.ui.standing;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandings;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandingsElement;
import com.the_coffe_coders.fastestlap.repository.driver.JolpicaDriverRepository;
import com.the_coffe_coders.fastestlap.ui.bio.DriverBioActivity;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.ConstructorViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.ConstructorViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.DriverViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.DriverViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.home.HomePageActivity;
import com.the_coffe_coders.fastestlap.ui.home.fragment.RacingFragment;
import com.the_coffe_coders.fastestlap.ui.home.fragment.StandingsFragment;
import com.the_coffe_coders.fastestlap.ui.standing.viewmodel.DriverStandingsViewModel;
import com.the_coffe_coders.fastestlap.ui.standing.viewmodel.DriverStandingsViewModelFactory;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;
import com.the_coffe_coders.fastestlap.util.UIUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class DriversStandingActivity extends AppCompatActivity {

    private static final String TAG = "DriverCardActivity";

    private DriverStandings driverStandings;

    private LoadingScreen loadingScreen;

    private DriverStandingsViewModel driverStandingsViewModel;

    private String driverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_drivers_standing);

        driverId = getIntent().getStringExtra("DRIVER_ID");

        start();

    }

    private void start() {
        loadingScreen = new LoadingScreen(getWindow().getDecorView(), this);

        loadingScreen.showLoadingScreen();


        MaterialToolbar toolbar = findViewById(R.id.topAppBar);

        UIUtils.applyWindowInsets(toolbar);

        toolbar.setNavigationOnClickListener(v -> {
            Intent intent = new Intent(DriversStandingActivity.this, HomePageActivity.class);
            intent.putExtra("CALLER", "DriversStandingActivity");
            startActivity(intent);
        });

        SwipeRefreshLayout driverStandingLayout = findViewById(R.id.driver_standing_layout);
        UIUtils.applyWindowInsets(driverStandingLayout);

        driverStandingLayout.setOnRefreshListener(() -> {
            start();
            driverStandingLayout.setRefreshing(false);
        });

        setupPage();
    }

    private void setupPage() {
        LinearLayout driverStanding = findViewById(R.id.driver_standing);

        driverStandingsViewModel = new ViewModelProvider(this, new DriverStandingsViewModelFactory()).get(DriverStandingsViewModel.class);
        MutableLiveData<Result> livedata = driverStandingsViewModel.getDriverStandingsLiveData();//TODO get last update from shared preferences

        livedata.observe(this, result -> {
            if(result instanceof Result.Loading) {
                // Gestisci lo stato di caricamento, ad esempio mostrando un indicatore di caricamento
                Log.i(TAG, "DRIVER STANDINGS LOADING");
                // Qui potresti voler mostrare una UI di caricamento
                return;
            }
            if (result.isSuccess()) {
                Log.i(TAG, "DRIVER STANDINGS SUCCESS");
                driverStanding.removeAllViews();
                driverStandings = ((Result.DriverStandingsSuccess) result).getData();
                List<DriverStandingsElement> driverList = driverStandings.getDriverStandingsElements();

                if (driverList.isEmpty()) {
                    Log.i(TAG, "DRIVER STANDINGS EMPTY");
                    MutableLiveData<Result> drivers = fetchDriversList();
                    drivers.observe(this, driverResult -> {
                        if(driverResult instanceof Result.Loading) {
                            return;
                        }
                        if (driverResult.isSuccess()) {
                            List<Driver> driverList2 = ((Result.DriversSuccess) driverResult).getData();
                            for (Driver driver : driverList2) {
                                View driverCard = generateDriverCard(driver, driverId);
                                driverStanding.addView(driverCard);
                                View space = new View(DriversStandingActivity.this);
                                space.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20));
                                driverStanding.addView(space);
                            }
                        }
                    });
                } else {
                    Log.i(TAG, "DRIVER STANDINGS NOT EMPTY");
                    for (DriverStandingsElement driver : driverList) {
                        View driverCard = generateDriverCard(driver, driverId);
                        driverStanding.addView(driverCard);
                        View space = new View(DriversStandingActivity.this);
                        space.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20));
                        driverStanding.addView(space);
                    }
                }
            } else {
                Log.i(TAG, "DRIVER STANDINGS ERROR");
                loadingScreen.hideLoadingScreen();
            }
        });
    }


    private MutableLiveData<Result> fetchDriversList() {
        MutableLiveData<Result> drivers = new MutableLiveData<>();
        JolpicaDriverRepository driverRepository = new JolpicaDriverRepository();
        driverRepository.getDrivers().thenAccept(drivers::postValue);
        return drivers;
    }

    private View generateDriverCard(Driver driver, String driverId) {
        DriverStandingsElement driverStandingsElement = new DriverStandingsElement();
        driverStandingsElement.setDriver(driver);
        driverStandingsElement.setPoints("0");

        return generateDriverCard(driverStandingsElement, driverId);
    }

    @SuppressLint("SetTextI18n")
    private View generateDriverCard(DriverStandingsElement standingElement, String driverIdToHighlight) {
        DriverViewModel driverViewModel = new ViewModelProvider(this, new DriverViewModelFactory()).get(DriverViewModel.class);
        MutableLiveData<Result> driverLiveData = driverViewModel.getDriver(standingElement.getDriver().getDriverId());

        View driverCard = getLayoutInflater().inflate(R.layout.driver_card, null);
        driverLiveData.observe(this, result -> {
            if(result instanceof Result.Loading) {
                return;
            }
            if (result.isSuccess()) {
                Driver driver = ((Result.DriverSuccess) result).getData();

                ImageView driverImageView = driverCard.findViewById(R.id.driver_image);
                Glide.with(this).load(driver.getDriver_pic_url()).into(driverImageView);

                TextView driverName = driverCard.findViewById(R.id.driver_name);
                driverName.setText(driver.getFullName());

                TextView driverPosition = driverCard.findViewById(R.id.driver_position);
                if(standingElement.getPosition() == null || standingElement.getPosition().equals("-")){
                    driverPosition.setText(R.string.last_driver_position);
                }else{
                    driverPosition.setText(standingElement.getPosition());
                }

                TextView driverPoints = driverCard.findViewById(R.id.driver_points);
                driverPoints.setText(standingElement.getPoints());

                if (standingElement.getDriver().getDriverId().equals(driverIdToHighlight)) {
                    MaterialCardView driverCardView = driverCard.findViewById(R.id.driver_card_view);
                    UIUtils.animateCardBackgroundColor(this, driverCardView, R.color.yellow, Color.TRANSPARENT, 1000, 10);
                }

                ConstructorViewModel constructorViewModel = new ViewModelProvider(this, new ConstructorViewModelFactory()).get(ConstructorViewModel.class);
                MutableLiveData<Result> constructorLiveData = constructorViewModel.getSelectedConstructor(driver.getTeam_id());
                constructorLiveData.observe(this, constructorResult -> {
                    if(constructorResult instanceof Result.Loading) {
                        return;
                    }
                    if (constructorResult.isSuccess()) {
                        Constructor constructor = ((Result.ConstructorSuccess) constructorResult).getData();

                        ImageView teamLogoImageView = driverCard.findViewById(R.id.team_logo);
                        if(constructor.getTeam_logo_minimal_url() != null){
                            Glide.with(this).load(constructor.getTeam_logo_minimal_url()).into(teamLogoImageView);
                        }else{
                            Glide.with(this).load(constructor.getTeam_logo_url()).into(teamLogoImageView);
                        }

                        RelativeLayout driverColor = driverCard.findViewById(R.id.small_driver_card);
                        try {
                            driverColor.setBackground(AppCompatResources.getDrawable(this, Constants.TEAM_GRADIENT_COLOR.get(driver.getTeam_id())));
                        } catch (Exception e) {
                            Log.i(TAG, "Driver has no team");
                            driverColor.setBackground(AppCompatResources.getDrawable(this, R.color.timer_gray));
                            Glide.with(this).load(R.drawable.f1_car_icon_filled).into(teamLogoImageView);
                        }

                        driverCard.setOnClickListener(v -> {
                            Intent intent = new Intent(DriversStandingActivity.this, DriverBioActivity.class);
                            intent.putExtra("DRIVER_ID", standingElement.getDriver().getDriverId());
                            intent.putExtra("CALLER", DriversStandingActivity.class.getName());
                            startActivity(intent);
                        });

                        loadingScreen.hideLoadingScreen();
                    }
                });
            }
        });

        return driverCard;
    }
}