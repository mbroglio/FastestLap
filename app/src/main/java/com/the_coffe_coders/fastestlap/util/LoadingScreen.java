package com.the_coffe_coders.fastestlap.util;

import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.the_coffe_coders.fastestlap.R;

public class LoadingScreen {

    private final Handler handler;
    private final View loadingScreen;

    private final TextView loadingText;

    private int dotCount = 0;
    private boolean addingDots = true;
    private final Runnable dotRunnable = new Runnable() {
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

    public LoadingScreen(View view, Context context) {
        this.handler = new Handler();
        //loading screen logic
        this.loadingScreen = view.findViewById(R.id.loading_screen);
        loadingText = view.findViewById(R.id.loading_text);
        ImageView loadingWheel = view.findViewById(R.id.loading_wheel);

        // Start the rotation animation
        Animation rotateAnimation = AnimationUtils.loadAnimation(context, R.anim.rotate);
        loadingWheel.startAnimation(rotateAnimation);
    }

    public void showLoadingScreen() {
        loadingScreen.setVisibility(View.VISIBLE);
        handler.post(dotRunnable);
    }

    public void hideLoadingScreen() {
        loadingScreen.setVisibility(View.GONE);
        handler.removeCallbacks(dotRunnable);

    }
}
