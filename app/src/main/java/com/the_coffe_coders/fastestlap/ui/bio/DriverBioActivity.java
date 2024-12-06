package com.the_coffe_coders.fastestlap.ui.bio;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.TextViewCompat;
import android.graphics.Typeface;
import androidx.core.content.res.ResourcesCompat;

import com.the_coffe_coders.fastestlap.R;

public class DriverBioActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_driver_bio);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.driver_bio_impl_logo_background), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //set listener to team logo and define a method onclick
        findViewById(R.id.team_logo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("DriverBioActivity", "Team logo clicked");
            }
        });

        findViewById(R.id.driver_current_standing).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("DriverBioActivity", "current standing clicked");
            }
        });

        TableLayout tableLayout = findViewById(R.id.history_table);
        LayoutInflater inflater = LayoutInflater.from(this);
        Typeface orbitronBold = ResourcesCompat.getFont(this, R.font.orbitron_bold);
        Typeface orbitronRegular = ResourcesCompat.getFont(this, R.font.orbitron_regular);

        View tableHeader = inflater.inflate(R.layout.driver_bio_table_row, tableLayout, false);
        TableLayout.LayoutParams paramsHeader = (TableLayout.LayoutParams) tableHeader.getLayoutParams();
        paramsHeader.setMargins(0, 0, 0, (int) getResources().getDisplayMetrics().density * 5);
        tableHeader.setLayoutParams(paramsHeader);
        tableHeader.setBackgroundColor(ContextCompat.getColor(this, R.color.mclaren_f1));

        TextView teamName = tableHeader.findViewById(R.id.team_name);
        teamName.setText("TEAM");
        teamName.setGravity(Gravity.CENTER);
        teamName.setTypeface(orbitronBold);
        teamName.setTextColor(ContextCompat.getColor(this, R.color.black));

        TextView year = tableHeader.findViewById(R.id.season_year);
        year.setText("YEAR");
        year.setGravity(Gravity.CENTER);
        year.setTypeface(orbitronBold);
        year.setTextColor(ContextCompat.getColor(this, R.color.black));

        TextView position = tableHeader.findViewById(R.id.driver_position);
        position.setText("POS");
        position.setGravity(Gravity.CENTER);
        position.setTypeface(orbitronBold);
        position.setTextColor(ContextCompat.getColor(this, R.color.black));

        TextView points = tableHeader.findViewById(R.id.driver_points);
        points.setText("PTS");
        points.setGravity(Gravity.CENTER);
        points.setTypeface(orbitronBold);
        points.setTextColor(ContextCompat.getColor(this, R.color.black));

        TextView wins = tableHeader.findViewById(R.id.driver_wins);
        wins.setText("W");
        wins.setGravity(Gravity.CENTER);
        wins.setTypeface(orbitronBold);
        wins.setTextColor(ContextCompat.getColor(this, R.color.black));

        TextView podiums = tableHeader.findViewById(R.id.driver_podiums);
        podiums.setText("POD");
        podiums.setGravity(Gravity.CENTER);
        podiums.setTypeface(orbitronBold);
        podiums.setTextColor(ContextCompat.getColor(this, R.color.black));

        //set stroke of tableHeader
        tableLayout.addView(tableHeader);


        for (int i = 0; i < 10; i++) {
            View tableRow = inflater.inflate(R.layout.driver_bio_table_row, tableLayout, false);
            tableRow.setBackgroundColor(ContextCompat.getColor(this, R.color.mclaren_f1_light));
            // Customize the row if needed
            TextView seasonYear, teamNameText, driverPosition, driverPoints, driverWins, driverPodiums;
            seasonYear = tableRow.findViewById(R.id.season_year);
            seasonYear.setTypeface(orbitronRegular);

            teamNameText = tableRow.findViewById(R.id.team_name);
            teamNameText.setTypeface(orbitronRegular);
            driverPosition = tableRow.findViewById(R.id.driver_position);
            driverPosition.setTypeface(orbitronRegular);

            driverPoints = tableRow.findViewById(R.id.driver_points);
            driverPoints.setTypeface(orbitronRegular);

            driverWins = tableRow.findViewById(R.id.driver_wins);
            driverWins.setTypeface(orbitronRegular);

            driverPodiums = tableRow.findViewById(R.id.driver_podiums);
            driverPodiums.setTypeface(orbitronRegular);


            // Set bottom margin to 5dp
            TableLayout.LayoutParams params = (TableLayout.LayoutParams) tableRow.getLayoutParams();
            params.setMargins(0, 0, 0, (int) getResources().getDisplayMetrics().density * 5);
            tableRow.setLayoutParams(params);


            tableLayout.addView(tableRow);


        }
    }
}