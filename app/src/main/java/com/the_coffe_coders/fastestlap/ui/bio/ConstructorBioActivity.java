package com.the_coffe_coders.fastestlap.ui.bio;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.constructor.ConstructorHistory;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.nation.Nation;
import com.the_coffe_coders.fastestlap.domain.user.User;
import com.the_coffe_coders.fastestlap.repository.user.IUserRepository;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.ConstructorViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.ConstructorViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.DriverViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.DriverViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.NationViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.NationViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.welcome.viewmodel.UserViewModel;
import com.the_coffe_coders.fastestlap.ui.welcome.viewmodel.UserViewModelFactory;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;
import com.the_coffe_coders.fastestlap.util.SharedPreferencesUtils;
import com.the_coffe_coders.fastestlap.util.UIUtils;

/*
 * TODO:
 *  - manage nullPointerExceptions
 */

public class ConstructorBioActivity extends AppCompatActivity {

    private static final String TAG = "ConstructorBioActivity";
    LoadingScreen loadingScreen;
    MaterialToolbar toolbar;
    AppBarLayout appBarLayout;
    private DriverViewModel driverViewModel;
    private NationViewModel nationViewModel;
    private ConstructorViewModel constructorViewModel;

    private Constructor constructor;
    private Nation nation;
    private Driver driverOne;
    private Driver driverTwo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_constructor_bio);

        loadingScreen = new LoadingScreen(getWindow().getDecorView(), this);
        loadingScreen.showLoadingScreen();

        toolbar = findViewById(R.id.topAppBar);
        appBarLayout = findViewById(R.id.top_bar_layout);

        UIUtils.applyWindowInsets(toolbar);

        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        ScrollView scrollView = findViewById(R.id.constructor_bio_scroll);
        UIUtils.applyWindowInsets(scrollView);

        String teamId = getIntent().getStringExtra("TEAM_ID");
        Log.i("ConstructorBioActivity", "Team ID: " + teamId);

        driverViewModel = new ViewModelProvider(this, new DriverViewModelFactory()).get(DriverViewModel.class);
        constructorViewModel = new ViewModelProvider(this, new ConstructorViewModelFactory()).get(ConstructorViewModel.class);
        nationViewModel = new ViewModelProvider(this, new NationViewModelFactory()).get(NationViewModel.class);

        createConstructorBioPage(teamId);

        Menu menu = toolbar.getMenu();
        MenuItem item = menu.findItem(R.id.favourite_icon_outline);
        item.setOnMenuItemClickListener(v -> {
            updateFavouriteConstructor(teamId);
            item.setIcon(R.drawable.star_fav);
            return false;
        });
    }

    private void updateFavouriteConstructor(String teamId) {
        IUserRepository userRepository = ServiceLocator.getInstance().getUserRepository(getApplication());
        UserViewModel userViewModel = new ViewModelProvider(getViewModelStore(), new UserViewModelFactory(userRepository)).get(UserViewModel.class);
        User currentUser = userViewModel.getLoggedUser();

        SharedPreferencesUtils sharedPreferencesUtils = new SharedPreferencesUtils(this);
        sharedPreferencesUtils.writeStringData(Constants.SHARED_PREFERENCES_FILENAME,
                Constants.SHARED_PREFERENCES_FAVORITE_TEAM,
                teamId);

        userViewModel.saveUserConstructorPreferences(teamId, currentUser.getIdToken());

        Log.i(TAG, "Favourite constructor updated: " + teamId);
    }

    private void createConstructorBioPage(String teamId) {
        MutableLiveData<Result> data = constructorViewModel.getSelectedConstructorLiveData(teamId);
        data.observe(this, result -> {
            if (result.isSuccess()) {
                constructor = ((Result.ConstructorSuccess) result).getData();
                String teamName = constructor.getName();

                toolbar.setTitle(teamName.toUpperCase());
                toolbar.setBackgroundColor(ContextCompat.getColor(this, Constants.TEAM_COLOR.get(teamId)));
                appBarLayout.setBackgroundColor(ContextCompat.getColor(this, Constants.TEAM_COLOR.get(teamId)));

                MaterialCardView teamLogoCard = findViewById(R.id.team_logo_card);
                teamLogoCard.setStrokeColor(ContextCompat.getColor(this, Constants.TEAM_COLOR.get(teamId)));

                MaterialCardView driverCard = findViewById(R.id.driver_1_card);
                driverCard.setCardBackgroundColor(ContextCompat.getColor(this, Constants.TEAM_COLOR.get(teamId)));

                driverCard = findViewById(R.id.driver_2_card);
                driverCard.setCardBackgroundColor(ContextCompat.getColor(this, Constants.TEAM_COLOR.get(teamId)));

                getDriverData(constructor.getDriverOneId(), constructor);
            }
        });
    }

    public void getDriverData(String driverId, Constructor team) {
        Log.i("ConstructorBioActivity", "Getting driver data");
        MutableLiveData<Result> data = driverViewModel.getDriver(driverId);

        data.observe(this, result -> {
            if (result.isSuccess()) {
                if (driverId.equals(team.getDriverOneId())) {
                    driverOne = ((Result.DriverSuccess) result).getData();

                    MaterialCardView driverOneCard = findViewById(R.id.driver_1_card);
                    driverOneCard.setOnClickListener(v -> {
                        Intent intent = new Intent(ConstructorBioActivity.this, DriverBioActivity.class);
                        intent.putExtra("DRIVER_ID", driverId);
                        startActivity(intent);
                    });
                    getDriverData(team.getDriverTwoId(), team);

                } else {
                    driverTwo = ((Result.DriverSuccess) result).getData();

                    MaterialCardView driverTwoCard = findViewById(R.id.driver_2_card);
                    driverTwoCard.setOnClickListener(v -> {
                        Intent intent = new Intent(ConstructorBioActivity.this, DriverBioActivity.class);
                        intent.putExtra("DRIVER_ID", driverId);
                        startActivity(intent);
                    });

                    getNationData(team.getNationality());
                }
            }
        });
    }

    public void getNationData(String nationId) {
        Log.i("ConstructorBioActivity", "Getting nation data");
        MutableLiveData<Result> data = nationViewModel.getNation(nationId);

        data.observe(this, result -> {
            if (result.isSuccess()) {
                nation = ((Result.NationSuccess) result).getData();
                setTeamData(constructor, nation, driverOne, driverTwo);
            }
        });

        loadingScreen.hideLoadingScreen();
    }

    private void setTeamData(Constructor team, Nation nation, Driver driverOne, Driver driverTwo) {
        Log.i("ConstructorBioActivity", "Setting team data");

        ImageView teamLogoImage = findViewById(R.id.team_logo_image);
        Glide.with(this).load(team.getTeam_logo_url()).into(teamLogoImage);

        ImageView teamFlag = findViewById(R.id.team_flag);
        Glide.with(this).load(nation.getNation_flag_url()).into(teamFlag);

        ImageView teamCarImage = findViewById(R.id.team_car_image);
        Glide.with(this).load(team.getCar_pic_url()).into(teamCarImage);

        ImageView driverOneImage = findViewById(R.id.driver_1_image);
        Glide.with(this).load(driverOne.getDriver_pic_url()).into(driverOneImage);

        ImageView driverTwoImage = findViewById(R.id.driver_2_image);
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
        Log.i("ConstructorBioActivity", "Creating history table");
        TableLayout tableLayout = findViewById(R.id.history_table);
        tableLayout.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        View tableHeader = inflater.inflate(R.layout.constructor_bio_table_header, tableLayout, false);
        TableLayout.LayoutParams paramsHeader = (TableLayout.LayoutParams) tableHeader.getLayoutParams();
        paramsHeader.setMargins(0, 0, 0, (int) getResources().getDisplayMetrics().density * 5);
        tableHeader.setLayoutParams(paramsHeader);
        tableHeader.setBackgroundColor(ContextCompat.getColor(this, R.color.timer_gray_dark));

        //set stroke of tableHeader
        tableLayout.addView(tableHeader);
        for (ConstructorHistory history : constructor.getTeam_history()) {
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
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}