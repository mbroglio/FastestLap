package com.the_coffe_coders.fastestlap;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
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

        String response = callApi();


        TextView teamNameTextView = findViewById(R.id.teamName);
        teamNameTextView.setText(response);
    }

    protected static String callApi() {
        try {
            // Specify the endpoint URL
            URL url = new URL("https://api.jolpi.ca/ergast/f1/2024/constructorstandings/");

            // Open the connection
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /*public String teamName = "Mercedes";
    public String driverOne = "Lewis Hamilton";
    public int driverOneNumber = 44;
    public String driverTwo = "George Russell";
    public int driverTwoNumber = 63;
    public int teamPoints = 573;
    public int teamPosition = 1;*/
}