package com.the_coffe_coders.fastestlap;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;
import org.threeten.bp.ZonedDateTime;

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

        Retrofit retrofit = new Retrofit.Builder().baseUrl("https://api.openf1.org/v1/").addConverterFactory(ScalarsConverterFactory.create()).build();

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

                // Update the timestamps
                currentPrevUtc = currentNowUtc;
                currentNowUtc = currentPrevUtc.plusSeconds(5);

                // Post the runnable again with a delay of 5 seconds
                handler.postDelayed(this, 5000);
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
