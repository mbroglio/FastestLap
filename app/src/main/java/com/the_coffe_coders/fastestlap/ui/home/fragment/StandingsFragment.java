package com.the_coffe_coders.fastestlap.ui.home.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.card.MaterialCardView;
import com.the_coffe_coders.fastestlap.R;

import com.the_coffe_coders.fastestlap.ui.standings.ConstructorsStandingActivity;
import com.the_coffe_coders.fastestlap.ui.standings.DriversStandingActivity;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link StandingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StandingsFragment extends Fragment {


    // TODO: Rename and change types and number of parameters
    public static StandingsFragment newInstance(String param1, String param2) {
        return new StandingsFragment();
    }

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

        driverCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), DriversStandingActivity.class);
                startActivity(intent);
            }
        });

        teamCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), ConstructorsStandingActivity.class);
                startActivity(intent);
            }
        });
        return view;

    }
}