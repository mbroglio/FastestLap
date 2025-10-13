package com.the_coffe_coders.fastestlap.ui.event.fragment;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.adapter.QualifyingResultsRecyclerAdapter;
import com.the_coffe_coders.fastestlap.domain.grand_prix.QualifyingResult;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;
import com.the_coffe_coders.fastestlap.util.UIUtils;

public class QualifyingResultsFragment extends DialogFragment {

    private Race race;

    public QualifyingResultsFragment() {
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
        View view = inflater.inflate(R.layout.fragment_qualifying_results, container, false);

        initializeFragment(view);

        return view;
    }

    private void initializeFragment(View view) {
        setupFragment(view);

    }

    private void setupFragment(View view) {
        List<QualifyingResult> qualifyingResultsList = race.getQualifyingResults();

        UIUtils.multipleSetTextViewText(
                new String[]{
                        race.getRaceName().toUpperCase(),
                        requireContext().getString(
                                R.string.full_event_results, requireContext().getString(R.string.qualifying))},
                new TextView[]{
                        view.findViewById(R.id.qualifying_results_title),
                        view.findViewById(R.id.event_description_title)});

        RecyclerView recyclerView = view.findViewById(R.id.qualifying_results_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        QualifyingResultsRecyclerAdapter adapter = new QualifyingResultsRecyclerAdapter(requireContext(), qualifyingResultsList);
        recyclerView.setAdapter(adapter);

        Button closeButton = view.findViewById(R.id.close_results_button);
        closeButton.setOnClickListener(v -> dismiss());

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