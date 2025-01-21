package com.the_coffe_coders.fastestlap.ui.bio;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.ui.standing.DriversStandingActivity;

/*
 * TODO:
 * - Implement firebase to get the data from the remote database
 */

public class DriverBioActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_driver_bio);

        String driverId = getIntent().getStringExtra("DRIVER_ID");
        Log.i("DriverBioActivity", "Driver ID: " + driverId);


        MaterialToolbar toolbar = findViewById(R.id.topAppBar);

        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.topMargin = systemBars.top;
            v.setLayoutParams(params);

            return insets;
        });

        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // Get the screen width
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;

        // Calculate 60% of the screen width
        int width_percent_60 = (int) (screenWidth * 0.6);


        //set listener to team logo and define a method onclick
        MaterialCardView teamLogo = findViewById(R.id.team_logo);
        ImageView teamLogoImage = findViewById(R.id.team_logo_image);
        String teamNameId = teamLogoImage.getContentDescription().toString();

        ViewGroup.LayoutParams params = teamLogo.getLayoutParams();
        params.width = width_percent_60;
        teamLogo.setLayoutParams(params);

        teamLogo.setOnClickListener(v -> {
            Intent intent = new Intent(DriverBioActivity.this, ConstructorBioActivity.class);
            intent.putExtra("TEAM NAME", teamNameId);
            startActivity(intent);
        });

        //set listener to driver rank and define a method onclick
        MaterialCardView driverRank = findViewById(R.id.driver_current_standing);
        driverRank.setOnClickListener(v -> {
            //Intent resultIntent = new Intent(DriverBioActivity.this, DriversStandingActivity.class);
            //startActivity(resultIntent);
            String callerActivity = getIntent().getStringExtra("CALLER");
            assert callerActivity != null;
            if (callerActivity.equals(DriversStandingActivity.class.getName())) {
                //Intent resultIntent = new Intent();
                //resultIntent.putExtra("DRIVER_ID", driverId);
                //setResult(RESULT_OK, resultIntent);
                finish();
            }else {
                Intent resultIntent = new Intent(DriverBioActivity.this, DriversStandingActivity.class);
                resultIntent.putExtra("DRIVER_ID", driverId);
                startActivity(resultIntent);
            }

        });


        //setting table
        TableLayout tableLayout = findViewById(R.id.history_table);
        LayoutInflater inflater = LayoutInflater.from(this);
        Typeface orbitronBold = ResourcesCompat.getFont(this, R.font.orbitron_bold);
        Typeface orbitronRegular = ResourcesCompat.getFont(this, R.font.orbitron_regular);

        View tableHeader = inflater.inflate(R.layout.driver_bio_table_header, tableLayout, false);
        TableLayout.LayoutParams paramsHeader = (TableLayout.LayoutParams) tableHeader.getLayoutParams();
        paramsHeader.setMargins(0, 0, 0, (int) getResources().getDisplayMetrics().density * 5);
        tableHeader.setLayoutParams(paramsHeader);
        tableHeader.setBackgroundColor(ContextCompat.getColor(this, R.color.mclaren_f1));

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
            TableLayout.LayoutParams tableParams = (TableLayout.LayoutParams) tableRow.getLayoutParams();
            tableParams.setMargins(0, 0, 0, (int) getResources().getDisplayMetrics().density * 5);
            tableRow.setLayoutParams(tableParams);


            tableLayout.addView(tableRow);


        }
    }
}