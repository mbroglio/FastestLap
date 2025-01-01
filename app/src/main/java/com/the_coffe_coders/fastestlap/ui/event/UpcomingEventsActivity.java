package com.the_coffe_coders.fastestlap.ui.event;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.api.RaceAPIResponse;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.EventViewModel;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.EventViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.standing.viewmodel.DriverStandingsViewModel;
import com.the_coffe_coders.fastestlap.ui.standing.viewmodel.DriverStandingsViewModelFactory;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;

import org.threeten.bp.LocalDate;
import org.threeten.bp.ZonedDateTime;

import java.util.ArrayList;
import java.util.List;

public class UpcomingEventsActivity extends AppCompatActivity {

    LoadingScreen loadingScreen;
    private boolean raceToProcess = true;

    private static final String TAG = "UpcomingEventsActivity";

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
        EventViewModel eventViewModel = new ViewModelProvider(this, new EventViewModelFactory(ServiceLocator.getInstance().getRaceRepository(getApplication(), false))).get(EventViewModel.class);
        MutableLiveData<Result> data = eventViewModel.getEventsLiveData(0);
        data.observe(this, result -> {
            if (result.isSuccess()) {
                List<WeeklyRace> races = ((Result.WeeklyRaceSuccess) result).getData();
                Log.i(TAG, "EVENTS SUCCESS" + races.toString());
                for (WeeklyRace race : races) {
                    Log.i(TAG, race.getRaceName());
                    createEventCards(races);
                }
                loadingScreen.hideLoadingScreen();
            }
        }
        );
    }

    private void createEventCards(List<WeeklyRace> upcomingRaces) {
        LinearLayout upcomingEvents = findViewById(R.id.upcoming_events_list);

        for (WeeklyRace weeklyRace : upcomingRaces) {
            upcomingEvents.addView(generateEventCard(weeklyRace));

            View space = new View(UpcomingEventsActivity.this);
            space.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20));
            upcomingEvents.addView(space);
        }

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
        trackOutline.setImageResource(Constants.EVENT_CIRCUIT.get(weeklyRace.getCircuit().getCircuitId()));

        TextView roundNumber = eventCard.findViewById(R.id.upcoming_round_number);
        roundNumber.setText("ROUND " + weeklyRace.getRound());

        TextView gpName = eventCard.findViewById(R.id.upcoming_gp_name);
        gpName.setText(weeklyRace.getRaceName());

        TextView gpDate = eventCard.findViewById(R.id.upcoming_date);
        String fp1Day = String.valueOf(weeklyRace.getFirstPractice().getStartDateTime().getDayOfMonth());
        String raceDay = String.valueOf(weeklyRace.getDateTime().getDayOfMonth());
        gpDate.setText(fp1Day + " - " + raceDay);

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