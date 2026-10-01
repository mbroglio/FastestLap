package com.the_coffe_coders.fastestlap.repository.f1.result;

import com.the_coffe_coders.fastestlap.domain.f1.result.Stint;
import java.util.List;

public interface StintCallback {
    void onSuccess(List<Stint> stints);
    void onFailure(Exception exception);
}
