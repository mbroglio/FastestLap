package com.the_coffe_coders.fastestlap.ui.bio;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.ui.standing.ConstructorsStandingActivity;

/*
 * TODO:
 * - Implement firebase to get the data from the remote database
 */

public class ConstructorBioActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_constructor_bio);


        MaterialToolbar toolbar = findViewById(R.id.topAppBar);

        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.topMargin = systemBars.top;
            v.setLayoutParams(params);

            return insets;
        });

        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        String teamId = getIntent().getStringExtra("TEAM_ID");
        Log.i("ConstructorBioActivity", "Team ID: " + teamId);

        MaterialCardView driverOneCard = findViewById(R.id.driver_1_card);

        driverOneCard.setOnClickListener(v -> {
            Intent intent = new Intent(ConstructorBioActivity.this, DriverBioActivity.class);
            intent.putExtra("DRIVER_ID", "leclerc");
            startActivity(intent);
        });

        MaterialCardView driverTwoCard = findViewById(R.id.driver_2_card);
        driverTwoCard.setOnClickListener(v -> {
            Intent intent = new Intent(ConstructorBioActivity.this, DriverBioActivity.class);
            intent.putExtra("DRIVER_ID", "sainz");
            startActivity(intent);
        });

        //set listener to team rank and define a method onclick
        MaterialCardView teamRank = findViewById(R.id.team_current_standing);
        teamRank.setOnClickListener(v -> {
            Intent intent = new Intent(ConstructorBioActivity.this, ConstructorsStandingActivity.class);
            intent.putExtra("TEAM_ID", "ferrari");
            startActivity(intent);
        });


        TableLayout tableLayout = findViewById(R.id.history_table);
        LayoutInflater inflater = LayoutInflater.from(this);

        View tableHeader = inflater.inflate(R.layout.constructor_bio_table_header, tableLayout, false);
        TableLayout.LayoutParams paramsHeader = (TableLayout.LayoutParams) tableHeader.getLayoutParams();
        paramsHeader.setMargins(0, 0, 0, (int) getResources().getDisplayMetrics().density * 5);
        tableHeader.setLayoutParams(paramsHeader);
        tableHeader.setBackgroundColor(ContextCompat.getColor(this, R.color.ferrari_f1));

        tableLayout.addView(tableHeader);

        // Add data rows
        for (int i = 0; i < 10; i++) {
            View tableRow = inflater.inflate(R.layout.constructor_bio_table_row, tableLayout, false);

            // Set bottom margin to 5dp
            TableLayout.LayoutParams params = (TableLayout.LayoutParams) tableRow.getLayoutParams();
            params.setMargins(0, 0, 0, (int) getResources().getDisplayMetrics().density * 5);
            tableRow.setLayoutParams(params);
            tableRow.setBackgroundColor(ContextCompat.getColor(this, R.color.ferrari_f1_light));

            tableLayout.addView(tableRow);


        }
    }
}