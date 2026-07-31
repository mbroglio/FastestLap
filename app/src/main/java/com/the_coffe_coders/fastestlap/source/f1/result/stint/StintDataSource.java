package com.the_coffe_coders.fastestlap.source.f1.result.stint;

import com.the_coffe_coders.fastestlap.repository.f1.result.StintCallback;

public interface StintDataSource {
    void getStints(String eventName, String sessionName, StintCallback callback);
}
