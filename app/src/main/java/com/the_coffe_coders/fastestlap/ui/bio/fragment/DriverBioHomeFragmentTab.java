package com.the_coffe_coders.fastestlap.ui.bio.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.f1.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.f1.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.f1.driver.DriverSeasonStats;
import com.the_coffe_coders.fastestlap.domain.nation.Nation;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.ui.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ui.NavigationUtils;
import com.the_coffe_coders.fastestlap.util.ui.TachometerView;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

public class DriverBioHomeFragmentTab extends Fragment {

    private static final String TAG = "DriverBioHomeFragmentTab";
    private static final String ARG_DRIVER_ID = "ARG_DRIVER_ID";
    private static final String ARG_DRIVER = "ARG_DRIVER";
    private static final String ARG_NATION = "ARG_NATION";
    private static final String ARG_TEAM = "ARG_TEAM";

    private String driverId;
    private Driver driver;
    private Nation nation;
    private Constructor team;
    private LoadingScreen loadingScreen;

    public DriverBioHomeFragmentTab() {
        // Required empty public constructor
    }

    public static DriverBioHomeFragmentTab newInstance(String driverId, Driver driver, Nation nation, Constructor team) {
        DriverBioHomeFragmentTab fragment = new DriverBioHomeFragmentTab();
        Bundle args = new Bundle();
        args.putString(ARG_DRIVER_ID, driverId);
        if (driver != null) {
            args.putParcelable(ARG_DRIVER, driver);
        }
        if (nation != null) {
            args.putSerializable(ARG_NATION, nation);
        }
        if (team != null) {
            args.putParcelable(ARG_TEAM, team);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            driverId = getArguments().getString(ARG_DRIVER_ID);
            driver = getArguments().getParcelable(ARG_DRIVER);
            nation = (Nation) getArguments().getSerializable(ARG_NATION);
            team = getArguments().getParcelable(ARG_TEAM);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_driver_bio_home_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View contentScroll = view.findViewById(R.id.driver_bio_scroll);
        loadingScreen = new LoadingScreen(view, requireContext(), null, contentScroll);
        loadingScreen.showLoadingScreen(false);
        loadingScreen.updateProgress();

        if (driver == null) {
            loadingScreen.hideLoadingScreenImmediately();
            return;
        }

        bindDriverData(view);
    }

    private void bindDriverData(View view) {
        MaterialCardView teamLogoCard = view.findViewById(R.id.team_logo_card);
        ImageView teamLogoImage = view.findViewById(R.id.team_logo_image);
        ImageView driverNumberImage = view.findViewById(R.id.driver_number_image);

        boolean teamIdPresent = (driver.getTeam_id() != null && team != null);
        Integer teamColor = null;
        if (teamIdPresent) {
            try {
                teamColor = Constants.TEAM_COLOR.get(driver.getTeam_id());
            } catch (Exception e) {
                teamColor = R.color.timer_gray;
            }
        }
        if (teamColor == null) {
            teamColor = R.color.timer_gray;
        }

        if (teamLogoCard != null) {
            if (teamIdPresent) {
                teamLogoCard.setStrokeColor(ContextCompat.getColor(requireContext(), teamColor));
                if ("rb".equals(team.getConstructorId())) {
                    teamLogoCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white));
                }
                teamLogoCard.setOnClickListener(v ->
                        NavigationUtils.navigateToBioPage(requireContext(), team.getConstructorId(), 0));
            } else {
                teamLogoCard.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.timer_gray));
            }
        }

        TextView seasonStatsTitle = view.findViewById(R.id.season_stats_title);
        if (seasonStatsTitle != null && teamIdPresent) {
            seasonStatsTitle.setTextColor(ContextCompat.getColor(requireContext(), teamColor));
        }

        // Popola tutti i dati di testo
        UIUtils.multipleSetTextViewText(
                new String[]{driver.getBirth_place(),
                        driver.getDateOfBirth(),
                        driver.getDriverAgeAsString(),
                        driver.getWeight(),
                        driver.getHeight(),
                        driver.getBest_result(),
                        driver.getPodiums(),
                        driver.getChampionships(),
                        driver.getFirst_entry()},

                new TextView[]{view.findViewById(R.id.driver_birthplace),
                        view.findViewById(R.id.driver_birthdate),
                        view.findViewById(R.id.driver_age),
                        view.findViewById(R.id.driver_weight),
                        view.findViewById(R.id.driver_height),
                        view.findViewById(R.id.driver_best_result),
                        view.findViewById(R.id.driver_podiums),
                        view.findViewById(R.id.driver_championships),
                        view.findViewById(R.id.driver_first_entry)
                }
        );

        DriverSeasonStats seasonStats = driver.getSeason_stats();
        String seasonWins = "0";
        String seasonPodiums = "0";
        String seasonPoles = "0";
        String seasonDnfs = "0";
        String seasonPosition = "-";
        String seasonPoints = "0";

        if (seasonStats != null) {
            if (seasonStats.getWins() != null) seasonWins = seasonStats.getWins();
            if (seasonStats.getPodiums() != null) seasonPodiums = seasonStats.getPodiums();
            if (seasonStats.getPoles() != null) seasonPoles = seasonStats.getPoles();
            if (seasonStats.getDnfs() != null) seasonDnfs = seasonStats.getDnfs();
            if (seasonStats.getSeason_position() != null) seasonPosition = seasonStats.getSeason_position();
            if (seasonStats.getSeason_points() != null) seasonPoints = seasonStats.getSeason_points();
        }

        UIUtils.multipleSetTextViewText(
                new String[]{seasonWins, seasonPodiums, seasonPoles, seasonDnfs, seasonPosition, seasonPoints},
                new TextView[]{
                        view.findViewById(R.id.season_wins),
                        view.findViewById(R.id.season_podiums),
                        view.findViewById(R.id.season_poles),
                        view.findViewById(R.id.season_dnfs),
                        view.findViewById(R.id.season_position),
                        view.findViewById(R.id.season_points)
                }
        );

        TextView driverSeasonPosition = view.findViewById(R.id.season_position);
        if (driverSeasonPosition != null && !seasonPosition.equals("-")) {
            if (seasonPosition.equals("1")) {
                driverSeasonPosition.setTextColor(ContextCompat.getColor(requireContext(), R.color.yellow));
            } else if (seasonPosition.equals("2")) {
                driverSeasonPosition.setTextColor(ContextCompat.getColor(requireContext(), R.color.silver));
            } else if (seasonPosition.equals("3")) {
                driverSeasonPosition.setTextColor(ContextCompat.getColor(requireContext(), R.color.bronze));
            }
        }

        String nationFlagUrl = nation != null ? nation.getNation_flag_url() : null;
        String teamLogoUrl = team != null ? team.getTeam_logo_url() : null;

        ImageView flagImageView = view.findViewById(R.id.flag);

        UIUtils.loadImagesInParallel(requireContext(),
                new String[]{
                        teamLogoUrl,
                        nationFlagUrl,
                        driver.getDriver_full_pic_url(),
                        driver.getRacing_number_pic_url()},
                new ImageView[]{
                        teamLogoImage,
                        flagImageView,
                        view.findViewById(R.id.driver_bio_pic),
                        driverNumberImage},
                () -> {
                    Log.i(TAG, "DATA_AND_IMAGES_FULLY_LOADED: DriverBioHomeFragmentTab at " + System.currentTimeMillis());
                    if (loadingScreen != null) {
                        loadingScreen.hideLoadingScreenImmediately();
                    }
                });

        if (loadingScreen != null) {
            loadingScreen.hideLoadingScreen();
        }
    }
}