package com.the_coffe_coders.fastestlap.ui.junior.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.util.ui.NavigationUtils;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

public class F2HomeFragment extends Fragment {

    private View view;

    private final int categoryType = 0;

    public F2HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_f2_home, container, false);

        setupFragment();

        return view;
    }

    private void setupFragment() {
        ScrollView layout = view.findViewById(R.id.f2_layout);
        UIUtils.applyWindowInsets(layout);

        MaterialCardView entryListCard, calendarCard, resultsCard, driversStandingCard, constructorsStandingCard, carBioCard;
        entryListCard = view.findViewById(R.id.f2_entry_list_card);
        calendarCard = view.findViewById(R.id.f2_calendar_card);
        resultsCard = view.findViewById(R.id.f2_results_card);
        driversStandingCard = view.findViewById(R.id.f2_drivers_standing_card);
        constructorsStandingCard = view.findViewById(R.id.f2_constructors_standing_card);
        carBioCard = view.findViewById(R.id.f2_car_bio_card);

        entryListCard.setOnClickListener(v ->
                NavigationUtils.showEntryListDialog(requireActivity().getSupportFragmentManager(), categoryType)
        );

        calendarCard.setOnClickListener(v ->
                NavigationUtils.showCalendarDialog(requireActivity().getSupportFragmentManager(), categoryType)
        );

        resultsCard.setOnClickListener(v ->
                NavigationUtils.navigateToJuniorResultsPage(v, categoryType)
        );

        driversStandingCard.setOnClickListener(v ->
                NavigationUtils.showDriversStandingDialog(requireActivity().getSupportFragmentManager(), categoryType)
        );

        constructorsStandingCard.setOnClickListener(v ->
                NavigationUtils.showConstructorsStandingDialog(requireActivity().getSupportFragmentManager(), categoryType)
        );

        carBioCard.setOnClickListener(v ->
                NavigationUtils.navigateToJuniorCarBioPage(v, categoryType)
        );
    }
}