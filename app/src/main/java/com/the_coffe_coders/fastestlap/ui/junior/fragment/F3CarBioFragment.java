package com.the_coffe_coders.fastestlap.ui.junior.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.the_coffe_coders.fastestlap.R;

public class F3CarBioFragment extends Fragment {

    private static final String TAG = "F3CarBioFragment";

    public F3CarBioFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_f3_car_bio, container, false);

        // Here you can add logic to populate car information
        // For example: load car specifications, images, etc.

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize UI components and set up data
        setupCarInformation();
    }

    private void setupCarInformation() {
        // Add your logic here to display F3 car information
        // You can fetch data from ViewModel, database, or static resources
    }
}

