package com.the_coffe_coders.fastestlap.ui.event;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.race.Race;
import com.the_coffe_coders.fastestlap.api.RaceAPIResponse;
import com.the_coffe_coders.fastestlap.api.ErgastAPI;
import com.the_coffe_coders.fastestlap.utils.Constants;
import com.the_coffe_coders.fastestlap.utils.JSONParserUtils;

import org.threeten.bp.LocalDate;
import org.threeten.bp.ZonedDateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class UpcomingEventsActivity extends AppCompatActivity {

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
        setContentView(R.layout.activity_upcoming_events);

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
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.jolpi.ca/ergast/f1/current/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ErgastAPI ergastApi = retrofit.create(ErgastAPI.class);

        ergastApi.getRaces().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseBody = response.body().string();
                        JsonObject jsonResponse = new Gson().fromJson(responseBody, JsonObject.class);
                        JsonObject mrData = jsonResponse.getAsJsonObject("MRData");

                        JSONParserUtils jsonParserUtils = new JSONParserUtils(UpcomingEventsActivity.this);
                        RaceAPIResponse raceAPIResponse = jsonParserUtils.parseRace(mrData);

                        List<WeeklyRace> upcomingRaces = extractUpcomingRaces(raceAPIResponse);
                        createEventCards(upcomingRaces);
                        raceToProcess = false;
                        hideLoadingScreen();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
            }
        });

    }

    private List<WeeklyRace> extractUpcomingRaces(RaceAPIResponse raceAPIResponse) {
        ZonedDateTime now = ZonedDateTime.now();
        /*//List<WeeklyRace> races = raceAPIResponse.getRaceTable().getRaces();
        List<WeeklyRace> upcomingRaces = new ArrayList<>();

        for (WeeklyRace weeklyRace : races) {
            String raceDate = weeklyRace.getDate();
            String raceTime = weeklyRace.getTime(); // ends with 'Z'
            ZonedDateTime raceDateTime = ZonedDateTime.parse(raceDate + "T" + raceTime);
            raceDateTime = raceDateTime.plusMinutes(Constants.SESSION_DURATION.get("Race"));

            // A Race is considered upcoming if it is yet to finish
            if (now.isBefore(raceDateTime)) {
                upcomingRaces.add(weeklyRace);
            }
        }

        return upcomingRaces;*/
        return null;
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

        if (race.isRaceWeekendUnderway()) {
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
        String fp1Day = String.valueOf(LocalDate.parse(weeklyRace.getFirstPractice().getStartDateTime().toString()).getDayOfMonth());
        String raceDay = String.valueOf(LocalDate.parse(weeklyRace.getDate()).getDayOfMonth());
        gpDate.setText(fp1Day + " - " + raceDay);

        TextView gpMonth = eventCard.findViewById(R.id.upcoming_month);
        String fp1Month = String.valueOf(LocalDate.parse(weeklyRace.getDate()).getMonth()).substring(0, 3).toUpperCase();
        gpMonth.setText(fp1Month);

        eventCard.setOnClickListener(v -> {
            Intent intent = new Intent(UpcomingEventsActivity.this, EventActivity.class);
            intent.putExtra("CIRCUIT_ID", weeklyRace.getCircuit().getCircuitId());
            startActivity(intent);
        });

        return eventCard;
    }
}