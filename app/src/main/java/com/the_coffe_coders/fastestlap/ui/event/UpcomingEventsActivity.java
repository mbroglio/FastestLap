package com.the_coffe_coders.fastestlap.ui.event;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.race.Race;
import com.the_coffe_coders.fastestlap.domain.race.RaceAPIResponse;
import com.the_coffe_coders.fastestlap.ui.ErgastAPI;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_upcoming_events);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.upcoming_events_view), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        processEvents();
    }

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

                        List<Race> upcomingRaces = extractUpcomingRaces(raceAPIResponse);
                        createEventCards(upcomingRaces);
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

    private List<Race> extractUpcomingRaces(RaceAPIResponse raceAPIResponse) {
        ZonedDateTime now = ZonedDateTime.now();
        List<Race> races = raceAPIResponse.getRaceTable().getRaces();
        List<Race> upcomingRaces = new ArrayList<>();

        for (Race race : races) {
            String raceDate = race.getDate();
            String raceTime = race.getTime(); // ends with 'Z'
            ZonedDateTime raceDateTime = ZonedDateTime.parse(raceDate + "T" + raceTime);
            raceDateTime = raceDateTime.plusMinutes(Constants.SESSION_DURATION.get("Race"));

            // A Race is considered upcoming if it is yet to finish
            if (now.isBefore(raceDateTime)) {
                upcomingRaces.add(race);
            }
        }

        return upcomingRaces;
    }

    private void createEventCards(List<Race> upcomingRaces) {
        LinearLayout upcomingEvents = findViewById(R.id.upcoming_events_list);

        for (Race race : upcomingRaces) {
            upcomingEvents.addView(generateEventCard(race));

            View space = new View(UpcomingEventsActivity.this);
            space.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20));
            upcomingEvents.addView(space);
        }

    }

    private View generateEventCard(Race race) {
        View eventCard = null;

        if (race.isUnderway()) {
            eventCard = getLayoutInflater().inflate(R.layout.upcoming_event_live_card, null);
        } else {
            eventCard = getLayoutInflater().inflate(R.layout.upcoming_event_card, null);
        }

        ImageView trackOutline = eventCard.findViewById(R.id.upcoming_track_outline);
        trackOutline.setImageResource(Constants.EVENT_CIRCUIT.get(race.getCircuit().getCircuitId()));

        TextView roundNumber = eventCard.findViewById(R.id.upcoming_round_number);
        roundNumber.setText("ROUND " + race.getRound());

        TextView gpName = eventCard.findViewById(R.id.upcoming_gp_name);
        gpName.setText(race.getRaceName());

        TextView gpDate = eventCard.findViewById(R.id.upcoming_date);
        String fp1Day = String.valueOf(LocalDate.parse(race.getFirstPractice().getDate()).getDayOfMonth());
        String raceDay = String.valueOf(LocalDate.parse(race.getDate()).getDayOfMonth());
        gpDate.setText(fp1Day + " - " + raceDay);

        TextView gpMonth = eventCard.findViewById(R.id.upcoming_month);
        String fp1Month = String.valueOf(LocalDate.parse(race.getDate()).getMonth()).substring(0, 3).toUpperCase();
        gpMonth.setText(fp1Month);

        eventCard.setOnClickListener(v -> {
            Intent intent = new Intent(UpcomingEventsActivity.this, EventActivity.class);
            intent.putExtra("CIRCUIT_ID", race.getCircuit().getCircuitId());
            startActivity(intent);
        });

        return eventCard;
    }
}