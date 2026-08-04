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
import com.the_coffe_coders.fastestlap.domain.f1.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.f1.driver.DriverHistory;
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
import com.the_coffe_coders.fastestlap.util.NetworkUtils;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;
import com.the_coffe_coders.fastestlap.util.ui.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ui.NavigationUtils;
import com.the_coffe_coders.fastestlap.util.ui.TachometerView;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import java.util.List;

public class DriverBioActivity extends AppCompatActivity {

    private final String TAG = "DriverBioActivity";
    LoadingScreen loadingScreen;
    private Driver driver;
    private Nation nation;
    private Constructor team;
    private MaterialCardView teamLogoCard;
    private ImageView teamLogoImage;
    private MaterialToolbar toolbar;
    private AppBarLayout appBarLayout;
    private ImageView driverNumberImage;
    private String driverId;
    private SwipeRefreshLayout driverBioLayout;

    private DriverViewModel driverViewModel;
    private NationViewModel nationViewModel;
    private ConstructorViewModel constructorViewModel;
    private UserViewModel userViewModel;
    private FavoriteBioHandler favoriteBioHandler;

    private TachometerView winPercentageTachometer, podiumPercentageTachometer;


    private NetworkUtils networkLiveData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_driver_bio);

        networkLiveData = new NetworkUtils(this);
        favoriteBioHandler = new FavoriteBioHandler(this);

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
        if (networkLiveData.isConnected()) {
            favoriteItem.setOnMenuItemClickListener(v -> {
                String updatedFavoriteId = favoriteBioHandler.toggleFavorite(
                        driverId,
                        Constants.SHARED_PREFERENCES_FAVORITE_DRIVER,
                        favoriteItem,
                        favoriteId -> {
                            String idToken = userViewModel.getLoggedUser() != null ? userViewModel.getLoggedUser().getIdToken() : null;
                            if (idToken != null) {
                                userViewModel.saveUserDriverPreferences(favoriteId, idToken);
                            }
                        });

                if ("null".equals(updatedFavoriteId)) {
                    Log.i(TAG, "Removed favorite driver: " + driverId);
                } else {
                    Log.i(TAG, "Set favorite driver to: " + driverId);
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

        UIUtils.applyWindowInsets(driverBioLayout);
        driverBioLayout.setOnRefreshListener(() -> {
            start();
            driverBioLayout.setRefreshing(false);
        });

        driverId = getIntent().getStringExtra("DRIVER_ID");
        Log.i("DriverBioActivity", "Driver ID: " + driverId);

        teamLogoCard = findViewById(R.id.team_logo_card);
        teamLogoImage = findViewById(R.id.team_logo_image);

        driverNumberImage = findViewById(R.id.driver_number_image);

        winPercentageTachometer = findViewById(R.id.win_percentage_tachometer_driver);
        podiumPercentageTachometer = findViewById(R.id.podium_percentage_tachometer_driver);

        initializeViewModels();
    }

    private void initializeViewModels() {
        driverViewModel = new ViewModelProvider(this, new DriverViewModelFactory(getApplication())).get(DriverViewModel.class);
        constructorViewModel = new ViewModelProvider(this, new ConstructorViewModelFactory(getApplication())).get(ConstructorViewModel.class);
        nationViewModel = new ViewModelProvider(this, new NationViewModelFactory(getApplication())).get(NationViewModel.class);
        IUserRepository userRepository = ServiceLocator.getInstance().getUserRepository(getApplication());
        userViewModel = new ViewModelProvider(getViewModelStore(), new UserViewModelFactory(userRepository)).get(UserViewModel.class);

        createDriverBioPage(driverId);
    }

    public void createDriverBioPage(String driverId) {
        MutableLiveData<Result> driverMutableLiveData = driverViewModel.getDriver(driverId);
        @SuppressWarnings("unchecked")
        androidx.lifecycle.Observer<Result>[] observerHolder = new androidx.lifecycle.Observer[1];
        observerHolder[0] = result -> {
            if (result instanceof Result.Loading) {
                return;
            }
            driverMutableLiveData.removeObserver(observerHolder[0]);
            if (result.isSuccess()) {
                driver = ((Result.DriverSuccess) result).getData();
                Log.i(TAG, "DRIVER SUCCESS: " + driver);

                favoriteBioHandler.updateFavoriteIcon(toolbar.getMenu(), R.id.favourite_icon_outline, driverId, Constants.SHARED_PREFERENCES_FAVORITE_DRIVER);
                if (driver.getTeam_id() != null) {
                    getTeamInfo(driver.getTeam_id());
                } else {
                    getNationInfo(driver.getNationality());
                }
            } else {
                Log.e(TAG, "DRIVER ERROR: " + result.getError());
                loadingScreen.hideLoadingScreen();
            }
        };
        driverMutableLiveData.observe(this, observerHolder[0]);
    }

    public void getTeamInfo(String teamId) {
        loadingScreen.updateProgress();

        if (teamId == null) {
            getNationInfo(driver.getNationality());
            return;
        }

        MutableLiveData<Result> constructorMutableLiveData = constructorViewModel.getSelectedConstructor(teamId);
        @SuppressWarnings("unchecked")
        androidx.lifecycle.Observer<Result>[] observerHolder = new androidx.lifecycle.Observer[1];
        observerHolder[0] = result -> {
            if (result instanceof Result.Loading) {
                return;
            }
            constructorMutableLiveData.removeObserver(observerHolder[0]);
            if (result.isSuccess()) {
                team = ((Result.ConstructorSuccess) result).getData();
                Log.i(TAG, "GET CONSTRUCTOR SUCCESS: " + team);
            } else {
                Log.w(TAG, "GET CONSTRUCTOR ERROR: " + result.getError());
            }
            getNationInfo(driver.getNationality());
        };
        constructorMutableLiveData.observe(this, observerHolder[0]);
    }

    public void getNationInfo(String nationId) {
        if (nationId == null) {
            boolean hasTeam = (driver != null && driver.getTeam_id() != null && team != null);
            setDriverData(driver, null, team, hasTeam, hasTeam ? driver.getTeam_id() : null);
            setToolbar(hasTeam, hasTeam ? driver.getTeam_id() : null);
            return;
        }

        try {
            MutableLiveData<Result> nationMutableLiveData = nationViewModel.getNation(nationId);
            @SuppressWarnings("unchecked")
            androidx.lifecycle.Observer<Result>[] observerHolder = new androidx.lifecycle.Observer[1];
            observerHolder[0] = result -> {
                if (result instanceof Result.Loading) {
                    return;
                }
                nationMutableLiveData.removeObserver(observerHolder[0]);
                boolean hasTeam = (driver != null && driver.getTeam_id() != null && team != null);
                if (result.isSuccess()) {
                    nation = ((Result.NationSuccess) result).getData();
                    Log.i(TAG, "GET NATION SUCCESS: " + nation);
                } else {
                    Log.w(TAG, "GET NATION ERROR: " + result.getError());
                }
                setDriverData(driver, nation, team, hasTeam, hasTeam ? driver.getTeam_id() : null);
                setToolbar(hasTeam, hasTeam ? driver.getTeam_id() : null);
            };
            nationMutableLiveData.observe(this, observerHolder[0]);
        } catch (RuntimeException e) {
            Log.e(TAG, "Error fetching nation data: " + e.getMessage());
            boolean hasTeam = (driver != null && driver.getTeam_id() != null && team != null);
            setDriverData(driver, null, team, hasTeam, hasTeam ? driver.getTeam_id() : null);
            setToolbar(hasTeam, hasTeam ? driver.getTeam_id() : null);
        }
    }

    public void setToolbar(boolean teamIdPresent, String teamId) {

        UIUtils.singleSetTextViewText((driver.getGivenName() + " " + driver.getFamilyName()).toUpperCase(java.util.Locale.ROOT),
                findViewById(R.id.topAppBarTitle));

        if (teamIdPresent) {
            Integer teamColor;
            try {
                teamColor = Constants.TEAM_COLOR.get(teamId);
            } catch (Exception e) {
                teamColor = R.color.timer_gray;
            }
            if (teamColor == null) {
                teamColor = R.color.timer_gray;
            }

            toolbar.setBackgroundColor(ContextCompat.getColor(this, teamColor));
            appBarLayout.setBackgroundColor(ContextCompat.getColor(this, teamColor));

            teamLogoCard.setOnClickListener(v ->
                    NavigationUtils.navigateToBioPage(this, team.getConstructorId(), 0));
        } else {
            toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.timer_gray));
            appBarLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.timer_gray));
        }
    }

    private void setDriverData(Driver driver, Nation nation, Constructor team, boolean teamIdPresent, String teamId) {
        if (teamIdPresent) {
            Integer teamColor;
            try {
                teamColor = Constants.TEAM_COLOR.get(teamId);
            } catch (Exception e) {
                teamColor = R.color.timer_gray;
            }
            if (teamColor == null) {
                teamColor = R.color.timer_gray;
            }

            teamLogoCard.setStrokeColor(ContextCompat.getColor(this, teamColor));

            if (team != null && "rb".equals(team.getConstructorId())) {
                teamLogoCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));
            }
        } else {
            teamLogoCard.setStrokeColor(ContextCompat.getColor(this, R.color.timer_gray));
        }

        // Popola subito tutti i dati di testo, i tachimetri e la tabella dello storico
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

        UIUtils.updateTachometers(this, driver, winPercentageTachometer, podiumPercentageTachometer);
        createHistoryTable();

        String nationFlagUrl = nation != null ? nation.getNation_flag_url() : null;
        String teamLogoUrl = team != null ? team.getTeam_logo_url() : null;

        // Carica tutte le immagini in parallelo e nascondi la schermata di caricamento solo al completamento
        UIUtils.loadImagesInParallel(this,
                new String[]{
                        teamLogoUrl,
                        nationFlagUrl,
                        driver.getDriver_full_pic_url(),
                        driver.getRacing_number_pic_url()},


                new ImageView[]{
                        teamLogoImage,
                        findViewById(R.id.driver_flag),
                        findViewById(R.id.driver_bio_pic),
                        driverNumberImage},

                () -> {
                    Log.i("ActivityDataLog", "DATA_AND_IMAGES_FULLY_LOADED: DriverBioActivity at " + System.currentTimeMillis());
                    loadingScreen.hideLoadingScreenImmediately();
                    if (winPercentageTachometer != null) winPercentageTachometer.startAnimation();
                    if (podiumPercentageTachometer != null) podiumPercentageTachometer.startAnimation();
                });
    }



    private void createHistoryTable() {
        loadingScreen.updateProgress();

        LinearLayout driverHistoryLayout = findViewById(R.id.driver_history);

        TableLayout tableLayout = findViewById(R.id.history_table);
        tableLayout.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        if (driver.getDriver_history() != null) {
            driverHistoryLayout.setVisibility(View.VISIBLE);
            tableLayout.setVisibility(View.VISIBLE);

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
        } else {
            Log.e(TAG, "Driver history is null");
            driverHistoryLayout.setVisibility(View.GONE);
            tableLayout.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}