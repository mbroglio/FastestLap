package com.the_coffe_coders.fastestlap.ui.event;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.domain.race.Race;
import com.the_coffe_coders.fastestlap.domain.race.RaceAPIResponse;
import com.the_coffe_coders.fastestlap.domain.race_result.Result;
import com.the_coffe_coders.fastestlap.domain.race_result.ResultsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.race.Session;
import com.the_coffe_coders.fastestlap.ui.ErgastAPI;
import com.the_coffe_coders.fastestlap.ui.bio.ConstructorBioActivity;
import com.the_coffe_coders.fastestlap.ui.bio.TrackBioActivity;
import com.the_coffe_coders.fastestlap.utils.Constants;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.utils.JSONParserUtils;

import org.threeten.bp.LocalDate;
import org.threeten.bp.Year;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

import java.io.IOException;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class EventActivity extends AppCompatActivity {
    private static final String TAG = "EventActivity";
    private String BASE_URL = "https://api.jolpi.ca/ergast/f1/";
    private ErgastAPI ergastApi;
    private String circuitId;
    private final ZoneId localZone = ZoneId.systemDefault();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event);

        circuitId = getIntent().getStringExtra("CIRCUIT_ID");
        Log.i(TAG, "Circui ID: " + circuitId);

        //setDynamicDimensions();

        Year currentYear = Year.now();
        BASE_URL += currentYear + "/";
        BASE_URL += "circuits/" + circuitId + "/";
        Log.i(TAG, "Base URL: " + BASE_URL);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();

        ergastApi = retrofit.create(ErgastAPI.class);

        getEventInfo();
    }

    private void getEventInfo() {
        ergastApi.getRaces().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                Log.i(TAG, "Response: " + response);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseBody = response.body().string();
                        JsonObject jsonResponse = new Gson().fromJson(responseBody, JsonObject.class);
                        JsonObject mrdata = jsonResponse.getAsJsonObject("MRData");

                        JSONParserUtils parser = new JSONParserUtils(EventActivity.this);
                        RaceAPIResponse raceSchedule = parser.parseRace(mrdata);

                        processRaceData(raceSchedule);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    Log.e(TAG, "Response body is null");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable throwable) {
                Log.e(TAG, "Error fetching data", throwable);
            }
        });
    }

    private void processRaceData(RaceAPIResponse raceSchedule) {
        Race race = raceSchedule.getRaceTable().getRaces().get(0);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        toolbar.setTitle(race.getRaceName());

        ImageView countryFlag = findViewById(R.id.country_flag);
        String nation = race.getCircuit().getLocation().getCountry();
        Integer flag = Constants.NATION_COUNTRY_FLAG.get(nation);
        countryFlag.setImageResource(flag);

        ImageView trackMap = findViewById(R.id.track_outline_image);
        Integer outline = Constants.EVENT_CIRCUIT.get(circuitId);
        trackMap.setImageResource(outline);

        TextView roundNumber = findViewById(R.id.round_number);
        String round = "Round " + race.getRound();
        roundNumber.setText(round);

        TextView seasonYear = findViewById(R.id.year);
        seasonYear.setText(raceSchedule.getRaceTable().getSeason());

        TextView name = findViewById(R.id.gp_name);
        name.setText(Constants.TRACK_LONG_GP_NAME.get(circuitId));

        setEventImage(circuitId);

        TextView eventDate = findViewById(R.id.event_date);
        eventDate.setText(race.getDateInterval());

        List<Session> sessions = race.getSessions();
        Session nextEvent = race.findNextEvent(sessions);
        Boolean underway = false;
        Log.i(TAG, "Race Underway: " + race.isUnderway());
        for(Session session : sessions) {
            if(session.isUnderway()) {
                setLiveSession();
                underway = true;
            }
        }
        if (nextEvent != null && !underway) {
            ZonedDateTime eventDateTime = nextEvent.getStartDateTime();
            startCountdown(eventDateTime);
        } else if (!underway) {
            showResults(raceSchedule);
        }

        createWeekSchedule(sessions);
    }

    private void setEventImage(String circuitId) {
        Integer eventImage = Constants.EVENT_PICTURE.get(circuitId);
        Drawable picture = ContextCompat.getDrawable(this, eventImage);
        picture.setAlpha(76);

        LinearLayout eventCard = findViewById(R.id.event_card);
        eventCard.setBackground(picture);
    }

    private void setLiveSession() {
        FrameLayout liveSessionContainer = findViewById(R.id.session_status_card_container);
        View liveSession = findViewById(R.id.event_live_card);
        View noLiveSession = findViewById(R.id.event_not_live_card);

        // Hide countdown view and show results view
        liveSession.setVisibility(View.VISIBLE);
        noLiveSession.setVisibility(View.GONE);

        ImageView liveIcon = findViewById(R.id.live_icon);
        Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse_dynamic);
        liveIcon.startAnimation(pulse);
    }

    private void startCountdown(ZonedDateTime eventDate) {
        long millisUntilStart = eventDate.toInstant().toEpochMilli() - ZonedDateTime.now(ZoneId.of("UTC")).toInstant().toEpochMilli();
        new CountDownTimer(millisUntilStart, 1000) {
            TextView days_counter = findViewById(R.id.next_days_counter);
            TextView hours_counter = findViewById(R.id.next_hours_counter);
            TextView minutes_counter = findViewById(R.id.next_minutes_counter);
            TextView seconds_counter = findViewById(R.id.next_seconds_counter);

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

    private void showResults(RaceAPIResponse raceSchedule) {
        FrameLayout eventCardContainer = findViewById(R.id.event_card_container);
        View countdownView = findViewById(R.id.timer_card_countdown);
        View resultsView = findViewById(R.id.timer_card_results);
        View pendingResultsView = findViewById(R.id.timer_card_pending_results);
        View raceCancelledView = findViewById(R.id.timer_card_race_cancelled);

        // Hide countdown view and show results view
        countdownView.setVisibility(View.GONE);
        resultsView.setVisibility(View.VISIBLE);

        // You can add logic here to populate the results view with actual data
        processRaceResults(raceSchedule);

        // Set podium cricuit image
        ImageView trackOutline = findViewById(R.id.track_outline_image);
        trackOutline.setImageResource(Constants.EVENT_CIRCUIT.get(circuitId));

        // Set listener to track outline and define a method onclick
        LinearLayout track = findViewById(R.id.track_outline_layout);
        track.setOnClickListener(v -> {
            Intent intent = new Intent(EventActivity.this, TrackBioActivity.class);
            intent.putExtra("CIRCUIT_ID", circuitId);
            startActivity(intent);
        });
    }

    private void processRaceResults(RaceAPIResponse raceSchedule) {
        // Get results from API https://ergast.com/api/f1/current/{roundNumber}/results.json
        String roundNumber = raceSchedule.getRaceTable().getRaces().get(0).getRound();
        BASE_URL = "https://ergast.com/api/f1/current/" + roundNumber + "/";
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();

        ergastApi = retrofit.create(ErgastAPI.class);

        ergastApi.getResults().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                Log.i(TAG, "Response: " + response);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseBody = response.body().string();
                        JsonObject jsonResponse = new Gson().fromJson(responseBody, JsonObject.class);
                        JsonObject mrdata = jsonResponse.getAsJsonObject("MRData");

                        JSONParserUtils parser = new JSONParserUtils(EventActivity.this);
                        ResultsAPIResponse raceResult = parser.parseRaceResults(mrdata);

                        showPodium(raceResult);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    Log.e(TAG, "Response body is null");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable throwable) {
                Log.e(TAG, "Error fetching data", throwable);
            }
        });
    }

    private void showPodium(ResultsAPIResponse raceResult) {
        List<Result> results = raceResult.getRaceTable().getRaces().get(0).getResults();
        for (int i = 0; i < 3; i++) {
            String driverId = results.get(i).getDriver().getDriverId();
            String teamId = results.get(i).getConstructor().getConstructorId();

            LinearLayout teamColor = findViewById(Constants.PODIUM_TEAM_COLOR.get(i));
            TextView driverName = findViewById(Constants.PODIUM_DRIVER_NAME.get(i));

            driverName.setText(Constants.DRIVER_FULLNAME.get(driverId));
            teamColor.setBackgroundColor(ContextCompat.getColor(this, Constants.TEAM_COLOR.get(teamId)));
        }
    }

    private void createWeekSchedule(List<Session> sessions) {
        String sessionId;
        for (Session session : sessions) {
            sessionId = session.getSessionId();
            TextView sessionName = findViewById(Constants.SESSION_NAME_FIELD.get(sessionId));
            sessionName.setText(Constants.SESSION_NAMES.get(session.getSessionId()));

            TextView sessionDay = findViewById(Constants.SESSION_DAY_FIELD.get(sessionId));
            sessionDay.setText(Constants.SESSION_DAY.get(session.getSessionId()));

            TextView sessionTime = findViewById(Constants.SESSION_TIME_FIELD.get(sessionId));
            if (session.getSessionId().equals("Race"))
                sessionTime.setText(session.getStartingTime());
            else
                sessionTime.setText(session.getTime());

            setChequeredFlag(session);
        }
    }

    private void setChequeredFlag(Session session) {
        if (session.isFinished()) {
            ImageView flag = findViewById(Constants.SESSION_FLAG_FIELD.get(session.getSessionId()));
            flag.setVisibility(View.VISIBLE);

            RelativeLayout currentSession = findViewById(Constants.SESSION_ROW.get(session.getSessionId()));
            currentSession.setClickable(true);
            currentSession.setFocusable(true);
            currentSession.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.i(TAG, "session clicked");
                }
            });
        }
    }
}