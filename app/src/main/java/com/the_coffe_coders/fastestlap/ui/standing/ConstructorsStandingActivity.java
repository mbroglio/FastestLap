package com.the_coffe_coders.fastestlap.ui.standing;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.constructor.ConstructorStanding;
import com.the_coffe_coders.fastestlap.domain.constructor.StandingsAPIResponse;
import com.the_coffe_coders.fastestlap.ui.ErgastAPI;
import com.the_coffe_coders.fastestlap.utils.Constants;
import com.the_coffe_coders.fastestlap.utils.JSONParserUtils;

import okhttp3.ResponseBody;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class ConstructorsStandingActivity extends AppCompatActivity {

    private static final String TAG = "TeamCardActivity";
    private static final String BASE_URL = "https://api.jolpi.ca/ergast/f1/2024/";

    private TextView teamPointsTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_constructors_standing);

        String constructorId = getIntent().getStringExtra("TEAM_ID");
        Log.i(TAG, "Constructor ID: " + constructorId);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        LinearLayout teamStanding = findViewById(R.id.team_standing);
        // Initialize Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();

        ErgastAPI ergastApi = retrofit.create(ErgastAPI.class);

        // Make the network request
        ergastApi.getConstructorStandings().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseString = response.body().string();
                        JsonObject jsonResponse = new Gson().fromJson(responseString, JsonObject.class);
                        JsonObject mrData = jsonResponse.get("MRData").getAsJsonObject();

                        JSONParserUtils jsonParserUtils = new JSONParserUtils(ConstructorsStandingActivity.this);
                        StandingsAPIResponse standingsAPIResponse = jsonParserUtils.parseConstructorStandings(mrData);

                        int total = Integer.parseInt(standingsAPIResponse.getTotal());
                        for (int i = 0; i < total; i++) {
                            ConstructorStanding standingElement = standingsAPIResponse
                                    .getStandingsTable()
                                    .getStandingsLists().get(0)
                                    .getConstructorStandings().get(i);
                            teamStanding.addView(generateTeamCard(standingElement, constructorId));
                            //add a space between each team card
                            View space = new View(ConstructorsStandingActivity.this);
                            space.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20));
                            teamStanding.addView(space);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse JSON response", e);
                    }
                } else {
                    Log.e(TAG, "Response not successful");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Network request failed", t);
            }
        });
    }

    private View generateTeamCard(ConstructorStanding standingElement, String constructorIdToHighlight) {
        // Inflate the team card layout
        View teamCard = getLayoutInflater().inflate(R.layout.team_card, null);

        // Preparing all the views
        TextView teamNameTextView = teamCard.findViewById(R.id.team_name);

        TextView driverOneNameTextView = teamCard.findViewById(R.id.driver_1_name);
        ImageView driverOneImageView = teamCard.findViewById(R.id.driver_1_pic);

        TextView driverTwoNameTextView = teamCard.findViewById(R.id.driver_2_name);
        ImageView driverTwoImageView = teamCard.findViewById(R.id.driver_2_pic);

        ImageView teamLogoImageView = teamCard.findViewById(R.id.team_logo);
        ImageView teamCarImageView = teamCard.findViewById(R.id.car_image);
        LinearLayout teamColor = teamCard.findViewById(R.id.team_card);

        // Populate the views with the data
        String teamId = standingElement.getConstructor().getConstructorId();
        teamNameTextView.setText(Constants.TEAM_FULLNAME.get(teamId));
        teamLogoImageView.setImageResource(Constants.TEAM_LOGO.get(teamId));
        teamCarImageView.setImageResource(Constants.TEAM_CAR.get(teamId));
        teamColor.setBackground(AppCompatResources.getDrawable(this, Constants.TEAM_GRADIENT_COLOR.get(teamId)));

        String driverId = Constants.TEAM_DRIVER1.get(teamId);
        driverOneNameTextView.setText(Constants.DRIVER_FULLNAME.get(driverId));
        driverOneImageView.setImageResource(Constants.DRIVER_IMAGE.get(driverId));

        driverId = Constants.TEAM_DRIVER2.get(teamId);
        driverTwoNameTextView.setText(Constants.DRIVER_FULLNAME.get(driverId));
        driverTwoImageView.setImageResource(Constants.DRIVER_IMAGE.get(driverId));

        // Set the team position
        TextView teamPositionTextView = teamCard.findViewById(R.id.team_position);
        teamPositionTextView.setText(standingElement.getPosition());

        // Set the team points
        TextView teamPointsTextView = teamCard.findViewById(R.id.team_points);
        teamPointsTextView.setText(standingElement.getPoints());

        if (teamId.equals(constructorIdToHighlight)) {
            int startColor = ContextCompat.getColor(this, R.color.yellow); // Replace with actual highlight color
            int endColor = Color.TRANSPARENT;

            ValueAnimator colorAnimator = ObjectAnimator.ofInt(teamCard, "backgroundColor", startColor, endColor);
            colorAnimator.setDuration(1000); // Duration in milliseconds
            colorAnimator.setEvaluator(new ArgbEvaluator());
            colorAnimator.setRepeatCount(ValueAnimator.INFINITE); // Repeat 5 times (includes forward and reverse)
            colorAnimator.setRepeatMode(ValueAnimator.REVERSE);
            colorAnimator.start();

        }

        return teamCard;
    }
}