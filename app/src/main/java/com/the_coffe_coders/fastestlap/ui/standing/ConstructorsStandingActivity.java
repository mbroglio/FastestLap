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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
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
import com.the_coffe_coders.fastestlap.ui.home.HomePageActivity;
import com.the_coffe_coders.fastestlap.ui.standing.viewmodel.ConstructorStandingsViewModel;
import com.the_coffe_coders.fastestlap.ui.standing.viewmodel.ConstructorStandingsViewModelFactory;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.UIUtils;

import java.util.List;
import java.util.Objects;

public class ConstructorsStandingActivity extends AppCompatActivity {

    private static final String TAG = "TeamCardActivity";
    private final boolean constructorToProcess = true;
    LoadingScreen loadingScreen;
    private TextView teamPointsTextView;
    private ConstructorStandings constructorStandings;
    private String constructorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_constructors_standing);

        constructorId = getIntent().getStringExtra("TEAM_ID");
        Log.i(TAG, "Constructor ID: " + constructorId);

        start();


    }

    private void start() {
        loadingScreen = new LoadingScreen(getWindow().getDecorView(), this);

        // Show loading screen initially
        loadingScreen.showLoadingScreen();

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);

        UIUtils.applyWindowInsets(toolbar);

        toolbar.setNavigationOnClickListener(v -> {
            Intent intent = new Intent(ConstructorsStandingActivity.this, HomePageActivity.class);
            intent.putExtra("CALLER", "ConstructorsStandingActivity");
            startActivity(intent);
        });

        SwipeRefreshLayout teamStandingLayout = findViewById(R.id.team_standing_layout);
        UIUtils.applyWindowInsets(teamStandingLayout);

        teamStandingLayout.setOnRefreshListener(() -> {
            start();
            teamStandingLayout.setRefreshing(false);
        });

        setupPage();
    }

    private void setupPage() {
        LinearLayout teamStanding = findViewById(R.id.team_standing);

        ConstructorStandingsViewModel constructorStandingsViewModel = new ViewModelProvider(this, new ConstructorStandingsViewModelFactory()).get(ConstructorStandingsViewModel.class);
        MutableLiveData<Result> liveData = (MutableLiveData<Result>) constructorStandingsViewModel.getConstructorStandings();
        Log.i(TAG, "Constructor Standings: " + liveData);
        liveData.observe(this, result -> {
            if(result instanceof Result.Loading) {
                // Gestisci lo stato di caricamento, ad esempio mostrando un indicatore di caricamento
                Log.i(TAG, "Constructor Standings LOADING");
                // Qui potresti voler mostrare una UI di caricamento
                return;
            }
            if (result.isSuccess()) {
                teamStanding.removeAllViews();
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
                        space.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Constants.SPACER_HEIGHT));
                        teamStanding.addView(space);
                    }
                    View space = new View(ConstructorsStandingActivity.this);
                    space.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Constants.SPACER_HEIGHT));
                    teamStanding.addView(space);
                }
            } else if (result instanceof Result.Error) {
                Result.Error error = (Result.Error) result;
                Log.e(TAG, "Error: " + error.getMessage());
                loadingScreen.hideLoadingScreen();
            }
        });
    }

    private View generateTeamCard(ConstructorStandingsElement standingElement, String constructorIdToHighlight) {
        String teamId = standingElement.getConstructor().getConstructorId();
        ConstructorViewModel constructorViewModel = new ViewModelProvider(this, new ConstructorViewModelFactory()).get(ConstructorViewModel.class);
        MutableLiveData<Result> liveData = constructorViewModel.getSelectedConstructor(teamId);

        View teamCard = getLayoutInflater().inflate(R.layout.team_card, null);
        liveData.observe(this, result -> {
            if(result instanceof Result.Loading) {
                return;
            }
            if (result.isSuccess()) {
                Constructor constructor = ((Result.ConstructorSuccess) result).getData();
                standingElement.setConstructor(constructor);

                LinearLayout teamColor = teamCard.findViewById(R.id.team_card);
                teamColor.setBackground(AppCompatResources.getDrawable(this, Objects.requireNonNull(Constants.TEAM_GRADIENT_COLOR.get(teamId))));

                TextView teamNameTextView = teamCard.findViewById(R.id.team_name);
                teamNameTextView.setText(constructor.getName());

                UIUtils.loadSequenceOfImagesWithGlide(this,
                        new String[]{constructor.getCar_pic_url(), constructor.getTeam_logo_url()},
                        new ImageView[]{teamCard.findViewById(R.id.car_image), teamCard.findViewById(R.id.team_logo)},
                        () -> generateTeamCardStepTwo(constructor, standingElement, teamCard, constructorIdToHighlight, teamId));
            }

        });

        return teamCard;
    }

    private void generateTeamCardStepTwo(Constructor constructor, ConstructorStandingsElement standingElement, View teamCard, String constructorIdToHighlight, String teamId) {
        DriverViewModel driverViewModel = new ViewModelProvider(this, new DriverViewModelFactory(getApplication())).get(DriverViewModel.class);
        MutableLiveData<Result> driverOneLiveData = driverViewModel.getDriver(constructor.getDriverOneId());
        MutableLiveData<Result> driverTwoLiveData = driverViewModel.getDriver(constructor.getDriverTwoId());

        driverOneLiveData.observe(this, driverResult -> {
            if (driverResult instanceof Result.Loading) {
                return;
            }
            if (driverResult.isSuccess()) {
                Driver driverOne = ((Result.DriverSuccess) driverResult).getData();

                TextView driverOneNameTextView = teamCard.findViewById(R.id.driver_1_name);
                driverOneNameTextView.setText(driverOne.getFullName());

                ImageView driverOneImageView = teamCard.findViewById(R.id.driver_1_pic);

                UIUtils.loadImageWithGlide(this, driverOne.getDriver_pic_url(), driverOneImageView, () ->
                        generateTeamCardStepThree(driverTwoLiveData, teamCard, constructorIdToHighlight, teamId, standingElement));
            }
        });
    }

    private void generateTeamCardStepThree(MutableLiveData<Result> driverTwoLiveData, View teamCard, String constructorIdToHighlight, String teamId, ConstructorStandingsElement standingElement) {
        driverTwoLiveData.observe(this, driverResult2 -> {
            if (driverResult2 instanceof Result.Loading) {
                return;
            }
            if (driverResult2.isSuccess()) {
                Driver driverTwo = ((Result.DriverSuccess) driverResult2).getData();

                TextView driverTwoNameTextView = teamCard.findViewById(R.id.driver_2_name);
                driverTwoNameTextView.setText(driverTwo.getFullName());

                ImageView driverTwoImageView = teamCard.findViewById(R.id.driver_2_pic);

                UIUtils.loadImageWithGlide(this, driverTwo.getDriver_pic_url(), driverTwoImageView, () ->
                        generateTeamCardFinalStep(teamCard, constructorIdToHighlight, teamId, standingElement));
                    // Load the image into the ImageView
            }
        });
    }

    private void generateTeamCardFinalStep(View teamCard, String constructorIdToHighlight, String teamId, ConstructorStandingsElement standingElement) {

        // Set the team position
        TextView teamPositionTextView = teamCard.findViewById(R.id.team_position);
        if (standingElement.getPosition() == null) {
            teamPositionTextView.setText(R.string.last_constructor_position);
        }
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

        loadingScreen.hideLoadingScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}