package com.the_coffe_coders.fastestlap;

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

import com.google.android.material.card.MaterialCardView;

import okhttp3.ResponseBody;

import org.json.JSONArray;
import org.json.JSONObject;
import org.threeten.bp.Year;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class DriverCardActivity extends AppCompatActivity {

    private static final String TAG = "DriverCardActivity";

    static Year year = Year.now();
    private static final String BASE_URL = "https://api.jolpi.ca/ergast/f1/" + year + "/";

    private TextView teamPointsTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_driver_card);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.driver_card_view), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        LinearLayout driverStanding = findViewById(R.id.driver_standing);
        // Initialize Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();

        ErgastAPI ergastApi = retrofit.create(ErgastAPI.class);

        // Make the network request
        ergastApi.getDriverStandings().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseString = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseString);
                        Log.i(TAG, "Response " + jsonResponse.toString(2));

                        JSONArray standing = jsonResponse.getJSONObject("MRData").getJSONObject("StandingsTable").getJSONArray("StandingsLists").getJSONObject(0).getJSONArray("DriverStandings");

                        for (int i = 0; i < standing.length(); i++) {
                            JSONObject driver = standing.getJSONObject(i);
                            JSONObject driverDetails = driver.getJSONObject("Driver");
                            String driverId = driverDetails.getString("driverId");
                            int points = driver.getInt("points");
                            int position = driver.getInt("position");


                            Log.i(TAG, "Driver ID: " + driverId);
                            Log.i(TAG, "Driver Points: " + points);
                            Log.i(TAG, "Position: " + position);

                            driverStanding.addView(generateDriverCard(driverId, points, position));
                            View space = new View(DriverCardActivity.this);
                            space.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20));
                            driverStanding.addView(space);
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

    private View generateDriverCard(String driverId, int points, int position) {
        // Inflate the team card layout
        View driverCard = getLayoutInflater().inflate(R.layout.small_driver_card, null);

        // Set the team name
        TextView driverPosition = driverCard.findViewById(R.id.driver_position);
        ImageView driverImageView = driverCard.findViewById(R.id.driver_image);
        TextView driverName = driverCard.findViewById(R.id.driver_name);
        TextView driverPoints = driverCard.findViewById(R.id.driver_points);

        ImageView teamLogoImageView = driverCard.findViewById(R.id.team_logo);
        LinearLayout driverColor = driverCard.findViewById(R.id.small_driver_card);

        switch (driverId) {
            case "hamilton":
                driverImageView.setImageResource(R.drawable.hamilton_pic);
                driverName.setText(R.string.lewis_hamilton);
                teamLogoImageView.setImageResource(R.drawable.mercedeslogo);
                driverColor.setBackground(AppCompatResources.getDrawable(this,R.drawable.gradient_color_mercedes));
            break;
            case "max_verstappen":
                driverImageView.setImageResource(R.drawable.verstappen_pic);
                driverName.setText(R.string.max_verstappen);
                teamLogoImageView.setImageResource(R.drawable.redbulllogo);
                driverColor.setBackground(AppCompatResources.getDrawable(this,R.drawable.gradient_color_red_bull));
                break;
            case "bottas":
                driverImageView.setImageResource(R.drawable.bottas_pic);
                driverName.setText(R.string.valtteri_bottas);
                teamLogoImageView.setImageResource(R.drawable.stakelogo);
                driverColor.setBackground(AppCompatResources.getDrawable(this,R.drawable.gradient_color_sauber));
                break;
            case "norris":
                driverImageView.setImageResource(R.drawable.norris_pic);
                driverName.setText(R.string.lando_norris);
                teamLogoImageView.setImageResource(R.drawable.mclarenlogo);
                driverColor.setBackground(AppCompatResources.getDrawable(this,R.drawable.gradient_color_mclaren));
                break;
            case "perez":
                driverImageView.setImageResource(R.drawable.perez_pic);
                driverName.setText(R.string.sergio_perez);
                teamLogoImageView.setImageResource(R.drawable.redbulllogo);
                driverColor.setBackground(AppCompatResources.getDrawable(this,R.drawable.gradient_color_red_bull));
                break;
            case "leclerc":
                driverImageView.setImageResource(R.drawable.leclerc_pic);
                driverName.setText(R.string.charles_leclerc);
                teamLogoImageView.setImageResource(R.drawable.ferrarilogo);
                driverColor.setBackground(AppCompatResources.getDrawable(this,R.drawable.gradient_color_ferrari));
                break;
            case "sainz":
                driverImageView.setImageResource(R.drawable.sainz_pic);
                driverName.setText(R.string.carlos_sainz);
                teamLogoImageView.setImageResource(R.drawable.ferrarilogo);
                driverColor.setBackground(AppCompatResources.getDrawable(this,R.drawable.gradient_color_ferrari));
                break;
            case "ricciardo":
                driverImageView.setImageResource(R.drawable.ricciardo_pic);
                driverName.setText(R.string.daniel_ricciardo);
                teamLogoImageView.setImageResource(R.drawable.racingbullslogo);
                driverColor.setBackground(AppCompatResources.getDrawable(this,R.drawable.gradient_color_rb));
                break;
            case "gasly":
                driverImageView.setImageResource(R.drawable.gasly_pic);
                driverName.setText(R.string.pierre_gasly);
                teamLogoImageView.setImageResource(R.drawable.alpinelogo);
                driverColor.setBackground(AppCompatResources.getDrawable(this,R.drawable.gradient_color_alpine));
                break;
            case "alonso":
                driverImageView.setImageResource(R.drawable.alonso_pic);
                driverName.setText(R.string.fernando_alonso);
                teamLogoImageView.setImageResource(R.drawable.astonmartinlogo);
                driverColor.setBackground(AppCompatResources.getDrawable(this,R.drawable.gradient_color_aston_martin));
                break;
            case "ocon":
                driverImageView.setImageResource(R.drawable.ocon_pic);
                driverName.setText(R.string.esteban_ocon);
                teamLogoImageView.setImageResource(R.drawable.alpinelogo);
                driverColor.setBackground(AppCompatResources.getDrawable(this,R.drawable.gradient_color_alpine));
                break;
            case "kevin_magnussen":
                driverImageView.setImageResource(R.drawable.magnussen_pic);
                driverName.setText(R.string.kevin_magnussen);
                teamLogoImageView.setImageResource(R.drawable.haaslogo);
                teamLogoImageView.setContentDescription(getString(R.string.haas_full));
                driverColor.setBackground(AppCompatResources.getDrawable(this,R.drawable.gradient_color_haas));
                break;
            case "stroll":
                driverImageView.setImageResource(R.drawable.stroll_pic);
                driverName.setText(R.string.lance_stroll);
                teamLogoImageView.setImageResource(R.drawable.astonmartinlogo);
                driverColor.setBackground(AppCompatResources.getDrawable(this,R.drawable.gradient_color_aston_martin));
                break;
            case "tsunoda":
                driverImageView.setImageResource(R.drawable.tsunoda_pic);
                driverName.setText(R.string.yuki_tsunoda);
                teamLogoImageView.setImageResource(R.drawable.racingbullslogo);
                driverColor.setBackground(AppCompatResources.getDrawable(this,R.drawable.gradient_color_rb));
                break;
            case "russell":
                driverImageView.setImageResource(R.drawable.russell_pic);
                driverName.setText(R.string.george_russell);
                teamLogoImageView.setImageResource(R.drawable.mercedeslogo);
                driverColor.setBackground(AppCompatResources.getDrawable(this,R.drawable.gradient_color_mercedes));
                break;
            case "piastri":
                driverImageView.setImageResource(R.drawable.piastri_pic);
                driverName.setText(R.string.oscar_piastri);
                teamLogoImageView.setImageResource(R.drawable.mclarenlogo);
                driverColor.setBackground(AppCompatResources.getDrawable(this,R.drawable.gradient_color_mclaren));
                break;
            case "bearman":
                driverImageView.setImageResource(R.drawable.bearman_pic);
                driverName.setText(R.string.oliver_bearman);
                teamLogoImageView.setImageResource(R.drawable.haaslogo);
                driverColor.setBackground(AppCompatResources.getDrawable(this,R.drawable.gradient_color_haas));
                break;
            case "hulkenberg":
                driverImageView.setImageResource(R.drawable.hulkenberg_pic);
                driverName.setText(R.string.nico_hulkenberg);
                teamLogoImageView.setImageResource(R.drawable.haaslogo);
                driverColor.setBackground(AppCompatResources.getDrawable(this,R.drawable.gradient_color_haas));
                break;
            case "colapinto":
                driverImageView.setImageResource(R.drawable.colapinto_pic);
                driverName.setText(R.string.franco_colapinto);
                teamLogoImageView.setImageResource(R.drawable.williamslogo);
                driverColor.setBackground(AppCompatResources.getDrawable(this,R.drawable.gradient_color_williams));
                break;
            case "lawson":
                driverImageView.setImageResource(R.drawable.lawson_pic);
                driverName.setText(R.string.liam_lawson);
                teamLogoImageView.setImageResource(R.drawable.racingbullslogo);
                driverColor.setBackground(AppCompatResources.getDrawable(this,R.drawable.gradient_color_rb));
                break;
            case "zhou":
                driverImageView.setImageResource(R.drawable.zhou_pic);
                driverName.setText(R.string.zhou_guanyu);
                teamLogoImageView.setImageResource(R.drawable.stakelogo);
                driverColor.setBackground(AppCompatResources.getDrawable(this,R.drawable.gradient_color_sauber));
                break;

            case "sargeant":
                driverImageView.setImageResource(R.drawable.sargeant_pic);
                driverName.setText(R.string.logan_sargeant);
                teamLogoImageView.setImageResource(R.drawable.williamslogo);
                driverColor.setBackground(AppCompatResources.getDrawable(this,R.drawable.gradient_color_williams));
                break;

            case "albon":
                driverImageView.setImageResource(R.drawable.albon_pic);
                driverName.setText(R.string.alexander_albon);
                teamLogoImageView.setImageResource(R.drawable.williamslogo);
                driverColor.setBackground(AppCompatResources.getDrawable(this,R.drawable.gradient_color_williams));
                break;
        }

        driverPosition.setText(String.valueOf(position));
        driverPoints.setText(String.valueOf(points));

        driverCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Driver card clicked");
                Log.i(TAG, "Driver Name " + driverName.getText().toString());
                Log.i(TAG, "Driver Team " + teamLogoImageView.getContentDescription().toString());
                Log.i(TAG, "Driver Points " + driverPoints.getText().toString());
            }
        });

        return driverCard;
    }
}