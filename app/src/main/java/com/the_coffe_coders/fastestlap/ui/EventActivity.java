package com.the_coffe_coders.fastestlap.ui;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.domain.race_result.Result;
import com.the_coffe_coders.fastestlap.domain.race_result.ResultsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.race_week.FirstPractice;
import com.the_coffe_coders.fastestlap.domain.race_week.Qualifying;
import com.the_coffe_coders.fastestlap.domain.race_week.RaceWeekAPIresponse;
import com.the_coffe_coders.fastestlap.domain.race_week.Races;
import com.the_coffe_coders.fastestlap.domain.race_week.SecondPractice;
import com.the_coffe_coders.fastestlap.domain.race_week.Session;
import com.the_coffe_coders.fastestlap.domain.race_week.Sprint;
import com.the_coffe_coders.fastestlap.domain.race_week.SprintQualifying;
import com.the_coffe_coders.fastestlap.domain.race_week.ThirdPractice;
import com.the_coffe_coders.fastestlap.utils.Constants;

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
import java.util.ArrayList;
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
    // Must be queried from the selected card
    private String circuitId = "yas_marina";
    private final ZoneId localZone = ZoneId.systemDefault();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_event), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

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

        /*//add event listener logic for countdown layout
        LinearLayout trackPic = findViewById(R.id.timer_card);
        trackPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "clicked");

            }
        });*/

        //add event listener logic for live_session card
        MaterialCardView liveSession = findViewById(R.id.live_session);
        ImageView liveIcon = findViewById(R.id.live_icon);
        Animation pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse);
        liveIcon.startAnimation(pulseAnimation);

        liveSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "live session clicked");
            }
        });

        //add event listener for weather card
        MaterialCardView weatherCard = findViewById(R.id.weather_forecast);
        weatherCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "weather card clicked");
            }
        });
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
                        RaceWeekAPIresponse raceSchedule = parser.parseRaceWeek(mrdata);

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

    private void processRaceData(RaceWeekAPIresponse raceSchedule) {
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setTitle(raceSchedule.getRaceTable().getRaces().get(0).getRaceName());

        ImageView countryFlag = findViewById(R.id.country_flag);
        Integer flag = Constants.EVENT_COUNTRY_FLAG.get(circuitId);
        countryFlag.setImageResource(flag);

        ImageView trackMap = findViewById(R.id.track_outline);
        Integer outline = Constants.EVENT_CIRCUIT.get(circuitId);
        trackMap.setImageResource(outline);

        TextView roundNumber = findViewById(R.id.round_number);
        String round = "Round " + raceSchedule.getRaceTable().getRaces().get(0).getRound();
        roundNumber.setText(round);

        TextView seasonYear = findViewById(R.id.year);
        seasonYear.setText(raceSchedule.getRaceTable().getSeason());

        TextView name = findViewById(R.id.gp_name);
        name.setText(Constants.TRACK_LONG_GP_NAME.get(circuitId));

        setEventImage(circuitId);

        TextView eventDate = findViewById(R.id.event_date);
        eventDate.setText(getDate(raceSchedule));

        List<Session> sessions = populateSessions(raceSchedule);

        Session nextEvent = findNextEvent(sessions);
        if (nextEvent != null) {
            ZonedDateTime eventDateTime = nextEvent.getStartDateTime();
            startCountdown(eventDateTime);
        } else
            showResults();


        createWeekSchedule(sessions);
    }

    private void setEventImage(String circuitId) {
        Integer eventImage = Constants.EVENT_PICTURE.get(circuitId);
        Drawable picture = ContextCompat.getDrawable(this, eventImage);
        picture.setAlpha(76);

        LinearLayout eventCard = findViewById(R.id.event_card);
        eventCard.setBackground(picture);
    }

    private String getDate(RaceWeekAPIresponse raceSchedule) {
        String fullDate;
        String startingDate = raceSchedule.getRaceTable().getRaces().get(0).getFirstPractice().getDate();
        String endingDate = raceSchedule.getRaceTable().getRaces().get(0).getDate();

        LocalDate startDate = LocalDate.parse(startingDate);
        LocalDate endDate = LocalDate.parse(endingDate);

        if (startDate.getMonth() != endDate.getMonth()) {
            fullDate = startDate.getDayOfMonth() + " " + startDate.getMonth() + " - " + endDate.getDayOfMonth() + " " + endDate.getMonth();
        } else {
            fullDate = startDate.getDayOfMonth() + " - " + endDate.getDayOfMonth() + " " + startDate.getMonth();
        }

        return fullDate;
    }

    private List<Session> populateSessions(RaceWeekAPIresponse raceSchedule) {
        List<Session> sessions = new ArrayList<>();
        Races race = raceSchedule.getRaceTable().getRaces().get(0);

        addSession(sessions, race.getFirstPractice());
        addSession(sessions, race.getSecondPractice());
        addSession(sessions, race.getThirdPractice());
        addSession(sessions, race.getSprintQualifying());
        addSession(sessions, race.getSprint());
        addSession(sessions, race.getQualifying());

        // Add Race session
        ZonedDateTime startDateTime = ZonedDateTime.parse(
                race.getDate() + "T" + race.getTime() + "[UTC]"
        );
        sessions.add(new Session("Race", false, false, startDateTime, null));

        return sessions;
    }

    private void addSession(List<Session> sessions, Object tmpSession) {
        if (tmpSession != null) {
            String sessionId = tmpSession.getClass().getSimpleName();
            Log.i(TAG, "Session ID: " + sessionId);

            String date = null;
            String time = null;

            if (tmpSession instanceof FirstPractice) {
                date = ((FirstPractice) tmpSession).getDate();
                time = ((FirstPractice) tmpSession).getTime();
            } else if (tmpSession instanceof SecondPractice) {
                date = ((SecondPractice) tmpSession).getDate();
                time = ((SecondPractice) tmpSession).getTime();
            } else if (tmpSession instanceof ThirdPractice) {
                date = ((ThirdPractice) tmpSession).getDate();
                time = ((ThirdPractice) tmpSession).getTime();
            } else if (tmpSession instanceof SprintQualifying) {
                date = ((SprintQualifying) tmpSession).getDate();
                time = ((SprintQualifying) tmpSession).getTime();
            } else if (tmpSession instanceof Sprint) {
                date = ((Sprint) tmpSession).getDate();
                time = ((Sprint) tmpSession).getTime();
            } else if (tmpSession instanceof Qualifying) {
                date = ((Qualifying) tmpSession).getDate();
                time = ((Qualifying) tmpSession).getTime();
            }

            if (date != null && time != null) {
                ZonedDateTime startDateTime = ZonedDateTime.parse(date + "T" + time + "[UTC]");
                sessions.add(new Session(sessionId, false, false, startDateTime, null));
            }
        }
    }


    private Session findNextEvent(List<Session> sessions) {
        ZonedDateTime currentDateTime = ZonedDateTime.now(localZone);
        Session nextSession = null;
        int i = 0;

        for (Session session : sessions) {
            Log.i(TAG, "Session: " + session.getStartDateTime());
            if (currentDateTime.isAfter(session.getStartDateTime()) && currentDateTime.isBefore(session.getEndDateTime())) {
                session.setUnderway(true);
                setLiveSession();
                Log.i(TAG, "Session underway: " + session.getSessionId());
            } else if (currentDateTime.isAfter(session.getEndDateTime())) {
                session.setUnderway(false);
                session.setFinished(true);
            }
        }

        for (Session session : sessions) {
            if (!session.isFinished()) {
                nextSession = session;
                break;
            }
        }

        return nextSession;
    }

    private void setLiveSession() {
        MaterialCardView liveSession = findViewById(R.id.live_session);
        MaterialCardView noLiveSession = findViewById(R.id.no_live_session);

        // Set weight of live session card to 1
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.weight = 1;
        liveSession.setLayoutParams(params);

        // Set weight of no live session card to 0
        params.weight = 0;
        noLiveSession.setLayoutParams(params);
    }

    private void startCountdown(ZonedDateTime eventDate) {
        long millisUntilStart = eventDate.toInstant().toEpochMilli() - ZonedDateTime.now(ZoneId.of("UTC")).toInstant().toEpochMilli();
        new CountDownTimer(millisUntilStart, 1000) {
            TextView days_counter = findViewById(R.id.days_counter);
            TextView hours_counter = findViewById(R.id.hours_counter);
            TextView minutes_counter = findViewById(R.id.minutes_counter);
            TextView seconds_counter = findViewById(R.id.seconds_counter);

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

    private void showResults() {
        FrameLayout eventCardContainer = findViewById(R.id.event_card_container);
        View countdownView = findViewById(R.id.timer_card_countdown);
        View resultsView = findViewById(R.id.timer_card_results);

        // Hide countdown view and show results view
        countdownView.setVisibility(View.GONE);
        resultsView.setVisibility(View.VISIBLE);

        // You can add logic here to populate the results view with actual data
        processRaceResults();

        // Set podium cricuit image
        ImageView trackOutline = findViewById(R.id.results_track_outline);
        trackOutline.setImageResource(Constants.EVENT_CIRCUIT.get(circuitId));
    }

    private void processRaceResults() {
        // Get results from API https://ergast.com/api/f1/current/last/results.json
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://ergast.com/api/f1/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();

        ergastApi = retrofit.create(ErgastAPI.class);

        ergastApi.getLastRaceResults().enqueue(new Callback<>() {
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