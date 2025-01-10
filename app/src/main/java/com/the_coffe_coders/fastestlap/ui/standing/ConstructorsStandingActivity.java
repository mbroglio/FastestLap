package com.the_coffe_coders.fastestlap.ui.standing;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
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
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandings;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandingsElement;
import com.the_coffe_coders.fastestlap.ui.bio.ConstructorBioActivity;
import com.the_coffe_coders.fastestlap.ui.standing.viewmodel.ConstructorStandingsViewModel;
import com.the_coffe_coders.fastestlap.ui.standing.viewmodel.ConstructorStandingsViewModelFactory;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;

public class ConstructorsStandingActivity extends AppCompatActivity {

    private static final String TAG = "TeamCardActivity";
    private static final String BASE_URL = "https://api.jolpi.ca/ergast/f1/2024/";
    LoadingScreen loadingScreen;
    private final boolean constructorToProcess = true;
    private TextView teamPointsTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_constructors_standing);
        loadingScreen = new LoadingScreen(getWindow().getDecorView(), this);

        // Show loading screen initially
        loadingScreen.showLoadingScreen();

        String constructorId = getIntent().getStringExtra("TEAM_ID");
        Log.i(TAG, "Constructor ID: " + constructorId);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        LinearLayout teamStanding = findViewById(R.id.team_standing);

        ConstructorStandingsViewModel constructorStandingsViewModel = new ViewModelProvider(this, new ConstructorStandingsViewModelFactory(ServiceLocator.getInstance().getConstructorRepository(getApplication(), false))).get(ConstructorStandingsViewModel.class);
        MutableLiveData<Result> liveData = constructorStandingsViewModel.getConstructorStandingsLiveData(0);

        Log.i(TAG, "Constructor Standings: " + liveData);
        liveData.observe(this, result -> {
            if (result instanceof Result.ConstructorStandingsSuccess) {
                Result.ConstructorStandingsSuccess constructorStandingsSuccess = (Result.ConstructorStandingsSuccess) result;
                ConstructorStandings constructorStandings = constructorStandingsSuccess.getData();
                Log.i(TAG, "Constructor Standings: " + constructorStandings);
                for (ConstructorStandingsElement standingElement : constructorStandings.getConstructorStandings()) {
                    View teamCard = generateTeamCard(standingElement, constructorId);
                    teamStanding.addView(teamCard);
                    View space = new View(ConstructorsStandingActivity.this);
                    space.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20));
                    teamStanding.addView(space);
                }
                loadingScreen.hideLoadingScreen();
            } else if (result instanceof Result.Error) {
                Result.Error error = (Result.Error) result;
                Log.e(TAG, "Error: " + error.getMessage());
            }
        });
    }

    private View generateTeamCard(ConstructorStandingsElement standingElement, String constructorIdToHighlight) {
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
            colorAnimator.setRepeatCount(5); // Repeat 5 times (includes forward and reverse)
            colorAnimator.setRepeatMode(ValueAnimator.REVERSE);
            colorAnimator.start();

        }

        teamCard.setOnClickListener(v -> {
            Intent intent = new Intent(ConstructorsStandingActivity.this, ConstructorBioActivity.class);
            intent.putExtra("TEAM_ID", teamId);
            startActivity(intent);
        });

        return teamCard;
    }
}