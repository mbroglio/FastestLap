package com.the_coffe_coders.fastestlap.ui.event;

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
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Practice;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Race;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Session;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.domain.f1.result.QualifyingResult;
import com.the_coffe_coders.fastestlap.domain.f1.result.RaceResult;
import com.the_coffe_coders.fastestlap.domain.f1.result.RaceResultFastestLap;
import com.the_coffe_coders.fastestlap.domain.f1.result.Stint;
import com.the_coffe_coders.fastestlap.domain.f1.track.Track;
import com.the_coffe_coders.fastestlap.domain.nation.Nation;
import com.the_coffe_coders.fastestlap.domain.f1.livetiming.RaceControlMessage;
import com.the_coffe_coders.fastestlap.domain.f1.livetiming.TeamRadioMessage;
import com.the_coffe_coders.fastestlap.repository.f1.livetiming.LiveTimingRepository;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.NationViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.NationViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.TrackViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.TrackViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.EventViewModel;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.EventViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.RaceResultViewModel;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.RaceResultViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.WeeklyRaceViewModel;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.WeeklyRaceViewModelFactory;
import com.the_coffe_coders.fastestlap.util.CalendarUtils;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.ui.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ui.NavigationUtils;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EventActivity extends AppCompatActivity {
    private static final String TAG = "EventActivity";
    LoadingScreen loadingScreen;
    EventViewModel eventViewModel;
    RaceResultViewModel raceResultViewModel;
    WeeklyRaceViewModel weeklyRaceViewModel;
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
        loadingScreen.updateProgress();

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
        raceResultViewModel = new ViewModelProvider(this, new RaceResultViewModelFactory(getApplication(), this)).get(RaceResultViewModel.class);
        weeklyRaceViewModel = new ViewModelProvider(this, new WeeklyRaceViewModelFactory(getApplication(), this)).get(WeeklyRaceViewModel.class);
        processRaceData();
    }

    private void processRaceData() {
        List<WeeklyRace> races = new ArrayList<>();
        LiveData<Result> data = weeklyRaceViewModel.getWeeklyRacesLiveData();
        @SuppressWarnings("unchecked")
        androidx.lifecycle.Observer<Result>[] observerHolder = new androidx.lifecycle.Observer[1];
        observerHolder[0] = result -> {
            if (result instanceof Result.Loading) {
                return;
            }
            data.removeObserver(observerHolder[0]);
            if (result.isSuccess()) {
                Log.i("EventActivity", "Weekly races loaded successfully");
                races.addAll(((Result.WeeklyRaceSuccess) result).getData());

                WeeklyRace weeklyRace = null;
                for (WeeklyRace race : races) {
                    if (race.getTrack().getTrackId().equals(trackId)) {
                        weeklyRace = race;
                        break;
                    }
                }

                if (weeklyRace != null) {
                    buildEventCard(weeklyRace);
                } else {
                    Log.e("EventActivity", "Weekly race not found for trackId: " + trackId);
                    loadingScreen.hideLoadingScreen();
                }
            } else {
                Log.e("EventActivity", "Error loading weekly races: " + result.getError());
                loadingScreen.hideLoadingScreen();
            }
        };
        data.observe(this, observerHolder[0]);
    }

    private void buildEventCard(WeeklyRace weeklyRace) {
        UIUtils.singleSetTextViewText(weeklyRace.getRaceName().toUpperCase(), findViewById(R.id.topAppBarTitle));

        TrackViewModel trackViewModel = new ViewModelProvider(this, new TrackViewModelFactory(getApplication())).get(TrackViewModel.class);
        MutableLiveData<Result> trackData = trackViewModel.getTrack(trackId);
        @SuppressWarnings("unchecked")
        androidx.lifecycle.Observer<Result>[] observerTrack = new androidx.lifecycle.Observer[1];
        observerTrack[0] = result -> {
            if (result instanceof Result.Loading) {
                return;
            }
            trackData.removeObserver(observerTrack[0]);
            if (result.isSuccess()) {
                track = ((Result.TrackSuccess) result).getData();
                Log.i(TAG, "Track: " + track);

                if (track != null && track.getCountry() != null) {
                    NationViewModel nationViewModel = new ViewModelProvider(this, new NationViewModelFactory(getApplication())).get(NationViewModel.class);
                    try {
                        MutableLiveData<Result> nationData = nationViewModel.getNation(track.getCountry());
                        @SuppressWarnings("unchecked")
                        androidx.lifecycle.Observer<Result>[] observerNation = new androidx.lifecycle.Observer[1];
                        observerNation[0] = result1 -> {
                            if (result1 instanceof Result.Loading) {
                                return;
                            }
                            nationData.removeObserver(observerNation[0]);
                            if (result1.isSuccess()) {
                                nation = ((Result.NationSuccess) result1).getData();
                                setEventImage(weeklyRace, track, nation);
                            } else {
                                setEventImage(weeklyRace, track, null);
                            }
                        };
                        nationData.observe(this, observerNation[0]);
                    } catch (RuntimeException e) {
                        Log.e(TAG, "Error getting nation data: " + e.getMessage());
                        setEventImage(weeklyRace, track, null);
                    }
                } else {
                    setEventImage(weeklyRace, track, null);
                }
            } else {
                Log.e(TAG, "Error getting track data: " + result.getError() + ", falling back to weeklyRace.getTrack()");
                Track fallbackTrack = weeklyRace.getTrack();
                if (fallbackTrack != null) {
                    track = fallbackTrack;
                    if (track.getCountry() != null) {
                        fetchNationAndSetImage(weeklyRace, track);
                    } else {
                        setEventImage(weeklyRace, track, null);
                    }
                } else {
                    loadingScreen.hideLoadingScreen();
                }
            }
        };
        trackData.observe(this, observerTrack[0]);
    }

    private void fetchNationAndSetImage(WeeklyRace weeklyRace, Track targetTrack) {
        NationViewModel nationViewModel = new ViewModelProvider(this, new NationViewModelFactory(getApplication())).get(NationViewModel.class);
        try {
            MutableLiveData<Result> nationData = nationViewModel.getNation(targetTrack.getCountry());
            @SuppressWarnings("unchecked")
            androidx.lifecycle.Observer<Result>[] observerNation = new androidx.lifecycle.Observer[1];
            observerNation[0] = result1 -> {
                if (result1 instanceof Result.Loading) {
                    return;
                }
                nationData.removeObserver(observerNation[0]);
                if (result1.isSuccess()) {
                    nation = ((Result.NationSuccess) result1).getData();
                    setEventImage(weeklyRace, targetTrack, nation);
                } else {
                    setEventImage(weeklyRace, targetTrack, null);
                }
            };
            nationData.observe(this, observerNation[0]);
        } catch (RuntimeException e) {
            Log.e(TAG, "Error getting nation data: " + e.getMessage());
            setEventImage(weeklyRace, targetTrack, null);
        }
    }

    private void setEventImage(WeeklyRace weeklyRace, Track track, Nation nation) {
        loadingScreen.updateProgress();

        String imageUrl = track != null ? track.getTrack_pic_url() : null;
        LinearLayout eventCard = findViewById(R.id.event_card);

        UIUtils.loadImageInEventCardWithAlpha(this, imageUrl, eventCard,
                () -> buildEventCardStepTwo(weeklyRace, track, nation),
                76);
    }

    private void buildEventCardStepTwo(WeeklyRace weeklyRace, Track track, Nation nation) {

        String gpName = (track != null && track.getGp_long_name() != null) ? track.getGp_long_name() : (weeklyRace != null ? weeklyRace.getRaceName() : "");

        UIUtils.multipleSetTextViewText(
                new String[]{
                        "Round " + weeklyRace.getRound(),
                        weeklyRace.getSeason(),
                        gpName},

                new TextView[]{
                        findViewById(R.id.round_number),
                        findViewById(R.id.event_year),
                        findViewById(R.id.gp_name)});

        UIUtils.translateEventDateInterval(weeklyRace.getDateInterval(), findViewById(R.id.event_date));

        LinearLayout trackLayout = findViewById(R.id.track_outline_layout);
        trackLayout.setOnClickListener(v -> NavigationUtils.navigateToBioPage(this, trackId + "&" + weeklyRace.getRaceName().toUpperCase(), 2));

        Button openForecastButton = findViewById(R.id.goToForecastButton);
        openForecastButton.setOnClickListener(v -> {
            String locality = track.getLocation().getLocality();
            String lat = track.getLocation().getLatitude();
            String lon = track.getLocation().getLongitude();

            String startDateStr = null;
            String endDateStr = null;
            if (weeklyRace.getFirstPractice() != null && weeklyRace.getFirstPractice().getStartDateTime() != null) {
                startDateStr = weeklyRace.getFirstPractice().getStartDateTime().toLocalDate().toString();
            }
            if (weeklyRace.getFinalRace() != null && weeklyRace.getFinalRace().getStartDateTime() != null) {
                endDateStr = weeklyRace.getFinalRace().getStartDateTime().toLocalDate().toString();
            }

            boolean isUnderway = weeklyRace.isUnderway(false);
            NavigationUtils.navigateToWeatherPage(this, locality, lat, lon, "latest", startDateStr, endDateStr, isUnderway);
        });

        String nationFlagUrl = null;
        if (nation != null) {
            nationFlagUrl = nation.getNation_flag_url();
        }

        UIUtils.loadSequenceOfImagesWithGlide(this,
                new String[]{nationFlagUrl, track.getTrack_minimal_layout_url()},
                new ImageView[]{findViewById(R.id.country_flag), findViewById(R.id.track_outline_image)},
                () -> buildEventCardFinalStep(weeklyRace));

        // Calendar export button
        Button addToCalendarButton = findViewById(R.id.addToCalendarButton);
        addToCalendarButton.setOnClickListener(v -> {
            try {
                CalendarUtils.addWeekendToCalendar(this, weeklyRace);
                Toast.makeText(this, R.string.add_to_calendar_success, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Error opening calendar: " + e.getMessage());
                Toast.makeText(this, R.string.calendar_not_found, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void buildEventCardFinalStep(WeeklyRace weeklyRace) {
        List<Session> sessions = weeklyRace.getSessions();
        Session nextEvent = weeklyRace.findNextEvent(sessions);
        boolean underway = weeklyRace.isUnderway(false) && !weeklyRace.isWeekFinished();

        createWeekSchedule(sessions, weeklyRace.getRound());

        String eventTitle = weeklyRace.getRaceName() != null
                ? weeklyRace.getRaceName().toUpperCase()
                : null;
        String totalLaps = (track != null && track.getLaps() != null) ? track.getLaps() : null;

        // TEST ONLY – decommentare per forzare la live card e testare OpenF1 senza GP in corso:
        //setLiveSession(eventTitle, totalLaps);

        if (nextEvent != null && !underway) {
            LocalDateTime eventDateTime = nextEvent.getStartDateTime();
            startCountdown(eventDateTime);
        } else if (!underway) {
            showResults(weeklyRace);
        } else {
            setLiveSession(eventTitle, totalLaps);
        }
    }

    private void setLiveSession(String eventTitle, String totalLaps) {
        View liveSession = findViewById(R.id.event_live_card);
        View noLiveSession = findViewById(R.id.event_not_live_card);

        liveSession.setVisibility(View.VISIBLE);
        noLiveSession.setVisibility(View.GONE);

        ImageView liveIcon = findViewById(R.id.live_icon);
        Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse_dynamic);
        liveIcon.startAnimation(pulse);

        // Al click apre la LiveActivity passando il titolo dell'evento e i giri totali
        liveSession.setOnClickListener(v -> NavigationUtils.navigateToLivePage(this, eventTitle, totalLaps));

        Log.i("ActivityDataLog", "DATA_AND_IMAGES_FULLY_LOADED: EventActivity at " + System.currentTimeMillis());
        loadingScreen.hideLoadingScreen();
    }

    private void startCountdown(LocalDateTime eventDate) {
        loadingScreen.updateProgress();

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

        Log.i("ActivityDataLog", "DATA_AND_IMAGES_FULLY_LOADED: EventActivity at " + System.currentTimeMillis());
        loadingScreen.hideLoadingScreen();
    }

    private void showRaceResultsDialog(Race race) {
        if (race != null) {
            if (race.getRaceResults() == null || race.getRaceResults().isEmpty()) {
                Log.e(TAG, "race results not found");
                if (race.getSprintResults() == null || race.getSprintResults().isEmpty()) {
                    Log.e(TAG, "sprint results not found");
                } else {
                    Log.i(TAG, "Showing sprint results");

                    fetchStintsAndShowDialog(race, "Sprint");
                }
            } else {
                Log.i(TAG, "Showing race results");

                fetchStintsAndShowDialog(race, "Race");
            }
        } else {
            Log.e(TAG, "race is null, cannot show results");
        }
    }

    private void fetchStintsAndShowDialog(Race race, String sessionName) {
        RaceResultFastestLap raceFastestLap;
        if (sessionName.equals("Sprint")) {
            raceFastestLap = eventViewModel.extractFastestLap(race.getSprintResults());
        } else {
            raceFastestLap = eventViewModel.extractFastestLap(race.getRaceResults());
        }

        List<Stint> cachedStints = sessionName.equals("Sprint") ? race.getSprintStints() : race.getRaceStints();
        if (cachedStints == null || cachedStints.isEmpty()) {
            cachedStints = race.getStints();
        }

        if (loadingScreen != null) {
            loadingScreen.showLoadingScreen(true);
        }

        List<Stint> finalCachedStints = cachedStints;
        MutableLiveData<Result> stintsLiveData = raceResultViewModel.getStints(race.getRaceName(), sessionName);
        Observer<Result> observer = new Observer<>() {
            @Override
            public void onChanged(Result result) {
                if (result instanceof Result.Loading) {
                    Log.i(TAG, "Loading stints...");
                    return;
                }

                stintsLiveData.removeObserver(this);

                if (loadingScreen != null) {
                    loadingScreen.hideLoadingScreenImmediately();
                }

                List<Stint> stints = null;
                if (result instanceof Result.StintsSuccess) {
                    Log.i(TAG, "Stints loaded successfully");

                    stints = ((Result.StintsSuccess) result).getData();
                    if (sessionName.equals("Sprint")) {
                        race.setSprintStints(stints);
                    } else {
                        race.setRaceStints(stints);
                    }
                    Log.i(TAG, "Stints:\n " + stints);

                } else if (result instanceof Result.Error) {
                    Log.e(TAG, "Error loading stints: " + result.getError());
                    stints = finalCachedStints;
                }

                NavigationUtils.showRaceResults(EventActivity.this, race, 0, (stints != null && !stints.isEmpty()) ? stints : finalCachedStints, raceFastestLap);
            }
        };
        stintsLiveData.observe(this, observer);
    }

    private void showQualifyingResultsDialog(Race race) {
        if (race != null) {
            if (race.getQualifyingResults() != null && !race.getQualifyingResults().isEmpty()) {
                Log.i(TAG, "Showing qualifying results");
                NavigationUtils.showRaceResults(this, race, 1, null, null);
            } else {
                Log.e(TAG, "qualifying results not found");
            }
        } else {
            Log.e(TAG, "race is null, cannot show qualifying results");
        }
    }

    private void showResults(WeeklyRace weeklyRace) {
        loadingScreen.updateProgress();

        View countdownView = findViewById(R.id.timer_card_countdown);
        View resultsView = findViewById(R.id.timer_card_results);

        countdownView.setVisibility(View.GONE);
        resultsView.setVisibility(View.VISIBLE);

        processRaceResults(weeklyRace);
    }

    private void processRaceResults(WeeklyRace weeklyRace) {
        Log.i(TAG, "Processing race results for round: " + weeklyRace.getRound());

        MutableLiveData<Result> resultMutableLiveData = raceResultViewModel.getRaceResults(weeklyRace.getRound());
        @SuppressWarnings("unchecked")
        androidx.lifecycle.Observer<Result>[] observerHolder = new androidx.lifecycle.Observer[1];
        observerHolder[0] = result -> {
            if (result instanceof Result.Loading) {
                return;
            }
            resultMutableLiveData.removeObserver(observerHolder[0]);

            try {
                Race race = ((Result.RaceResultsSuccess) result).getData();
                List<RaceResult> podium = race != null ? race.getRaceResults() : null;

                if (podium == null || podium.isEmpty()) {
                    showPendingResults();
                } else {
                    this.currentRace = race;

                    Log.i(TAG, "Podium found, size: " + podium.size());
                    for (int i = 0; i < 3 && i < podium.size(); i++) {
                        String teamId = podium.get(i).getConstructor().getConstructorId();

                        UIUtils.singleSetTextViewText(podium.get(i).getDriver().getFullName(),
                                findViewById(Constants.PODIUM_DRIVER_NAME.get(i)));

                        LinearLayout teamColor = findViewById(Constants.PODIUM_TEAM_COLOR.get(i));
                        Integer teamColorObj = Constants.TEAM_COLOR.get(teamId);
                        teamColor.setBackgroundColor(ContextCompat.getColor(this, Objects.requireNonNullElseGet(teamColorObj, () -> R.color.mercedes_f1)));
                    }

                    View resultsView = findViewById(R.id.timer_card_results);
                    resultsView.setOnClickListener(v -> showRaceResultsDialog(currentRace));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing race results: " + e.getMessage());
                showPendingResults();
            } finally {
                Log.i("ActivityDataLog", "DATA_AND_IMAGES_FULLY_LOADED: EventActivity at " + System.currentTimeMillis());
                loadingScreen.hideLoadingScreen();
            }
        };
        resultMutableLiveData.observe(this, observerHolder[0]);
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

    private void createWeekSchedule(List<Session> sessions, String round) {
        View eventSchedule = findViewById(R.id.event_schedule_table);
        loadingScreen.updateProgress();

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
                    session.getStartingTime(),
                    session.getTime(),
                    eventSchedule.findViewById(Constants.SESSION_TIME_FIELD.get(sessionId)));

            setChequeredFlag(eventSchedule, session, round);
        }
    }

    private void setChequeredFlag(View view, Session session, String round) {
        String sessionId = session.getClass().getSimpleName();
        if (session.isPractice()) {
            Practice practice = (Practice) session;
            sessionId = practice.getPractice();
        }

        if (session.isFinished()) {
            ImageView flag = view.findViewById(Constants.SESSION_FLAG_FIELD.get(sessionId));
            flag.setVisibility(View.VISIBLE);

            LinearLayout currentSession = view.findViewById(Constants.SESSION_ROW.get(sessionId));
            currentSession.setClickable(true);
            currentSession.setFocusable(true);
            currentSession.setOnClickListener(v -> manageSessionScheduleClick(session, round));
        }
    }

    private void manageSessionScheduleClick(Session session, String round) {

        Log.i(TAG, "session id clicked: " + session.getClass().getSimpleName());
        if (session.isRace()) {
            showRaceResultsDialog(currentRace);
        }
        if (session.isQualifying()) {
            processQualifyingData(round);
        }
        if (session.isSprint()) {
            processSprintData(round);
        }

    }

    private void processQualifyingData(String round) {
        Log.d(TAG, "Processing qualifying data for round: " + round);
        if (loadingScreen != null) {
            loadingScreen.showLoadingScreen(true);
        }

        MutableLiveData<Result> qualifyingLiveData = raceResultViewModel.getQualifyingResults(round);
        Observer<Result> observer = new Observer<>() {
            @Override
            public void onChanged(Result result) {
                if (result instanceof Result.Loading) {
                    return;
                }

                qualifyingLiveData.removeObserver(this);

                if (loadingScreen != null) {
                    loadingScreen.hideLoadingScreenImmediately();
                }

                try {
                    if (result instanceof Result.RaceResultsSuccess) {
                        Race race = ((Result.RaceResultsSuccess) result).getData();
                        List<QualifyingResult> qualifyingResults = race != null ? race.getQualifyingResults() : null;

                        if (qualifyingResults == null || qualifyingResults.isEmpty()) {
                            Log.i(TAG, "No qualifying results found");
                            Toast.makeText(EventActivity.this, "No qualifying results found", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.i(TAG, "Qualifying results found: " + qualifyingResults.size());
                            showQualifyingResultsDialog(race);
                        }
                    } else if (result instanceof Result.Error) {
                        Toast.makeText(EventActivity.this, "Error loading qualifying results", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing qualifying data: " + e.getMessage());
                }
            }
        };
        qualifyingLiveData.observe(this, observer);
    }

    private void processSprintData(String round) {
        Log.d(TAG, "Processing sprint data for round: " + round);
        if (loadingScreen != null) {
            loadingScreen.showLoadingScreen(true);
        }

        MutableLiveData<Result> sprintLiveData = raceResultViewModel.getSprintResults(round);
        Observer<Result> observer = new Observer<Result>() {
            @Override
            public void onChanged(Result result) {
                if (result instanceof Result.Loading) {
                    return;
                }

                sprintLiveData.removeObserver(this);

                if (loadingScreen != null) {
                    loadingScreen.hideLoadingScreenImmediately();
                }

                try {
                    if (result instanceof Result.RaceResultsSuccess) {
                        Race race = ((Result.RaceResultsSuccess) result).getData();
                        List<RaceResult> sprintResults = race != null ? race.getSprintResults() : null;

                        if (sprintResults == null || sprintResults.isEmpty()) {
                            Log.i(TAG, "No sprint results found");
                            Toast.makeText(EventActivity.this, "No sprint results found", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.i(TAG, "Sprint results found: " + sprintResults.size());
                            showRaceResultsDialog(race);
                        }
                    } else if (result instanceof Result.Error) {
                        Toast.makeText(EventActivity.this, "Error loading sprint results", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing sprint data: " + e.getMessage());
                }
            }
        };
        sprintLiveData.observe(this, observer);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}