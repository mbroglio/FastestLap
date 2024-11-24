package com.the_coffe_coders.fastestlap;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.os.CountDownTimer;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.ViewGroup;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalTime;
import org.threeten.bp.Year;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;

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
    private String underwaySession = null;

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

        setDynamicDimensions();

        Year currentYear = Year.now();
        BASE_URL += currentYear + "/";

        String circuitId = "losail"; // Must be queried from the selected card
        BASE_URL += "circuits/" + circuitId + "/";

        Log.i(TAG, "Base URL: " + BASE_URL);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();

        ergastApi = retrofit.create(ErgastAPI.class);

        getEventInfo();

        //add event listener logic for countdown layout
        LinearLayout trackPic = findViewById(R.id.track_pic);
        trackPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "clicked");

            }
        });

        //add event listener logic for live_session card
        MaterialCardView liveSession = findViewById(R.id.live_session);
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

        //add event listener for session_1_flag
        ImageView session1Flag = findViewById(R.id.session_1_flag);
        session1Flag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "session 1 flag clicked");
            }
        });
    }

    private void setDynamicDimensions() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;

        setDynamicMargins((int) (screenWidth * 0.15), R.id.session_1_name);
        setDynamicMargins((int) (screenWidth * 0.15), R.id.session_1_day);
    }

    private void setDynamicMargins(int screenWidth, int viewId) {
        TextView textView = findViewById(viewId);
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) textView.getLayoutParams();
        layoutParams.setMarginEnd(screenWidth);
        textView.setLayoutParams(layoutParams);
    }

    private void getEventInfo() {
        ergastApi.getRaces().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                Log.i(TAG, "Response: " + response);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        Log.i(TAG, "Json Response: " + jsonResponse);

                        JSONArray raceSchedule = jsonResponse.getJSONObject("MRData").getJSONObject("RaceTable").getJSONArray("Races");
                        Log.i(TAG, "Json Array: " + raceSchedule);

                        try {
                            processRaceData(raceSchedule);
                        } catch (JSONException e) {
                            Log.e(TAG, "Error processing Race Data", e);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing JSON response", e);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    Log.e(TAG, "Response body is null");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable throwable) {
                Log.e(TAG, "Error fetching data", throwable);
            }
        });
    }

    private void processRaceData(JSONArray race) throws JSONException {
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        String title = race.getJSONObject(0).getString("raceName").toUpperCase();
        toolbar.setTitle(title);

        TextView roundNumber = findViewById(R.id.round_number);
        String roundText = "Round " + race.getJSONObject(0).getString("round");
        roundNumber.setText(roundText);

        String season = race.getJSONObject(0).getString("season");

        TextView eventDate = findViewById(R.id.event_date);
        String eventDateString = getDateString(race);
        eventDate.setText(eventDateString);

        ZonedDateTime nextEventDate = findNextDate(race);
        if (nextEventDate != null) {
            startCountdown(nextEventDate);
        }

        Log.i(TAG, "Underway Session: " + underwaySession);

        createWeekSchedule(race);
    }

    private String getDateString(JSONArray race) throws JSONException {
        String startDate = race.getJSONObject(0).getJSONObject("FirstPractice").getString("date");
        LocalDate eventStart = LocalDate.parse(startDate);

        String endDate = race.getJSONObject(0).getString("date");
        LocalDate eventEnd = LocalDate.parse(endDate);

        String eventDate = eventStart.getDayOfMonth() + " - " + eventEnd.getDayOfMonth();
        eventDate += " " + eventStart.getMonth().toString();

        return eventDate;
    }

    private ZonedDateTime findNextDate(JSONArray race) throws JSONException {
        String[] sessions = {"FirstPractice", "SecondPractice", "ThirdPractice", "Qualifying", "Sprint", "SprintQualifying"};
        ZonedDateTime currentDateTime = ZonedDateTime.now(ZoneId.of("UTC"));

        ZonedDateTime nextEvent = null;
        String nextSession = "";
        for (String session : sessions) {
            try {
                JSONObject sessionObj = race.getJSONObject(0).optJSONObject(session);
                if (sessionObj != null) {
                    String nextEventString = sessionObj.getString("date") + "T" + sessionObj.getString("time") + "[UTC]";
                    ZonedDateTime sessionDateTime = ZonedDateTime.parse(nextEventString);
                    Log.i(TAG, "Next Event: " + sessionDateTime);

                    if (currentDateTime.isBefore(sessionDateTime) && (nextEvent == null || sessionDateTime.isBefore(nextEvent))) {
                        nextEvent = sessionDateTime;
                        nextSession = session;
                    }
                }
            } catch (JSONException e) {
                Log.i(TAG, "No " + session + " Session", e);
            }
        }

        String raceDateString = race.getJSONObject(0).getString("date") + "T" + race.getJSONObject(0).getString("time") + "[UTC]";
        ZonedDateTime raceDateTime = ZonedDateTime.parse(raceDateString);

        if (currentDateTime.isBefore(raceDateTime) && (nextEvent == null || raceDateTime.isBefore(nextEvent))) {
            nextEvent = raceDateTime;
            nextSession = "Race";
        }

        if (nextEvent != null) {
            switch (nextSession) {
                case "FirstPractice":
                    if (currentDateTime.isBefore(nextEvent.plusHours(1))) {
                        underwaySession = "First Practice";
                    }
                    break;
                case "SecondPractice":
                    if (currentDateTime.isBefore(nextEvent.plusHours(1))) {
                        underwaySession = "Second Practice";
                    }
                    break;
                case "ThirdPractice":
                    if (currentDateTime.isBefore(nextEvent.plusHours(1))) {
                        underwaySession = "Third Practice";
                    }
                    break;
                case "Qualifying":
                    if (currentDateTime.isBefore(nextEvent.plusHours(1))) {
                        underwaySession = "Qualifying";
                    }
                    break;
                case "Sprint":
                    if (currentDateTime.isBefore(nextEvent.plusHours(1))) {
                        underwaySession = "Sprint";
                    }
                    break;

                case "SprintQualifying":
                    if (currentDateTime.isBefore(nextEvent.plusMinutes(45))) {
                        underwaySession = "Sprint Qualifying";
                    }
                    break;
                case "Race":
                    if (currentDateTime.isBefore(nextEvent.plusHours(2))) {
                        underwaySession = "Race";
                    }
                    return null;  // As per original logic,
                default:
                    Log.e(TAG, "Unknown Session: " + nextSession);
            }
        }

        return nextEvent;
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

    private void createWeekSchedule(JSONArray race) throws JSONException {
        // First Practice Session
        updateSession(R.id.session_1_name, R.string.practice_1_string,
                R.id.session_1_day, R.string.friday_string,
                R.id.session_1_time, race.getJSONObject(0).getJSONObject("FirstPractice"), 60);

        // Qualifying Session
        updateSession(R.id.session_4_name, R.string.qualifying_string,
                R.id.session_4_day, R.string.saturday_string,
                R.id.session_4_time, race.getJSONObject(0).getJSONObject("Qualifying"), 60);

        // Race Session
        updateSession(R.id.session_5_name, R.string.race_string,
                R.id.session_5_day, R.string.sunday_string,
                R.id.session_5_time, race.getJSONObject(0), "time", 0);

        try {
            // Sprint Qualifying Session
            updateSession(R.id.session_2_name, R.string.sprint_qualifying_string,
                    R.id.session_2_day, R.string.friday_string,
                    R.id.session_2_time, race.getJSONObject(0).getJSONObject("SprintQualifying"), 45);

            // Sprint Race Session
            updateSession(R.id.session_3_name, R.string.sprint_string,
                    R.id.session_3_day, R.string.saturday_string,
                    R.id.session_3_time, race.getJSONObject(0).getJSONObject("Sprint"), 60);
        } catch (JSONException e) {
            // Second Practice Session
            updateSession(R.id.session_2_name, R.string.practice_2_string,
                    R.id.session_2_day, R.string.friday_string,
                    R.id.session_2_time, race.getJSONObject(0).getJSONObject("SecondPractice"), 60);

            // Third Practice Session
            updateSession(R.id.session_3_name, R.string.practice_3_string,
                    R.id.session_3_day, R.string.saturday_string,
                    R.id.session_3_time, race.getJSONObject(0).getJSONObject("ThirdPractice"), 60);
        }
    }

    private void updateSession(int nameViewId, int nameStringId, int dayViewId, int dayStringId, int timeViewId, JSONObject session, int durationMinutes) throws JSONException {
        updateSession(nameViewId, nameStringId, dayViewId, dayStringId, timeViewId, session, "time", durationMinutes);
    }

    private void updateSession(int nameViewId, int nameStringId, int dayViewId, int dayStringId, int timeViewId, JSONObject session, String timeKey, int durationMinutes) throws JSONException {
        TextView sessionName = findViewById(nameViewId);
        sessionName.setText(nameStringId);

        TextView sessionDay = findViewById(dayViewId);
        sessionDay.setText(dayStringId);

        TextView sessionTimeView = findViewById(timeViewId);
        String sessionTime = session.getString(timeKey);
        sessionTime = sessionTime.substring(0, sessionTime.length() - 1);

        LocalTime sessionStart = convertUtcToLocal(sessionTime);
        LocalTime sessionEnd = sessionStart.plusMinutes(durationMinutes);

        String sessionTimeString = durationMinutes > 0 ? sessionStart + " - " + sessionEnd : sessionStart.toString();
        sessionTimeView.setText(sessionTimeString);
    }

    private LocalTime convertUtcToLocal(String utcTime) {
        LocalTime utcLocalTime = LocalTime.parse(utcTime, DateTimeFormatter.ofPattern("HH:mm:ss"));
        ZonedDateTime utcZoned = utcLocalTime.atDate(LocalDate.now()).atZone(ZoneId.of("UTC"));
        ZonedDateTime localZoned = utcZoned.withZoneSameInstant(ZoneId.systemDefault());
        return localZoned.toLocalTime();
    }
}
