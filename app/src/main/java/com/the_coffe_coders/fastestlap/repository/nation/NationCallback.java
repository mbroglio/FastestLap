package com.the_coffe_coders.fastestlap.repository.nation;

import com.the_coffe_coders.fastestlap.domain.nation.Nation;

public interface NationCallback {
    void onNationLoaded(Nation nation);
    void onError(Exception e);
}