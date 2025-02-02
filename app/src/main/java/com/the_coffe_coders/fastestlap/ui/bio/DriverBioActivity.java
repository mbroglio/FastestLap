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
import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.driver.DriverHistory;
import com.the_coffe_coders.fastestlap.domain.nation.Nation;
import com.the_coffe_coders.fastestlap.repository.constructor.CommonConstructorRepository;
import com.the_coffe_coders.fastestlap.repository.driver.CommonDriverRepository;
import com.the_coffe_coders.fastestlap.repository.nation.FirebaseNationRepository;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.DriverViewModel;
import com.the_coffe_coders.fastestlap.ui.standing.DriversStandingActivity;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.UIUtils;

import org.threeten.bp.LocalDate;
import org.threeten.bp.Period;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.concurrent.Future;

/*
 * TODO:
 * - Implement firebase to get the data from the remote database
 */

public class DriverBioActivity extends AppCompatActivity {

    private final String TAG = "DriverBioActivity";
    LoadingScreen loadingScreen;
    private GestureDetector tapDetector;
    private Driver driver;
    private Nation nation;
    private Constructor team;
    private MaterialCardView teamLogoCard;
    private ImageView teamLogoImage;
    private MaterialCardView driverRank;
    private MaterialCardView driverNumberCard;
    private MaterialToolbar toolbar;
    private AppBarLayout appBarLayout;
    private ImageView driverNumberImage;

    private DriverViewModel driverViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        UIUtils.hideSystemUI(this);
        setContentView(R.layout.activity_driver_bio);

        loadingScreen = new LoadingScreen(getWindow().getDecorView(), this);
        loadingScreen.showLoadingScreen();

        String driverId = getIntent().getStringExtra("DRIVER_ID");
        Log.i("DriverBioActivity", "Driver ID: " + driverId);

        tapDetector = UIUtils.createTapDetector(this);

        toolbar = findViewById(R.id.topAppBar);
        appBarLayout = findViewById(R.id.top_bar_layout);

        UIUtils.applyWindowInsets(toolbar);

        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        teamLogoCard = findViewById(R.id.team_logo_card);
        teamLogoImage = findViewById(R.id.team_logo_image);

        driverNumberCard = findViewById(R.id.driver_number_card);
        driverNumberImage = findViewById(R.id.driver_number_image);

        createDriverBioPage(driverId);


    }

    public void createDriverBioPage(String driverId) {
        CommonDriverRepository commonDriverRepository = new CommonDriverRepository();


        MutableLiveData<Result> driverMutableLiveData = commonDriverRepository.getDriver(driverId);

        driverMutableLiveData.observe(this, result -> {
            if (result.isSuccess()) {
                driver = ((Result.DriverSuccess) result).getData();
                Log.i(TAG, "DRIVER SUCCESS");
                getTeamInfo(driver.getTeam_id());
            } else {
                Log.i(TAG, "DRIVER ERROR");
            }
        });
    }

    public void getTeamInfo(String teamId) {
        CommonConstructorRepository commonConstructorRepository = new CommonConstructorRepository();
        MutableLiveData<Result> constructorMutableLiveData = commonConstructorRepository.getConstructor(teamId);

        constructorMutableLiveData.observe(this, result -> {
            if (result.isSuccess()) {
                team = ((Result.ConstructorSuccess) result).getData();
                Log.i(TAG, "GET CONSTRUCTOR FROM COMMON REPO: " + team.toString());
                getNationInfo(driver.getNationality());
            }else {
                Log.i(TAG, "GET CONSTRUCTOR FROM COMMON REPO ERROR");
            }
        });
    }

    public void getNationInfo(String nationId) {
        FirebaseNationRepository firebaseNationRepository = new FirebaseNationRepository();
        MutableLiveData<Result> nationMutableLiveData = new MutableLiveData<>();
        nationMutableLiveData = firebaseNationRepository.getNation(nationId);

        nationMutableLiveData.observe(this, result -> {
            if (result.isSuccess()) {
                nation = ((Result.NationSuccess) result).getData();
                Log.i(TAG, "GET NATION FROM FIREBASE REPO: " + nation);
                setDriverData(driver, nation, team);
                setToolbar();
            }
        });
    }

    public void setToolbar() {
        String fullName = driver.getGivenName() + " " + driver.getFamilyName();

        toolbar.setTitle(fullName.toUpperCase());
        toolbar.setBackgroundColor(ContextCompat.getColor(this, Constants.TEAM_COLOR.get(driver.getTeam_id())));
        appBarLayout.setBackgroundColor(ContextCompat.getColor(this, Constants.TEAM_COLOR.get(driver.getTeam_id())));

        //setDriverRankingButton(driverId);

        teamLogoCard.setOnClickListener(v -> {
            Intent intent = new Intent(DriverBioActivity.this, ConstructorBioActivity.class);
            intent.putExtra("TEAM_ID", driver.getTeam_id());
            startActivity(intent);
        });
    }

    private void setDriverData(Driver driver, Nation nation, Constructor team) {
        teamLogoCard.setStrokeColor(ContextCompat.getColor(this, Constants.TEAM_COLOR.get(driver.getTeam_id())));
        driverNumberCard.setStrokeColor(ContextCompat.getColor(this, Constants.TEAM_COLOR.get(driver.getTeam_id())));

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
        age.setText(driver.getDriverAgeAsString());

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