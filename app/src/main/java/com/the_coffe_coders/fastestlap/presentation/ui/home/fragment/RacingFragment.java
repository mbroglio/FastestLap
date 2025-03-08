package com.the_coffe_coders.fastestlap.presentation.ui.home.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.presentation.ui.event.PastEventsActivity;
import com.the_coffe_coders.fastestlap.presentation.ui.event.UpcomingEventsActivity;

public class RacingFragment extends Fragment {

    public RacingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_racing, container, false);

        // Find the views
        MaterialCardView pastEventsCard = view.findViewById(R.id.past_events_card);
        MaterialCardView upcomingEventsCard = view.findViewById(R.id.upcoming_events_card);

        // Set click listeners
        pastEventsCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), PastEventsActivity.class);
                startActivity(intent);
            }
        });

        upcomingEventsCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), UpcomingEventsActivity.class);
                startActivity(intent);
            }
        });

        return view;
    }
}