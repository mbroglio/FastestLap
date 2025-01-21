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

        DatabaseReference databaseReference = FirebaseDatabase.getInstance(FIREBASE_REALTIME_DATABASE).getReference(FIREBASE_DRIVERS_COLLECTION).child(driverId);
        Log.i("DriverBioActivity", "Database reference: " + databaseReference.toString());
        databaseReference.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                driver = task.getResult().getValue(Driver.class);
                Log.i("DriverBioActivity", "Driver data: " + driver.toStringDB());
                toolbar.setTitle(driverId);

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
            Intent intent = new Intent(DriverBioActivity.this, DriversStandingActivity.class);
            intent.putExtra("DRIVER_ID", driverId);
            startActivity(intent);
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

    private void setDriverData(Driver driver, Nation nation, Constructor team) {
        ImageView teamLogo = findViewById(R.id.team_logo_image);
        Glide.with(this).load(team.getTeam_logo_url()).into(teamLogo);

        ImageView driverFlag = findViewById(R.id.driver_flag);
        Glide.with(this).load(nation.getNation_flag_url()).into(driverFlag);

        ImageView driverPic = findViewById(R.id.driver_bio_pic);
        Glide.with(this).load(driver.getDriver_pic_url()).into(driverPic);

        TextView birthplace = findViewById(R.id.driver_birthplace);
        birthplace.setText(driver.getBirth_place());

        TextView birthdate = findViewById(R.id.driver_birthdate);
        birthdate.setText(driver.getDateOfBirth());

        TextView age = findViewById(R.id.driver_age);
        //age.setText(calculateAge(driver.getDateOfBirth()));

        TextView weight = findViewById(R.id.driver_weight);
        weight.setText(driver.getWeight() + " kg");

        TextView height = findViewById(R.id.driver_height);
        height.setText(driver.getHeight()+ " m");

        TextView bestResult = findViewById(R.id.driver_best_result);
        bestResult.setText(driver.getBest_result());

        TextView championships = findViewById(R.id.driver_championships);
        championships.setText(driver.getChampionships());

        ImageView racingNumber = findViewById(R.id.driver_number);
        Glide.with(this).load(driver.getRacing_number_pic_url()).into(racingNumber);
    }

    public static int calculateAge(String dateOfBirth) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        LocalDate birthDate = LocalDate.parse(dateOfBirth, formatter);
        LocalDate currentDate = LocalDate.now();
        return Period.between(birthDate, currentDate).getYears();
    }
}