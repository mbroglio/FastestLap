package com.the_coffe_coders.fastestlap.ui.junior;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentContainerView;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

public class Formula3Activity extends AppCompatActivity {

    private NavController navController;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_formula3);
        setToolbarAndNavigation();
    }

    private void setToolbarAndNavigation() {
        toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        UIUtils.applyWindowInsets(toolbar);

        FragmentContainerView fragmentContainerView = findViewById(R.id.fragmentContainerView);
        UIUtils.applyWindowInsets(fragmentContainerView);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            toolbar.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.news_outline) {
                    if (navController.getCurrentDestination() != null
                            && navController.getCurrentDestination().getId() == R.id.junior_news) {
                        return true;
                    }
                    Bundle bundle = new Bundle();
                    bundle.putString("SERIES_ID", "f3");
                    bundle.putInt("CATEGORY_TYPE", 1);
                    navController.navigate(R.id.junior_news, bundle);
                    return true;
                }
                return false;
            });

            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                invalidateOptionsMenu();
                if (toolbar != null && toolbar.getMenu() != null) {
                    MenuItem newsItem = toolbar.getMenu().findItem(R.id.news_outline);
                    if (newsItem != null) {
                        newsItem.setVisible(destination.getId() != R.id.junior_news);
                    }
                }
            });

            UIUtils.manualToolbarTitleUpdateWithNavigation(navController, this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.junior_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem newsItem = menu.findItem(R.id.news_outline);
        if (newsItem != null && navController != null && navController.getCurrentDestination() != null) {
            newsItem.setVisible(navController.getCurrentDestination().getId() != R.id.junior_news);
        }
        return super.onPrepareOptionsMenu(menu);
    }
}