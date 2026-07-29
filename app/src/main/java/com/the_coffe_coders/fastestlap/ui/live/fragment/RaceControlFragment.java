package com.the_coffe_coders.fastestlap.ui.live.fragment;

import android.content.pm.ActivityInfo;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import com.the_coffe_coders.fastestlap.R;


public class RaceControlFragment extends Fragment {

    ;

    public RaceControlFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_race_control, container, false);

        LinearLayout raceControlLayout = view.findViewById(R.id.race_control_layout);



        // Inflate race_control_view_entry multiple times and set different layouts visible
        int[] includeIds = {
                R.id.flag_message,
                R.id.safety_car_message,
                R.id.stewards_message,
                R.id.track_limits_message,
                R.id.flag_driver_message,
                R.id.pit_lane_message,
                R.id.drs_message
        };

        for (int includeId : includeIds) {
            View entryView = inflater.inflate(R.layout.race_control_view_entry, raceControlLayout, false);
            for (int id : includeIds) {
                entryView.findViewById(id).setVisibility(id == includeId ? View.VISIBLE : View.GONE);
            }
            raceControlLayout.addView(entryView);
        }

        return view;



    }

    @Override
    public void onResume() {
        super.onResume();
        // Disable screen rotation
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Re-enable screen rotation
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }
}