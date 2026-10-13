package com.the_coffe_coders.fastestlap.ui.bio.fragment;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.f1.constructor.ConstructorSeasonStats;
import com.the_coffe_coders.fastestlap.domain.f1.constructor.EventResultConstructor;
import com.the_coffe_coders.fastestlap.domain.f1.driver.DriverSeasonStats;
import com.the_coffe_coders.fastestlap.domain.f1.driver.EventResultDriver;
import com.the_coffe_coders.fastestlap.util.chart.SeasonPerformancePoint;
import com.the_coffe_coders.fastestlap.util.chart.SeasonProgressionChartView;
import com.the_coffe_coders.fastestlap.util.chart.StickyYAxisView;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.ui.LoadingScreen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SeasonPerformanceChartFragmentTab extends Fragment {

    public static final String TAG = "SeasonPerfChartFragment";

    private static final String ARG_IS_CONSTRUCTOR = "arg_is_constructor";
    private static final String ARG_ENTITY_ID = "arg_entity_id";
    private static final String ARG_ENTITY_NAME = "arg_entity_name";
    private static final String ARG_TEAM_ID = "arg_team_id";
    private static final String ARG_TEAM_COLOR = "arg_team_color";
    private static final String ARG_DRIVER_ONE_NAME = "arg_driver_one_name";
    private static final String ARG_DRIVER_TWO_NAME = "arg_driver_two_name";
    private static final String ARG_DRIVER_SEASON_STATS = "arg_driver_season_stats";
    private static final String ARG_CONSTRUCTOR_SEASON_STATS = "arg_constructor_season_stats";

    private boolean isConstructor;
    private String entityId;
    private String entityName;
    private String teamId;
    private int teamColor;
    private String driverOneName;
    private String driverTwoName;

    private DriverSeasonStats driverSeasonStats;
    private ConstructorSeasonStats constructorSeasonStats;
    private final Map<Integer, String> roundRaceNames = new HashMap<>();

    private boolean isRace = true;
    private boolean isQualifying = false;
    private boolean isSprint = false;

    private StickyYAxisView stickyYAxisView;
    private SeasonProgressionChartView chartView;
    private ProgressBar loadingProgress;
    private TextView emptyMessage;
    private MaterialButtonToggleGroup toggleButtonGroup;
    private MaterialButton btnToggleRaces;
    private MaterialButton btnToggleQualifyings;
    private MaterialButton btnToggleSprints;
    private LoadingScreen loadingScreen;

    public SeasonPerformanceChartFragmentTab() {
        // Required empty public constructor
    }

    public static SeasonPerformanceChartFragmentTab newInstanceForDriver(String driverId,
                                                                         String driverName,
                                                                         String teamId,
                                                                         int teamColor,
                                                                         DriverSeasonStats seasonStats) {
        SeasonPerformanceChartFragmentTab fragment = new SeasonPerformanceChartFragmentTab();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_CONSTRUCTOR, false);
        args.putString(ARG_ENTITY_ID, driverId);
        args.putString(ARG_ENTITY_NAME, driverName);
        args.putString(ARG_TEAM_ID, teamId);
        args.putInt(ARG_TEAM_COLOR, teamColor);
        if (seasonStats != null) {
            args.putParcelable(ARG_DRIVER_SEASON_STATS, seasonStats);
        }
        fragment.setArguments(args);
        return fragment;
    }
    public static SeasonPerformanceChartFragmentTab newInstanceForConstructor(String constructorId,
                                                                              String constructorName,
                                                                              int teamColor,
                                                                              String driverOneName,
                                                                              String driverTwoName,
                                                                              ConstructorSeasonStats seasonStats) {
        SeasonPerformanceChartFragmentTab fragment = new SeasonPerformanceChartFragmentTab();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_CONSTRUCTOR, true);
        args.putString(ARG_ENTITY_ID, constructorId);
        args.putString(ARG_ENTITY_NAME, constructorName);
        args.putString(ARG_TEAM_ID, constructorId);
        args.putInt(ARG_TEAM_COLOR, teamColor);
        args.putString(ARG_DRIVER_ONE_NAME, driverOneName);
        args.putString(ARG_DRIVER_TWO_NAME, driverTwoName);
        if (seasonStats != null) {
            args.putParcelable(ARG_CONSTRUCTOR_SEASON_STATS, seasonStats);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            isConstructor = getArguments().getBoolean(ARG_IS_CONSTRUCTOR, false);
            entityId = getArguments().getString(ARG_ENTITY_ID, "");
            entityName = getArguments().getString(ARG_ENTITY_NAME, "");
            teamId = getArguments().getString(ARG_TEAM_ID, "");
            teamColor = getArguments().getInt(ARG_TEAM_COLOR, ContextCompat.getColor(requireContext(), R.color.app_primary_red));
            driverOneName = getArguments().getString(ARG_DRIVER_ONE_NAME, "");
            driverTwoName = getArguments().getString(ARG_DRIVER_TWO_NAME, "");
            driverSeasonStats = getArguments().getParcelable(ARG_DRIVER_SEASON_STATS);
            constructorSeasonStats = getArguments().getParcelable(ARG_CONSTRUCTOR_SEASON_STATS);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_season_performance_chart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        stickyYAxisView = view.findViewById(R.id.sticky_y_axis_view);
        chartView = view.findViewById(R.id.season_progression_chart_view);
        loadingProgress = view.findViewById(R.id.chart_loading_progress);
        emptyMessage = view.findViewById(R.id.chart_empty_message);
        toggleButtonGroup = view.findViewById(R.id.toggle_button_group);
        btnToggleRaces = view.findViewById(R.id.btn_toggle_races);
        btnToggleQualifyings = view.findViewById(R.id.btn_toggle_qualifyings);
        btnToggleSprints = view.findViewById(R.id.btn_toggle_sprints);


        View contentLayout = view.findViewById(R.id.chart_main_layout);
        loadingScreen = new LoadingScreen(view, requireContext(), null, contentLayout);
        loadingScreen.showLoadingScreen(false);
        loadingScreen.updateProgress();

        // Configure chart and axis views
        chartView.setTeamColor(teamColor);
        stickyYAxisView.setGeometry(chartView.getTopPadding(), chartView.getBottomPadding(), chartView.getTotalPositions());

        setupToggleButtons();
        fetchCalendarAndData();
    }

    private void setupToggleButtons() {
        toggleButtonGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                isRace = (checkedId == R.id.btn_toggle_races);
                isQualifying = (checkedId == R.id.btn_toggle_qualifyings);
                isSprint = (checkedId == R.id.btn_toggle_sprints);
                updateToggleStyles();
                renderChart();
            }
        });
        updateToggleStyles();
    }

    private void updateToggleStyles() {
        if (getContext() == null || btnToggleRaces == null || btnToggleQualifyings == null || btnToggleSprints == null) return;

        if (isQualifying) {
            btnToggleQualifyings.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.card_surface_elevated)));
            btnToggleQualifyings.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            btnToggleRaces.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.card_surface_dark)));
            btnToggleRaces.setTextColor(ContextCompat.getColor(requireContext(), R.color.timer_gray));
            btnToggleSprints.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.card_surface_dark)));
            btnToggleSprints.setTextColor(ContextCompat.getColor(requireContext(), R.color.timer_gray));
        } else if(isRace) {
            btnToggleRaces.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.card_surface_elevated)));
            btnToggleRaces.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            btnToggleQualifyings.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.card_surface_dark)));
            btnToggleQualifyings.setTextColor(ContextCompat.getColor(requireContext(), R.color.timer_gray));
            btnToggleSprints.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.card_surface_dark)));
            btnToggleSprints.setTextColor(ContextCompat.getColor(requireContext(), R.color.timer_gray));
        }else if(isSprint){
            btnToggleSprints.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.card_surface_elevated)));
            btnToggleSprints.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            btnToggleRaces.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.card_surface_dark)));
            btnToggleRaces.setTextColor(ContextCompat.getColor(requireContext(), R.color.timer_gray));
            btnToggleQualifyings.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.card_surface_dark)));
            btnToggleQualifyings.setTextColor(ContextCompat.getColor(requireContext(), R.color.timer_gray));
        }else{
            btnToggleRaces.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.card_surface_dark)));
            btnToggleRaces.setTextColor(ContextCompat.getColor(requireContext(), R.color.timer_gray));
            btnToggleRaces.setClickable(false);
            btnToggleQualifyings.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.card_surface_dark)));
            btnToggleQualifyings.setTextColor(ContextCompat.getColor(requireContext(), R.color.timer_gray));
            btnToggleQualifyings.setClickable(false);
            btnToggleSprints.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.card_surface_dark)));
            btnToggleSprints.setTextColor(ContextCompat.getColor(requireContext(), R.color.timer_gray));
            btnToggleSprints.setClickable(false);
        }
    }

    private void fetchCalendarAndData() {
        if (loadingProgress != null) {
            loadingProgress.setVisibility(View.GONE);
        }
        emptyMessage.setVisibility(View.GONE);

        chartView.setTotalRaces(Constants.CALENDAR_RACES);

        renderChart();
    }

    private void renderChart() {
        List<SeasonPerformancePoint> primaryPoints = new ArrayList<>();
        List<SeasonPerformancePoint> secondaryPoints = new ArrayList<>();

        if (isConstructor) {
            extractPointsFromConstructorStats(constructorSeasonStats, primaryPoints, secondaryPoints);
        } else {
            extractPointsFromDriverStats(driverSeasonStats, primaryPoints);
        }

        if (primaryPoints.isEmpty() && secondaryPoints.isEmpty()) {
            showEmptyState();
        } else {
            if (loadingProgress != null) {
                loadingProgress.setVisibility(View.GONE);
            }
            emptyMessage.setVisibility(View.GONE);
            if(isRace){
                chartView.setData(primaryPoints, secondaryPoints, isConstructor, 0);
            }else if (isQualifying){
                chartView.setData(primaryPoints, secondaryPoints, isConstructor, 1);
            }else if (isSprint){
                chartView.setData(primaryPoints, secondaryPoints, isConstructor, 2);
            }else{
                chartView.setData(primaryPoints, secondaryPoints, isConstructor, 0);
            }

            chartView.setVisibility(View.VISIBLE);
            stickyYAxisView.setVisibility(View.VISIBLE);
            emptyMessage.setVisibility(View.GONE);

            if (loadingScreen != null) {
                loadingScreen.hideLoadingScreenImmediately();
            }
        }
    }

    private void extractPointsFromDriverStats(DriverSeasonStats dss, List<SeasonPerformancePoint> points) {
        if (dss == null || dss.getSeason_results() == null) return;
        ArrayList<EventResultDriver> list = dss.getSeason_results();

        for (int i = 0; i < list.size(); i++) {
            EventResultDriver res = list.get(i);
            Log.i(TAG, "extractPointsFromDriverStats: " + res);

            if (res == null) continue;

            int roundNum = list.get(0) == null ? i : i + 1;

            String position = null;
            if(isRace){
                position = res.getRace();
            }else if(isQualifying){
                position = res.getQualifying();
            }else if(isSprint){
                position = res.getSprint_race();
            }

            String raceName = roundRaceNames.get(roundNum);
            SeasonPerformancePoint pt = createPointFromPosStr(roundNum, raceName, position, entityId, entityName);
            if (pt != null) {
                points.add(pt);
            }
        }
    }

    private void extractPointsFromConstructorStats(ConstructorSeasonStats css,
                                                   List<SeasonPerformancePoint> primaryPoints,
                                                   List<SeasonPerformancePoint> secondaryPoints) {

        if (css == null || css.getSeason_results() == null) return;
        ArrayList<EventResultConstructor> list = css.getSeason_results();
        String name1 = driverOneName != null && !driverOneName.isEmpty() ? driverOneName : requireContext().getString(R.string.not_found);
        String name2 = driverTwoName != null && !driverTwoName.isEmpty() ? driverTwoName : requireContext().getString(R.string.not_found);

        for (int i = 0; i < list.size(); i++) {
            EventResultConstructor res = list.get(i);
            if (res == null) continue;

            int roundNum = list.get(0) == null ? i : i + 1;

            String raceName = roundRaceNames.get(roundNum);
            String position1 = null;
            String position2 = null;

            if(isRace){
                position1 = res.getRace_driver_1();
                position2 = res.getRace_driver_2();
            }else if(isQualifying){
                position1 = res.getQualifying_driver_1();
                position2 = res.getQualifying_driver_2();
            }else if(isSprint){
                position1 = res.getSprint_race_driver_1();
                position2 = res.getSprint_race_driver_2();
            }

            SeasonPerformancePoint pt1 = createPointFromPosStr(roundNum, raceName, position1, "d1", name1);
            SeasonPerformancePoint pt2 = createPointFromPosStr(roundNum, raceName, position2, "d2", name2);

            if (pt1 != null) {
                primaryPoints.add(pt1);
            }
            if (pt2 != null) {
                secondaryPoints.add(pt2);
            }
        }
    }

    @Nullable
    private SeasonPerformancePoint createPointFromPosStr(int roundNum, String raceName, String posStr,
                                                          String pointEntityId, String pointEntityName) {
        if (posStr == null || posStr.trim().isEmpty() || "-".equals(posStr.trim())) {
            return null;
        }

        String trimmed = posStr.trim().toLowerCase();
        String lower = trimmed.toLowerCase();

        boolean isDnf = "r".equalsIgnoreCase(lower) || "d".equalsIgnoreCase(lower)
                || "w".equalsIgnoreCase(lower) || "nc".equalsIgnoreCase(lower)
                || "dnf".equalsIgnoreCase(lower) || "dns".equalsIgnoreCase(lower)
                || "dsq".equalsIgnoreCase(lower)
                || lower.contains("retired") || lower.contains("accident")
                || lower.contains("collision") || lower.contains("did not");

        int parsedPos;
        if (isDnf) {
            parsedPos = chartView.getTotalPositions(); // Position 23 (DNF)
        } else {
            try {
                parsedPos = Integer.parseInt(trimmed);
                if (parsedPos <= 0 || parsedPos > 22) {
                    parsedPos = chartView.getTotalPositions();
                    isDnf = true;
                }
            } catch (NumberFormatException e) {
                parsedPos = chartView.getTotalPositions();
                isDnf = true;
            }
        }

        String displayPosText = isDnf ? "DNF" : String.valueOf(parsedPos);

        if(isRace){
            return new SeasonPerformancePoint(roundNum, "R" + roundNum, parsedPos, displayPosText, isDnf, 0, raceName, pointEntityId, pointEntityName);
        }else if(isQualifying){
            return new SeasonPerformancePoint(roundNum, "Q" + roundNum, parsedPos, displayPosText, isDnf, 1, raceName, pointEntityId, pointEntityName);
        }else{
            return new SeasonPerformancePoint(roundNum, "S" + roundNum, parsedPos, displayPosText, isDnf, 2, raceName, pointEntityId, pointEntityName);
        }
    }

    private void showEmptyState() {
        if (loadingProgress != null) {
            loadingProgress.setVisibility(View.GONE);
        }
        emptyMessage.setVisibility(View.VISIBLE);
        chartView.setVisibility(View.GONE);
        stickyYAxisView.setVisibility(View.GONE);
        if (loadingScreen != null) {
            loadingScreen.hideLoadingScreenImmediately();
        }
    }
}
