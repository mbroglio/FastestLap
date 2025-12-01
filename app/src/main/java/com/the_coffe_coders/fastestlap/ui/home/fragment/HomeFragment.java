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
import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandings;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandingsElement;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandings;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandingsElement;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Practice;
import com.the_coffe_coders.fastestlap.domain.grand_prix.RaceResult;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Session;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Track;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;
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
import com.the_coffe_coders.fastestlap.util.ui.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.NetworkUtils;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;
import com.the_coffe_coders.fastestlap.util.SharedPreferencesUtils;
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
    private boolean loginWithConnection;
    private NetworkUtils networkLiveData;
    private int loadingCounter = 0;

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

        setupFragment(view);

        return view;
    }

    private void setupFragment(View view) {
        Intent intent = requireActivity().getIntent();
        if (intent != null){
            if (intent.hasExtra("RELOADED")) {
                if (Objects.equals(intent.getStringExtra("RELOADED"), "true")) {
                    hasReloaded = true;
                }
            }
            if (intent.hasExtra("LOGIN_WITH_CONNECTION")) {
                loginWithConnection = intent.getBooleanExtra("LOGIN_WITH_CONNECTION", false);
            }
        }


        initializeViewModels();
        setupLoadingScreen(view);
        setupUI(view);
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
        loadingCounter = 4; // Last race, next session, favorite driver, favorite constructor

        if(networkLiveData.isConnected()){
            userViewModel.getUserPreferences(userViewModel.getLoggedUser().getIdToken()).observe(getViewLifecycleOwner(), result -> {
                if (result != null) {
                    if (result.isSuccess()) {
                        Log.d(TAG, "User preferences loaded successfully");
                    } else {
                        Log.e(TAG, "Failed to load user preferences: " + result.getError());
                    }

                    setFavouriteDriverCard(view);
                    setFavouriteConstructorCard(view);
                }
            });
        }else{
            Log.e(TAG, "Failed to load user preferences: No internet connection");
            showDriverNotFound(view,1);
            showConstructorNotFound(view,1);
        }

        setRefreshLayout(view);
        setLastRaceCard(view);
        setNextSessionCard(view);
    }

    private void setRefreshLayout(View view) {
        SwipeRefreshLayout homeSwipeRefreshLayout = view.findViewById(R.id.home_refresh_layout);
        homeSwipeRefreshLayout.setOnRefreshListener(() -> {
            setupFragment(view);
            homeSwipeRefreshLayout.setRefreshing(false);
        });
    }

    private synchronized void decrementLoadingCounter() {
        loadingCounter--;
        if (loadingCounter == 0) {
            loadingScreen.hideLoadingScreen();
        }
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

    private void showPodium(View view, WeeklyRace race) {
        decrementLoadingCounter();
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
        decrementLoadingCounter();
        try {
            UIUtils.singleSetTextViewText(race.getRaceName(), view.findViewById(R.id.last_race_name));
            UIUtils.loadImageWithGlide(requireContext(), track.getTrack_minimal_layout_url(), view.findViewById(R.id.last_race_track_outline), () -> updateLastRaceUIFinalStep(race, view));
        } catch (Exception e) {
            Log.e(TAG, "Error updating last race UI: " + e.getMessage());
            loadPendingResultsLayout(view);
        }
    }

    private void updateLastRaceUIFinalStep(WeeklyRace race, View view) {
        decrementLoadingCounter();
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
        decrementLoadingCounter();
        try {
            for (int i = 0; i < Math.min(3, raceResults.size()); i++) {
                UIUtils.singleSetTextViewText(raceResults.get(i).getDriver().getFullName(), view.findViewById(Constants.LAST_RACE_DRIVER_NAME.get(i)));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting driver names: " + e.getMessage());
        }
    }

    private void loadPendingResultsLayout(View view) {
        decrementLoadingCounter();
        view.findViewById(R.id.pending_last_race_results).setVisibility(View.VISIBLE);
        view.findViewById(R.id.last_race_results).setVisibility(View.GONE);
    }

    private void setNextSessionCard(View view) {
        decrementLoadingCounter();

        LiveData<Result> nextRaceLiveData = weeklyRaceViewModel.getNextRaceLiveData();
        try {
            nextRaceLiveData.observe(getViewLifecycleOwner(), result -> {

                try {
                    if (result instanceof Result.Loading) {
                        return;
                    }
                    if (result.isSuccess()) {
                        WeeklyRace nextRace = ((Result.NextRaceSuccess) result).getData();
                        processNextRace(view, nextRace);
                    } else {
                        throw new Exception("Failed to fetch next race: " + result.getError());
                    }
                } catch (Exception e) {
                    if(networkLiveData.isConnected()){
                        Log.e(TAG, "Error in setNextSessionCard: " + e.getMessage());
                        setSeasonEnded(view);
                    }else{
                        Log.e(TAG, "Error in setNextSessionCard: No internet connection");
                        setUpdating(view);
                    }
                }
            });
        } catch (Exception e) {
            if(networkLiveData.isConnected()){
                Log.e(TAG, "Error in setNextSessionCard: " + e.getMessage());
                setSeasonEnded(view);
            }else{
                Log.e(TAG, "Error in setNextSessionCard: No internet connection");
                setUpdating(view);
            }
        }

        ImageView iconImageView = view.findViewById(R.id.live_icon);
        Animation pulseAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.pulse_static);
        iconImageView.startAnimation(pulseAnimation);
    }

    private void processNextRace(View view, WeeklyRace nextRace) {
        decrementLoadingCounter();
        try {
            if (nextRace == null) throw new Exception("Next race is null");
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
                    if(networkLiveData.isConnected()){
                        Log.e(TAG, "Error in processNextRace: " + e.getMessage());
                        setSeasonEnded(view);
                    }else{
                        Log.e(TAG, "Error in processNextRace: No internet connection");
                        setUpdating(view);
                    }
                }
            });
        } catch (Exception e) {
            if(networkLiveData.isConnected()){
                Log.e(TAG, "Error in processNextRace: " + e.getMessage());
                setSeasonEnded(view);
            }else{
                Log.e(TAG, "Error in processNextRace: No internet connection");
                setUpdating(view);
            }
        }
    }

    private void fetchNationForNextRace(View view, WeeklyRace nextRace, Track track) {
        decrementLoadingCounter();
        try{
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
        }catch (RuntimeException e) {
            Log.e(TAG, "Error fetching nation: " + e.getMessage());
            setNextRaceCard(view, nextRace, null);
        }

    }

    private void setNextRaceCard(View view, WeeklyRace nextRace, Nation nation) {
        decrementLoadingCounter();
        try {
            UIUtils.singleSetTextViewText(nextRace.getRaceName(), view.findViewById(R.id.home_next_gp_name));

            String nationFlagUrl = null;
            if(nation != null) {
                nationFlagUrl = nation.getNation_flag_url();
            }

            UIUtils.loadImageWithGlide(requireContext(), nationFlagUrl, view.findViewById(R.id.home_next_gp_flag), () -> {
                try {
                    setNextRaceCardFinalStep(nextRace, view);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

        } catch (Exception e) {
            Log.i(TAG, "connected: " + networkLiveData.isConnected());
            if(networkLiveData.isConnected()){
                setSeasonEnded(view);
                Log.e(TAG, "Error in setNextRaceCard: " + e.getMessage());
            }else{
                Log.e(TAG, "Error in setNextRaceCard: No internet connection");
                loadPendingResultsLayout(view);
            }

        }
    }

    private void setNextRaceCardFinalStep(WeeklyRace nextRace, View view) throws Exception {
        decrementLoadingCounter();
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

    private void updateSessionType(View view, Session nextEvent) {
        String sessionId = nextEvent.getClass().getSimpleName().equals("Practice") ? "Practice" + ((Practice) nextEvent).getNumber() : nextEvent.getClass().getSimpleName();
        TextView sessionTypeView = view.findViewById(R.id.next_session_type);

        UIUtils.translateSessionType(requireContext(), sessionTypeView, sessionId);
    }

    private void startCountdown(View view, LocalDateTime eventDate) {
        decrementLoadingCounter();
        LinearLayout liveIconLayout = view.findViewById(R.id.timer_live_layout);
        liveIconLayout.setVisibility(View.GONE);
        long millisUntilStart = ZonedDateTime.of(eventDate, ZoneId.systemDefault()).toInstant().toEpochMilli() - System.currentTimeMillis();
        if (millisUntilStart <= 0) {
            liveIconLayout.setVisibility(View.VISIBLE);
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
    }

    private void setSeasonEnded(View view) {
        decrementLoadingCounter();

        view.findViewById(R.id.last_race_results).setVisibility(View.GONE);
        view.findViewById(R.id.timer).setVisibility(View.GONE);
        view.findViewById(R.id.season_ended).setVisibility(View.VISIBLE);
        view.findViewById(R.id.season_results).setVisibility(View.VISIBLE);
        view.findViewById(R.id.pending_last_race_results).setVisibility(View.GONE);

        buildFinalDriversStanding(view.findViewById(R.id.season_results));
        buildFinalTeamsStanding(view.findViewById(R.id.season_results));
    }

    private void setUpdating(View view) {
        decrementLoadingCounter();
        view.findViewById(R.id.timer_card_countdown).setVisibility(View.GONE);
        view.findViewById(R.id.timer_updating).setVisibility(View.VISIBLE);
    }

    private void buildFinalDriversStanding(View seasonEndedCard) {
        MutableLiveData<Result> driverStandingsLiveData = homeViewModel.getDriverStandingsLiveData(requireActivity().getApplication());
        decrementLoadingCounter();

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
        decrementLoadingCounter();

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
        decrementLoadingCounter();

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
        decrementLoadingCounter();

        String favoriteDriverId = getFavoriteDriverId();
        if (favoriteDriverId == null || favoriteDriverId.isEmpty() || favoriteDriverId.equals("null")) {
            showSelectFavouriteDriver(view);
            return;
        }

        MutableLiveData<Result> driverStandingsLiveData = homeViewModel.getDriverStandingsLiveData(requireActivity().getApplication());
        driverStandingsLiveData.observe(getViewLifecycleOwner(), result -> {
            try {
                if (result instanceof Result.Loading) {
                    return;
                }
                if (result.isSuccess()) {
                    DriverStandings driverStandings = ((Result.DriverStandingsSuccess) result).getData();
                    DriverStandingsElement favouriteDriver = homeViewModel.getDriverStandingsElement(driverStandings.getDriverStandingsElements(), favoriteDriverId);
                    if (favouriteDriver == null) {
                        showSelectFavouriteDriver(view);
                    } else {
                        Log.e(TAG, "Fetching driver data card");
                        fetchDriverDataForCard(view, favoriteDriverId, favouriteDriver);
                    }
                } else {
                    throw new Exception("Failed to fetch driver standings: " + result.getError());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in setFavouriteDriverCard: " + e.getMessage());
                showDriverNotFound(view,0);
            }
        });
    }

    private void fetchDriverDataForCard(View view, String driverId, DriverStandingsElement favouriteDriver) {
        decrementLoadingCounter();

        MutableLiveData<Result> driverData = driverViewModel.getDriver(driverId);
        driverData.observe(getViewLifecycleOwner(), driverResult -> {
            try {
                if (driverResult instanceof Result.Loading) {
                    return;
                }
                if (driverResult.isSuccess()) {
                    Driver driver = ((Result.DriverSuccess) driverResult).getData();
                    favouriteDriver.setDriver(driver);
                    Log.e(TAG, "Fetching nation data for driver card");
                    fetchNationForDriver(view, favouriteDriver);
                } else {
                    throw new Exception("Failed to fetch driver data: " + driverResult.getError());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching driver data: " + e.getMessage());
                showDriverNotFound(view,0);
            }
        });
    }

    private void fetchNationForDriver(View view, DriverStandingsElement favouriteDriver) {
        decrementLoadingCounter();

        try{
            MutableLiveData<Result> nationData = nationViewModel.getNation(favouriteDriver.getDriver().getNationality());
            nationData.observe(getViewLifecycleOwner(), nationResult -> {
                try {
                    if (nationResult instanceof Result.Loading) {
                        return;
                    }
                    if (nationResult.isSuccess()) {
                        Nation nation = ((Result.NationSuccess) nationResult).getData();
                        Log.e(TAG, "Building driver card");
                        buildDriverCard(view, favouriteDriver, nation);
                    } else {
                        throw new Exception("Failed to fetch nation data: " + nationResult.getError());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error fetching nation for driver: " + e.getMessage());
                    showDriverNotFound(view,0);
                }
            });
        } catch (RuntimeException e) {
            Log.e(TAG, "Error fetching nation for driver: " + e.getMessage());
            buildDriverCard(view, favouriteDriver, null);
        }

    }

    private void buildDriverCard(View view, DriverStandingsElement standingElement, Nation nation) {
        decrementLoadingCounter();

        if(networkLiveData.isConnected() && loginWithConnection){
            try {
                Driver driver = standingElement.getDriver();

                String nationFlagUrl = null;
                String nationAbbreviation = null;
                if(nation != null) {
                    nationFlagUrl = nation.getNation_flag_url();
                    nationAbbreviation = nation.getAbbreviation();
                }

                UIUtils.multipleSetTextViewText(new String[]{driver.getGivenName() + " " + driver.getFamilyName(), nationAbbreviation},
                        new TextView[]{view.findViewById(R.id.favourite_driver_name), view.findViewById(R.id.favourite_driver_nationality)});

                ImageView driverFlag = view.findViewById(R.id.favourite_driver_flag);
                ImageView driverImage = view.findViewById(R.id.favourite_driver_pic);
                driverImage.setOnClickListener(v -> UIUtils.navigateToBioPage(getContext(), driver.getDriverId(), 1));

                UIUtils.loadSequenceOfImagesWithGlide(requireContext(), new String[]{nationFlagUrl, driver.getDriver_pic_url()}, new ImageView[]{driverFlag, driverImage}, () -> buildDriverCardFinalStep(standingElement, view, driver));
            } catch (Exception e) {
                Log.e(TAG, "Error building driver card: " + e.getMessage());
                showDriverNotFound(view,0);
            }
        }else{
            Log.e(TAG, "Error building driver card: No internet connection");
            loginWithConnection = false;
            showDriverNotFound(view,1);
        }

    }

    private void buildDriverCardFinalStep(DriverStandingsElement standingElement, View view, Driver driver) {
        decrementLoadingCounter();

        if (standingElement.getPosition() != null && standingElement.getPoints() != null) {
            UIUtils.multipleSetTextViewText(new String[]{standingElement.getPosition(), standingElement.getPoints()},
                    new TextView[]{view.findViewById(R.id.favourite_driver_position), view.findViewById(R.id.favourite_driver_points)});

            MaterialCardView driverRank = view.findViewById(R.id.favourite_driver_rank);
            driverRank.setOnClickListener(v -> UIUtils.navigateToStandingsPage(getContext(), driver.getDriverId(), 1));
        } else {
            MaterialCardView driverRank = view.findViewById(R.id.favourite_driver_rank);
            driverRank.setClickable(false);
        }

        Log.e(TAG, "Driver card built successfully");
        showFavouriteDriverCard(view);
    }

    private void setFavouriteConstructorCard(View view) {
        decrementLoadingCounter();

        String favoriteTeamId = getFavoriteTeamId();
        if (favoriteTeamId == null || favoriteTeamId.isEmpty() || favoriteTeamId.equals("null")) {
            Log.e(TAG, "Showing select favourite constructor card");
            showSelectFavouriteConstructor(view);
            return;
        }

        MutableLiveData<Result> constructorStandingsData = homeViewModel.getConstructorStandingsLiveData(requireActivity().getApplication());
        constructorStandingsData.observe(getViewLifecycleOwner(), result -> {
            try {
                if (result instanceof Result.Loading) {
                    return;
                }
                if (result.isSuccess()) {
                    ConstructorStandings standings = ((Result.ConstructorStandingsSuccess) result).getData();
                    ConstructorStandingsElement favouriteConstructor = homeViewModel.getConstructorStandingsElement(standings.getConstructorStandings(), favoriteTeamId);
                    if (favouriteConstructor == null) {
                        Log.e(TAG, "Showing select favourite constructor card");
                        showSelectFavouriteConstructor(view);
                    } else {
                        Log.e(TAG, "Fetching constructor data card");
                        fetchConstructorDataForCard(view, favoriteTeamId, favouriteConstructor);
                    }
                } else {
                    throw new Exception("Failed to fetch constructor standings: " + result.getError());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in setFavouriteConstructorCard: " + e.getMessage());
                showConstructorNotFound(view,0);
            }
        });
    }

    private void fetchConstructorDataForCard(View view, String teamId, ConstructorStandingsElement favouriteConstructor) {
        decrementLoadingCounter();

        MutableLiveData<Result> constructorData = constructorViewModel.getSelectedConstructor(teamId);
        constructorData.observe(getViewLifecycleOwner(), constructorResult -> {
            try {
                if (constructorResult instanceof Result.Loading) {
                    return;
                }
                if (constructorResult.isSuccess()) {
                    Constructor constructor = ((Result.ConstructorSuccess) constructorResult).getData();
                    favouriteConstructor.setConstructor(constructor);
                    Log.e(TAG, "Fetching nation data for constructor card");
                    fetchNationForConstructor(view, favouriteConstructor);
                } else {
                    throw new Exception("Failed to fetch constructor data: " + constructorResult.getError());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching constructor data: " + e.getMessage());
                showConstructorNotFound(view,0);
            }
        });
    }

    private void fetchNationForConstructor(View view, ConstructorStandingsElement favouriteConstructor) {
        decrementLoadingCounter();

        try{
            MutableLiveData<Result> nationData = nationViewModel.getNation(favouriteConstructor.getConstructor().getNationality());
            nationData.observe(getViewLifecycleOwner(), nationResult -> {
                try {
                    if (nationResult instanceof Result.Loading) {
                        return;
                    }
                    if (nationResult.isSuccess()) {
                        Nation nation = ((Result.NationSuccess) nationResult).getData();
                        Log.e(TAG, "Building constructor card");
                        buildConstructorCard(view, favouriteConstructor, nation);
                    } else {
                        throw new Exception("Failed to fetch nation data: " + nationResult.getError());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error fetching nation for constructor: " + e.getMessage());
                    showConstructorNotFound(view,0);
                }
            });
        }catch (RuntimeException e) {
            Log.e(TAG, "Error fetching nation for constructor: " + e.getMessage());
            buildConstructorCard(view, favouriteConstructor, null);
        }

    }

    private void buildConstructorCard(View view, ConstructorStandingsElement standingElement, Nation nation) {
        decrementLoadingCounter();

        if(networkLiveData.isConnected() && loginWithConnection){
            try {
                Constructor constructor = standingElement.getConstructor();

                String nationFlagUrl = null;
                String nationAbbreviation = null;
                if(nation != null) {
                    nationFlagUrl = nation.getNation_flag_url();
                    nationAbbreviation = nation.getAbbreviation();
                }

                UIUtils.multipleSetTextViewText(new String[]{constructor.getName(), nationAbbreviation},
                        new TextView[]{view.findViewById(R.id.favourite_constructor_name), view.findViewById(R.id.favourite_constructor_nationality)});

                ImageView constructorCar = view.findViewById(R.id.favourite_constructor_car);
                ImageView constructorFlag = view.findViewById(R.id.favourite_constructor_flag);

                FrameLayout constructorCard = view.findViewById(R.id.favourite_constructor_layout);
                constructorCard.setOnClickListener(v -> UIUtils.navigateToBioPage(getContext(), constructor.getConstructorId(), 0));

                UIUtils.loadSequenceOfImagesWithGlide(requireContext(), new String[]{nationFlagUrl, constructor.getCar_pic_url()},
                        new ImageView[]{constructorFlag, constructorCar},
                        () -> buildConstructorCardFinalStep(standingElement, view, constructor));

            } catch (Exception e) {
                Log.e(TAG, "Error building constructor card: " + e.getMessage());
                showConstructorNotFound(view, 0);
            }
        }else{
            Log.e(TAG, "Error building constructor card: No internet connection");
            loginWithConnection = false;
            showConstructorNotFound(view,1);
        }
    }

    private void buildConstructorCardFinalStep(ConstructorStandingsElement standingElement, View view, Constructor constructor) {
        decrementLoadingCounter();

        if (standingElement.getPosition() != null && standingElement.getPoints() != null) {
            UIUtils.multipleSetTextViewText(new String[]{standingElement.getPosition(), standingElement.getPoints()},
                    new TextView[]{view.findViewById(R.id.favourite_constructor_position), view.findViewById(R.id.favourite_constructor_points)});

            MaterialCardView teamRank = view.findViewById(R.id.favourite_constructor_rank);
            teamRank.setOnClickListener(v -> UIUtils.navigateToStandingsPage(getContext(), constructor.getConstructorId(), 0));
        } else {
            MaterialCardView teamRank = view.findViewById(R.id.favourite_constructor_rank);
            teamRank.setClickable(false);
        }

        showFavouriteConstructorCard(view);
        Log.e(TAG, "Constructor card built successfully");
    }

    private void showSelectFavouriteDriver(View view) {
        decrementLoadingCounter();

        updateVisibility(view, R.id.pending_favorite_driver, R.id.favorite_driver, R.id.missing_favorite_driver);
        view.findViewById(R.id.pending_favorite_driver).setOnClickListener(v -> startActivity(new Intent(getActivity(), DriversStandingActivity.class)));
        Log.e(TAG, "Showing select favourite driver card");
    }

    private void showDriverNotFound(View view, int problem) {
        decrementLoadingCounter();
        updateVisibility(view, R.id.missing_favorite_driver, R.id.favorite_driver, R.id.pending_favorite_driver);

        switch(problem){
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
        decrementLoadingCounter();

        updateVisibility(view, R.id.pending_favorite_constructor, R.id.favorite_constructor, R.id.missing_favorite_constructor);
        view.findViewById(R.id.pending_favorite_constructor).setOnClickListener(v -> startActivity(new Intent(getActivity(), ConstructorsStandingActivity.class)));
    }

    private void showConstructorNotFound(View view, int problem) {
        decrementLoadingCounter();

        updateVisibility(view, R.id.missing_favorite_constructor, R.id.favorite_constructor, R.id.pending_favorite_constructor);

        switch(problem){
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