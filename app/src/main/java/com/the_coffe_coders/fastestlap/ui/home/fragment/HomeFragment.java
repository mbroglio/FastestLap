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

import com.google.android.material.card.MaterialCardView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandings;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandingsElement;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandings;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandingsElement;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Session;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.ui.bio.ConstructorBioActivity;
import com.the_coffe_coders.fastestlap.ui.bio.DriverBioActivity;
import com.the_coffe_coders.fastestlap.ui.event.EventActivity;
import com.the_coffe_coders.fastestlap.ui.profile.ProfileActivity;
import com.the_coffe_coders.fastestlap.ui.standing.ConstructorsStandingActivity;
import com.the_coffe_coders.fastestlap.ui.standing.DriversStandingActivity;
import com.the_coffe_coders.fastestlap.ui.standing.viewmodel.ConstructorStandingsViewModel;
import com.the_coffe_coders.fastestlap.ui.standing.viewmodel.ConstructorStandingsViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.standing.viewmodel.DriverStandingsViewModel;
import com.the_coffe_coders.fastestlap.ui.standing.viewmodel.DriverStandingsViewModelFactory;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;
import com.the_coffe_coders.fastestlap.util.SharedPreferencesUtils;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

import java.util.List;

/**
 * TODO:
 *  - Implement fetchLastRace
 *  - Implement fetchNextRace
 *  - Fix setNextSessionCard to check if underway for live icon
 *  - Fix setFavouriteConstructorCard
 *  - Implement firebase to get favourite driver and constructor after user login
 */

public class HomeFragment extends Fragment {
    private final String TAG = HomeFragment.class.getSimpleName();
    private final SharedPreferencesUtils sharedPreferencesUtils = new SharedPreferencesUtils(getActivity());

    LoadingScreen loadingScreen;


    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Show loading screen initially
        loadingScreen = new LoadingScreen(view, getContext());
        loadingScreen.showLoadingScreen();

        setLastRaceCard(view);
        setNextSessionCard(view);
        setFavouriteDriverCard(view);
        setFavouriteConstructorCard(view);

