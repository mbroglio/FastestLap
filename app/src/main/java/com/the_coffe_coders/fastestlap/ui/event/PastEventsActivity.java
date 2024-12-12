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

import com.google.android.material.appbar.MaterialToolbar;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.race_result.Race;
import com.the_coffe_coders.fastestlap.domain.race_result.Result;
import com.the_coffe_coders.fastestlap.domain.api.ResultsAPIResponse;
import com.the_coffe_coders.fastestlap.ui.ErgastAPI;
import com.the_coffe_coders.fastestlap.utils.Constants;
import com.the_coffe_coders.fastestlap.utils.JSONParserUtils;

import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

import java.io.IOException;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

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

        processEvents(raceIndex);
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

    private void processEvents(int i) {
        //"https://api.jolpi.ca/ergast/f1/current/" + i + "/"; // This will be the default URL
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://ergast.com/api/f1/current/" + i + "/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ErgastAPI ergastApi = retrofit.create(ErgastAPI.class);

        ergastApi.getResults().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.i("PastEvents", "Response: " + response.toString());
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseBody = response.body().string();
                        JsonObject jsonResponse = new Gson().fromJson(responseBody, JsonObject.class);
                        JsonObject mrData = jsonResponse.getAsJsonObject("MRData");

                        JSONParserUtils jsonParserUtils = new JSONParserUtils(PastEventsActivity.this);
                        ResultsAPIResponse resultsAPIResponse = jsonParserUtils.parseRaceResults(mrData);

                        List<Race> races = resultsAPIResponse.getRaceTable().getRaces();
                        if (races.isEmpty()) {
                            Log.i("PastEvents", "No past events found.");
                            raceToProcess = false;
                            hideLoadingScreen();
                        } else {
                            Race race = races.get(0);
                            if (isPast(race)) {
                                createEventCard(race);
                                raceIndex++;
                                processEvents(raceIndex);
                            } else {
                                raceToProcess = false;
                                hideLoadingScreen();
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    raceToProcess = false;
                    hideLoadingScreen();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                raceToProcess = false;
                hideLoadingScreen();
            }
        });
    }

    private boolean isPast(Race race) {
        String raceDate = race.getDate();
        String raceTime = race.getTime(); // ends with 'Z'
        ZonedDateTime raceDateTime = ZonedDateTime.parse(raceDate + "T" + raceTime);
        raceDateTime = raceDateTime.plusMinutes(Constants.SESSION_DURATION.get("Race"));

        return now.isAfter(raceDateTime);
    }

    private void createEventCard(Race race) {
        LinearLayout pastEvents = findViewById(R.id.past_events_list);
        pastEvents.addView(generateEventCard(race));

        View space = new View(PastEventsActivity.this);
        space.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20));
        pastEvents.addView(space);
    }

    private View generateEventCard(Race race) {
        View eventCard = getLayoutInflater().inflate(R.layout.past_event_card, null);

        TextView day = eventCard.findViewById(R.id.past_date);
        String dayString = String.valueOf(LocalDate.parse(race.getDate()).getDayOfMonth());
        day.setText(dayString);

        TextView month = eventCard.findViewById(R.id.past_month);
        String monthString = LocalDate.parse(race.getDate()).getMonth().toString().substring(0, 3).toUpperCase();
        month.setText(monthString);

        ImageView trackOutline = eventCard.findViewById(R.id.past_track_outline);
        trackOutline.setImageResource(Constants.EVENT_CIRCUIT.get(race.getCircuit().getCircuitId()));

        TextView round = eventCard.findViewById(R.id.past_round_number);
        round.setText("ROUND " + race.getRound());

        TextView gpName = eventCard.findViewById(R.id.past_gp_name);
        gpName.setText(race.getRaceName());

        List<Result> results = race.getResults();
        for (int i = 0; i < 3; i++) {
            Result result = results.get(i);

            TextView driverName = eventCard.findViewById(Constants.PAST_RACE_DRIVER_NAME.get(i));
            driverName.setText(Constants.DRIVER_FULLNAME.get(result.getDriver().getDriverId()));
        }

        Log.i("PastEvent", "gpName: " + race.getRaceName());
        eventCard.setOnClickListener(v -> {
            Intent intent = new Intent(PastEventsActivity.this, EventActivity.class);
            intent.putExtra("CIRCUIT_ID", race.getCircuit().getCircuitId());
            startActivity(intent);
        });
        return eventCard;
    }
}