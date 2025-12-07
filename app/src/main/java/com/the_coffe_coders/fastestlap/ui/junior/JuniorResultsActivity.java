package com.the_coffe_coders.fastestlap.ui.junior;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

public class JuniorResultsActivity extends AppCompatActivity {

    private int categoryType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_junior_results);

        categoryType = getIntent().getIntExtra("CATEGORY_TYPE", 0);

        setToolbar();

        setupPage();

    }

    private void setToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        AppBarLayout appBarLayout = findViewById(R.id.top_bar_layout);

        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        if (categoryType == 0) {
            toolbar.setBackgroundColor(getColor(R.color.formula_2));
            appBarLayout.setBackgroundColor(getColor(R.color.formula_2));
        } else {
            toolbar.setBackgroundColor(getColor(R.color.ferrari_secondary));
            appBarLayout.setBackgroundColor(getColor(R.color.ferrari_secondary));
        }

        UIUtils.applyWindowInsets(toolbar);
    }

    private void setupPage() {
        SwipeRefreshLayout layout = findViewById(R.id.results_layout);
        UIUtils.applyWindowInsets(layout);
    }
}