package com.the_coffe_coders.fastestlap;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import okhttp3.ResponseBody;

public class IntervalsActivity extends AppCompatActivity {
    private static final String TAG = "IntervalsActivity";
    private OpenF1 openF1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_intervals);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.openf1.org/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        openF1 = retrofit.create(OpenF1.class);

        //updateIntervals();
        try {
            testIntervals();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateIntervals() {
        ZonedDateTime now_utc = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime prev_utc = now_utc.minusMinutes(1);


        boolean raceEnded = false;
        while (!raceEnded) {
            Log.i(TAG, "Now --> " + now_utc);
            Log.i(TAG, "Prev --> " + prev_utc);

            requestIntervals("latest", now_utc, prev_utc);

            // Update the timestamps
            prev_utc = now_utc;
            now_utc = ZonedDateTime.now(ZoneOffset.UTC);
        }
    }

    private void testIntervals() throws InterruptedException {
        ZonedDateTime now_utc = ZonedDateTime.parse("2023-09-17T13:31:02+00:00");
        ZonedDateTime prev_utc = now_utc.minusMinutes(1);


        boolean raceEnded = false;
        while (!raceEnded) {
            Log.i(TAG, "Now --> " + now_utc);
            Log.i(TAG, "Prev --> " + prev_utc);

            requestIntervals("9165", now_utc, prev_utc);

            // Sleep for 5 seconds
            Thread.sleep(5000);
            // Update the timestamps
            prev_utc = now_utc;
            now_utc = prev_utc.plusSeconds(5);
        }
    }

    private void requestIntervals(String sessionKey, ZonedDateTime now_utc, ZonedDateTime prev_utc) {
        Log.i(TAG, "Requesting intervals for session: " + sessionKey);
        // Make the request
        Call<String> call = openF1.getIntervals(sessionKey, now_utc.toString(), prev_utc.toString());
        Log.i(TAG, "Request URL: " + call.request().url());

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                Log.i(TAG, "Request successful");
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonResponse = new JSONObject(response.body());
                        // Process the JSON response
                        Log.i(TAG, "Response: " + jsonResponse.toString());

                        JSONArray intervals = jsonResponse.getJSONArray("intervals");
                        for (int i = 0; i < intervals.length(); i++) {
                            JSONObject interval = intervals.getJSONObject(i);
                            Log.i(TAG, "Interval: " + interval.toString());
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.e(TAG, "Request failed with code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.e(TAG, "Request failed: " + t.getMessage());
            }
        });
    }
}
