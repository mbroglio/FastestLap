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
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.adapter.f1.ConstructorBioPagerAdapter;
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
import com.the_coffe_coders.fastestlap.util.ui.NavigationUtils;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class ConstructorBioActivity extends AppCompatActivity {

    private static final String TAG = "ConstructorBioActivity";
    private LoadingScreen loadingScreen;
    private MaterialToolbar toolbar;
    private AppBarLayout appBarLayout;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private String teamId;

    private DriverViewModel driverViewModel;
    private NationViewModel nationViewModel;
    private ConstructorViewModel constructorViewModel;
    private UserViewModel userViewModel;
    private FavoriteBioHandler favoriteBioHandler;

    private Constructor constructor;
    private Nation nation;
    private Driver driverOne;
    private Driver driverTwo;

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
                        teamId,
                        Constants.SHARED_PREFERENCES_FAVORITE_TEAM,
                        favoriteItem,
                        favId -> {
                            String idToken = userViewModel.getLoggedUser() != null ? userViewModel.getLoggedUser().getIdToken() : null;
                            if (idToken != null) {
                                userViewModel.saveUserConstructorPreferences(favId, idToken);
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

        teamId = getIntent().getStringExtra("TEAM_ID");
        Log.i(TAG, "Team ID: " + teamId);

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
        data.observe(this, result -> {
            if (result instanceof Result.Loading) {
                return;
            }
            if (result.isSuccess()) {
                constructor = ((Result.ConstructorSuccess) result).getData();

                if (constructor == null) {
                    NavigationUtils.navigateToHomePage(this);
                } else {
                    Log.i(TAG, "Constructor loaded: " + constructor);

                    favoriteBioHandler.updateFavoriteIcon(toolbar.getMenu(), R.id.favourite_icon_outline, teamId, Constants.SHARED_PREFERENCES_FAVORITE_TEAM);

                    fetchAllTeamDependencies(constructor);
                }
            } else {
                Log.e(TAG, "Error fetching constructor: " + result.getError());
                loadingScreen.hideLoadingScreenImmediately();
            }
        });
    }

    private void fetchAllTeamDependencies(Constructor team) {
        AtomicInteger pendingCount = new AtomicInteger(3);

        Runnable checkComplete = () -> {
            if (pendingCount.decrementAndGet() == 0) {
                setupConstructorBio(constructor, nation, driverOne, driverTwo);
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

    private void setupConstructorBio(Constructor team, Nation nation, Driver driverOne, Driver driverTwo) {
        if (team == null) {
            loadingScreen.hideLoadingScreenImmediately();
            return;
        }

        Integer teamColorRes = null;
        try {
            if (team.getConstructorId() != null) {
                teamColorRes = Constants.TEAM_COLOR.get(team.getConstructorId());
            } else if (teamId != null) {
                teamColorRes = Constants.TEAM_COLOR.get(teamId);
            }
        } catch (Exception ignored) {}
        if (teamColorRes == null) {
            teamColorRes = R.color.timer_gray;
        }
        int resolvedColor = ContextCompat.getColor(this, teamColorRes);

        UIUtils.singleSetTextViewText(team.getName().toUpperCase(Locale.ROOT), findViewById(R.id.topAppBarTitle));

        toolbar.setBackgroundColor(resolvedColor);
        appBarLayout.setBackgroundColor(resolvedColor);
        tabLayout.setSelectedTabIndicatorColor(resolvedColor);
        tabLayout.setTabTextColors(
                resolvedColor,
                resolvedColor
        );

        ConstructorBioPagerAdapter pagerAdapter = new ConstructorBioPagerAdapter(this, teamId, team, nation, driverOne, driverTwo);
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