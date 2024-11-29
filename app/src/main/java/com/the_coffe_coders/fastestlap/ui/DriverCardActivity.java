package com.the_coffe_coders.fastestlap.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Driver;
import com.the_coffe_coders.fastestlap.utils.Constants;

import okhttp3.ResponseBody;

import org.json.JSONArray;
import org.json.JSONObject;
import org.threeten.bp.Year;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class DriverCardActivity extends AppCompatActivity {

    private static final String TAG = "DriverCardActivity";

    static Year year = Year.now();
    private static final String BASE_URL = "https://api.jolpi.ca/ergast/f1/" + year + "/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_driver_card);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.driver_card_view), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        LinearLayout driverStanding = findViewById(R.id.driver_standing);
        // Initialize Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();

        ErgastAPI ergastApi = retrofit.create(ErgastAPI.class);

        // Make the network request
        ergastApi.getDriverStandings().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseString = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseString);

                        JSONArray standing = jsonResponse.getJSONObject("MRData").getJSONObject("StandingsTable").getJSONArray("StandingsLists").getJSONObject(0).getJSONArray("DriverStandings");
                        Driver driver;

                        for (int i = 0; i < standing.length(); i++) {
                            driver = new Driver(standing.getJSONObject(i));
                            driverStanding.addView(generateDriverCard(driver));

                            View space = new View(DriverCardActivity.this);
                            space.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20));
                            driverStanding.addView(space);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse JSON response", e);
                    }
                } else {
                    Log.e(TAG, "Response not successful");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.e(TAG, "Network request failed", t);
            }
        });
    }

    private View generateDriverCard(Driver driver) {
        // Inflate the team card layout
        View driverCard = getLayoutInflater().inflate(R.layout.small_driver_card, null);

        // Preparing all the views
        TextView driverPosition = driverCard.findViewById(R.id.driver_position);
        ImageView driverImageView = driverCard.findViewById(R.id.driver_image);
        TextView driverName = driverCard.findViewById(R.id.driver_name);
        TextView driverPoints = driverCard.findViewById(R.id.driver_points);

        ImageView teamLogoImageView = driverCard.findViewById(R.id.team_logo);
        LinearLayout driverColor = driverCard.findViewById(R.id.small_driver_card);

        // Setting the values
        String driverId = driver.getDriverId();
        driverImageView.setImageResource(Constants.DRIVER_IMAGE.get(driverId));
        driverName.setText(getText(Constants.DRIVER_FULLNAME.get(driverId)));

        String team = Constants.DRIVER_TEAM.get(driverId);
        teamLogoImageView.setImageResource(Constants.TEAM_LOGO.get(team));
        driverColor.setBackground(AppCompatResources.getDrawable(this, Constants.TEAM_GRADIENT_COLOR.get(team)));

        int position = driver.getStandingPosition();
        driverPosition.setText(String.valueOf(position));

        int points = driver.getPoints();
        driverPoints.setText(String.valueOf(points));

        driverCard.setOnClickListener(v -> Log.i(TAG, "Driver card clicked"));

        return driverCard;
    }
}