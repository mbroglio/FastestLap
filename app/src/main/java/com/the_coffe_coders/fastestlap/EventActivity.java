package com.the_coffe_coders.fastestlap;

import android.graphics.Paint;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.text.HtmlCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.TextView;
import android.view.ViewGroup;

public class EventActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_event), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        int dynamicDimension = (int) (screenWidth * 0.18);

        TextView textView = findViewById(R.id.session_1_name);
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) textView.getLayoutParams();
        layoutParams.setMarginEnd(dynamicDimension); // Set margin end to 20 pixels
        textView.setLayoutParams(layoutParams);


        int dynamicDimension2 = (int) (screenWidth * 0.22);
        TextView textView2 = findViewById(R.id.session_1_day);
        ViewGroup.MarginLayoutParams layoutParams2 = (ViewGroup.MarginLayoutParams) textView2.getLayoutParams();
        layoutParams2.setMarginEnd(dynamicDimension2); // Set margin end to 20 pixels
        textView2.setLayoutParams(layoutParams2);



    }
}