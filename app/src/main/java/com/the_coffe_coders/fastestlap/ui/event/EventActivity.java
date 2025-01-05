package com.the_coffe_coders.fastestlap.ui.event;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;

import com.google.android.material.appbar.MaterialToolbar;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Practice;
import com.the_coffe_coders.fastestlap.domain.grand_prix.RaceResult;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Session;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.ui.bio.TrackBioActivity;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/*
 * TODO:
 *  - Implement raceCancelled logic
 *  - Fix raceResult logic
 */

public class EventActivity extends AppCompatActivity {
    private static final String TAG = "EventActivity";
    private final boolean eventToProcess = true;
    LoadingScreen loadingScreen;
    private String circuitId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event);


        // Show loading screen initially
        loadingScreen = new LoadingScreen(getWindow().getDecorView(), this);
        loadingScreen.showLoadingScreen();
        Log.i(TAG, "Loading screen shown");

        circuitId = getIntent().getStringExtra("CIRCUIT_ID");
        Log.i(TAG, "Circuit ID: " + circuitId);

        processRaceData();
        loadingScreen.hideLoadingScreen();
    }

    private void processRaceData() {
        List<WeeklyRace> races = new ArrayList<>();
        MutableLiveData<Result> data = ServiceLocator.getInstance().getRaceRepository(getApplication(), false).fetchWeeklyRaces(0);
        data.observe(this, result -> {
            if (result.isSuccess()) {
                Log.i("PastEvent", "SUCCESS");
                races.addAll(((Result.WeeklyRaceSuccess) result).getData());
                loadingScreen.hideLoadingScreen();
                WeeklyRace weeklyRace = races.get(0);

                MaterialToolbar toolbar = findViewById(R.id.topAppBar);
                toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
                toolbar.setTitle(weeklyRace.getRaceName().toUpperCase());

                ImageView countryFlag = findViewById(R.id.country_flag);
                String nation = weeklyRace.getCircuit().getLocation().getCountry();
                Integer flag = Constants.NATION_COUNTRY_FLAG.get(nation);
                countryFlag.setImageResource(Objects.requireNonNullElseGet(flag, () -> R.drawable.austria_flag));

                ImageView trackMap = findViewById(R.id.track_outline_image);
                Integer outline = Constants.EVENT_CIRCUIT.get(circuitId);
                trackMap.setImageResource(Objects.requireNonNullElseGet(outline, () -> R.drawable.back_curved_arrow));

                TextView roundNumber = findViewById(R.id.round_number);
                String round = "Round " + weeklyRace.getRound();
                roundNumber.setText(round);

                TextView seasonYear = findViewById(R.id.event_year);
                String year = weeklyRace.getSeason();
                seasonYear.setText(year);

                TextView name = findViewById(R.id.gp_name);
                name.setText(Constants.TRACK_LONG_GP_NAME.get(circuitId));

                setEventImage();

                TextView eventDate = findViewById(R.id.event_date);
                eventDate.setText(weeklyRace.getDateInterval());

                LinearLayout track = findViewById(R.id.track_outline_layout);
                Log.i(TAG, "Track: " + track);
                track.setOnClickListener(v -> {
                    Intent intent = new Intent(EventActivity.this, TrackBioActivity.class);
                    intent.putExtra("CIRCUIT_ID", circuitId);
                    Log.i(TAG, "Circuit ID: " + circuitId);
                    startActivity(intent);
                });

                List<Session> sessions = weeklyRace.getSessions();
                Session nextEvent = weeklyRace.findNextEvent(sessions);
                boolean underway = false;
                if (weeklyRace.isUnderway() && !weeklyRace.isWeekFinished()) {
                    underway = true;
                    setLiveSession();
                }
                if (nextEvent != null && !underway) {
                    LocalDateTime eventDateTime = nextEvent.getStartDateTime();
                    startCountdown(eventDateTime);
                } else if (!underway) {
                    showResults(weeklyRace);
                }

                createWeekSchedule(sessions);
            }
        });
    }

    private void setEventImage() {
        Integer eventImage = Constants.EVENT_PICTURE.get(circuitId);
        Drawable picture = ContextCompat.getDrawable(this, Objects.requireNonNullElseGet(eventImage, () -> R.drawable.australia_image));
        if (picture != null) {
            picture.setAlpha(76);
        }

        LinearLayout eventCard = findViewById(R.id.event_card);
        eventCard.setBackground(picture);
    }

    private void setLiveSession() {
        View liveSession = findViewById(R.id.event_live_card);
        View noLiveSession = findViewById(R.id.event_not_live_card);

        // Hide countdown view and show results view
        liveSession.setVisibility(View.VISIBLE);
        noLiveSession.setVisibility(View.GONE);

        ImageView liveIcon = findViewById(R.id.live_icon);
        Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse_dynamic);
        liveIcon.startAnimation(pulse);
    }

    private void startCountdown(LocalDateTime eventDate) {
        long millisUntilStart = ZonedDateTime.of(eventDate, ZoneId.systemDefault()).toInstant().toEpochMilli() - System.currentTimeMillis();
        new CountDownTimer(millisUntilStart, 1000) {
            final TextView days_counter = findViewById(R.id.next_days_counter);
            final TextView hours_counter = findViewById(R.id.next_hours_counter);
            final TextView minutes_counter = findViewById(R.id.next_minutes_counter);
            final TextView seconds_counter = findViewById(R.id.next_seconds_counter);

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

    private void showResults(WeeklyRace weeklyRace) {
        View countdownView = findViewById(R.id.timer_card_countdown);
        View resultsView = findViewById(R.id.timer_card_results);
        View raceCancelledView = findViewById(R.id.timer_card_race_cancelled);

        // Hide countdown view and show results view
        countdownView.setVisibility(View.GONE);
        resultsView.setVisibility(View.VISIBLE);

        processRaceResults(weeklyRace);

        // Set podium circuit image
        ImageView trackOutline = findViewById(R.id.track_outline_image);
        Integer outline = Constants.EVENT_CIRCUIT.get(circuitId);
        trackOutline.setImageResource(Objects.requireNonNullElseGet(outline, () -> R.drawable.arrow_back_ios_style));
    }

    private void processRaceResults(WeeklyRace raceResults) {
        List<RaceResult> podium = raceResults.getFinalRace().getResults();

        if (podium == null) {
            showPendingResults();
        } else {
            for (int i = 0; i < 3; i++) {
                String driverId = podium.get(i).getDriver().getDriverId();
                String teamId = podium.get(i).getConstructor().getConstructorId();

                TextView driverName = findViewById(Constants.PODIUM_DRIVER_NAME.get(i));
                Integer driverNameObj = Constants.DRIVER_FULLNAME.get(driverId);
                driverName.setText(Objects.requireNonNullElseGet(driverNameObj, () -> R.string.unknown));

                LinearLayout teamColor = findViewById(Constants.PODIUM_TEAM_COLOR.get(i));
                Integer teamColorObj = Constants.TEAM_COLOR.get(teamId);
                teamColor.setBackgroundColor(ContextCompat.getColor(this, Objects.requireNonNullElseGet(teamColorObj, () -> R.color.mercedes_f1)));
            }
        }
    }

    private void showPendingResults() {
        Log.i(TAG, "No results found");
        View pendingResultsView = findViewById(R.id.timer_card_pending_results);
        View countdownView = findViewById(R.id.timer_card_countdown);
        View resultsView = findViewById(R.id.timer_card_results);

        pendingResultsView.setVisibility(View.VISIBLE);
        countdownView.setVisibility(View.GONE);
        resultsView.setVisibility(View.GONE);
    }

    private void createWeekSchedule(List<Session> sessions) {
        String sessionId;

        for (Session session : sessions) {
            sessionId = session.getClass().getSimpleName();
            if (sessionId.equals("Practice")) {
                Practice practice = (Practice) session;
                sessionId = practice.getPractice();
            }

            TextView sessionName = findViewById(Constants.SESSION_NAME_FIELD.get(sessionId));
            sessionName.setText(Constants.SESSION_NAMES.get(sessionId));

            TextView sessionDay = findViewById(Constants.SESSION_DAY_FIELD.get(sessionId));
            sessionDay.setText(Constants.SESSION_DAY.get(sessionId));

            TextView sessionTime = findViewById(Constants.SESSION_TIME_FIELD.get(sessionId));
            if (sessionId.equals("Race")) sessionTime.setText(session.getStartingTime());
            else sessionTime.setText(session.getTime());

            setChequeredFlag(session);
        }
    }

    private void setChequeredFlag(Session session) {
        String sessionId = session.getClass().getSimpleName();
        if (sessionId.equals("Practice")) {
            Practice practice = (Practice) session;
            sessionId = practice.getPractice();
        }

        if (session.isFinished()) {
            ImageView flag = findViewById(Constants.SESSION_FLAG_FIELD.get(sessionId));
            flag.setVisibility(View.VISIBLE);

            RelativeLayout currentSession = findViewById(Constants.SESSION_ROW.get(sessionId));
            currentSession.setClickable(true);
            currentSession.setFocusable(true);
            currentSession.setOnClickListener(view -> Log.i(TAG, "session clicked"));
        }
    }
}