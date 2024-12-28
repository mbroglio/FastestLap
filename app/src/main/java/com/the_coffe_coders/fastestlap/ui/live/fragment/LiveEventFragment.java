package com.the_coffe_coders.fastestlap.ui.live.fragment;

import android.content.pm.ActivityInfo;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.provider.ContactsContract;
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


public class LiveEventFragment extends Fragment {

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

        CheckBox fullTelemetryCheckbox = view.findViewById(R.id.full_telemetry_checkbox);
        fullTelemetryCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else {
                requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
            if (requireActivity() instanceof LiveActivity) {
                ((LiveActivity) requireActivity()).setFullTelemetryMode(isChecked);
            }
        });

        TableLayout tableLayout = view.findViewById(R.id.live_table);

        //String[] driverNames = {"Max Verstappen", "Sergio Perez", "Lewis Hamilton", "George Russell", "Charles Leclerc", "Carlos Sainz", "Lando Norris", "Oscar Piastri", "Fernando Alonso", "Lance Stroll", "Pierre Gasly", "Esteban Ocon", "Valtteri Bottas", "Zhou Guanyu", "Kevin Magnussen", "Nico Hulkenberg", "Yuki Tsunoda", "Daniel Ricciardo", "Alexander Albon", "Logan Sargeant"};
        String[] driverAbbreviations = {"VER", "PER", "HAM", "RUS", "LEC", "SAI", "NOR", "PIA", "ALO", "STR", "GAS", "OCO", "BOT", "ZHO", "MAG", "HUL", "TSU", "RIC", "SAR", "ALB"};
        String[] tyreTypes = {"S", "M", "H"};

        for (int i = 0; i < 20; i++) {
            TableRow tableRow = (TableRow) inflater.inflate(R.layout.live_table_row_short_elements, tableLayout, false);

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

            TextView tyreInUseSymbol = tableRow.findViewById(R.id.tyre_in_use_symbol);
            tyreInUseSymbol.setText(tyreTypes[(int) (Math.random() * tyreTypes.length)]);

            tableLayout.addView(tableRow);
        }

        return view;
    }
}