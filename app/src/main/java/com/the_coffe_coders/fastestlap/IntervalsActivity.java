package com.the_coffe_coders.fastestlap;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
        LinearLayout liveIntervalsLayout = findViewById(R.id.liveIntervals);

        // Clear previous views to refresh data
        liveIntervalsLayout.removeAllViews();

        // Loop through the sorted drivers and add each with gap to leader and interval details to the layout
        for (Driver driver : sortedDrivers) {
            TextView driverView = new TextView(this);

            // Display driver number, gap to leader, and interval
            String driverText = String.format("Driver #%d\nGap to Leader: %.2f s\nInterval: %.2f s",
                    driver.getDriverNo(), driver.getGapToLeader(), driver.getInterval());

            driverView.setText(driverText);
            driverView.setTextSize(16);
            driverView.setPadding(16, 8, 16, 8);
            driverView.setTextColor(ContextCompat.getColor(this, android.R.color.black));

            liveIntervalsLayout.addView(driverView);
        }
    }
}