        return view;
    }

    private void setLastRaceCard(View view) {
        MutableLiveData<Result> data = ServiceLocator.getInstance().getRaceRepository(getActivity().getApplication(), false).fetchLastRace(0);
        try {
            data.observe(getViewLifecycleOwner(), result -> {
                if (result.isSuccess()) {
                    WeeklyRace raceResult = ((Result.WeeklyRaceSuccess) result).getData().get(0); // TODO: fix index requirement
                    Log.i("PastEvent", "SUCCESS");

                    showPodium(view, raceResult);
                }
            });
        } catch (Exception e) {
            loadPendingResultsLayout(view);
        }
    }

    private void loadPendingResultsLayout(View view) {
        View pendingResults = view.findViewById(R.id.pending_last_race_results);
        View results = view.findViewById(R.id.last_race_results);

        pendingResults.setVisibility(View.VISIBLE);
        results.setVisibility(View.GONE);
    }

    private void showPodium(View view, WeeklyRace raceResult) {
        Race race = raceResult.getFinalRace();
        String circuitId = race.getCircuit().getCircuitId();

        TextView raceName = view.findViewById(R.id.last_race_name);
        raceName.setText(race.getRaceName());

        ImageView trackOutline = view.findViewById(R.id.last_race_track_outline);
        trackOutline.setImageResource(Constants.EVENT_CIRCUIT.get(circuitId));

        TextView raceDate = view.findViewById(R.id.last_race_date);
        LocalDate date = race.getStartDateTime().toLocalDate();
        raceDate.setText(String.valueOf(date.getDayOfMonth()));

        // Get the first three characters of the month in capital letters
        TextView raceMonth = view.findViewById(R.id.last_race_month);
        String month = date.getMonth().toString().substring(0, 3).toUpperCase();
        raceMonth.setText(month);

        TextView roundNumber = view.findViewById(R.id.last_race_round);
        String round = "Round " + race.getRound();
        roundNumber.setText(round);

        setDriverNames(view, raceResult);

        MaterialCardView resultCard = view.findViewById(R.id.past_event_result);
        resultCard.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EventActivity.class);
            intent.putExtra("CIRCUIT_ID", race.getCircuit().getCircuitId());
            startActivity(intent);
        });
    }

    private void setDriverNames(View view, WeeklyRace raceResults) {
        for (int i = 0; i < 3; i++) {
            String driverId = raceResults.getFinalRace().getResults().get(i).getDriver().getDriverId();
            Integer driverFullName = Constants.DRIVER_FULLNAME.get(driverId);
            TextView driver = view.findViewById(Constants.LAST_RACE_DRIVER_NAME.get(i));
            driver.setText(driverFullName);
        }
    }

    private void setNextSessionCard(View view) {
        MutableLiveData<Result> data = ServiceLocator.getInstance().getRaceRepository(getActivity().getApplication(), false).fetchNextRace(0);
        try {
            data.observe(getViewLifecycleOwner(), result -> {
                if (result.isSuccess()) {
                    WeeklyRace nextRace = ((Result.WeeklyRaceSuccess) result).getData().get(0); // TODO: fix index requirement
                    Log.i("PastEvent", "SUCCESS");

                    processNextRace(view, nextRace);
                }
            });
        } catch (Exception e) {
            setSeasonEnded(view);
        }

        // CHECK IF CORRECT
        ImageView iconImageView = view.findViewById(R.id.live_icon);
        Animation pulseAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.pulse_static);
        iconImageView.startAnimation(pulseAnimation);
    }

    private void processNextRace(View view, WeeklyRace nextRace) {
        TextView nextRaceName = view.findViewById(R.id.home_next_gp_name);
        nextRaceName.setText(nextRace.getRaceName());

        ImageView nextRaceFlag = view.findViewById(R.id.home_next_gp_flag);
        String nation = nextRace.getCircuit().getLocation().getCountry();
        nextRaceFlag.setImageResource(Constants.NATION_COUNTRY_FLAG.get(nation));

        List<Session> sessions = nextRace.getSessions();
        Session nextEvent = nextRace.findNextEvent(sessions);
        if (nextEvent != null) {
            LocalDateTime eventDateTime = nextEvent.getStartDateTime();
            startCountdown(view, eventDateTime);
        }

        TextView sessionType = view.findViewById(R.id.next_session_type);
        sessionType.setText(Constants.SESSION_NAMES.get(nextEvent.getClass().getSimpleName()));

        FrameLayout nextSessionCard = view.findViewById(R.id.timer_card_countdown);
        nextSessionCard.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EventActivity.class);
            intent.putExtra("CIRCUIT_ID", nextRace.getCircuit().getCircuitId());
            startActivity(intent);
        });
    }

    private void setSeasonEnded(View view) {
        View lastRace = view.findViewById(R.id.last_race_results);
        View timerCard = view.findViewById(R.id.timer);
        View seasonEndedCard = view.findViewById(R.id.season_ended);
        View seasonResults = view.findViewById(R.id.season_results);
        View pendingResults = view.findViewById(R.id.pending_last_race_results);

        timerCard.setVisibility(View.GONE);
        lastRace.setVisibility(View.GONE);
        seasonEndedCard.setVisibility(View.VISIBLE);
        seasonResults.setVisibility(View.VISIBLE);
        pendingResults.setVisibility(View.GONE);

        buildFinalDriversStanding(seasonResults);
        buildFinalTeamsStanding(seasonResults);

        //nextRaceToProcess = false;
    }

    private void buildFinalDriversStanding(View seasonEndedCard) {
        DriverStandingsViewModel driverStandingsViewModel = new ViewModelProvider(this, new DriverStandingsViewModelFactory(ServiceLocator.getInstance().getDriverRepository(getActivity().getApplication(), false))).get(DriverStandingsViewModel.class);
        MutableLiveData<Result> data = driverStandingsViewModel.getDriverStandingsLiveData(0);//TODO get last update from shared preferences

        data.observe(getViewLifecycleOwner(), result -> {
            if (result.isSuccess()) {
                DriverStandings driverStandings = ((Result.DriverStandingsSuccess) result).getData();
                List<DriverStandingsElement> driversList = driverStandings.getDriverStandingsElements();

                for (int i = 0; i < 3; i++) {
                    DriverStandingsElement driver = driversList.get(i);

                    TextView driverName = seasonEndedCard.findViewById(Constants.HOME_SEASON_DRIVER_STANDINGS_NAME_FIELD.get(i));
                    driverName.setText(Constants.DRIVER_FULLNAME.get(driver.getDriver().getDriverId()));

                    View driverColor = seasonEndedCard.findViewById(Constants.HOME_SEASON_DRIVER_STANDINGS_COLOR_FIELD.get(i));
                    String team = Constants.DRIVER_TEAM.get(driver.getDriver().getDriverId());
                    driverColor.setBackgroundResource(Constants.TEAM_COLOR.get(team));
                }
            } else {
                Log.i(TAG, "DRIVER STANDINGS ERROR");
                loadingScreen.hideLoadingScreen();
            }
        });
    }

    private void buildFinalTeamsStanding(View seasonEndedCard) {
        ConstructorStandingsViewModel constructorStandingsViewModel = new ViewModelProvider(this, new ConstructorStandingsViewModelFactory(ServiceLocator.getInstance().getConstructorRepository(getActivity().getApplication(), false))).get(ConstructorStandingsViewModel.class);
        MutableLiveData<Result> data = constructorStandingsViewModel.getConstructorStandingsLiveData(0); // TODO get last update from shared preferences

        data.observe(getViewLifecycleOwner(), result -> {
            if (result.isSuccess()) {
                ConstructorStandings constructorStandings = ((Result.ConstructorStandingsSuccess) result).getData();
                List<ConstructorStandingsElement> constructorsList = constructorStandings.getConstructorStandings();

                for (int i = 0; i < 3; i++) {
                    ConstructorStandingsElement constructor = constructorsList.get(i);

                    TextView constructorName = seasonEndedCard.findViewById(Constants.HOME_SEASON_TEAM_STANDINGS_NAME_FIELD.get(i));
                    constructorName.setText(constructor.getConstructor().getName());

                    View constructorColor = seasonEndedCard.findViewById(Constants.HOME_SEASON_TEAM_STANDINGS_COLOR_FIELD.get(i));
                    Integer teamColor = Constants.TEAM_COLOR.get(constructor.getConstructor().getConstructorId());
                    constructorColor.setBackgroundResource(teamColor);
                }
            } else {
                Log.i(TAG, "CONSTRUCTOR STANDINGS ERROR");
                loadingScreen.hideLoadingScreen();
            }
        });
    }
    /* old method
    private void buildFinalTeamsStanding(View seasonEndedCard) {
        ConstructorRepository constructorRepository = ServiceLocator.getInstance().getConstructorRepository(getActivity().getApplication(), false);
        MutableLiveData<Result> liveData = constructorRepository.fetchConstructorStandings(0);
        Log.i(TAG, "Constructor Standings: " + liveData);
        liveData.observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.ConstructorStandingsSuccess) {
                Result.ConstructorStandingsSuccess constructorStandingsSuccess = (Result.ConstructorStandingsSuccess) result;
                ConstructorStandings constructorStandings = constructorStandingsSuccess.getData();
                Log.i(TAG, "Constructor Standings: " + constructorStandings);
                for (int i = 0; i < 3; i++) {
                    ConstructorStandingsElement constructor = constructorStandings.getConstructorStandings().get(i);

                    TextView constructorName = seasonEndedCard.findViewById(Constants.HOME_SEASON_TEAM_STANDINGS_NAME_FIELD.get(i));
                    constructorName.setText(constructor.getConstructor().getName());

                    View constructorColor = seasonEndedCard.findViewById(Constants.HOME_SEASON_TEAM_STANDINGS_COLOR_FIELD.get(i));
                    Integer teamColor = Constants.TEAM_COLOR.get(constructor.getConstructor().getConstructorId());
                    constructorColor.setBackgroundResource(teamColor);
                }
            } else if (result instanceof Result.Error) {
                Result.Error error = (Result.Error) result;
                Log.e(TAG, "Error: " + error.getMessage());
            }
        });
    }
    */


    private void startCountdown(View view, LocalDateTime eventDate) {
        LinearLayout liveIconLayout = view.findViewById(R.id.timer_live_layout);
        liveIconLayout.setVisibility(View.GONE);
        long millisUntilStart = ZonedDateTime.of(eventDate, ZoneId.systemDefault()).toInstant().toEpochMilli() - System.currentTimeMillis();
        new CountDownTimer(millisUntilStart, 1000) {
            final TextView days_counter = view.findViewById(R.id.next_days_counter);
            final TextView hours_counter = view.findViewById(R.id.next_hours_counter);
            final TextView minutes_counter = view.findViewById(R.id.next_minutes_counter);
            final TextView seconds_counter = view.findViewById(R.id.next_seconds_counter);

            public void onTick(long millisUntilFinished) {
                long days = millisUntilFinished / 86400000;
                long hours = (millisUntilFinished % 86400000) / 3600000;
                long minutes = ((millisUntilFinished % 86400000) % 3600000) / 60000;
                long seconds = (((millisUntilFinished % 86400000) % 3600000) % 60000) / 1000;

                days_counter.setText(String.valueOf(days));
                hours_counter.setText(String.valueOf(hours));
                minutes_counter.setText(String.valueOf(minutes));
                seconds_counter.setText(String.valueOf(seconds));
            }

            public void onFinish() {
                days_counter.setText("0");
                hours_counter.setText("0");
                minutes_counter.setText("0");
                seconds_counter.setText("0");
            }
        }.start();
    }


    private String getFavoriteDriverId() {
        SharedPreferencesUtils sharedPreferencesUtils = new SharedPreferencesUtils(getActivity());
        String favoriteDriver = sharedPreferencesUtils.readStringData(Constants.SHARED_PREFERENCES_FILENAME, Constants.SHARED_PREFERENCES_FAVORITE_DRIVER);
        Log.i(TAG, "Favorite Driver: " + favoriteDriver);
        return favoriteDriver;
    }

    private String getFavoriteTeamId() {
        SharedPreferencesUtils sharedPreferencesUtils = new SharedPreferencesUtils(getActivity());
        String favoriteTeam = sharedPreferencesUtils.readStringData(Constants.SHARED_PREFERENCES_FILENAME, Constants.SHARED_PREFERENCES_FAVORITE_TEAM);
        Log.i(TAG, "Favorite Team: " + favoriteTeam);
        return favoriteTeam;
    }

    private void setFavouriteDriverCard(View view) {
        DriverStandingsViewModel driverStandingsViewModel = new ViewModelProvider(this, new DriverStandingsViewModelFactory(ServiceLocator.getInstance().getDriverRepository(getActivity().getApplication(), false))).get(DriverStandingsViewModel.class);
        MutableLiveData<Result> data = driverStandingsViewModel.getDriverStandingsLiveData(0);//TODO get last update from shared preferences

        data.observe(getViewLifecycleOwner(), result -> {
            if (result.isSuccess()) {
                DriverStandings driverStandings = ((Result.DriverStandingsSuccess) result).getData();
                List<DriverStandingsElement> driversList = driverStandings.getDriverStandingsElements();

                DriverStandingsElement favouriteDriver = driverStandingsViewModel.getDriverStandingsElement(driversList, getFavoriteDriverId());
                if (favouriteDriver == null) {
                    Log.i(TAG, "Favorite Driver not found");
                    Intent intent = new Intent(getActivity(), ProfileActivity.class);
                    startActivity(intent);
                }
                //Log.i(TAG, "Favorite Driver: " + favouriteDriver.toString());
                buildDriverCard(view, favouriteDriver);
            } else {
                Log.i(TAG, "DRIVER STANDINGS ERROR");
                loadingScreen.hideLoadingScreen();
            }
        });
    }

    private void buildDriverCard(View view, DriverStandingsElement standingElement) {
        TextView driverName = view.findViewById(R.id.favourite_driver_name);
        driverName.setText(standingElement.getDriver().getGivenName() + " " + standingElement.getDriver().getFamilyName());

        String driverNationality = standingElement.getDriver().getNationality();
        ImageView driverFlag = view.findViewById(R.id.favourite_driver_flag);
        driverFlag.setImageResource(Constants.NATION_COUNTRY_FLAG.get(Constants.NATIONALITY_NATION.get(driverNationality)));

        TextView nationality = view.findViewById(R.id.favourite_driver_nationality);
        nationality.setText(Constants.NATIONALITY_ABBREVIATION.get(driverNationality));

        ImageView driverImage = view.findViewById(R.id.favourite_driver_pic);
        driverImage.setImageResource(Constants.DRIVER_IMAGE.get(standingElement.getDriver().getDriverId()));

        TextView driverPosition = view.findViewById(R.id.favourite_driver_position);
        driverPosition.setText(standingElement.getPosition());

        TextView driverPoints = view.findViewById(R.id.favourite_driver_points);
        driverPoints.setText(standingElement.getPoints());

        //set favourite driver card color
        RelativeLayout driverCard = view.findViewById(R.id.favourite_driver_layout);
        //driverCard.setBackgroundResource(Constants.TEAM_COLOR.get(Constants.DRIVER_TEAM.get(standingElement.getDriver().getDriverId())));

        driverImage.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), DriverBioActivity.class);
            intent.putExtra("DRIVER_ID", standingElement.getDriver().getDriverId());
            startActivity(intent);
        });

        MaterialCardView driverRank = view.findViewById(R.id.favourite_driver_rank);
        driverRank.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), DriversStandingActivity.class);
            intent.putExtra("DRIVER_ID", standingElement.getDriver().getDriverId());
            startActivity(intent);
        });
    }

    private void setFavouriteConstructorCard(View view) {
        // FIX: The implementation with the ViewModel is not working

        /*ConstructorStandingsViewModel constructorStandingsViewModel = new ViewModelProvider(this, new ConstructorStandingsViewModelFactory(ServiceLocator.getInstance().getConstructorRepository(getActivity().getApplication(), false))).get(ConstructorStandingsViewModel.class);
        MutableLiveData<Result> data = constructorStandingsViewModel.getConstructorStandingsLiveData(0);//TODO get last update from shared preferences

        if(data != null){
            data.observe(getViewLifecycleOwner(), result -> {
                if (result.isSuccess()) {
                    Log.i(TAG, "CONSTRUCTOR STANDINGS SUCCESS");

                    ConstructorStandings constructorStandings = ((Result.ConstructorStandingsSuccess) result).getData();
                    List<ConstructorStandingsElement> constructorsList = constructorStandings.getConstructorStandings();

                    ConstructorStandingsElement favouriteConstructor = constructorStandingsViewModel.getConstructorStandingsElement(constructorsList, Constants.FAVOURITE_TEAM);
                    buildConstructorCard(view, favouriteConstructor);
                } else {
                    Log.i(TAG, "CONSTRUCTOR STANDINGS ERROR");
                    loadingScreen.hideLoadingScreen();
                }
            });
        }*/

        /*ConstructorRepository constructorRepository = ServiceLocator.getInstance().getConstructorRepository(getActivity().getApplication(), false);
        MutableLiveData<Result> liveData = constructorRepository.fetchConstructorStandings(0);
        Log.i(TAG, "Constructor Standings: " + liveData);
        liveData.observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.ConstructorStandingsSuccess) {
                Result.ConstructorStandingsSuccess constructorStandingsSuccess = (Result.ConstructorStandingsSuccess) result;
                ConstructorStandings constructorStandings = constructorStandingsSuccess.getData();
                Log.i(TAG, "Constructor Standings: " + constructorStandings);
                for (ConstructorStandingsElement standingElement : constructorStandings.getConstructorStandings()) {
                    Log.i(TAG, "Constructor (standing element): " + standingElement.getConstructor().getConstructorId());
                    if (standingElement.getConstructor().getConstructorId().equals(getFavoriteTeamId())) {
                        buildConstructorCard(view, standingElement);
                        break;
                    }
                }
            } else if (result instanceof Result.Error) {
                Result.Error error = (Result.Error) result;
                Log.e(TAG, "Error: " + error.getMessage());
            }
        });*/

        ConstructorStandingsViewModel constructorStandingsViewModel = new ViewModelProvider(this, new ConstructorStandingsViewModelFactory(ServiceLocator.getInstance().getConstructorRepository(getActivity().getApplication(), false))).get(ConstructorStandingsViewModel.class);
        MutableLiveData<Result> data = constructorStandingsViewModel.getConstructorStandingsLiveData(0); // TODO get last update from shared preferences

        data.observe(getViewLifecycleOwner(), result -> {
            if (result.isSuccess()) {
                ConstructorStandings constructorStandings = ((Result.ConstructorStandingsSuccess) result).getData();
                List<ConstructorStandingsElement> constructorsList = constructorStandings.getConstructorStandings();

                ConstructorStandingsElement favouriteConstructor = constructorStandingsViewModel.getConstructorStandingsElement(constructorsList, getFavoriteTeamId());
                Log.i(TAG, "Favorite Constructor: " + favouriteConstructor.toString());
                buildConstructorCard(view, favouriteConstructor);
            } else {
                Log.i(TAG, "CONSTRUCTOR STANDINGS ERROR");
                loadingScreen.hideLoadingScreen();
            }
        });
    }

    private void buildConstructorCard(View view, ConstructorStandingsElement standingElement) {
        TextView constructorName = view.findViewById(R.id.favourite_constructor_name);
        constructorName.setText(standingElement.getConstructor().getName());

        ImageView constructorCar = view.findViewById(R.id.favourite_constructor_car);
        constructorCar.setImageResource(Constants.TEAM_CAR.get(standingElement.getConstructor().getConstructorId().toLowerCase()));

        TextView constructorPosition = view.findViewById(R.id.favourite_constructor_position);
        constructorPosition.setText(standingElement.getPosition());

        TextView constructorPoints = view.findViewById(R.id.favourite_constructor_points);
        constructorPoints.setText(standingElement.getPoints());

        ImageView constructorFlag = view.findViewById(R.id.favourite_constructor_flag);
        String nationality = standingElement.getConstructor().getNationality();
        constructorFlag.setImageResource(Constants.NATION_COUNTRY_FLAG.get(Constants.NATIONALITY_NATION.get(nationality)));

        TextView constructorNationality = view.findViewById(R.id.favourite_constructor_nationality);
        constructorNationality.setText(Constants.NATIONALITY_ABBREVIATION.get(nationality));

        RelativeLayout constructorCard = view.findViewById(R.id.favourite_constructor_layout);
        //constructorCard.setBackgroundResource(Constants.TEAM_COLOR.get(standingElement.getConstructor().getConstructorId().toLowerCase()));

        constructorCar.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ConstructorBioActivity.class);
            intent.putExtra("TEAM_ID", standingElement.getConstructor().getConstructorId());
            startActivity(intent);
        });

        MaterialCardView teamRank = view.findViewById(R.id.favourite_constructor_rank);
        teamRank.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ConstructorsStandingActivity.class);
            intent.putExtra("TEAM_ID", standingElement.getConstructor().getConstructorId());
            startActivity(intent);
        });

        loadingScreen.hideLoadingScreen();
    }
}