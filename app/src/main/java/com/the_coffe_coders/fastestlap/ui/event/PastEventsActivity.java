package com.the_coffe_coders.fastestlap.ui.event;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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
import com.the_coffe_coders.fastestlap.adapter.PastEventsRecyclerAdapter;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;
import com.the_coffe_coders.fastestlap.domain.grand_prix.RaceResult;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Track;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.TrackViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.TrackViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.EventViewModel;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.EventViewModelFactory;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.UIUtils;

import org.threeten.bp.LocalDateTime;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PastEventsActivity extends AppCompatActivity {

    LoadingScreen loadingScreen;
    EventViewModel eventViewModel;
    TrackViewModel trackViewModel;

    private SwipeRefreshLayout pastEventsLayout;

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

        loadingScreen.showLoadingScreen(true);

        eventViewModel = new ViewModelProvider(this, new EventViewModelFactory(getApplication())).get(EventViewModel.class);
        trackViewModel = new ViewModelProvider(this, new TrackViewModelFactory(getApplication())).get(TrackViewModel.class);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        UIUtils.applyWindowInsets(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        UIUtils.applyWindowInsets(pastEventsLayout);
        pastEventsLayout.setOnRefreshListener(() -> {
            start();
            pastEventsLayout.setRefreshing(false);
        });

        processEvents();
    }

    private void processEvents() {
        Log.i("PastEvent", "Process Event");

        loadingScreen.postLoadingStatus(this.getString(R.string.initializing));

        LiveData<Result> dataEvent = eventViewModel.getWeeklyRacesLiveData();
        dataEvent.observe(this, resultEvent -> {
            Log.i("PastEvent", "observed");
            if (resultEvent instanceof Result.Loading) {
                return;
            }
            if (resultEvent.isSuccess()) {
                List<WeeklyRace> eventRaces = ((Result.WeeklyRaceSuccess) resultEvent).getData();
                int totalRaces = eventViewModel.extractPastRaces(eventRaces).size();
                MutableLiveData<Result> data = eventViewModel.getAllResults(totalRaces);
                data.observe(this, result -> {
                    Log.i("PastEvent", "observed");
                    if (result.isSuccess()) {
                        List<Race> races = ((Result.RaceSuccess) result).getData();
                        races.sort(Comparator.comparingInt(Race::getRoundAsInt));
                        Collections.reverse(races);

                        RecyclerView pastEventsRecyclerView = findViewById(R.id.past_events_recycler_view);
                        pastEventsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                        PastEventsRecyclerAdapter pastEventsAdapter = new PastEventsRecyclerAdapter(this, races, trackViewModel, this, loadingScreen);
                        pastEventsRecyclerView.setAdapter(pastEventsAdapter);

                        for (int i = 0; i < pastEventsAdapter.getItemCount(); i++) {
                            pastEventsAdapter.onBindViewHolder(
                                    pastEventsAdapter.createViewHolder(pastEventsRecyclerView, pastEventsAdapter.getItemViewType(i)), i);
                        }
                    } else {
                        loadingScreen.hideLoadingScreen();
                    }
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}