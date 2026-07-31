package com.the_coffe_coders.fastestlap.ui.live;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabItem;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.adapter.LivePagerAdapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.f1.livetiming.RaceControlMessage;
import com.the_coffe_coders.fastestlap.ui.live.viewmodel.LiveViewModel;
import com.the_coffe_coders.fastestlap.ui.live.viewmodel.LiveViewModelFactory;

import java.util.List;

public class LiveActivity extends AppCompatActivity {

    private AppBarLayout appBarLayout;
    private MaterialToolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private CheckBox fullTelemetryCheckbox;
    private LiveViewModel liveViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_live);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        appBarLayout = findViewById(R.id.top_bar_layout);
        toolbar = findViewById(R.id.top_app_bar);
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        //fullTelemetryCheckbox = findViewById(R.id.full_telemetry_checkbox);

        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        String eventTitle = getIntent().getStringExtra("EVENT_TITLE");
        if (eventTitle != null && !eventTitle.isEmpty()) {
            toolbar.setTitle(eventTitle);
        }

        ViewPager2 viewPager = findViewById(R.id.view_pager);
        LivePagerAdapter adapter = new LivePagerAdapter(this);
        viewPager.setAdapter(adapter);
        viewPager.setUserInputEnabled(false); // solo click sul tab, nessuno swipe

        TabLayout tabLayout = findViewById(R.id.tab_layout);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("LIVE");
                    break;
                case 1:
                    tab.setText("RACE CONTROL");
                    break;
                case 2:
                    tab.setText("VERSUS");
                    break;
            }
        }).attach();

        // Controlla il polling in base alla tab selezionata
        liveViewModel = new ViewModelProvider(
                this,
                new LiveViewModelFactory(getApplication())
        ).get(LiveViewModel.class);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            private static final int RACE_CONTROL_TAB = 1;

            @Override
            public void onPageSelected(int position) {
                if (position == RACE_CONTROL_TAB) {
                    liveViewModel.startPolling();
                } else {
                    liveViewModel.stopPolling();
                }
            }
        });

        setupSessionSituationObserver();
    }

    private void setupSessionSituationObserver() {
        TextView currentLapText = findViewById(R.id.current_lap_text);
        TextView eventSituationText = findViewById(R.id.event_situation_text);
        View eventSituationTextLayout = findViewById(R.id.event_situation_text_layout);
        View light1 = findViewById(R.id.event_situation_light_1);
        View light2 = findViewById(R.id.event_situation_light_2);

        String totalLaps = getIntent().getStringExtra("TOTAL_LAPS");
        if (totalLaps == null || totalLaps.isEmpty()) {
            totalLaps = "53";
        }
        final String totalLapsFormatted = totalLaps;

        LiveViewModel liveViewModel = new ViewModelProvider(
                this,
                new LiveViewModelFactory(getApplication())
        ).get(LiveViewModel.class);

        liveViewModel.getRaceControlMessages().observe(this, result -> {
            if (result instanceof Result.RaceControlSuccess) {
                List<RaceControlMessage> messages = ((Result.RaceControlSuccess) result).getData();
                if (messages != null && !messages.isEmpty()) {
                    int maxLap = -1;
                    String latestFlag = null;

                    for (RaceControlMessage msg : messages) {
                        if (msg.getLapNumber() != null && msg.getLapNumber() > maxLap) {
                            maxLap = msg.getLapNumber();
                        }
                    }

                    // Prende l'ultimo flag registrato in ordine cronologico
                    for (int i = messages.size() - 1; i >= 0; i--) {
                        RaceControlMessage msg = messages.get(i);
                        if (msg.getFlag() != null && !msg.getFlag().isEmpty()) {
                            latestFlag = msg.getFlag().toUpperCase();
                            break;
                        }
                    }

                    // Formato LAP: XX/YY
                    if (currentLapText != null) {
                        if (maxLap > 0) {
                            currentLapText.setText("LAP: " + maxLap + "/" + totalLapsFormatted);
                        } else {
                            currentLapText.setText("LAP: 0/" + totalLapsFormatted);
                        }
                    }

                    // Gestione colore e testo bandiera + luci circolari laterali
                    if (eventSituationText != null && eventSituationTextLayout != null) {
                        int flagColor;
                        if (latestFlag != null && latestFlag.contains("YELLOW")) {
                            flagColor = Color.parseColor("#FBC02D");
                            eventSituationText.setText("YELLOW\nFLAG");
                            eventSituationTextLayout.setBackgroundResource(R.drawable.background_all_margins_filled_yellow);
                        } else if (latestFlag != null && latestFlag.contains("RED")) {
                            flagColor = Color.parseColor("#D32F2F");
                            eventSituationText.setText("RED\nFLAG");
                            eventSituationTextLayout.setBackgroundResource(R.drawable.background_all_margins_filled_red);
                        } else {
                            // Default o GREEN / CLEAR / CHEQUERED -> GREEN FLAG su sfondo verde
                            flagColor = Color.parseColor("#2E7D32");
                            eventSituationText.setText("GREEN\nFLAG");
                            eventSituationTextLayout.setBackgroundResource(R.drawable.background_all_margins_filled_green);
                        }

                        if (light1 != null) {
                            light1.setBackgroundTintList(ColorStateList.valueOf(flagColor));
                        }
                        if (light2 != null) {
                            light2.setBackgroundTintList(ColorStateList.valueOf(flagColor));
                        }
                    }
                }
            }
        });
    }

    public void setFullTelemetryMode(boolean isFullTelemetry) {
        if (isFullTelemetry) {
            appBarLayout.setVisibility(View.GONE);
            toolbar.setVisibility(View.GONE);
            tabLayout.setVisibility(View.GONE);
            hideSystemUI();
            viewPager.setUserInputEnabled(false);
        } else {
            appBarLayout.setVisibility(View.VISIBLE);
            toolbar.setVisibility(View.VISIBLE);
            tabLayout.setVisibility(View.VISIBLE);
            showSystemUI();
            viewPager.setUserInputEnabled(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (liveViewModel != null) {
            liveViewModel.stopPolling();
        }
    }

    private void showSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }
}