package com.the_coffe_coders.fastestlap.ui.junior.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.fragment.app.DialogFragment;

import com.the_coffe_coders.fastestlap.R;


public class JuniorDialogFragment extends DialogFragment {

    private int categoryType, content, raceType;
    private LinearLayout raceInfoLayout;
    private RelativeLayout fastestLapLayout, polePositionLayout;


    public JuniorDialogFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            categoryType = getArguments().getInt("CATEGORY_TYPE");
            content = getArguments().getInt("CONTENT");
            raceType = getArguments().getInt("RACE_TYPE"); //0: sprint; 1: feature
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_junior_dialog, container, false);

        raceInfoLayout = view.findViewById(R.id.race_info_layout);
        fastestLapLayout = view.findViewById(R.id.fastest_lap_layout);
        polePositionLayout = view.findViewById(R.id.pole_position_layout);

        Button closeButton = view.findViewById(R.id.close_button);
        closeButton.setOnClickListener(v -> dismiss());

        executeFunctions(content);

        return view;
    }

    private void executeFunctions(int content) {
        switch (content) {
            case 0: // Entry list
                raceInfoLayout.setVisibility(View.GONE);
                executeEntryList();
                break;
            case 1: // Calendar
                raceInfoLayout.setVisibility(View.GONE);
                executeCalendar();
                break;
            case 2: // Results
                raceInfoLayout.setVisibility(View.VISIBLE);
                if (raceType == 0) {
                    polePositionLayout.setVisibility(View.GONE);
                } else {
                    polePositionLayout.setVisibility(View.VISIBLE);
                }
                executeResults();
                break;
            case 3: // Drivers standing
                executeDriversStanding();
                break;
            case 4: // Constructors standing
                executeConstructorsStanding();
                break;

        }
    }

    private void executeEntryList() {
    }

    private void executeCalendar() {
    }

    private void executeResults() {
    }

    private void executeDriversStanding() {
    }

    private void executeConstructorsStanding() {
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
