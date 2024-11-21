package com.the_coffe_coders.fastestlap.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.the_coffe_coders.fastestlap.R;

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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.team_card_view), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        LinearLayout teamStanding = findViewById(R.id.team_standing);
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

                        for (int i = 0; i < standing.length(); i++) {
                            JSONObject team = standing.getJSONObject(i);
                            int points = team.getInt("points");
                            String position = team.getString("position");
                            String teamId = team.getJSONObject("Constructor").getString("constructorId");

                            Log.i(TAG, "Team ID: " + teamId);
                            Log.i(TAG, "Position: " + position);
                            Log.i(TAG, "Points: " + points);

                            teamStanding.addView(generateTeamCard(teamId, position, points));
                            //add a space between each team card
                            View space = new View(TeamCardActivity.this);
                            space.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20));
                            teamStanding.addView(space);
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

    private View generateTeamCard(String teamId, String position, int points) {
        // Inflate the team card layout
        View teamCard = getLayoutInflater().inflate(R.layout.team_card, null);

        // Set the team name
        TextView teamNameTextView = teamCard.findViewById(R.id.team_name);

        TextView driverOneNameTextView = teamCard.findViewById(R.id.driver_1_name);
        ImageView driverOneImageView = teamCard.findViewById(R.id.driver_1_pic);

        TextView driverTwoNameTextView = teamCard.findViewById(R.id.driver_2_name);
        ImageView driverTwoImageView = teamCard.findViewById(R.id.driver_2_pic);

        ImageView teamLogoImageView = teamCard.findViewById(R.id.team_logo);
        ImageView teamCarImageView = teamCard.findViewById(R.id.car_image);
        LinearLayout teamColor = teamCard.findViewById(R.id.team_card);

        switch (teamId) {
            case "mercedes":
                teamNameTextView.setText(R.string.mercedes);
                teamLogoImageView.setImageResource(R.drawable.mercedeslogo);
                teamCarImageView.setImageResource(R.drawable.mercedes);
                teamColor.setBackground(AppCompatResources.getDrawable(this, R.drawable.gradient_color_mercedes));

                driverOneNameTextView.setText(R.string.lewis_hamilton);
                driverOneImageView.setImageResource(R.drawable.hamilton_pic);

                driverTwoNameTextView.setText(R.string.george_russell);
                driverTwoImageView.setImageResource(R.drawable.russell_pic);
                break;
            case "red_bull":
                teamNameTextView.setText(R.string.red_bull);
                teamLogoImageView.setImageResource(R.drawable.redbulllogo);
                teamCarImageView.setImageResource(R.drawable.red_bull_racing);
                teamColor.setBackground(AppCompatResources.getDrawable(this, R.drawable.gradient_color_red_bull));

                driverOneNameTextView.setText(R.string.max_verstappen);
                driverOneImageView.setImageResource(R.drawable.verstappen_pic);

                driverTwoNameTextView.setText(R.string.sergio_perez);
                driverTwoImageView.setImageResource(R.drawable.perez_pic);
                break;
            case "ferrari":
                teamNameTextView.setText(R.string.ferrari);
                teamLogoImageView.setImageResource(R.drawable.ferrarilogo);
                teamCarImageView.setImageResource(R.drawable.ferrari);
                teamColor.setBackground(AppCompatResources.getDrawable(this, R.drawable.gradient_color_ferrari));

                driverOneNameTextView.setText(R.string.charles_leclerc);
                driverOneImageView.setImageResource(R.drawable.leclerc_pic);

                driverTwoNameTextView.setText(R.string.carlos_sainz);
                driverTwoImageView.setImageResource(R.drawable.sainz_pic);
                break;
            case "mclaren":
                teamNameTextView.setText(R.string.mclaren);
                teamLogoImageView.setImageResource(R.drawable.mclarenlogo);
                teamCarImageView.setImageResource(R.drawable.mclaren);
                teamColor.setBackground(AppCompatResources.getDrawable(this,R.drawable.gradient_color_mclaren));

                driverOneNameTextView.setText(R.string.lando_norris);
                driverOneImageView.setImageResource(R.drawable.norris_pic);

                driverTwoNameTextView.setText(R.string.oscar_piastri);
                driverTwoImageView.setImageResource(R.drawable.piastri_pic);
                break;
            case "aston_martin":
                teamNameTextView.setText(R.string.aston_martin);
                teamLogoImageView.setImageResource(R.drawable.astonmartinlogo);
                teamCarImageView.setImageResource(R.drawable.aston_martin);
                teamColor.setBackground(AppCompatResources.getDrawable(this,R.drawable.gradient_color_aston_martin));

                driverOneNameTextView.setText(R.string.fernando_alonso);
                driverOneImageView.setImageResource(R.drawable.alonso_pic);

                driverTwoNameTextView.setText(R.string.lance_stroll);
                driverTwoImageView.setImageResource(R.drawable.stroll_pic);
                break;
            case "alpine":
                teamNameTextView.setText(R.string.alpine);
                teamLogoImageView.setImageResource(R.drawable.alpinelogo);
                teamCarImageView.setImageResource(R.drawable.alpine);
                teamColor.setBackground(AppCompatResources.getDrawable(this, R.drawable.gradient_color_alpine));

                driverOneNameTextView.setText(R.string.esteban_ocon);
                driverOneImageView.setImageResource(R.drawable.ocon_pic);

                driverTwoNameTextView.setText(R.string.pierre_gasly);
                driverTwoImageView.setImageResource(R.drawable.gasly_pic);
                break;
            case "rb":
                teamNameTextView.setText(R.string.rb);
                teamLogoImageView.setImageResource(R.drawable.racingbullslogo);
                teamCarImageView.setImageResource(R.drawable.rb);
                teamColor.setBackground(AppCompatResources.getDrawable(this, R.drawable.gradient_color_rb));

                driverOneNameTextView.setText(R.string.yuki_tsunoda);
                driverOneImageView.setImageResource(R.drawable.tsunoda_pic);

                driverTwoNameTextView.setText(R.string.liam_lawson);
                driverTwoImageView.setImageResource(R.drawable.lawson_pic);
                break;
            case "haas":
                teamNameTextView.setText(R.string.haas);
                teamLogoImageView.setImageResource(R.drawable.haaslogo);
                teamCarImageView.setImageResource(R.drawable.haas);
                teamColor.setBackground(AppCompatResources.getDrawable(this, R.drawable.gradient_color_haas));

                driverOneNameTextView.setText(R.string.nico_hulkenberg);
                driverOneImageView.setImageResource(R.drawable.hulkenberg_pic);

                driverTwoNameTextView.setText(R.string.kevin_magnussen);
                driverTwoImageView.setImageResource(R.drawable.magnussen_pic);
                break;
            case "williams":
                teamNameTextView.setText(R.string.williams);
                teamLogoImageView.setImageResource(R.drawable.williamslogo);
                teamCarImageView.setImageResource(R.drawable.williams);
                teamColor.setBackground(AppCompatResources.getDrawable(this, R.drawable.gradient_color_williams));

                driverOneNameTextView.setText(R.string.alexander_albon);
                driverOneImageView.setImageResource(R.drawable.albon_pic);

                driverTwoNameTextView.setText(R.string.franco_colapinto);
                driverTwoImageView.setImageResource(R.drawable.colapinto_pic);
                break;
            case "sauber":
                teamNameTextView.setText(R.string.sauber);
                teamLogoImageView.setImageResource(R.drawable.stakelogo);
                teamCarImageView.setImageResource(R.drawable.kick_sauber);
                teamColor.setBackground(AppCompatResources.getDrawable(this, R.drawable.gradient_color_sauber));

                driverOneNameTextView.setText(R.string.valtteri_bottas);
                driverOneImageView.setImageResource(R.drawable.bottas_pic);

                driverTwoNameTextView.setText(R.string.zhou_guanyu);
                driverTwoImageView.setImageResource(R.drawable.zhou_pic);
                break;
            default:
                teamNameTextView.setText(R.string.no_team_name_found);
                break;
        }

        // Set the team position
        TextView teamPositionTextView = teamCard.findViewById(R.id.team_position);
        teamPositionTextView.setText(position);

        // Set the team points
        TextView teamPointsTextView = teamCard.findViewById(R.id.team_points);
        teamPointsTextView.setText(String.valueOf(points));

        return teamCard;
    }
}