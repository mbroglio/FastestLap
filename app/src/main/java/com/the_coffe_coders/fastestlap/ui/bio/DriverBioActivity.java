package com.the_coffe_coders.fastestlap.ui.bio;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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

import java.util.List;

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
    private String driverId;
    private SwipeRefreshLayout driverBioLayout;

    private DriverViewModel driverViewModel;
    private NationViewModel nationViewModel;
    private ConstructorViewModel constructorViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_driver_bio);

        start();
    }

    private void start() {
        driverBioLayout = findViewById(R.id.driver_bio_layout);
        loadingScreen = new LoadingScreen(getWindow().getDecorView(), this, driverBioLayout, null);
        loadingScreen.showLoadingScreen(false);
        loadingScreen.updateProgress();

        toolbar = findViewById(R.id.topAppBar);
        UIUtils.applyWindowInsets(toolbar);

        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        Menu menu = toolbar.getMenu();
        MenuItem favoriteItem = menu.findItem(R.id.favourite_icon_outline);
        favoriteItem.setOnMenuItemClickListener(v -> {
            toggleFavoriteDriver(driverId, favoriteItem);
            return true;
        });

        appBarLayout = findViewById(R.id.top_bar_layout);

        UIUtils.applyWindowInsets(driverBioLayout);
        driverBioLayout.setOnRefreshListener(() -> {
            start();
            driverBioLayout.setRefreshing(false);
        });

        driverId = getIntent().getStringExtra("DRIVER_ID");
        Log.i("DriverBioActivity", "Driver ID: " + driverId);

        teamLogoCard = findViewById(R.id.team_logo_card);
        teamLogoImage = findViewById(R.id.team_logo_image);

        driverNumberCard = findViewById(R.id.driver_number_card);
        driverNumberImage = findViewById(R.id.driver_number_image);

        initializeViewModels();
    }

    private void initializeViewModels() {
        driverViewModel = new ViewModelProvider(this, new DriverViewModelFactory(getApplication())).get(DriverViewModel.class);
        constructorViewModel = new ViewModelProvider(this, new ConstructorViewModelFactory(getApplication())).get(ConstructorViewModel.class);
        nationViewModel = new ViewModelProvider(this, new NationViewModelFactory(getApplication())).get(NationViewModel.class);

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
            userViewModel.saveUserDriverPreferences("null", currentUser.getIdToken());

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
            if (result instanceof Result.Loading) {
                return;
            }
            if (result.isSuccess()) {
                driver = ((Result.DriverSuccess) result).getData();
                Log.i(TAG, "DRIVER SUCCESS");
                Log.i(TAG, "DRIVER: " + driver.toString());

                // Update the favorite icon when driver data is loaded
                updateFavoriteIcon(driverId);
                getTeamInfo(driver.getTeam_id());
            } else {
                Log.i(TAG, "DRIVER ERROR");
            }
        });
    }

    public void getTeamInfo(String teamId) {
        loadingScreen.updateProgress();

        MutableLiveData<Result> constructorMutableLiveData = constructorViewModel.getSelectedConstructor(teamId);

        constructorMutableLiveData.observe(this, result -> {
            if (result instanceof Result.Loading) {
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
        try{
            MutableLiveData<Result> nationMutableLiveData = nationViewModel.getNation(nationId);
            nationMutableLiveData.observe(this, result -> {
                if (result instanceof Result.Loading) {
                    return;
                }
                if (result.isSuccess()) {
                    nation = ((Result.NationSuccess) result).getData();
                    Log.i(TAG, "GET NATION FROM FIREBASE REPO: " + nation);
                    if (driver.getTeam_id() != null) {
                        setDriverData(driver, nation, team, true, driver.getTeam_id());
                        setToolbar(true, driver.getTeam_id());
                    } else {
                        setDriverData(driver, nation, team, false, null);
                        setToolbar(false, null);
                    }
                }else {
                    if (driver.getTeam_id() != null) {
                        setDriverData(driver, null, team, true, driver.getTeam_id());
                        setToolbar(true, driver.getTeam_id());
                    } else {
                        setDriverData(driver, null, team, false, null);
                        setToolbar(false, null);
                    }
                }
            });
        }catch (RuntimeException e) {
            Log.e(TAG, "Error fetching nation data: " + e.getMessage());
            if (driver.getTeam_id() != null) {
                setDriverData(driver, null, team, true, driver.getTeam_id());
                setToolbar(true, driver.getTeam_id());
            } else {
                setDriverData(driver, null, team, false, null);
                setToolbar(false, null);
            }
        }

    }

    public void setToolbar(boolean teamIdPresent, String teamId) {

        UIUtils.singleSetTextViewText((driver.getGivenName() + " " + driver.getFamilyName()).toUpperCase(),
                findViewById(R.id.topAppBarTitle));

        if (teamIdPresent) {
            toolbar.setBackgroundColor(ContextCompat.getColor(this, Constants.TEAM_COLOR.get(teamId)));
            appBarLayout.setBackgroundColor(ContextCompat.getColor(this, Constants.TEAM_COLOR.get(teamId)));

            teamLogoCard.setOnClickListener(v ->
                    UIUtils.navigateToBioPage(this, team.getConstructorId(), 0));
        } else {
            toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.timer_gray));
            appBarLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.timer_gray));
        }
    }

    private void setDriverData(Driver driver, Nation nation, Constructor team, boolean teamIdPresent, String teamId) {
        if (teamIdPresent) {
            teamLogoCard.setStrokeColor(ContextCompat.getColor(this, Constants.TEAM_COLOR.get(teamId)));
            driverNumberCard.setStrokeColor(ContextCompat.getColor(this, Constants.TEAM_COLOR.get(teamId)));
            if (team.getConstructorId().equals("rb")) {
                teamLogoCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));
            }
        } else {
            teamLogoCard.setStrokeColor(ContextCompat.getColor(this, R.color.timer_gray));
            driverNumberCard.setStrokeColor(ContextCompat.getColor(this, R.color.timer_gray));
        }

        String nationFlagUrl = null;
        if(nation != null) {
            nationFlagUrl = nation.getNation_flag_url();
        }

        UIUtils.loadSequenceOfImagesWithGlide(this,
                new String[]{
                        team.getTeam_logo_url(),
                        nationFlagUrl,
                        driver.getDriver_pic_url(),
                        driver.getRacing_number_pic_url()},

                new ImageView[]{
                        teamLogoImage,
                        findViewById(R.id.driver_flag),
                        findViewById(R.id.driver_bio_pic),
                        driverNumberImage},

                () -> setDriverDataFinalStep(driver));
    }

    private void setDriverDataFinalStep(Driver driver) {

        UIUtils.multipleSetTextViewText(
                new String[]{driver.getBirth_place(),
                        driver.getDateOfBirth(),
                        driver.getDriverAgeAsString(),
                        driver.getWeight(),
                        driver.getHeight(),
                        driver.getBest_result(),
                        driver.getPodiums(),
                        driver.getChampionships(),
                        driver.getFirst_entry()},

                new TextView[]{findViewById(R.id.driver_birthplace),
                        findViewById(R.id.driver_birthdate),
                        findViewById(R.id.driver_age),
                        findViewById(R.id.driver_weight),
                        findViewById(R.id.driver_height),
                        findViewById(R.id.driver_best_result),
                        findViewById(R.id.driver_podiums),
                        findViewById(R.id.driver_championships),
                        findViewById(R.id.driver_first_entry)
                }
        );

        createHistoryTable();
    }

    private void createHistoryTable() {
        loadingScreen.updateProgress();

        TableLayout tableLayout = findViewById(R.id.history_table);
        tableLayout.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        if (driver.getDriver_history() != null) {

            View tableHeader = inflater.inflate(R.layout.driver_bio_table_header, tableLayout, false);
            TableLayout.LayoutParams paramsHeader = (TableLayout.LayoutParams) tableHeader.getLayoutParams();
            paramsHeader.setMargins(0, 0, 0, (int) getResources().getDisplayMetrics().density * 5);
            tableHeader.setLayoutParams(paramsHeader);
            tableHeader.setBackgroundColor(ContextCompat.getColor(this, R.color.timer_gray_dark));

            tableLayout.addView(tableHeader);

            List<DriverHistory> driverHistoryList = driver.getDriver_history();
            for (int i = driverHistoryList.size() - 1; i >= 0; i--) {
                DriverHistory driverHistory = driverHistoryList.get(i);
                View tableRow = inflater.inflate(R.layout.driver_bio_table_row, tableLayout, false);
                tableRow.setBackgroundColor(ContextCompat.getColor(this, R.color.timer_gray));

                UIUtils.multipleSetTextViewText(
                        new String[]{
                                driverHistory.getYear(),
                                driverHistory.getTeam(),
                                driverHistory.getPosition(),
                                driverHistory.getPoints(),
                                driverHistory.getWins(),
                                driverHistory.getPodiums()},

                        new TextView[]{
                                tableRow.findViewById(R.id.season_year),
                                tableRow.findViewById(R.id.team_name),
                                tableRow.findViewById(R.id.driver_position),
                                tableRow.findViewById(R.id.driver_points),
                                tableRow.findViewById(R.id.driver_wins),
                                tableRow.findViewById(R.id.driver_podiums)}
                );

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