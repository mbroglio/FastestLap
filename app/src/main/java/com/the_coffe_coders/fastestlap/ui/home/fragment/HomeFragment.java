package com.the_coffe_coders.fastestlap.ui.home.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.f1.standing.ConstructorStandings;
import com.the_coffe_coders.fastestlap.domain.f1.standing.DriverStandings;
import com.the_coffe_coders.fastestlap.repository.user.IUserRepository;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.ConstructorViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.ConstructorViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.DriverViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.DriverViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.NationViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.NationViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.TrackViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.TrackViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.RaceResultViewModel;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.RaceResultViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.WeeklyRaceViewModel;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.WeeklyRaceViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.home.handler.FavoriteConstructorHandler;
import com.the_coffe_coders.fastestlap.ui.home.handler.FavoriteDriverHandler;
import com.the_coffe_coders.fastestlap.ui.home.handler.LastRaceHandler;
import com.the_coffe_coders.fastestlap.ui.home.handler.NextRaceHandler;
import com.the_coffe_coders.fastestlap.ui.home.viewmodel.HomeViewModel;
import com.the_coffe_coders.fastestlap.ui.home.viewmodel.HomeViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.welcome.viewmodel.UserViewModel;
import com.the_coffe_coders.fastestlap.ui.welcome.viewmodel.UserViewModelFactory;
import com.the_coffe_coders.fastestlap.util.NetworkUtils;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;
import com.the_coffe_coders.fastestlap.util.SharedPreferencesUtils;
import com.the_coffe_coders.fastestlap.util.ui.LoadingScreen;



public class HomeFragment extends Fragment {
    private static final String TAG = HomeFragment.class.getSimpleName();
    private SharedPreferencesUtils sharedPreferencesUtils;
    private LoadingScreen loadingScreen;
    private HomeViewModel homeViewModel;
    private ConstructorViewModel constructorViewModel;
    private DriverViewModel driverViewModel;
    private TrackViewModel trackViewModel;
    private NationViewModel nationViewModel;
    private RaceResultViewModel raceResultViewModel;
    private WeeklyRaceViewModel weeklyRaceViewModel;
    private UserViewModel userViewModel;
    private View view;
    private NetworkUtils networkLiveData;
    private Boolean previousNetworkState = null;
    private boolean isSettingUp = false;

    // Track individual card loading states
    private boolean lastRaceCardLoaded = false;
    private boolean nextSessionCardLoaded = false;
    private boolean driverCardLoaded = false;
    private boolean constructorCardLoaded = false;

    // Cache standings data to avoid duplicate fetches
    private DriverStandings cachedDriverStandings = null;
    private ConstructorStandings cachedConstructorStandings = null;

