package com.the_coffe_coders.fastestlap.ui.bio;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.driver.DriverHistory;
import com.the_coffe_coders.fastestlap.domain.nation.Nation;
import com.the_coffe_coders.fastestlap.domain.user.User;
import com.the_coffe_coders.fastestlap.repository.user.IUserRepository;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.ConstructorViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.ConstructorViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.DriverViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.DriverViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.NationViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.NationViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.welcome.viewmodel.UserViewModel;
import com.the_coffe_coders.fastestlap.ui.welcome.viewmodel.UserViewModelFactory;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;
import com.the_coffe_coders.fastestlap.util.SharedPreferencesUtils;
import com.the_coffe_coders.fastestlap.util.UIUtils;

public class DriverBioActivity extends AppCompatActivity {

    private final String TAG = "DriverBioActivity";
    LoadingScreen loadingScreen;
    private Driver driver;
    private Nation nation;
    private Constructor team;
    private MaterialCardView teamLogoCard;
    private ImageView teamLogoImage;
    private MaterialCardView driverRank;
    private MaterialCardView driverNumberCard;
    private MaterialToolbar toolbar;
    private AppBarLayout appBarLayout;
    private ImageView driverNumberImage;
    private ScrollView scrollView;

    private DriverViewModel driverViewModel;
    private NationViewModel nationViewModel;
    private ConstructorViewModel constructorViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_driver_bio);

        loadingScreen = new LoadingScreen(getWindow().getDecorView(), this);
        loadingScreen.showLoadingScreen();

        String driverId = getIntent().getStringExtra("DRIVER_ID");
        Log.i("DriverBioActivity", "Driver ID: " + driverId);

        toolbar = findViewById(R.id.topAppBar);
        appBarLayout = findViewById(R.id.top_bar_layout);

        UIUtils.applyWindowInsets(toolbar);

        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        scrollView = findViewById(R.id.driver_bio_scroll);
        UIUtils.applyWindowInsets(scrollView);

        teamLogoCard = findViewById(R.id.team_logo_card);
        teamLogoImage = findViewById(R.id.team_logo_image);

        driverNumberCard = findViewById(R.id.driver_number_card);
        driverNumberImage = findViewById(R.id.driver_number_image);

        //driverViewModel = new ViewModelProvider(this, new DriverViewModelFactory()).get(DriverViewModel.class);
        // get driver viewmodel without initializing it again

        driverViewModel = new ViewModelProvider(this, new DriverViewModelFactory(getApplication())).get(DriverViewModel.class);
        constructorViewModel = new ViewModelProvider(this, new ConstructorViewModelFactory()).get(ConstructorViewModel.class);
        nationViewModel = new ViewModelProvider(this, new NationViewModelFactory()).get(NationViewModel.class);

        // Set up the favorite icon click listener
        Menu menu = toolbar.getMenu();
        MenuItem favoriteItem = menu.findItem(R.id.favourite_icon_outline);
        favoriteItem.setOnMenuItemClickListener(v -> {
            toggleFavoriteDriver(driverId, favoriteItem);
            return true;
        });

        createDriverBioPage(driverId);
    }

    private void toggleFavoriteDriver(String driverId, MenuItem menuItem) {
        SharedPreferencesUtils sharedPreferencesUtils = new SharedPreferencesUtils(this);
        String currentFavoriteDriverId = sharedPreferencesUtils.readStringData(Constants.SHARED_PREFERENCES_FILENAME, Constants.SHARED_PREFERENCES_FAVORITE_DRIVER);

        IUserRepository userRepository = ServiceLocator.getInstance().getUserRepository(getApplication());
        UserViewModel userViewModel = new ViewModelProvider(getViewModelStore(), new UserViewModelFactory(userRepository)).get(UserViewModel.class);
        User currentUser = userViewModel.getLoggedUser();

        if (currentFavoriteDriverId.equals(driverId)) {
            // Remove as favorite
            sharedPreferencesUtils.writeStringData(Constants.SHARED_PREFERENCES_FILENAME, Constants.SHARED_PREFERENCES_FAVORITE_DRIVER, "null");
            menuItem.setIcon(R.drawable.baseline_star_border_24);

            // Update user preferences in backend
            userViewModel.saveUserDriverPreferences("", currentUser.getIdToken());

            Log.i(TAG, "Removed favorite driver: " + driverId);
        } else {
            // Set as favorite
            sharedPreferencesUtils.writeStringData(Constants.SHARED_PREFERENCES_FILENAME, Constants.SHARED_PREFERENCES_FAVORITE_DRIVER, driverId);
            menuItem.setIcon(R.drawable.star_fav);

            // Update user preferences in backend
            userViewModel.saveUserDriverPreferences(driverId, currentUser.getIdToken());

            Log.i(TAG, "Set favorite driver to: " + driverId);
        }
    }

    private void updateFavoriteIcon(String driverId) {
        SharedPreferencesUtils sharedPreferencesUtils = new SharedPreferencesUtils(this);
        String favoriteDriverId = sharedPreferencesUtils.readStringData(Constants.SHARED_PREFERENCES_FILENAME, Constants.SHARED_PREFERENCES_FAVORITE_DRIVER);

        Menu menu = toolbar.getMenu();
        MenuItem favoriteItem = menu.findItem(R.id.favourite_icon_outline);

        if (favoriteDriverId.equals(driverId)) {
            favoriteItem.setIcon(R.drawable.star_fav);
        } else {
            favoriteItem.setIcon(R.drawable.baseline_star_border_24);
        }
    }

    public void createDriverBioPage(String driverId) {
        MutableLiveData<Result> driverMutableLiveData = driverViewModel.getDriver(driverId);

        driverMutableLiveData.observe(this, result -> {
            if(result instanceof Result.Loading) {
                return;
            }
            if (result.isSuccess()) {
                driver = ((Result.DriverSuccess) result).getData();
                Log.i(TAG, "DRIVER SUCCESS");
                // Update the favorite icon when driver data is loaded
                updateFavoriteIcon(driverId);
                getTeamInfo(driver.getTeam_id());
            } else {
                Log.i(TAG, "DRIVER ERROR");
            }
        });
    }

    public void getTeamInfo(String teamId) {
        MutableLiveData<Result> constructorMutableLiveData = constructorViewModel.getSelectedConstructor(teamId);

        constructorMutableLiveData.observe(this, result -> {
            if(result instanceof Result.Loading) {
                return;
            }
            if (result.isSuccess()) {
                team = ((Result.ConstructorSuccess) result).getData();
                Log.i(TAG, "GET CONSTRUCTOR FROM COMMON REPO: " + team.toString());
                getNationInfo(driver.getNationality());
            } else {
                Log.i(TAG, "GET CONSTRUCTOR FROM COMMON REPO ERROR");
            }
        });
    }

    public void getNationInfo(String nationId) {
        MutableLiveData<Result> nationMutableLiveData = nationViewModel.getNation(nationId);

        nationMutableLiveData.observe(this, result -> {
            if(result instanceof Result.Loading) {
                return;
            }
            if (result.isSuccess()) {
                nation = ((Result.NationSuccess) result).getData();
                Log.i(TAG, "GET NATION FROM FIREBASE REPO: " + nation);
                setDriverData(driver, nation, team);
                setToolbar();
            }
        });
    }

    public void setToolbar() {
        String fullName = driver.getGivenName() + " " + driver.getFamilyName();

        TextView toolbarTitle = findViewById(R.id.topAppBarTitle);
        toolbarTitle.setText(fullName.toUpperCase());

        try {
            toolbar.setBackgroundColor(ContextCompat.getColor(this, Constants.TEAM_COLOR.get(driver.getTeam_id())));
            appBarLayout.setBackgroundColor(ContextCompat.getColor(this, Constants.TEAM_COLOR.get(driver.getTeam_id())));

            teamLogoCard.setOnClickListener(v -> {
                Intent intent = new Intent(DriverBioActivity.this, ConstructorBioActivity.class);
                intent.putExtra("TEAM_ID", driver.getTeam_id());
                startActivity(intent);
            });
        } catch (Exception e) {
            toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.timer_gray));
            appBarLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.timer_gray));
        }
    }

    private void setDriverData(Driver driver, Nation nation, Constructor team) {
        try {
            teamLogoCard.setStrokeColor(ContextCompat.getColor(this, Constants.TEAM_COLOR.get(driver.getTeam_id())));
            if(team.getConstructorId().equals("rb")){
                teamLogoCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));
            }
            driverNumberCard.setStrokeColor(ContextCompat.getColor(this, Constants.TEAM_COLOR.get(driver.getTeam_id())));

            Glide.with(this).load(team.getTeam_logo_url()).into(teamLogoImage);
        } catch (Exception e) {
            teamLogoCard.setStrokeColor(ContextCompat.getColor(this, R.color.timer_gray));
            driverNumberCard.setStrokeColor(ContextCompat.getColor(this, R.color.timer_gray));

            teamLogoImage.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.f1_car_icon_filled));
        }

        UIUtils.loadSequenceOfImagesWithGlide(this,
                new String[]{nation.getNation_flag_url(), driver.getDriver_pic_url(), driver.getRacing_number_pic_url()},
                new ImageView[]{findViewById(R.id.driver_flag), findViewById(R.id.driver_bio_pic), driverNumberImage},
                () -> {
                    setDriverDataFinalStep(driver);
                    Log.i(TAG, "All images loaded successfully");
                });
    }

    private void setDriverDataFinalStep(Driver driver) {
        TextView birthplace = findViewById(R.id.driver_birthplace);
        birthplace.setText(driver.getBirth_place());

        TextView birthdate = findViewById(R.id.driver_birthdate);
        birthdate.setText(driver.getDateOfBirth());

        TextView age = findViewById(R.id.driver_age);
        age.setText(driver.getDriverAgeAsString());

        TextView weight = findViewById(R.id.driver_weight);
        weight.setText(driver.getWeight());

        TextView height = findViewById(R.id.driver_height);
        height.setText(driver.getHeight());

        TextView bestResult = findViewById(R.id.driver_best_result);
        bestResult.setText(driver.getBest_result());

        TextView championships = findViewById(R.id.driver_championships);
        championships.setText(driver.getChampionships());

        TextView firstEntry = findViewById(R.id.driver_first_entry);
        firstEntry.setText(driver.getFirst_entry());

        createHistoryTable();
    }

    private void createHistoryTable() {
        TableLayout tableLayout = findViewById(R.id.history_table);
        LayoutInflater inflater = LayoutInflater.from(this);

        View tableHeader = inflater.inflate(R.layout.driver_bio_table_header, tableLayout, false);
        TableLayout.LayoutParams paramsHeader = (TableLayout.LayoutParams) tableHeader.getLayoutParams();
        paramsHeader.setMargins(0, 0, 0, (int) getResources().getDisplayMetrics().density * 5);
        tableHeader.setLayoutParams(paramsHeader);
        tableHeader.setBackgroundColor(ContextCompat.getColor(this, R.color.timer_gray_dark));

        tableLayout.addView(tableHeader);

        if (driver.getDriver_history() != null) {
            for (DriverHistory history : driver.getDriver_history()) {
                View tableRow = inflater.inflate(R.layout.driver_bio_table_row, tableLayout, false);
                tableRow.setBackgroundColor(ContextCompat.getColor(this, R.color.timer_gray));

                TextView seasonYear = tableRow.findViewById(R.id.season_year);
                seasonYear.setText(history.getYear());

                TextView teamNameText = tableRow.findViewById(R.id.team_name);
                teamNameText.setText(history.getTeam());

                TextView driverPosition = tableRow.findViewById(R.id.driver_position);
                driverPosition.setText(history.getPosition());

                TextView driverPoints = tableRow.findViewById(R.id.driver_points);
                driverPoints.setText(history.getPoints());

                TextView driverWins = tableRow.findViewById(R.id.driver_wins);
                driverWins.setText(history.getWins());

                TextView driverPodiums = tableRow.findViewById(R.id.driver_podiums);
                driverPodiums.setText(history.getPodiums());

                TableLayout.LayoutParams tableParams = (TableLayout.LayoutParams) tableRow.getLayoutParams();
                tableParams.setMargins(0, 0, 0, (int) getResources().getDisplayMetrics().density * 5);
                tableRow.setLayoutParams(tableParams);

                tableLayout.addView(tableRow);
            }
        }
        loadingScreen.hideLoadingScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}