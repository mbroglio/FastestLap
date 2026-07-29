package com.the_coffe_coders.fastestlap.repository.junior.result;

import com.the_coffe_coders.fastestlap.domain.junior.result.JuniorResult;

public interface JuniorResultCallback {
    void onResultLoaded(JuniorResult result);

    void onError(Exception e);

}
