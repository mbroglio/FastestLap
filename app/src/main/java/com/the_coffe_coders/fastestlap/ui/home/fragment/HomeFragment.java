package com.the_coffe_coders.fastestlap.ui.home.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.card.MaterialCardView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.f1.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.f1.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Practice;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Session;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.domain.f1.result.RaceResult;
import com.the_coffe_coders.fastestlap.domain.f1.standing.ConstructorStandings;
import com.the_coffe_coders.fastestlap.domain.f1.standing.ConstructorStandingsElement;
import com.the_coffe_coders.fastestlap.domain.f1.standing.DriverStandings;
import com.the_coffe_coders.fastestlap.domain.f1.standing.DriverStandingsElement;
import com.the_coffe_coders.fastestlap.domain.f1.track.Track;
import com.the_coffe_coders.fastestlap.domain.nation.Nation;
import com.the_coffe_coders.fastestlap.repository.user.IUserRepository;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.ConstructorViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.ConstructorViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.DriverViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.DriverViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.NationViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.NationViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.TrackViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.TrackViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.event.EventActivity;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.RaceResultViewModel;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.RaceResultViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.WeeklyRaceViewModel;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.WeeklyRaceViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.home.viewmodel.HomeViewModel;
import com.the_coffe_coders.fastestlap.ui.home.viewmodel.HomeViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.standing.ConstructorsStandingActivity;
import com.the_coffe_coders.fastestlap.ui.standing.DriversStandingActivity;
import com.the_coffe_coders.fastestlap.ui.welcome.viewmodel.UserViewModel;
import com.the_coffe_coders.fastestlap.ui.welcome.viewmodel.UserViewModelFactory;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.NetworkUtils;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;
import com.the_coffe_coders.fastestlap.util.SharedPreferencesUtils;
import com.the_coffe_coders.fastestlap.util.ui.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ui.NavigationUtils;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

