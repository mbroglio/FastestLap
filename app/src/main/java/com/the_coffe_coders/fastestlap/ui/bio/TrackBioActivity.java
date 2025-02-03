package com.the_coffe_coders.fastestlap.ui.bio;

import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_NATIONS_COLLECTION;
import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_REALTIME_DATABASE;

import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Track;
import com.the_coffe_coders.fastestlap.domain.grand_prix.TrackHistory;
import com.the_coffe_coders.fastestlap.domain.nation.Nation;
import com.the_coffe_coders.fastestlap.repository.track.TrackRepository;
import com.the_coffe_coders.fastestlap.util.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.UIUtils;

public class TrackBioActivity extends AppCompatActivity {

    LoadingScreen loadingScreen;
    private GestureDetector tapDetector;
    private Track track;
    private Nation nation;
    private ImageView circuitImage;
    private ImageView countryFlag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        UIUtils.hideSystemUI(this);
        setContentView(R.layout.activity_track_bio);

        loadingScreen = new LoadingScreen(getWindow().getDecorView(), this);
        loadingScreen.showLoadingScreen();

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);

        tapDetector = UIUtils.createTapDetector(this);


        UIUtils.applyWindowInsets(toolbar);

        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        String trackId = getIntent().getStringExtra("CIRCUIT_ID");
        Log.i("TrackBioActivity", "CIRCUIT_ID: " + trackId);

        String grandPrixName = getIntent().getStringExtra("GRAND_PRIX_NAME");
        Log.i("TrackBioActivity", "GRAND_PRIX_NAME: " + grandPrixName);

        TextView title = findViewById(R.id.topAppBarTitle);
        title.setText(grandPrixName);

        circuitImage = findViewById(R.id.track_image);
        countryFlag = findViewById(R.id.country_flag);

        TrackRepository trackRepository = new TrackRepository();
        MutableLiveData<Result> trackLiveData = trackRepository.getTrack(trackId);

        try {
            trackLiveData.observe(this, trackResult -> {
                if (trackResult.isSuccess()) {
                    track = ((Result.TrackSuccess) trackResult).getData();
                    Log.i("TrackBioActivity", "Circuit from DB: " + track.toStringDB());
                    DatabaseReference nationReference = FirebaseDatabase.getInstance(FIREBASE_REALTIME_DATABASE).getReference(FIREBASE_NATIONS_COLLECTION).child(track.getCountry());
                    Log.i("DriverBioActivity", "Nation reference: " + nationReference);
                    nationReference.get().addOnCompleteListener(nationTask -> {
                        if (nationTask.isSuccessful()) {
                            nation = nationTask.getResult().getValue(Nation.class);
                            Log.i("DriverBioActivity", "Nation data: " + nation.toString());

                            setCircuitData(track, nation);

                        } else {
                            Log.e("DriverBioActivity", "Error getting nation data", nationTask.getException());
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
        Glide.with(this).load(track.getTrack_full_layout_url()).into(circuitImage);
        Glide.with(this).load(nation.getNation_flag_url()).into(countryFlag);

        TextView circuitName = findViewById(R.id.circuit_name_value);
        //circuitName.setText(circuit.getCircuitName());
        circuitName.setText("NA");

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
        //loadingScreen.hideLoadingScreen();
        createHistoryTable();
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
    public boolean dispatchTouchEvent(MotionEvent ev) {
        tapDetector.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onResume() {
        UIUtils.hideSystemUI(this);
        super.onResume();
    }
}