package com.the_coffe_coders.fastestlap.domain.junior.result;

import android.os.Parcelable;

import java.util.List;

public interface JuniorRaceSession extends Parcelable {
    String getStatus();
    String getFastest_lap();
    String getPole_position();
    List<JuniorSessionResultElement> getOrder();
    List<JuniorSessionResultElement> getPodium();
    boolean isCompleted();
    boolean isCancelled();
    boolean isYetToStart();
}
