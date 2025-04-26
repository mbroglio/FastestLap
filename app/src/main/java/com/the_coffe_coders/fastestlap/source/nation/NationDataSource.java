package com.the_coffe_coders.fastestlap.source.nation;

import com.the_coffe_coders.fastestlap.repository.nation.NationCallback;

public interface NationDataSource {
    void getNation(String nationId, NationCallback callback);
}