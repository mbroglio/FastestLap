package com.the_coffe_coders.fastestlap.ui.junior;

import android.os.Bundle;
import android.widget.ScrollView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.util.ui.NavigationUtils;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

public class Formula3Activity extends AppCompatActivity {
    private static final String TAG = "Formula3Activity";

    private final String series = "f3";
    private int categoryType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_formula3);

        //0: F2; 1: F3
        categoryType = getIntent().getIntExtra("CATEGORY_TYPE", 0);

        setToolbar();

        setupPage();
    }

    private void setToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        UIUtils.applyWindowInsets(toolbar);
    }

    private void setupPage() {
        ScrollView layout = findViewById(R.id.f3_layout);
        UIUtils.applyWindowInsets(layout);

        MaterialCardView entryListCard, calendarCard, resultsCard, driversStandingCard, constructorsStandingCard;
        entryListCard = findViewById(R.id.f3_entry_list_card);
        calendarCard = findViewById(R.id.f3_calendar_card);
        resultsCard = findViewById(R.id.f3_results_card);
        driversStandingCard = findViewById(R.id.f3_drivers_standing_card);
        constructorsStandingCard = findViewById(R.id.f3_constructors_standing_card);

        entryListCard.setOnClickListener(v ->
                NavigationUtils.showEntryListDialog(getSupportFragmentManager(), categoryType)
        );

        calendarCard.setOnClickListener(v ->
                NavigationUtils.showCalendarDialog(getSupportFragmentManager(), categoryType)
        );

        resultsCard.setOnClickListener(v ->
                NavigationUtils.navigateToJuniorResultsPage(this, categoryType)
        );

        driversStandingCard.setOnClickListener(v ->
                NavigationUtils.showDriversStandingDialog(getSupportFragmentManager(), categoryType)
        );

        constructorsStandingCard.setOnClickListener(v ->
                NavigationUtils.showConstructorsStandingDialog(getSupportFragmentManager(), categoryType)
        );


    }
}