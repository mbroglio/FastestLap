package com.the_coffe_coders.fastestlap.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.race_result.Race;
import com.the_coffe_coders.fastestlap.domain.race_result.Result;
import com.the_coffe_coders.fastestlap.domain.race_result.ResultsAPIResponse;
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
    private boolean raceToProcess = true;
    private ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
    private int raceIndex = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_past_events);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.past_events_view), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        processEvents(raceIndex);
    }

    private void processEvents(int i) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.jolpi.ca/ergast/f1/current/" + i + "/")
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
                        } else {
                            Race race = races.get(0);
                            if (isPast(race)) {
                                createEventCard(race);
                                raceIndex++;
                                processEvents(raceIndex);
                            } else {
                                raceToProcess = false;
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    raceToProcess = false;
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                raceToProcess = false;
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

        return eventCard;
    }
}