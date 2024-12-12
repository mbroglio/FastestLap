package com.the_coffe_coders.fastestlap.ui.home.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
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

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.constructor.ConstructorStanding;
import com.the_coffe_coders.fastestlap.domain.driver.DriverStanding;
import com.the_coffe_coders.fastestlap.api.StandingsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.race.Race;
import com.the_coffe_coders.fastestlap.api.RaceAPIResponse;
import com.the_coffe_coders.fastestlap.domain.race.Session;
import com.the_coffe_coders.fastestlap.api.ResultsAPIResponse;
import com.the_coffe_coders.fastestlap.ui.ErgastAPI;
import com.the_coffe_coders.fastestlap.ui.bio.ConstructorBioActivity;
import com.the_coffe_coders.fastestlap.ui.bio.DriverBioActivity;
import com.the_coffe_coders.fastestlap.ui.event.EventActivity;
import com.the_coffe_coders.fastestlap.ui.standing.ConstructorsStandingActivity;
import com.the_coffe_coders.fastestlap.ui.standing.DriversStandingActivity;
import com.the_coffe_coders.fastestlap.utils.Constants;
import com.the_coffe_coders.fastestlap.utils.JSONParserUtils;

import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

import java.io.IOException;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment {
    private final String TAG = "HomeFragment";
    private ErgastAPI ergastAPI;
    private final ZoneId localZone = ZoneId.systemDefault();

    private View loadingScreen;
    private TextView loadingText;
    private Handler handler = new Handler();
    private int dotCount = 0;
    private boolean addingDots = true;
    private boolean raceToProcess = true;
    private boolean nextRaceToProcess = true;
    private boolean driverToProcess = true;
    private boolean constructorToProcess = true;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Loading screen logic
        loadingScreen = view.findViewById(R.id.loading_screen);
        loadingText = view.findViewById(R.id.loading_text);
        ImageView loadingWheel = view.findViewById(R.id.loading_wheel);

        // Start the rotation animation
        Animation rotateAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.rotate);
        loadingWheel.startAnimation(rotateAnimation);

        // Show loading screen initially
        showLoadingScreen();
        Log.i(TAG, "Loading screen shown");

        // Start the dots animation
        handler.post(dotRunnable);

        setLastRaceCard(view);
        setNextSessionCard(view);
        setFavouriteDriverCard(view);
        setFavouriteConstructorCard(view);

        return view;
    }

    private void setLastRaceCard(View view) {
        //String BASE_URL = "https://api.jolpi.ca/ergast/f1/";
        String BASE_URL = "https://ergast.com/api/f1/";
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ergastAPI = retrofit.create(ErgastAPI.class);

        ergastAPI.getLastRaceResults().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                Log.i(TAG, "Response: " + response);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseBody = response.body().string();
                        JsonObject jsonObject = new Gson().fromJson(responseBody, JsonObject.class);
                        JsonObject mrdata = jsonObject.getAsJsonObject("MRData");

                        JSONParserUtils parser = new JSONParserUtils(getContext());
                        ResultsAPIResponse raceResults = parser.parseRaceResults(mrdata);

                        if (!raceResults.getRaceTable().getRaces().isEmpty()) {
                            showPodium(view, raceResults);
                            raceToProcess = false;
                            checkIfAllDataLoaded();
                        } else {
                            loadPendingResultsLayout(view);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    raceToProcess = false;
                    checkIfAllDataLoaded();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                raceToProcess = false;
                checkIfAllDataLoaded();
            }
        });
    }

    private void loadPendingResultsLayout(View view) {
        View pendingResults = view.findViewById(R.id.pending_last_race_results);
        View results = view.findViewById(R.id.last_race_results);

        pendingResults.setVisibility(View.VISIBLE);
        results.setVisibility(View.GONE);

        raceToProcess = false;
        checkIfAllDataLoaded();
    }

    private void showPodium(View view, ResultsAPIResponse raceResults) {
        com.the_coffe_coders.fastestlap.domain.race_result.Race race = raceResults.getRaceTable().getRaces().get(0);
        String circuitId = race.getCircuit().getCircuitId();

        TextView raceName = view.findViewById(R.id.last_race_name);
        raceName.setText(race.getRaceName());

        ImageView trackOutline = view.findViewById(R.id.last_race_track_outline);
        trackOutline.setImageResource(Constants.EVENT_CIRCUIT.get(circuitId));

        TextView raceDate = view.findViewById(R.id.last_race_date);
        LocalDate date = LocalDate.parse(race.getDate());
        raceDate.setText(String.valueOf(date.getDayOfMonth()));

        // Get the first three characters of the month in capital letters
        TextView raceMonth = view.findViewById(R.id.last_race_month);
        String month = date.getMonth().toString().substring(0, 3).toUpperCase();
        raceMonth.setText(month);

        TextView roundNumber = view.findViewById(R.id.last_race_round);
        String round = "Round " + race.getRound();
        roundNumber.setText(round);

        setDriverNames(view, raceResults);

        MaterialCardView resultCard = view.findViewById(R.id.past_event_result);
        resultCard.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EventActivity.class);
            intent.putExtra("CIRCUIT_ID", race.getCircuit().getCircuitId());
            startActivity(intent);
        });
    }

    private void setDriverNames(View view, ResultsAPIResponse raceResults) {
        for (int i = 0; i < 3; i++) {
            String driverId = raceResults.getRaceTable().getRaces().get(0).getResults().get(i).getDriver().getDriverId();
            Integer driverFullName = Constants.DRIVER_FULLNAME.get(driverId);
            TextView driver = view.findViewById(Constants.LAST_RACE_DRIVER_NAME.get(i));
            driver.setText(driverFullName);
        }
    }


    private void setNextSessionCard(View view) {
        String BASE_URL = "https://api.jolpi.ca/ergast/f1/";
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ergastAPI = retrofit.create(ErgastAPI.class);

        ergastAPI.getNextRace().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                Log.i(TAG, "Response: " + response);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseBody = response.body().string();
                        JsonObject jsonObject = new Gson().fromJson(responseBody, JsonObject.class);
                        JsonObject mrdata = jsonObject.getAsJsonObject("MRData");

                        JSONParserUtils parser = new JSONParserUtils(getContext());
                        RaceAPIResponse raceSchedule = parser.parseRace(mrdata);

                        if (!raceSchedule.getRaceTable().getRaces().isEmpty()) {
                            processNextRace(view, raceSchedule);
                            nextRaceToProcess = false;
                            checkIfAllDataLoaded();
                        } else {
                            setSeasonEnded(view);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    nextRaceToProcess = false;
                    checkIfAllDataLoaded();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                nextRaceToProcess = false;
                checkIfAllDataLoaded();
            }
        });

        ImageView iconImageView = view.findViewById(R.id.live_icon);
        Animation pulseAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.pulse_static);
        iconImageView.startAnimation(pulseAnimation);
    }

    private void processNextRace(View view, RaceAPIResponse raceSchedule) {
        Race race = raceSchedule.getRaceTable().getRaces().get(0);

        TextView nextRaceName = view.findViewById(R.id.home_next_gp_name);
        nextRaceName.setText(race.getRaceName());

        ImageView nextRaceFlag = view.findViewById(R.id.home_next_gp_flag);
        String nation = race.getCircuit().getLocation().getCountry();
        nextRaceFlag.setImageResource(Constants.NATION_COUNTRY_FLAG.get(nation));

        List<Session> sessions = race.getSessions();
        Session nextEvent = race.findNextEvent(sessions);
        if (nextEvent != null) {
            ZonedDateTime eventDateTime = nextEvent.getStartDateTime();
            startCountdown(view, eventDateTime);
        }

        TextView sessionType = view.findViewById(R.id.next_session_type);
        sessionType.setText(Constants.SESSION_NAMES.get(nextEvent.getSessionId()));

        FrameLayout nextSessionCard = view.findViewById(R.id.timer_card_countdown);
        nextSessionCard.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EventActivity.class);
            intent.putExtra("CIRCUIT_ID", race.getCircuit().getCircuitId());
            startActivity(intent);
        });
    }

    private void setSeasonEnded(View view) {
        View timerCard = view.findViewById(R.id.timer);
        View seasonEndedCard = view.findViewById(R.id.season_ended);

        timerCard.setVisibility(View.GONE);
        seasonEndedCard.setVisibility(View.VISIBLE);

        nextRaceToProcess = false;
        checkIfAllDataLoaded();
    }

    private void startCountdown(View view, ZonedDateTime eventDate) {
        LinearLayout liveIconLayout = view.findViewById(R.id.timer_live_layout);
        liveIconLayout.setVisibility(View.GONE);
        long millisUntilStart = eventDate.toInstant().toEpochMilli() - ZonedDateTime.now(ZoneId.of("UTC")).toInstant().toEpochMilli();
        new CountDownTimer(millisUntilStart, 1000) {
            TextView days_counter = view.findViewById(R.id.next_days_counter);
            TextView hours_counter = view.findViewById(R.id.next_hours_counter);
            TextView minutes_counter = view.findViewById(R.id.next_minutes_counter);
            TextView seconds_counter = view.findViewById(R.id.next_seconds_counter);

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

    private void setFavouriteDriverCard(View view) {
        String BASE_URL = "https://api.jolpi.ca/ergast/f1/current/";
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ergastAPI = retrofit.create(ErgastAPI.class);

        ergastAPI.getDriverStandings().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                Log.i(TAG, "Response: " + response);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseString = response.body().string();
                        JsonObject jsonResponse = new Gson().fromJson(responseString, JsonObject.class);
                        JsonObject mrdata = jsonResponse.getAsJsonObject("MRData");

                        JSONParserUtils jsonParserUtils = new JSONParserUtils(getContext());
                        StandingsAPIResponse standing = jsonParserUtils.parseDriverStandings(mrdata);

                        processStanding(view, standing);
                        driverToProcess = false;
                        checkIfAllDataLoaded();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    driverToProcess = false;
                    checkIfAllDataLoaded();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                driverToProcess = false;
                checkIfAllDataLoaded();
            }
        });
    }

    private void processStanding(View view, StandingsAPIResponse standing) {
        List<DriverStanding> standings = standing.getStandingsTable().getStandingsLists().get(0).getDriverStandings();

        for (DriverStanding standingElement : standings) {
            if (standingElement.getDriver().getDriverId().equals(Constants.FAVOURITE_DRIVER)) {
                buildDriverCard(view, standingElement);
            }
        }

        MaterialCardView driverRank = view.findViewById(R.id.favourite_driver_rank);
        driverRank.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), DriversStandingActivity.class);
            intent.putExtra("DRIVER_ID", Constants.FAVOURITE_DRIVER);
            startActivity(intent);
        });
    }

    private void buildDriverCard(View view, DriverStanding standingElement) {
        TextView driverName = view.findViewById(R.id.favourite_driver_name);
        driverName.setText(Constants.DRIVER_FULLNAME.get(Constants.FAVOURITE_DRIVER));

        String driverNationality = standingElement.getDriver().getNationality();
        ImageView driverFlag = view.findViewById(R.id.favourite_driver_flag);
        driverFlag.setImageResource(Constants.NATION_COUNTRY_FLAG.get(Constants.NATIONALITY_NATION.get(driverNationality)));

        TextView nationality = view.findViewById(R.id.favourite_driver_nationality);
        nationality.setText(Constants.NATIONALITY_ABBREVIATION.get(driverNationality));

        ImageView driverImage = view.findViewById(R.id.favourite_driver_pic);
        driverImage.setImageResource(Constants.DRIVER_IMAGE.get(Constants.FAVOURITE_DRIVER));

        TextView driverPosition = view.findViewById(R.id.favourite_driver_position);
        driverPosition.setText(standingElement.getPosition());

        TextView driverPoints = view.findViewById(R.id.favourite_driver_points);
        driverPoints.setText(standingElement.getPoints());

        //set favourite driver card color
        RelativeLayout driverCard = view.findViewById(R.id.favourite_driver_layout);
        driverCard.setBackgroundResource(Constants.TEAM_COLOR.get(Constants.DRIVER_TEAM.get(Constants.FAVOURITE_DRIVER)));

        driverImage.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), DriverBioActivity.class);
            intent.putExtra("DRIVER_ID", Constants.FAVOURITE_DRIVER);
            startActivity(intent);
        });
    }

    private void setFavouriteConstructorCard(View view) {
        String BASE_URL = "https://api.jolpi.ca/ergast/f1/current/";
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ergastAPI = retrofit.create(ErgastAPI.class);

        ergastAPI.getConstructorStandings().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                Log.i(TAG, "Response: " + response);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseString = response.body().string();
                        JsonObject jsonResponse = new Gson().fromJson(responseString, JsonObject.class);
                        JsonObject mrdata = jsonResponse.getAsJsonObject("MRData");

                        JSONParserUtils jsonParserUtils = new JSONParserUtils(getContext());
                        com.the_coffe_coders.fastestlap.domain.constructor.StandingsAPIResponse standing = jsonParserUtils.parseConstructorStandings(mrdata);

                        processConstructorStanding(view, standing);
                        constructorToProcess = false;
                        checkIfAllDataLoaded();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    constructorToProcess = false;
                    checkIfAllDataLoaded();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                constructorToProcess = false;
                checkIfAllDataLoaded();
            }
        });
    }

    private void processConstructorStanding(View view, com.the_coffe_coders.fastestlap.domain.constructor.StandingsAPIResponse standing) {
        List<com.the_coffe_coders.fastestlap.domain.constructor.ConstructorStanding> standings = standing.getStandingsTable().getStandingsLists().get(0).getConstructorStandings();

        for (com.the_coffe_coders.fastestlap.domain.constructor.ConstructorStanding standingElement : standings){
            if(standingElement.getConstructor().getConstructorId().equals(Constants.FAVOURITE_TEAM)){
                buildConstructorCard(view, standingElement);
            }
        }

        MaterialCardView teamRank = view.findViewById(R.id.favourite_constructor_rank);
        teamRank.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ConstructorsStandingActivity.class);
            intent.putExtra("TEAM_ID", Constants.FAVOURITE_TEAM);
            startActivity(intent);
        });
    }

    private void buildConstructorCard(View view, ConstructorStanding standingElement) {
        TextView constructorName = view.findViewById(R.id.favourite_constructor_name);
        constructorName.setText(standingElement.getConstructor().getName());

        ImageView constructorCar = view.findViewById(R.id.favourite_constructor_car);
        constructorCar.setImageResource(Constants.TEAM_CAR.get(Constants.FAVOURITE_TEAM));

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
        constructorCard.setBackgroundResource(Constants.TEAM_COLOR.get(standingElement.getConstructor().getName().toLowerCase()));

        constructorCar.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ConstructorBioActivity.class);
            intent.putExtra("DRIVER_NAME", Constants.FAVOURITE_TEAM);
            startActivity(intent);
        });
    }

    private void showLoadingScreen() {
        loadingScreen.setVisibility(View.VISIBLE);
    }

    private void hideLoadingScreen() {
        loadingScreen.setVisibility(View.GONE);
        handler.removeCallbacks(dotRunnable);
    }

    private Runnable dotRunnable = new Runnable() {
        @Override
        public void run() {
            if (addingDots) {
                dotCount++;
                if (dotCount == 4) {
                    addingDots = false;
                }
            } else {
                dotCount--;
                if (dotCount == 0) {
                    addingDots = true;
                }
            }
            StringBuilder dots = new StringBuilder();
            for (int i = 0; i < dotCount; i++) {
                dots.append(".");
            }
            loadingText.setText("LOADING" + dots);
            handler.postDelayed(this, 500);
        }
    };

    private void checkIfAllDataLoaded() {
        if (!raceToProcess && !nextRaceToProcess && !driverToProcess && !constructorToProcess) {
            handler.postDelayed(this::hideLoadingScreen, 500); // 500 milliseconds delay
        }
    }
}

