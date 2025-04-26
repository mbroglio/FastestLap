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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.constructor.ConstructorHistory;
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

public class ConstructorBioActivity extends AppCompatActivity {

    private static final String TAG = "ConstructorBioActivity";
    LoadingScreen loadingScreen;
    MaterialToolbar toolbar;
    AppBarLayout appBarLayout;
    private DriverViewModel driverViewModel;
    private NationViewModel nationViewModel;
    private ConstructorViewModel constructorViewModel;

    private SwipeRefreshLayout constructorBioLayout;
    private String teamId;
    private Constructor constructor;
    private Nation nation;
    private Driver driverOne;
    private Driver driverTwo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_constructor_bio);

        start();
    }

    private void start(){
        constructorBioLayout = findViewById(R.id.constructor_bio_layout);

        loadingScreen = new LoadingScreen(getWindow().getDecorView(), this, constructorBioLayout, null);
        loadingScreen.showLoadingScreen();
        loadingScreen.updateProgress(0);

        toolbar = findViewById(R.id.topAppBar);
        UIUtils.applyWindowInsets(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        Menu menu = toolbar.getMenu();
        MenuItem favoriteItem = menu.findItem(R.id.favourite_icon_outline);
        favoriteItem.setOnMenuItemClickListener(v -> {
            toggleFavoriteConstructor(teamId, favoriteItem);
            return true;
        });

        appBarLayout = findViewById(R.id.top_bar_layout);

        UIUtils.applyWindowInsets(constructorBioLayout);
        constructorBioLayout.setOnRefreshListener(() -> {
            start();
            constructorBioLayout.setRefreshing(false);
        });

        teamId = getIntent().getStringExtra("TEAM_ID");
        Log.i("ConstructorBioActivity", "Team ID: " + teamId);

        initializeViewModels();

    }

    private void initializeViewModels() {
        driverViewModel = new ViewModelProvider(this, new DriverViewModelFactory(getApplication())).get(DriverViewModel.class);
        constructorViewModel = new ViewModelProvider(this, new ConstructorViewModelFactory()).get(ConstructorViewModel.class);
        nationViewModel = new ViewModelProvider(this, new NationViewModelFactory(getApplication())).get(NationViewModel.class);

        createConstructorBioPage(teamId);
    }

    private void toggleFavoriteConstructor(String teamId, MenuItem menuItem) {
        SharedPreferencesUtils sharedPreferencesUtils = new SharedPreferencesUtils(this);
        String currentFavoriteTeamId = sharedPreferencesUtils.readStringData(Constants.SHARED_PREFERENCES_FILENAME, Constants.SHARED_PREFERENCES_FAVORITE_TEAM);

        IUserRepository userRepository = ServiceLocator.getInstance().getUserRepository(getApplication());
        UserViewModel userViewModel = new ViewModelProvider(getViewModelStore(), new UserViewModelFactory(userRepository)).get(UserViewModel.class);
        User currentUser = userViewModel.getLoggedUser();

        if (currentFavoriteTeamId.equals(teamId)) {
            // Remove as favorite
            sharedPreferencesUtils.writeStringData(Constants.SHARED_PREFERENCES_FILENAME, Constants.SHARED_PREFERENCES_FAVORITE_TEAM, "null");
            menuItem.setIcon(R.drawable.baseline_star_border_24);

            // Update user preferences in backend (if needed)
            userViewModel.saveUserConstructorPreferences("", currentUser.getIdToken());

            Log.i(TAG, "Removed favorite constructor: " + teamId);
        } else {
            // Set as favorite
            sharedPreferencesUtils.writeStringData(Constants.SHARED_PREFERENCES_FILENAME, Constants.SHARED_PREFERENCES_FAVORITE_TEAM, teamId);
            menuItem.setIcon(R.drawable.star_fav);

            // Update user preferences in backend
            userViewModel.saveUserConstructorPreferences(teamId, currentUser.getIdToken());

            Log.i(TAG, "Set favorite constructor to: " + teamId);
        }
    }

    private void createConstructorBioPage(String teamId) {

        loadingScreen.postLoadingStatus(this.getString(R.string.initializing));

        MutableLiveData<Result> data = constructorViewModel.getSelectedConstructor(teamId);
        data.observe(this, result -> {
            if(result instanceof Result.Loading) {
                return;
            }
            if (result.isSuccess()) {
                constructor = ((Result.ConstructorSuccess) result).getData();
                Log.i(TAG, "Constructor: " + constructor);

                UIUtils.singleSetTextViewText(constructor.getName().toUpperCase(), findViewById(R.id.topAppBarTitle));

                toolbar.setBackgroundColor(ContextCompat.getColor(this, Constants.TEAM_COLOR.get(teamId)));
                appBarLayout.setBackgroundColor(ContextCompat.getColor(this, Constants.TEAM_COLOR.get(teamId)));

                MaterialCardView teamLogoCard = findViewById(R.id.team_logo_card);
                teamLogoCard.setStrokeColor(ContextCompat.getColor(this, Constants.TEAM_COLOR.get(teamId)));

                if(teamId.equals("rb")){
                    teamLogoCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));
                }

                MaterialCardView driverCard = findViewById(R.id.driver_1_card);
                driverCard.setCardBackgroundColor(ContextCompat.getColor(this, Constants.TEAM_COLOR.get(teamId)));

                driverCard = findViewById(R.id.driver_2_card);
                driverCard.setCardBackgroundColor(ContextCompat.getColor(this, Constants.TEAM_COLOR.get(teamId)));

                // Check if this constructor is the favorite and update the icon
                updateFavoriteIcon(teamId);

                getDriverData(constructor.getDriverOneId(), constructor);
            }
        });
    }

    private void updateFavoriteIcon(String teamId) {
        SharedPreferencesUtils sharedPreferencesUtils = new SharedPreferencesUtils(this);
        String favoriteTeamId = sharedPreferencesUtils.readStringData(Constants.SHARED_PREFERENCES_FILENAME, Constants.SHARED_PREFERENCES_FAVORITE_TEAM);

        Menu menu = toolbar.getMenu();
        MenuItem favoriteItem = menu.findItem(R.id.favourite_icon_outline);

        if (favoriteTeamId.equals(teamId)) {
            favoriteItem.setIcon(R.drawable.star_fav);
        } else {
            favoriteItem.setIcon(R.drawable.baseline_star_border_24);
        }
    }

    public void getDriverData(String driverId, Constructor team) {
        Log.i("ConstructorBioActivity", "Getting driver data");
        MutableLiveData<Result> data = driverViewModel.getDriver(driverId);

        data.observe(this, result -> {
            if (result instanceof Result.Loading) {
                return;
            }
            if (result.isSuccess()) {
                if (driverId.equals(team.getDriverOneId())) {
                    driverOne = ((Result.DriverSuccess) result).getData();

                    MaterialCardView driverOneCard = findViewById(R.id.driver_1_card);
                    driverOneCard.setOnClickListener(v -> {
                        Intent intent = new Intent(ConstructorBioActivity.this, DriverBioActivity.class);
                        intent.putExtra("DRIVER_ID", driverId);
                        startActivity(intent);
                    });
                    getDriverData(team.getDriverTwoId(), team);

                } else {
                    driverTwo = ((Result.DriverSuccess) result).getData();

                    MaterialCardView driverTwoCard = findViewById(R.id.driver_2_card);
                    driverTwoCard.setOnClickListener(v -> {
                        Intent intent = new Intent(ConstructorBioActivity.this, DriverBioActivity.class);
                        intent.putExtra("DRIVER_ID", driverId);
                        startActivity(intent);
                    });

                    getNationData(team.getNationality());
                }
            }
        });
    }

    public void getNationData(String nationId) {
        Log.i("ConstructorBioActivity", "Getting nation data");
        MutableLiveData<Result> data = nationViewModel.getNation(nationId);

        data.observe(this, result -> {
            if (result instanceof Result.Loading) {
                return;
            }
            if (result.isSuccess()) {
                nation = ((Result.NationSuccess) result).getData();
                setTeamData(constructor, nation, driverOne, driverTwo);
            }
        });

        loadingScreen.hideLoadingScreen();
    }

    private void setTeamData(Constructor team, Nation nation, Driver driverOne, Driver driverTwo) {
        loadingScreen.postLoadingStatus(this.getString(R.string.fetching_constructor_info));
        loadingScreen.updateProgress(50);

        UIUtils.loadSequenceOfImagesWithGlide(this,
                new String[]{team.getTeam_logo_url(), nation.getNation_flag_url(), team.getCar_pic_url(), driverOne.getDriver_pic_url(), driverTwo.getDriver_pic_url()},
                new ImageView[]{findViewById(R.id.team_logo_image), findViewById(R.id.team_flag), findViewById(R.id.team_car_image), findViewById(R.id.driver_1_image), findViewById(R.id.driver_2_image)},
                () -> setTeamDataFinalStep(team));

    }

    private void setTeamDataFinalStep(Constructor team) {

        UIUtils.multipleSetTextViewText(
                new String[]{driverOne.getGivenName() + " " + driverOne.getFamilyName(),
                        driverTwo.getGivenName() + " " + driverTwo.getFamilyName(),
                        team.getFull_name(),
                        team.getHq(),
                        team.getTeam_principal(),
                        team.getChassis(),
                        team.getPower_unit(),
                        team.getFirst_entry(),
                        team.getWorld_championships(),
                        team.getWins(),
                        team.getPodiums()},

                new TextView[]{findViewById(R.id.driver_1_name),
                        findViewById(R.id.driver_2_name),
                        findViewById(R.id.team_full_name_value),
                        findViewById(R.id.team_base_value),
                        findViewById(R.id.team_principal_value),
                        findViewById(R.id.team_chassis_value),
                        findViewById(R.id.team_power_unit_value),
                        findViewById(R.id.team_first_entry_value),
                        findViewById(R.id.team_championships_value),
                        findViewById(R.id.team_wins_value),
                        findViewById(R.id.team_podiums_value)});

        createHistoryTable();
    }

    private void createHistoryTable() {
        Log.i("ConstructorBioActivity", "Creating history table");

        loadingScreen.postLoadingStatus(this.getString(R.string.setting_constructor_history));
        loadingScreen.updateProgress(100);

        TableLayout tableLayout = findViewById(R.id.history_table);
        tableLayout.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        View tableHeader = inflater.inflate(R.layout.constructor_bio_table_header, tableLayout, false);
        TableLayout.LayoutParams paramsHeader = (TableLayout.LayoutParams) tableHeader.getLayoutParams();
        paramsHeader.setMargins(0, 0, 0, (int) getResources().getDisplayMetrics().density * 5);
        tableHeader.setLayoutParams(paramsHeader);
        tableHeader.setBackgroundColor(ContextCompat.getColor(this, R.color.timer_gray_dark));

        //set stroke of tableHeader
        tableLayout.addView(tableHeader);

        if (constructor.getTeam_history() != null) {
            List<ConstructorHistory> constructorHistoryList = constructor.getTeam_history();
            for (int i = constructorHistoryList.size() - 1; i >= 0; i--) {
                ConstructorHistory constructorHistory = constructorHistoryList.get(i);
                View tableRow = inflater.inflate(R.layout.constructor_bio_table_row, tableLayout, false);
                tableRow.setBackgroundColor(ContextCompat.getColor(this, R.color.timer_gray));

                UIUtils.multipleSetTextViewText(
                        new String[]{constructorHistory.getYear(),
                                constructorHistory.getPosition(),
                                constructorHistory.getPoints(),
                                constructorHistory.getWins(),
                                constructorHistory.getPodiums()},

                        new TextView[]{tableRow.findViewById(R.id.season_year),
                                tableRow.findViewById(R.id.team_position),
                                tableRow.findViewById(R.id.team_points),
                                tableRow.findViewById(R.id.team_wins),
                                tableRow.findViewById(R.id.team_podiums)}
                );


                TableLayout.LayoutParams tableParams = (TableLayout.LayoutParams) tableRow.getLayoutParams();
                tableParams.setMargins(0, 0, 0, (int) getResources().getDisplayMetrics().density * 5);
                tableRow.setLayoutParams(tableParams);

                tableLayout.addView(tableRow);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}