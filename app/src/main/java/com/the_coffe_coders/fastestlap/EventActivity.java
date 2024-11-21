package com.the_coffe_coders.fastestlap;

import android.graphics.Paint;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.text.HtmlCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Layout;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.ViewGroup;

import com.google.android.material.card.MaterialCardView;

import org.w3c.dom.Text;

public class EventActivity extends AppCompatActivity {

    private static final String TAG = "EventActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_event), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        int dynamicDimension = (int) (screenWidth * 0.18);

        TextView textView = findViewById(R.id.session_1_name);
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) textView.getLayoutParams();
        layoutParams.setMarginEnd(dynamicDimension); // Set margin end to 20 pixels
        textView.setLayoutParams(layoutParams);


        int dynamicDimension2 = (int) (screenWidth * 0.22);
        TextView textView2 = findViewById(R.id.session_1_day);
        ViewGroup.MarginLayoutParams layoutParams2 = (ViewGroup.MarginLayoutParams) textView2.getLayoutParams();
        layoutParams2.setMarginEnd(dynamicDimension2); // Set margin end to 20 pixels
        textView2.setLayoutParams(layoutParams2);

       //create a countdown logic for countdown layout using days_counter, hours_counter, minutes_counter and seconds_counter
        new CountDownTimer(1000000000, 1000) {
            TextView days_counter = findViewById(R.id.days_counter);
            TextView hours_counter = findViewById(R.id.hours_counter);
            TextView minutes_counter = findViewById(R.id.minutes_counter);
            TextView seconds_counter = findViewById(R.id.seconds_counter);

            public void onTick(long millisUntilFinished) {
                long days = millisUntilFinished / 86400000;
                long hours = (millisUntilFinished % 86400000) / 3600000;
                long minutes = ((millisUntilFinished % 86400000) % 3600000) / 60000;
                long seconds = (((millisUntilFinished % 86400000) % 3600000) % 60000) / 1000;

                days_counter.setText(String.valueOf(days));
                hours_counter.setText(String.valueOf(hours));
                minutes_counter.setText(String.valueOf(minutes));
                seconds_counter.setText(String.valueOf(seconds));
            }

            public void onFinish() {
                days_counter.setText("0");
                hours_counter.setText("0");
                minutes_counter.setText("0");
                seconds_counter.setText("0");
            }
        }.start();

        //add event listener logic for countdown layout
        LinearLayout trackPic = findViewById(R.id.track_pic);
        trackPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "clicked");

            }
        });

        //add event listener logic for live_session card
        MaterialCardView liveSession = findViewById(R.id.live_session);
        liveSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "live session clicked");
            }
        });

        //add event listener for weather card
        MaterialCardView weatherCard = findViewById(R.id.weather_forecast);
        weatherCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "weather card clicked");
            }
        });

        //add event listener for session_1_flag
        ImageView session1Flag = findViewById(R.id.session_1_flag);
        session1Flag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "session 1 flag clicked");
            }
        });









    }
}