package com.the_coffe_coders.fastestlap.ui.bio;

import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_DRIVERS_COLLECTION;
import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_NATIONS_COLLECTION;
import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_REALTIME_DATABASE;
import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_CONSTRUCTOR_COLLECTION;

import android.content.Intent;
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
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.constructor.ConstructorHistory;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.nation.Nation;
import com.the_coffe_coders.fastestlap.repository.constructor.CommonConstructorRepository;
import com.the_coffe_coders.fastestlap.repository.driver.CommonDriverRepository;
import com.the_coffe_coders.fastestlap.repository.nation.FirebaseNationRepository;
import com.the_coffe_coders.fastestlap.ui.standing.ConstructorsStandingActivity;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.UIUtils;

/*
 * TODO:
 * - manage nullPointerException events
 */

public class ConstructorBioActivity extends AppCompatActivity {

    private static final String TAG = "ConstructorBioActivity";
    LoadingScreen loadingScreen;
    private GestureDetector tapDetector;
    private Constructor team;
    private Driver driverOne;
    private Driver driverTwo;
    private Nation nation;
    private MaterialCardView teamLogoCard;
    private ImageView teamLogoImage;
    private ImageView teamCarImage;
    private MaterialCardView driverOneCard;
    private ImageView driverOneImage;
    private MaterialCardView driverTwoCard;
    private ImageView driverTwoImage;
    private ImageView teamFlag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        UIUtils.hideSystemUI(this);
        setContentView(R.layout.activity_constructor_bio);

        loadingScreen = new LoadingScreen(getWindow().getDecorView(), this);
        loadingScreen.showLoadingScreen();

        tapDetector = UIUtils.createTapDetector(this);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        AppBarLayout appBarLayout = findViewById(R.id.top_bar_layout);

        UIUtils.applyWindowInsets(toolbar);

        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        String teamId = getIntent().getStringExtra("TEAM_ID");
        Log.i("ConstructorBioActivity", "Team ID: " + teamId);

        teamLogoCard = findViewById(R.id.team_logo_card);
        teamLogoImage = findViewById(R.id.team_logo_image);

        teamCarImage = findViewById(R.id.team_car_image);

        driverOneCard = findViewById(R.id.driver_1_card);
        driverOneImage = findViewById(R.id.driver_1_image);
        driverTwoCard = findViewById(R.id.driver_2_card);
        driverTwoImage = findViewById(R.id.driver_2_image);

