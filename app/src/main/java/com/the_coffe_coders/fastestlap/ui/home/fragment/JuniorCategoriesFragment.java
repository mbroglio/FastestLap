package com.the_coffe_coders.fastestlap.ui.home.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.util.ui.NavigationUtils;


public class JuniorCategoriesFragment extends Fragment {

    public JuniorCategoriesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_junior_categories, container, false);

        MaterialCardView formula2Card = view.findViewById(R.id.formula_2_card);
        MaterialCardView formula3Card = view.findViewById(R.id.formula_3_card);

        formula2Card.setOnClickListener(v ->
                NavigationUtils.navigateToJuniorPage(getContext(), 0));

        formula3Card.setOnClickListener(v ->
                NavigationUtils.navigateToJuniorPage(getContext(), 1));

        return view;

    }
}