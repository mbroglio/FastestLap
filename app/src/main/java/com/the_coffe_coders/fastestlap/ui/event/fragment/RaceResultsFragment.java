package com.the_coffe_coders.fastestlap.ui.event.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.adapter.RaceResultsRecyclerAdapter;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;
import com.the_coffe_coders.fastestlap.domain.grand_prix.RaceResult;
import com.the_coffe_coders.fastestlap.domain.grand_prix.RaceResultFastestLap;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.EventViewModel;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.EventViewModelFactory;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.UIUtils;

import java.util.List;
import java.util.Objects;

public class RaceResultsFragment extends DialogFragment {
    private Race race;
    private EventViewModel eventViewModel;

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

        initializeFragment(view);

        return view;
    }

    private void initializeFragment(View view) {
        initializeViewModels();
        setupFragment(view);
    }

    private void initializeViewModels() {
        eventViewModel = new ViewModelProvider(this, new EventViewModelFactory(requireActivity().getApplication())).get(EventViewModel.class);
    }

    private void setupFragment(View view) {
        List<RaceResult> resultsList = race.getRaceResults();

        UIUtils.singleSetTextViewText(race.getRaceName().toUpperCase(), view.findViewById(R.id.race_results_title));

        setupFastestLapLayout(view, resultsList);

        RecyclerView recyclerView = view.findViewById(R.id.race_results_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        RaceResultsRecyclerAdapter adapter = new RaceResultsRecyclerAdapter(requireContext(), resultsList);
        recyclerView.setAdapter(adapter);

        Button closeButton = view.findViewById(R.id.close_results_button);
        closeButton.setOnClickListener(v -> dismiss());
    }

    private void setupFastestLapLayout(View view, List<RaceResult> resultsList) {
        RaceResultFastestLap raceFastestLap = eventViewModel.extractFastestLap(resultsList);
        RelativeLayout fastestLapLayout = view.findViewById(R.id.fastest_lap_layout);

        View teamColorIndicator = fastestLapLayout.findViewById(R.id.team_color_indicator);
        int teamColor = ContextCompat.getColor(requireContext(),
                Objects.requireNonNullElseGet(Constants.TEAM_COLOR.get(raceFastestLap.getConstructorId()), () -> R.color.white));
        teamColorIndicator.setBackgroundColor(teamColor);

        UIUtils.multipleSetTextViewText(
                new String[]{
                        raceFastestLap.getDriverName(),
                        raceFastestLap.getTime().getTime(),
                        requireContext().getString(R.string.lap, raceFastestLap.getLap())},
                new TextView[]{
                        fastestLapLayout.findViewById(R.id.driver_name),
                        fastestLapLayout.findViewById(R.id.fastest_lap),
                        fastestLapLayout.findViewById(R.id.lap_value)});
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            Window window = getDialog().getWindow();
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }
    }
}