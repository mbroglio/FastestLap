package com.the_coffe_coders.fastestlap.ui.home.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.util.UIUtils;

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

        View view = inflater.inflate(R.layout.fragment_racing, container, false);

        MaterialCardView pastEventsCard = view.findViewById(R.id.past_events_card);
        MaterialCardView upcomingEventsCard = view.findViewById(R.id.upcoming_events_card);

        pastEventsCard.setOnClickListener(v ->
                UIUtils.navigateToEventsListPage(getContext(), 1));

        upcomingEventsCard.setOnClickListener(v ->
                UIUtils.navigateToEventsListPage(getContext(), 0));

        return view;
    }
}