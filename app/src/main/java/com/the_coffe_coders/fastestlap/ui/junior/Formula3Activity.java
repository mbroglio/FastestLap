package com.the_coffe_coders.fastestlap.ui.junior;

import android.os.Bundle;
import android.util.Log;
import android.widget.ScrollView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.junior.JuniorEntryList;
import com.the_coffe_coders.fastestlap.domain.junior.calendar.JuniorCalendar;
import com.the_coffe_coders.fastestlap.ui.junior.viewmodel.f3.F3ViewModel;
import com.the_coffe_coders.fastestlap.ui.junior.viewmodel.f3.F3ViewModelFactory;
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

        entryListCard.setOnClickListener(v -> executeOperation2()
                //NavigationUtils.showEntryListDialog(getSupportFragmentManager(), categoryType)
                //content = entryList
        );

        calendarCard.setOnClickListener(v -> executeOperation1()
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

    private void executeOperation2() {
        F3ViewModel f3ViewModel = new ViewModelProvider(this, new F3ViewModelFactory(getApplication())).get(F3ViewModel.class);

        MutableLiveData<Result> livedata = f3ViewModel.getEntryList(series);

        livedata.observe(this, result -> {
            if (result instanceof Result.Loading) {
                return;
            }
            if (result.isSuccess()) {
                Log.i(TAG, "ENTRY LIST SUCCESS");
                JuniorEntryList entryList = ((Result.JuniorEntryListSuccess) result).getData();

                if (entryList == null){
                    Log.i(TAG, "ENTRY LIST NULL");
                }else {
                    Log.i(TAG, "ENTRY LIST NOT NULL");
                    //print entryList on console
                    for (int i = 0; i < entryList.getTeams().size(); i++) {
                        Log.i(TAG, "Entry: " + entryList.getTeams().get(i).toString());
                    }
                }
            }
        });
    }

    private void executeOperation1() {
        F3ViewModel f3ViewModel = new ViewModelProvider(this, new F3ViewModelFactory(getApplication())).get(F3ViewModel.class);

        MutableLiveData<Result> livedata = f3ViewModel.getCalendar(series);

        livedata.observe(this, result -> {
            if (result instanceof Result.Loading) {
                return;
            }
            if (result.isSuccess()) {
                Log.i(TAG, "CALENDAR SUCCESS");
                JuniorCalendar calendar = ((Result.JuniorCalendarSuccess) result).getData();

                if (calendar == null){
                    Log.i(TAG, "CALENDAR NULL");
                }else{
                    Log.i(TAG, "CALENDAR NOT NULL");
                    //print calendar on console
                    for (int i = 0; i < calendar.getEvents().size(); i++) {
                        Log.i(TAG, "Event: " + calendar.getEvents().get(i).toString());
                    }
                }
            }
        });


    }
}