package com.the_coffe_coders.fastestlap.ui.bio;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.the_coffe_coders.fastestlap.domain.f1.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.f1.constructor.ConstructorHistory;
import com.the_coffe_coders.fastestlap.domain.f1.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.nation.Nation;
import com.the_coffe_coders.fastestlap.repository.user.IUserRepository;
import com.the_coffe_coders.fastestlap.ui.bio.handler.FavoriteBioHandler;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.ConstructorViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.ConstructorViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.DriverViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.DriverViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.NationViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.NationViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.welcome.viewmodel.UserViewModel;
import com.the_coffe_coders.fastestlap.ui.welcome.viewmodel.UserViewModelFactory;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.service.NetworkUtils;
import com.the_coffe_coders.fastestlap.util.service.ServiceLocator;
import com.the_coffe_coders.fastestlap.util.ui.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ui.NavigationUtils;
import com.the_coffe_coders.fastestlap.util.ui.TachometerView;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import java.util.List;

public class ConstructorBioActivity extends AppCompatActivity {

    private static final String TAG = "ConstructorBioActivity";
    LoadingScreen loadingScreen;
    MaterialToolbar toolbar;
    AppBarLayout appBarLayout;
    private DriverViewModel driverViewModel;
    private NationViewModel nationViewModel;
    private ConstructorViewModel constructorViewModel;
    private UserViewModel userViewModel;
    private FavoriteBioHandler favoriteBioHandler;

    private SwipeRefreshLayout constructorBioLayout;
    private String teamId;
    private Constructor constructor;
    private Nation nation;
    private Driver driverOne;
    private Driver driverTwo;

    private TachometerView winPercentageTachometer, podiumPercentageTachometer;

