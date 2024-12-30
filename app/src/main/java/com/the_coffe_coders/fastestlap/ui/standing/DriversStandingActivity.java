package com.the_coffe_coders.fastestlap.ui.standing;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.driver.DriverAPIResponse;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandings;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandingsElement;
import com.the_coffe_coders.fastestlap.repository.driver.DriverRepository;
import com.the_coffe_coders.fastestlap.ui.bio.DriverBioActivity;
import com.the_coffe_coders.fastestlap.ui.standing.viewmodel.DriverStandingsViewModel;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;

import org.threeten.bp.Year;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class DriversStandingActivity extends AppCompatActivity {

    private static final String TAG = "DriverCardActivity";

    private View loadingScreen;
    private TextView loadingText;
    private Handler handler = new Handler();
    private int dotCount = 0;
    private boolean addingDots = true;
    private boolean driverToProcess = true;
    private DriverStandings driverStandings;

    private DriverStandingsViewModel driverStandingsViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_drivers_standing);


        //loading screen logic
        loadingScreen = findViewById(R.id.loading_screen);
        loadingText = findViewById(R.id.loading_text);
        ImageView loadingWheel = findViewById(R.id.loading_wheel);

        // Start the rotation animation
        Animation rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate);
        loadingWheel.startAnimation(rotateAnimation);

        // Show loading screen initially
        showLoadingScreen();

        // Start the dots animation
        handler.post(dotRunnable);


        String driverId = getIntent().getStringExtra("DRIVER_ID");

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());


        LinearLayout driverStanding = findViewById(R.id.driver_standing);

        MutableLiveData<Result> data = driverStandingsViewModel.getDriverStandingsLiveData(0);//TODO get last update from shared preferences


        data.observe(this, result -> {
                    if (result.isSuccess()) {
                        Log.i(TAG, "DRIVER STANDINGS SUCCESS");
                        driverStandings = ((Result.DriverStandingsSuccess) result).getData();

                        List<DriverStandingsElement> driverList = driverStandings.getDriverStandingsElements();
                        int initialSize = driverList.size();
                        hideLoadingScreen();

                        for (DriverStandingsElement driver : driverList) {
                            View driverCard = generateDriverCard(driver, driverId);
                            driverStanding.addView(driverCard);
                        }
                    } else {
                        Log.i(TAG, "DRIVER STANDINGS ERROR");
                        hideLoadingScreen();
                    }
                });
    }

    private void showLoadingScreen() {
        loadingScreen.setVisibility(View.VISIBLE);
    }

    private void hideLoadingScreen() {
        loadingScreen.setVisibility(View.GONE);
        handler.removeCallbacks(dotRunnable);
    }

    private Runnable dotRunnable = new Runnable() {
        @Override
        public void run() {
            if (addingDots) {
                dotCount++;
                if (dotCount == 4) {
                    addingDots = false;
                }
            } else {
                dotCount--;
                if (dotCount == 0) {
                    addingDots = true;
                }
            }
            StringBuilder dots = new StringBuilder();
            for (int i = 0; i < dotCount; i++) {
                dots.append(".");
            }
            loadingText.setText("LOADING" + dots);
            handler.postDelayed(this, 500);
        }
    };


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
            colorAnimator.setRepeatCount(ValueAnimator.INFINITE); // Repeat 5 times (includes forward and reverse)
            colorAnimator.setRepeatMode(ValueAnimator.REVERSE);
            colorAnimator.start();

        }

        driverCard.setOnClickListener(v -> {
            Intent intent = new Intent(DriversStandingActivity.this, DriverBioActivity.class);
            intent.putExtra("DRIVER_ID", driverId);
            startActivity(intent);
        });


        return driverCard;
    }
}