        teamFlag = findViewById(R.id.team_flag);
        CommonConstructorRepository commonConstructorRepository = new CommonConstructorRepository();
        MutableLiveData<Result> data = commonConstructorRepository.getConstructor(teamId);
        data.observe(this, result -> {
            if(result.isSuccess()) {
                team = ((Result.ConstructorSuccess) result).getData();
                String teamName = team.getName();

                toolbar.setTitle(teamName.toUpperCase());
                toolbar.setBackgroundColor(ContextCompat.getColor(this, Constants.TEAM_COLOR.get(teamId)));
                appBarLayout.setBackgroundColor(ContextCompat.getColor(this, Constants.TEAM_COLOR.get(teamId)));

                teamLogoCard.setStrokeColor(ContextCompat.getColor(this, Constants.TEAM_COLOR.get(teamId)));
                driverOneCard.setCardBackgroundColor(ContextCompat.getColor(this, Constants.TEAM_COLOR.get(teamId)));
                driverTwoCard.setCardBackgroundColor(ContextCompat.getColor(this, Constants.TEAM_COLOR.get(teamId)));

                getDriverData(team.getDriverOneId());
            }
        });
    }

    public void getDriverData(String driverId) {
        CommonDriverRepository commonDriverRepository = new CommonDriverRepository();
        MutableLiveData<Result> data = commonDriverRepository.getDriver(driverId);

        data.observe(this, result -> {
            if(result.isSuccess()) {
                if(driverId.equals(team.getDriverOneId())) {
                    driverOne = ((Result.DriverSuccess) result).getData();
                    driverOneCard.setOnClickListener(v -> {
                        Intent intent = new Intent(ConstructorBioActivity.this, DriverBioActivity.class);
                        intent.putExtra("DRIVER_ID", team.getDrivers().get(0));
                        startActivity(intent);
                    });
                    getDriverData(team.getDriverTwoId());

                } else {
                    driverTwo = ((Result.DriverSuccess) result).getData();
                    driverTwoCard.setOnClickListener(v -> {
                        Intent intent = new Intent(ConstructorBioActivity.this, DriverBioActivity.class);
                        intent.putExtra("DRIVER_ID", team.getDrivers().get(1));
                        startActivity(intent);
                    });
                    getNationData(team.getNationality());
                }
            }
        });
    }

    public void getNationData(String nationId) {
        FirebaseNationRepository firebaseNationRepository = new FirebaseNationRepository();
        MutableLiveData<Result> data = firebaseNationRepository.getNation(nationId);

        data.observe(this, result -> {
            if(result.isSuccess()) {
                nation = ((Result.NationSuccess) result).getData();
                setTeamData(team, nation, driverOne, driverTwo);
            }
        });
    }

    private void setTeamData(Constructor team, Nation nation, Driver driverOne, Driver driverTwo) {
        Glide.with(this).load(team.getTeam_logo_url()).into(teamLogoImage);
        Glide.with(this).load(nation.getNation_flag_url()).into(teamFlag);
        Glide.with(this).load(team.getCar_pic_url()).into(teamCarImage);
        Glide.with(this).load(driverOne.getDriver_pic_url()).into(driverOneImage);
        Glide.with(this).load(driverTwo.getDriver_pic_url()).into(driverTwoImage);

        TextView driverOneName = findViewById(R.id.driver_1_name);
        driverOneName.setText(driverOne.getGivenName() + " " + driverOne.getFamilyName());

        TextView driverTwoName = findViewById(R.id.driver_2_name);
        driverTwoName.setText(driverTwo.getGivenName() + " " + driverTwo.getFamilyName());

        //Team Data
        TextView teamFullName = findViewById(R.id.team_full_name_value);
        teamFullName.setText(team.getFull_name());

        TextView teamHq = findViewById(R.id.team_base_value);
        teamHq.setText(team.getHq());

        TextView teamPrincipal = findViewById(R.id.team_principal_value);
        teamPrincipal.setText(team.getTeam_principal());

        TextView teamChassis = findViewById(R.id.team_chassis_value);
        teamChassis.setText(team.getChassis());

        TextView teamPowerUnit = findViewById(R.id.team_power_unit_value);
        teamPowerUnit.setText(team.getPower_unit());

        TextView teamFirstEntry = findViewById(R.id.team_first_entry_value);
        teamFirstEntry.setText(team.getFirst_entry());

        TextView championships = findViewById(R.id.team_championships_value);
        championships.setText(team.getWorld_championships());

        TextView wins = findViewById(R.id.team_wins_value);
        wins.setText(team.getWins());

        TextView podiums = findViewById(R.id.team_podiums_value);
        podiums.setText(team.getPodiums());

        createHistoryTable();

    }

    private void createHistoryTable() {
        TableLayout tableLayout = findViewById(R.id.history_table);
        LayoutInflater inflater = LayoutInflater.from(this);

        View tableHeader = inflater.inflate(R.layout.constructor_bio_table_header, tableLayout, false);
        TableLayout.LayoutParams paramsHeader = (TableLayout.LayoutParams) tableHeader.getLayoutParams();
        paramsHeader.setMargins(0, 0, 0, (int) getResources().getDisplayMetrics().density * 5);
        tableHeader.setLayoutParams(paramsHeader);
        tableHeader.setBackgroundColor(ContextCompat.getColor(this, R.color.timer_gray_dark));

        //set stroke of tableHeader
        tableLayout.addView(tableHeader);

        for (ConstructorHistory history : team.getTeam_history()) {
            View tableRow = inflater.inflate(R.layout.constructor_bio_table_row, tableLayout, false);
            tableRow.setBackgroundColor(ContextCompat.getColor(this, R.color.timer_gray));
            // Customize the row if needed
            TextView seasonYear, teamPosition, teamPoints, teamWins, teamPodiums;
            seasonYear = tableRow.findViewById(R.id.season_year);
            seasonYear.setText(history.getYear());

            teamPosition = tableRow.findViewById(R.id.team_position);
            teamPosition.setText(history.getPosition());

            teamPoints = tableRow.findViewById(R.id.team_points);
            teamPoints.setText(history.getPoints());

            teamWins = tableRow.findViewById(R.id.team_wins);
            teamWins.setText(history.getWins());

            teamPodiums = tableRow.findViewById(R.id.team_podiums);
            teamPodiums.setText(history.getPodiums());

            // Set bottom margin to 5dp
            TableLayout.LayoutParams tableParams = (TableLayout.LayoutParams) tableRow.getLayoutParams();
            tableParams.setMargins(0, 0, 0, (int) getResources().getDisplayMetrics().density * 5);
            tableRow.setLayoutParams(tableParams);

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