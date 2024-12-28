package com.the_coffe_coders.fastestlap.ui.live.fragment;

import android.content.pm.ActivityInfo;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import com.the_coffe_coders.fastestlap.R;


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
        });

        return view;
    }
}