package com.the_coffe_coders.fastestlap.util;

import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.the_coffe_coders.fastestlap.R;

public class LoadingScreen {

    private final Handler handler;
    private final View loadingScreen;
    private final Context context;
    private final TextView loadingText, percentageText, loadingStatusText;
    private final ProgressBar loadingProgressBar;
    private final View rootView, fragmentView;

    private int dotCount = 0;
    private boolean addingDots = true;
    private Runnable dotRunnable;

    private final Handler timerHandler = new Handler();
    private final Runnable timerRunnable = this::hide;

    public LoadingScreen(View view, Context context, View fragmentView) {
        this.handler = new Handler();
        //loading screen logic
        this.loadingScreen = view.findViewById(R.id.loading_screen);
        this.context = context;
        this.loadingText = view.findViewById(R.id.loading_text);
        this.percentageText = view.findViewById(R.id.loading_percentage_text);
        this.loadingProgressBar = view.findViewById(R.id.loading_progress_bar);
        this.loadingStatusText = view.findViewById(R.id.loading_status_text);

        this.rootView = view;
        this.fragmentView = fragmentView;

        ImageView loadingWheel = view.findViewById(R.id.loading_wheel);
        // Start the rotation animation
        Animation rotateAnimation = AnimationUtils.loadAnimation(context, R.anim.rotate);
        loadingWheel.startAnimation(rotateAnimation);
    }

    public void showLoadingScreen() {
        if (fragmentView != null) {
            fragmentView.setVisibility(View.GONE);
        } else {
            rootView.setVisibility(View.GONE);
        }

        loadingScreen.setVisibility(View.VISIBLE);
        dotRunnable = new Runnable() {
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
                loadingText.setText(context.getString(R.string.loading_upper_case, dots));
                handler.postDelayed(this, 500);
            }
        };
        handler.post(dotRunnable);
    }

    public void postLoadingStatus(String status) {
        resetTimer();
        loadingStatusText.setText(status);
    }

    private void resetTimer() {
        timerHandler.removeCallbacks(timerRunnable);
        timerHandler.postDelayed(timerRunnable, Constants.LOADING_SLEEP_TIMER_DURATION);
    }

    public void updateProgress(int progress) {
        resetTimer();
        loadingProgressBar.setProgress(progress);
        String progressString = Integer.toString(progress);
        percentageText.setText(context.getString(R.string.progress, progressString));
    }

    public void hideLoadingScreenWithCondition(boolean condition) {
        if (condition) {
            handler.postDelayed(this::hide, 1000);
        }
    }

    public void hideLoadingScreen() {
        handler.postDelayed(this::hide, 1000);
    }

    private void hide() {
        loadingScreen.setVisibility(View.GONE);
        if (fragmentView != null) {
            fragmentView.setVisibility(View.VISIBLE);
        }else{
            rootView.setVisibility(View.VISIBLE);
        }
        handler.removeCallbacks(dotRunnable);
        timerHandler.removeCallbacks(timerRunnable);
    }
}
