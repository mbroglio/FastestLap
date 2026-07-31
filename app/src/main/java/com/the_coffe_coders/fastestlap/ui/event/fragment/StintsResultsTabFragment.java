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
import com.the_coffe_coders.fastestlap.adapter.f1.StintsResultsRecyclerAdapter;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Race;
import com.the_coffe_coders.fastestlap.domain.f1.result.RaceResult;
import com.the_coffe_coders.fastestlap.domain.f1.result.Stint;

import java.util.ArrayList;
import java.util.List;

public class StintsResultsTabFragment extends Fragment {

    private Race race;
    private List<Stint> stintsList;

    public static StintsResultsTabFragment newInstance(Race race, List<Stint> stints) {
        StintsResultsTabFragment fragment = new StintsResultsTabFragment();
        Bundle args = new Bundle();
        if (race != null) {
            args.putParcelable("RACE", race);
        }
        if (stints != null) {
            args.putParcelableArrayList("STINTS", new ArrayList<>(stints));
        }
        fragment.setArguments(args);
        return fragment;
    }

    public static StintsResultsTabFragment newInstance(List<Stint> stints) {
        return newInstance(null, stints);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            race = androidx.core.os.BundleCompat.getParcelable(getArguments(), "RACE", Race.class);
            stintsList = getArguments().getParcelableArrayList("STINTS");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stints_results_tab, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.stints_results_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        if ((stintsList == null || stintsList.isEmpty()) && race != null) {
            stintsList = race.getStints();
        }
        if (stintsList == null) {
            stintsList = new ArrayList<>();
        }

        List<RaceResult> raceResults = (race != null)
                ? ((race.getRaceResults() != null && !race.getRaceResults().isEmpty())
                    ? race.getRaceResults() : race.getSprintResults())
                : null;

        recyclerView.setAdapter(new StintsResultsRecyclerAdapter(requireContext(), stintsList, raceResults));

        return view;
    }
}
