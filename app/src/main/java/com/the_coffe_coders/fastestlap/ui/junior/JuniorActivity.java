package com.the_coffe_coders.fastestlap.ui.junior;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.util.ui.NavigationUtils;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

public class JuniorActivity extends AppCompatActivity {

    private int categoryType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_junior);

        //0: F2; 1: F3
        categoryType = getIntent().getIntExtra("CATEGORY_TYPE", 0);

        setToolbar();

        setupPage();
    }

    private void setToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        if(categoryType == 0){
            toolbar.setTitle("FORMULA 2");
            toolbar.setBackgroundColor(getColor(R.color.formula_2));
        }else{
            toolbar.setTitle("FORMULA 3");
            toolbar.setBackgroundColor(getColor(R.color.ferrari_secondary));
        }

        UIUtils.applyWindowInsets(toolbar);
    }

    private void setupPage() {
        MaterialCardView entryListCard, calendarCard, resultsCard, driversStandingCard, constructorsStandingCard;
        entryListCard = findViewById(R.id.entry_list_card);
        calendarCard = findViewById(R.id.calendar_card);
        resultsCard = findViewById(R.id.results_card);
        driversStandingCard = findViewById(R.id.drivers_standing_card);
        constructorsStandingCard = findViewById(R.id.constructors_standing_card);

        entryListCard.setOnClickListener(v -> {}
                //NavigationUtils.showEntryListDialog(getSupportFragmentManager(), categoryType)
                    //content = entryList
        );

        calendarCard.setOnClickListener(v -> {}
                //NavigationUtils.showCalendarDialog(getSupportFragmentManager(), categoryType)
        );

        resultsCard.setOnClickListener(v ->
                NavigationUtils.navigateToJuniorResultsPage(this, categoryType)
        );

        driversStandingCard.setOnClickListener(v -> {}
                //NavigationUtils.showDriversStandingDialog(getSupportFragmentManager(this, categoryType)
        );

        constructorsStandingCard.setOnClickListener(v -> {}
                //NavigationUtils.showConstructorsStandingDialog(getSupportFragmentManager(this, categoryType)
        );



    }
}