    private NetworkUtils networkLiveData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_constructor_bio);

        networkLiveData = new NetworkUtils(this);
        favoriteBioHandler = new FavoriteBioHandler(this);

        start();
    }

    private void start() {
        constructorBioLayout = findViewById(R.id.constructor_bio_layout);

        loadingScreen = new LoadingScreen(getWindow().getDecorView(), this, constructorBioLayout, null);
        loadingScreen.showLoadingScreen(false);
        loadingScreen.updateProgress();

        toolbar = findViewById(R.id.topAppBar);
        UIUtils.applyWindowInsets(toolbar);

        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        Menu menu = toolbar.getMenu();
        MenuItem favoriteItem = menu.findItem(R.id.favourite_icon_outline);

        if (networkLiveData.isConnected()) {
            favoriteItem.setOnMenuItemClickListener(v -> {
                String updatedFavoriteId = favoriteBioHandler.toggleFavorite(
                        teamId,
                        Constants.SHARED_PREFERENCES_FAVORITE_TEAM,
                        favoriteItem,
                        favoriteId -> {
                            String idToken = userViewModel.getLoggedUser() != null ? userViewModel.getLoggedUser().getIdToken() : null;
                            if (idToken != null) {
                                userViewModel.saveUserConstructorPreferences(favoriteId, idToken);
                            }
                        });

                if ("null".equals(updatedFavoriteId)) {
                    Log.i(TAG, "Removed favorite constructor: " + teamId);
                } else {
                    Log.i(TAG, "Set favorite constructor to: " + teamId);
                }
                return true;
            });
        } else {
            favoriteItem.setOnMenuItemClickListener(v -> {
                Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        appBarLayout = findViewById(R.id.top_bar_layout);

        UIUtils.applyWindowInsets(constructorBioLayout);
        constructorBioLayout.setOnRefreshListener(() -> {
            start();
            constructorBioLayout.setRefreshing(false);
        });

        teamId = getIntent().getStringExtra("TEAM_ID");
        Log.i("ConstructorBioActivity", "Team ID: " + teamId);

        winPercentageTachometer = findViewById(R.id.win_percentage_tachometer_team);
        podiumPercentageTachometer = findViewById(R.id.podium_percentage_tachometer_team);

        initializeViewModels();

    }

    private void initializeViewModels() {
        driverViewModel = new ViewModelProvider(this, new DriverViewModelFactory(getApplication())).get(DriverViewModel.class);
        constructorViewModel = new ViewModelProvider(this, new ConstructorViewModelFactory(getApplication())).get(ConstructorViewModel.class);
        nationViewModel = new ViewModelProvider(this, new NationViewModelFactory(getApplication())).get(NationViewModel.class);
        IUserRepository userRepository = ServiceLocator.getInstance().getUserRepository(getApplication());
        userViewModel = new ViewModelProvider(getViewModelStore(), new UserViewModelFactory(userRepository)).get(UserViewModel.class);

        createConstructorBioPage(teamId);
    }


    private void createConstructorBioPage(String teamId) {
        MutableLiveData<Result> data = constructorViewModel.getSelectedConstructor(teamId);
        @SuppressWarnings("unchecked")
        androidx.lifecycle.Observer<Result>[] observerHolder = new androidx.lifecycle.Observer[1];
        observerHolder[0] = result -> {
            if (result instanceof Result.Loading) {
                return;
            }
            data.removeObserver(observerHolder[0]);
            if (result.isSuccess()) {
                constructor = ((Result.ConstructorSuccess) result).getData();

                if (constructor == null) {
                    NavigationUtils.navigateToHomePage(this);
                } else {
                    Log.i(TAG, "Constructor loaded: " + constructor);

                    UIUtils.singleSetTextViewText(constructor.getName().toUpperCase(), findViewById(R.id.topAppBarTitle));

                    Integer teamColor = Constants.TEAM_COLOR.get(teamId);
                    if (teamColor == null) {
                        teamColor = R.color.timer_gray;
                    }

                    toolbar.setBackgroundColor(ContextCompat.getColor(this, teamColor));
                    appBarLayout.setBackgroundColor(ContextCompat.getColor(this, teamColor));

                    MaterialCardView teamLogoCard = findViewById(R.id.team_logo_card);
                    teamLogoCard.setStrokeColor(ContextCompat.getColor(this, teamColor));

                    if (teamId.equals("rb")) {
                        teamLogoCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));
                    }

                    MaterialCardView driverCard = findViewById(R.id.driver_1_card);
                    driverCard.setCardBackgroundColor(ContextCompat.getColor(this, teamColor));

                    driverCard = findViewById(R.id.driver_2_card);
                    driverCard.setCardBackgroundColor(ContextCompat.getColor(this, teamColor));

                    favoriteBioHandler.updateFavoriteIcon(toolbar.getMenu(), R.id.favourite_icon_outline, teamId, Constants.SHARED_PREFERENCES_FAVORITE_TEAM);

                    fetchAllTeamDependencies(constructor);
                }
            } else {
                Log.e(TAG, "Error fetching constructor: " + result.getError());
                loadingScreen.hideLoadingScreen();
            }
        };
        data.observe(this, observerHolder[0]);
    }

    private void fetchAllTeamDependencies(Constructor team) {
        java.util.concurrent.atomic.AtomicInteger pendingCount = new java.util.concurrent.atomic.AtomicInteger(3);

        Runnable checkComplete = () -> {
            if (pendingCount.decrementAndGet() == 0) {
                setTeamData(constructor, nation, driverOne, driverTwo);
            }
        };

        // Fetch Driver 1 in parallel
        if (team.getDriverOneId() != null) {
            MutableLiveData<Result> driver1Data = driverViewModel.getDriver(team.getDriverOneId());
            @SuppressWarnings("unchecked")
            androidx.lifecycle.Observer<Result>[] observer1 = new androidx.lifecycle.Observer[1];
            observer1[0] = result -> {
                if (result instanceof Result.Loading) return;
                driver1Data.removeObserver(observer1[0]);
                if (result.isSuccess()) {
                    driverOne = ((Result.DriverSuccess) result).getData();
                    MaterialCardView driverOneCard = findViewById(R.id.driver_1_card);
                    driverOneCard.setOnClickListener(v ->
                            NavigationUtils.navigateToBioPage(this, driverOne.getDriverId(), 1));
                }
                checkComplete.run();
            };
            driver1Data.observe(this, observer1[0]);
        } else {
            checkComplete.run();
        }

        // Fetch Driver 2 in parallel
        if (team.getDriverTwoId() != null) {
            MutableLiveData<Result> driver2Data = driverViewModel.getDriver(team.getDriverTwoId());
            @SuppressWarnings("unchecked")
            androidx.lifecycle.Observer<Result>[] observer2 = new androidx.lifecycle.Observer[1];
            observer2[0] = result -> {
                if (result instanceof Result.Loading) return;
                driver2Data.removeObserver(observer2[0]);
                if (result.isSuccess()) {
                    driverTwo = ((Result.DriverSuccess) result).getData();
                    MaterialCardView driverTwoCard = findViewById(R.id.driver_2_card);
                    driverTwoCard.setOnClickListener(v ->
                            NavigationUtils.navigateToBioPage(this, driverTwo.getDriverId(), 1));
                }
                checkComplete.run();
            };
            driver2Data.observe(this, observer2[0]);
        } else {
            checkComplete.run();
        }

        // Fetch Nation in parallel
        if (team.getNationality() != null) {
            try {
                MutableLiveData<Result> nationData = nationViewModel.getNation(team.getNationality());
                @SuppressWarnings("unchecked")
                androidx.lifecycle.Observer<Result>[] observerNation = new androidx.lifecycle.Observer[1];
                observerNation[0] = result -> {
                    if (result instanceof Result.Loading) return;
                    nationData.removeObserver(observerNation[0]);
                    if (result.isSuccess()) {
                        nation = ((Result.NationSuccess) result).getData();
                    }
                    checkComplete.run();
                };
                nationData.observe(this, observerNation[0]);
            } catch (Exception e) {
                Log.e(TAG, "Error fetching nation: " + e.getMessage());
                checkComplete.run();
            }
        } else {
            checkComplete.run();
        }
    }

    private void setTeamData(Constructor team, Nation nation, Driver driverOne, Driver driverTwo) {
        loadingScreen.updateProgress();

        String d1Name = driverOne != null ? driverOne.getGivenName() + " " + driverOne.getFamilyName() : "TBA";
        String d2Name = driverTwo != null ? driverTwo.getGivenName() + " " + driverTwo.getFamilyName() : "TBA";

        // Popola subito tutti i dati di testo, i tachimetri e la tabella dello storico
        UIUtils.multipleSetTextViewText(
                new String[]{d1Name,
                        d2Name,
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

        UIUtils.updateTachometers(this, team, winPercentageTachometer, podiumPercentageTachometer);
        createHistoryTable();

        String nationFlagUrl = nation != null ? nation.getNation_flag_url() : null;
        String d1HalfPic = driverOne != null ? driverOne.getDriver_half_pic_url() : null;
        String d2HalfPic = driverTwo != null ? driverTwo.getDriver_half_pic_url() : null;

        // Carica le immagini in parallelo e nascondi la schermata di caricamento solo al completamento
        UIUtils.loadImagesInParallel(this,
                new String[]{
                        team.getTeam_logo_url(),
                        nationFlagUrl,
                        team.getCar_pic_url(),
                        d1HalfPic,
                        d2HalfPic},

                new ImageView[]{
                        findViewById(R.id.team_logo_image),
                        findViewById(R.id.team_flag),
                        findViewById(R.id.team_car_image),
                        findViewById(R.id.driver_1_image),
                        findViewById(R.id.driver_2_image)},

                () -> {
                    Log.i("ActivityDataLog", "DATA_AND_IMAGES_FULLY_LOADED: ConstructorBioActivity at " + System.currentTimeMillis());
                    loadingScreen.hideLoadingScreen();
                    if (winPercentageTachometer != null) winPercentageTachometer.startAnimation();
                    if (podiumPercentageTachometer != null) podiumPercentageTachometer.startAnimation();
                });

    }



    private void createHistoryTable() {
        loadingScreen.updateProgress();

        LinearLayout teamHistory = findViewById(R.id.team_history);

        TableLayout tableLayout = findViewById(R.id.history_table);
        tableLayout.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        if (constructor.getTeam_history() != null && !constructor.getTeam_history().isEmpty()) {
            teamHistory.setVisibility(View.VISIBLE);
            tableLayout.setVisibility(View.VISIBLE);

            View tableHeader = inflater.inflate(R.layout.constructor_bio_table_header, tableLayout, false);
            TableLayout.LayoutParams paramsHeader = (TableLayout.LayoutParams) tableHeader.getLayoutParams();
            paramsHeader.setMargins(0, 0, 0, (int) getResources().getDisplayMetrics().density * 5);
            tableHeader.setLayoutParams(paramsHeader);
            tableHeader.setBackgroundColor(ContextCompat.getColor(this, R.color.timer_gray_dark));

            //set stroke of tableHeader
            tableLayout.addView(tableHeader);

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
        } else {
            Log.e(TAG, "Constructor history is null");
            teamHistory.setVisibility(View.GONE);
            tableLayout.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}