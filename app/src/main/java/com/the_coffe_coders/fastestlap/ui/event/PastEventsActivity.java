package com.the_coffe_coders.fastestlap.ui.event;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.adapter.f1.PastEventsRecyclerAdapter;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Race;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.TrackViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.TrackViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.EventViewModel;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.EventViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.RaceResultViewModel;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.RaceResultViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.WeeklyRaceViewModel;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.WeeklyRaceViewModelFactory;
import com.the_coffe_coders.fastestlap.util.ui.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PastEventsActivity extends AppCompatActivity {

    LoadingScreen loadingScreen;
    EventViewModel eventViewModel;
    TrackViewModel trackViewModel;
    RaceResultViewModel raceResultViewModel;
    WeeklyRaceViewModel weeklyRaceViewModel;

    private SwipeRefreshLayout pastEventsLayout;
    private PastEventsRecyclerAdapter pastEventsAdapter;
    private List<Race> racesList;
    private int totalRaces;
    private int loadedRaces;

    // Flag per controllare se i dati sono già stati caricati
    private boolean dataLoaded = false;

    // Observer per il cleanup
    private LiveData<Result> currentWeeklyRaceObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_past_events);

        start();
    }

    private void start() {
        pastEventsLayout = findViewById(R.id.past_events_layout);
        loadingScreen = new LoadingScreen(getWindow().getDecorView(), this, pastEventsLayout, null);

        // Mostra loading solo se i dati non sono già stati caricati
        if (!dataLoaded) {
            loadingScreen.showLoadingScreen(true);
        }

        eventViewModel = new ViewModelProvider(this, new EventViewModelFactory(getApplication())).get(EventViewModel.class);
        trackViewModel = new ViewModelProvider(this, new TrackViewModelFactory(getApplication())).get(TrackViewModel.class);
        raceResultViewModel = new ViewModelProvider(this, new RaceResultViewModelFactory(getApplication(), this)).get(RaceResultViewModel.class);
        weeklyRaceViewModel = new ViewModelProvider(this, new WeeklyRaceViewModelFactory(getApplication(), this)).get(WeeklyRaceViewModel.class);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        UIUtils.applyWindowInsets(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        UIUtils.applyWindowInsets(pastEventsLayout);
        pastEventsLayout.setOnRefreshListener(() -> {
            refreshData();
            pastEventsLayout.setRefreshing(false);
        });

        setupRecyclerView();

        // Carica i dati solo se non sono già stati caricati
        if (!dataLoaded) {
            processEvents();
        }
    }

    private void refreshData() {
        Log.i("PastEvent", "Refreshing data...");
        // Pulisci i dati esistenti
        clearData();
        // Resetta il flag
        dataLoaded = false;
        // Ricarica i dati
        processEvents();
    }

    private void clearData() {
        Log.i("PastEvent", "Clearing data...");

        // Rimuovi tutti gli observer attivi
        if (currentWeeklyRaceObserver != null) {
            currentWeeklyRaceObserver.removeObservers(this);
            currentWeeklyRaceObserver = null;
        }

        if (racesList != null) {
            racesList.clear();
        }
        if (pastEventsAdapter != null) {
            runOnUiThread(() -> pastEventsAdapter.notifyDataSetChanged());
        }
        totalRaces = 0;
        loadedRaces = 0;
        loadingScreen.showLoadingScreen(true);
    }

    private void setupRecyclerView() {
        if (racesList == null) {
            racesList = new ArrayList<>();
        }

        RecyclerView pastEventsRecyclerView = findViewById(R.id.past_events_recycler_view);
        pastEventsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        if (pastEventsAdapter == null) {
            pastEventsAdapter = new PastEventsRecyclerAdapter(this, racesList, trackViewModel, this, loadingScreen);
            pastEventsRecyclerView.setAdapter(pastEventsAdapter);
        }
    }

    private void processEvents() {
        Log.i("PastEvent", "Process Event");

        // Rimuovi observer precedenti se esistono
        if (currentWeeklyRaceObserver != null) {
            currentWeeklyRaceObserver.removeObservers(this);
        }

        currentWeeklyRaceObserver = weeklyRaceViewModel.getWeeklyRacesLiveData();
        currentWeeklyRaceObserver.observe(this, resultEvent -> {
            Log.i("PastEvent", "observed");
            if (resultEvent instanceof Result.Loading) {
                return;
            }
            if (resultEvent.isSuccess()) {
                List<WeeklyRace> eventRaces = ((Result.WeeklyRaceSuccess) resultEvent).getData();
                List<WeeklyRace> pastRaces = eventViewModel.extractPastRaces(eventRaces);
                totalRaces = pastRaces.size();
                loadedRaces = 0;

                Log.i("PastEvent", "Starting to load " + totalRaces + " races in parallel");

                loadRacesInParallel(pastRaces);
            } else {
                Log.e("PastEvent", "Failed to load weekly races");
                loadingScreen.hideLoadingScreen();
            }
        });
    }

    private void loadRacesInParallel(List<WeeklyRace> pastRaces) {
        if (pastRaces == null || pastRaces.isEmpty()) {
            sortAndUpdateList();
            loadingScreen.hideLoadingScreen();
            dataLoaded = true;
            return;
        }

        totalRaces = pastRaces.size();
        List<Race> fetchedRaces = java.util.Collections.synchronizedList(new ArrayList<>());
        final int[] completedCount = {0};

        for (WeeklyRace weeklyRace : pastRaces) {
            MutableLiveData<Result> singleRaceData = raceResultViewModel.getRaceResults(weeklyRace.getRound());
            @SuppressWarnings("unchecked")
            androidx.lifecycle.Observer<Result>[] observerHolder = new androidx.lifecycle.Observer[1];
            observerHolder[0] = result -> {
                if (result instanceof Result.Loading) {
                    return;
                }
                singleRaceData.removeObserver(observerHolder[0]);
                if (result.isSuccess()) {
                    Race race = ((Result.RaceResultsSuccess) result).getData();
                    fetchedRaces.add(race);
                }
                synchronized (completedCount) {
                    completedCount[0]++;
                    if (completedCount[0] >= totalRaces) {
                        racesList.clear();
                        racesList.addAll(fetchedRaces);
                        sortAndUpdateList();
                        dataLoaded = true;
                    }
                }
            };
            singleRaceData.observe(this, observerHolder[0]);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void sortAndUpdateList() {
        // Ordina la lista finale per sicurezza (dalla più recente alla meno recente)
        racesList.sort(Comparator.comparingInt(Race::getRoundAsInt));
        Collections.reverse(racesList);

        runOnUiThread(() -> {
            pastEventsAdapter.updateTargetLoadCount();
            pastEventsAdapter.notifyDataSetChanged();
            Log.i("PastEvent", "Final sort completed, total races: " + racesList.size());
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("PastEvent", "onResume - dataLoaded: " + dataLoaded);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("PastEvent", "onPause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("PastEvent", "onDestroy - cleaning up observers");

        // Cleanup degli observer
        if (currentWeeklyRaceObserver != null) {
            currentWeeklyRaceObserver.removeObservers(this);
            currentWeeklyRaceObserver = null;
        }
    }
}