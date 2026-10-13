package com.the_coffe_coders.fastestlap.ui.bio;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.adapter.f1.DriverBioPagerAdapter;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.f1.constructor.Constructor;
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
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import java.util.Locale;

public class DriverBioActivity extends AppCompatActivity {

    private static final String TAG = "DriverBioActivity";
    private LoadingScreen loadingScreen;
    private Driver driver;
    private Nation nation;
    private Constructor team;
    private MaterialToolbar toolbar;
    private AppBarLayout appBarLayout;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private String driverId;

    private DriverViewModel driverViewModel;
    private NationViewModel nationViewModel;
    private ConstructorViewModel constructorViewModel;
    private UserViewModel userViewModel;
    private FavoriteBioHandler favoriteBioHandler;

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
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        viewPager.setUserInputEnabled(false);

        loadingScreen = new LoadingScreen(getWindow().getDecorView(), this, viewPager, null);
        loadingScreen.showLoadingScreen(false);
        loadingScreen.updateProgress();

        toolbar = findViewById(R.id.topAppBar);
        UIUtils.applyWindowInsets(toolbar);

        UIUtils.applyWindowInsets(viewPager);

        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        Menu menu = toolbar.getMenu();
        MenuItem favoriteItem = menu.findItem(R.id.favourite_icon_outline);
        if (networkLiveData.isConnected()) {
            favoriteItem.setOnMenuItemClickListener(v -> {
                String updatedFavoriteId = favoriteBioHandler.toggleFavorite(
                        driverId,
                        Constants.SHARED_PREFERENCES_FAVORITE_DRIVER,
                        favoriteItem,
                        favId -> {
                            String idToken = userViewModel.getLoggedUser() != null ? userViewModel.getLoggedUser().getIdToken() : null;
                            if (idToken != null) {
                                userViewModel.saveUserDriverPreferences(favId, idToken);
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

        driverId = getIntent().getStringExtra("DRIVER_ID");
        Log.i(TAG, "Driver ID: " + driverId);

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
        driverMutableLiveData.observe(this, result -> {
            if (result instanceof Result.Loading) {
                return;
            }
            if (result.isSuccess()) {
                driver = ((Result.DriverSuccess) result).getData();
                Log.i(TAG, "DRIVER SUCCESS: " + driver);

                favoriteBioHandler.updateFavoriteIcon(toolbar.getMenu(), R.id.favourite_icon_outline, driverId, Constants.SHARED_PREFERENCES_FAVORITE_DRIVER);
                if (driver != null && driver.getTeam_id() != null) {
                    getTeamInfo(driver.getTeam_id());
                } else if (driver != null) {
                    getNationInfo(driver.getNationality());
                } else {
                    loadingScreen.hideLoadingScreenImmediately();
                }
            } else {
                Log.e(TAG, "DRIVER ERROR: " + result.getError());
                loadingScreen.hideLoadingScreenImmediately();
            }
        });
    }

    public void getTeamInfo(String teamId) {
        loadingScreen.updateProgress();

        if (teamId == null) {
            getNationInfo(driver != null ? driver.getNationality() : null);
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
            getNationInfo(driver != null ? driver.getNationality() : null);
        };
        constructorMutableLiveData.observe(this, observerHolder[0]);
    }

    public void getNationInfo(String nationId) {
        if (nationId == null) {
            setupDriverBio(driver, null, team);
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
                if (result.isSuccess()) {
                    nation = ((Result.NationSuccess) result).getData();
                    Log.i(TAG, "GET NATION SUCCESS: " + nation);
                } else {
                    Log.w(TAG, "GET NATION ERROR: " + result.getError());
                }
                setupDriverBio(driver, nation, team);
            };
            nationMutableLiveData.observe(this, observerHolder[0]);
        } catch (RuntimeException e) {
            Log.e(TAG, "Error fetching nation data: " + e.getMessage());
            setupDriverBio(driver, null, team);
        }
    }

    private void setupDriverBio(Driver driver, Nation nation, Constructor team) {
        if (driver == null) {
            loadingScreen.hideLoadingScreenImmediately();
            return;
        }

        boolean hasTeam = (driver.getTeam_id() != null && team != null);
        Integer teamColorRes = null;
        if (hasTeam) {
            try {
                teamColorRes = Constants.TEAM_COLOR.get(driver.getTeam_id());
            } catch (Exception ignored) {}
        }
        if (teamColorRes == null) {
            teamColorRes = R.color.timer_gray;
        }
        int resolvedColor = ContextCompat.getColor(this, teamColorRes);

        UIUtils.singleSetTextViewText(
                (driver.getGivenName() + " " + driver.getFamilyName()).toUpperCase(Locale.ROOT),
                findViewById(R.id.topAppBarTitle)
        );

        toolbar.setBackgroundColor(resolvedColor);
        appBarLayout.setBackgroundColor(resolvedColor);
        tabLayout.setSelectedTabIndicatorColor(resolvedColor);
        tabLayout.setTabTextColors(resolvedColor, resolvedColor);

        DriverBioPagerAdapter pagerAdapter = new DriverBioPagerAdapter(this, driverId, driver, nation, team);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText(R.string.tab_bio);
                    break;
                case 1:
                    tab.setText(R.string.tab_history);
                    break;
                case 2:
                    tab.setText(R.string.tab_charts);
                    break;
            }
        }).attach();

        loadingScreen.hideLoadingScreenImmediately();
    }
}