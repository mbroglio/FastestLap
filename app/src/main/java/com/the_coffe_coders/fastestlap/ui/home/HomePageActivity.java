package com.the_coffe_coders.fastestlap.ui.home;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.constructor.ConstructorStanding;
import com.the_coffe_coders.fastestlap.domain.driver.DriverStanding;
import com.the_coffe_coders.fastestlap.domain.driver.StandingsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.race_result.Race;
import com.the_coffe_coders.fastestlap.domain.race_result.ResultsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.race_week.FirstPractice;
import com.the_coffe_coders.fastestlap.domain.race_week.Qualifying;
import com.the_coffe_coders.fastestlap.domain.race_week.RaceWeekAPIresponse;
import com.the_coffe_coders.fastestlap.domain.race_week.Races;
import com.the_coffe_coders.fastestlap.domain.race_week.SecondPractice;
import com.the_coffe_coders.fastestlap.domain.race_week.Session;
import com.the_coffe_coders.fastestlap.domain.race_week.Sprint;
import com.the_coffe_coders.fastestlap.domain.race_week.SprintQualifying;
import com.the_coffe_coders.fastestlap.domain.race_week.ThirdPractice;
import com.the_coffe_coders.fastestlap.ui.ErgastAPI;
import com.the_coffe_coders.fastestlap.utils.Constants;
import com.the_coffe_coders.fastestlap.utils.JSONParserUtils;
import com.google.android.material.appbar.AppBarLayout;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class HomePageActivity extends AppCompatActivity {
    private final String TAG = "HomePageActivity";
    private ErgastAPI ergastAPI;
    private final ZoneId localZone = ZoneId.systemDefault();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        MaterialToolbar toolbar = findViewById(R.id.top_app_bar);
        setSupportActionBar(toolbar);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView);

        NavController navController = navHostFragment.getNavController();

        BottomNavigationView bottomNavigationView = findViewById(R.id.navbar);

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(R.id.homeFragment, R.id.standingsFragment, R.id.racingFragment).build();

        NavigationUI.setupWithNavController(bottomNavigationView, navController);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

    }

}