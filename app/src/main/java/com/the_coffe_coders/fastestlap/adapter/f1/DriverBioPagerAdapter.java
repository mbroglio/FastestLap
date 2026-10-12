package com.the_coffe_coders.fastestlap.adapter.f1;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.f1.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.f1.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.nation.Nation;
import com.the_coffe_coders.fastestlap.ui.bio.fragment.DriverBioHomeFragmentTab;
import com.the_coffe_coders.fastestlap.ui.bio.fragment.HistoryFragmentTab;
import com.the_coffe_coders.fastestlap.ui.bio.fragment.SeasonPerformanceChartFragmentTab;
import com.the_coffe_coders.fastestlap.util.Constants;

public class DriverBioPagerAdapter extends FragmentStateAdapter {

    private final FragmentActivity fragmentActivity;
    private final String driverId;
    private final Driver driver;
    private final Nation nation;
    private final Constructor team;

    public DriverBioPagerAdapter(@NonNull FragmentActivity fragmentActivity,
                                 String driverId,
                                 Driver driver,
                                 Nation nation,
                                 Constructor team) {
        super(fragmentActivity);
        this.fragmentActivity = fragmentActivity;
        this.driverId = driverId;
        this.driver = driver;
        this.nation = nation;
        this.team = team;
    }

    public DriverBioPagerAdapter(@NonNull FragmentActivity fragmentActivity, String driverId) {
        this(fragmentActivity, driverId, null, null, null);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Integer teamColorRes = null;
        try {
            if (driver != null && driver.getTeam_id() != null) {
                teamColorRes = Constants.TEAM_COLOR.get(driver.getTeam_id());
            } else if (team != null && team.getConstructorId() != null) {
                teamColorRes = Constants.TEAM_COLOR.get(team.getConstructorId());
            }
        } catch (Exception ignored) {}
        int resolvedColor = ContextCompat.getColor(fragmentActivity, teamColorRes != null ? teamColorRes : R.color.app_primary_red);

        switch (position) {
            case 0:
                return DriverBioHomeFragmentTab.newInstance(driverId, driver, nation, team);
            case 1:
                return HistoryFragmentTab.newInstanceForDriver(driver, resolvedColor);
            case 2:
                String fullName = driver != null ? driver.getFullName() : "";
                String teamId = driver != null && driver.getTeam_id() != null ? driver.getTeam_id() : (team != null ? team.getConstructorId() : "");
                return SeasonPerformanceChartFragmentTab.newInstanceForDriver(
                        driverId,
                        fullName,
                        teamId,
                        resolvedColor,
                        driver != null ? driver.getSeason_stats() : null
                );
        }
        return DriverBioHomeFragmentTab.newInstance(driverId, driver, nation, team);
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
