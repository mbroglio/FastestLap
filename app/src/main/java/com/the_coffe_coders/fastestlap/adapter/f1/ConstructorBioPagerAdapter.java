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
import com.the_coffe_coders.fastestlap.ui.bio.fragment.ConstructorBioHomeFragmentTab;
import com.the_coffe_coders.fastestlap.ui.bio.fragment.HistoryFragmentTab;
import com.the_coffe_coders.fastestlap.ui.bio.fragment.SeasonPerformanceChartFragmentTab;
import com.the_coffe_coders.fastestlap.util.Constants;

public class ConstructorBioPagerAdapter extends FragmentStateAdapter {

    private final FragmentActivity fragmentActivity;
    private final String teamId;
    private final Constructor constructor;
    private final Nation nation;
    private final Driver driverOne;
    private final Driver driverTwo;

    public ConstructorBioPagerAdapter(@NonNull FragmentActivity fragmentActivity,
                                      String teamId,
                                      Constructor constructor,
                                      Nation nation,
                                      Driver driverOne,
                                      Driver driverTwo) {
        super(fragmentActivity);
        this.fragmentActivity = fragmentActivity;
        this.teamId = teamId;
        this.constructor = constructor;
        this.nation = nation;
        this.driverOne = driverOne;
        this.driverTwo = driverTwo;
    }

    public ConstructorBioPagerAdapter(@NonNull FragmentActivity fragmentActivity, String teamId) {
        this(fragmentActivity, teamId, null, null, null, null);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Integer teamColorRes = null;
        try {
            if (constructor != null) {
                teamColorRes = Constants.TEAM_COLOR.get(constructor.getConstructorId());
            } else if (teamId != null) {
                teamColorRes = Constants.TEAM_COLOR.get(teamId);
            }
        } catch (Exception ignored) {
        }
        int resolvedColor = ContextCompat.getColor(fragmentActivity, teamColorRes != null ? teamColorRes : R.color.app_primary_red);

        switch (position) {
            case 0:
                return ConstructorBioHomeFragmentTab.newInstance(teamId, constructor, nation, driverOne, driverTwo);
            case 1:
                return HistoryFragmentTab.newInstanceForConstructor(constructor, resolvedColor);
            case 2:
                String constructorName = constructor != null && constructor.getName() != null && !constructor.getName().isEmpty()
                        ? constructor.getName()
                        : (constructor != null && constructor.getFull_name() != null ? constructor.getFull_name() : teamId);
                String d1Name = driverOne != null ? driverOne.getGivenName() + " " + driverOne.getFamilyName() : "TBA";
                String d2Name = driverTwo != null ? driverTwo.getGivenName() + " " + driverTwo.getFamilyName() : "TBA";
                return SeasonPerformanceChartFragmentTab.newInstanceForConstructor(
                        teamId,
                        constructorName,
                        resolvedColor,
                        d1Name,
                        d2Name,
                        constructor != null ? constructor.getSeason_stats() : null
                );
        }
        return ConstructorBioHomeFragmentTab.newInstance(teamId, constructor, nation, driverOne, driverTwo);
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
