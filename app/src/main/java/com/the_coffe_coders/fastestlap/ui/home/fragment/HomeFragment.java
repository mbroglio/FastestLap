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
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
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
import com.the_coffe_coders.fastestlap.ui.bio.ConstructorBioActivity;
import com.the_coffe_coders.fastestlap.ui.bio.DriverBioActivity;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.ConstructorViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.ConstructorViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.DriverViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.DriverViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.NationViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.NationViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.TrackViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.TrackViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.event.EventActivity;
import com.the_coffe_coders.fastestlap.ui.home.viewmodel.HomeViewModel;
import com.the_coffe_coders.fastestlap.ui.home.viewmodel.HomeViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.standing.ConstructorsStandingActivity;
import com.the_coffe_coders.fastestlap.ui.standing.DriversStandingActivity;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;
import com.the_coffe_coders.fastestlap.util.SharedPreferencesUtils;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

import java.util.List;

public class HomeFragment extends Fragment {
    private static final String TAG = HomeFragment.class.getSimpleName();
    private SharedPreferencesUtils sharedPreferencesUtils;
    private LoadingScreen loadingScreen;
    private HomeViewModel homeViewModel;
    private ConstructorViewModel constructorViewModel;
    private DriverViewModel driverViewModel;
    private TrackViewModel trackViewModel;
    private NationViewModel nationViewModel;

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferencesUtils = new SharedPreferencesUtils(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        initializeViewModels();
        setupLoadingScreen(view);
        setupUI(view);
        observeLoadingAndErrors();
        return view;
    }

    private void initializeViewModels() {
        homeViewModel = new ViewModelProvider(this, new HomeViewModelFactory(
                ServiceLocator.getInstance().getRaceRepository(getActivity().getApplication(), false),
                ServiceLocator.getInstance().getRaceResultRepository(getActivity().getApplication(), false),
                ServiceLocator.getInstance().getDriverStandingsRepository(getActivity().getApplication(), false),
                ServiceLocator.getInstance().getConstructorStandingsRepository(getActivity().getApplication(), false)
        )).get(HomeViewModel.class);

        constructorViewModel = new ViewModelProvider(this, new ConstructorViewModelFactory(
                ServiceLocator.getInstance().getCommonConstructorRepository())).get(ConstructorViewModel.class);

        driverViewModel = new ViewModelProvider(this, new DriverViewModelFactory(
                ServiceLocator.getInstance().getCommonDriverRepository())).get(DriverViewModel.class);

        trackViewModel = new ViewModelProvider(this, new TrackViewModelFactory(
                ServiceLocator.getInstance().getTrackRepository())).get(TrackViewModel.class);

        nationViewModel = new ViewModelProvider(this, new NationViewModelFactory(
                ServiceLocator.getInstance().getFirebaseNationRepository())).get(NationViewModel.class);
    }

    private void setupLoadingScreen(View view) {
        loadingScreen = new LoadingScreen(view, getContext());
        loadingScreen.showLoadingScreen();
    }

    private void setupUI(View view) {
        setLastRaceCard(view);
        setNextSessionCard(view);
        setFavouriteDriverCard(view);
        setFavouriteConstructorCard(view);
    }

