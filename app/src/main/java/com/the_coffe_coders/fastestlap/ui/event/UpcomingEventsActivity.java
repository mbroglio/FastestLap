package com.the_coffe_coders.fastestlap.ui.event;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.adapter.f1.UpcomingEventsRecyclerAdapter;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.TrackViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.TrackViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.EventViewModel;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.EventViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.WeeklyRaceViewModel;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.WeeklyRaceViewModelFactory;
import com.the_coffe_coders.fastestlap.util.CalendarUtils;
import com.the_coffe_coders.fastestlap.util.ui.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import java.util.ArrayList;
import java.util.List;

public class UpcomingEventsActivity extends AppCompatActivity {

    private static final String TAG = "UpcomingEventsActivity";

    // ── ViewModel fields ──────────────────────────────────────────────
    EventViewModel eventViewModel;
    TrackViewModel trackViewModel;
    WeeklyRaceViewModel weeklyRaceViewModel;

    // ── UI fields ─────────────────────────────────────────────────────
    private MaterialToolbar toolbar;
    LoadingScreen loadingScreen;
    private SwipeRefreshLayout upcomingEventsLayout;
    private UpcomingEventsRecyclerAdapter upcomingEventsAdapter;
    private List<WeeklyRace> racesList;

    // ── State ─────────────────────────────────────────────────────────
    private boolean dataLoaded = false;
    private LiveData<Result> currentWeeklyRaceObserver;

    // ─────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_upcoming_events);

        start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume - dataLoaded: " + dataLoaded);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy - cleaning up observers");
        if (currentWeeklyRaceObserver != null) {
            currentWeeklyRaceObserver.removeObservers(this);
            currentWeeklyRaceObserver = null;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Initialization
    // ─────────────────────────────────────────────────────────────────

    private void start() {
        upcomingEventsLayout = findViewById(R.id.upcoming_events_layout);
        loadingScreen = new LoadingScreen(getWindow().getDecorView(), this, upcomingEventsLayout, null);

        // Show loading only if data hasn't been loaded yet
        if (!dataLoaded) {
            loadingScreen.showLoadingScreen(true);
        }

        eventViewModel      = new ViewModelProvider(this, new EventViewModelFactory(getApplication())).get(EventViewModel.class);
        trackViewModel      = new ViewModelProvider(this, new TrackViewModelFactory(getApplication())).get(TrackViewModel.class);
        weeklyRaceViewModel = new ViewModelProvider(this, new WeeklyRaceViewModelFactory(getApplication(), this)).get(WeeklyRaceViewModel.class);

        toolbar = findViewById(R.id.topAppBar);
        UIUtils.applyWindowInsets(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        UIUtils.applyWindowInsets(upcomingEventsLayout);
        upcomingEventsLayout.setOnRefreshListener(() -> {
            refreshData();
            upcomingEventsLayout.setRefreshing(false);
        });

        setupRecyclerView();

        // Load data only if not already loaded
        if (!dataLoaded) {
            processEvents();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Refresh / Clear
    // ─────────────────────────────────────────────────────────────────

    private void refreshData() {
        Log.i(TAG, "Refreshing data...");
        clearData();
        dataLoaded = false;
        processEvents();
    }

    private void clearData() {
        Log.i(TAG, "Clearing data...");

        if (currentWeeklyRaceObserver != null) {
            currentWeeklyRaceObserver.removeObservers(this);
            currentWeeklyRaceObserver = null;
        }

        if (racesList != null) {
            racesList.clear();
        }
        if (upcomingEventsAdapter != null) {
            runOnUiThread(() -> upcomingEventsAdapter.notifyDataSetChanged());
        }

        loadingScreen.showLoadingScreen(true);
    }

    // ─────────────────────────────────────────────────────────────────
    // RecyclerView setup
    // ─────────────────────────────────────────────────────────────────

    private void setupRecyclerView() {
        if (racesList == null) {
            racesList = new ArrayList<>();
        }

        RecyclerView upcomingEventsRecyclerView = findViewById(R.id.upcoming_events_recycler_view);
        upcomingEventsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        if (upcomingEventsAdapter == null) {
            upcomingEventsAdapter = new UpcomingEventsRecyclerAdapter(
                    this, racesList, trackViewModel, this, loadingScreen);
            upcomingEventsRecyclerView.setAdapter(upcomingEventsAdapter);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Data loading
    // ─────────────────────────────────────────────────────────────────

    private void processEvents() {
        Log.i(TAG, "Process Event");

        // Remove any previous observer
        if (currentWeeklyRaceObserver != null) {
            currentWeeklyRaceObserver.removeObservers(this);
        }

        currentWeeklyRaceObserver = weeklyRaceViewModel.getWeeklyRacesLiveData();
        currentWeeklyRaceObserver.observe(this, result -> {
            if (result instanceof Result.Loading) {
                return;
            }
            if (result.isSuccess()) {
                List<WeeklyRace> races = ((Result.WeeklyRaceSuccess) result).getData();
                Log.i(TAG, "SUCCESS – total races: " + races.size());

                List<WeeklyRace> upcomingRaces = eventViewModel.extractUpcomingRaces(races);

                // Update the existing adapter's data instead of recreating it
                racesList.clear();
                racesList.addAll(upcomingRaces);
                runOnUiThread(() -> {
                    upcomingEventsAdapter.updateTargetLoadCount();
                    upcomingEventsAdapter.notifyDataSetChanged();
                });

                dataLoaded = true;
                setMenu(upcomingRaces);

            } else {
                Log.e(TAG, "Failed to load weekly races");
                loadingScreen.hideLoadingScreen();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────
    // Menu
    // ─────────────────────────────────────────────────────────────────

    private void setMenu(List<WeeklyRace> upcomingRaces) {
        MenuItem addToCalendarItem = toolbar.getMenu().findItem(R.id.add_to_calendar);
        if (addToCalendarItem == null) return;

        addToCalendarItem.setOnMenuItemClickListener(v -> {
            for (WeeklyRace race : upcomingRaces) {
                CalendarUtils.addWeekendToCalendar(this, race);
            }
            return true;
        });
    }
}