    // Content handlers
    private LastRaceHandler lastRaceHandler;
    private NextRaceHandler nextRaceHandler;
    private FavoriteDriverHandler favoriteDriverHandler;
    private FavoriteConstructorHandler favoriteConstructorHandler;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_home, container, false);

        homeViewModel = new ViewModelProvider(this, new HomeViewModelFactory(requireActivity().getApplication())).get(HomeViewModel.class);
        networkLiveData = new NetworkUtils(requireContext());

        // Setup the fragment immediately. This ensures cached data is shown if offline.
        setupFragment(view);

        // Observe network changes to refresh data upon reconnection.
        networkLiveData.observe(getViewLifecycleOwner(), isConnected -> {
            if (previousNetworkState != null && !previousNetworkState && isConnected) {
                // If we transitioned from offline to online, refresh the data.
                Log.d(TAG, "Network connection restored. Refreshing fragment.");
                setupFragment(view);
            }
            previousNetworkState = isConnected;
        });

        return view;
    }

    private void setupFragment(View view) {
        // Prevent multiple simultaneous setups
        if (isSettingUp) {
            Log.d(TAG, "Setup already in progress, skipping...");
            return;
        }
        isSettingUp = true;

        // Reset loading states
        lastRaceCardLoaded = false;
        nextSessionCardLoaded = false;
        driverCardLoaded = false;
        constructorCardLoaded = false;

        // Clear cached standings for fresh data
        cachedDriverStandings = null;
        cachedConstructorStandings = null;


        // Remove all existing observers to prevent duplicates
        if (homeViewModel != null) {
            homeViewModel.getDriverStandingsLiveData(requireActivity().getApplication()).removeObservers(getViewLifecycleOwner());
            homeViewModel.getConstructorStandingsLiveData(requireActivity().getApplication()).removeObservers(getViewLifecycleOwner());
        }

        initializeViewModels();
        setupLoadingScreen(view);
        setupHandlers();
        setupUI(view);

        isSettingUp = false;
    }

    private void initializeViewModels() {
        homeViewModel = new ViewModelProvider(this, new HomeViewModelFactory(requireActivity().getApplication())).get(HomeViewModel.class);
        constructorViewModel = new ViewModelProvider(this, new ConstructorViewModelFactory(requireActivity().getApplication())).get(ConstructorViewModel.class);
        driverViewModel = new ViewModelProvider(this, new DriverViewModelFactory(requireActivity().getApplication())).get(DriverViewModel.class);
        trackViewModel = new ViewModelProvider(this, new TrackViewModelFactory(requireActivity().getApplication())).get(TrackViewModel.class);
        nationViewModel = new ViewModelProvider(this, new NationViewModelFactory(requireActivity().getApplication())).get(NationViewModel.class);
        sharedPreferencesUtils = new SharedPreferencesUtils(this.getContext());
        raceResultViewModel = new ViewModelProvider(this, new RaceResultViewModelFactory(requireActivity().getApplication(), getContext())).get(RaceResultViewModel.class);
        weeklyRaceViewModel = new ViewModelProvider(this, new WeeklyRaceViewModelFactory(requireActivity().getApplication(), getContext())).get(WeeklyRaceViewModel.class);
        IUserRepository userRepository = ServiceLocator.getInstance().getUserRepository(requireActivity().getApplication());
        userViewModel = new ViewModelProvider(getViewModelStore(), new UserViewModelFactory(userRepository)).get(UserViewModel.class);
    }

    private void setupLoadingScreen(View view) {
        loadingScreen = new LoadingScreen(view, getContext(), null, view.findViewById(R.id.home_refresh_layout));
        loadingScreen.showLoadingScreen(false);
    }

    private void setupHandlers() {
        // Initialize handlers
        lastRaceHandler = new LastRaceHandler(
            this, view, weeklyRaceViewModel, trackViewModel,
            raceResultViewModel, networkLiveData, this::markCardLoaded
        );

        nextRaceHandler = new NextRaceHandler(
            this, view, weeklyRaceViewModel, trackViewModel, nationViewModel,
            homeViewModel, driverViewModel, networkLiveData, this::markCardLoaded
        );

        favoriteDriverHandler = new FavoriteDriverHandler(
            this, view, homeViewModel, driverViewModel, nationViewModel,
            userViewModel, networkLiveData, sharedPreferencesUtils, this::markCardLoaded
        );

        favoriteConstructorHandler = new FavoriteConstructorHandler(
            this, view, homeViewModel, constructorViewModel, nationViewModel,
            userViewModel, networkLiveData, sharedPreferencesUtils, this::markCardLoaded
        );
    }

    private void setupUI(View view) {
        // Always start with the race cards (these are independent of user preferences)
        setRefreshLayout(view);

        // Setup next session card and get the round
        nextRaceHandler.setupNextSessionCard(round -> {
            // Once we have the next race round, setup the last race card
            lastRaceHandler.setNextRaceRound(round);
            lastRaceHandler.setupLastRaceCard();
        });

        // Pre-fetch standings data once to be used by both cards
        prefetchStandingsData();

        // Now handle the favorite cards based on login status
        if (networkLiveData.isConnected()) {
            if (userViewModel.getLoggedUser() != null) {
                // Start loading favorite cards immediately from SharedPreferences
                // Don't wait for getUserPreferences API call - it's just a sync operation
                favoriteDriverHandler.setupFavoriteDriverCard();
                favoriteConstructorHandler.setupFavoriteConstructorCard();

                // Sync preferences in background (for future loads)
                userViewModel.getUserPreferences(userViewModel.getLoggedUser().getIdToken()).observe(getViewLifecycleOwner(), result -> {
                    if (result != null) {
                        if (result.isSuccess()) {
                            Log.d(TAG, "User preferences synced successfully");
                        } else {
                            Log.e(TAG, "Failed to sync user preferences: " + result.getError());
                        }
                    }
                });
            } else {
                // Not logged in - handlers will show selection prompts automatically
                favoriteDriverHandler.setupFavoriteDriverCard();
                favoriteConstructorHandler.setupFavoriteConstructorCard();
            }
        } else {
            Log.e(TAG, "No internet connection - loading from cache");
            // Load from cache if possible
            favoriteDriverHandler.setupFavoriteDriverCard();
            favoriteConstructorHandler.setupFavoriteConstructorCard();
        }
    }

    private void prefetchStandingsData() {
        // Pre-fetch driver standings - only if not already cached
        if (cachedDriverStandings == null) {
            MutableLiveData<Result> driverStandingsLiveData = homeViewModel.getDriverStandingsLiveData(requireActivity().getApplication());
            driverStandingsLiveData.observe(getViewLifecycleOwner(), result -> {
                if (result instanceof Result.Loading) {
                    return;
                }
                if (result instanceof Result.DriverStandingsSuccess) {
                    cachedDriverStandings = ((Result.DriverStandingsSuccess) result).getData();
                    favoriteDriverHandler.setCachedDriverStandings(cachedDriverStandings);
                    Log.d(TAG, "Driver standings cached from prefetch");
                } else {
                    Log.d(TAG, "Driver standings prefetch failed or no data available");
                }
            });
        }

        // Pre-fetch constructor standings - only if not already cached
        if (cachedConstructorStandings == null) {
            MutableLiveData<Result> constructorStandingsLiveData = homeViewModel.getConstructorStandingsLiveData(requireActivity().getApplication());
            constructorStandingsLiveData.observe(getViewLifecycleOwner(), result -> {
                if (result instanceof Result.Loading) {
                    return;
                }
                if (result instanceof Result.ConstructorStandingsSuccess) {
                    cachedConstructorStandings = ((Result.ConstructorStandingsSuccess) result).getData();
                    favoriteConstructorHandler.setCachedConstructorStandings(cachedConstructorStandings);
                    Log.d(TAG, "Constructor standings cached from prefetch");
                } else {
                    Log.d(TAG, "Constructor standings prefetch failed or no data available");
                }
            });
        }
    }

    private void setRefreshLayout(View view) {
        SwipeRefreshLayout homeSwipeRefreshLayout = view.findViewById(R.id.home_refresh_layout);
        homeSwipeRefreshLayout.setOnRefreshListener(() -> {
            setupFragment(view);
            homeSwipeRefreshLayout.setRefreshing(false);
        });
    }

    private synchronized void markCardLoaded(String cardName) {
        switch (cardName) {
            case "lastRace":
                lastRaceCardLoaded = true;
                break;
            case "nextSession":
                nextSessionCardLoaded = true;
                break;
            case "driver":
                driverCardLoaded = true;
                break;
            case "constructor":
                constructorCardLoaded = true;
                break;
        }

        Log.d(TAG, "Card loaded: " + cardName + " | LastRace: " + lastRaceCardLoaded +
                " | NextSession: " + nextSessionCardLoaded +
                " | Driver: " + driverCardLoaded +
                " | Constructor: " + constructorCardLoaded);

        // Hide loading screen early - as soon as the race cards are ready
        // User can see something immediately, preference cards can load in background
        if (lastRaceCardLoaded && nextSessionCardLoaded) {
            Log.d(TAG, "Critical cards loaded, hiding loading screen early");
            loadingScreen.hideLoadingScreen();
        }
    }
}
