package com.the_coffe_coders.fastestlap.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Race;
import com.the_coffe_coders.fastestlap.domain.f1.result.Stint;
import com.the_coffe_coders.fastestlap.ui.event.fragment.RaceResultsTabFragment;
import com.the_coffe_coders.fastestlap.ui.event.fragment.StintsResultsTabFragment;

import java.util.List;

/**
 * PagerAdapter per gestire le tab Risultati e Stint in RaceAndSprintResultsActivity.
 */
public class RaceAndSprintResultsPagerAdapter extends FragmentStateAdapter {

    private final Race race;
    private final List<Stint> stints;

    public RaceAndSprintResultsPagerAdapter(@NonNull FragmentActivity fragmentActivity, Race race, List<Stint> stints) {
        super(fragmentActivity);
        this.race = race;
        this.stints = stints;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 1) {
            List<Stint> stintsToPass = stints;
            if (stintsToPass == null && race != null) {
                stintsToPass = race.getStints();
            }
            return StintsResultsTabFragment.newInstance(race, stintsToPass);
        }
        return RaceResultsTabFragment.newInstance(race);
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
