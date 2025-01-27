package com.the_coffe_coders.fastestlap.ui.standing;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandings;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandingsElement;
import com.the_coffe_coders.fastestlap.repository.result.RaceResultRepository;
import com.the_coffe_coders.fastestlap.ui.bio.DriverBioActivity;
import com.the_coffe_coders.fastestlap.ui.standing.viewmodel.DriverStandingsViewModel;
import com.the_coffe_coders.fastestlap.ui.standing.viewmodel.DriverStandingsViewModelFactory;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;

import java.util.List;

public class DriversStandingActivity extends AppCompatActivity {

    private static final String TAG = "DriverCardActivity";

    private DriverStandings driverStandings;

    private LoadingScreen loadingScreen;

    private DriverStandingsViewModel driverStandingsViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_drivers_standing);

        loadingScreen = new LoadingScreen(getWindow().getDecorView(), this);

        loadingScreen.showLoadingScreen();
        String driverId = getIntent().getStringExtra("DRIVER_ID");

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);

        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.topMargin = systemBars.top;
            v.setLayoutParams(params);

            return insets;
        });

        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());


        LinearLayout driverStanding = findViewById(R.id.driver_standing);

        driverStandingsViewModel = new ViewModelProvider(this, new DriverStandingsViewModelFactory(ServiceLocator.getInstance().getDriverRepository(getApplication(), false))).get(DriverStandingsViewModel.class);
        MutableLiveData<Result> livedata = driverStandingsViewModel.getDriverStandingsLiveData(0);//TODO get last update from shared preferences

        livedata.observe(this, result -> {
            if (result.isSuccess()) {
                Log.i(TAG, "DRIVER STANDINGS SUCCESS");
                driverStandings = ((Result.DriverStandingsSuccess) result).getData();

                List<DriverStandingsElement> driverList = driverStandings.getDriverStandingsElements();
                int initialSize = driverList.size();
                loadingScreen.hideLoadingScreen();

                for (DriverStandingsElement driver : driverList) {
                    View driverCard = generateDriverCard(driver, driverId);
                    driverStanding.addView(driverCard);
                    View space = new View(DriversStandingActivity.this);
                    space.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20));
                    driverStanding.addView(space);
                }
            } else {
                Log.i(TAG, "DRIVER STANDINGS ERROR");
                loadingScreen.hideLoadingScreen();
            }
        });
    }

    private View generateDriverCard(DriverStandingsElement standingElement, String driverIdToHighlight) {
        // Inflate the team card layout
        View driverCard = getLayoutInflater().inflate(R.layout.driver_card, null);

        // Preparing all the views
        TextView driverPosition = driverCard.findViewById(R.id.driver_position);
        ImageView driverImageView = driverCard.findViewById(R.id.driver_image);
        TextView driverName = driverCard.findViewById(R.id.driver_name);
        TextView driverPoints = driverCard.findViewById(R.id.driver_points);

        ImageView teamLogoImageView = driverCard.findViewById(R.id.team_logo);
        RelativeLayout driverColor = driverCard.findViewById(R.id.small_driver_card);

        // Setting the values
        String driverId = standingElement.getDriver().getDriverId();
        driverImageView.setImageResource(Constants.DRIVER_IMAGE.get(driverId));
        driverName.setText(getText(Constants.DRIVER_FULLNAME.get(driverId)));

        String team = Constants.DRIVER_TEAM.get(driverId);
        teamLogoImageView.setImageResource(Constants.TEAM_LOGO_DRIVER_CARD.get(team));
        driverColor.setBackground(AppCompatResources.getDrawable(this, Constants.TEAM_GRADIENT_COLOR.get(team)));

        String position = standingElement.getPosition();
        driverPosition.setText(position);

        String points = standingElement.getPoints();
        driverPoints.setText(points);

        if (driverId.equals(driverIdToHighlight)) {
            int startColor = ContextCompat.getColor(this, R.color.yellow); // Replace with actual highlight color
            int endColor = Color.TRANSPARENT;

            ValueAnimator colorAnimator = ObjectAnimator.ofInt(driverCard, "backgroundColor", startColor, endColor);
            colorAnimator.setDuration(1000); // Duration in milliseconds
            colorAnimator.setEvaluator(new ArgbEvaluator());
            colorAnimator.setRepeatCount(5); // Repeat 5 times (includes forward and reverse)
            colorAnimator.setRepeatMode(ValueAnimator.REVERSE);
            colorAnimator.start();
        }

        driverCard.setOnClickListener(v -> {
            Intent intent = new Intent(DriversStandingActivity.this, DriverBioActivity.class);
            intent.putExtra("DRIVER_ID", driverId);
            intent.putExtra("CALLER", DriversStandingActivity.class.getName());
            startActivity(intent);
        });

        return driverCard;
    }
}