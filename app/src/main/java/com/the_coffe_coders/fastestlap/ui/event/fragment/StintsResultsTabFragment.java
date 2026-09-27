package com.the_coffe_coders.fastestlap.ui.event.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.adapter.f1.StintsResultsRecyclerAdapter;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Race;
import com.the_coffe_coders.fastestlap.domain.f1.result.RaceResult;
import com.the_coffe_coders.fastestlap.domain.f1.result.Stint;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.RaceResultViewModel;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.RaceResultViewModelFactory;

import androidx.lifecycle.ViewModelProvider;

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
        View notAvailableLayout = view.findViewById(R.id.stints_not_available_layout);
        ProgressBar progressBar = view.findViewById(R.id.stints_progress_bar);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        if ((stintsList == null || stintsList.isEmpty()) && race != null) {
            String sessionType = (race.getRaceResults() != null && !race.getRaceResults().isEmpty()) ? "Race" : "Sprint";
            stintsList = sessionType.equals("Sprint") ? race.getSprintStints() : race.getRaceStints();
            if (stintsList == null || stintsList.isEmpty()) {
                stintsList = race.getStints();
            }
        }

        List<RaceResult> raceResults = (race != null)
                ? ((race.getRaceResults() != null && !race.getRaceResults().isEmpty())
                    ? race.getRaceResults() : race.getSprintResults())
                : null;

        if (stintsList != null && !stintsList.isEmpty()) {
            recyclerView.setVisibility(View.VISIBLE);
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            if (notAvailableLayout != null) {
                notAvailableLayout.setVisibility(View.GONE);
            }
            recyclerView.setAdapter(new StintsResultsRecyclerAdapter(requireContext(), stintsList, raceResults));
        } else {
            recyclerView.setVisibility(View.GONE);

            if (race != null && race.getRaceName() != null && isAdded()) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.VISIBLE);
                }
                if (notAvailableLayout != null) {
                    notAvailableLayout.setVisibility(View.GONE);
                }

                String sessionType = (race.getRaceResults() != null && !race.getRaceResults().isEmpty()) ? "Race" : "Sprint";
                RaceResultViewModel raceResultViewModel = new ViewModelProvider(
                        requireActivity(),
                        new RaceResultViewModelFactory(requireActivity().getApplication(), requireActivity())
                ).get(RaceResultViewModel.class);

                raceResultViewModel.getStints(race.getRaceName(), sessionType).observe(getViewLifecycleOwner(), result -> {
                    if (result instanceof Result.Loading) {
                        if (progressBar != null) {
                            progressBar.setVisibility(View.VISIBLE);
                        }
                        recyclerView.setVisibility(View.GONE);
                        if (notAvailableLayout != null) {
                            notAvailableLayout.setVisibility(View.GONE);
                        }
                        return;
                    }

                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }

                    if (result instanceof Result.StintsSuccess) {
                        List<Stint> fetched = ((Result.StintsSuccess) result).getData();
                        if (fetched != null && !fetched.isEmpty() && isAdded()) {
                            stintsList = fetched;
                            if (sessionType.equals("Sprint")) {
                                race.setSprintStints(fetched);
                            } else {
                                race.setRaceStints(fetched);
                            }
                            recyclerView.setVisibility(View.VISIBLE);
                            if (notAvailableLayout != null) {
                                notAvailableLayout.setVisibility(View.GONE);
                            }
                            recyclerView.setAdapter(new StintsResultsRecyclerAdapter(requireContext(), stintsList, raceResults));
                        } else if (isAdded()) {
                            recyclerView.setVisibility(View.GONE);
                            if (notAvailableLayout != null) {
                                notAvailableLayout.setVisibility(View.VISIBLE);
                            }
                        }
                    } else if (result instanceof Result.Error && isAdded()) {
                        recyclerView.setVisibility(View.GONE);
                        if (notAvailableLayout != null) {
                            notAvailableLayout.setVisibility(View.VISIBLE);
                        }
                    }
                });
            } else {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                if (notAvailableLayout != null) {
                    notAvailableLayout.setVisibility(View.VISIBLE);
                }
            }
        }

        return view;
    }
}
