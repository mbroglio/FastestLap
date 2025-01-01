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
import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.ui.event.EventActivity;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.LoadingScreen;

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

public class HomeFragment extends Fragment {
    private final String TAG = "HomeFragment";
    private final ZoneId localZone = ZoneId.systemDefault();


    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    /*

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Show loading screen initially
        loadingScreen.showLoadingScreen();


        setLastRaceCard(view);
        setNextSessionCard(view);
        setFavouriteDriverCard(view);
        setFavouriteConstructorCard(view);

        return view;
    }

    private void setLastRaceCard(View view) {
        showPodium(view, WeeklyRace raceResults);
    }

    private void loadPendingResultsLayout(View view) {
        View pendingResults = view.findViewById(R.id.pending_last_race_results);
        View results = view.findViewById(R.id.last_race_results);

        pendingResults.setVisibility(View.VISIBLE);
        results.setVisibility(View.GONE);

        raceToProcess = false;
        checkIfAllDataLoaded();
    }

    private void showPodium(View view, WeeklyRace raceResults) {
        Race race = raceResults.getFinalRace();
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

        setDriverNames(view, raceResults);

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
            }
        });

        ImageView iconImageView = view.findViewById(R.id.live_icon);
        Animation pulseAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.pulse_static);
        iconImageView.startAnimation(pulseAnimation);
    }

    private void processNextRace(View view, RaceAPIResponse raceSchedule) {
        WeeklyRace weeklyRace = null;

        TextView nextRaceName = view.findViewById(R.id.home_next_gp_name);
        nextRaceName.setText(weeklyRace.getRaceName());

        ImageView nextRaceFlag = view.findViewById(R.id.home_next_gp_flag);
        String nation = weeklyRace.getCircuit().getLocation().getCountry();
        nextRaceFlag.setImageResource(Constants.NATION_COUNTRY_FLAG.get(nation));

        List<Session> sessions = weeklyRace.getSessions();
        Session nextEvent = weeklyRace.findNextEvent(sessions);
        if (nextEvent != null) {
            ZonedDateTime eventDateTime = nextEvent.getStartDateTime();
            startCountdown(view, eventDateTime);
        }

        TextView sessionType = view.findViewById(R.id.next_session_type);
        sessionType.setText(Constants.SESSION_NAMES.get(nextEvent.getSessionId()));

        FrameLayout nextSessionCard = view.findViewById(R.id.timer_card_countdown);
        nextSessionCard.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EventActivity.class);
            intent.putExtra("CIRCUIT_ID", weeklyRace.getCircuit().getCircuitId());
            startActivity(intent);
        });
    }

    private void setSeasonEnded(View view) {
        View timerCard = view.findViewById(R.id.timer);
        View seasonEndedCard = view.findViewById(R.id.season_ended);

        timerCard.setVisibility(View.GONE);
        seasonEndedCard.setVisibility(View.VISIBLE);

        nextRaceToProcess = false;
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
        processStanding(view, new DriverStandingsElement());
    }

    private void processStanding(View view, DriverStandingsElement standing) {
        buildDriverCard(view, standing);
        MaterialCardView driverRank = view.findViewById(R.id.favourite_driver_rank);
        driverRank.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), DriversStandingActivity.class);
            intent.putExtra("DRIVER_ID", Constants.FAVOURITE_DRIVER);
            startActivity(intent);
        });
    }

    private void buildDriverCard(View view, DriverStandingsElement standingElement) {
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
        ConstructorStandingsElement standingsElement = new ConstructorStandingsElement();
        processConstructorStanding(view, standingsElement);
        constructorToProcess = false;
    }

    private void processConstructorStanding(View view, ConstructorStandingsElement standing) {
        buildConstructorCard(view, standing);

        MaterialCardView teamRank = view.findViewById(R.id.favourite_constructor_rank);
        teamRank.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ConstructorsStandingActivity.class);
            intent.putExtra("TEAM_ID", Constants.FAVOURITE_TEAM);
            startActivity(intent);
        });
    }

    private void buildConstructorCard(View view, ConstructorStandingsElement standingElement) {
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
    */
}