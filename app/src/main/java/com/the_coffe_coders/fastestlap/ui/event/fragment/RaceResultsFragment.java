package com.the_coffe_coders.fastestlap.ui.event.fragment;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.adapter.RaceResultsAdapter;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;

public class RaceResultsFragment extends DialogFragment {
    private Race race;

    public RaceResultsFragment() {
        // Required empty public constructor
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            race = getArguments().getParcelable("RACE");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_race_results, container, false);

        TextView titleTextView = view.findViewById(R.id.race_results_title);
        titleTextView.setText(race.getRaceName().toUpperCase());

        RecyclerView recyclerView = view.findViewById(R.id.race_results_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        RaceResultsAdapter adapter = new RaceResultsAdapter(requireContext(), race.getRaceResults());
        recyclerView.setAdapter(adapter);

        Button closeButton = view.findViewById(R.id.close_results_button);
        closeButton.setOnClickListener(v -> dismiss());

        return view;
    }

    @Override
    public void onStart(){
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            Window window = getDialog().getWindow();
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }
    }
}