package com.the_coffe_coders.fastestlap.ui.bio.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.f1.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.f1.constructor.ConstructorHistory;
import com.the_coffe_coders.fastestlap.domain.f1.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.f1.driver.DriverHistory;
import com.the_coffe_coders.fastestlap.util.ui.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ui.TachometerView;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragmentTab extends Fragment {

    private static final String ARG_IS_CONSTRUCTOR = "ARG_IS_CONSTRUCTOR";
    private static final String ARG_TEAM_COLOR = "ARG_TEAM_COLOR";
    private static final String ARG_DRIVER = "ARG_DRIVER";
    private static final String ARG_CONSTRUCTOR = "ARG_CONSTRUCTOR";
    private static final String ARG_DRIVER_HISTORY = "ARG_DRIVER_HISTORY";
    private static final String ARG_CONSTRUCTOR_HISTORY = "ARG_CONSTRUCTOR_HISTORY";

    private boolean isConstructor;
    private int teamColor;
    private Driver driver;
    private Constructor constructor;
    private ArrayList<DriverHistory> driverHistoryList;
    private ArrayList<ConstructorHistory> constructorHistoryList;
    private LoadingScreen loadingScreen;

    public HistoryFragmentTab() {
        // Required empty public constructor
    }

    public static HistoryFragmentTab newInstanceForDriver(Driver driver, int teamColor) {
        HistoryFragmentTab fragment = new HistoryFragmentTab();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_CONSTRUCTOR, false);
        args.putInt(ARG_TEAM_COLOR, teamColor);
        if (driver != null) {
            args.putParcelable(ARG_DRIVER, driver);
            if (driver.getDriver_history() != null) {
                args.putSerializable(ARG_DRIVER_HISTORY, new ArrayList<>(driver.getDriver_history()));
            }
        }
        fragment.setArguments(args);
        return fragment;
    }

    public static HistoryFragmentTab newInstanceForDriver(List<DriverHistory> driverHistory, int teamColor) {
        HistoryFragmentTab fragment = new HistoryFragmentTab();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_CONSTRUCTOR, false);
        args.putInt(ARG_TEAM_COLOR, teamColor);
        if (driverHistory != null) {
            args.putSerializable(ARG_DRIVER_HISTORY, new ArrayList<>(driverHistory));
        }
        fragment.setArguments(args);
        return fragment;
    }

    public static HistoryFragmentTab newInstanceForConstructor(Constructor constructor, int teamColor) {
        HistoryFragmentTab fragment = new HistoryFragmentTab();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_CONSTRUCTOR, true);
        args.putInt(ARG_TEAM_COLOR, teamColor);
        if (constructor != null) {
            args.putParcelable(ARG_CONSTRUCTOR, constructor);
            if (constructor.getTeam_history() != null) {
                args.putSerializable(ARG_CONSTRUCTOR_HISTORY, new ArrayList<>(constructor.getTeam_history()));
            }
        }
        fragment.setArguments(args);
        return fragment;
    }

    public static HistoryFragmentTab newInstanceForConstructor(List<ConstructorHistory> constructorHistory, int teamColor) {
        HistoryFragmentTab fragment = new HistoryFragmentTab();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_CONSTRUCTOR, true);
        args.putInt(ARG_TEAM_COLOR, teamColor);
        if (constructorHistory != null) {
            args.putSerializable(ARG_CONSTRUCTOR_HISTORY, new ArrayList<>(constructorHistory));
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            isConstructor = getArguments().getBoolean(ARG_IS_CONSTRUCTOR, false);
            teamColor = getArguments().getInt(ARG_TEAM_COLOR, 0);
            if (isConstructor) {
                constructor = getArguments().getParcelable(ARG_CONSTRUCTOR);
                //noinspection unchecked
                constructorHistoryList = (ArrayList<ConstructorHistory>) getArguments().getSerializable(ARG_CONSTRUCTOR_HISTORY);
            } else {
                driver = getArguments().getParcelable(ARG_DRIVER);
                //noinspection unchecked
                driverHistoryList = (ArrayList<DriverHistory>) getArguments().getSerializable(ARG_DRIVER_HISTORY);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View contentScroll = view.findViewById(R.id.history_scroll_view);
        loadingScreen = new LoadingScreen(view, requireContext(), null, contentScroll);
        loadingScreen.showLoadingScreen(false);
        loadingScreen.updateProgress();

        TachometerView winPercentageTachometer = view.findViewById(R.id.win_percentage_tachometer_driver);
        TachometerView podiumPercentageTachometer = view.findViewById(R.id.podium_percentage_tachometer_driver);

        if (isConstructor) {
            if (constructor != null && winPercentageTachometer != null && podiumPercentageTachometer != null) {
                UIUtils.updateTachometers(requireContext(), constructor, winPercentageTachometer, podiumPercentageTachometer);
                winPercentageTachometer.startAnimation();
                podiumPercentageTachometer.startAnimation();
            }
        } else {
            if (driver != null && winPercentageTachometer != null && podiumPercentageTachometer != null) {
                UIUtils.updateTachometers(requireContext(), driver, winPercentageTachometer, podiumPercentageTachometer);
                winPercentageTachometer.startAnimation();
                podiumPercentageTachometer.startAnimation();
            }
        }

        TableLayout tableLayout = view.findViewById(R.id.history_table);
        if (tableLayout == null) {
            if (loadingScreen != null) {
                loadingScreen.hideLoadingScreenImmediately();
            }
            return;
        }
        tableLayout.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        if (isConstructor) {
            populateConstructorHistory(tableLayout, inflater);
        } else {
            populateDriverHistory(tableLayout, inflater);
        }

        if (loadingScreen != null) {
            loadingScreen.hideLoadingScreenImmediately();
        }
    }

    private void populateDriverHistory(TableLayout tableLayout, LayoutInflater inflater) {
        if (driverHistoryList == null || driverHistoryList.isEmpty()) {
            return;
        }

        View tableHeader = inflater.inflate(R.layout.driver_bio_table_header, tableLayout, false);
        tableLayout.addView(tableHeader);

        for (int i = driverHistoryList.size() - 1; i >= 0; i--) {
            DriverHistory driverHistory = driverHistoryList.get(i);
            View tableRow = inflater.inflate(R.layout.driver_bio_table_row, tableLayout, false);

            if (i % 2 == 1) {
                tableRow.setBackgroundColor(requireContext().getColor(R.color.odd_table_row_color));
            } else {
                tableRow.setBackgroundColor(Color.TRANSPARENT);
            }

            UIUtils.multipleSetTextViewText(
                    new String[]{
                            driverHistory.getYear(),
                            driverHistory.getTeam(),
                            driverHistory.getPosition(),
                            driverHistory.getPoints(),
                            driverHistory.getWins(),
                            driverHistory.getPodiums()
                    },
                    new TextView[]{
                            tableRow.findViewById(R.id.season_year),
                            tableRow.findViewById(R.id.team_name),
                            tableRow.findViewById(R.id.driver_position),
                            tableRow.findViewById(R.id.driver_points),
                            tableRow.findViewById(R.id.driver_wins),
                            tableRow.findViewById(R.id.driver_podiums)
                    }
            );

            tableLayout.addView(tableRow);
        }
    }

    private void populateConstructorHistory(TableLayout tableLayout, LayoutInflater inflater) {
        if (constructorHistoryList == null || constructorHistoryList.isEmpty()) {
            return;
        }

        View tableHeader = inflater.inflate(R.layout.constructor_bio_table_header, tableLayout, false);
        tableLayout.addView(tableHeader);

        for (int i = constructorHistoryList.size() - 1; i >= 0; i--) {
            ConstructorHistory constructorHistory = constructorHistoryList.get(i);
            View tableRow = inflater.inflate(R.layout.constructor_bio_table_row, tableLayout, false);

            if (i % 2 == 1) {
                tableRow.setBackgroundColor(0x0AFFFFFF);
            } else {
                tableRow.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }

            UIUtils.multipleSetTextViewText(
                    new String[]{
                            constructorHistory.getYear(),
                            constructorHistory.getPosition(),
                            constructorHistory.getPoints(),
                            constructorHistory.getWins(),
                            constructorHistory.getPodiums()
                    },
                    new TextView[]{
                            tableRow.findViewById(R.id.season_year),
                            tableRow.findViewById(R.id.team_position),
                            tableRow.findViewById(R.id.team_points),
                            tableRow.findViewById(R.id.team_wins),
                            tableRow.findViewById(R.id.team_podiums)
                    }
            );

            tableLayout.addView(tableRow);
        }
    }
}