package com.the_coffe_coders.fastestlap.ui.event;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Session;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.SessionStatus;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.domain.f1.track.Track;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.NationViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.NationViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.TrackViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.TrackViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.event.handler.EventCountdownHandler;
import com.the_coffe_coders.fastestlap.ui.event.handler.EventHeaderHandler;
import com.the_coffe_coders.fastestlap.ui.event.handler.EventLiveHandler;
import com.the_coffe_coders.fastestlap.ui.event.handler.EventResultsHandler;
import com.the_coffe_coders.fastestlap.ui.event.handler.EventScheduleHandler;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.EventViewModel;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.EventViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.RaceResultViewModel;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.RaceResultViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.WeeklyRaceViewModel;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.WeeklyRaceViewModelFactory;
import com.the_coffe_coders.fastestlap.util.ui.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import org.threeten.bp.LocalDate;

import java.util.ArrayList;
import java.util.List;

public class EventActivity extends AppCompatActivity {
    private static final String TAG = "EventActivity";

    private LoadingScreen loadingScreen;
    private EventViewModel eventViewModel;
    private RaceResultViewModel raceResultViewModel;
    private WeeklyRaceViewModel weeklyRaceViewModel;
    private TrackViewModel trackViewModel;
    private NationViewModel nationViewModel;

    private EventHeaderHandler headerHandler;
    private EventLiveHandler liveHandler;
    private EventCountdownHandler countdownHandler;
    private EventScheduleHandler scheduleHandler;
    private EventResultsHandler resultsHandler;

    private String trackId;
    private SwipeRefreshLayout eventLayout;

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
            if (weeklyRaceViewModel != null) {
                weeklyRaceViewModel.refreshWeeklyRaces();
            }
            processRaceData();
        });

        trackId = getIntent().getStringExtra("CIRCUIT_ID");
        Log.i(TAG, "Circuit ID: " + trackId);

        initializeViewModels();
        initializeHandlers();

        headerHandler.setupOuterCard();
        processRaceData();
    }

    private void initializeViewModels() {
        eventViewModel = new ViewModelProvider(this, new EventViewModelFactory(getApplication())).get(EventViewModel.class);
        raceResultViewModel = new ViewModelProvider(this, new RaceResultViewModelFactory(getApplication(), this)).get(RaceResultViewModel.class);
        weeklyRaceViewModel = new ViewModelProvider(this, new WeeklyRaceViewModelFactory(getApplication(), this)).get(WeeklyRaceViewModel.class);
        trackViewModel = new ViewModelProvider(this, new TrackViewModelFactory(getApplication())).get(TrackViewModel.class);
        nationViewModel = new ViewModelProvider(this, new NationViewModelFactory(getApplication())).get(NationViewModel.class);
    }

    private void initializeHandlers() {
        View rootView = getWindow().getDecorView();
        headerHandler = new EventHeaderHandler(this, rootView, loadingScreen, trackViewModel, nationViewModel);
        liveHandler = new EventLiveHandler(this, rootView, loadingScreen);
        countdownHandler = new EventCountdownHandler(this, rootView, loadingScreen);
        scheduleHandler = new EventScheduleHandler(this, rootView, loadingScreen);
        resultsHandler = new EventResultsHandler(this, rootView, loadingScreen, raceResultViewModel, eventViewModel);
    }

    private void processRaceData() {
        List<WeeklyRace> races = new ArrayList<>();
        LiveData<Result> data = weeklyRaceViewModel.getWeeklyRacesLiveData();
        @SuppressWarnings("unchecked")
        Observer<Result>[] observerHolder = new Observer[1];
        observerHolder[0] = result -> {
            if (result instanceof Result.Loading) {
                return;
            }
            data.removeObserver(observerHolder[0]);
            eventLayout.setRefreshing(false);
            if (result.isSuccess()) {
                Log.i(TAG, "Weekly races loaded successfully");
                races.addAll(((Result.WeeklyRaceSuccess) result).getData());

                WeeklyRace weeklyRace = null;
                for (WeeklyRace race : races) {
                    if (race.getTrack().getTrackId().equals(trackId)) {
                        weeklyRace = race;
                        break;
                    }
                }

                if (weeklyRace != null) {
                    WeeklyRace targetRace = weeklyRace;
                    headerHandler.loadHeaderData(targetRace, trackId, (track, nation) -> setupEventDetails(targetRace, track));
                } else {
                    Log.e(TAG, "Weekly race not found for trackId: " + trackId);
                    loadingScreen.hideLoadingScreen();
                }
            } else {
                Log.e(TAG, "Error loading weekly races: " + result.getError());
                loadingScreen.hideLoadingScreen();
            }
        };
        data.observe(this, observerHolder[0]);
    }

    private void setupEventDetails(WeeklyRace weeklyRace, Track track) {
        resultsHandler.setTrack(track);

        List<Session> sessions = weeklyRace.getSessions();
        if (sessions != null) {
            weeklyRace.setSessions(sessions);
        }
        Session nextEvent = weeklyRace.findNextEvent(sessions);
        boolean underway = weeklyRace.isUnderway(false) && !weeklyRace.isWeekFinished();

        scheduleHandler.createWeekSchedule(weeklyRace, sessions, (session, round) ->
                resultsHandler.manageSessionScheduleClick(session, round));

        Session activeSession = null;
        if (sessions != null) {
            for (Session s : sessions) {
                if (s != null && (s.isUnderway() || s.getSessionStatus() == SessionStatus.IN_PROGRESS)) {
                    activeSession = s;
                    break;
                }
            }
            if (activeSession == null) {
                // Se nessuna sessione è marcata IN_PROGRESS, cerca la sessione della data odierna
                LocalDate today = LocalDate.now();
                for (Session s : sessions) {
                    if (s != null && s.getStartDateTime() != null) {
                        if (s.getStartDateTime().toLocalDate().equals(today)) {
                            activeSession = s;
                            break;
                        }
                    }
                }
            }
        }

        Track resolvedTrack = (track != null) ? track : (weeklyRace != null ? weeklyRace.getTrack() : null);
        liveHandler.setupLiveSession(weeklyRace, resolvedTrack, trackId, activeSession, nextEvent);

        if (nextEvent != null && !underway) {
            countdownHandler.startCountdown(nextEvent.getStartDateTime(), () -> {});
        } else if (!underway) {
            resultsHandler.showResults(weeklyRace, track);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countdownHandler != null) {
            countdownHandler.cancelCountdown();
        }
    }
}