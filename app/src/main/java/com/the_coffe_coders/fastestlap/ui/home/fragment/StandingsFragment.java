package com.the_coffe_coders.fastestlap.ui.home.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.util.UIUtils;

public class StandingsFragment extends Fragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_standings, container, false);

        MaterialCardView driverCardView = view.findViewById(R.id.drivers_card);
        MaterialCardView teamCardView = view.findViewById(R.id.constructors_card);

        driverCardView.setOnClickListener(v ->
                UIUtils.navigateToStandingsPage(getContext(), null, 1));

        teamCardView.setOnClickListener(v ->
                UIUtils.navigateToStandingsPage(getContext(), null, 0));

        return view;

    }

}