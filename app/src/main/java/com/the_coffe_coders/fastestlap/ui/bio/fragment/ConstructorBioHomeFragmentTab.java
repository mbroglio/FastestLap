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
import com.the_coffe_coders.fastestlap.domain.f1.constructor.ConstructorSeasonStats;
import com.the_coffe_coders.fastestlap.domain.f1.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.nation.Nation;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.ui.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ui.NavigationUtils;
import com.the_coffe_coders.fastestlap.util.ui.TachometerView;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

public class ConstructorBioHomeFragmentTab extends Fragment {

    private static final String TAG = "ConstructorBioHomeTab";
    private static final String ARG_TEAM_ID = "ARG_TEAM_ID";
    private static final String ARG_CONSTRUCTOR = "ARG_CONSTRUCTOR";
    private static final String ARG_NATION = "ARG_NATION";
    private static final String ARG_DRIVER_ONE = "ARG_DRIVER_ONE";
    private static final String ARG_DRIVER_TWO = "ARG_DRIVER_TWO";

    private String teamId;
    private Constructor constructor;
    private Nation nation;
    private Driver driverOne;
    private Driver driverTwo;
    private LoadingScreen loadingScreen;

    public ConstructorBioHomeFragmentTab() {
        // Required empty public constructor
    }

    public static ConstructorBioHomeFragmentTab newInstance(String teamId,
                                                             Constructor constructor,
                                                             Nation nation,
                                                             Driver driverOne,
                                                             Driver driverTwo) {
        ConstructorBioHomeFragmentTab fragment = new ConstructorBioHomeFragmentTab();
        Bundle args = new Bundle();
        args.putString(ARG_TEAM_ID, teamId);
        if (constructor != null) {
            args.putParcelable(ARG_CONSTRUCTOR, constructor);
        }
        if (nation != null) {
            args.putSerializable(ARG_NATION, nation);
        }
        if (driverOne != null) {
            args.putParcelable(ARG_DRIVER_ONE, driverOne);
        }
        if (driverTwo != null) {
            args.putParcelable(ARG_DRIVER_TWO, driverTwo);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            teamId = getArguments().getString(ARG_TEAM_ID);
            constructor = getArguments().getParcelable(ARG_CONSTRUCTOR);
            nation = (Nation) getArguments().getSerializable(ARG_NATION);
            driverOne = getArguments().getParcelable(ARG_DRIVER_ONE);
            driverTwo = getArguments().getParcelable(ARG_DRIVER_TWO);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_constructor_bio_home_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View contentScroll = view.findViewById(R.id.constructor_bio_scroll);
        loadingScreen = new LoadingScreen(view, requireContext(), null, contentScroll);
        loadingScreen.showLoadingScreen(false);
        loadingScreen.updateProgress();

        if (constructor == null) {
            loadingScreen.hideLoadingScreenImmediately();
            return;
        }

        bindConstructorData(view);
    }

    private void bindConstructorData(View view) {
        Integer teamColor = null;
        try {
            teamColor = Constants.TEAM_COLOR.get(constructor.getConstructorId());
        } catch (Exception ignored) {}
        if (teamColor == null) {
            teamColor = R.color.timer_gray;
        }

        TextView teamInfoTitle = view.findViewById(R.id.team_info_title);
        if (teamInfoTitle != null) {
            teamInfoTitle.setTextColor(ContextCompat.getColor(requireContext(), teamColor));
        }

        TextView driversTitle = view.findViewById(R.id.drivers_title);
        if (driversTitle != null) {
            driversTitle.setTextColor(ContextCompat.getColor(requireContext(), teamColor));
        }

        TextView teamSeasonStatsTitle = view.findViewById(R.id.season_stats_title);
        if (teamSeasonStatsTitle != null) {
            teamSeasonStatsTitle.setTextColor(ContextCompat.getColor(requireContext(), teamColor));
        }

        MaterialCardView teamLogoCard = view.findViewById(R.id.team_logo_card);
        if (teamLogoCard != null) {
            teamLogoCard.setStrokeColor(ContextCompat.getColor(requireContext(), teamColor));
            if ("rb".equals(constructor.getConstructorId()) || "rb".equals(teamId)) {
                teamLogoCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white));
            }
        }

