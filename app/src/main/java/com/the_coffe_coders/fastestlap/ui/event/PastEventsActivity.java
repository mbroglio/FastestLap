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
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.the_coffe_coders.fastestlap.R;
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
import com.the_coffe_coders.fastestlap.util.ServiceLocator;
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
    private int counter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_past_events);

        start();

    }

    private void start(){
        pastEventsLayout = findViewById(R.id.past_events_layout);
        loadingScreen = new LoadingScreen(getWindow().getDecorView(), this, pastEventsLayout, null);

        loadingScreen.showLoadingScreen();

        eventViewModel = new ViewModelProvider(this, new EventViewModelFactory(getApplication())).get(EventViewModel.class);
        trackViewModel = new ViewModelProvider(this, new TrackViewModelFactory(ServiceLocator.getInstance().getTrackRepository())).get(TrackViewModel.class);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        UIUtils.applyWindowInsets(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        UIUtils.applyWindowInsets(pastEventsLayout);
        pastEventsLayout.setOnRefreshListener(() ->{
            counter = 0;
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

                        LinearLayout pastEvents = findViewById(R.id.past_events_list);
                        pastEvents.removeAllViews();
                        //List<WeeklyRace> pastRaces = eventViewModel.extractPastRaces(races);
                        for (int i=0; i < races.size(); i++) {
                            createEventCard(pastEvents, races.get(i), i, races.size());
                        }

                        View space = new View(PastEventsActivity.this);
                        space.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Constants.SPACER_HEIGHT));
                        pastEvents.addView(space);

                        loadingScreen.hideLoadingScreen();
                    } else {
                        loadingScreen.hideLoadingScreen();
                    }
                });
            }
        });

    }

    private void createEventCard(LinearLayout eventsListLayout, Race race, int i, int totalRaces) {

        eventsListLayout.addView(generateEventCard(race, i, totalRaces));

        View space = new View(PastEventsActivity.this);
        space.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20));
        eventsListLayout.addView(space);
    }

    private View generateEventCard(Race weeklyRace, int i, int totalRaces) {
        View eventCard = getLayoutInflater().inflate(R.layout.past_event_card, null);

        MutableLiveData<Result> trackData = trackViewModel.getTrack(weeklyRace.getTrack().getTrackId());

        trackData.observe(this, result -> {
            if (result instanceof Result.Loading) {
                return;
            }
            if (result.isSuccess()) {
                Track track = ((Result.TrackSuccess) result).getData();

                LocalDateTime raceDateTime = weeklyRace.getStartDateTime();

                UIUtils.multipleSetTextViewText(
                        new String[]{raceDateTime.getDayOfMonth() + "", raceDateTime.getMonth().toString().substring(0, 3)},

                        new TextView[]{eventCard.findViewById(R.id.past_date),eventCard.findViewById(R.id.past_month)}
                );

                ImageView trackOutline = eventCard.findViewById(R.id.past_track_outline);

                UIUtils.loadImageWithGlide(this, track.getTrack_minimal_layout_url(), trackOutline, () ->
                        generateEventCardFinalStep(eventCard, weeklyRace, i, totalRaces));

            }

        });

        return eventCard;
    }

    private void generateEventCardFinalStep(View eventCard, Race weeklyRace, int i, int totalRaces) {

        UIUtils.multipleSetTextViewText(
                new String[]{this.getString(R.string.round_upper_case_plus_value, weeklyRace.getRound()), weeklyRace.getTrack().getCountry()},

                new TextView[]{eventCard.findViewById(R.id.past_round_number), eventCard.findViewById(R.id.past_gp_name)});

        generatePodium(eventCard, weeklyRace, i, totalRaces);

        Log.i("PastEvent", "gpName: " + weeklyRace.getRaceName());
        eventCard.setOnClickListener(v -> {
            Intent intent = new Intent(PastEventsActivity.this, EventActivity.class);
            intent.putExtra("CIRCUIT_ID", weeklyRace.getTrack().getTrackId());
            startActivity(intent);
        });
    }

    private void generatePodium(View eventCard, Race weeklyRace, int i, int totalRaces) {
        if (weeklyRace.getResults() == null) {
            setPendingPodium(eventCard);
        } else {
            for (int k = 0; k < 3; k++) {
                RaceResult raceResult = weeklyRace.getResults().get(k);
                UIUtils.singleSetTextViewText(raceResult.getDriver().getFullName(), eventCard.findViewById(Constants.PAST_RACE_DRIVER_NAME.get(k)));
            }
        }
        loadingScreen.postLoadingStatus(this.getString(R.string.generating_event_card, Integer.toString(i + 1), Integer.toString(totalRaces)));
        loadingScreen.updateProgress((i + 1) * 100 / totalRaces);
        counter++;

        loadingScreen.hideLoadingScreenWithCondition(counter == totalRaces - 1);
    }

    private void setPendingPodium(View eventCard) {
        View pendingResults = eventCard.findViewById(R.id.pending_results_text);
        View podium = eventCard.findViewById(R.id.race_podium);
        View arrow = eventCard.findViewById(R.id.past_event_card_arrow);

        pendingResults.setVisibility(View.VISIBLE);
        podium.setVisibility(View.GONE);
        arrow.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}