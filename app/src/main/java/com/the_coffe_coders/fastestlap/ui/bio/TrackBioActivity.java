package com.the_coffe_coders.fastestlap.ui.bio;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TableLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.the_coffe_coders.fastestlap.R;

/*
 * TODO:
 * - Implement firebase to get the data from the remote database
 */

public class TrackBioActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_track_bio);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        String circuitId = getIntent().getStringExtra("CIRCUIT_ID");
        Log.i("ConstructorBioActivity", "Circuit ID: " + circuitId);

        TableLayout tableLayout = findViewById(R.id.history_table);
        LayoutInflater inflater = LayoutInflater.from(this);

        View tableHeader = inflater.inflate(R.layout.track_bio_table_header, tableLayout, false);
        tableLayout.addView(tableHeader);

        // Add data rows
        for (int i = 0; i < 10; i++) {
            View tableRow = inflater.inflate(R.layout.track_bio_table_row, tableLayout, false);
            tableLayout.addView(tableRow);

        }

    }
}