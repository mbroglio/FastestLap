package com.the_coffe_coders.fastestlap.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.repository.user.IUserRepository;
import com.the_coffe_coders.fastestlap.ui.profile.ProfileActivity;
import com.the_coffe_coders.fastestlap.ui.standing.ConstructorsStandingActivity;
import com.the_coffe_coders.fastestlap.ui.standing.DriversStandingActivity;
import com.the_coffe_coders.fastestlap.ui.welcome.viewmodel.UserViewModel;
import com.the_coffe_coders.fastestlap.ui.welcome.viewmodel.UserViewModelFactory;
import com.the_coffe_coders.fastestlap.util.NetworkUtils;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import org.threeten.bp.ZoneId;

import java.util.Objects;

public class HomePageActivity extends AppCompatActivity {
    private final String TAG = "HomePageActivity";
    private final ZoneId localZone = ZoneId.systemDefault();
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_home);

        setToolbar();

        setNavigationBar();

        getUserPreferences();
    }

    private void setToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.top_app_bar);
        UIUtils.applyWindowInsets(toolbar);

        setSupportActionBar(toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.userActivity) {
                Intent intent = new Intent(HomePageActivity.this, ProfileActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }

    private void setNavigationBar() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView);
        NavController navController = navHostFragment.getNavController();

        bottomNavigationView = findViewById(R.id.navbar);
        UIUtils.applyWindowInsets(bottomNavigationView);

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(R.id.homeFragment, R.id.standingsFragment, R.id.racingFragment, R.id.newsFragment, R.id.juniorCategoriesFragment).build();

        NavigationUI.setupWithNavController(bottomNavigationView, navController);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
    }

    private void getUserPreferences() {
        IUserRepository userRepository = ServiceLocator.getInstance().getUserRepository(getApplication());
        UserViewModel userViewModel = new ViewModelProvider(getViewModelStore(), new UserViewModelFactory(userRepository)).get(UserViewModel.class);
        String idToken = userViewModel.getLoggedUser().getIdToken();

        userViewModel.getUserPreferences(idToken).observe(this, result -> {
            if (result.isSuccess()) {
                Intent intent = getIntent();
                if (intent != null && intent.hasExtra("CALLER")) {
                    String caller = intent.getStringExtra("CALLER");
                    switch (Objects.requireNonNull(caller)) {
                        case "ConstructorsStandingActivity":
                        case "DriversStandingActivity":
                            bottomNavigationView.post(() -> bottomNavigationView.setSelectedItemId(R.id.standingsFragment));
                            break;
                        case "WelcomeActivity":
                        case "HomeFragment":
                            Intent home = new Intent(HomePageActivity.this, HomePageActivity.class);
                            home.putExtra("RELOADED", "true");
                            startActivity(home);
                            break;
                        case "ConstructorBioActivity":
                            Intent constructorStanding = new Intent(HomePageActivity.this, ConstructorsStandingActivity.class);
                            startActivity(constructorStanding);
                            break;
                        case "DriverBioActivity":
                            Intent driverStanding = new Intent(HomePageActivity.this, DriversStandingActivity.class);
                            startActivity(driverStanding);
                            break;
                    }
                }
            } else {
                Log.e(TAG, "Error getting user preferences: " + result.getError());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.top_app_bar_menu, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

}