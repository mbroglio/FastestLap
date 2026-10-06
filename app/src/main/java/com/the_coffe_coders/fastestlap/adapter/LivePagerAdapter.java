package com.the_coffe_coders.fastestlap.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.the_coffe_coders.fastestlap.ui.live.fragment.*;

public class LivePagerAdapter extends FragmentStateAdapter {

    private String circuitId = null;

    public LivePagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    public LivePagerAdapter(@NonNull FragmentActivity fragmentActivity, String circuitId) {
        super(fragmentActivity);
        this.circuitId = circuitId;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new LiveEventFragment();
            case 1:
                return new RaceControlFragment();
            case 2:
                return new VersusFragment();
            case 3:
                return circuitId != null ? LiveTrackMapFragment.newInstance(circuitId) : new LiveTrackMapFragment();
        }
        return new LiveEventFragment();
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