        MaterialCardView driverOneCard = view.findViewById(R.id.driver_1_card);
        if (driverOneCard != null) {
            UIUtils.styleDriverCard(requireContext(), driverOneCard, teamColor);
            if (driverOne != null) {
                driverOneCard.setOnClickListener(v ->
                        NavigationUtils.navigateToBioPage(requireContext(), driverOne.getDriverId(), 1));
            }
        }

        MaterialCardView driverTwoCard = view.findViewById(R.id.driver_2_card);
        if (driverTwoCard != null) {
            UIUtils.styleDriverCard(requireContext(), driverTwoCard, teamColor);
            if (driverTwo != null) {
                driverTwoCard.setOnClickListener(v ->
                        NavigationUtils.navigateToBioPage(requireContext(), driverTwo.getDriverId(), 1));
            }
        }

        String d1Name = driverOne != null ? driverOne.getGivenName() + " " + driverOne.getFamilyName() : "TBA";
        String d2Name = driverTwo != null ? driverTwo.getGivenName() + " " + driverTwo.getFamilyName() : "TBA";

        UIUtils.multipleSetTextViewText(
                new String[]{d1Name,
                        d2Name,
                        constructor.getFull_name(),
                        constructor.getHq(),
                        constructor.getTeam_principal(),
                        constructor.getChassis(),
                        constructor.getPower_unit(),
                        constructor.getFirst_entry(),
                        constructor.getWorld_championships(),
                        constructor.getWins(),
                        constructor.getPodiums()},

                new TextView[]{view.findViewById(R.id.driver_1_name),
                        view.findViewById(R.id.driver_2_name),
                        view.findViewById(R.id.team_full_name_value),
                        view.findViewById(R.id.team_base_value),
                        view.findViewById(R.id.team_principal_value),
                        view.findViewById(R.id.team_chassis_value),
                        view.findViewById(R.id.team_power_unit_value),
                        view.findViewById(R.id.team_first_entry_value),
                        view.findViewById(R.id.team_championships_value),
                        view.findViewById(R.id.team_wins_value),
                        view.findViewById(R.id.team_podiums_value)});

        ConstructorSeasonStats seasonStats = constructor.getSeason_stats();
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
            if (seasonStats.getSeason_position() != null && !seasonStats.getSeason_position().equals("0")) {
                seasonPosition = seasonStats.getSeason_position();
            }
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

        TextView teamSeasonPosition = view.findViewById(R.id.season_position);
        if (teamSeasonPosition != null && !seasonPosition.equals("-")) {
            if (seasonPosition.equals("1")) {
                teamSeasonPosition.setTextColor(ContextCompat.getColor(requireContext(), R.color.yellow));
            } else if (seasonPosition.equals("2")) {
                teamSeasonPosition.setTextColor(ContextCompat.getColor(requireContext(), R.color.silver));
            } else if (seasonPosition.equals("3")) {
                teamSeasonPosition.setTextColor(ContextCompat.getColor(requireContext(), R.color.bronze));
            }
        }

        String nationFlagUrl = nation != null ? nation.getNation_flag_url() : null;
        String d1HalfPic = driverOne != null ? driverOne.getDriver_half_pic_url() : null;
        String d2HalfPic = driverTwo != null ? driverTwo.getDriver_half_pic_url() : null;

        ImageView flagImageView = view.findViewById(R.id.flag);

        UIUtils.loadImagesInParallel(requireContext(),
                new String[]{
                        constructor.getTeam_logo_url(),
                        nationFlagUrl,
                        constructor.getCar_pic_url(),
                        d1HalfPic,
                        d2HalfPic},
                new ImageView[]{
                        view.findViewById(R.id.team_logo_image),
                        flagImageView,
                        view.findViewById(R.id.team_car_image),
                        view.findViewById(R.id.driver_1_image),
                        view.findViewById(R.id.driver_2_image)},
                () -> {
                    Log.i(TAG, "DATA_AND_IMAGES_FULLY_LOADED: ConstructorBioHomeFragmentTab at " + System.currentTimeMillis());
                    if (loadingScreen != null) {
                        loadingScreen.hideLoadingScreenImmediately();
                    }
                });

        if (loadingScreen != null) {
            loadingScreen.hideLoadingScreen();
        }
    }
}