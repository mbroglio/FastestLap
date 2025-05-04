package com.the_coffe_coders.fastestlap.ui.event;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Practice;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;
import com.the_coffe_coders.fastestlap.domain.grand_prix.RaceResult;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Session;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Track;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.domain.nation.Nation;
import com.the_coffe_coders.fastestlap.ui.bio.TrackBioActivity;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.NationViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.NationViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.TrackViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.TrackViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.event.fragment.RaceResultsFragment;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.EventViewModel;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.EventViewModelFactory;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.UIUtils;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import android.app.Dialog;
import android.view.Window;

public class EventActivity extends AppCompatActivity {
    private static final String TAG = "EventActivity";
    private final boolean eventToProcess = true;
    LoadingScreen loadingScreen;
    EventViewModel eventViewModel;
    private String trackId;
    private Track track;
    private Nation nation;
    private SwipeRefreshLayout eventLayout;
    private Race currentRace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_event);

        start();
    }

    private void start() {
        eventLayout = findViewById(R.id.event_layout);
        loadingScreen = new LoadingScreen(getWindow().getDecorView(), this, eventLayout, null);
        loadingScreen.showLoadingScreen(false);
        loadingScreen.updateProgress(0);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        UIUtils.applyWindowInsets(toolbar);

        UIUtils.applyWindowInsets(eventLayout);
        eventLayout.setOnRefreshListener(() -> {
            start();
            eventLayout.setRefreshing(false);
        });

        trackId = getIntent().getStringExtra("CIRCUIT_ID");
        Log.i(TAG, "Circuit ID: " + trackId);

        initializeViewModels();
    }

    private void initializeViewModels() {
        eventViewModel = new ViewModelProvider(this, new EventViewModelFactory(getApplication())).get(EventViewModel.class);
        processRaceData();
    }

    private void processRaceData() {
        List<WeeklyRace> races = new ArrayList<>();
        MutableLiveData<Result> data = eventViewModel.getWeeklyRacesLiveData();
        data.observe(this, result -> {
            if (result instanceof Result.Loading) {
                return;
            }
            if (result.isSuccess()) {
                Log.i("PastEvent", "SUCCESS");
                races.addAll(((Result.WeeklyRaceSuccess) result).getData());

                // Extract in another class
                WeeklyRace weeklyRace = null;
                for (WeeklyRace race : races) {
                    if (race.getTrack().getTrackId().equals(trackId)) {
                        weeklyRace = race;
                    }
                }

                assert weeklyRace != null;
                buildEventCard(weeklyRace);
            }
        });
    }

    private void buildEventCard(WeeklyRace weeklyRace) {

        loadingScreen.postLoadingStatus(this.getString(R.string.initializing));

        String grandPrixName = weeklyRace.getRaceName().toUpperCase();
        TextView title = findViewById(R.id.topAppBarTitle);
        title.setText(grandPrixName);

        TrackViewModel trackViewModel = new ViewModelProvider(this, new TrackViewModelFactory(getApplication())).get(TrackViewModel.class);
        MutableLiveData<Result> trackData = trackViewModel.getTrack(trackId);

        trackData.observe(this, result -> {
            if (result instanceof Result.Loading) {
                return;
            }
            if (result.isSuccess()) {
                track = ((Result.TrackSuccess) result).getData();
                Log.i(TAG, "Track: " + track.toString());

                NationViewModel nationViewModel = new ViewModelProvider(this, new NationViewModelFactory(getApplication())).get(NationViewModel.class);
                MutableLiveData<Result> nationData = nationViewModel.getNation(track.getCountry());

                nationData.observe(this, result1 -> {
                    if (result1 instanceof Result.Loading) {
                        return;
                    }
                    if (result1.isSuccess()) {
                        nation = ((Result.NationSuccess) result1).getData();
                        Log.i(TAG, "Nation: " + nation.toString());

                        setEventImage(weeklyRace, track, nation);
                    }
                });
            }
        });
    }

    private void setEventImage(WeeklyRace weeklyRace, Track track, Nation nation) {

        loadingScreen.postLoadingStatus(this.getString(R.string.setting_event_info));
        loadingScreen.updateProgress(33);

        String imageUrl = track.getTrack_pic_url();
        LinearLayout eventCard = findViewById(R.id.event_card);

        UIUtils.loadImageInEventCardWithAlpha(this, imageUrl, eventCard,
                () -> buildEventCardStepTwo(weeklyRace, track, nation),
                76);
    }

    private void buildEventCardStepTwo(WeeklyRace weeklyRace, Track track, Nation nation) {

        UIUtils.multipleSetTextViewText(
                new String[]{
                        "Round " + weeklyRace.getRound(),
                        weeklyRace.getSeason(),
                        track.getGp_long_name(),
                        weeklyRace.getDateInterval()},

                new TextView[]{
                        findViewById(R.id.round_number),
                        findViewById(R.id.event_year),
                        findViewById(R.id.gp_name),
                        findViewById(R.id.event_date),
                }
        );

        LinearLayout trackLayout = findViewById(R.id.track_outline_layout);
        trackLayout.setOnClickListener(v -> {
            Intent intent = new Intent(EventActivity.this, TrackBioActivity.class);
            intent.putExtra("CIRCUIT_ID", trackId);
            intent.putExtra("GRAND_PRIX_NAME", weeklyRace.getRaceName().toUpperCase());
            startActivity(intent);
        });

        Button openForecastButton = findViewById(R.id.goToForecastButton);
        openForecastButton.setOnClickListener(v ->
                UIUtils.openGoogleWeather(this, track.getLocation().getLocality()));

        UIUtils.loadSequenceOfImagesWithGlide(this,
                new String[]{nation.getNation_flag_url(), track.getTrack_minimal_layout_url()},

                new ImageView[]{findViewById(R.id.country_flag), findViewById(R.id.track_outline_image)},

                () -> buildEventCardFinalStep(weeklyRace));
    }

    private void buildEventCardFinalStep(WeeklyRace weeklyRace) {
        List<Session> sessions = weeklyRace.getSessions();
        Session nextEvent = weeklyRace.findNextEvent(sessions);
        boolean underway = weeklyRace.isUnderway(false) && !weeklyRace.isWeekFinished();
        //setLiveSession();
        if (nextEvent != null && !underway) {
            LocalDateTime eventDateTime = nextEvent.getStartDateTime();
            startCountdown(eventDateTime);
        } else if (!underway) {
            showResults(weeklyRace);
        }

        createWeekSchedule(sessions);
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

        loadingScreen.postLoadingStatus(this.getString(R.string.setting_up_countdown));
        loadingScreen.updateProgress(66);

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

    private void showRaceResultsDialog(Race race) {
        if (race == null || race.getRaceResults() == null || race.getRaceResults().isEmpty()) {
            Log.e(TAG, "No race results to show");
            return;
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        RaceResultsFragment raceResultsFragment = new RaceResultsFragment();
        Bundle args = new Bundle();
        args.putParcelable("RACE", race);
        raceResultsFragment.setArguments(args);
        raceResultsFragment.show(fragmentManager, "ForgotPasswordFragment");
    }

    private void showResults(WeeklyRace weeklyRace) {

        loadingScreen.postLoadingStatus(this.getString(R.string.fetching_race_result, ""));
        loadingScreen.updateProgress(66);

        View countdownView = findViewById(R.id.timer_card_countdown);
        View resultsView = findViewById(R.id.timer_card_results);
        View raceCancelledView = findViewById(R.id.timer_card_race_cancelled);

        // Hide countdown view and show results view
        countdownView.setVisibility(View.GONE);
        resultsView.setVisibility(View.VISIBLE);

        UIUtils.loadImageWithGlide(this, track.getTrack_minimal_layout_url(),
                findViewById(R.id.track_outline_image), () -> processRaceResults(weeklyRace));

    }

    private void processRaceResults(WeeklyRace weeklyRace) {
        Log.i(TAG, "Processing race results" + " " + weeklyRace.getRound());
        MutableLiveData<Result> resultMutableLiveData = eventViewModel.getRaceResults(weeklyRace.getRound());

        resultMutableLiveData.observe(this, result -> {
            if (result instanceof Result.Loading) {
                return;
            }
            Race race = ((Result.LastRaceResultsSuccess) result).getData();
            List<RaceResult> podium = race.getRaceResults();
            if (podium == null) {
                showPendingResults();
            } else {
                // Store the race for later use
                this.currentRace = race;


                Log.i(TAG, "Podium found" + podium.size());
                for (int i = 0; i < 3; i++) {
                    String driverId = podium.get(i).getDriver().getDriverId();
                    String teamId = podium.get(i).getConstructor().getConstructorId();

                    UIUtils.singleSetTextViewText(podium.get(i).getDriver().getFullName(),
                            findViewById(Constants.PODIUM_DRIVER_NAME.get(i)));

                    LinearLayout teamColor = findViewById(Constants.PODIUM_TEAM_COLOR.get(i));
                    Integer teamColorObj = Constants.TEAM_COLOR.get(teamId);
                    teamColor.setBackgroundColor(ContextCompat.getColor(this, Objects.requireNonNullElseGet(teamColorObj, () -> R.color.mercedes_f1)));
                }

                // Make the podium clickable
                View resultsView = findViewById(R.id.timer_card_results);
                resultsView.setOnClickListener(v -> showRaceResultsDialog(currentRace));
            }
        });
    }

    private void showPendingResults() {
        Log.i(TAG, "No results found");

        loadingScreen.postLoadingStatus(this.getString(R.string.fetching_race_result, this.getString(R.string.not_found_text)));
        loadingScreen.updateProgress(66);

        View pendingResultsView = findViewById(R.id.timer_card_pending_results);
        View countdownView = findViewById(R.id.timer_card_countdown);
        View resultsView = findViewById(R.id.timer_card_results);

        pendingResultsView.setVisibility(View.VISIBLE);
        countdownView.setVisibility(View.GONE);
        resultsView.setVisibility(View.GONE);
    }

    private void createWeekSchedule(List<Session> sessions) {
        View eventSchedule = findViewById(R.id.event_schedule_table);

        loadingScreen.postLoadingStatus(this.getString(R.string.setting_event_schedule));
        loadingScreen.updateProgress(100);

        String sessionId;

        for (Session session : sessions) {
            sessionId = session.getClass().getSimpleName();
            if (sessionId.equals("Practice")) {
                Practice practice = (Practice) session;
                sessionId = practice.getPractice();
            }

            UIUtils.translateSchedule(this,
                    eventSchedule.findViewById(Constants.SESSION_NAME_FIELD.get(sessionId)),
                    eventSchedule.findViewById(Constants.SESSION_DAY_FIELD.get(sessionId)),
                    sessionId);

            UIUtils.setTextViewTextWithCondition(sessionId.equals("Race"),
                    session.getStartingTime(), //if true
                    session.getTime(), //if false
                    eventSchedule.findViewById(Constants.SESSION_TIME_FIELD.get(sessionId)));

            setChequeredFlag(eventSchedule, session);
        }
        loadingScreen.hideLoadingScreen();
    }

    private void setChequeredFlag(View view, Session session) {
        String sessionId = session.getClass().getSimpleName();
        if (sessionId.equals("Practice")) {
            Practice practice = (Practice) session;
            sessionId = practice.getPractice();
        }

        if (session.isFinished()) {
            ImageView flag = view.findViewById(Constants.SESSION_FLAG_FIELD.get(sessionId));
            flag.setVisibility(View.VISIBLE);

            LinearLayout currentSession = view.findViewById(Constants.SESSION_ROW.get(sessionId));
            currentSession.setClickable(true);
            currentSession.setFocusable(true);
            currentSession.setOnClickListener(v -> manageSessionScheduleClick(session));
        }
    }

    private void manageSessionScheduleClick(Session session) {
        String sessionId = session.getClass().getSimpleName();
        if(sessionId.equals("Race")){
            showRaceResultsDialog(currentRace);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}