package com.the_coffe_coders.fastestlap;

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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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
    private String circuitId = "losail"; // Must be queried from the selected card
    private ZoneId localZone = ZoneId.systemDefault();

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

        //add event listener logic for countdown layout
        LinearLayout trackPic = findViewById(R.id.timer_card);
        trackPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "clicked");

            }
        });

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

        //add event listener for session_1_flag
        LinearLayout session1Flag = findViewById(R.id.session_1_flag_container);
        session1Flag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "session 1 flag clicked");
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
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable throwable) {
                Log.e(TAG, "Error fetching data", throwable);
            }
        });
    }

    private void processRaceData(JSONArray race) throws JSONException {
        RaceWeek raceWeek = new RaceWeek(race.getJSONObject(0), circuitId);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setTitle(raceWeek.getGpName());

        ImageView countryFlag = findViewById(R.id.country_flag);
        Integer flag = Constants.EVENT_COUNTRY_FLAG.get(raceWeek.getTrackId());
        countryFlag.setImageResource(flag);

        ImageView trackMap = findViewById(R.id.track_outline);
        Integer outline = Constants.EVENT_CIRCUIT.get(raceWeek.getTrackId());
        trackMap.setImageResource(outline);

        TextView roundNumber = findViewById(R.id.round_number);
        String round = "Round " + raceWeek.getEventNumber();
        roundNumber.setText(round);

        TextView seasonYear = findViewById(R.id.year);
        seasonYear.setText(raceWeek.getYear());

        TextView name = findViewById(R.id.gp_name);
        name.setText(Constants.TRACK_LONG_GP_NAME.get(raceWeek.getTrackId()));

        setEventImage(raceWeek.getTrackId());

        TextView eventDate = findViewById(R.id.event_date);
        eventDate.setText(raceWeek.getDate());

        ZonedDateTime nextEventDate = findNextDate(raceWeek);
        if (nextEventDate != null) {
            startCountdown(nextEventDate);
        }

        createWeekSchedule(raceWeek);
    }

    private void setEventImage(String circuitId) {
        Integer eventImage = Constants.EVENT_PICTURE.get(circuitId);
        Drawable picture = ContextCompat.getDrawable(this, eventImage);
        picture.setAlpha(76);

        LinearLayout eventCard = findViewById(R.id.event_card);
        eventCard.setBackground(picture);
    }

    private ZonedDateTime findNextDate(RaceWeek raceWeek) throws JSONException {
        ZonedDateTime currentDateTime = ZonedDateTime.now(localZone);
        Session nextSession = null;

        for (int i = 0; i < 5; i++) {
            Log.i(TAG, "Session: " + raceWeek.getSessions()[i].getStartDateTime());
            if (currentDateTime.isAfter(raceWeek.getSessions()[i].getStartDateTime()) && currentDateTime.isBefore(raceWeek.getSessions()[i].getEndDateTime())) {
                raceWeek.getSessions()[i].setUnderway(true);
                setLiveSession();
                Log.i(TAG, "Session underway: " + raceWeek.getSessions()[i].getSessionId());
            } else if (currentDateTime.isAfter(raceWeek.getSessions()[i].getEndDateTime())) {
                raceWeek.getSessions()[i].setUnderway(false);
                raceWeek.getSessions()[i].setFinished(true);
            }

            if (currentDateTime.isAfter(raceWeek.getSessions()[i].getStartDateTime())) {
                if (i <= 3) {
                    nextSession = raceWeek.getSessions()[i + 1];
                } else {
                    nextSession = raceWeek.getSessions()[i];
                }
            }

            Log.i(TAG, "Is " + raceWeek.getSessions()[i].getSessionId() + " underway? " + raceWeek.getSessions()[i].isUnderway());
        }

        if (nextSession == null) {
            nextSession = raceWeek.getSessions()[0];
        }

        Log.i(TAG, "Next Session: " + nextSession.getSessionId());
        return nextSession.getStartDateTime();
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

    private void createWeekSchedule(RaceWeek raceWeek) throws JSONException {
        for (Session session : raceWeek.getSessions()) {
            TextView sessionName = findViewById(Constants.SESSION_NAME_FIELD.get(session.getSessionId()));
            sessionName.setText(Constants.SESSION_NAMES.get(session.getSessionId()));

            TextView sessionDay = findViewById(Constants.SESSION_DAY_FIELD.get(session.getSessionId()));
            sessionDay.setText(Constants.SESSION_DAY.get(session.getSessionId()));

            TextView sessionTime = findViewById(Constants.SESSION_TIME_FIELD.get(session.getSessionId()));
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
        }
    }
}