    private void observeLoadingAndErrors() {
        homeViewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null && !isLoading) {
                loadingScreen.hideLoadingScreen();
            }
        });

        homeViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Log.e(TAG, "Error from ViewModel: " + error);
                // Optionally show a toast or UI feedback to the user
            }
        });
    }

    private void setLastRaceCard(View view) {
        MutableLiveData<Result> lastRace = homeViewModel.getLastRace(0L);
        lastRace.observe(getViewLifecycleOwner(), result -> {
            try {
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
        try {
            if (race.getFinalRace().getResults().isEmpty()) {
                throw new Exception("No results available for last race");
            }
            String circuitId = race.getTrack().getTrackId();
            MutableLiveData<Result> trackData = trackViewModel.getTrack(circuitId);
            trackData.observe(getViewLifecycleOwner(), trackResult -> {
                try {
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
            TextView raceName = view.findViewById(R.id.last_race_name);
            raceName.setText(race.getRaceName());

            ImageView trackOutline = view.findViewById(R.id.last_race_track_outline);
            Glide.with(this).load(track.getTrack_minimal_layout_url()).into(trackOutline);

            LocalDateTime dateTime = race.getDateTime();
            TextView raceDate = view.findViewById(R.id.last_race_date);
            raceDate.setText(String.valueOf(dateTime.getDayOfMonth()));

            TextView raceMonth = view.findViewById(R.id.last_race_month);
            raceMonth.setText(dateTime.getMonth().toString().substring(0, 3).toUpperCase());

            TextView roundNumber = view.findViewById(R.id.last_race_round);
            roundNumber.setText("Round " + race.getRound());

            MutableLiveData<Result> raceResultData = ServiceLocator.getInstance()
                    .getRaceResultRepository(getActivity().getApplication(), false)
                    .fetchRaceResult(Integer.parseInt(race.getRound()), 0L);
            raceResultData.observe(getViewLifecycleOwner(), result -> {
                try {
                    if (result.isSuccess()) {
                        List<RaceResult> raceResults = ((Result.RacesResultSuccess) result).getData();
                        setDriverNames(view, raceResults);
                    } else {
                        throw new Exception("Failed to fetch race results: " + result.getError());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error setting driver names: " + e.getMessage());
                }
            });

            MaterialCardView resultCard = view.findViewById(R.id.past_event_result);
            resultCard.setOnClickListener(v -> startActivity(new Intent(getActivity(), EventActivity.class)
                    .putExtra("CIRCUIT_ID", race.getTrack().getTrackId())));
        } catch (Exception e) {
            Log.e(TAG, "Error updating last race UI: " + e.getMessage());
            loadPendingResultsLayout(view);
        }
    }

    private void setDriverNames(View view, List<RaceResult> raceResults) {
        try {
            for (int i = 0; i < Math.min(3, raceResults.size()); i++) {
                TextView driver = view.findViewById(Constants.LAST_RACE_DRIVER_NAME.get(i));
                driver.setText(raceResults.get(i).getDriver().getFullName());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting driver names: " + e.getMessage());
        }
    }

    private void loadPendingResultsLayout(View view) {
        view.findViewById(R.id.pending_last_race_results).setVisibility(View.VISIBLE);
        view.findViewById(R.id.last_race_results).setVisibility(View.GONE);
    }

    private void setNextSessionCard(View view) {
        MutableLiveData<Result> nextRaceLiveData = homeViewModel.getNextRaceLiveData(0L);
        nextRaceLiveData.observe(getViewLifecycleOwner(), result -> {
            try {
                if (result.isSuccess()) {
                    WeeklyRace nextRace = ((Result.NextRaceSuccess) result).getData();
                    processNextRace(view, nextRace);
                } else {
                    throw new Exception("Failed to fetch next race: " + result.getError());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in setNextSessionCard: " + e.getMessage());
                setSeasonEnded(view);
            }
        });

        ImageView iconImageView = view.findViewById(R.id.live_icon);
        Animation pulseAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.pulse_static);
        iconImageView.startAnimation(pulseAnimation);
    }

    private void processNextRace(View view, WeeklyRace nextRace) {
        try {
            if (nextRace == null) throw new Exception("Next race is null");
            MutableLiveData<Result> trackData = trackViewModel.getTrack(nextRace.getTrack().getTrackId());
            trackData.observe(getViewLifecycleOwner(), trackResult -> {
                try {
                    if (trackResult.isSuccess()) {
                        Track track = ((Result.TrackSuccess) trackResult).getData();
                        nextRace.setTrack(track);
                        fetchNationForNextRace(view, nextRace, track);
                    } else {
                        throw new Exception("Failed to fetch track data: " + trackResult.getError());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing track: " + e.getMessage());
                    setSeasonEnded(view);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in processNextRace: " + e.getMessage());
            setSeasonEnded(view);
        }
    }

    private void fetchNationForNextRace(View view, WeeklyRace nextRace, Track track) {
        MutableLiveData<Result> nationData = nationViewModel.getNation(track.getCountry());
        nationData.observe(getViewLifecycleOwner(), nationResult -> {
            try {
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
    }

    private void setNextRaceCard(View view, WeeklyRace nextRace, Nation nation) {
        try {
            TextView nextRaceName = view.findViewById(R.id.home_next_gp_name);
            nextRaceName.setText(nextRace.getRaceName());

            ImageView nextRaceFlag = view.findViewById(R.id.home_next_gp_flag);
            Glide.with(this).load(nation.getNation_flag_url()).into(nextRaceFlag);

            if (!nextRace.getSeason().equals(ServiceLocator.getCurrentYear())) {
                throw new Exception("Season mismatch");
            }

            List<Session> sessions = nextRace.getSessions();
            Session nextEvent = nextRace.findNextEvent(sessions);
            if (nextEvent != null) {
                startCountdown(view, nextEvent.getStartDateTime());
                updateSessionType(view, nextEvent);
            } else {
                throw new Exception("No upcoming session found");
            }

            FrameLayout nextSessionCard = view.findViewById(R.id.timer_card_countdown);
            nextSessionCard.setOnClickListener(v -> startActivity(new Intent(getActivity(), EventActivity.class)
                    .putExtra("CIRCUIT_ID", nextRace.getTrack().getTrackId())));
        } catch (Exception e) {
            Log.e(TAG, "Error in setNextRaceCard: " + e.getMessage());
            setSeasonEnded(view);
        }
    }

    private void updateSessionType(View view, Session nextEvent) {
        TextView sessionType = view.findViewById(R.id.next_session_type);
        String sessionId = nextEvent.getClass().getSimpleName().equals("Practice")
                ? "Practice" + ((Practice) nextEvent).getNumber()
                : nextEvent.getClass().getSimpleName();
        sessionType.setText(Constants.SESSION_NAMES.getOrDefault(sessionId, "Unknown"));
    }

    private void startCountdown(View view, LocalDateTime eventDate) {
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
                days.setText(String.valueOf(millisUntilFinished / 86400000));
                hours.setText(String.valueOf((millisUntilFinished % 86400000) / 3600000));
                minutes.setText(String.valueOf(((millisUntilFinished % 86400000) % 3600000) / 60000));
                seconds.setText(String.valueOf((((millisUntilFinished % 86400000) % 3600000) % 60000) / 1000));
            }

            @Override
            public void onFinish() {
                days.setText("0");
                hours.setText("0");
                minutes.setText("0");
                seconds.setText("0");
                liveIconLayout.setVisibility(View.VISIBLE);
            }
        }.start();
    }

    private void setSeasonEnded(View view) {
        view.findViewById(R.id.last_race_results).setVisibility(View.GONE);
        view.findViewById(R.id.timer).setVisibility(View.GONE);
        view.findViewById(R.id.season_ended).setVisibility(View.VISIBLE);
        view.findViewById(R.id.season_results).setVisibility(View.VISIBLE);
        view.findViewById(R.id.pending_last_race_results).setVisibility(View.GONE);

        buildFinalDriversStanding(view.findViewById(R.id.season_results));
        buildFinalTeamsStanding(view.findViewById(R.id.season_results));
    }

    private void buildFinalDriversStanding(View seasonEndedCard) {
        MutableLiveData<Result> driverStandingsLiveData = homeViewModel.getDriverStandingsLiveData(0L);
        driverStandingsLiveData.observe(getViewLifecycleOwner(), result -> {
            try {
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
                if (result.isSuccess()) {
                    Driver driver = ((Result.DriverSuccess) result).getData();
                    TextView driverName = seasonEndedCard.findViewById(Constants.HOME_SEASON_DRIVER_STANDINGS_NAME_FIELD.get(position));
                    driverName.setText(driver.getFullName());

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
        MutableLiveData<Result> constructorStandingsData = homeViewModel.getConstructorStandingsLiveData(0L);
        constructorStandingsData.observe(getViewLifecycleOwner(), result -> {
            try {
                if (result.isSuccess()) {
                    ConstructorStandings standings = ((Result.ConstructorStandingsSuccess) result).getData();
                    List<ConstructorStandingsElement> constructorsList = standings.getConstructorStandings();
                    for (int i = 0; i < Math.min(3, constructorsList.size()); i++) {
                        ConstructorStandingsElement constructor = constructorsList.get(i);
                        TextView constructorName = seasonEndedCard.findViewById(Constants.HOME_SEASON_TEAM_STANDINGS_NAME_FIELD.get(i));
                        constructorName.setText(constructor.getConstructor().getName());

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
        if (favoriteDriverId == null || favoriteDriverId.isEmpty()) {
            showSelectFavouriteDriver(view);
            return;
        }

        MutableLiveData<Result> driverStandingsLiveData = homeViewModel.getDriverStandingsLiveData(0L);
        driverStandingsLiveData.observe(getViewLifecycleOwner(), result -> {
            try {
                if (result.isSuccess()) {
                    DriverStandings driverStandings = ((Result.DriverStandingsSuccess) result).getData();
                    DriverStandingsElement favouriteDriver = homeViewModel.getDriverStandingsElement(
                            driverStandings.getDriverStandingsElements(), favoriteDriverId);
                    if (favouriteDriver == null) {
                        showSelectFavouriteDriver(view);
                    } else {
                        fetchDriverDataForCard(view, favoriteDriverId, favouriteDriver);
                    }
                } else {
                    throw new Exception("Failed to fetch driver standings: " + result.getError());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in setFavouriteDriverCard: " + e.getMessage());
                showDriverNotFound(view);
            }
        });
    }

    private void fetchDriverDataForCard(View view, String driverId, DriverStandingsElement favouriteDriver) {
        MutableLiveData<Result> driverData = driverViewModel.getDriver(driverId);
        driverData.observe(getViewLifecycleOwner(), driverResult -> {
            try {
                if (driverResult.isSuccess()) {
                    Driver driver = ((Result.DriverSuccess) driverResult).getData();
                    favouriteDriver.setDriver(driver);
                    fetchNationForDriver(view, favouriteDriver);
                } else {
                    throw new Exception("Failed to fetch driver data: " + driverResult.getError());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching driver data: " + e.getMessage());
                showDriverNotFound(view);
            }
        });
    }

    private void fetchNationForDriver(View view, DriverStandingsElement favouriteDriver) {
        MutableLiveData<Result> nationData = nationViewModel.getNation(favouriteDriver.getDriver().getNationality());
        nationData.observe(getViewLifecycleOwner(), nationResult -> {
            try {
                if (nationResult.isSuccess()) {
                    Nation nation = ((Result.NationSuccess) nationResult).getData();
                    buildDriverCard(view, favouriteDriver, nation);
                } else {
                    throw new Exception("Failed to fetch nation data: " + nationResult.getError());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching nation for driver: " + e.getMessage());
                showDriverNotFound(view);
            }
        });
    }

    private void buildDriverCard(View view, DriverStandingsElement standingElement, Nation nation) {
        try {
            Driver driver = standingElement.getDriver();
            TextView driverName = view.findViewById(R.id.favourite_driver_name);
            driverName.setText(driver.getGivenName() + " " + driver.getFamilyName());

            ImageView driverFlag = view.findViewById(R.id.favourite_driver_flag);
            Glide.with(this).load(nation.getNation_flag_url()).into(driverFlag);

            TextView nationality = view.findViewById(R.id.favourite_driver_nationality);
            nationality.setText(nation.getAbbreviation());

            ImageView driverImage = view.findViewById(R.id.favourite_driver_pic);
            Glide.with(this).load(driver.getDriver_pic_url()).into(driverImage);

            if (standingElement.getPosition() != null && standingElement.getPoints() != null) {
                TextView driverPosition = view.findViewById(R.id.favourite_driver_position);
                driverPosition.setText(standingElement.getPosition());

                TextView driverPoints = view.findViewById(R.id.favourite_driver_points);
                driverPoints.setText(standingElement.getPoints());

                MaterialCardView driverRank = view.findViewById(R.id.favourite_driver_rank);
                driverRank.setOnClickListener(v -> startActivity(new Intent(getActivity(), DriversStandingActivity.class)
                        .putExtra("DRIVER_ID", driver.getDriverId())));
            } else {
                MaterialCardView driverRank = view.findViewById(R.id.favourite_driver_rank);
                driverRank.setClickable(false);
            }

            driverImage.setOnClickListener(v -> startActivity(new Intent(getActivity(), DriverBioActivity.class)
                    .putExtra("DRIVER_ID", driver.getDriverId())));
        } catch (Exception e) {
            Log.e(TAG, "Error building driver card: " + e.getMessage());
            showDriverNotFound(view);
        }
    }

    private void setFavouriteConstructorCard(View view) {
        String favoriteTeamId = getFavoriteTeamId();
        if (favoriteTeamId == null || favoriteTeamId.isEmpty()) {
            showSelectFavouriteConstructor(view);
            return;
        }

        MutableLiveData<Result> constructorStandingsData = homeViewModel.getConstructorStandingsLiveData(0L);
        constructorStandingsData.observe(getViewLifecycleOwner(), result -> {
            try {
                if (result.isSuccess()) {
                    ConstructorStandings standings = ((Result.ConstructorStandingsSuccess) result).getData();
                    ConstructorStandingsElement favouriteConstructor = homeViewModel.getConstructorStandingsElement(
                            standings.getConstructorStandings(), favoriteTeamId);
                    if (favouriteConstructor == null) {
                        showSelectFavouriteConstructor(view);
                    } else {
                        fetchConstructorDataForCard(view, favoriteTeamId, favouriteConstructor);
                    }
                } else {
                    throw new Exception("Failed to fetch constructor standings: " + result.getError());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in setFavouriteConstructorCard: " + e.getMessage());
                showConstructorNotFound(view);
            }
        });
    }

    private void fetchConstructorDataForCard(View view, String teamId, ConstructorStandingsElement favouriteConstructor) {
        MutableLiveData<Result> constructorData = constructorViewModel.getSelectedConstructorLiveData(teamId);
        constructorData.observe(getViewLifecycleOwner(), constructorResult -> {
            try {
                if (constructorResult.isSuccess()) {
                    Constructor constructor = ((Result.ConstructorSuccess) constructorResult).getData();
                    favouriteConstructor.setConstructor(constructor);
                    fetchNationForConstructor(view, favouriteConstructor);
                } else {
                    throw new Exception("Failed to fetch constructor data: " + constructorResult.getError());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching constructor data: " + e.getMessage());
                showConstructorNotFound(view);
            }
        });
    }

    private void fetchNationForConstructor(View view, ConstructorStandingsElement favouriteConstructor) {
        MutableLiveData<Result> nationData = nationViewModel.getNation(favouriteConstructor.getConstructor().getNationality());
        nationData.observe(getViewLifecycleOwner(), nationResult -> {
            try {
                if (nationResult.isSuccess()) {
                    Nation nation = ((Result.NationSuccess) nationResult).getData();
                    buildConstructorCard(view, favouriteConstructor, nation);
                } else {
                    throw new Exception("Failed to fetch nation data: " + nationResult.getError());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching nation for constructor: " + e.getMessage());
                showConstructorNotFound(view);
            }
        });
    }

    private void buildConstructorCard(View view, ConstructorStandingsElement standingElement, Nation nation) {
        try {
            Constructor constructor = standingElement.getConstructor();
            TextView constructorName = view.findViewById(R.id.favourite_constructor_name);
            constructorName.setText(constructor.getName());

            ImageView constructorCar = view.findViewById(R.id.favourite_constructor_car);
            Glide.with(this).load(constructor.getCar_pic_url()).into(constructorCar);

            ImageView constructorFlag = view.findViewById(R.id.favourite_constructor_flag);
            Glide.with(this).load(nation.getNation_flag_url()).into(constructorFlag);

            TextView constructorNationality = view.findViewById(R.id.favourite_constructor_nationality);
            constructorNationality.setText(nation.getAbbreviation());

            if (standingElement.getPosition() != null && standingElement.getPoints() != null) {
                TextView constructorPosition = view.findViewById(R.id.favourite_constructor_position);
                constructorPosition.setText(standingElement.getPosition());

                TextView constructorPoints = view.findViewById(R.id.favourite_constructor_points);
                constructorPoints.setText(standingElement.getPoints());

                MaterialCardView teamRank = view.findViewById(R.id.favourite_constructor_rank);
                teamRank.setOnClickListener(v -> startActivity(new Intent(getActivity(), ConstructorsStandingActivity.class)
                        .putExtra("TEAM_ID", constructor.getConstructorId())));
            } else {
                MaterialCardView teamRank = view.findViewById(R.id.favourite_constructor_rank);
                teamRank.setClickable(false);
            }

            FrameLayout constructorCard = view.findViewById(R.id.favourite_constructor_layout);
            constructorCard.setOnClickListener(v -> startActivity(new Intent(getActivity(), ConstructorBioActivity.class)
                    .putExtra("TEAM_ID", constructor.getConstructorId())));
        } catch (Exception e) {
            Log.e(TAG, "Error building constructor card: " + e.getMessage());
            showConstructorNotFound(view);
        }
    }

    private void showSelectFavouriteDriver(View view) {
        updateVisibility(view, R.id.pending_favorite_driver, R.id.favorite_driver, R.id.missing_favorite_driver);
        view.findViewById(R.id.pending_favorite_driver).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), DriversStandingActivity.class)));
    }

    private void showDriverNotFound(View view) {
        updateVisibility(view, R.id.missing_favorite_driver, R.id.favorite_driver, R.id.pending_favorite_driver);
        view.findViewById(R.id.missing_favorite_driver).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), DriversStandingActivity.class)));
    }

    private void showSelectFavouriteConstructor(View view) {
        updateVisibility(view, R.id.pending_favorite_constructor, R.id.favorite_constructor, R.id.missing_favorite_constructor);
        view.findViewById(R.id.pending_favorite_constructor).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), ConstructorsStandingActivity.class)));
    }

    private void showConstructorNotFound(View view) {
        updateVisibility(view, R.id.missing_favorite_constructor, R.id.favorite_constructor, R.id.pending_favorite_constructor);
        view.findViewById(R.id.missing_favorite_constructor).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), ConstructorsStandingActivity.class)));
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