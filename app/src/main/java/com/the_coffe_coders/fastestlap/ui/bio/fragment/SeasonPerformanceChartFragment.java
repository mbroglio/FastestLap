package com.the_coffe_coders.fastestlap.ui.bio.fragment;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.f1.constructor.ConstructorSeasonStats;
import com.the_coffe_coders.fastestlap.domain.f1.constructor.EventResultConstructor;
import com.the_coffe_coders.fastestlap.domain.f1.driver.DriverSeasonStats;
import com.the_coffe_coders.fastestlap.domain.f1.driver.EventResultDriver;
import com.the_coffe_coders.fastestlap.ui.bio.chart.SeasonPerformancePoint;
import com.the_coffe_coders.fastestlap.ui.bio.chart.SeasonProgressionChartView;
import com.the_coffe_coders.fastestlap.ui.bio.chart.StickyYAxisView;
import com.the_coffe_coders.fastestlap.util.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SeasonPerformanceChartFragment extends DialogFragment {

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

    private boolean isQualifyingMode = false;

    private StickyYAxisView stickyYAxisView;
    private SeasonProgressionChartView chartView;
    private ProgressBar loadingProgress;
    private TextView emptyMessage;
    private MaterialButtonToggleGroup toggleButtonGroup;
    private MaterialButton btnToggleRaces;
    private MaterialButton btnToggleQualifyings;

    private View legendFinishDot;
    private TextView legendFinishText;
    private View legendSecondaryDot;
    private TextView legendSecondaryText;

    public static SeasonPerformanceChartFragment newInstanceForDriver(String driverId,
                                                                      String driverName,
                                                                      String teamId,
                                                                      int teamColor) {
        return newInstanceForDriver(driverId, driverName, teamId, teamColor, null);
    }

    public static SeasonPerformanceChartFragment newInstanceForDriver(String driverId,
                                                                      String driverName,
                                                                      String teamId,
                                                                      int teamColor,
                                                                      @Nullable DriverSeasonStats seasonStats) {
        SeasonPerformanceChartFragment fragment = new SeasonPerformanceChartFragment();
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

    public static SeasonPerformanceChartFragment newInstanceForConstructor(String constructorId,
                                                                           String constructorName,
                                                                           int teamColor) {
        return newInstanceForConstructor(constructorId, constructorName, teamColor, null, null, null);
    }

    public static SeasonPerformanceChartFragment newInstanceForConstructor(String constructorId,
                                                                           String constructorName,
                                                                           int teamColor,
                                                                           @Nullable String driverOneName,
                                                                           @Nullable String driverTwoName,
                                                                           @Nullable ConstructorSeasonStats seasonStats) {
        SeasonPerformanceChartFragment fragment = new SeasonPerformanceChartFragment();
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
            driverOneName = getArguments().getString(ARG_DRIVER_ONE_NAME, "Pilota 1");
            driverTwoName = getArguments().getString(ARG_DRIVER_TWO_NAME, "Pilota 2");
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

        legendFinishDot = view.findViewById(R.id.legend_finish_dot);
        legendFinishText = view.findViewById(R.id.legend_finish_text);
        legendSecondaryDot = view.findViewById(R.id.legend_secondary_dot);
        legendSecondaryText = view.findViewById(R.id.legend_secondary_text);

        // Configure chart and axis views
        chartView.setTeamColor(teamColor);
        stickyYAxisView.setGeometry(chartView.getTopPadding(), chartView.getBottomPadding(), chartView.getTotalPositions());

        setupLegend();
        setupToggleButtons();
        fetchCalendarAndData();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void setupLegend() {
        if (isConstructor) {
            legendFinishDot.setBackgroundTintList(ColorStateList.valueOf(chartView.getTeamColor()));
            legendFinishText.setText(driverOneName != null && !driverOneName.isEmpty() ? driverOneName : "Pilota 1");

            legendSecondaryDot.setVisibility(View.VISIBLE);
            legendSecondaryDot.setBackgroundTintList(ColorStateList.valueOf(chartView.getSecondaryTeamColor()));
            legendSecondaryText.setVisibility(View.VISIBLE);
            legendSecondaryText.setText(driverTwoName != null && !driverTwoName.isEmpty() ? driverTwoName : "Pilota 2");
        } else {
            legendFinishDot.setBackgroundTintList(ColorStateList.valueOf(chartView.getTeamColor()));
            legendFinishText.setText(isQualifyingMode ? "Qualifica" : "Gara (GP)");

            legendSecondaryDot.setVisibility(View.GONE);
            legendSecondaryText.setVisibility(View.GONE);
        }
    }

    private void setupToggleButtons() {
        toggleButtonGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                isQualifyingMode = (checkedId == R.id.btn_toggle_qualifyings);
                updateToggleStyles();
                setupLegend();
                renderChartFromCurrentData();
            }
        });
        updateToggleStyles();
    }

    private void updateToggleStyles() {
        if (getContext() == null || btnToggleRaces == null || btnToggleQualifyings == null) return;

        if (isQualifyingMode) {
            btnToggleQualifyings.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.card_surface_elevated)));
            btnToggleQualifyings.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            btnToggleRaces.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.card_surface_dark)));
            btnToggleRaces.setTextColor(ContextCompat.getColor(requireContext(), R.color.timer_gray));
        } else {
            btnToggleRaces.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.card_surface_elevated)));
            btnToggleRaces.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            btnToggleQualifyings.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.card_surface_dark)));
            btnToggleQualifyings.setTextColor(ContextCompat.getColor(requireContext(), R.color.timer_gray));
        }
    }

    private void fetchCalendarAndData() {
        loadingProgress.setVisibility(View.VISIBLE);
        emptyMessage.setVisibility(View.GONE);

        FirebaseDatabase db = FirebaseDatabase.getInstance(Constants.FIREBASE_REALTIME_DATABASE);

        // Fetch calendar rounds count & race names
        db.getReference("app_config/db_calendar/f1").get().addOnCompleteListener(calTask -> {
            int totalRaces = 24;
            if (calTask.isSuccessful() && calTask.getResult() != null && calTask.getResult().exists()) {
                long count = calTask.getResult().getChildrenCount();
                if (count > 0) {
                    totalRaces = (int) count;
                }
                for (DataSnapshot rSnap : calTask.getResult().getChildren()) {
                    try {
                        int rNum = Integer.parseInt(rSnap.getKey());
                        String name = rSnap.child("race_name").getValue(String.class);
                        if (name == null) name = rSnap.child("name").getValue(String.class);
                        if (name != null) roundRaceNames.put(rNum, name);
                    } catch (Exception ignored) {}
                }
            }
            chartView.setTotalRaces(totalRaces);

            // If season_stats was already passed and has season_results, render immediately!
            if (hasSeasonResultsAvailable()) {
                loadingProgress.setVisibility(View.GONE);
                renderChartFromCurrentData();
            } else {
                fetchSeasonStatsFromFirebase(db);
            }
        });
    }

    private boolean hasSeasonResultsAvailable() {
        if (isConstructor) {
            return constructorSeasonStats != null
                    && constructorSeasonStats.getSeason_results() != null
                    && !constructorSeasonStats.getSeason_results().isEmpty();
        } else {
            return driverSeasonStats != null
                    && driverSeasonStats.getSeason_results() != null
                    && !driverSeasonStats.getSeason_results().isEmpty();
        }
    }

    private void fetchSeasonStatsFromFirebase(FirebaseDatabase db) {
        String path = isConstructor ? "teams/" + entityId + "/season_stats" : "drivers/" + entityId + "/season_stats";
        DatabaseReference statsRef = db.getReference(path);

        statsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                if (snapshot.exists()) {
                    if (isConstructor) {
                        constructorSeasonStats = snapshot.getValue(ConstructorSeasonStats.class);
                    } else {
                        driverSeasonStats = snapshot.getValue(DriverSeasonStats.class);
                    }
                }

                if (hasSeasonResultsAvailable()) {
                    loadingProgress.setVisibility(View.GONE);
                    renderChartFromCurrentData();
                } else {
                    // Fallback to stats_tracker/race_updates
                    fetchRaceUpdatesFallback(db);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                fetchRaceUpdatesFallback(db);
            }
        });
    }

    private void renderChartFromCurrentData() {
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
            emptyMessage.setVisibility(View.GONE);
            chartView.setData(primaryPoints, secondaryPoints, isConstructor);
        }
    }

    private void extractPointsFromDriverStats(DriverSeasonStats dss, List<SeasonPerformancePoint> points) {
        if (dss == null || dss.getSeason_results() == null) return;
        Map<String, EventResultDriver> map = dss.getSeason_results();

        for (Map.Entry<String, EventResultDriver> entry : map.entrySet()) {
            int roundNum;
            try {
                roundNum = Integer.parseInt(entry.getKey());
            } catch (NumberFormatException e) {
                continue;
            }
            EventResultDriver res = entry.getValue();
            if (res == null) continue;

            String posStr = isQualifyingMode ? res.getQualifying() : res.getRace();
            if (!isQualifyingMode && (posStr == null || posStr.isEmpty())) {
                posStr = res.getSprint_race();
            }

            String raceName = roundRaceNames.getOrDefault(roundNum, "Round " + roundNum);
            SeasonPerformancePoint pt = createPointFromPosStr(roundNum, raceName, posStr, entityId, entityName);
            if (pt != null) {
                points.add(pt);
            }
        }
    }

    private void extractPointsFromConstructorStats(ConstructorSeasonStats css,
                                                   List<SeasonPerformancePoint> primaryPoints,
                                                   List<SeasonPerformancePoint> secondaryPoints) {
        if (css == null || css.getSeason_results() == null) return;
        Map<String, EventResultConstructor> map = css.getSeason_results();
        String name1 = driverOneName != null && !driverOneName.isEmpty() ? driverOneName : "Pilota 1";
        String name2 = driverTwoName != null && !driverTwoName.isEmpty() ? driverTwoName : "Pilota 2";

        for (Map.Entry<String, EventResultConstructor> entry : map.entrySet()) {
            int roundNum;
            try {
                roundNum = Integer.parseInt(entry.getKey());
            } catch (NumberFormatException e) {
                continue;
            }
            EventResultConstructor res = entry.getValue();
            if (res == null) continue;

            String raceName = roundRaceNames.getOrDefault(roundNum, "Round " + roundNum);

            String pos1 = isQualifyingMode ? res.getQualifying_driver_1() : res.getRace_driver_1();
            if (!isQualifyingMode && (pos1 == null || pos1.isEmpty())) {
                pos1 = res.getSprint_race_driver_1();
            }
            SeasonPerformancePoint pt1 = createPointFromPosStr(roundNum, raceName, pos1, "d1", name1);
            if (pt1 != null) {
                primaryPoints.add(pt1);
            }

            String pos2 = isQualifyingMode ? res.getQualifying_driver_2() : res.getRace_driver_2();
            if (!isQualifyingMode && (pos2 == null || pos2.isEmpty())) {
                pos2 = res.getSprint_race_driver_2();
            }
            SeasonPerformancePoint pt2 = createPointFromPosStr(roundNum, raceName, pos2, "d2", name2);
            if (pt2 != null) {
                secondaryPoints.add(pt2);
            }
        }
    }

    private void fetchRaceUpdatesFallback(FirebaseDatabase db) {
        DatabaseReference updatesRef = db.getReference("app_config/stats_tracker/race_updates");
        updatesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                loadingProgress.setVisibility(View.GONE);

                if (!snapshot.exists()) {
                    showEmptyState();
                    return;
                }

                DataSnapshot targetSnapshot = snapshot;
                for (DataSnapshot child : snapshot.getChildren()) {
                    String key = child.getKey();
                    if (key != null && key.length() == 4 && (key.startsWith("202") || key.startsWith("203"))) {
                        targetSnapshot = child;
                    }
                }

                List<SeasonPerformancePoint> primaryPoints = new ArrayList<>();
                List<SeasonPerformancePoint> secondaryPoints = new ArrayList<>();

                for (DataSnapshot roundSnap : targetSnapshot.getChildren()) {
                    String roundKey = roundSnap.getKey();
                    if (roundKey == null) continue;

                    int roundNum;
                    try {
                        roundNum = Integer.parseInt(roundKey);
                    } catch (NumberFormatException e) {
                        continue;
                    }

                    String raceName = roundSnap.child("race_name").getValue(String.class);
                    if (raceName == null) raceName = roundRaceNames.getOrDefault(roundNum, "Round " + roundNum);

                    if (isConstructor) {
                        parseConstructorRoundFallback(roundSnap, roundNum, raceName, primaryPoints, secondaryPoints);
                    } else {
                        parseDriverRoundFallback(roundSnap, roundNum, raceName, primaryPoints);
                    }
                }

                if (primaryPoints.isEmpty() && secondaryPoints.isEmpty()) {
                    showEmptyState();
                } else {
                    emptyMessage.setVisibility(View.GONE);
                    chartView.setData(primaryPoints, secondaryPoints, isConstructor);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                loadingProgress.setVisibility(View.GONE);
                showEmptyState();
            }
        });
    }

    private void parseDriverRoundFallback(DataSnapshot roundSnap, int roundNum, String raceName,
                                          List<SeasonPerformancePoint> points) {
        String targetDriverId = entityId != null ? entityId.trim().toLowerCase() : "";

        DataSnapshot resultsSnap = roundSnap.child("results_snapshot");
        if (resultsSnap.exists()) {
            for (DataSnapshot rSnap : resultsSnap.getChildren()) {
                String dId = rSnap.child("driverId").getValue(String.class);
                if (dId != null && dId.trim().equalsIgnoreCase(targetDriverId)) {
                    Object posObj = rSnap.child("position").getValue();
                    String posText = rSnap.child("positionText").getValue(String.class);
                    SeasonPerformancePoint pt = createPointFromPosStr(roundNum, raceName, String.valueOf(posObj != null ? posObj : posText), entityId, entityName);
                    if (pt != null) points.add(pt);
                    return;
                }
            }
        }
    }

    private void parseConstructorRoundFallback(DataSnapshot roundSnap, int roundNum, String raceName,
                                               List<SeasonPerformancePoint> primaryPoints,
                                               List<SeasonPerformancePoint> secondaryPoints) {
        String targetTeamId = teamId != null ? teamId.trim().toLowerCase() : "";
        String name1 = driverOneName != null && !driverOneName.isEmpty() ? driverOneName : "Pilota 1";
        String name2 = driverTwoName != null && !driverTwoName.isEmpty() ? driverTwoName : "Pilota 2";

        DataSnapshot resultsSnap = roundSnap.child("results_snapshot");
        if (resultsSnap.exists()) {
            int carIndex = 0;
            for (DataSnapshot rSnap : resultsSnap.getChildren()) {
                String cId = rSnap.child("constructorId").getValue(String.class);
                if (cId != null && cId.trim().equalsIgnoreCase(targetTeamId)) {
                    Object posObj = rSnap.child("position").getValue();
                    String posText = rSnap.child("positionText").getValue(String.class);
                    String currentName = (carIndex == 0) ? name1 : name2;
                    SeasonPerformancePoint pt = createPointFromPosStr(roundNum, raceName, String.valueOf(posObj != null ? posObj : posText), "d" + (carIndex + 1), currentName);
                    if (pt != null) {
                        if (carIndex == 0) primaryPoints.add(pt);
                        else secondaryPoints.add(pt);
                        carIndex++;
                    }
                }
            }
        }
    }

    @Nullable
    private SeasonPerformancePoint createPointFromPosStr(int roundNum, String raceName, String posStr,
                                                          String pointEntityId, String pointEntityName) {
        if (posStr == null || posStr.trim().isEmpty() || "-".equals(posStr.trim())) {
            return null;
        }

        String trimmed = posStr.trim();
        String lower = trimmed.toLowerCase();

        boolean isDnf = "r".equalsIgnoreCase(lower) || "d".equalsIgnoreCase(lower)
                || "w".equalsIgnoreCase(lower) || "nc".equalsIgnoreCase(lower)
                || "dnf".equalsIgnoreCase(lower) || "dns".equalsIgnoreCase(lower)
                || "dsq".equalsIgnoreCase(lower)
                || lower.contains("retired") || lower.contains("accident")
                || lower.contains("collision") || lower.contains("did not");

        int parsedPos;
        if (isDnf) {
            parsedPos = chartView.getTotalPositions(); // Position 21 (DNF)
        } else {
            try {
                parsedPos = Integer.parseInt(trimmed);
                if (parsedPos <= 0 || parsedPos > 20) {
                    parsedPos = chartView.getTotalPositions();
                    isDnf = true;
                }
            } catch (NumberFormatException e) {
                parsedPos = chartView.getTotalPositions();
                isDnf = true;
            }
        }

        String displayPosText = isDnf ? "DNF" : String.valueOf(parsedPos);
        return new SeasonPerformancePoint(roundNum, "R" + roundNum, parsedPos, displayPosText,
                isDnf, false, raceName, pointEntityId, pointEntityName);
    }

    private void showEmptyState() {
        emptyMessage.setVisibility(View.VISIBLE);
        chartView.setData(Collections.emptyList(), Collections.emptyList(), isConstructor);
    }
}
