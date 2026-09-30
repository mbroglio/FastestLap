package com.the_coffe_coders.fastestlap.ui.junior.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.os.BundleCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.adapter.junior.JuniorResultsRecyclerAdapter;
import com.the_coffe_coders.fastestlap.domain.junior.result.FeatureRace;
import com.the_coffe_coders.fastestlap.domain.junior.result.SprintRace;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

public class JuniorFullResultsDialogFragment extends DialogFragment {

    private String circuit;
    private FeatureRace featureRace;
    private SprintRace sprintRace;
    private int categoryType = 0;
    private int raceType = 0;

    private MaterialCardView dialogPage;
    private LinearLayout raceInfoLayout;
    private LinearLayout titleLayout;
    private TextView dialogTitle;
    private TextView raceTypeTitle;
    private TextView driverNamePole;
    private TextView driverNameFastestLap;
    private TextView polepositionTitle;
    private View fastestLapLayout;
    private View polePositionLayout;
    private RecyclerView juniorRecyclerView;
    private Button closeButton;

    public JuniorFullResultsDialogFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            categoryType = getArguments().getInt("CATEGORY_TYPE", 0);
            raceType = getArguments().getInt("RACE_TYPE", 0);
            circuit = getArguments().getString("CIRCUIT");
            featureRace = BundleCompat.getParcelable(getArguments(), "JUNIOR_FEATURE_RACE", FeatureRace.class);
            sprintRace = BundleCompat.getParcelable(getArguments(), "JUNIOR_SPRINT_RACE", SprintRace.class);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_junior_dialog, container, false);

        dialogPage = view.findViewById(R.id.dialog_page);
        raceInfoLayout = view.findViewById(R.id.race_info_layout);
        raceTypeTitle = view.findViewById(R.id.race_type_title);
        fastestLapLayout = view.findViewById(R.id.fastest_lap_layout);
        polePositionLayout = view.findViewById(R.id.pole_position_layout);
        juniorRecyclerView = view.findViewById(R.id.junior_recycler_view);
        dialogTitle = view.findViewById(R.id.dialog_title);
        titleLayout = view.findViewById(R.id.title_layout);
        driverNameFastestLap = view.findViewById(R.id.driver_name_fastest_lap);
        driverNamePole = view.findViewById(R.id.driver_name_pole);
        closeButton = view.findViewById(R.id.close_button);
        polepositionTitle = view.findViewById(R.id.pole_position_title);

        int primaryColor = (categoryType == 0)
                ? ContextCompat.getColor(requireContext(), R.color.formula_2)
                : ContextCompat.getColor(requireContext(), R.color.app_primary_red);

        dialogPage.setStrokeColor(primaryColor);
        dialogTitle.setBackgroundColor(primaryColor);
        polepositionTitle.setTextColor(primaryColor);
        titleLayout.setBackgroundColor(primaryColor);
        closeButton.setBackgroundColor(primaryColor);

        closeButton.setOnClickListener(v -> dismiss());

        setupResultsView();
        setupRecyclerView();

        return view;
    }

    private void setupResultsView() {
        UIUtils.singleSetTextViewText(circuit, dialogTitle);
        raceInfoLayout.setVisibility(View.VISIBLE);
        fastestLapLayout.setVisibility(View.VISIBLE);

        if (featureRace == null && sprintRace != null) {
            polePositionLayout.setVisibility(View.GONE);
            UIUtils.multipleSetTextViewText(
                    new String[]{
                            ContextCompat.getString(requireContext(), R.string.sprint),
                            sprintRace.getFastest_lap()
                    },
                    new TextView[]{
                            raceTypeTitle,
                            driverNameFastestLap
                    }
            );
        } else if (featureRace != null) {
            String featureTitle = (raceType == 2) ? ContextCompat.getString(requireContext(), R.string.feature_2)
                    : (raceType == 1 ? ContextCompat.getString(requireContext(), R.string.feature_1)
                    : ContextCompat.getString(requireContext(), R.string.feature));
            UIUtils.multipleSetTextViewText(
                    new String[]{
                            featureTitle,
                            featureRace.getPole_position(),
                            featureRace.getFastest_lap()
                    },
                    new TextView[]{
                            raceTypeTitle,
                            driverNamePole,
                            driverNameFastestLap
                    }
            );
        }
    }

    private void setupRecyclerView() {
        juniorRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        JuniorResultsRecyclerAdapter adapter;
        if (featureRace != null) {
            adapter = new JuniorResultsRecyclerAdapter(requireContext(), featureRace, getParentFragmentManager());
        } else {
            adapter = new JuniorResultsRecyclerAdapter(requireContext(), sprintRace, getParentFragmentManager());
        }
        juniorRecyclerView.setAdapter(adapter);
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
