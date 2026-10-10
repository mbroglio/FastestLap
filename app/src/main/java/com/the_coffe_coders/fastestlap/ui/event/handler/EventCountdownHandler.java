package com.the_coffe_coders.fastestlap.ui.event.handler;

import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.util.ui.LoadingScreen;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

public class EventCountdownHandler {
    private static final String TAG = "EventCountdownHandler";

    private final AppCompatActivity activity;
    private final View rootView;
    private final LoadingScreen loadingScreen;
    private CountDownTimer countDownTimer;

    public interface OnCountdownLiveTriggerListener {
        void onLiveTriggered();
    }

    public EventCountdownHandler(AppCompatActivity activity, View rootView, LoadingScreen loadingScreen) {
        this.activity = activity;
        this.rootView = rootView;
        this.loadingScreen = loadingScreen;
    }

    public void startCountdown(LocalDateTime eventDate, OnCountdownLiveTriggerListener liveTriggerListener) {
        loadingScreen.updateProgress();

        View countdownView = rootView.findViewById(R.id.timer_card_countdown);
        View resultsView = rootView.findViewById(R.id.timer_card_results);
        View pendingResultsView = rootView.findViewById(R.id.timer_card_pending_results);

        if (countdownView != null) {
            countdownView.setVisibility(View.VISIBLE);
        }
        if (resultsView != null) {
            resultsView.setVisibility(View.GONE);
        }
        if (pendingResultsView != null) {
            pendingResultsView.setVisibility(View.GONE);
        }

        cancelCountdown();

        long millisUntilStart = ZonedDateTime.of(eventDate, ZoneId.systemDefault()).toInstant().toEpochMilli() - System.currentTimeMillis();
        countDownTimer = new CountDownTimer(millisUntilStart, 1000) {
            final TextView days_counter = rootView.findViewById(R.id.next_days_counter);
            final TextView hours_counter = rootView.findViewById(R.id.next_hours_counter);
            final TextView minutes_counter = rootView.findViewById(R.id.next_minutes_counter);
            final TextView seconds_counter = rootView.findViewById(R.id.next_seconds_counter);
            boolean liveActivated = false;

            @Override
            public void onTick(long millisUntilFinished) {
                long days = millisUntilFinished / 86400000;
                long hours = (millisUntilFinished % 86400000) / 3600000;
                long minutes = ((millisUntilFinished % 86400000) % 3600000) / 60000;
                long seconds = (((millisUntilFinished % 86400000) % 3600000) % 60000) / 1000;

                if (days_counter != null) days_counter.setText(String.valueOf(days));
                if (hours_counter != null) hours_counter.setText(String.valueOf(hours));
                if (minutes_counter != null) minutes_counter.setText(String.valueOf(minutes));
                if (seconds_counter != null) seconds_counter.setText(String.valueOf(seconds));

                if (millisUntilFinished <= 5 * 60 * 1000L && !liveActivated) {
                    liveActivated = true;
                    if (liveTriggerListener != null) {
                        liveTriggerListener.onLiveTriggered();
                    }
                }
            }

            @Override
            public void onFinish() {
                if (days_counter != null) days_counter.setText("0");
                if (hours_counter != null) hours_counter.setText("0");
                if (minutes_counter != null) minutes_counter.setText("0");
                if (seconds_counter != null) seconds_counter.setText("0");

                if (liveTriggerListener != null) {
                    liveTriggerListener.onLiveTriggered();
                }
            }
        }.start();

        Log.i("ActivityDataLog", "DATA_AND_IMAGES_FULLY_LOADED: EventActivity at " + System.currentTimeMillis());
        loadingScreen.hideLoadingScreen();
    }

    public void cancelCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }
}
