package com.the_coffe_coders.fastestlap.ui.login;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.the_coffe_coders.fastestlap.R;

public class IntroScreen {

    private static final String TAG = "IntroScreen";
    private final View introScreen;
    private final TextView appName;
    private final TextView appCredits;
    private final ProgressBar progressIndicator;
    private final ImageView appLogo;
    private final Handler handler = new Handler();
    private final MediaPlayer mediaPlayer;
    private final MediaPlayer logoMediaPlayer;

    public IntroScreen(View view, Context context) {
        introScreen = view.findViewById(R.id.intro_screen);
        appName = view.findViewById(R.id.app_name);
        appCredits = view.findViewById(R.id.app_credits);
        appLogo = view.findViewById(R.id.app_logo);
        progressIndicator = view.findViewById(R.id.progress_indicator);

        // Initialize MediaPlayer with the sound files
        mediaPlayer = MediaPlayer.create(context, R.raw.type_writer_short);
        logoMediaPlayer = MediaPlayer.create(context, R.raw.f1_car_sound);

        // Set app name, app credits, and progress indicator initially invisible
        appName.setVisibility(View.INVISIBLE);
        appCredits.setVisibility(View.INVISIBLE);
        progressIndicator.setVisibility(View.INVISIBLE);
    }

    public void showIntroScreen() {
        introScreen.setVisibility(View.VISIBLE);
        Log.i(TAG, "Intro screen shown");

        // Load animations
        Animation logoAnimation = AnimationUtils.loadAnimation(introScreen.getContext(), R.anim.slide_in);
        Animation nameAnimation = AnimationUtils.loadAnimation(introScreen.getContext(), R.anim.slide_up);

        // Start animations in sequence with delays
        appLogo.startAnimation(logoAnimation);
        logoMediaPlayer.start(); // Start the logo sound
        handler.postDelayed(() -> {
            appName.startAnimation(nameAnimation);
            appName.setVisibility(View.VISIBLE);
            handler.postDelayed(() -> {
                // App credits animation (writing machine effect)
                String creditsText = introScreen.getContext().getString(R.string.by_the_coffee_coders);
                int delay = 100; // milliseconds delay for each character
                for (int i = 0; i < creditsText.length(); i++) {
                    final int index = i;
                    handler.postDelayed(() -> {
                        mediaPlayer.start();
                        Log.i(TAG, "Playing sound");
                        appCredits.setVisibility(View.VISIBLE);
                        appCredits.setText(creditsText.substring(0, index + 1));
                    }, (long) delay * i);
                }

                handler.postDelayed(() -> {
                    progressIndicator.setVisibility(View.VISIBLE);
                    handler.postDelayed(() -> {
                        hideIntroScreen();
                        progressIndicator.setVisibility(View.INVISIBLE);
                    }, 5000); // 5 seconds delay
                }, (long) creditsText.length() * delay);
            }, 1000); // 1 second delay
        }, 2000); // 2 seconds delay
    }

    public void hideIntroScreen() {
        introScreen.setVisibility(View.GONE);
    }

    public void releaseResources() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        if (logoMediaPlayer != null) {
            logoMediaPlayer.release();
        }
    }
}