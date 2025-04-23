package com.the_coffe_coders.fastestlap.ui.welcome;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModelProvider;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.ui.home.HomePageActivity;
import com.the_coffe_coders.fastestlap.ui.welcome.viewmodel.UserViewModel;
import com.the_coffe_coders.fastestlap.ui.welcome.viewmodel.UserViewModelFactory;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;
import com.the_coffe_coders.fastestlap.util.UIUtils;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "IntroScreenActivity";
    private final Handler handler = new Handler();
    private TextView appName;
    private TextView appCredits;
    private ProgressBar progressIndicator;
    private ImageView appLogo;
    private MediaPlayer mediaPlayer;
    private MediaPlayer logoMediaPlayer;
    private UserViewModel userViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash_screen);

        start();
    }

    private void start(){
        ConstraintLayout introScreen = findViewById(R.id.intro_screen);
        UIUtils.applyWindowInsets(introScreen);

        String season_year = getIntent().getStringExtra("season_year");
        Log.d("LaunchFlag", "Valore ricevuto: " + season_year);
        if (season_year != null) {
            ServiceLocator.setCurrentYearBaseUrl(season_year);
        }else {
            Log.d("LaunchFlag", "Using default year (current)");
            ServiceLocator.setCurrentYearBaseUrl("2025");
        }


        userViewModel = new ViewModelProvider(getViewModelStore(), new UserViewModelFactory(ServiceLocator.getInstance().getUserRepository((Application) getApplicationContext()))).get(UserViewModel.class);

        appName = findViewById(R.id.app_name);
        appCredits = findViewById(R.id.app_credits);
        appLogo = findViewById(R.id.app_logo);
        progressIndicator = findViewById(R.id.progress_indicator);
        mediaPlayer = MediaPlayer.create(this, R.raw.type_writer_short);
        logoMediaPlayer = MediaPlayer.create(this, R.raw.f1_car_sound);
        appName.setVisibility(View.INVISIBLE);
        appCredits.setVisibility(View.INVISIBLE);
        progressIndicator.setVisibility(View.INVISIBLE);

        UIUtils.setAppLocale();
        setupIntro();
    }

    private void showIntroScreen() {
        Animation logoAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_in);
        Animation nameAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up);

        appLogo.startAnimation(logoAnimation);
        appLogo.setVisibility(View.VISIBLE);
        logoMediaPlayer.start();  // Start the logo sound
        handler.postDelayed(() -> {
            appName.startAnimation(nameAnimation);
            appName.setVisibility(View.VISIBLE);
            handler.postDelayed(() -> {

                String creditsText = getString(R.string.app_credits);
                int delay = 100;
                for (int i = 0; i < creditsText.length(); i++) {
                    final int index = i;
                    handler.postDelayed(() -> {
                        mediaPlayer.start();
                        appCredits.setVisibility(View.VISIBLE);
                        appCredits.setText(creditsText.substring(0, index + 1));
                    }, (long) delay * i);
                }

                handler.postDelayed(() -> {
                    progressIndicator.setVisibility(View.VISIBLE);

                    handler.postDelayed(() -> {
                        startActivity(new Intent(SplashActivity.this, WelcomeActivity.class));
                        finish();
                    }, 5000); // 5 seconds delay
                }, (long) creditsText.length() * delay);
            }, 1000);
        }, 2000);
    }

    public void showForAutoLogin() {
        findViewById(R.id.intro_screen).setVisibility(View.VISIBLE);
        appName.setVisibility(View.VISIBLE);
        appCredits.setVisibility(View.VISIBLE);
    }

    public void hideIntroScreen() {
        appLogo.setVisibility(View.GONE);
        appName.setVisibility(View.GONE);
        appCredits.setVisibility(View.GONE);
        progressIndicator.setVisibility(View.GONE);
        //findViewById(R.id.intro_screen).setVisibility(View.GONE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        if (logoMediaPlayer != null) {
            logoMediaPlayer.release();
        }
    }


    protected void setupIntro() {

        Log.d(TAG, "Setting up intro screen");
        Log.d(TAG, "Logged user: " + userViewModel.getLoggedUser());
        if (userViewModel.getLoggedUser() != null) {
            showForAutoLogin();

            userViewModel.isAutoLoginEnabled(userViewModel.getLoggedUser().getIdToken()).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    boolean isEnabled = task.getResult();
                    Log.d(TAG, "Auto login is enabled: " + isEnabled);
                    if (isEnabled) {
                        startActivity(new Intent(SplashActivity.this, HomePageActivity.class));
                    } else {
                        hideIntroScreen();
                        new Handler().postDelayed(this::showIntroScreen, 500);
                    }
                }
            });
        } else {
            showIntroScreen();
        }
    }


}