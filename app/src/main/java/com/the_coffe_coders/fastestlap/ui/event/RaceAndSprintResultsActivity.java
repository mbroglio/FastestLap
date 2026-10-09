package com.the_coffe_coders.fastestlap.ui.event;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.adapter.f1.RaceAndSprintResultsPagerAdapter;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Race;
import com.the_coffe_coders.fastestlap.domain.f1.result.RaceResult;
import com.the_coffe_coders.fastestlap.domain.f1.result.RaceResultFastestLap;
import com.the_coffe_coders.fastestlap.domain.f1.result.Stint;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.EventViewModel;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Activity per la visualizzazione dei risultati di Gara/Sprint e degli Stint tramite TabLayout.
 */
public class RaceAndSprintResultsActivity extends AppCompatActivity {

    private static final String TAG = "RaceAndSprintResultsActivity";
    private Race race;
    private String locality;
    private List<Stint> stintsList;
    private RaceResultFastestLap raceFastestLap;
    private EventViewModel eventViewModel;
    private RelativeLayout resultsLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_race_and_sprint_results);

        if (getIntent() != null) {
            race = getIntent().getParcelableExtra("RACE");
            stintsList = getIntent().getParcelableArrayListExtra("STINTS");
            locality = getIntent().getStringExtra("LOCALITY");
            raceFastestLap = getIntent().getParcelableExtra("FASTEST_LAP");
            String gpLongName = getIntent().getStringExtra("GP_LONG_NAME");
            if (race != null) {
                if (race.getTrack() == null) {
                    com.the_coffe_coders.fastestlap.domain.f1.track.Track t = new com.the_coffe_coders.fastestlap.domain.f1.track.Track();
                    if (locality != null) {
                        com.the_coffe_coders.fastestlap.domain.f1.track.Location loc = new com.the_coffe_coders.fastestlap.domain.f1.track.Location();
                        loc.setLocality(locality);
                        t.setLocation(loc);
                    }
                    t.setGp_long_name(gpLongName);
                    race.setTrack(t);
                } else {
                    if (race.getTrack().getGp_long_name() == null && gpLongName != null) {
                        race.getTrack().setGp_long_name(gpLongName);
                    }
                    if (race.getTrack().getLocation() == null && locality != null) {
                        com.the_coffe_coders.fastestlap.domain.f1.track.Location loc = new com.the_coffe_coders.fastestlap.domain.f1.track.Location();
                        loc.setLocality(locality);
                        race.getTrack().setLocation(loc);
                    }
                }
                if (locality == null && race.getTrack().getLocation() != null) {
                    locality = race.getTrack().getLocation().getLocality();
                }
            }

            // Recovery: se non passati esplicitamente, recupera stint persistiti nell'oggetto Race
            if (stintsList == null || stintsList.isEmpty()) {
                if (race != null) {
                    String sessionType = (race.getRaceResults() != null && !race.getRaceResults().isEmpty()) ? "Race" : "Sprint";
                    stintsList = "Sprint".equalsIgnoreCase(sessionType) ? race.getSprintStints() : race.getRaceStints();
                    if (stintsList == null || stintsList.isEmpty()) {
                        stintsList = race.getStints();
                    }
                }
            }
        }

        start();
    }

    private void start(){
        resultsLayout = findViewById(R.id.results_layout);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
            UIUtils.applyWindowInsets(toolbar);
        }

        UIUtils.applyWindowInsets(resultsLayout);

        setupActivity();
    }

    private void setupActivity() {
        if (race == null) {
            View notAvailable = findViewById(R.id.data_not_available_layout);
            if (notAvailable != null) notAvailable.setVisibility(View.VISIBLE);
            View tabLayout = findViewById(R.id.tab_layout);
            if (tabLayout != null) tabLayout.setVisibility(View.GONE);
            View viewPager = findViewById(R.id.view_pager);
            if (viewPager != null) viewPager.setVisibility(View.GONE);
            View fastestLapLayout = findViewById(R.id.fastest_lap_layout);
            if (fastestLapLayout != null) fastestLapLayout.setVisibility(View.GONE);
            return;
        }

        List<RaceResult> raceResultsList = race.getRaceResults();
        List<RaceResult> sprintResultsList = race.getSprintResults();

        TextView raceTitle = findViewById(R.id.topAppBarTitle);
        if (race.getRaceName() != null && raceTitle != null) {
            UIUtils.singleSetTextViewText(race.getRaceName().toUpperCase(), raceTitle);
        }
        Log.i(TAG, "Race: " +race);
        TextView eventDescriptionTitle = findViewById(R.id.event_description_title);
        if (eventDescriptionTitle != null) {
            String sessionTypeStr = (raceResultsList != null && !raceResultsList.isEmpty())
                    ? getString(R.string.race) : getString(R.string.sprint);
            UIUtils.singleSetTextViewText(getString(R.string.full_event_results, sessionTypeStr), eventDescriptionTitle);
        }

        // Setup TabLayout & ViewPager2
        ViewPager2 viewPager = findViewById(R.id.view_pager);
        RaceAndSprintResultsPagerAdapter pagerAdapter = new RaceAndSprintResultsPagerAdapter(this, race, locality, stintsList);
        if (viewPager != null) {
            viewPager.setAdapter(pagerAdapter);
        }

        TabLayout tabLayout = findViewById(R.id.tab_layout);
        if (tabLayout != null && viewPager != null) {
            new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
                switch (position) {
                    case 0:
                        tab.setText(getString(R.string.results).toUpperCase(java.util.Locale.getDefault()));
                        break;
                    case 1:
                        tab.setText(getString(R.string.stints).toUpperCase(java.util.Locale.getDefault()));
                        break;
                }
            }).attach();
        }

        setupFastestLapLayout();
    }

    private void setupFastestLapLayout() {
        Log.i(TAG, "Fastest lap: " + raceFastestLap);
        MaterialCardView fastestLapLayout = findViewById(R.id.fastest_lap_layout);

        if (fastestLapLayout == null) return;

        if (raceFastestLap == null || raceFastestLap.isNull()) {
            fastestLapLayout.setVisibility(View.GONE);
            return;
        }

        fastestLapLayout.setVisibility(View.VISIBLE);
        View teamColorIndicator = fastestLapLayout.findViewById(R.id.team_color_indicator);
        if (teamColorIndicator != null) {
            int teamColor = ContextCompat.getColor(this,
                    Objects.requireNonNullElseGet(Constants.TEAM_COLOR.get(raceFastestLap.getConstructorId()), () -> R.color.white));
            teamColorIndicator.setBackgroundColor(teamColor);
        }

        UIUtils.multipleSetTextViewText(
                new String[]{
                        raceFastestLap.getDriverName(),
                        raceFastestLap.getTime().getTime(),
                        getString(R.string.lap, raceFastestLap.getLap())},
                new TextView[]{
                        fastestLapLayout.findViewById(R.id.driver_name),
                        fastestLapLayout.findViewById(R.id.fastest_lap),
                        fastestLapLayout.findViewById(R.id.lap_value)});
    }

    public void updateStints(List<Stint> newStints) {
        if (newStints == null || newStints.isEmpty()) return;
        this.stintsList = new ArrayList<>(newStints);
        if (getIntent() != null) {
            getIntent().putParcelableArrayListExtra("STINTS", new ArrayList<>(newStints));
        }
        if (race != null) {
            String sessionType = (race.getRaceResults() != null && !race.getRaceResults().isEmpty()) ? "Race" : "Sprint";
            if ("Sprint".equalsIgnoreCase(sessionType)) {
                race.setSprintStints(newStints);
            } else {
                race.setRaceStints(newStints);
            }
        }
    }

    public List<Stint> getStintsList() {
        return stintsList;
    }
}
