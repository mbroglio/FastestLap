package com.the_coffe_coders.fastestlap.ui.event.fragment;

import android.os.Bundle;
import android.util.Log;
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
import com.the_coffe_coders.fastestlap.ui.event.RaceAndSprintResultsActivity;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.RaceResultViewModel;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.RaceResultViewModelFactory;

import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;

public class StintsResultsTabFragment extends Fragment {

    private Race race;
    private String locality;
    private List<Stint> stintsList;

    public static StintsResultsTabFragment newInstance(Race race, String locality, List<Stint> stints) {
        StintsResultsTabFragment fragment = new StintsResultsTabFragment();
        Bundle args = new Bundle();
        if (race != null) {
            args.putParcelable("RACE", race);
            if (race.getTrack() != null && race.getTrack().getGp_long_name() != null) {
                args.putString("GP_LONG_NAME", race.getTrack().getGp_long_name());
            }
        }
        if (stints != null) {
            args.putParcelableArrayList("STINTS", new ArrayList<>(stints));
        }
        if (locality != null) {
            args.putString("LOCALITY", locality);
        }
        fragment.setArguments(args);
        return fragment;
    }

    public static StintsResultsTabFragment newInstance(List<Stint> stints) {
        return newInstance(null, null, stints);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            race = androidx.core.os.BundleCompat.getParcelable(getArguments(), "RACE", Race.class);
            stintsList = androidx.core.os.BundleCompat.getParcelableArrayList(getArguments(), "STINTS", Stint.class);
            locality = getArguments().getString("LOCALITY");
            String gpLongName = getArguments().getString("GP_LONG_NAME");
            if (race != null) {
                if (race.getTrack() == null) {
                    com.the_coffe_coders.fastestlap.domain.f1.track.Track t = new com.the_coffe_coders.fastestlap.domain.f1.track.Track();
                    if (locality != null) {
                        com.the_coffe_coders.fastestlap.domain.f1.track.Location loc = new com.the_coffe_coders.fastestlap.domain.f1.track.Location();
                        loc.setLocality(locality);
                        t.setLocation(loc);
                    }
                    t.setGp_long_name(gpLongName);
                    race.setTrack(t);
                } else if (race.getTrack().getGp_long_name() == null && gpLongName != null) {
                    race.getTrack().setGp_long_name(gpLongName);
                }
                if (locality == null && race.getTrack().getLocation() != null) {
                    locality = race.getTrack().getLocation().getLocality();
                }
            }
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

        // Recovery baseline: se non presenti in stintsList, recupera stint già persistiti nell'oggetto race o nell'Activity
        if (stintsList == null || stintsList.isEmpty()) {
            stintsList = getLocalStintsFallback();
        }

        List<RaceResult> raceResults = (race != null)
                ? ((race.getRaceResults() != null && !race.getRaceResults().isEmpty())
                    ? race.getRaceResults() : race.getSprintResults())
                : null;

        boolean hasLocalStints = stintsList != null && !stintsList.isEmpty();

        if (hasLocalStints) {
            recyclerView.setVisibility(View.VISIBLE);
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            if (notAvailableLayout != null) notAvailableLayout.setVisibility(View.GONE);
            recyclerView.setAdapter(new StintsResultsRecyclerAdapter(requireContext(), stintsList, raceResults));
        } else {
            recyclerView.setVisibility(View.GONE);
            if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
            if (notAvailableLayout != null) notAvailableLayout.setVisibility(View.GONE);
        }

        if (race != null && race.getRaceName() != null && isAdded()) {
            String sessionType = (race.getRaceResults() != null && !race.getRaceResults().isEmpty()) ? "Race" : "Sprint";
            RaceResultViewModel raceResultViewModel = new ViewModelProvider(
                    requireActivity(),
                    new RaceResultViewModelFactory(requireActivity().getApplication(), requireActivity())
            ).get(RaceResultViewModel.class);

            Log.i("StintsTab", "Requesting fresh stints for race: " + race.getRaceName() + " (" + sessionType + ")");

            String gpLongName = (race.getTrack() != null) ? race.getTrack().getGp_long_name() : null;
            raceResultViewModel.getStints(locality, race.getRaceName(), sessionType, gpLongName).observe(getViewLifecycleOwner(), result -> {
                if (result instanceof Result.Loading) {
                    if (!hasLocalStints && (stintsList == null || stintsList.isEmpty())) {
                        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                        if (notAvailableLayout != null) notAvailableLayout.setVisibility(View.GONE);
                    }
                    return;
                }

                cancelTimeout();

                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }

                if (result instanceof Result.StintsSuccess) {
                    List<Stint> fetched = ((Result.StintsSuccess) result).getData();
                    if (fetched != null && !fetched.isEmpty() && isAdded()) {
                        Log.i("StintsTab", "OpenF1 stints loaded: " + fetched.size() + ". Overwriting local data.");
                        stintsList = fetched;
                        if (sessionType.equals("Sprint")) {
                            race.setSprintStints(fetched);
                        } else {
                            race.setRaceStints(fetched);
                        }
                        if (requireActivity() instanceof RaceAndSprintResultsActivity) {
                            ((RaceAndSprintResultsActivity) requireActivity()).updateStints(fetched);
                        }
                        recyclerView.setVisibility(View.VISIBLE);
                        if (notAvailableLayout != null) {
                            notAvailableLayout.setVisibility(View.GONE);
                        }
                        recyclerView.setAdapter(new StintsResultsRecyclerAdapter(requireContext(), stintsList, raceResults));
                    } else if (isAdded()) {
                        Log.w("StintsTab", "OpenF1 returned empty stints. Checking recovery fallback.");
                        applyRecoveryOrShowNotAvailable(recyclerView, progressBar, notAvailableLayout, raceResults);
                    }
                } else if (result instanceof Result.Error && isAdded()) {
                    Log.w("StintsTab", "OpenF1 API error: " + ((Result.Error) result).getMessage() + ". Applying recovery fallback.");
                    applyRecoveryOrShowNotAvailable(recyclerView, progressBar, notAvailableLayout, raceResults);
                }
            });

            if (!hasLocalStints) {
                startTimeout(progressBar, recyclerView, notAvailableLayout, raceResults);
            }
        } else {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            if (!hasLocalStints && notAvailableLayout != null) {
                notAvailableLayout.setVisibility(View.VISIBLE);
            }
        }

