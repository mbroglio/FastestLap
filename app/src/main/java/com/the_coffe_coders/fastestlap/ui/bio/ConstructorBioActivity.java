package com.the_coffe_coders.fastestlap.ui.bio;

import android.content.Intent;
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

import android.graphics.Typeface;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.constructor.ConstructorStanding;
import com.the_coffe_coders.fastestlap.ui.event.EventActivity;
import com.the_coffe_coders.fastestlap.ui.event.UpcomingEventsActivity;
import com.the_coffe_coders.fastestlap.ui.standing.ConstructorsStandingActivity;
import org.w3c.dom.Text;

public class ConstructorBioActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_constructor_bio);


        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

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
        Typeface orbitronBold = ResourcesCompat.getFont(this, R.font.orbitron_bold);
        Typeface orbitronRegular = ResourcesCompat.getFont(this, R.font.orbitron_regular);

        View tableHeader = inflater.inflate(R.layout.constructor_bio_table_row, tableLayout, false);
        TableLayout.LayoutParams paramsHeader = (TableLayout.LayoutParams) tableHeader.getLayoutParams();
        paramsHeader.setMargins(0, 0, 0, (int) getResources().getDisplayMetrics().density * 5);
        tableHeader.setLayoutParams(paramsHeader);
        tableHeader.setBackgroundColor(ContextCompat.getColor(this, R.color.ferrari_f1));

        TextView year = tableHeader.findViewById(R.id.season_year);
        year.setText("YEAR");
        year.setGravity(Gravity.CENTER);
        year.setTextColor(ContextCompat.getColor(this, R.color.black));

        TextView position = tableHeader.findViewById(R.id.team_position);
        position.setText("POS");
        position.setGravity(Gravity.CENTER);
        position.setTextColor(ContextCompat.getColor(this, R.color.black));

        TextView points = tableHeader.findViewById(R.id.team_points);
        points.setText("PTS");
        points.setGravity(Gravity.CENTER);
        points.setTextColor(ContextCompat.getColor(this, R.color.black));

        TextView wins = tableHeader.findViewById(R.id.team_wins);
        wins.setText("WINS");
        wins.setGravity(Gravity.CENTER);
        wins.setTextColor(ContextCompat.getColor(this, R.color.black));

        TextView podiums = tableHeader.findViewById(R.id.team_podiums);
        podiums.setText("PODS");
        podiums.setGravity(Gravity.CENTER);
        podiums.setTextColor(ContextCompat.getColor(this, R.color.black));

        tableLayout.addView(tableHeader);




// Add data rows
        for (int i = 0; i < 10; i++) {
            View tableRow = inflater.inflate(R.layout.constructor_bio_table_row, tableLayout, false);
            // Customize the row if needed
            TextView seasonYear, teamPosition, teamPoints, teamWins, teamPodiums;
            seasonYear = tableRow.findViewById(R.id.season_year);
            seasonYear.setTypeface(orbitronRegular);

            teamPosition = tableRow.findViewById(R.id.team_position);
            teamPosition.setTypeface(orbitronRegular);

            teamPoints = tableRow.findViewById(R.id.team_points);
            teamPoints.setTypeface(orbitronRegular);

            teamWins = tableRow.findViewById(R.id.team_wins);
            teamWins.setTypeface(orbitronRegular);

            teamPodiums = tableRow.findViewById(R.id.team_podiums);
            teamPodiums.setTypeface(orbitronRegular);

            // Set bottom margin to 5dp
            TableLayout.LayoutParams params = (TableLayout.LayoutParams) tableRow.getLayoutParams();
            params.setMargins(0, 0, 0, (int) getResources().getDisplayMetrics().density * 5);
            tableRow.setLayoutParams(params);
            tableRow.setBackgroundColor(ContextCompat.getColor(this, R.color.ferrari_f1_light));

            tableLayout.addView(tableRow);


        }
    }
}