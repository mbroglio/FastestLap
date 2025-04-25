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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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
    private String trackId;

    private TrackViewModel trackViewModel;
    private NationViewModel nationViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_track_bio);

        start();
    }

    private void start(){
        loadingScreen = new LoadingScreen(getWindow().getDecorView(), this);
        loadingScreen.showLoadingScreen();

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        UIUtils.applyWindowInsets(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        SwipeRefreshLayout trackBioLayout = findViewById(R.id.track_bio_layout);
        UIUtils.applyWindowInsets(trackBioLayout);
        trackBioLayout.setOnRefreshListener(() -> {
            start();
            trackBioLayout.setRefreshing(false);
        });

        trackId = getIntent().getStringExtra("CIRCUIT_ID");
        Log.i("TrackBioActivity", "CIRCUIT_ID: " + trackId);

        UIUtils.singleSetTextViewText(getIntent().getStringExtra("GRAND_PRIX_NAME"),
                findViewById(R.id.topAppBarTitle));

        circuitImage = findViewById(R.id.track_image);
        countryFlag = findViewById(R.id.country_flag);

        initializeViewModels();
    }

    private void initializeViewModels(){
        trackViewModel = new ViewModelProvider(this, new TrackViewModelFactory(getApplication())).get(TrackViewModel.class);
        nationViewModel = new ViewModelProvider(this, new NationViewModelFactory(getApplication())).get(NationViewModel.class);

        fetchTrack();
    }

    private void fetchTrack() {
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

        UIUtils.multipleSetTextViewText(
                new String[]{track.getTrackName(),
                track.getFirst_entry(),
                        track.getLaps(),
                        track.getLength(),
                        track.getRace_distance(),
                        track.getLap_record().split(" ")[0],
                        track.getLap_record().substring(track.getLap_record().split(" ")[0].length() + 1)},

                new TextView[]{findViewById(R.id.circuit_name_value),
                        findViewById(R.id.circuit_first_entry_value),
                        findViewById(R.id.number_of_laps_value),
                        findViewById(R.id.circuit_length_value),
                        findViewById(R.id.race_distance_value),
                        findViewById(R.id.fastest_lap_value),
                        findViewById(R.id.fastest_lap_driver)});

        UIUtils.loadSequenceOfImagesWithGlide(this,
                new String[]{track.getTrack_full_layout_url(),nation.getNation_flag_url()},
                new ImageView[]{circuitImage, countryFlag},
                this::createHistoryTable);

    }

    private void createHistoryTable() {
        TableLayout tableLayout = findViewById(R.id.history_table);
        tableLayout.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        View tableHeader = inflater.inflate(R.layout.track_bio_table_header, tableLayout, false);
        tableHeader.setBackgroundColor(ContextCompat.getColor(this, R.color.timer_gray_dark));
        tableLayout.addView(tableHeader);

        for (TrackHistory history : track.getTrack_history()) {
            View tableRow = inflater.inflate(R.layout.track_bio_table_row, tableLayout, false);

            UIUtils.multipleSetTextViewText(
                    new String[]{
                            history.getYear(),
                            history.getPodium().get(0),
                            history.getPodium().get(1),
                            history.getPodium().get(2),
                            history.getTeam().get(0),
                            history.getTeam().get(1),
                            history.getTeam().get(2)},

                    new TextView[]{
                            tableRow.findViewById(R.id.season_year),
                            tableRow.findViewById(R.id.first_driver),
                            tableRow.findViewById(R.id.second_driver),
                            tableRow.findViewById(R.id.third_driver),
                            tableRow.findViewById(R.id.first_driver_team),
                            tableRow.findViewById(R.id.second_driver_team),
                            tableRow.findViewById(R.id.third_driver_team)});

            tableLayout.addView(tableRow);
        }

        loadingScreen.hideLoadingScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}