package com.the_coffe_coders.fastestlap.ui.welcome;

import android.annotation.SuppressLint;
import android.app.Application;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.lifecycle.ViewModelProvider;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.ui.welcome.viewmodel.UserViewModel;
import com.the_coffe_coders.fastestlap.ui.welcome.viewmodel.UserViewModelFactory;
import com.the_coffe_coders.fastestlap.util.service.NetworkUtils;
import com.the_coffe_coders.fastestlap.util.service.ServiceLocator;
import com.the_coffe_coders.fastestlap.util.ui.NavigationUtils;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import java.util.Calendar;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "IntroScreenActivity";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView appName;
    private TextView appCredits;
    private ProgressBar progressIndicator;
    private ImageView appLogo;
    private MediaPlayer logoMediaPlayer;
    private SoundPool soundPool;
    private int soundId;
    private boolean soundLoaded = false;
    private UserViewModel userViewModel;

    private NetworkUtils networkLiveData;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash_screen);

        start();
    }

    private void start() {
        networkLiveData = new NetworkUtils(getApplicationContext());

        String season_year = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
        Log.d("LaunchFlag", "Valore ricevuto: " + season_year);
        ServiceLocator.setCurrentYearBaseUrl(season_year);

        userViewModel = new ViewModelProvider(getViewModelStore(), new UserViewModelFactory(ServiceLocator.getInstance().getUserRepository((Application) getApplicationContext()))).get(UserViewModel.class);

        appName = findViewById(R.id.app_name);
        appCredits = findViewById(R.id.app_credits);
        appLogo = findViewById(R.id.app_logo);
        progressIndicator = findViewById(R.id.progress_indicator);

        logoMediaPlayer = MediaPlayer.create(this, R.raw.f1_car_sound);

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(audioAttributes)
                .build();
        soundPool.setOnLoadCompleteListener((sp, sampleId, status) -> {
            if (status == 0) {
                soundLoaded = true;
            }
        });
        soundId = soundPool.load(this, R.raw.type_writer_short, 1);

        appName.setVisibility(View.INVISIBLE);
        appCredits.setVisibility(View.INVISIBLE);
        progressIndicator.setVisibility(View.INVISIBLE);

        UIUtils.setAppLocale();
        setupIntro();
    }

    private void showIntroScreen() {
        Animation logoAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_in);
        Animation nameAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up);

        appLogo.setVisibility(View.VISIBLE);
        appLogo.startAnimation(logoAnimation);

        // 1. Audio rombo motore F1 durante l'animazione di ingresso del logo
        if (logoMediaPlayer != null) {
            try {
                logoMediaPlayer.start();
            } catch (Exception e) {
                Log.e(TAG, "Error starting logoMediaPlayer: " + e.getMessage());
            }
        }

        // 2. Attesa completamento animazione logo prima di far scorrere il nome app
        handler.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;

            appName.setVisibility(View.VISIBLE);
            appName.startAnimation(nameAnimation);

            // 3. Attesa completamento animazione nome app (1000ms) prima dei credits a macchina da scrivere
            handler.postDelayed(() -> {
                if (isFinishing() || isDestroyed()) return;

                String creditsText = getString(R.string.app_credits);
                appCredits.setVisibility(View.VISIBLE);
                appCredits.setText("");

                int delay = 90; // Rallentato a 90ms per un effetto macchina da scrivere ritmico e realistico
                for (int i = 0; i < creditsText.length(); i++) {
                    final int index = i;
                    handler.postDelayed(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        appCredits.setText(creditsText.substring(0, index + 1));

                        char c = creditsText.charAt(index);
                        if (c != ' ' && soundPool != null && soundLoaded) {
                            try {
                                soundPool.play(soundId, 0.8f, 0.8f, 1, 0, 1.0f);
                            } catch (Exception e) {
                                Log.e(TAG, "Error playing soundPool: " + e.getMessage());
                            }
                        }
                    }, (long) delay * i);
                }

                // 4. Al termine della digitazione, mostra il progress indicator prima della transizione
                long totalTypingTime = (long) creditsText.length() * delay;
                handler.postDelayed(() -> {
                    if (isFinishing() || isDestroyed()) return;

                    progressIndicator.setVisibility(View.VISIBLE);
                    handler.postDelayed(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        NavigationUtils.navigateToWelcomePage(this);
                        finish();
                    }, 800);
                }, totalTypingTime + 200);
            }, 1000);
        }, 1800);
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacksAndMessages(null);
        stopMediaPlayers();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        stopMediaPlayers();
    }

    private void stopMediaPlayers() {
        if (logoMediaPlayer != null) {
            try {
                if (logoMediaPlayer.isPlaying()) logoMediaPlayer.stop();
            } catch (Exception ignored) {}
            logoMediaPlayer.release();
            logoMediaPlayer = null;
        }
        if (soundPool != null) {
            try {
                soundPool.release();
            } catch (Exception ignored) {}
            soundPool = null;
        }
    }

    protected void setupIntro() {
        Log.d(TAG, "Setting up intro screen");
        Log.d(TAG, "Logged user: " + userViewModel.getLoggedUser());
        if (userViewModel.getLoggedUser() != null) {
            Log.d(TAG, "Logged user is not null");
            if (networkLiveData.isConnected()) {
                userViewModel.isAutoLoginEnabled(userViewModel.getLoggedUser().getIdToken()).addOnCompleteListener(task -> {
                    if (task.isSuccessful() && Boolean.TRUE.equals(task.getResult())) {
                        Log.d(TAG, "Auto login is enabled");
                        NavigationUtils.navigateToHomePage(this);
                        finish();
                    } else {
                        Log.d(TAG, "Auto login is not enabled");
                        showIntroScreen();
                    }
                });
            } else {
                Log.e(TAG, "No internet connection");
                NavigationUtils.navigateToHomePage(this);
                finish();
            }
        } else {
            showIntroScreen();
        }
    }
}