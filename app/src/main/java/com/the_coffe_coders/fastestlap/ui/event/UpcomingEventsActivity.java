package com.the_coffe_coders.fastestlap.ui.event;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
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

import java.util.List;

public class UpcomingEventsActivity extends AppCompatActivity {

    private static final String TAG = "UpcomingEventsActivity";
    private final boolean raceToProcess = true;
    LoadingScreen loadingScreen;

    EventViewModel eventViewModel;
    TrackViewModel trackViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_upcoming_events);

        start();

    }

    private void start(){
        loadingScreen = new LoadingScreen(getWindow().getDecorView(), this);

        loadingScreen.showLoadingScreen();

        eventViewModel = new ViewModelProvider(this, new EventViewModelFactory(getApplication())).get(EventViewModel.class);
        trackViewModel = new ViewModelProvider(this, new TrackViewModelFactory(ServiceLocator.getInstance().getTrackRepository())).get(TrackViewModel.class);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        UIUtils.applyWindowInsets(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        SwipeRefreshLayout upcomingEventsLayout = findViewById(R.id.upcoming_events_layout);
        UIUtils.applyWindowInsets(upcomingEventsLayout);
        upcomingEventsLayout.setOnRefreshListener(() ->{
            start();
            upcomingEventsLayout.setRefreshing(false);
        });

        processEvents();
    }

    private void processEvents() {
        Log.i("UpcomingEvents", "Process Event");
        MutableLiveData<Result> data = eventViewModel.getWeeklyRacesLiveData();
        data.observe(this, result -> {
            if(result instanceof Result.Loading) {
                return;
            }
            if (result.isSuccess()) {
                List<WeeklyRace> races = ((Result.WeeklyRaceSuccess) result).getData();
                Log.i("UpcomingEvents", "SUCCESS");
                LinearLayout upcomingEvents = findViewById(R.id.upcoming_events_list);
                upcomingEvents.removeAllViews();
                List<WeeklyRace> upcomingRaces = eventViewModel.extractUpcomingRaces(races);
                Log.i("UpcomingEvents", "upcomingRaces: " + upcomingRaces.size());
                for (WeeklyRace race : upcomingRaces) {
                    createEventCard(upcomingEvents, race);
                }

                View space = new View(UpcomingEventsActivity.this);
                space.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Constants.SPACER_HEIGHT));
                upcomingEvents.addView(space);
            }

        });

    }

    private void createEventCard(LinearLayout eventsListLayout, WeeklyRace weeklyRace) {
        eventsListLayout.addView(generateEventCard(weeklyRace));

        View space = new View(UpcomingEventsActivity.this);
        space.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Constants.SPACER_HEIGHT));
        eventsListLayout.addView(space);
    }

    private View generateEventCard(WeeklyRace weeklyRace) {
        View eventCard;

        if (weeklyRace.isUnderway(true)) {
            eventCard = getLayoutInflater().inflate(R.layout.upcoming_event_live_card, null);

            ImageView liveIcon = eventCard.findViewById(R.id.upcoming_event_icon);
            Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse_dynamic);
            liveIcon.startAnimation(pulse);
        } else {
            eventCard = getLayoutInflater().inflate(R.layout.upcoming_event_card, null);
        }

        MutableLiveData<Result> trackData = trackViewModel.getTrack(weeklyRace.getTrack().getTrackId());

        View finalEventCard = eventCard;
        trackData.observe(this, result -> {
            if (result instanceof Result.Loading) {
                return;
            }
            if (result.isSuccess()) {
                Track track = ((Result.TrackSuccess) result).getData();

                ImageView trackOutline = finalEventCard.findViewById(R.id.upcoming_track_outline);

                UIUtils.loadImageWithGlide(this, track.getTrack_minimal_layout_url(), trackOutline,
                        () -> generateEventCardFinalStep(finalEventCard, weeklyRace));
            }

        });

        return eventCard;
    }

    private void generateEventCardFinalStep(View finalEventCard, WeeklyRace weeklyRace) {

        UIUtils.multipleSetTextViewText(
                new String[]{this.getString(R.string.round_upper_case_plus_value, weeklyRace.getRound()),
                        weeklyRace.getRaceName(),
                        weeklyRace.getFirstPractice().getStartDateTime().getDayOfMonth() + " - " + weeklyRace.getDateTime().getDayOfMonth(),
                        weeklyRace.getDateTime().getMonth().toString().substring(0, 3)},

                new TextView[]{finalEventCard.findViewById(R.id.upcoming_round_number),
                        finalEventCard.findViewById(R.id.upcoming_gp_name),
                        finalEventCard.findViewById(R.id.upcoming_date),
                        finalEventCard.findViewById(R.id.upcoming_month)}
        );

        finalEventCard.setOnClickListener(v -> {
            Intent intent = new Intent(UpcomingEventsActivity.this, EventActivity.class);
            intent.putExtra("CIRCUIT_ID", weeklyRace.getTrack().getTrackId());
            startActivity(intent);
        });

        loadingScreen.hideLoadingScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}