import java.util.List;
import java.util.Objects;


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
    private boolean hasReloaded = false;
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

    private String nextRaceRound;

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

        Intent intent = requireActivity().getIntent();
        if (intent != null) {
            if (intent.hasExtra("RELOADED")) {
                if (Objects.equals(intent.getStringExtra("RELOADED"), "true")) {
                    hasReloaded = true;
                }
            }
        }

        // Remove all existing observers to prevent duplicates
        if (homeViewModel != null) {
            homeViewModel.getDriverStandingsLiveData(requireActivity().getApplication()).removeObservers(getViewLifecycleOwner());
            homeViewModel.getConstructorStandingsLiveData(requireActivity().getApplication()).removeObservers(getViewLifecycleOwner());
        }

        initializeViewModels();
        setupLoadingScreen(view);
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

    private void setupUI(View view) {
        // Always start with the race cards (these are independent of user preferences)
        setRefreshLayout(view);
        setNextSessionCard(view);
        setLastRaceCard(view);

        // Pre-fetch standings data once to be used by both cards
        prefetchStandingsData();

        // Now handle the favorite cards based on login status
        if (networkLiveData.isConnected()) {
            if (userViewModel.getLoggedUser() != null) {
                // Start loading favorite cards immediately from SharedPreferences
                // Don't wait for getUserPreferences API call - it's just a sync operation
                setFavouriteDriverCard(view);
                setFavouriteConstructorCard(view);

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
                // Not logged in - show selection prompts
                showSelectFavouriteDriver(view);
                showSelectFavouriteConstructor(view);
            }
        } else {
            Log.e(TAG, "No internet connection - loading from cache");
            // Load from cache if possible
            setFavouriteDriverCard(view);
            setFavouriteConstructorCard(view);
        }
    }

    private void prefetchStandingsData() {
        // Pre-fetch driver standings
        if (cachedDriverStandings == null) {
            MutableLiveData<Result> driverStandingsLiveData = homeViewModel.getDriverStandingsLiveData(requireActivity().getApplication());
            driverStandingsLiveData.observe(getViewLifecycleOwner(), result -> {
                if (result instanceof Result.Loading) {
                    return;
                }
                if (result.isSuccess()) {
                    cachedDriverStandings = ((Result.DriverStandingsSuccess) result).getData();
                    Log.d(TAG, "Driver standings cached");
                }
            });
        }

        // Pre-fetch constructor standings
        if (cachedConstructorStandings == null) {
            MutableLiveData<Result> constructorStandingsLiveData = homeViewModel.getConstructorStandingsLiveData(requireActivity().getApplication());
            constructorStandingsLiveData.observe(getViewLifecycleOwner(), result -> {
                if (result instanceof Result.Loading) {
                    return;
                }
                if (result.isSuccess()) {
                    cachedConstructorStandings = ((Result.ConstructorStandingsSuccess) result).getData();
                    Log.d(TAG, "Constructor standings cached");
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

    private void setNextSessionCard(View view) {
        LiveData<Result> nextRaceLiveData = weeklyRaceViewModel.getNextRaceLiveData();
        try {
            nextRaceLiveData.observe(getViewLifecycleOwner(), result -> {

                try {
                    if (result instanceof Result.Loading) {
                        return;
                    }
                    if (result.isSuccess()) {
                        WeeklyRace nextRace = ((Result.NextRaceSuccess) result).getData();
                        Log.i(TAG, "Next race: " + nextRace.getRound());
                        nextRaceRound = nextRace.getRound();
                        processNextRace(view, nextRace);
                    } else {
                        throw new Exception("Failed to fetch next race: " + result.getError());
                    }
                } catch (Exception e) {
                    if (networkLiveData.isConnected()) {
                        Log.e(TAG, "Error in setNextSessionCard: " + e.getMessage());
                        setSeasonEnded(view);
                    } else {
                        Log.e(TAG, "Error in setNextSessionCard: No internet connection");
                        setUpdating(view);
                    }
                }
            });
        } catch (Exception e) {
            if (networkLiveData.isConnected()) {
                Log.e(TAG, "Error in setNextSessionCard: " + e.getMessage());
                setSeasonEnded(view);
            } else {
                Log.e(TAG, "Error in setNextSessionCard: No internet connection");
                setUpdating(view);
            }
        }

        ImageView iconImageView = view.findViewById(R.id.live_icon);
        Animation pulseAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.pulse_static);
        iconImageView.startAnimation(pulseAnimation);
    }

    private void processNextRace(View view, WeeklyRace nextRace) {
        try {
            if (nextRace == null) throw new Exception("Next race is null");
            if (!nextRace.getSeason().equals(ServiceLocator.currentYear))
                throw new Exception("Season mismatch");
            MutableLiveData<Result> trackData = trackViewModel.getTrack(nextRace.getTrack().getTrackId());
            trackData.observe(getViewLifecycleOwner(), trackResult -> {
                try {
                    if (trackResult instanceof Result.Loading) {
                        return;
                    }
                    if (trackResult.isSuccess()) {
                        Track track = ((Result.TrackSuccess) trackResult).getData();
                        nextRace.setTrack(track);
                        fetchNationForNextRace(view, nextRace, track);
                    } else {
                        throw new Exception("Failed to fetch track data: " + trackResult.getError());
                    }
                } catch (Exception e) {
                    if (networkLiveData.isConnected()) {
                        Log.e(TAG, "Error in processNextRace: " + e.getMessage());
                        setSeasonEnded(view);
                    } else {
                        Log.e(TAG, "Error in processNextRace: No internet connection");
                        setUpdating(view);
                    }
                }
            });
        } catch (Exception e) {
            if (networkLiveData.isConnected()) {
                Log.e(TAG, "Error in processNextRace: " + e.getMessage());
                setSeasonEnded(view);
            } else {
                if (Objects.equals(e.getMessage(), "Season mismatch")) setSeasonEnded(view);
                Log.e(TAG, "Error in processNextRace: No internet connection");
                setUpdating(view);
            }
        }
    }

    private void fetchNationForNextRace(View view, WeeklyRace nextRace, Track track) {
        try {
            MutableLiveData<Result> nationData = nationViewModel.getNation(track.getCountry());
            nationData.observe(getViewLifecycleOwner(), nationResult -> {
                try {
                    if (nationResult instanceof Result.Loading) {
                        return;
                    }
                    if (nationResult.isSuccess()) {
                        Nation nation = ((Result.NationSuccess) nationResult).getData();
                        setNextRaceCard(view, nextRace, nation);
                    } else {
                        throw new Exception("Failed to fetch nation data: " + nationResult.getError());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error fetching nation: " + e.getMessage());
                    setSeasonEnded(view);
                }
            });
        } catch (RuntimeException e) {
            Log.e(TAG, "Error fetching nation: " + e.getMessage());
            setNextRaceCard(view, nextRace, null);
        }

    }

    private void setNextRaceCard(View view, WeeklyRace nextRace, Nation nation) {
        try {
            UIUtils.singleSetTextViewText(nextRace.getRaceName(), view.findViewById(R.id.home_next_gp_name));

            String nationFlagUrl = null;
            if (nation != null) {
                nationFlagUrl = nation.getNation_flag_url();
            }

            UIUtils.loadImageWithGlide(requireContext(), nationFlagUrl, view.findViewById(R.id.home_next_gp_flag), () -> {
                try {
                    setNextRaceCardFinalStep(nextRace, view);
                } catch (Exception e) {
                    setSeasonEnded(view);
                }
            });

        } catch (Exception e) {
            Log.i(TAG, "connected: " + networkLiveData.isConnected());
            if (networkLiveData.isConnected()) {
                setSeasonEnded(view);
                Log.e(TAG, "Error in setNextRaceCard: " + e.getMessage());
            } else {
                Log.e(TAG, "Error in setNextRaceCard: No internet connection");
                loadPendingResultsLayout(view);
            }

        }
    }

    private void setNextRaceCardFinalStep(WeeklyRace nextRace, View view) throws Exception {
        if (!nextRace.getSeason().equals(ServiceLocator.currentYear)) {
            throw new Exception("Season mismatch");
        }

        List<Session> sessions = nextRace.getSessions();
        Session nextEvent = nextRace.findNextEvent(sessions);
        if (nextEvent != null) {
            startCountdown(view, nextEvent.getStartDateTime());
            updateSessionType(view, nextEvent);
        } else {
            setUpdating(view);
        }

        FrameLayout nextSessionCard = view.findViewById(R.id.timer_card_countdown);
        nextSessionCard.setOnClickListener(v -> startActivity(new Intent(getActivity(), EventActivity.class).putExtra("CIRCUIT_ID", nextRace.getTrack().getTrackId())));

    }

    private void setLastRaceCard(View view) {
        LiveData<Result> lastRace = weeklyRaceViewModel.getLastRace();
        lastRace.observe(getViewLifecycleOwner(), result -> {
            try {
                if (result instanceof Result.Loading) {
                    return;
                }
                if (result.isSuccess()) {
                    WeeklyRace raceResult = ((Result.NextRaceSuccess) result).getData();
                    Log.i(TAG, "Last Race: " + raceResult);

                    if (raceResult.getRound().equals(nextRaceRound)) {
                        // Next race and last race are the same - we're at the beginning of the season
                        setSeasonBeginning(view);
                        return;
                    }

                    showPodium(view, raceResult);
                } else {
                    throw new Exception("Failed to fetch last race: " + result.getError());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in setLastRaceCard: " + e.getMessage());
                loadPendingResultsLayout(view);
            }
        });
    }

    private void setSeasonBeginning(View view) {
        Log.i(TAG, "Season beginning detected - next race equals last race");
        showLastRaceNotFound(view);
        markCardLoaded("lastRace");
    }

    private void showPodium(View view, WeeklyRace race) {
        try {
            String circuitId = race.getTrack().getTrackId();
            MutableLiveData<Result> trackData = trackViewModel.getTrack(circuitId);
            trackData.observe(getViewLifecycleOwner(), trackResult -> {
                try {
                    if (trackResult instanceof Result.Loading) {
                        return;
                    }
                    if (trackResult.isSuccess()) {
                        Track track = ((Result.TrackSuccess) trackResult).getData();
                        updateLastRaceUI(view, race, track);
                    } else {
                        throw new Exception("Failed to fetch track data: " + trackResult.getError());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error loading track: " + e.getMessage());
                    loadPendingResultsLayout(view);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in showPodium: " + e.getMessage());
            loadPendingResultsLayout(view);
        }
    }

    private void updateLastRaceUI(View view, WeeklyRace race, Track track) {
        try {
            UIUtils.singleSetTextViewText(race.getRaceName(), view.findViewById(R.id.last_race_name));
            UIUtils.loadImageWithGlide(requireContext(), track.getTrack_minimal_layout_url(), view.findViewById(R.id.last_race_track_outline), () -> updateLastRaceUIFinalStep(race, view));
        } catch (Exception e) {
            Log.e(TAG, "Error updating last race UI: " + e.getMessage());
            loadPendingResultsLayout(view);
        }
    }

    private void updateLastRaceUIFinalStep(WeeklyRace race, View view) {
        LocalDateTime dateTime = race.getDateTime();

        UIUtils.multipleSetTextViewText(new String[]{String.valueOf(dateTime.getDayOfMonth()), requireContext().getString(R.string.round, race.getRound())}, new TextView[]{view.findViewById(R.id.last_race_date), view.findViewById(R.id.last_race_round)});

        UIUtils.translateMonth(dateTime.getMonth().toString().substring(0, 3).toUpperCase(), view.findViewById(R.id.last_race_month), true);

        MutableLiveData<Result> raceResultData = raceResultViewModel.getRaceResults(race.getRound());
        raceResultData.observe(getViewLifecycleOwner(), result -> {
            try {
                if (result instanceof Result.Loading) {
                    return;
                }
                if (result.isSuccess()) {
                    List<RaceResult> raceResults = ((Result.RaceResultsSuccess) result).getData().getResults();
                    setDriverNames(view, raceResults);
                } else {
                    throw new Exception("Failed to fetch race results: " + result.getError());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error setting driver names: " + e.getMessage());
            }
        });

        MaterialCardView resultCard = view.findViewById(R.id.past_event_result);
        resultCard.setOnClickListener(v -> startActivity(new Intent(getActivity(), EventActivity.class).putExtra("CIRCUIT_ID", race.getTrack().getTrackId())));
    }

    private void setDriverNames(View view, List<RaceResult> raceResults) {
        try {
            for (int i = 0; i < Math.min(3, raceResults.size()); i++) {
                UIUtils.singleSetTextViewText(raceResults.get(i).getDriver().getFullName(), view.findViewById(Constants.LAST_RACE_DRIVER_NAME.get(i)));
            }
            markCardLoaded("lastRace");
        } catch (Exception e) {
            Log.e(TAG, "Error setting driver names: " + e.getMessage());
            markCardLoaded("lastRace");
        }
    }

    private void loadPendingResultsLayout(View view) {
        view.findViewById(R.id.pending_last_race_results).setVisibility(View.VISIBLE);
        view.findViewById(R.id.last_race_results).setVisibility(View.GONE);
        markCardLoaded("lastRace");
    }

    private void updateSessionType(View view, Session nextEvent) {
        String sessionId = nextEvent.getClass().getSimpleName().equals("Practice") ? "Practice" + ((Practice) nextEvent).getNumber() : nextEvent.getClass().getSimpleName();
        TextView sessionTypeView = view.findViewById(R.id.next_session_type);

        UIUtils.translateSessionType(requireContext(), sessionTypeView, sessionId);
    }

    private void startCountdown(View view, LocalDateTime eventDate) {
        LinearLayout liveIconLayout = view.findViewById(R.id.timer_live_layout);
        liveIconLayout.setVisibility(View.GONE);
        long millisUntilStart = ZonedDateTime.of(eventDate, ZoneId.systemDefault()).toInstant().toEpochMilli() - System.currentTimeMillis();
        if (millisUntilStart <= 0) {
            liveIconLayout.setVisibility(View.VISIBLE);
            markCardLoaded("nextSession");
            return;
        }

        new CountDownTimer(millisUntilStart, 1000) {
            final TextView days = view.findViewById(R.id.next_days_counter);
            final TextView hours = view.findViewById(R.id.next_hours_counter);
            final TextView minutes = view.findViewById(R.id.next_minutes_counter);
            final TextView seconds = view.findViewById(R.id.next_seconds_counter);

            @Override
            public void onTick(long millisUntilFinished) {
                UIUtils.multipleSetTextViewText(new String[]{String.valueOf(millisUntilFinished / 86400000), String.valueOf((millisUntilFinished % 86400000) / 3600000), String.valueOf(((millisUntilFinished % 86400000) % 3600000) / 60000), String.valueOf((((millisUntilFinished % 86400000) % 3600000) % 60000) / 1000)},

                        new TextView[]{days, hours, minutes, seconds});
            }

            @Override
            public void onFinish() {
                UIUtils.multipleSetTextViewText(new String[]{"0", "0", "0", "0"},

                        new TextView[]{days, hours, minutes, seconds});

                liveIconLayout.setVisibility(View.VISIBLE);
            }
        }.start();
        markCardLoaded("nextSession");
    }

    private void showLastRaceNotFound(View view) {
        view.findViewById(R.id.last_race_results).setVisibility(View.GONE);
        view.findViewById(R.id.season_results).setVisibility(View.GONE);
        view.findViewById(R.id.pending_last_race_results).setVisibility(View.GONE);
        view.findViewById(R.id.last_race_not_found_layout).setVisibility(View.VISIBLE);
    }

    private void setSeasonEnded(View view) {
        view.findViewById(R.id.last_race_results).setVisibility(View.GONE);
        view.findViewById(R.id.timer).setVisibility(View.GONE);
        view.findViewById(R.id.season_ended).setVisibility(View.VISIBLE);
        view.findViewById(R.id.season_results).setVisibility(View.VISIBLE);
        view.findViewById(R.id.pending_last_race_results).setVisibility(View.GONE);

        buildFinalDriversStanding(view.findViewById(R.id.season_results));
        buildFinalTeamsStanding(view.findViewById(R.id.season_results));
        markCardLoaded("nextSession");
    }

    private void setUpdating(View view) {
        view.findViewById(R.id.timer_card_countdown).setVisibility(View.GONE);
        view.findViewById(R.id.timer_updating).setVisibility(View.VISIBLE);
        markCardLoaded("nextSession");
    }

    private void buildFinalDriversStanding(View seasonEndedCard) {
        MutableLiveData<Result> driverStandingsLiveData = homeViewModel.getDriverStandingsLiveData(requireActivity().getApplication());

        driverStandingsLiveData.observe(getViewLifecycleOwner(), result -> {
            try {
                if (result instanceof Result.Loading) {
                    return;
                }
                if (result.isSuccess()) {
                    DriverStandings driverStandings = ((Result.DriverStandingsSuccess) result).getData();
                    List<DriverStandingsElement> driversList = driverStandings.getDriverStandingsElements();
                    for (int i = 0; i < Math.min(3, driversList.size()); i++) {
                        setStandingFields(seasonEndedCard, driversList.get(i).getDriver().getDriverId(), i);
                    }
                } else {
                    throw new Exception("Failed to fetch driver standings: " + result.getError());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in buildFinalDriversStanding: " + e.getMessage());
            }
        });
    }

    private void setStandingFields(View seasonEndedCard, String driverId, int position) {
        MutableLiveData<Result> driverData = driverViewModel.getDriver(driverId);
        driverData.observe(getViewLifecycleOwner(), result -> {
            try {
                if (result instanceof Result.Loading) {
                    return;
                }
                if (result.isSuccess()) {
                    Driver driver = ((Result.DriverSuccess) result).getData();

                    UIUtils.singleSetTextViewText(driver.getFullName(), seasonEndedCard.findViewById(Constants.HOME_SEASON_DRIVER_STANDINGS_NAME_FIELD.get(position)));

                    View driverColor = seasonEndedCard.findViewById(Constants.HOME_SEASON_DRIVER_STANDINGS_COLOR_FIELD.get(position));
                    driverColor.setBackgroundResource(Constants.TEAM_COLOR.getOrDefault(driver.getTeam_id(), R.color.timer_gray));
                } else {
                    throw new Exception("Failed to fetch driver data: " + result.getError());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in setStandingFields: " + e.getMessage());
            }
        });
    }

    private void buildFinalTeamsStanding(View seasonEndedCard) {
        MutableLiveData<Result> constructorStandingsData = homeViewModel.getConstructorStandingsLiveData(requireActivity().getApplication());
        constructorStandingsData.observe(getViewLifecycleOwner(), result -> {
            try {
                if (result instanceof Result.Loading) {
                    return;
                }
                if (result.isSuccess()) {
                    ConstructorStandings standings = ((Result.ConstructorStandingsSuccess) result).getData();
                    List<ConstructorStandingsElement> constructorsList = standings.getConstructorStandings();
                    for (int i = 0; i < Math.min(3, constructorsList.size()); i++) {
                        ConstructorStandingsElement constructor = constructorsList.get(i);

                        UIUtils.singleSetTextViewText(constructor.getConstructor().getName(), seasonEndedCard.findViewById(Constants.HOME_SEASON_TEAM_STANDINGS_NAME_FIELD.get(i)));

                        View constructorColor = seasonEndedCard.findViewById(Constants.HOME_SEASON_TEAM_STANDINGS_COLOR_FIELD.get(i));
                        constructorColor.setBackgroundResource(Constants.TEAM_COLOR.getOrDefault(constructor.getConstructor().getConstructorId(), R.color.timer_gray));
                    }
                } else {
                    throw new Exception("Failed to fetch constructor standings: " + result.getError());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in buildFinalTeamsStanding: " + e.getMessage());
            }
        });
    }

    private void setFavouriteDriverCard(View view) {
        String favoriteDriverId = getFavoriteDriverId();
        if (favoriteDriverId == null || favoriteDriverId.isEmpty() || favoriteDriverId.equals("null")) {
            showSelectFavouriteDriver(view);
            return;
        }

        // Use cached data if available, otherwise fetch
        if (cachedDriverStandings != null) {
            processDriverStandings(view, favoriteDriverId, cachedDriverStandings);
        } else {
            MutableLiveData<Result> driverStandingsLiveData = homeViewModel.getDriverStandingsLiveData(requireActivity().getApplication());
            driverStandingsLiveData.observe(getViewLifecycleOwner(), result -> {
                try {
                    if (result instanceof Result.Loading) {
                        return;
                    }
                    if (result.isSuccess()) {
                        DriverStandings driverStandings = ((Result.DriverStandingsSuccess) result).getData();
                        cachedDriverStandings = driverStandings;
                        processDriverStandings(view, favoriteDriverId, driverStandings);
                    } else {
                        // Standings fetch failed - still try to create card with driver data only
                        Log.w(TAG, "Failed to fetch driver standings, attempting to show driver data only: " + result.getError());
                        processDriverStandings(view, favoriteDriverId, null);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in setFavouriteDriverCard: " + e.getMessage());
                    // Try to show driver data even if standings failed
                    processDriverStandings(view, favoriteDriverId, null);
                }
            });
        }
    }

    private void processDriverStandings(View view, String favoriteDriverId, DriverStandings driverStandings) {
        try {
            DriverStandingsElement favouriteDriver = null;
            if (driverStandings != null && driverStandings.getDriverStandingsElements() != null) {
                favouriteDriver = homeViewModel.getDriverStandingsElement(driverStandings.getDriverStandingsElements(), favoriteDriverId);
            }

            if (favouriteDriver == null) {
                // No standing found - create a card with driver data only (no position/points)
                Log.i(TAG, "No standing found for driver, fetching driver data only");
                favouriteDriver = new DriverStandingsElement();
                fetchDriverDataForCard(view, favoriteDriverId, favouriteDriver);
            } else {
                Log.i(TAG, "Fetching driver data card");
                fetchDriverDataForCard(view, favoriteDriverId, favouriteDriver);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing driver standings: " + e.getMessage());
            showDriverNotFound(view, 0);
        }
    }

    private void fetchDriverDataForCard(View view, String driverId, DriverStandingsElement favouriteDriver) {
        MutableLiveData<Result> driverData = driverViewModel.getDriver(driverId);
        driverData.observe(getViewLifecycleOwner(), driverResult -> {
            try {
                if (driverResult instanceof Result.Loading) {
                    return;
                }
                if (driverResult.isSuccess()) {
                    Driver driver = ((Result.DriverSuccess) driverResult).getData();
                    favouriteDriver.setDriver(driver);
                    Log.i(TAG, "Fetching nation data for driver card");
                    fetchNationForDriver(view, favouriteDriver);
                } else {
                    throw new Exception("Failed to fetch driver data: " + driverResult.getError());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching driver data: " + e.getMessage());
                showDriverNotFound(view, 0);
            }
        });
    }

    private void fetchNationForDriver(View view, DriverStandingsElement favouriteDriver) {
        try {
            MutableLiveData<Result> nationData = nationViewModel.getNation(favouriteDriver.getDriver().getNationality());
            nationData.observe(getViewLifecycleOwner(), nationResult -> {
                try {
                    if (nationResult instanceof Result.Loading) {
                        return;
                    }
                    if (nationResult.isSuccess()) {
                        Nation nation = ((Result.NationSuccess) nationResult).getData();
                        Log.i(TAG, "Building driver card");
                        buildDriverCard(view, favouriteDriver, nation);
                    } else {
                        throw new Exception("Failed to fetch nation data: " + nationResult.getError());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error fetching nation for driver: " + e.getMessage());
                    showDriverNotFound(view, 0);
                }
            });
        } catch (RuntimeException e) {
            Log.e(TAG, "Error fetching nation for driver: " + e.getMessage());
            buildDriverCard(view, favouriteDriver, null);
        }

    }

    private void buildDriverCard(View view, DriverStandingsElement standingElement, Nation nation) {
        if (networkLiveData.isConnected() && userViewModel.getLoggedUser() != null) {
            try {
                Driver driver = standingElement.getDriver();

                String nationFlagUrl = null;
                String nationAbbreviation = null;
                if (nation != null) {
                    nationFlagUrl = nation.getNation_flag_url();
                    nationAbbreviation = nation.getAbbreviation();
                }

                UIUtils.multipleSetTextViewText(new String[]{driver.getGivenName() + " " + driver.getFamilyName(), nationAbbreviation},
                        new TextView[]{view.findViewById(R.id.favourite_driver_name), view.findViewById(R.id.favourite_driver_nationality)});

                ImageView driverFlag = view.findViewById(R.id.favourite_driver_flag);
                ImageView driverImage = view.findViewById(R.id.favourite_driver_pic);
                driverImage.setOnClickListener(v -> NavigationUtils.navigateToBioPage(getContext(), driver.getDriverId(), 1));

                UIUtils.loadImagesInParallel(requireContext(), new String[]{nationFlagUrl, driver.getDriver_pic_url()}, new ImageView[]{driverFlag, driverImage}, () -> buildDriverCardFinalStep(standingElement, view, driver));
            } catch (Exception e) {
                Log.e(TAG, "Error building driver card: " + e.getMessage());
                showDriverNotFound(view, 0);
            }
        } else {
            Log.e(TAG, "Error building driver card: No internet connection");
            showDriverNotFound(view, 1);
        }

    }

    private void buildDriverCardFinalStep(DriverStandingsElement standingElement, View view, Driver driver) {
        if (standingElement.getPosition() != null && standingElement.getPoints() != null) {
            UIUtils.multipleSetTextViewText(new String[]{standingElement.getPosition(), standingElement.getPoints()},
                    new TextView[]{view.findViewById(R.id.favourite_driver_position), view.findViewById(R.id.favourite_driver_points)});

            MaterialCardView driverRank = view.findViewById(R.id.favourite_driver_rank);
            driverRank.setOnClickListener(v -> NavigationUtils.navigateToStandingsPage(getContext(), driver.getDriverId(), 1));
        } else {
            MaterialCardView driverRank = view.findViewById(R.id.favourite_driver_rank);
            driverRank.setClickable(false);
        }

        Log.i(TAG, "Driver card built successfully");
        showFavouriteDriverCard(view);
        markCardLoaded("driver");
    }

    private void setFavouriteConstructorCard(View view) {
        String favoriteTeamId = getFavoriteTeamId();
        if (favoriteTeamId == null || favoriteTeamId.isEmpty() || favoriteTeamId.equals("null")) {
            Log.i(TAG, "Showing select favourite constructor card");
            showSelectFavouriteConstructor(view);
            return;
        }

        // Use cached data if available, otherwise fetch
        if (cachedConstructorStandings != null) {
            processConstructorStandings(view, favoriteTeamId, cachedConstructorStandings);
        } else {
            MutableLiveData<Result> constructorStandingsData = homeViewModel.getConstructorStandingsLiveData(requireActivity().getApplication());
            constructorStandingsData.observe(getViewLifecycleOwner(), result -> {
                try {
                    if (result instanceof Result.Loading) {
                        return;
                    }
                    if (result.isSuccess()) {
                        ConstructorStandings standings = ((Result.ConstructorStandingsSuccess) result).getData();
                        cachedConstructorStandings = standings;
                        processConstructorStandings(view, favoriteTeamId, standings);
                    } else {
                        // Standings fetch failed - still try to create card with constructor data only
                        Log.w(TAG, "Failed to fetch constructor standings, attempting to show constructor data only: " + result.getError());
                        processConstructorStandings(view, favoriteTeamId, null);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in setFavouriteConstructorCard: " + e.getMessage());
                    // Try to show constructor data even if standings failed
                    processConstructorStandings(view, favoriteTeamId, null);
                }
            });
        }
    }

    private void processConstructorStandings(View view, String favoriteTeamId, ConstructorStandings standings) {
        try {
            ConstructorStandingsElement favouriteConstructor = null;
            if (standings != null && standings.getConstructorStandings() != null) {
                favouriteConstructor = homeViewModel.getConstructorStandingsElement(standings.getConstructorStandings(), favoriteTeamId);
            }

            if (favouriteConstructor == null) {
                // No standing found - create a card with constructor data only (no position/points)
                Log.i(TAG, "No standing found for constructor, fetching constructor data only");
                favouriteConstructor = new ConstructorStandingsElement();
                fetchConstructorDataForCard(view, favoriteTeamId, favouriteConstructor);
            } else {
                Log.i(TAG, "Fetching constructor data card");
                fetchConstructorDataForCard(view, favoriteTeamId, favouriteConstructor);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing constructor standings: " + e.getMessage());
            showConstructorNotFound(view, 0);
        }
    }

    private void fetchConstructorDataForCard(View view, String teamId, ConstructorStandingsElement favouriteConstructor) {
        MutableLiveData<Result> constructorData = constructorViewModel.getSelectedConstructor(teamId);
        constructorData.observe(getViewLifecycleOwner(), constructorResult -> {
            try {
                if (constructorResult instanceof Result.Loading) {
                    return;
                }
                if (constructorResult.isSuccess()) {
                    Constructor constructor = ((Result.ConstructorSuccess) constructorResult).getData();
                    favouriteConstructor.setConstructor(constructor);
                    Log.i(TAG, "Fetching nation data for constructor card");
                    fetchNationForConstructor(view, favouriteConstructor);
                } else {
                    throw new Exception("Failed to fetch constructor data: " + constructorResult.getError());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching constructor data: " + e.getMessage());
                showConstructorNotFound(view, 0);
            }
        });
    }

    private void fetchNationForConstructor(View view, ConstructorStandingsElement favouriteConstructor) {
        try {
            MutableLiveData<Result> nationData = nationViewModel.getNation(favouriteConstructor.getConstructor().getNationality());
            nationData.observe(getViewLifecycleOwner(), nationResult -> {
                try {
                    if (nationResult instanceof Result.Loading) {
                        return;
                    }
                    if (nationResult.isSuccess()) {
                        Nation nation = ((Result.NationSuccess) nationResult).getData();
                        Log.i(TAG, "Building constructor card");
                        buildConstructorCard(view, favouriteConstructor, nation);
                    } else {
                        throw new Exception("Failed to fetch nation data: " + nationResult.getError());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error fetching nation for constructor: " + e.getMessage());
                    showConstructorNotFound(view, 0);
                }
            });
        } catch (RuntimeException e) {
            Log.e(TAG, "Error fetching nation for constructor: " + e.getMessage());
            buildConstructorCard(view, favouriteConstructor, null);
        }

    }

    private void buildConstructorCard(View view, ConstructorStandingsElement standingElement, Nation nation) {

        if (networkLiveData.isConnected() && userViewModel.getLoggedUser() != null) {
            try {
                Constructor constructor = standingElement.getConstructor();

                String nationFlagUrl = null;
                String nationAbbreviation = null;
                if (nation != null) {
                    nationFlagUrl = nation.getNation_flag_url();
                    nationAbbreviation = nation.getAbbreviation();
                }

                UIUtils.multipleSetTextViewText(new String[]{constructor.getName(), nationAbbreviation},
                        new TextView[]{view.findViewById(R.id.favourite_constructor_name), view.findViewById(R.id.favourite_constructor_nationality)});

                ImageView constructorCar = view.findViewById(R.id.favourite_constructor_car);
                ImageView constructorFlag = view.findViewById(R.id.favourite_constructor_flag);

                FrameLayout constructorCard = view.findViewById(R.id.favourite_constructor_layout);
                constructorCard.setOnClickListener(v -> NavigationUtils.navigateToBioPage(getContext(), constructor.getConstructorId(), 0));

                UIUtils.loadImagesInParallel(requireContext(), new String[]{nationFlagUrl, constructor.getCar_pic_url()},
                        new ImageView[]{constructorFlag, constructorCar},
                        () -> buildConstructorCardFinalStep(standingElement, view, constructor));

            } catch (Exception e) {
                Log.e(TAG, "Error building constructor card: " + e.getMessage());
                showConstructorNotFound(view, 0);
            }
        } else {
            Log.e(TAG, "Error building constructor card: No internet connection");
            showConstructorNotFound(view, 1);
        }
    }

    private void buildConstructorCardFinalStep(ConstructorStandingsElement standingElement, View view, Constructor constructor) {
        if (standingElement.getPosition() != null && standingElement.getPoints() != null) {
            UIUtils.multipleSetTextViewText(new String[]{standingElement.getPosition(), standingElement.getPoints()},
                    new TextView[]{view.findViewById(R.id.favourite_constructor_position), view.findViewById(R.id.favourite_constructor_points)});

            MaterialCardView teamRank = view.findViewById(R.id.favourite_constructor_rank);
            teamRank.setOnClickListener(v -> NavigationUtils.navigateToStandingsPage(getContext(), constructor.getConstructorId(), 0));
        } else {
            MaterialCardView teamRank = view.findViewById(R.id.favourite_constructor_rank);
            teamRank.setClickable(false);
        }

        showFavouriteConstructorCard(view);
        Log.i(TAG, "Constructor card built successfully");
        markCardLoaded("constructor");
    }

    private void showSelectFavouriteDriver(View view) {
        markCardLoaded("driver");

        updateVisibility(view, R.id.pending_favorite_driver, R.id.favorite_driver, R.id.missing_favorite_driver);
        view.findViewById(R.id.pending_favorite_driver).setOnClickListener(v -> startActivity(new Intent(getActivity(), DriversStandingActivity.class)));
        Log.e(TAG, "Showing select favourite driver card");
    }

    private void showDriverNotFound(View view, int problem) {
        markCardLoaded("driver");
        updateVisibility(view, R.id.missing_favorite_driver, R.id.favorite_driver, R.id.pending_favorite_driver);

        switch (problem) {
            case 0: //general error
                view.findViewById(R.id.missing_favorite_driver).setOnClickListener(v -> startActivity(new Intent(getActivity(), DriversStandingActivity.class)));
                break;
            case 1: //no internet connection
                Log.e(TAG, "Driver: No internet connection");
                view.findViewById(R.id.missing_favorite_driver).setOnClickListener(v ->
                        Toast.makeText(getContext(), "No internet connection", Toast.LENGTH_SHORT).show());
                break;
            default:
                Toast.makeText(getContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
        }
    }

    private void showFavouriteDriverCard(View view) {
        updateVisibility(view, R.id.favorite_driver, R.id.missing_favorite_driver, R.id.pending_favorite_driver);
    }

    private void showFavouriteConstructorCard(View view) {
        updateVisibility(view, R.id.favorite_constructor, R.id.pending_favorite_constructor, R.id.missing_favorite_constructor);
    }

    private void showSelectFavouriteConstructor(View view) {
        markCardLoaded("constructor");

        updateVisibility(view, R.id.pending_favorite_constructor, R.id.favorite_constructor, R.id.missing_favorite_constructor);
        view.findViewById(R.id.pending_favorite_constructor).setOnClickListener(v -> startActivity(new Intent(getActivity(), ConstructorsStandingActivity.class)));
    }

    private void showConstructorNotFound(View view, int problem) {
        markCardLoaded("constructor");

        updateVisibility(view, R.id.missing_favorite_constructor, R.id.favorite_constructor, R.id.pending_favorite_constructor);

        switch (problem) {
            case 0: //general error
                view.findViewById(R.id.missing_favorite_constructor).setOnClickListener(v -> startActivity(new Intent(getActivity(), ConstructorsStandingActivity.class)));
                break;
            case 1: //no internet connection
                Log.e(TAG, "Constructor: No internet connection");
                view.findViewById(R.id.missing_favorite_constructor).setOnClickListener(v ->
                        Toast.makeText(getContext(), "No internet connection", Toast.LENGTH_SHORT).show());
                break;
            default:
                Toast.makeText(getContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateVisibility(View view, int visibleId, int... goneIds) {
        view.findViewById(visibleId).setVisibility(View.VISIBLE);
        for (int id : goneIds) {
            view.findViewById(id).setVisibility(View.GONE);
        }
    }

    private String getFavoriteDriverId() {
        String driverId = sharedPreferencesUtils.readStringData(Constants.SHARED_PREFERENCES_FILENAME, Constants.SHARED_PREFERENCES_FAVORITE_DRIVER);
        Log.i(TAG, "Favorite Driver ID: " + driverId);
        return driverId;
    }

    private String getFavoriteTeamId() {
        String teamId = sharedPreferencesUtils.readStringData(Constants.SHARED_PREFERENCES_FILENAME, Constants.SHARED_PREFERENCES_FAVORITE_TEAM);
        Log.i(TAG, "Favorite Team ID: " + teamId);
        return teamId;
    }

}
