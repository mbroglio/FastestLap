package com.the_coffe_coders.fastestlap.ui.event;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.View;
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
import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;
import com.the_coffe_coders.fastestlap.domain.grand_prix.RaceResult;
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
import java.util.Objects;

public class PastEventsActivity extends AppCompatActivity {

    LoadingScreen loadingScreen;
    EventViewModel eventViewModel;
    private GestureDetector tapDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        //UIUtils.hideSystemUI(this);
        setContentView(R.layout.activity_past_events);

        //tapDetector = UIUtils.createTapDetector(this);

        eventViewModel = new ViewModelProvider(this, new EventViewModelFactory(ServiceLocator.getInstance().getRaceRepository(getApplication(), false), ServiceLocator.getInstance().getRaceResultRepository(getApplication(), false))).get(EventViewModel.class);

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
        EventViewModel eventViewModel = new ViewModelProvider(this, new EventViewModelFactory(ServiceLocator.getInstance().getRaceRepository(getApplication(), false), ServiceLocator.getInstance().getRaceResultRepository(getApplication(), false))).get(EventViewModel.class);
        MutableLiveData<Result> data = eventViewModel.getAllResults();
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
                loadingScreen.hideLoadingScreen();
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

        LocalDateTime raceDateTime = weeklyRace.getStartDateTime();

        TextView day = eventCard.findViewById(R.id.past_date);
        String dayString = raceDateTime.getDayOfMonth() + "";
        day.setText(dayString);

        TextView month = eventCard.findViewById(R.id.past_month);
        String monthString = raceDateTime.getMonth().toString().substring(0, 3); //Ex: JAN, FEB, etc
        month.setText(monthString);

        ImageView trackOutline = eventCard.findViewById(R.id.past_track_outline);
        Integer track = Constants.EVENT_CIRCUIT.get(weeklyRace.getCircuit().getCircuitId());
        trackOutline.setImageResource(Objects.requireNonNullElseGet(track, () -> R.string.unknown));

        TextView round = eventCard.findViewById(R.id.past_round_number);
        String roundString = "ROUND " + weeklyRace.getRound();
        round.setText(roundString);

        TextView gpName = eventCard.findViewById(R.id.past_gp_name);
        gpName.setText(weeklyRace.getRaceName());

        generatePodium(eventCard, weeklyRace);

        Log.i("PastEvent", "gpName: " + weeklyRace.getRaceName());
        eventCard.setOnClickListener(v -> {
            Intent intent = new Intent(PastEventsActivity.this, EventActivity.class);
            intent.putExtra("CIRCUIT_ID", weeklyRace.getCircuit().getCircuitId());
            startActivity(intent);
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
                Integer driverFullName = Constants.DRIVER_FULLNAME.get(raceResult.getDriver().getDriverId());
                driverName.setText(Objects.requireNonNullElseGet(driverFullName, () -> R.string.unknown));
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

/*
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        tapDetector.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

 */

    @Override
    protected void onResume() {
        //UIUtils.hideSystemUI(this);
        super.onResume();
    }
}