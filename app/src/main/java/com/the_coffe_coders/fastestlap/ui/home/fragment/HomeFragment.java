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
    // isInitialized: persists for the fragment's full lifetime.
    // Prevents redundant re-initialization when the user navigates back to this fragment
    // (which triggers a new onCreateView call and would otherwise re-fetch all data).
    private boolean isInitialized = false;
    // isSettingUp: guards against concurrent setup calls (e.g. network reconnect racing with
    // a manual refresh). Cleared only after all 4 cards have finished loading.
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

        networkLiveData = new NetworkUtils(requireContext());

        // Initialize ViewModels once per fragment instance (ViewModelProvider is idempotent
        // but calling initializeViewModels on every setupFragment adds unnecessary overhead).
        initializeViewModels();

        if (!isInitialized) {
            // First time setup: load all data from network / cache.
            setupFragment(view);
        } else {
            // Fragment is returning from back-stack: data is already loaded in ViewModels / cache.
            // Just re-attach the loading screen and handlers without triggering new network calls.
            Log.d(TAG, "Fragment returning from back-stack, skipping re-initialization.");

            // Reset flags so markCardLoaded can hide the loading screen again
            lastRaceCardLoaded = false;
            nextSessionCardLoaded = false;
            driverCardLoaded = false;
            constructorCardLoaded = false;
            isSettingUp = true;

            setupLoadingScreen(view);
            setupHandlers();
            setupUI(view);
        }

        // Observe network changes to refresh data upon reconnection.
        networkLiveData.observe(getViewLifecycleOwner(), isConnected -> {
            if (previousNetworkState != null && !previousNetworkState && isConnected) {
                // Transitioned from offline to online: force a full refresh.
                Log.d(TAG, "Network connection restored. Refreshing fragment.");
                isInitialized = false; // allow setupFragment to run again
                setupFragment(view);
            }
            previousNetworkState = isConnected;
        });

        return view;
    }

    private void setupFragment(View view) {
        // Prevent concurrent setups (e.g. swipe-refresh racing with a network-restore event).
        if (isSettingUp) {
            Log.d(TAG, "Setup already in progress, skipping...");
            return;
        }
        isSettingUp = true;
        isInitialized = true;

        // Reset individual card loading flags so markCardLoaded works correctly.
        lastRaceCardLoaded = false;
        nextSessionCardLoaded = false;
        driverCardLoaded = false;
        constructorCardLoaded = false;

        // Clear cached standings so fresh data is fetched on a full refresh.
        cachedDriverStandings = null;
        cachedConstructorStandings = null;

        // Remove existing LiveData observers to prevent duplicates when setupFragment
        // is called a second time (e.g. swipe-refresh or network-restore).
        if (homeViewModel != null) {
            homeViewModel.getDriverStandingsLiveData(requireActivity().getApplication()).removeObservers(getViewLifecycleOwner());
            homeViewModel.getConstructorStandingsLiveData(requireActivity().getApplication()).removeObservers(getViewLifecycleOwner());
        }

        // NOTE: initializeViewModels() is NOT called here anymore — it is called once
        // from onCreateView to avoid re-creating ViewModelProviders on every setup.
        setupLoadingScreen(view);
        setupHandlers();
        setupUI(view);

        // isSettingUp is intentionally NOT cleared here.
        // It is cleared in markCardLoaded() once all 4 cards have finished loading,
        // which prevents a concurrent network-restore or swipe-refresh from interrupting
        // an in-progress async load.
    }

    private void initializeViewModels() {
        // ViewModelProvider.get() is idempotent — it returns the same ViewModel instance
        // if one already exists for this scope. Calling this multiple times is safe but
        // unnecessary; we call it once from onCreateView.
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

        // If standings data was already fetched in a previous setup (e.g. returning from
        // back-stack), pass it directly to the new handlers so ranking/points are displayed
        // immediately without waiting for the LiveData to re-emit.
        if (cachedDriverStandings != null) {
            favoriteDriverHandler.setCachedDriverStandings(cachedDriverStandings);
        }
        if (cachedConstructorStandings != null) {
            favoriteConstructorHandler.setCachedConstructorStandings(cachedConstructorStandings);
        }
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
                // Check if preferences are already available locally
                String localDriverId = sharedPreferencesUtils.readStringData(
                        com.the_coffe_coders.fastestlap.util.Constants.SHARED_PREFERENCES_FILENAME,
                        com.the_coffe_coders.fastestlap.util.Constants.SHARED_PREFERENCES_FAVORITE_DRIVER);
                boolean prefsAvailable = localDriverId != null && !localDriverId.isEmpty() && !localDriverId.equals("null");

                if (prefsAvailable) {
                    // Preferences already cached locally — start cards immediately
                    favoriteDriverHandler.setupFavoriteDriverCard();
                    favoriteConstructorHandler.setupFavoriteConstructorCard();

                    // Sync in background (keep remote in sync for future sessions)
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
                    // First login: preferences not yet in SharedPreferences.
                    // Fetch from remote first, then build the cards once the data is available.
                    Log.d(TAG, "Local preferences empty — fetching from remote before building cards");
                    userViewModel.getUserPreferences(userViewModel.getLoggedUser().getIdToken()).observe(getViewLifecycleOwner(), result -> {
                        if (result != null) {
                            if (result.isSuccess()) {
                                Log.d(TAG, "User preferences synced successfully");
                            } else {
                                Log.e(TAG, "Failed to sync user preferences: " + result.getError());
                            }
                            // Build cards regardless of success/failure so the UI isn't blocked
                            favoriteDriverHandler.setupFavoriteDriverCard();
                            favoriteConstructorHandler.setupFavoriteConstructorCard();
                        }
                    });
                }
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

        // Hide loading screen only when all 4 cards are fully ready.
        if (lastRaceCardLoaded && nextSessionCardLoaded && driverCardLoaded && constructorCardLoaded) {
            Log.d(TAG, "All cards loaded — hiding loading screen and setup complete.");
            loadingScreen.hideLoadingScreen();
            isSettingUp = false;
        }
    }
}
