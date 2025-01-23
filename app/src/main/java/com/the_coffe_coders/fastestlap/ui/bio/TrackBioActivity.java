package com.the_coffe_coders.fastestlap.ui.bio;

import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_CIRCUITS_COLLECTION;
import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_NATIONS_COLLECTION;
import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_REALTIME_DATABASE;
import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_TEAMS_COLLECTION;

import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Circuit;
import com.the_coffe_coders.fastestlap.domain.grand_prix.CircuitHistory;
import com.the_coffe_coders.fastestlap.domain.nation.Nation;
import com.the_coffe_coders.fastestlap.util.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.UIUtils;

import org.w3c.dom.Text;

/*
 * TODO:
 * - Implement firebase to get the data from the remote database
 */

public class TrackBioActivity extends AppCompatActivity {

    private GestureDetector tapDetector;

    private Circuit circuit;
    private Nation nation;
    private ImageView circuitImage;
    private ImageView countryFlag;

    LoadingScreen loadingScreen;

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

        String circuitId = getIntent().getStringExtra("CIRCUIT_ID");
        Log.i("TrackBioActivity", "CIRCUIT_ID: " + circuitId);

        String grandPrixName = getIntent().getStringExtra("GRAND_PRIX_NAME");
        Log.i("TrackBioActivity", "GRAND_PRIX_NAME: " + grandPrixName);

        TextView title = findViewById(R.id.topAppBarTitle);
        title.setText(grandPrixName);

        circuitImage = findViewById(R.id.track_image);
        countryFlag = findViewById(R.id.country_flag);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance(FIREBASE_REALTIME_DATABASE).getReference(FIREBASE_CIRCUITS_COLLECTION).child(circuitId);
        Log.i("TrackBioActivity", "Database reference: " + databaseReference.toString());
        databaseReference.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                circuit = task.getResult().getValue(Circuit.class);
                Log.i("TrackBioActivity", "Circuit from DB: " + circuit.toStringDB());
                DatabaseReference nationReference = FirebaseDatabase.getInstance(FIREBASE_REALTIME_DATABASE).getReference(FIREBASE_NATIONS_COLLECTION).child(circuit.getCountry());
                Log.i("DriverBioActivity", "Nation reference: " + nationReference.toString());
                nationReference.get().addOnCompleteListener(nationTask -> {
                    if (nationTask.isSuccessful()) {
                        nation = nationTask.getResult().getValue(Nation.class);
                        Log.i("DriverBioActivity", "Nation data: " + nation.toString());

                        setCircuitData(circuit, nation);

                    } else {
                        Log.e("DriverBioActivity", "Error getting nation data", nationTask.getException());
                    }
                });

            } else {
                Log.e("TrackBioActivity", "Error getting data", task.getException());
            }
        });
    }

    private void setCircuitData(Circuit circuit, Nation nation) {
        Glide.with(this).load(circuit.getCircuit_full_layout_url()).into(circuitImage);
        Glide.with(this).load(nation.getNation_flag_url()).into(countryFlag);

        TextView circuitName = findViewById(R.id.circuit_name_value);
        //circuitName.setText(circuit.getCircuitName());
        circuitName.setText("NA");

        TextView numberOfLaps = findViewById(R.id.number_of_laps_value);
        numberOfLaps.setText(circuit.getLaps());

        TextView circuitLength = findViewById(R.id.circuit_length_value);
        circuitLength.setText(circuit.getLength());

        TextView raceDistance = findViewById(R.id.race_distance_value);
        raceDistance.setText(circuit.getRace_distance());

        String fastestLapValue = circuit.getLap_record().split(" ")[0];

        String fastestLapDriver = circuit.getLap_record().substring(fastestLapValue.length() + 1);

        TextView fastestLap = findViewById(R.id.fastest_lap_value);
        fastestLap.setText(fastestLapValue);

        TextView fastestLapDriverName = findViewById(R.id.fastest_lap_driver);
        fastestLapDriverName.setText(fastestLapDriver);

        createHistoryTable();
    }

    private void createHistoryTable() {
        TableLayout tableLayout = findViewById(R.id.history_table);
        LayoutInflater inflater = LayoutInflater.from(this);

        View tableHeader = inflater.inflate(R.layout.track_bio_table_header, tableLayout, false);
        tableHeader.setBackgroundColor(ContextCompat.getColor(this, R.color.timer_gray_dark));
        tableLayout.addView(tableHeader);

        for(CircuitHistory history : circuit.getCircuit_history()) {
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