package com.the_coffe_coders.fastestlap.ui.login.fragment;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.the_coffe_coders.fastestlap.R;

public class LoginFragment extends Fragment {

    private static final String TAG = "LoginFragment";
    private View introScreen;
    private View loadingScreen;
    private TextView appName;
    private TextView appCredits;
    private ProgressBar progressIndicator;
    private Handler handler = new Handler();
    private MediaPlayer mediaPlayer;
    private MediaPlayer logoMediaPlayer;

    public LoginFragment() {
        // Required empty public constructor
    }

    public static LoginFragment newInstance() {
        return new LoginFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize MediaPlayer with the sound files
        mediaPlayer = MediaPlayer.create(getContext(), R.raw.type_writer_short);
        logoMediaPlayer = MediaPlayer.create(getContext(), R.raw.f1_car_sound);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        introScreen = view.findViewById(R.id.intro_screen);
        appName = view.findViewById(R.id.app_name);
        appCredits = view.findViewById(R.id.app_credits);
        ImageView appLogo = view.findViewById(R.id.app_logo);
        progressIndicator = view.findViewById(R.id.progress_indicator);

        // Set app name, app credits, and progress indicator initially invisible
        appName.setVisibility(View.INVISIBLE);
        appCredits.setVisibility(View.INVISIBLE);
        progressIndicator.setVisibility(View.INVISIBLE);

        // Show intro screen initially
        showIntroScreen();
        Log.i(TAG, "Intro screen shown");

        // Load animations
        Animation logoAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.slide_in);
        Animation nameAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.slide_up);

        // Start animations in sequence with delays
        appLogo.startAnimation(logoAnimation);
        logoMediaPlayer.start(); // Start the logo sound
        handler.postDelayed(() -> {
            appName.startAnimation(nameAnimation);
            appName.setVisibility(View.VISIBLE);
            handler.postDelayed(() -> {
                // App credits animation (writing machine effect)
                String creditsText = getString(R.string.by_the_coffee_coders);
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

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Release the MediaPlayer resources
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (logoMediaPlayer != null) {
            logoMediaPlayer.release();
            logoMediaPlayer = null;
        }
    }

    private void showIntroScreen() {
        introScreen.setVisibility(View.VISIBLE);
    }

    private void hideIntroScreen() {
        introScreen.setVisibility(View.GONE);
    }
}