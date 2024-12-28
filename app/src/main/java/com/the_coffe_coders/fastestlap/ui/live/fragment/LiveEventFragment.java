package com.the_coffe_coders.fastestlap.ui.live.fragment;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.ui.live.LiveActivity;

import java.util.Objects;

public class LiveEventFragment extends Fragment {

    private CheckBox fullTelemetryCheckbox;
    private TableLayout tableLayout;
    private View liveTableHeaderShort;
    private View liveTableHeaderFull;

    public LiveEventFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_live_event, container, false);

        fullTelemetryCheckbox = view.findViewById(R.id.full_telemetry_checkbox);
        tableLayout = view.findViewById(R.id.live_table);
        liveTableHeaderShort = view.findViewById(R.id.race_header_short);
        liveTableHeaderFull = view.findViewById(R.id.race_header_full);

        fullTelemetryCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else {
                requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
            if (requireActivity() instanceof LiveActivity) {
                ((LiveActivity) requireActivity()).setFullTelemetryMode(isChecked);
            }
            updateTableHeaderAndRows(isChecked);
        });

        updateTableHeaderAndRows(fullTelemetryCheckbox.isChecked());

        return view;
    }

    private void updateTableHeaderAndRows(boolean isFullTelemetry) {
        if (isFullTelemetry) {
            clearTableRows();
            liveTableHeaderShort.setVisibility(View.GONE);
            liveTableHeaderFull.setVisibility(View.VISIBLE);
            inflateFullTableRows();
        } else {
            liveTableHeaderShort.setVisibility(View.VISIBLE);
            liveTableHeaderFull.setVisibility(View.GONE);
            inflateShortTableRows();
        }
    }

    private void inflateShortTableRows() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        String[] driverAbbreviations = {"VER", "PER", "HAM", "RUS", "LEC", "SAI", "NOR", "PIA", "ALO", "STR", "GAS", "OCO", "BOT", "ZHO", "MAG", "HUL", "TSU", "RIC", "SAR", "ALB"};
        String[] tyreTypes = {"S", "M", "H", "W", "I"};

        for (int i = 0; i < 20; i++) {
            TableRow tableRow = (TableRow) inflater.inflate(R.layout.live_race_table_row_short_elements, tableLayout, false);

            TextView driverPosition = tableRow.findViewById(R.id.driver_position);
            driverPosition.setText(String.valueOf(i + 1));

            TextView driverName = tableRow.findViewById(R.id.driver_name);
            driverName.setText(driverAbbreviations[i]);

            TextView lastLapTime = tableRow.findViewById(R.id.last_lap_time);
            lastLapTime.setText(String.format("1:%02d.%03d", (int) (Math.random() * 60), (int) (Math.random() * 1000)));

            TextView gapAheadText = tableRow.findViewById(R.id.gap_ahead_text);
            gapAheadText.setText(String.format("+%d.%03d", (int) (Math.random() * 60), (int) (Math.random() * 1000)));

            TextView positionChangeValue = tableRow.findViewById(R.id.position_change_value);
            positionChangeValue.setText(String.valueOf((int) (Math.random() * 10) - 5));
            ImageView positionChangeArrow = tableRow.findViewById(R.id.position_change_icon);
            if (positionChangeValue.getText().charAt(0) == '-') {
                positionChangeArrow.setImageResource(R.drawable.down_arrow);
                positionChangeArrow.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red));
            } else if (Integer.parseInt(positionChangeValue.getText().toString())>0) {
                positionChangeArrow.setColorFilter(ContextCompat.getColor(requireContext(), R.color.green));
            }else{
                positionChangeArrow.setImageResource(R.drawable.equals_icon);
                positionChangeArrow.setColorFilter(ContextCompat.getColor(requireContext(), R.color.grey_card));
            }

            TextView tyreInUseSymbol = tableRow.findViewById(R.id.tyre_in_use_symbol);
            tyreInUseSymbol.setText(tyreTypes[(int) (Math.random() * tyreTypes.length)]);
            ImageView tyreInUse = tableRow.findViewById(R.id.tyre_icon);
            if(tyreInUseSymbol.getText().equals("S")) {
                tyreInUse.setImageResource(R.drawable.soft_tyre_icon);
            } else if(tyreInUseSymbol.getText().equals("M")) {
                tyreInUse.setImageResource(R.drawable.medium_tyre_icon);
            } else if(tyreInUseSymbol.getText().equals("H")){
                tyreInUse.setImageResource(R.drawable.hard_tyre_icon);
            }else if (tyreInUseSymbol.getText().equals("W")){
                tyreInUse.setImageResource(R.drawable.wet_tyre_icon);
            }else{
                tyreInUse.setImageResource(R.drawable.intermediate_tyre_icon);
            }


            tableLayout.addView(tableRow);
        }
    }

    private void inflateFullTableRows() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        String[] driverAbbreviations = {"VER", "PER", "HAM", "RUS", "LEC", "SAI", "NOR", "PIA", "ALO", "STR", "GAS", "OCO", "BOT", "ZHO", "MAG", "HUL", "TSU", "RIC", "SAR", "ALB"};
        String[] tyreTypes = {"S", "M", "H", "W", "I"};

        for (int i = 0; i < 20; i++) {
            TableRow tableRow = (TableRow) inflater.inflate(R.layout.live_race_table_row_full_elements, tableLayout, false);

            TextView driverPosition = tableRow.findViewById(R.id.driver_position);
            driverPosition.setText(String.valueOf(i + 1));

            TextView driverName = tableRow.findViewById(R.id.driver_name);
            driverName.setText(driverAbbreviations[i]);

            TextView lastLapTime = tableRow.findViewById(R.id.last_lap_time);
            lastLapTime.setText(String.format("1:%02d.%03d", (int) (Math.random() * 60), (int) (Math.random() * 1000)));

            TextView intervalTime = tableRow.findViewById(R.id.interval_text);
            intervalTime.setText(String.format("+%d.%03d", (int) (Math.random() * 60), (int) (Math.random() * 1000)));

            TextView gapAheadText = tableRow.findViewById(R.id.gap_ahead_text);
            gapAheadText.setText(String.format("+%d.%03d", (int) (Math.random() * 60), (int) (Math.random() * 1000)));

            TextView positionChangeValue = tableRow.findViewById(R.id.position_change_value);
            positionChangeValue.setText(String.valueOf((int) (Math.random() * 10) - 5));

            TextView tyreInUseSymbol = tableRow.findViewById(R.id.tyre_in_use_symbol);
            tyreInUseSymbol.setText(tyreTypes[(int) (Math.random() * tyreTypes.length)]);
            ImageView tyreInUse = tableRow.findViewById(R.id.tyre_icon);
            if(tyreInUseSymbol.getText().equals("S")) {
                tyreInUse.setImageResource(R.drawable.soft_tyre_icon);
            } else if(tyreInUseSymbol.getText().equals("M")) {
                tyreInUse.setImageResource(R.drawable.medium_tyre_icon);
            } else if(tyreInUseSymbol.getText().equals("H")){
                tyreInUse.setImageResource(R.drawable.hard_tyre_icon);
            }else if (tyreInUseSymbol.getText().equals("W")){
                tyreInUse.setImageResource(R.drawable.wet_tyre_icon);
            }else{
                tyreInUse.setImageResource(R.drawable.intermediate_tyre_icon);
            }

            TextView boxCount = tableRow.findViewById(R.id.box_count);
            boxCount.setText(String.valueOf((int) (Math.random() * 3)));

            TextView lapCount = tableRow.findViewById(R.id.lap_count);
            lapCount.setText(String.valueOf((int) (Math.random() * 53)));

            ImageView positionChangeArrow = tableRow.findViewById(R.id.position_change_icon);
            if (positionChangeValue.getText().charAt(0) == '-') {
                positionChangeArrow.setImageResource(R.drawable.down_arrow);
                positionChangeArrow.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red));
            } else if (Integer.parseInt(positionChangeValue.getText().toString())>0) {
                positionChangeArrow.setColorFilter(ContextCompat.getColor(requireContext(), R.color.green));
            }else{
                positionChangeArrow.setImageResource(R.drawable.equals_icon);
                positionChangeArrow.setColorFilter(ContextCompat.getColor(requireContext(), R.color.grey_card));
            }
            tableLayout.addView(tableRow);
        }
    }

    private void clearTableRows() {
        tableLayout.removeViews(1, tableLayout.getChildCount() - 1);
    }
}