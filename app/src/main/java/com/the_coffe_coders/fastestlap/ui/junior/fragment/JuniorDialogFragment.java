package com.the_coffe_coders.fastestlap.ui.junior.fragment;

import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.adapter.junior.JuniorCalendarRecyclerAdapter;
import com.the_coffe_coders.fastestlap.adapter.junior.JuniorEntryListRecyclerAdapter;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.junior.standings.JuniorEntryList;
import com.the_coffe_coders.fastestlap.domain.junior.calendar.JuniorCalendar;
import com.the_coffe_coders.fastestlap.ui.junior.viewmodel.JuniorCategoryViewModel;
import com.the_coffe_coders.fastestlap.ui.junior.viewmodel.JuniorCategoryViewModelFactory;

public class JuniorDialogFragment extends DialogFragment {
    private static final String TAG = "JuniorDialogFragment";

    private int categoryType, content, raceType;
    private MaterialCardView dialogPage;
    private LinearLayout raceInfoLayout, raceTypeChoiceLayout, titleLayout;
    private TextView dialogTitle;
    private MaterialSwitch switchButton;
    private RelativeLayout fastestLapLayout, polePositionLayout;
    private JuniorCategoryViewModel juniorCategoryViewModel;
    private RecyclerView juniorRecyclerView;

    public JuniorDialogFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            categoryType = getArguments().getInt("CATEGORY_TYPE"); //0: F2; 1:F3
            content = getArguments().getInt("CONTENT");
            raceType = getArguments().getInt("RACE_TYPE"); //0: sprint; 1: feature
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_junior_dialog, container, false);

        juniorCategoryViewModel = new ViewModelProvider(this, new JuniorCategoryViewModelFactory(requireActivity().getApplication())).get(JuniorCategoryViewModel.class);

        dialogPage = view.findViewById(R.id.dialog_page);
        raceInfoLayout = view.findViewById(R.id.race_info_layout);
        raceTypeChoiceLayout = view.findViewById(R.id.race_choice_layout);
        fastestLapLayout = view.findViewById(R.id.fastest_lap_layout);
        polePositionLayout = view.findViewById(R.id.pole_position_layout);
        juniorRecyclerView = view.findViewById(R.id.junior_recycler_view);
        dialogTitle = view.findViewById(R.id.dialog_title);
        titleLayout = view.findViewById(R.id.title_layout);

        Button closeButton = view.findViewById(R.id.close_button);
        closeButton.setOnClickListener(v -> dismiss());

        executeFunctions();

        return view;
    }

    private void executeFunctions() {
        switch (content) {
            case 0: // Entry list
                Log.i(TAG, "ENTRY LIST clicked");
                setDialogForEntryList();
                executeEntryList();
                break;
            case 1: // Calendar
                Log.i(TAG, "CALENDAR clicked");
                setDialogForCalendar();
                executeCalendar();
                break;
            case 2: // Drivers standing
                executeDriversStanding();
                break;
            case 3: // Constructors standing
                executeConstructorsStanding();
                break;
            case 4:
                executeFullResults();
                break;

        }
    }

    private void setDialogForCalendar() {
        dialogTitle.setText(R.string.calendar);
        raceInfoLayout.setVisibility(View.GONE);
        raceTypeChoiceLayout.setVisibility(View.GONE);
        switch (categoryType) {
            case 0: //F2
                dialogPage.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.formula_2));
                dialogTitle.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.formula_2));
                titleLayout.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.formula_2));
                break;
            case 1: //F3
                dialogPage.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.ferrari_secondary));
                dialogTitle.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.ferrari_secondary));
                titleLayout.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.ferrari_secondary));
                break;
        }
    }

    private void setDialogForEntryList() {
        dialogTitle.setText(R.string.entry_list);
        raceInfoLayout.setVisibility(View.GONE);
        raceTypeChoiceLayout.setVisibility(View.GONE);
        switch (categoryType) {
            case 0: //F2
                dialogPage.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.formula_2));
                dialogTitle.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.formula_2));
                titleLayout.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.formula_2));
                break;
            case 1: //F3
                dialogPage.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.ferrari_secondary));
                dialogTitle.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.ferrari_secondary));
                titleLayout.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.ferrari_secondary));
                break;
        }
    }

    private void executeEntryList() {
        MutableLiveData<Result> entryListLiveData = juniorCategoryViewModel.getEntryList(categoryType);
        entryListLiveData.observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.Loading) {
                return;
            }
            if (result.isSuccess()) {
                Log.i(TAG, "ENTRY LIST SUCCESS");
                JuniorEntryList entryList = ((Result.JuniorEntryListSuccess) result).getData();

                if (entryList == null) {
                    Log.i(TAG, "ENTRY LIST NULL");
                } else {
                    juniorRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
                    JuniorEntryListRecyclerAdapter juniorEntryListRecyclerAdapter = new JuniorEntryListRecyclerAdapter(requireContext(), entryList, categoryType);
                    juniorRecyclerView.setAdapter(juniorEntryListRecyclerAdapter);

                }
            }
        });
    }

    private void executeCalendar() {
        Log.i(TAG, "CALENDAR: " + categoryType);
        MutableLiveData<Result> calendarLiveData = juniorCategoryViewModel.getCalendar(categoryType);
        calendarLiveData.observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.Loading) {
                return;
            }
            if (result.isSuccess()) {
                Log.i(TAG, "CALENDAR SUCCESS");
                JuniorCalendar calendar = ((Result.JuniorCalendarSuccess) result).getData();

                if (calendar == null) {
                    Log.i(TAG, "CALENDAR NULL");
                } else {
                    juniorRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
                    JuniorCalendarRecyclerAdapter juniorCalendarRecyclerAdapter = new JuniorCalendarRecyclerAdapter(requireContext(), calendar);
                    juniorRecyclerView.setAdapter(juniorCalendarRecyclerAdapter);

                }
            }
        });
    }

    private void executeDriversStanding() {
    }

    private void executeConstructorsStanding() {
    }

    private void executeFullResults() {
       //show full results
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
