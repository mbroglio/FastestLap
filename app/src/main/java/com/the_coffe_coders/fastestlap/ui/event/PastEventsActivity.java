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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_past_events);

        eventViewModel = new ViewModelProvider(this, new EventViewModelFactory(getApplication())).get(EventViewModel.class);

        loadingScreen = new LoadingScreen(getWindow().getDecorView(), this);
        loadingScreen.showLoadingScreen();

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);

        UIUtils.applyWindowInsets(toolbar);

        LinearLayout pastEventsLayout = findViewById(R.id.past_events_layout);
        UIUtils.applyWindowInsets(pastEventsLayout);

        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        Log.i("PastEvent", "onCreate");

        processEvents();
    }

    private void processEvents() {
        Log.i("PastEvent", "Process Event");
        EventViewModel eventViewModel = new ViewModelProvider(this, new EventViewModelFactory(getApplication())).get(EventViewModel.class);
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

                        //List<WeeklyRace> pastRaces = eventViewModel.extractPastRaces(races);
                        for (Race race : races) {
                            createEventCard(race);
                        }
                    } else {
                        loadingScreen.hideLoadingScreen();
                    }
                });
            }
        });

    }

    private void createEventCard(Race race) {
        LinearLayout pastEvents = findViewById(R.id.past_events_list);
        pastEvents.addView(generateEventCard(race));

        View space = new View(PastEventsActivity.this);
        space.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20));
        pastEvents.addView(space);
    }

    private View generateEventCard(Race weeklyRace) {
        View eventCard = getLayoutInflater().inflate(R.layout.past_event_card, null);

        TrackViewModel trackViewModel = new ViewModelProvider(this, new TrackViewModelFactory(ServiceLocator.getInstance().getTrackRepository())).get(TrackViewModel.class);
        MutableLiveData<Result> trackData = trackViewModel.getTrack(weeklyRace.getTrack().getTrackId());

        trackData.observe(this, result -> {
            if (result instanceof Result.Loading) {
                return;
            }
            if (result.isSuccess()) {
                Track track = ((Result.TrackSuccess) result).getData();

                LocalDateTime raceDateTime = weeklyRace.getStartDateTime();

                TextView day = eventCard.findViewById(R.id.past_date);
                String dayString = raceDateTime.getDayOfMonth() + "";
                day.setText(dayString);

                TextView month = eventCard.findViewById(R.id.past_month);
                String monthString = raceDateTime.getMonth().toString().substring(0, 3); //Ex: JAN, FEB, etc
                month.setText(monthString);

                ImageView trackOutline = eventCard.findViewById(R.id.past_track_outline);
                Glide.with(this).load(track.getTrack_minimal_layout_url()).into(trackOutline);

                TextView round = eventCard.findViewById(R.id.past_round_number);
                String roundString = "ROUND " + weeklyRace.getRound();
                round.setText(roundString);

                TextView gpName = eventCard.findViewById(R.id.past_gp_name);
                gpName.setText(weeklyRace.getRaceName());

                generatePodium(eventCard, weeklyRace);

                Log.i("PastEvent", "gpName: " + weeklyRace.getRaceName());
                eventCard.setOnClickListener(v -> {
                    Intent intent = new Intent(PastEventsActivity.this, EventActivity.class);
                    intent.putExtra("CIRCUIT_ID", weeklyRace.getTrack().getTrackId());
                    startActivity(intent);
                });

            }
            loadingScreen.hideLoadingScreen();
        });

        return eventCard;
    }

    private void generatePodium(View eventCard, Race weeklyRace) {
        if (weeklyRace.getResults() == null) {
            setPendingPodium(eventCard);
        } else {
            for (int i = 0; i < 3; i++) {
                RaceResult raceResult = weeklyRace.getResults().get(i);
                TextView driverName = eventCard.findViewById(Constants.PAST_RACE_DRIVER_NAME.get(i));
                driverName.setText(raceResult.getDriver().getFullName());
            }
        }
    }

    private void setPendingPodium(View eventCard) {
        View pendingResults = eventCard.findViewById(R.id.pending_results_text);
        View podium = eventCard.findViewById(R.id.race_podium);
        View arrow = eventCard.findViewById(R.id.paste_event_card_arrow);

        pendingResults.setVisibility(View.VISIBLE);
        podium.setVisibility(View.GONE);
        arrow.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}