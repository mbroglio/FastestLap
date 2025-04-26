package com.the_coffe_coders.fastestlap.ui.standing;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
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
import com.the_coffe_coders.fastestlap.ui.standing.viewmodel.DriverStandingsViewModel;
import com.the_coffe_coders.fastestlap.ui.standing.viewmodel.DriverStandingsViewModelFactory;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.UIUtils;

import java.util.List;

public class DriversStandingActivity extends AppCompatActivity {

    private static final String TAG = "DriverCardActivity";
    private DriverStandings driverStandings;
    private LoadingScreen loadingScreen;

    private SwipeRefreshLayout driverStandingLayout;
    private String driverId;
    private int counter = 0;

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

        loadingScreen.showLoadingScreen();

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);

        UIUtils.applyWindowInsets(toolbar);

        toolbar.setNavigationOnClickListener(v -> {
            Intent intent = new Intent(DriversStandingActivity.this, HomePageActivity.class);
            intent.putExtra("CALLER", "DriversStandingActivity");
            startActivity(intent);
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(DriversStandingActivity.this, HomePageActivity.class);
                intent.putExtra("CALLER", "DriversStandingActivity");
                startActivity(intent);
            }
        });

        UIUtils.applyWindowInsets(driverStandingLayout);

        driverStandingLayout.setOnRefreshListener(() -> {
            counter = 0;
            start();
            driverStandingLayout.setRefreshing(false);
        });

        setupPage();
    }

    private void setupPage() {
        LinearLayout driverStanding = findViewById(R.id.driver_standing);

        loadingScreen.postLoadingStatus(this.getString(R.string.initializing));

        DriverStandingsViewModel driverStandingsViewModel = new ViewModelProvider(this, new DriverStandingsViewModelFactory(getApplication())).get(DriverStandingsViewModel.class);
        MutableLiveData<Result> livedata = driverStandingsViewModel.getDriverStandingsLiveData();//TODO get last update from shared preferences

        livedata.observe(this, result -> {
            if (result instanceof Result.Loading) {
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
                        if (driverResult instanceof Result.Loading) {
                            return;
                        }
                        if (driverResult.isSuccess()) {
                            List<Driver> driverList2 = ((Result.DriversSuccess) driverResult).getData();
                            for (int i = 0; i < driverList2.size(); i++) {
                                View driverCard = generateDriverCard(driverList2.get(i), driverId, i, driverList2.size());
                                driverStanding.addView(driverCard);

                                View space = new View(DriversStandingActivity.this);
                                space.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20));
                                driverStanding.addView(space);
                            }
                            View space = new View(DriversStandingActivity.this);
                            space.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20));
                            driverStanding.addView(space);
                        }
                    });
                } else {
                    Log.i(TAG, "DRIVER STANDINGS NOT EMPTY");
                    for (int k = 0; k < driverList.size(); k++) {
                        View driverCard = generateDriverCard(driverList.get(k), driverId, k, driverList.size());
                        driverStanding.addView(driverCard);

                        View space = new View(DriversStandingActivity.this);
                        space.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20));
                        driverStanding.addView(space);
                    }
                    View space = new View(DriversStandingActivity.this);
                    space.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20));
                    driverStanding.addView(space);
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

    private View generateDriverCard(Driver driver, String driverId, int pos, int size) {

        DriverStandingsElement driverStandingsElement = new DriverStandingsElement();
        driverStandingsElement.setDriver(driver);
        driverStandingsElement.setPoints("0");

        return generateDriverCard(driverStandingsElement, driverId, pos, size);
    }

    private View generateDriverCard(DriverStandingsElement standingElement, String driverIdToHighlight, int pos, int size) {

        DriverViewModel driverViewModel = new ViewModelProvider(this, new DriverViewModelFactory(getApplication())).get(DriverViewModel.class);
        MutableLiveData<Result> driverLiveData = driverViewModel.getDriver(standingElement.getDriver().getDriverId());

        View driverCard = getLayoutInflater().inflate(R.layout.driver_card, null);
        driverLiveData.observe(this, result -> {
            if (result instanceof Result.Loading) {
                return;
            }
            if (result.isSuccess()) {
                Driver driver = ((Result.DriverSuccess) result).getData();

                ImageView driverImageView = driverCard.findViewById(R.id.driver_image);

                UIUtils.loadImageWithGlide(this, driver.getDriver_pic_url(), driverImageView, () ->
                        generateDriverCardStepTwo(driverCard, driver, standingElement, driverIdToHighlight, pos, size));
            }
        });

        return driverCard;
    }

    private void generateDriverCardStepTwo(View driverCard, Driver driver, DriverStandingsElement standingElement, String driverIdToHighlight, int pos, int size) {

        UIUtils.multipleSetTextViewText(
                new String[]{driver.getFullName(), standingElement.getPoints()},

                new TextView[]{driverCard.findViewById(R.id.driver_name),
                        driverCard.findViewById(R.id.driver_points)});

        UIUtils.setTextViewTextWithCondition(standingElement.getPosition() == null || standingElement.getPosition().equals("-"),
                ContextCompat.getString(this, R.string.last_driver_position), //if true
                standingElement.getPosition(), //if false
                driverCard.findViewById(R.id.driver_position));

        if (standingElement.getDriver().getDriverId().equals(driverIdToHighlight)) {
            UIUtils.animateCardBackgroundColor(this, driverCard.findViewById(R.id.driver_card_view), R.color.yellow, Color.TRANSPARENT, 1000, 10);
        }

        ConstructorViewModel constructorViewModel = new ViewModelProvider(this, new ConstructorViewModelFactory()).get(ConstructorViewModel.class);
        RelativeLayout driverColor = driverCard.findViewById(R.id.small_driver_card);
        ImageView teamLogoImageView = driverCard.findViewById(R.id.team_logo);
        if (driver.getTeam_id() != null) {
            driverColor.setBackground(AppCompatResources.getDrawable(this, Constants.TEAM_GRADIENT_COLOR.get(driver.getTeam_id())));
        } else {
            driverColor.setBackground(AppCompatResources.getDrawable(this, R.color.timer_gray));
            teamLogoImageView.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.f1_car_icon_filled));
        }

        MutableLiveData<Result> constructorLiveData = constructorViewModel.getSelectedConstructor(driver.getTeam_id());
        constructorLiveData.observe(this, constructorResult -> {
            if (constructorResult instanceof Result.Loading) {
                return;
            }
            if (constructorResult.isSuccess()) {
                Constructor constructor = ((Result.ConstructorSuccess) constructorResult).getData();

                UIUtils.loadImageWithGlide(this, constructor.getTeam_logo_minimal_url(), teamLogoImageView, () ->
                        generateDriverCardFinalStep(driverCard, standingElement, pos, size));
            }
        });
    }

    private void generateDriverCardFinalStep(View driverCard, DriverStandingsElement standingElement, int pos, int size) {
        driverCard.setOnClickListener(v -> {
            Intent intent = new Intent(DriversStandingActivity.this, DriverBioActivity.class);
            intent.putExtra("DRIVER_ID", standingElement.getDriver().getDriverId());
            intent.putExtra("CALLER", DriversStandingActivity.class.getName());
            startActivity(intent);
        });
        loadingScreen.postLoadingStatus(this.getString(R.string.generating_driver_card, Integer.toString(pos + 1), Integer.toString(size)));
        loadingScreen.updateProgress((pos + 1) * 100 / size);
        counter++;

        Log.i(TAG, "Counter: " + counter);

        loadingScreen.hideLoadingScreenWithCondition(counter == size - 1);
    }

}