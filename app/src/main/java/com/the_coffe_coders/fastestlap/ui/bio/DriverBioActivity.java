package com.the_coffe_coders.fastestlap.ui.bio;

import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_DRIVERS_COLLECTION;
import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_NATIONS_COLLECTION;
import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_REALTIME_DATABASE;
import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_TEAMS_COLLECTION;

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

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.driver.DriverHistory;
import com.the_coffe_coders.fastestlap.domain.nation.Nation;
import com.the_coffe_coders.fastestlap.ui.standing.DriversStandingActivity;
import com.the_coffe_coders.fastestlap.util.Constants;

import org.threeten.bp.LocalDate;
import org.threeten.bp.Period;
import org.threeten.bp.format.DateTimeFormatter;

/*
 * TODO:
 * - Implement firebase to get the data from the remote database
 */

public class DriverBioActivity extends AppCompatActivity {
    private Driver driver;
    private Nation nation;
    private Constructor team;

    private MaterialCardView teamLogoCard;
    private ImageView teamLogoImage;

    private MaterialCardView driverRank;

    private MaterialCardView driverNumberCard;
    private ImageView driverNumberImage;

    public static String calculateAge(String dateOfBirth) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate birthDate = LocalDate.parse(dateOfBirth, formatter);
        LocalDate currentDate = LocalDate.now();
        return Integer.toString(Period.between(birthDate, currentDate).getYears());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_driver_bio);

        String driverId = getIntent().getStringExtra("DRIVER_ID");
        Log.i("DriverBioActivity", "Driver ID: " + driverId);


        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        AppBarLayout appBarLayout = findViewById(R.id.top_bar_layout);

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

        teamLogoCard = findViewById(R.id.team_logo_card);
        teamLogoImage = findViewById(R.id.team_logo_image);

        driverNumberCard = findViewById(R.id.driver_number_card);
        driverNumberImage = findViewById(R.id.driver_number_image);

        ViewGroup.LayoutParams params = teamLogoCard.getLayoutParams();
        params.width = width_percent_60;
        teamLogoCard.setLayoutParams(params);

        //set listener to driver rank and define a method onclick
        driverRank = findViewById(R.id.driver_current_standing);
        driverRank.setOnClickListener(v -> {
            Intent intent = new Intent(DriverBioActivity.this, DriversStandingActivity.class);
            intent.putExtra("DRIVER_ID", driverId);
            startActivity(intent);
        });

        DatabaseReference databaseReference = FirebaseDatabase.getInstance(FIREBASE_REALTIME_DATABASE).getReference(FIREBASE_DRIVERS_COLLECTION).child(driverId);
        Log.i("DriverBioActivity", "Database reference: " + databaseReference.toString());
        databaseReference.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                driver = task.getResult().getValue(Driver.class);
                Log.i("DriverBioActivity", "Driver data: " + driver.toStringDB());
                String fullName = driver.getGivenName() + " " + driver.getFamilyName();

                toolbar.setTitle(fullName.toUpperCase());
                toolbar.setBackgroundColor(ContextCompat.getColor(this, Constants.TEAM_COLOR.get(driver.getTeam_id())));
                appBarLayout.setBackgroundColor(ContextCompat.getColor(this, Constants.TEAM_COLOR.get(driver.getTeam_id())));

                teamLogoCard.setStrokeColor(ContextCompat.getColor(this, Constants.TEAM_COLOR.get(driver.getTeam_id())));
                driverNumberCard.setStrokeColor(ContextCompat.getColor(this, Constants.TEAM_COLOR.get(driver.getTeam_id())));

                teamLogoCard.setOnClickListener(v -> {
                    Intent intent = new Intent(DriverBioActivity.this, ConstructorBioActivity.class);
                    intent.putExtra("TEAM NAME", driver.getTeam_id());
                    startActivity(intent);
                });

                DatabaseReference nationReference = FirebaseDatabase.getInstance(FIREBASE_REALTIME_DATABASE).getReference(FIREBASE_NATIONS_COLLECTION).child(driver.getNationality());
                Log.i("DriverBioActivity", "Nation reference: " + nationReference.toString());
                nationReference.get().addOnCompleteListener(nationTask -> {
                    if (nationTask.isSuccessful()) {
                        nation = nationTask.getResult().getValue(Nation.class);
                        Log.i("DriverBioActivity", "Nation data: " + nation.toString());

                        DatabaseReference teamReference = FirebaseDatabase.getInstance(FIREBASE_REALTIME_DATABASE).getReference(FIREBASE_TEAMS_COLLECTION).child(driver.getTeam_id());
                        Log.i("DriverBioActivity", "Team reference: " + teamReference.toString());
                        teamReference.get().addOnCompleteListener(teamTask -> {
                            if (teamTask.isSuccessful()) {
                                team = teamTask.getResult().getValue(Constructor.class);
                                Log.i("DriverBioActivity", "Team data: " + team.toString());
                                setDriverData(driver, nation, team);
                            } else {
                                Log.e("DriverBioActivity", "Error getting team data", teamTask.getException());
                            }
                        });
                    } else {
                        Log.e("DriverBioActivity", "Error getting nation data", nationTask.getException());
                    }
                });
            } else {
                Log.e("DriverBioActivity", "Error getting driver data", task.getException());
            }
        });
    }

    private void setDriverData(Driver driver, Nation nation, Constructor team) {
        Glide.with(this).load(team.getTeam_logo_url()).into(teamLogoImage);

        ImageView driverFlag = findViewById(R.id.driver_flag);
        Glide.with(this).load(nation.getNation_flag_url()).into(driverFlag);

        ImageView driverPic = findViewById(R.id.driver_bio_pic);
        Glide.with(this).load(driver.getDriver_pic_url()).into(driverPic);

        TextView birthplace = findViewById(R.id.driver_birthplace);
        birthplace.setText(driver.getBirth_place());

        TextView birthdate = findViewById(R.id.driver_birthdate);
        birthdate.setText(driver.getDateOfBirth());

        TextView age = findViewById(R.id.driver_age);
        age.setText(calculateAge(driver.getDateOfBirth()));

        TextView weight = findViewById(R.id.driver_weight);
        weight.setText(driver.getWeight());

        TextView height = findViewById(R.id.driver_height);
        height.setText(driver.getHeight());

        TextView bestResult = findViewById(R.id.driver_best_result);
        bestResult.setText(driver.getBest_result());

        TextView championships = findViewById(R.id.driver_championships);
        championships.setText(driver.getChampionships());

        Glide.with(this).load(driver.getRacing_number_pic_url()).into(driverNumberImage);

        createHistoryTable();
    }

    private void createHistoryTable() {
        TableLayout tableLayout = findViewById(R.id.history_table);
        LayoutInflater inflater = LayoutInflater.from(this);

        View tableHeader = inflater.inflate(R.layout.driver_bio_table_header, tableLayout, false);
        TableLayout.LayoutParams paramsHeader = (TableLayout.LayoutParams) tableHeader.getLayoutParams();
        paramsHeader.setMargins(0, 0, 0, (int) getResources().getDisplayMetrics().density * 5);
        tableHeader.setLayoutParams(paramsHeader);
        tableHeader.setBackgroundColor(ContextCompat.getColor(this, R.color.timer_gray_dark));

        //set stroke of tableHeader
        tableLayout.addView(tableHeader);

        for (DriverHistory history : driver.getDriver_history()) {
            View tableRow = inflater.inflate(R.layout.driver_bio_table_row, tableLayout, false);
            tableRow.setBackgroundColor(ContextCompat.getColor(this, R.color.timer_gray));
            // Customize the row if needed
            TextView seasonYear, teamNameText, driverPosition, driverPoints, driverWins, driverPodiums;
            seasonYear = tableRow.findViewById(R.id.season_year);
            seasonYear.setText(history.getYear());

            teamNameText = tableRow.findViewById(R.id.team_name);
            teamNameText.setText(history.getTeam());

            driverPosition = tableRow.findViewById(R.id.driver_position);
            driverPosition.setText(history.getPosition());

            driverPoints = tableRow.findViewById(R.id.driver_points);
            driverPoints.setText(history.getPoints());

            driverWins = tableRow.findViewById(R.id.driver_wins);
            driverWins.setText(history.getWins());

            driverPodiums = tableRow.findViewById(R.id.driver_podiums);
            driverPodiums.setText(history.getPodiums());

            // Set bottom margin to 5dp
            TableLayout.LayoutParams tableParams = (TableLayout.LayoutParams) tableRow.getLayoutParams();
            tableParams.setMargins(0, 0, 0, (int) getResources().getDisplayMetrics().density * 5);
            tableRow.setLayoutParams(tableParams);


            tableLayout.addView(tableRow);
        }
    }
}