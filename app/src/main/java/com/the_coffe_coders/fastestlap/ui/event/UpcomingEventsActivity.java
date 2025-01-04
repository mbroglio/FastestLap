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

import com.google.android.material.appbar.MaterialToolbar;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.EventViewModel;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.EventViewModelFactory;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;

import java.util.List;
import java.util.Objects;

/*
 * TODO:
 *  - Fix the if(true) in generateEventCard method, it should check if a race is live
 */

public class UpcomingEventsActivity extends AppCompatActivity {

    private static final String TAG = "UpcomingEventsActivity";
    private final boolean raceToProcess = true;
    LoadingScreen loadingScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_upcoming_events);
        loadingScreen = new LoadingScreen(getWindow().getDecorView(), this);
        loadingScreen.showLoadingScreen();

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        processEvents();
    }

    private void processEvents() {
        Log.i("UpcomingEvents", "Process Event");
        EventViewModel eventViewModel = new ViewModelProvider(this, new EventViewModelFactory(ServiceLocator.getInstance().getRaceRepository(getApplication(), false))).get(EventViewModel.class);
        MutableLiveData<Result> data = ServiceLocator.getInstance().getRaceRepository(getApplication(), false).fetchWeeklyRaces(0);
        data.observe(this, result -> {
            if (result.isSuccess()) {
                List<WeeklyRace> races = ((Result.WeeklyRaceSuccess) result).getData();
                Log.i("UpcomingEvents", "SUCCESS");

                List<WeeklyRace> upcomingRaces = eventViewModel.extractUpcomingRaces(races);
                for (WeeklyRace race : upcomingRaces) {
                    createEventCard(race);
                }
                loadingScreen.hideLoadingScreen();
            }
        });
    }

    private void createEventCard(WeeklyRace weeklyRace) {
        LinearLayout upcomingEvents = findViewById(R.id.upcoming_events_list);
        upcomingEvents.addView(generateEventCard(weeklyRace));

        View space = new View(UpcomingEventsActivity.this);
        space.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Constants.SPACER_HEIGHT));
        upcomingEvents.addView(space);
    }

    private View generateEventCard(WeeklyRace weeklyRace) {
        View eventCard = null;

        if (true) {
            eventCard = getLayoutInflater().inflate(R.layout.upcoming_event_live_card, null);

            ImageView liveIcon = eventCard.findViewById(R.id.upcoming_event_icon);
            Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse_dynamic);
            liveIcon.startAnimation(pulse);
        } else {
            eventCard = getLayoutInflater().inflate(R.layout.upcoming_event_card, null);
        }

        ImageView trackOutline = eventCard.findViewById(R.id.upcoming_track_outline);
        Integer track = Constants.EVENT_CIRCUIT.get(weeklyRace.getCircuit().getCircuitId());
        trackOutline.setImageResource(Objects.requireNonNullElseGet(track, () -> R.string.unknown));

        TextView roundNumber = eventCard.findViewById(R.id.upcoming_round_number);
        String round = "ROUND " + weeklyRace.getRound();
        roundNumber.setText(round);

        TextView gpName = eventCard.findViewById(R.id.upcoming_gp_name);
        gpName.setText(weeklyRace.getRaceName());

        TextView gpDate = eventCard.findViewById(R.id.upcoming_date);
        String fp1Day = String.valueOf(weeklyRace.getFirstPractice().getStartDateTime().getDayOfMonth());
        String raceDay = String.valueOf(weeklyRace.getDateTime().getDayOfMonth());
        String date = fp1Day + " - " + raceDay;
        gpDate.setText(date);

        TextView gpMonth = eventCard.findViewById(R.id.upcoming_month);
        String fp1Month = weeklyRace.getDateTime().getMonth().toString().substring(0, 3);
        gpMonth.setText(fp1Month);

        eventCard.setOnClickListener(v -> {
            Intent intent = new Intent(UpcomingEventsActivity.this, EventActivity.class);
            intent.putExtra("CIRCUIT_ID", weeklyRace.getCircuit().getCircuitId());
            startActivity(intent);
        });

        return eventCard;
    }
}