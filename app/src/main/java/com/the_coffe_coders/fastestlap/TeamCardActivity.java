package com.the_coffe_coders.fastestlap;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import okhttp3.ResponseBody;
import org.json.JSONArray;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class TeamCardActivity extends AppCompatActivity {

    private static final String TAG = "TeamCardActivity";
    private static final String BASE_URL = "https://api.jolpi.ca/ergast/f1/2024/";

    private TextView teamPointsTextView;

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

        LinearLayout teamStanding = findViewById(R.id.team_standing);

        teamPointsTextView = findViewById(R.id.team_points);

        // Initialize Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();

        ErgastAPI ergastApi = retrofit.create(ErgastAPI.class);

        // Make the network request
        ergastApi.getConstructorStandings().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseString = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseString);
                        Log.i(TAG, "Response " + jsonResponse.toString(2));

                        JSONArray standing = jsonResponse.getJSONObject("MRData").getJSONObject("StandingsTable").getJSONArray("StandingsLists").getJSONObject(0).getJSONArray("ConstructorStandings");

                        for(int i = 0; i < standing.length(); i++) {
                            JSONObject team = standing.getJSONObject(i);
                            int points = team.getInt("points");
                            String teamName = team.getJSONObject("Constructor").getString("name");
                            String position = team.getString("position");
                            String teamId = team.getJSONObject("Constructor").getString("constructorId");

                            Log.i(TAG, "Team ID: " + teamId);
                            Log.i(TAG, "Team: " + teamName);
                            Log.i(TAG, "Position: " + position);
                            Log.i(TAG, "Points: " + points);

                            teamStanding.addView(generateTeamCard(teamId, teamName, position, points));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse JSON response", e);
                    }
                } else {
                    Log.e(TAG, "Response not successful");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Network request failed", t);
            }
        });
    }

    private View generateTeamCard(String teamId, String teamName, String position, int points) {
        // Inflate the team card layout
        View teamCard = getLayoutInflater().inflate(R.layout.team_card, null);

        // Set the team name
        TextView teamNameTextView = teamCard.findViewById(R.id.team_name);
        teamNameTextView.setText(R.string.teamId);

        return null;
    }
}