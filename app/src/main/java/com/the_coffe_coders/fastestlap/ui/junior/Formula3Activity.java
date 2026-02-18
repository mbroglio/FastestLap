package com.the_coffe_coders.fastestlap.ui.junior;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentContainerView;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

public class Formula3Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_formula3);

        setToolbarAndNavigation();

    }

    private void setToolbarAndNavigation() {
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        UIUtils.applyWindowInsets(toolbar);

        FragmentContainerView fragmentContainerView = findViewById(R.id.fragmentContainerView);
        UIUtils.applyWindowInsets(fragmentContainerView);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();

            // Manually update toolbar title based on current destination
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                if (getSupportActionBar() != null) {
                    CharSequence title = destination.getLabel();
                    getSupportActionBar().setTitle(title);
                    // Always show the navigation icon (back button)
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                }
            });
        }
    }
}