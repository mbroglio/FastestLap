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

import com.google.android.material.appbar.MaterialToolbar;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;
import com.the_coffe_coders.fastestlap.domain.grand_prix.RaceResult;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.repository.weeklyrace.RaceRepository;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;

import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

import java.util.ArrayList;
import java.util.List;

public class PastEventsActivity extends AppCompatActivity {

    private ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
    private int raceIndex = 1;

    private View loadingScreen;
    private TextView loadingText;
    private Handler handler = new Handler();
    private int dotCount = 0;
    private boolean addingDots = true;
    private boolean raceToProcess = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_past_events);

        //loading screen logic
        loadingScreen = findViewById(R.id.loading_screen);
        loadingText = findViewById(R.id.loading_text);
        ImageView loadingWheel = findViewById(R.id.loading_wheel);

        // Start the rotation animation
        Animation rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate);
        loadingWheel.startAnimation(rotateAnimation);

        // Show loading screen initially
        showLoadingScreen();

        // Start the dots animation
        handler.post(dotRunnable);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        Log.i("PastEvent", "onCreate");
        processEvents();
    }

    private void showLoadingScreen() {
        loadingScreen.setVisibility(View.VISIBLE);
    }

    private void hideLoadingScreen() {
        loadingScreen.setVisibility(View.GONE);
        handler.removeCallbacks(dotRunnable);
    }

    private Runnable dotRunnable = new Runnable() {
        @Override
        public void run() {
            if (addingDots) {
                dotCount++;
                if (dotCount == 4) {
                    addingDots = false;
                }
            } else {
                dotCount--;
                if (dotCount == 0) {
                    addingDots = true;
                }
            }
            StringBuilder dots = new StringBuilder();
            for (int i = 0; i < dotCount; i++) {
                dots.append(".");
            }
            loadingText.setText("LOADING" + dots);
            handler.postDelayed(this, 500);
        }
    };

    private void processEvents() {
        Log.i("PastEvent", "Process Event");
        List<WeeklyRace> races = new ArrayList<>();
        MutableLiveData<Result> data = ServiceLocator.getInstance().getRaceRepository(getApplication(), false).fetchWeeklyRaces(0);
        data.observe(this, result -> {
            if(result.isSuccess()) {
                Log.i("PastEvent", "SUCCESS");
                races.addAll(((Result.WeeklyRaceSuccess) result).getData());
                for (WeeklyRace race: races) {
                    createEventCard(race);
                }
                hideLoadingScreen();
            }
        });
    }

    private boolean isPast(WeeklyRace race) {
        /*String raceDate = race.getStartDateTime().toString();
        String raceTime = race.getTime(); // ends with 'Z'
        ZonedDateTime raceDateTime = ZonedDateTime.parse(raceDate + "T" + raceTime);
        raceDateTime = raceDateTime.plusMinutes(Constants.SESSION_DURATION.get("Race"));

        return now.isAfter(raceDateTime);*/
        return true;
    }


    private void createEventCard(WeeklyRace race) {
        LinearLayout pastEvents = findViewById(R.id.past_events_list);
        pastEvents.addView(generateEventCard(race));

        View space = new View(PastEventsActivity.this);
        space.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20));
        pastEvents.addView(space);
    }

    private View generateEventCard(WeeklyRace race) {
        View eventCard = getLayoutInflater().inflate(R.layout.past_event_card, null);

        TextView day = eventCard.findViewById(R.id.past_date);
        String dayString = "";//String.valueOf(LocalDate.parse(race.getDate()).getDayOfMonth());
        day.setText(dayString);

        TextView month = eventCard.findViewById(R.id.past_month);
        String monthString = "";//LocalDate.parse(race.getDate().toString()).getMonth().toString().substring(0, 3).toUpperCase();
        month.setText(monthString);

        ImageView trackOutline = eventCard.findViewById(R.id.past_track_outline);
        trackOutline.setImageResource(Constants.EVENT_CIRCUIT.get(race.getCircuit().getCircuitId()));

        TextView round = eventCard.findViewById(R.id.past_round_number);
        round.setText("ROUND " + race.getRound());

        TextView gpName = eventCard.findViewById(R.id.past_gp_name);
        gpName.setText(race.getRaceName());

        /*List<RaceResult> raceResults = race.getFinalRace().getResults();
        for (int i = 0; i < 3; i++) {
            RaceResult raceResult = raceResults.get(i);

            TextView driverName = eventCard.findViewById(Constants.PAST_RACE_DRIVER_NAME.get(i));
            driverName.setText(Constants.DRIVER_FULLNAME.get(raceResult.getDriver().getDriverId()));
        }*/

        Log.i("PastEvent", "gpName: " + race.getRaceName());
        eventCard.setOnClickListener(v -> {
            Intent intent = new Intent(PastEventsActivity.this, EventActivity.class);
            intent.putExtra("CIRCUIT_ID", race.getCircuit().getCircuitId());
            startActivity(intent);
        });
        return eventCard;
    }
}