        return view;
    }

    private void applyRecoveryOrShowNotAvailable(RecyclerView recyclerView, ProgressBar progressBar, View notAvailableLayout, List<RaceResult> raceResults) {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        List<Stint> recovery = getLocalStintsFallback();
        if (recovery != null && !recovery.isEmpty()) {
            stintsList = recovery;
            if (recyclerView != null) {
                recyclerView.setVisibility(View.VISIBLE);
                recyclerView.setAdapter(new StintsResultsRecyclerAdapter(requireContext(), stintsList, raceResults));
            }
            if (notAvailableLayout != null) {
                notAvailableLayout.setVisibility(View.GONE);
            }
            Log.i("StintsTab", "Recovery succeeded: displaying " + recovery.size() + " local persisted stints");
        } else {
            if (recyclerView != null) {
                recyclerView.setVisibility(View.GONE);
            }
            if (notAvailableLayout != null) {
                notAvailableLayout.setVisibility(View.VISIBLE);
            }
        }
    }

    private List<Stint> getLocalStintsFallback() {
        if (stintsList != null && !stintsList.isEmpty()) {
            return stintsList;
        }
        if (isAdded() && requireActivity() instanceof RaceAndSprintResultsActivity) {
            List<Stint> actStints = ((RaceAndSprintResultsActivity) requireActivity()).getStintsList();
            if (actStints != null && !actStints.isEmpty()) {
                return actStints;
            }
        }
        if (race != null) {
            String sessionType = (race.getRaceResults() != null && !race.getRaceResults().isEmpty()) ? "Race" : "Sprint";
            List<Stint> list = "Sprint".equalsIgnoreCase(sessionType) ? race.getSprintStints() : race.getRaceStints();
            if (list != null && !list.isEmpty()) {
                return list;
            }
            if (race.getStints() != null && !race.getStints().isEmpty()) {
                return race.getStints();
            }
        }
        return null;
    }

    private android.os.Handler stintTimeoutHandler;
    private Runnable stintTimeoutRunnable;

    private void startTimeout(ProgressBar progressBar, RecyclerView recyclerView, View notAvailableLayout, List<RaceResult> raceResults) {
        cancelTimeout();
        stintTimeoutHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        stintTimeoutRunnable = () -> {
            if (isAdded()) {
                applyRecoveryOrShowNotAvailable(recyclerView, progressBar, notAvailableLayout, raceResults);
            }
        };
        stintTimeoutHandler.postDelayed(stintTimeoutRunnable, 5000);
    }

    private void cancelTimeout() {
        if (stintTimeoutHandler != null && stintTimeoutRunnable != null) {
            stintTimeoutHandler.removeCallbacks(stintTimeoutRunnable);
            stintTimeoutRunnable = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cancelTimeout();
    }
}
