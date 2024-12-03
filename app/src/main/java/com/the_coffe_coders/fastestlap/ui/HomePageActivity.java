package com.the_coffe_coders.fastestlap.ui;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.the_coffe_coders.fastestlap.R;

import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

public class HomePageActivity extends AppCompatActivity {
    private String TAG = "HomePageActivity";
    private ErgastAPI ergastAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home_page);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.homepage), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setLastRaceCard();
    }

    private void setLastRaceCard() {
        String BASE_URL = "https://ergast.com/api/f1/";
    }
}