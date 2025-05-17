package com.the_coffe_coders.fastestlap.ui.event;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.adapter.UpcomingEventsRecyclerAdapter;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.TrackViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.TrackViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.EventViewModel;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.EventViewModelFactory;
import com.the_coffe_coders.fastestlap.util.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.UIUtils;

import java.util.List;

public class UpcomingEventsActivity extends AppCompatActivity {

    private static final String TAG = "UpcomingEventsActivity";
    private final boolean raceToProcess = true;
    LoadingScreen loadingScreen;
    EventViewModel eventViewModel;
    TrackViewModel trackViewModel;
    private SwipeRefreshLayout upcomingEventsLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_upcoming_events);

        start();

    }

    private void start() {
        upcomingEventsLayout = findViewById(R.id.upcoming_events_layout);
        loadingScreen = new LoadingScreen(getWindow().getDecorView(), this, upcomingEventsLayout, null);

        loadingScreen.showLoadingScreen(true);

        eventViewModel = new ViewModelProvider(this, new EventViewModelFactory(getApplication())).get(EventViewModel.class);
        trackViewModel = new ViewModelProvider(this, new TrackViewModelFactory(getApplication())).get(TrackViewModel.class);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        UIUtils.applyWindowInsets(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        UIUtils.applyWindowInsets(upcomingEventsLayout);
        upcomingEventsLayout.setOnRefreshListener(() -> {
            start();
            upcomingEventsLayout.setRefreshing(false);
        });

        processEvents();
    }

    private void processEvents() {
        Log.i("UpcomingEvents", "Process Event");

        MutableLiveData<Result> data = eventViewModel.getWeeklyRacesLiveData();
        data.observe(this, result -> {
            if (result instanceof Result.Loading) {
                return;
            }
            if (result.isSuccess()) {
                List<WeeklyRace> races = ((Result.WeeklyRaceSuccess) result).getData();
                Log.i("UpcomingEvents", "SUCCESS");

                List<WeeklyRace> upcomingRaces = eventViewModel.extractUpcomingRaces(races);

                RecyclerView upcomingEventsRecyclerView = findViewById(R.id.upcoming_events_recycler_view);
                upcomingEventsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                UpcomingEventsRecyclerAdapter upcomingEventsAdapter = new UpcomingEventsRecyclerAdapter(this, upcomingRaces, trackViewModel, this, loadingScreen);
                upcomingEventsRecyclerView.setAdapter(upcomingEventsAdapter);

                for (int i = 0; i < upcomingEventsAdapter.getItemCount(); i++) {
                    upcomingEventsAdapter.onBindViewHolder(
                            upcomingEventsAdapter.createViewHolder(upcomingEventsRecyclerView, upcomingEventsAdapter.getItemViewType(i)), i);
                }
            } else {
                loadingScreen.hideLoadingScreen();
            }

        });

    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}