package com.the_coffe_coders.fastestlap.ui.junior;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.the_coffe_coders.fastestlap.R;

public class JuniorResultsActivity extends AppCompatActivity {

    private int categoryType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_junior_results);

        categoryType = getIntent().getIntExtra("CATEGORY_TYPE", 0);

        setToolbar();

    }

    private void setToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        if(categoryType == 0){
            toolbar.setBackgroundColor(getColor(R.color.formula_2));
        }else{
            toolbar.setBackgroundColor(getColor(R.color.ferrari_secondary));
        }
    }
}