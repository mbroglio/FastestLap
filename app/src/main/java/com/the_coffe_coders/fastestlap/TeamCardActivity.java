package com.the_coffe_coders.fastestlap;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.*;

import java.io.IOException;
import java.net.*;

public class TeamCardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_team_card);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.team_card), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        TextView teamNameTextView = findViewById(R.id.teamName);
        teamNameTextView.setText(teamName);
    }

    String url = "https://api.jolpi.ca/ergast/f1/2024/constructorstandings/";


    public String teamName = "Mercedes";
    public String driverOne = "Lewis Hamilton";
    public int driverOneNumber = 44;
    public String driverTwo = "George Russell";
    public int driverTwoNumber = 63;
    public int teamPoints = 573;
    public int teamPosition = 1;
}