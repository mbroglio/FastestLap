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

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

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
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();

        openF1 = retrofit.create(OpenF1.class);

        try {
            testIntervals();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateIntervals() throws IOException {
        ZonedDateTime nowUtc = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime prevUtc = nowUtc.minusMinutes(1);

        boolean raceEnded = false;
        while (!raceEnded) {
            Log.i(TAG, "Now --> " + nowUtc);
            Log.i(TAG, "Prev --> " + prevUtc);

            requestIntervals("latest", nowUtc, prevUtc);

            // Update the timestamps
            prevUtc = nowUtc;
            nowUtc = ZonedDateTime.now(ZoneOffset.UTC);
        }
    }

    private void testIntervals() throws InterruptedException, IOException {
        ZonedDateTime nowUtc = ZonedDateTime.parse("2023-09-17T13:31:02+00:00");
        ZonedDateTime prevUtc = nowUtc.minusMinutes(1);

        boolean raceEnded = false;
        while (!raceEnded) {
            Log.i(TAG, "Now --> " + nowUtc);
            Log.i(TAG, "Prev --> " + prevUtc);

            requestIntervals("9165", nowUtc, prevUtc);

            // Sleep for 5 seconds
            Thread.sleep(5000);
            // Update the timestamps
            prevUtc = nowUtc;
            nowUtc = prevUtc.plusSeconds(5);
        }
    }

    private void requestIntervals(String sessionKey, ZonedDateTime nowUtc, ZonedDateTime prevUtc) throws IOException {
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
                            Log.i(TAG, "Interval: " + interval.toString());
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
}
