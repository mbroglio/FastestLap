package com.the_coffe_coders.fastestlap.ui.standing;

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
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandings;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandingsElement;
import com.the_coffe_coders.fastestlap.ui.bio.ConstructorBioActivity;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.ConstructorViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.ConstructorViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.DriverViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.DriverViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.standing.viewmodel.ConstructorStandingsViewModel;
import com.the_coffe_coders.fastestlap.ui.standing.viewmodel.ConstructorStandingsViewModelFactory;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;
import com.the_coffe_coders.fastestlap.util.UIUtils;

import java.util.List;

public class ConstructorsStandingActivity extends AppCompatActivity {

    private static final String TAG = "TeamCardActivity";
    private final boolean constructorToProcess = true;
    LoadingScreen loadingScreen;
    private TextView teamPointsTextView;
    private ConstructorStandings constructorStandings;

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

        UIUtils.applyWindowInsets(toolbar);

        LinearLayout teamStandingLayout = findViewById(R.id.team_standing_layout);
        UIUtils.applyWindowInsets(teamStandingLayout);

        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        LinearLayout teamStanding = findViewById(R.id.team_standing);

        ConstructorStandingsViewModel constructorStandingsViewModel = new ViewModelProvider(this, new ConstructorStandingsViewModelFactory(ServiceLocator.getInstance().getConstructorStandingsRepository(getApplication(), false))).get(ConstructorStandingsViewModel.class);
        MutableLiveData<Result> liveData = (MutableLiveData<Result>) constructorStandingsViewModel.getConstructorStandings();
        constructorStandingsViewModel.fetchConstructorStandings(0);
        Log.i(TAG, "Constructor Standings: " + liveData);
        liveData.observe(this, result -> {
            if (result.isSuccess()) {
                constructorStandings = ((Result.ConstructorStandingsSuccess) result).getData();
                List<ConstructorStandingsElement> constructorList = constructorStandings.getConstructorStandings();

                if (constructorList.isEmpty()) {
                    Log.i(TAG, "Constructor Standings is empty");
                } else {
                    Log.i(TAG, "Constructor Standings is not empty");
                    for (ConstructorStandingsElement constructor : constructorList) {
                        View teamCard = generateTeamCard(constructor, constructorId);
                        teamStanding.addView(teamCard);
                        View space = new View(ConstructorsStandingActivity.this);
                        space.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20));
                        teamStanding.addView(space);
                    }
                    loadingScreen.hideLoadingScreen();
                }
            } else if (result instanceof Result.Error) {
                Result.Error error = (Result.Error) result;
                Log.e(TAG, "Error: " + error.getMessage());
                loadingScreen.hideLoadingScreen();
            }
        });
    }

    private View generateTeamCard(Constructor constructor, String constructorId) {
        ConstructorStandingsElement constructorStandingsElement = new ConstructorStandingsElement();
        constructorStandingsElement.setConstructor(constructor);
        constructorStandingsElement.setPoints("0");

        return generateTeamCard(constructorStandingsElement, constructorId);
    }

    private View generateTeamCard(ConstructorStandingsElement standingElement, String constructorIdToHighlight) {
        String teamId = standingElement.getConstructor().getConstructorId();
        ConstructorViewModel constructorViewModel = new ViewModelProvider(this, new ConstructorViewModelFactory(ServiceLocator.getInstance().getCommonConstructorRepository())).get(ConstructorViewModel.class);
        MutableLiveData<Result> liveData = constructorViewModel.getSelectedConstructorLiveData(teamId);

        View teamCard = getLayoutInflater().inflate(R.layout.team_card, null);
        liveData.observe(this, result -> {
            if (result.isSuccess()) {
                Constructor constructor = ((Result.ConstructorSuccess) result).getData();
                standingElement.setConstructor(constructor);

                LinearLayout teamColor = teamCard.findViewById(R.id.team_card);
                teamColor.setBackground(AppCompatResources.getDrawable(this, Constants.TEAM_GRADIENT_COLOR.get(teamId)));

                TextView teamNameTextView = teamCard.findViewById(R.id.team_name);
                teamNameTextView.setText(constructor.getName());

                ImageView teamLogoImageView = teamCard.findViewById(R.id.team_logo);
                Glide.with(this).load(constructor.getTeam_logo_url()).into(teamLogoImageView);

                ImageView teamCarImageView = teamCard.findViewById(R.id.car_image);
                Glide.with(this).load(constructor.getCar_pic_url()).into(teamCarImageView);

                DriverViewModel driverViewModel = new ViewModelProvider(this, new DriverViewModelFactory(ServiceLocator.getInstance().getCommonDriverRepository())).get(DriverViewModel.class);
                MutableLiveData<Result> driverOneLiveData = driverViewModel.getDriver(constructor.getDriverOneId());
                MutableLiveData<Result> driverTwoLiveData = driverViewModel.getDriver(constructor.getDriverTwoId());

                driverOneLiveData.observe(this, driverResult -> {
                    if (driverResult.isSuccess()) {
                        Driver driverOne = ((Result.DriverSuccess) driverResult).getData();

                        TextView driverOneNameTextView = teamCard.findViewById(R.id.driver_1_name);
                        driverOneNameTextView.setText(driverOne.getFullName());

                        ImageView driverOneImageView = teamCard.findViewById(R.id.driver_1_pic);
                        Glide.with(this).load(driverOne.getDriver_pic_url()).into(driverOneImageView);

                        driverTwoLiveData.observe(this, driverResult2 -> {
                            if (driverResult2.isSuccess()) {
                                Driver driverTwo = ((Result.DriverSuccess) driverResult2).getData();

                                TextView driverTwoNameTextView = teamCard.findViewById(R.id.driver_2_name);
                                driverTwoNameTextView.setText(driverTwo.getFullName());

                                ImageView driverTwoImageView = teamCard.findViewById(R.id.driver_2_pic);
                                Glide.with(this).load(driverTwo.getDriver_pic_url()).into(driverTwoImageView);

                                // Set the team position
                                TextView teamPositionTextView = teamCard.findViewById(R.id.team_position);
                                teamPositionTextView.setText(standingElement.getPosition());

                                // Set the team points
                                TextView teamPointsTextView = teamCard.findViewById(R.id.team_points);
                                teamPointsTextView.setText(standingElement.getPoints());

                                if (teamId.equals(constructorIdToHighlight)) {
                                    MaterialCardView teamCardView = teamCard.findViewById(R.id.team_card_view);
                                    UIUtils.animateCardBackgroundColor(this, teamCardView, R.color.yellow, Color.TRANSPARENT, 1000, 10);
                                }

                                teamCard.setOnClickListener(v -> {
                                    Intent intent = new Intent(ConstructorsStandingActivity.this, ConstructorBioActivity.class);
                                    intent.putExtra("TEAM_ID", teamId);
                                    startActivity(intent);
                                });
                            }
                        });
                    }
                });
            }
        });

        return teamCard;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}