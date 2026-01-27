package com.the_coffe_coders.fastestlap.repository.f1.constructor;

import com.the_coffe_coders.fastestlap.domain.f1.constructor.Constructor;

public interface ConstructorCallback {
    void onConstructorLoaded(Constructor constructor);

    void onError(Exception e);
}
