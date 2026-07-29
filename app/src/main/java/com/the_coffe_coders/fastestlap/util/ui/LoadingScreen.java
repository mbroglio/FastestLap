package com.the_coffe_coders.fastestlap.util.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.util.Constants;

public class LoadingScreen {

    private final Handler handler;
    private final View loadingScreen;
    private final Context context;
    private final TextView loadingText;
    private final View activityView, fragmentView;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private int dotCount = 0;
    private boolean addingDots = true;
    private Runnable dotRunnable;

    public LoadingScreen(View view, Context context, View activityView, View fragmentView) {
        this.handler = new Handler(Looper.getMainLooper());
        this.loadingScreen = view.findViewById(R.id.loading_screen);
        this.context = context;
        this.loadingText = view.findViewById(R.id.loading_text);
        this.activityView = activityView;
        this.fragmentView = fragmentView;

        ImageView loadingWheel = view.findViewById(R.id.loading_wheel);
        Animation rotateAnimation = AnimationUtils.loadAnimation(context, R.anim.rotate);
        loadingWheel.startAnimation(rotateAnimation);
    }

    public void showLoadingScreen(boolean invisible) {
        handler.removeCallbacks(hideRunnable);
        timerHandler.removeCallbacks(timerRunnable);
        resetTimer();
        if (fragmentView != null) {
            fragmentView.setVisibility(invisible ? View.INVISIBLE : View.GONE);
        } else {
            activityView.setVisibility(invisible ? View.INVISIBLE : View.GONE);
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
                loadingText.setText(context.getString(R.string.loading, dots));
                handler.postDelayed(this, 500);
            }
        };
        handler.post(dotRunnable);
    }

    private void resetTimer() {
        timerHandler.removeCallbacks(timerRunnable);
        timerHandler.postDelayed(timerRunnable, Constants.LOADING_SLEEP_TIMER_DURATION);
    }

    public void updateProgress() {
        resetTimer();
    }

    private final Runnable hideRunnable = this::hide;

    @SuppressWarnings("unused")
    public void hideLoadingScreenWithCondition(boolean condition) {
        if (condition) {
            hideLoadingScreen();
        }
    }

    public void hideLoadingScreen() {
        if (loadingScreen != null && loadingScreen.getVisibility() == View.GONE) {
            return;
        }
        handler.removeCallbacks(hideRunnable);
        timerHandler.removeCallbacks(timerRunnable);
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    @SuppressWarnings("unused")
    public void hideLoadingScreenImmediately() {
        if (loadingScreen != null && loadingScreen.getVisibility() == View.GONE) {
            return;
        }
        handler.removeCallbacks(hideRunnable);
        timerHandler.removeCallbacks(timerRunnable);
        handler.post(hideRunnable);
    }

    private void hide() {
        if (loadingScreen == null || loadingScreen.getVisibility() == View.GONE) {
            return;
        }
        loadingScreen.setVisibility(View.GONE);
        if (fragmentView != null) {
            fragmentView.setVisibility(View.VISIBLE);
        } else if (activityView != null) {
            activityView.setVisibility(View.VISIBLE);
        }
        handler.removeCallbacks(dotRunnable);
        timerHandler.removeCallbacks(timerRunnable);
        android.util.Log.i("LoadingScreenLog", "LOADING_SCREEN_HIDDEN for activity: " + context.getClass().getSimpleName() + " at " + System.currentTimeMillis());
    }

    private final Runnable timerRunnable = hideRunnable;

}
