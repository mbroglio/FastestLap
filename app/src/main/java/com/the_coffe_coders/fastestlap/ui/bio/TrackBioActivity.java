package com.the_coffe_coders.fastestlap.ui.bio;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Track;
import com.the_coffe_coders.fastestlap.domain.grand_prix.TrackHistory;
import com.the_coffe_coders.fastestlap.domain.nation.Nation;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.NationViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.NationViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.TrackViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.TrackViewModelFactory;
import com.the_coffe_coders.fastestlap.util.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.UIUtils;

public class TrackBioActivity extends AppCompatActivity {

    LoadingScreen loadingScreen;
    private Track track;
    private Nation nation;
    private ImageView circuitImage;
    private ImageView countryFlag;
    private ScrollView scrollView;

    private TrackViewModel trackViewModel;

    private NationViewModel nationViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_track_bio);

        loadingScreen = new LoadingScreen(getWindow().getDecorView(), this);
        loadingScreen.showLoadingScreen();

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);

        UIUtils.applyWindowInsets(toolbar);

        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        scrollView = findViewById(R.id.track_bio_scroll);
        UIUtils.applyWindowInsets(scrollView);

        String trackId = getIntent().getStringExtra("CIRCUIT_ID");
        Log.i("TrackBioActivity", "CIRCUIT_ID: " + trackId);

        String grandPrixName = getIntent().getStringExtra("GRAND_PRIX_NAME");
        Log.i("TrackBioActivity", "GRAND_PRIX_NAME: " + grandPrixName);

        TextView title = findViewById(R.id.topAppBarTitle);
        title.setText(grandPrixName);

        circuitImage = findViewById(R.id.track_image);
        countryFlag = findViewById(R.id.country_flag);

        trackViewModel = new ViewModelProvider(this, new TrackViewModelFactory()).get(TrackViewModel.class);
        nationViewModel = new ViewModelProvider(this, new NationViewModelFactory()).get(NationViewModel.class);

        MutableLiveData<Result> trackLiveData = trackViewModel.getTrack(trackId);

        try {
            trackLiveData.observe(this, trackResult -> {
                if (trackResult instanceof Result.Loading) {
                    return;
                }
                if (trackResult.isSuccess()) {
                    track = ((Result.TrackSuccess) trackResult).getData();
                    Log.i("TrackBioActivity", "Circuit from DB: " + track);

                    MutableLiveData<Result> nationLiveData = nationViewModel.getNation(track.getCountry());
                    nationLiveData.observe(this, nationResult -> {
                        if (nationResult instanceof Result.Loading) {
                            return;
                        }
                        if (nationResult.isSuccess()) {
                            nation = ((Result.NationSuccess) nationResult).getData();
                            setCircuitData(track, nation);
                        } else {
                            Log.e("TrackBioActivity", "Error getting data");
                        }
                    });
                } else {
                    Log.e("TrackBioActivity", "Error getting data");
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private void setCircuitData(Track track, Nation nation) {
        TextView circuitName = findViewById(R.id.circuit_name_value);
        circuitName.setText(track.getTrackName());

        TextView firstEntry = findViewById(R.id.circuit_first_entry_value);
        firstEntry.setText(track.getFirst_entry());

        TextView numberOfLaps = findViewById(R.id.number_of_laps_value);
        numberOfLaps.setText(track.getLaps());

        TextView circuitLength = findViewById(R.id.circuit_length_value);
        circuitLength.setText(track.getLength());

        TextView raceDistance = findViewById(R.id.race_distance_value);
        raceDistance.setText(track.getRace_distance());

        String fastestLapValue = track.getLap_record().split(" ")[0];

        String fastestLapDriver = track.getLap_record().substring(fastestLapValue.length() + 1);

        TextView fastestLap = findViewById(R.id.fastest_lap_value);
        fastestLap.setText(fastestLapValue);

        TextView fastestLapDriverName = findViewById(R.id.fastest_lap_driver);
        fastestLapDriverName.setText(fastestLapDriver);

        UIUtils.loadSequenceOfImagesWithGlide(this,
                new String[]{track.getTrack_full_layout_url(),nation.getNation_flag_url()},
                new ImageView[]{circuitImage, countryFlag},
                this::createHistoryTable);

    }

    private void createHistoryTable() {
        TableLayout tableLayout = findViewById(R.id.history_table);
        LayoutInflater inflater = LayoutInflater.from(this);

        View tableHeader = inflater.inflate(R.layout.track_bio_table_header, tableLayout, false);
        tableHeader.setBackgroundColor(ContextCompat.getColor(this, R.color.timer_gray_dark));
        tableLayout.addView(tableHeader);

        for (TrackHistory history : track.getTrack_history()) {
            View tableRow = inflater.inflate(R.layout.track_bio_table_row, tableLayout, false);

            TextView year = tableRow.findViewById(R.id.season_year);
            year.setText(history.getYear());

            TextView firstDriver = tableRow.findViewById(R.id.first_driver);
            firstDriver.setText(history.getPodium().get(0));

            TextView secondDriver = tableRow.findViewById(R.id.second_driver);
            secondDriver.setText(history.getPodium().get(1));

            TextView thirdDriver = tableRow.findViewById(R.id.third_driver);
            thirdDriver.setText(history.getPodium().get(2));

            TextView firstTeam = tableRow.findViewById(R.id.first_driver_team);
            firstTeam.setText(history.getTeam().get(0));

            TextView secondTeam = tableRow.findViewById(R.id.second_driver_team);
            secondTeam.setText(history.getTeam().get(1));

            TextView thirdTeam = tableRow.findViewById(R.id.third_driver_team);
            thirdTeam.setText(history.getTeam().get(2));

            tableLayout.addView(tableRow);
        }

        loadingScreen.hideLoadingScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}