package com.the_coffe_coders.fastestlap;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;
import org.threeten.bp.ZonedDateTime;

import java.util.HashMap;
import java.util.TreeSet;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class IntervalsActivity extends AppCompatActivity {
    private static final String TAG = "IntervalsActivity";
    private OpenF1 openF1;
    private Handler handler = new Handler(Looper.getMainLooper());
    private TreeSet<Driver> sortedDrivers = new TreeSet<>();
    private HashMap<Integer, Driver> driverMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_intervals);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.liveIntervals), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.openf1.org/v1/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();

        openF1 = retrofit.create(OpenF1.class);

        // Start the interval test
        testIntervals();
    }

    private void testIntervals() {
        ZonedDateTime nowUtc = ZonedDateTime.parse("2023-09-17T13:31:02+00:00");
        ZonedDateTime prevUtc = nowUtc.minusMinutes(1);

        // Define a runnable that will repeatedly call the API
        Runnable intervalRunnable = new Runnable() {
            ZonedDateTime currentNowUtc = nowUtc;
            ZonedDateTime currentPrevUtc = prevUtc;

            @Override
            public void run() {
                Log.i(TAG, "Now --> " + currentNowUtc);
                Log.i(TAG, "Prev --> " + currentPrevUtc);

                requestIntervals("9165", currentNowUtc, currentPrevUtc);

                int secondsDelay = 5;
                // Update the timestamps
                currentPrevUtc = currentNowUtc;
                currentNowUtc = currentPrevUtc.plusSeconds(secondsDelay);

                // Post the runnable again with a delay of 5 seconds
                handler.postDelayed(this, secondsDelay * 1000);
            }
        };

        // Start the initial runnable
        handler.post(intervalRunnable);
    }

    private void requestIntervals(String sessionKey, ZonedDateTime nowUtc, ZonedDateTime prevUtc) {
        Log.i(TAG, "Requesting intervals for session: " + sessionKey);
        Call<ResponseBody> call = openF1.getIntervals(sessionKey, nowUtc.toString(), prevUtc.toString());
        Log.i(TAG, "Request URL: " + call.request().url());

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.i(TAG, "onResponse: " + response.code() + " " + response.message());
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseString = response.body().string();
                        Log.i(TAG, "Raw Response: " + responseString);
                        JSONArray intervals = new JSONArray(responseString);

                        for (int i = 0; i < intervals.length(); i++) {
                            JSONObject interval = intervals.getJSONObject(i);
                            //Log.i(TAG, "Interval: " + interval.toString());
                            processInterval(interval);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse JSON response", e);
                    }
                } else {
                    Log.e(TAG, "Response not successful: " + response.code() + " " + response.message());
                    if (response.errorBody() != null) {
                        try {
                            Log.e(TAG, "Error Body: " + response.errorBody().string());
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to read error body", e);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Request failed: " + t.getMessage(), t);
            }
        });
    }

    private void processInterval(JSONObject interval) {
        try {
            // Extract driver data from JSON
            int driverNo = interval.getInt("driver_number");
            double gapToLeader;
            double intervalValue;

            if (interval.isNull("gap_to_leader") && interval.isNull("interval")) {
                gapToLeader = 0;
                intervalValue = 0;
            } else if (interval.isNull("gap_to_leader")) {
                Log.e(TAG, "Missing gap_to_leader in interval: " + interval.toString());
                return;
            } else if (interval.isNull("interval")) {
                Log.e(TAG, "Missing interval in interval: " + interval.toString());
                return;
            } else {
                gapToLeader = interval.getDouble("gap_to_leader");
                intervalValue = interval.getDouble("interval");
            }

            Driver driver = driverMap.get(driverNo);
            if (driver != null) {
                // Update existing driver
                sortedDrivers.remove(driver);  // Remove old data
                driver.setGapToLeader(gapToLeader);
                driver.setInterval(intervalValue);
                sortedDrivers.add(driver);     // Reinsert updated driver
            } else {
                // Add new driver
                driver = new Driver(driverNo, gapToLeader, intervalValue);
                driverMap.put(driverNo, driver);
                sortedDrivers.add(driver);
            }

            updateUI();
        } catch (Exception e) {
            Log.e(TAG, "Failed to process interval", e);
            Log.e(TAG, "Interval JSON: " + interval.toString());
        }
    }

    private void updateUI() {
        LinearLayout liveIntervals = findViewById(R.id.liveIntervals);

        // Clear previous views to refresh data
        liveIntervals.removeAllViews();

        // Loop through the sorted drivers and add each with gap to leader and interval details to the layout
        int position = 1;
        for (Driver driver : sortedDrivers) {
            liveIntervals.addView(generateIntervalEntry(driver, position));
            position++;

            View space = new View(this);
            space.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 16));
            liveIntervals.addView(space);
        }
    }

    private View generateIntervalEntry(Driver driver, int position) {
        View intervalEntry = getLayoutInflater().inflate(R.layout.driver_card_interval, null);

        TextView driverPosition = intervalEntry.findViewById(R.id.driver_position);
        LinearLayout teamColor = intervalEntry.findViewById(R.id.team_color);
        TextView driverName = intervalEntry.findViewById(R.id.driver_name);
        ImageView driverNumber = intervalEntry.findViewById(R.id.driver_number);
        TextView driverInterval = intervalEntry.findViewById(R.id.driver_interval);
        TextView gapToLeader = intervalEntry.findViewById(R.id.gap_to_leader);


        switch (driver.getDriverNo()){
            case 1:
                // Max Verstappen
                teamColor.setBackgroundColor(ContextCompat.getColor(this, R.color.redbull_f1));
                driverName.setText("Max Verstappen");
                driverNumber.setImageResource(R.drawable.max_verstappen_number);
                break;

            case 4:
                // Lando Norris
                teamColor.setBackgroundColor(ContextCompat.getColor(this, R.color.mclaren_f1));
                driverName.setText("Lando Norris");
                driverNumber.setImageResource(R.drawable.max_verstappen_number);
                break;

            case 16:
                // Charles Leclerc
                teamColor.setBackgroundColor(ContextCompat.getColor(this, R.color.ferrari_f1));
                driverName.setText("Charles Leclerc");
                driverNumber.setImageResource(R.drawable.max_verstappen_number);
                break;

            case 81:
                // Oscar Piastri
                teamColor.setBackgroundColor(ContextCompat.getColor(this, R.color.mclaren_f1));
                driverName.setText("Oscar Piastri");
                driverNumber.setImageResource(R.drawable.max_verstappen_number);
                break;

            case 55:
                // Carlos Sainz
                teamColor.setBackgroundColor(ContextCompat.getColor(this, R.color.ferrari_f1));
                driverName.setText("Carlos Sainz");
                driverNumber.setImageResource(R.drawable.max_verstappen_number);
                break;

            case 63:
                // George Russell
                teamColor.setBackgroundColor(ContextCompat.getColor(this, R.color.mercedes_f1));
                driverName.setText("George Russell");
                driverNumber.setImageResource(R.drawable.max_verstappen_number);
                break;

            case 44:
                // Lewis Hamilton
                teamColor.setBackgroundColor(ContextCompat.getColor(this, R.color.mercedes_f1));
                driverName.setText("Lewis Hamilton");
                driverNumber.setImageResource(R.drawable.max_verstappen_number);
                break;

            case 11:
                // Sergio Perez
                teamColor.setBackgroundColor(ContextCompat.getColor(this, R.color.redbull_f1));
                driverName.setText("Sergio Perez");
                driverNumber.setImageResource(R.drawable.max_verstappen_number);
                break;

            case 14:
                // Fernando Alonso
                teamColor.setBackgroundColor(ContextCompat.getColor(this, R.color.aston_martin_f1));
                driverName.setText("Fernando Alonso");
                driverNumber.setImageResource(R.drawable.max_verstappen_number);
                break;

            case 27:
                // Nico Hulkenberg
                teamColor.setBackgroundColor(ContextCompat.getColor(this, R.color.haas_f1));
                driverName.setText("Nico Hulkenberg");
                driverNumber.setImageResource(R.drawable.max_verstappen_number);
                break;

            case 22:
                // Yuki Tsunoda
                teamColor.setBackgroundColor(ContextCompat.getColor(this, R.color.racing_bulls_f1));
                driverName.setText("Yuki Tsunoda");
                driverNumber.setImageResource(R.drawable.max_verstappen_number);
                break;

            case 10:
                // Pierre Gasly
                teamColor.setBackgroundColor(ContextCompat.getColor(this, R.color.alpine_f1));
                driverName.setText("Pierre Gasly");
                driverNumber.setImageResource(R.drawable.max_verstappen_number);
                break;

            case 18:
                // Lance Stroll
                teamColor.setBackgroundColor(ContextCompat.getColor(this, R.color.aston_martin_f1));
                driverName.setText("Lance Stroll");
                driverNumber.setImageResource(R.drawable.max_verstappen_number);
                break;

            case 31:
                // Esteban Ocon
                teamColor.setBackgroundColor(ContextCompat.getColor(this, R.color.alpine_f1));
                driverName.setText("Esteban Ocon");
                driverNumber.setImageResource(R.drawable.max_verstappen_number);
                break;

            case 20:
                // Kevin Magnussen
                teamColor.setBackgroundColor(ContextCompat.getColor(this, R.color.haas_f1));
                driverName.setText("Kevin Magnussen");
                driverNumber.setImageResource(R.drawable.max_verstappen_number);
                break;

            case 23:
                // Alex Albon
                teamColor.setBackgroundColor(ContextCompat.getColor(this, R.color.williams_f1));
                driverName.setText("Alex Albon");
                driverNumber.setImageResource(R.drawable.max_verstappen_number);
                break;

            case 3:
                // Daniel Ricciardo
                teamColor.setBackgroundColor(ContextCompat.getColor(this, R.color.racing_bulls_f1));
                driverName.setText("Daniel Ricciardo");
                driverNumber.setImageResource(R.drawable.max_verstappen_number);
                break;

            case 50:
                // Oliver Bearmann
                teamColor.setBackgroundColor(ContextCompat.getColor(this, R.color.haas_f1));
                driverName.setText("Oliver Bearmann");
                driverNumber.setImageResource(R.drawable.max_verstappen_number);
                break;

            case 43:
                // Franco Colapinto
                teamColor.setBackgroundColor(ContextCompat.getColor(this, R.color.williams_f1));
                driverName.setText("Franco Colapinto");
                driverNumber.setImageResource(R.drawable.max_verstappen_number);
                break;

            case 30:
                // Liam Lawson
                teamColor.setBackgroundColor(ContextCompat.getColor(this, R.color.racing_bulls_f1));
                driverName.setText("Liam Lawson");
                driverNumber.setImageResource(R.drawable.max_verstappen_number);
                break;

            case 24:
                // Zhou Guanyu
                teamColor.setBackgroundColor(ContextCompat.getColor(this, R.color.kick_f1));
                driverName.setText("Zhou Guanyu");
                driverNumber.setImageResource(R.drawable.max_verstappen_number);
                break;

            case 2:
                // Logan Sargeant
                teamColor.setBackgroundColor(ContextCompat.getColor(this, R.color.williams_f1));
                driverName.setText("Logan Sargeant");
                driverNumber.setImageResource(R.drawable.max_verstappen_number);
                break;

            case 77:
                // Valtteri Bottas
                teamColor.setBackgroundColor(ContextCompat.getColor(this, R.color.kick_f1));
                driverName.setText("Valtteri Bottas");
                driverNumber.setImageResource(R.drawable.max_verstappen_number);
                break;
            default:
                break;
        }

        driverPosition.setText(String.valueOf(position));
        driverInterval.setText(String.format("%.3f", driver.getInterval()));
        gapToLeader.setText(String.format("%.3f", driver.getGapToLeader()));

        return intervalEntry;
    }
}
