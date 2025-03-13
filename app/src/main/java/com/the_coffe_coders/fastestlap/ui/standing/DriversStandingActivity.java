package com.the_coffe_coders.fastestlap.ui.standing;

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

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandings;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandingsElement;
import com.the_coffe_coders.fastestlap.ui.bio.DriverBioActivity;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.ConstructorViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.ConstructorViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.DriverViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.DriverViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.standing.viewmodel.DriverStandingsViewModel;
import com.the_coffe_coders.fastestlap.ui.standing.viewmodel.DriverStandingsViewModelFactory;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;
import com.the_coffe_coders.fastestlap.util.UIUtils;

import java.util.List;

public class DriversStandingActivity extends AppCompatActivity {

    private static final String TAG = "DriverCardActivity";

    private DriverStandings driverStandings;

    private LoadingScreen loadingScreen;

    private DriverStandingsViewModel driverStandingsViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_drivers_standing);

        loadingScreen = new LoadingScreen(getWindow().getDecorView(), this);

        loadingScreen.showLoadingScreen();
        String driverId = getIntent().getStringExtra("DRIVER_ID");

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);

        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.topMargin = systemBars.top;
            v.setLayoutParams(params);

            return insets;
        });

        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());


        LinearLayout driverStanding = findViewById(R.id.driver_standing);

        driverStandingsViewModel = new ViewModelProvider(this, new DriverStandingsViewModelFactory(ServiceLocator.getInstance().getDriverStandingsRepository(getApplication(), false))).get(DriverStandingsViewModel.class);
        MutableLiveData<Result> livedata = driverStandingsViewModel.getDriverStandingsLiveData(0);//TODO get last update from shared preferences

        livedata.observe(this, result -> {
            if (result.isSuccess()) {
                Log.i(TAG, "DRIVER STANDINGS SUCCESS");
                driverStandings = ((Result.DriverStandingsSuccess) result).getData();
                List<DriverStandingsElement> driverList = driverStandings.getDriverStandingsElements();

                if (driverList.isEmpty()) {
                    Log.i(TAG, "DRIVER STANDINGS EMPTY");
                    List<Driver> drivers = fetchDriversList();

                    for (Driver driver : drivers) {
                        View driverCard = generateDriverCard(driver, driverId);
                        driverStanding.addView(driverCard);
                        View space = new View(DriversStandingActivity.this);
                        space.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20));
                        driverStanding.addView(space);
                    }
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

        loadingScreen.hideLoadingScreen();
    }


    private List<Driver> fetchDriversList() {
        return null;
    }

    private View generateDriverCard(Driver driver, String driverId) {
        DriverStandingsElement driverStandingsElement = new DriverStandingsElement();
        driverStandingsElement.setDriver(driver);
        driverStandingsElement.setPoints("0");

        return generateDriverCard(driverStandingsElement, driverId);
    }

    private View generateDriverCard(DriverStandingsElement standingElement, String driverIdToHighlight) {
        DriverViewModel driverViewModel = new ViewModelProvider(this, new DriverViewModelFactory(ServiceLocator.getInstance().getCommonDriverRepository())).get(DriverViewModel.class);
        MutableLiveData<Result> driverLiveData = driverViewModel.getDriver(standingElement.getDriver().getDriverId());

        View driverCard = getLayoutInflater().inflate(R.layout.driver_card, null);
        driverLiveData.observe(this, result -> {
            if (result.isSuccess()) {
                Driver driver = ((Result.DriverSuccess) result).getData();

                ImageView driverImageView = driverCard.findViewById(R.id.driver_image);
                Glide.with(this).load(driver.getDriver_pic_url()).into(driverImageView);

                TextView driverName = driverCard.findViewById(R.id.driver_name);
                driverName.setText(driver.getFullName());

                TextView driverPosition = driverCard.findViewById(R.id.driver_position);
                driverPosition.setText(standingElement.getPosition());

                TextView driverPoints = driverCard.findViewById(R.id.driver_points);
                driverPoints.setText(standingElement.getPoints());

                if (standingElement.getDriver().getDriverId().equals(driverIdToHighlight)) {
                    MaterialCardView driverCardView = driverCard.findViewById(R.id.driver_card_view);
                    UIUtils.animateCardBackgroundColor(this, driverCardView, R.color.yellow, Color.TRANSPARENT, 1000, 10);
                }

                ConstructorViewModel constructorViewModel = new ViewModelProvider(this, new ConstructorViewModelFactory(ServiceLocator.getInstance().getCommonConstructorRepository())).get(ConstructorViewModel.class);
                MutableLiveData<Result> constructorLiveData = constructorViewModel.getSelectedConstructorLiveData(driver.getTeam_id());
                constructorLiveData.observe(this, constructorResult -> {
                    if (constructorResult.isSuccess()) {
                        Constructor constructor = ((Result.ConstructorSuccess) constructorResult).getData();

                        ImageView teamLogoImageView = driverCard.findViewById(R.id.team_logo);
                        Glide.with(this).load(constructor.getTeam_logo_url()).into(teamLogoImageView);

                        RelativeLayout driverColor = driverCard.findViewById(R.id.small_driver_card);
                        try {
                            driverColor.setBackground(AppCompatResources.getDrawable(this, Constants.TEAM_GRADIENT_COLOR.get(driver.getTeam_id())));
                        } catch (Exception e) {
                            Log.i(TAG, "Driver has no team");
                            driverColor.setBackground(AppCompatResources.getDrawable(this, R.color.white));
                        }

                        driverCard.setOnClickListener(v -> {
                            Intent intent = new Intent(DriversStandingActivity.this, DriverBioActivity.class);
                            intent.putExtra("DRIVER_ID", standingElement.getDriver().getDriverId());
                            intent.putExtra("CALLER", DriversStandingActivity.class.getName());
                            startActivity(intent);
                        });
                    }
                });
            }
        });

        return driverCard;
    }
}