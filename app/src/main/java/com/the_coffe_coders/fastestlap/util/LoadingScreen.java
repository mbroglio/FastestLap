package com.the_coffe_coders.fastestlap.util;

import android.content.Context;
import android.os.CountDownTimer;
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
    private final View activityView, fragmentView;
    private final Handler timerHandler = new Handler();
    private int dotCount = 0;
    private boolean addingDots = true;
    private Runnable dotRunnable;
    public LoadingScreen(View view, Context context, View activityView, View fragmentView) {
        this.handler = new Handler();
        this.loadingScreen = view.findViewById(R.id.loading_screen);
        this.context = context;
        this.loadingText = view.findViewById(R.id.loading_text);
        this.percentageText = view.findViewById(R.id.loading_percentage_text);
        this.loadingProgressBar = view.findViewById(R.id.loading_progress_bar);
        this.loadingStatusText = view.findViewById(R.id.loading_status_text);
        this.activityView = activityView;
        this.fragmentView = fragmentView;

        ImageView loadingWheel = view.findViewById(R.id.loading_wheel);
        Animation rotateAnimation = AnimationUtils.loadAnimation(context, R.anim.rotate);
        loadingWheel.startAnimation(rotateAnimation);
    }    private final Runnable timerRunnable = this::hide;

    public void showLoadingScreen(boolean invisible) {
        if (fragmentView != null) {
            if(invisible){
                fragmentView.setVisibility(View.INVISIBLE);
            } else {
                fragmentView.setVisibility(View.GONE);
            }
        } else {
            if(invisible){
                activityView.setVisibility(View.INVISIBLE);
            } else {
                activityView.setVisibility(View.GONE);
            }
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

    private void resetTimer() {
        timerHandler.removeCallbacks(timerRunnable);
        new CountDownTimer(Constants.LOADING_SLEEP_TIMER_DURATION, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                timerRunnable.run();
            }
        }.start();
    }

    public void updateProgress() {
        resetTimer();
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
        } else {
            activityView.setVisibility(View.VISIBLE);
        }
        handler.removeCallbacks(dotRunnable);
        timerHandler.removeCallbacks(timerRunnable);
    }


}
