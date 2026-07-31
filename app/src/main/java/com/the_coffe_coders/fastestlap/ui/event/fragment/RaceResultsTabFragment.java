package com.the_coffe_coders.fastestlap.ui.event.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.adapter.f1.RaceResultsRecyclerAdapter;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Race;
import com.the_coffe_coders.fastestlap.domain.f1.result.RaceResult;

import java.util.List;

/**
 * Fragment per visualizzare la classifica dei risultati di Gara o Sprint.
 */
public class RaceResultsTabFragment extends Fragment {

    private Race race;

    public static RaceResultsTabFragment newInstance(Race race) {
        RaceResultsTabFragment fragment = new RaceResultsTabFragment();
        Bundle args = new Bundle();
        args.putParcelable("RACE", race);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            race = androidx.core.os.BundleCompat.getParcelable(getArguments(), "RACE", Race.class);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_race_results_tab, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.race_results_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        if (race != null) {
            List<RaceResult> raceResultsList = race.getRaceResults();
            List<RaceResult> sprintResultsList = race.getSprintResults();

            if (raceResultsList != null && !raceResultsList.isEmpty()) {
                recyclerView.setAdapter(new RaceResultsRecyclerAdapter(requireContext(), raceResultsList));
            } else if (sprintResultsList != null && !sprintResultsList.isEmpty()) {
                recyclerView.setAdapter(new RaceResultsRecyclerAdapter(requireContext(), sprintResultsList));
            }
        }

        return view;
    }
}
