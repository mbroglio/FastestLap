package com.the_coffe_coders.fastestlap.ui.junior.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.adapter.junior.JuniorCalendarRecyclerAdapter;
import com.the_coffe_coders.fastestlap.adapter.junior.JuniorEntryListRecyclerAdapter;
import com.the_coffe_coders.fastestlap.adapter.junior.JuniorResultsRecyclerAdapter;
import com.the_coffe_coders.fastestlap.adapter.junior.JuniorStandingsRecyclerAdapter;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.junior.calendar.JuniorCalendar;
import com.the_coffe_coders.fastestlap.domain.junior.result.FeatureRace;
import com.the_coffe_coders.fastestlap.domain.junior.result.SprintRace;
import com.the_coffe_coders.fastestlap.domain.junior.standings.JuniorConstructorStandings;
import com.the_coffe_coders.fastestlap.domain.junior.standings.JuniorDriverStandings;
import com.the_coffe_coders.fastestlap.domain.junior.standings.JuniorEntryList;
import com.the_coffe_coders.fastestlap.ui.junior.viewmodel.JuniorCategoryViewModel;
import com.the_coffe_coders.fastestlap.ui.junior.viewmodel.JuniorCategoryViewModelFactory;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

public class JuniorDialogFragment extends DialogFragment {
    private static final String TAG = "JuniorDialogFragment";
    String circuit;
    FeatureRace featureRace;
    SprintRace sprintRace;
    private int categoryType, content, raceType;
    private MaterialCardView dialogPage;
    private LinearLayout raceInfoLayout, titleLayout;
    private TextView dialogTitle, raceTypeTitle, driverNamePole, driverNameFastestLap;
    private RelativeLayout fastestLapLayout, polePositionLayout;
    private JuniorCategoryViewModel juniorCategoryViewModel;
    private RecyclerView juniorRecyclerView;
    private Button closeButton;


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
            circuit = getArguments().getString("CIRCUIT");
            featureRace = androidx.core.os.BundleCompat.getParcelable(getArguments(), "JUNIOR_FEATURE_RACE", FeatureRace.class);
            sprintRace = androidx.core.os.BundleCompat.getParcelable(getArguments(), "JUNIOR_SPRINT_RACE", SprintRace.class);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_junior_dialog, container, false);

        juniorCategoryViewModel = new ViewModelProvider(this, new JuniorCategoryViewModelFactory(requireActivity().getApplication())).get(JuniorCategoryViewModel.class);

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

        switch (categoryType) {
            case 0: //F2
                dialogPage.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.formula_2));
                dialogTitle.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.formula_2));
                titleLayout.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.formula_2));
                closeButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.formula_2));
                break;
            case 1: //F3
                dialogPage.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.app_primary_red));
                dialogTitle.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.app_primary_red));
                titleLayout.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.app_primary_red));
                closeButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.app_primary_red));
                break;
        }

        closeButton.setOnClickListener(v -> dismiss());

        try {
            executeFunctions();
        } catch (Exception e) {
            dismiss();
        }


        return view;
    }

    private void executeFunctions() {
        switch (content) {
            case 0: // Entry list
                setDialogForEntryList();
                executeEntryList();
                break;
            case 1: // Calendar
                setDialogForCalendar();
                executeCalendar();
                break;
            case 2: // Drivers standing
                setDialogForDriversStanding();
                executeDriversStanding();
                break;
            case 3: // Constructors standing
                setDialogForConstructorsStanding();
                executeConstructorsStanding();
                break;
            case 4:
                setDialogForResults();
                executeFullResults();
                break;

        }
    }

    private void setDialogForCalendar() {
        UIUtils.singleSetTextViewText(ContextCompat.getString(requireContext(), R.string.calendar), dialogTitle);
        raceInfoLayout.setVisibility(View.GONE);
        raceTypeTitle.setVisibility(View.GONE);
    }

    private void setDialogForEntryList() {
        UIUtils.singleSetTextViewText(ContextCompat.getString(requireContext(), R.string.entry_list), dialogTitle);
        raceInfoLayout.setVisibility(View.GONE);
        raceTypeTitle.setVisibility(View.GONE);
    }

    private void setDialogForDriversStanding() {
        UIUtils.singleSetTextViewText(ContextCompat.getString(requireContext(), R.string.drivers), dialogTitle);
        raceInfoLayout.setVisibility(View.GONE);
        raceTypeTitle.setVisibility(View.GONE);
    }

    private void setDialogForConstructorsStanding() {
        UIUtils.singleSetTextViewText(ContextCompat.getString(requireContext(), R.string.constructors), dialogTitle);
        raceInfoLayout.setVisibility(View.GONE);
        raceTypeTitle.setVisibility(View.GONE);
    }

    private void setDialogForResults() {
        UIUtils.singleSetTextViewText(circuit, dialogTitle);
        raceInfoLayout.setVisibility(View.VISIBLE);
        fastestLapLayout.setVisibility(View.VISIBLE);

        if (featureRace == null) {
            polePositionLayout.setVisibility(View.GONE);
            UIUtils.multipleSetTextViewText(
                    new String[]{
                            ContextCompat.getString(requireContext(), R.string.sprint),
                            sprintRace.getFastest_lap()
                    },
                    new TextView[]{
                            raceTypeTitle,
                            driverNameFastestLap});
        } else {
            polePositionLayout.setVisibility(View.VISIBLE);
            UIUtils.multipleSetTextViewText(
                    new String[]{
                            ContextCompat.getString(requireContext(), R.string.feature),
                            featureRace.getPole_position(),
                            featureRace.getFastest_lap()
                    },
                    new TextView[]{
                            raceTypeTitle,
                            driverNamePole,
                            driverNameFastestLap});
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
                    dismiss();
                } else {
                    if (entryList.getTeams().isEmpty()) {
                        Log.i(TAG, "ENTRY LIST EMPTY");
                        Toast.makeText(requireContext(), ContextCompat.getString(requireContext(), R.string.content_not_available), Toast.LENGTH_SHORT).show();
                        dismiss();
                    } else {
                        juniorRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
                        JuniorEntryListRecyclerAdapter juniorEntryListRecyclerAdapter = new JuniorEntryListRecyclerAdapter(requireContext(), entryList, categoryType);
                        juniorRecyclerView.setAdapter(juniorEntryListRecyclerAdapter);
                    }
                }
            } else {
                Toast.makeText(requireContext(), ContextCompat.getString(requireContext(), R.string.content_not_available), Toast.LENGTH_SHORT).show();
                dismiss();
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
                    Toast.makeText(requireContext(), ContextCompat.getString(requireContext(), R.string.content_not_available), Toast.LENGTH_SHORT).show();
                    dismiss();
                } else {
                    juniorRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
                    JuniorCalendarRecyclerAdapter juniorCalendarRecyclerAdapter = new JuniorCalendarRecyclerAdapter(requireContext(), calendar);
                    juniorRecyclerView.setAdapter(juniorCalendarRecyclerAdapter);

                }
            } else {
                Toast.makeText(requireContext(), ContextCompat.getString(requireContext(), R.string.content_not_available), Toast.LENGTH_SHORT).show();
                dismiss();
            }
        });
    }

    private void executeDriversStanding() {
        MutableLiveData<Result> standingsLiveData = juniorCategoryViewModel.getDriverStandings(categoryType);
        standingsLiveData.observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.Loading) {
                return;
            }
            if (result.isSuccess()) {
                Log.i(TAG, "DRIVERS STANDINGS SUCCESS");
                JuniorDriverStandings driverStandings = ((Result.JuniorDriverStandingsSuccess) result).getData();
                Log.i(TAG, "DRIVERS STANDINGS: " + driverStandings);
                if (driverStandings == null) {
                    Log.i(TAG, "DRIVERS STANDINGS NULL");
                    Toast.makeText(requireContext(), ContextCompat.getString(requireContext(), R.string.content_not_available), Toast.LENGTH_SHORT).show();
                    dismiss();
                } else {
                    juniorRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
                    JuniorStandingsRecyclerAdapter juniorStandingsRecyclerAdapter = new JuniorStandingsRecyclerAdapter(requireContext(), driverStandings);
                    juniorRecyclerView.setAdapter(juniorStandingsRecyclerAdapter);
                }
            } else {
                Toast.makeText(requireContext(), ContextCompat.getString(requireContext(), R.string.content_not_available), Toast.LENGTH_SHORT).show();
                dismiss();
            }
        });

    }

    private void executeConstructorsStanding() {
        MutableLiveData<Result> standingsLiveData = juniorCategoryViewModel.getConstructorStandings(categoryType);
        standingsLiveData.observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.Loading) {
                return;
            }
            if (result.isSuccess()) {
                Log.i(TAG, "CONSTRUCTORS STANDINGS SUCCESS");
                JuniorConstructorStandings constructorStandings = ((Result.JuniorConstructorStandingsSuccess) result).getData();
                Log.i(TAG, "CONSTRUCTORS STANDINGS: " + constructorStandings);
                if (constructorStandings == null) {
                    Log.i(TAG, "CONSTRUCTORS STANDINGS NULL");
                    Toast.makeText(requireContext(), ContextCompat.getString(requireContext(), R.string.content_not_available), Toast.LENGTH_SHORT).show();
                    dismiss();
                } else {
                    juniorRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
                    JuniorStandingsRecyclerAdapter juniorStandingsRecyclerAdapter = new JuniorStandingsRecyclerAdapter(requireContext(), constructorStandings);
                    juniorRecyclerView.setAdapter(juniorStandingsRecyclerAdapter);
                }
            } else {
                Toast.makeText(requireContext(), ContextCompat.getString(requireContext(), R.string.content_not_available), Toast.LENGTH_SHORT).show();
                dismiss();
            }
        });

    }

    private void executeFullResults() {
        juniorRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        JuniorResultsRecyclerAdapter juniorResultsRecyclerAdapter;

        if (featureRace != null) {
            juniorResultsRecyclerAdapter = new JuniorResultsRecyclerAdapter(requireContext(), featureRace, getParentFragmentManager());
        } else {
            juniorResultsRecyclerAdapter = new JuniorResultsRecyclerAdapter(requireContext(), sprintRace, getParentFragmentManager());
        }

        juniorRecyclerView.setAdapter(juniorResultsRecyclerAdapter);
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
