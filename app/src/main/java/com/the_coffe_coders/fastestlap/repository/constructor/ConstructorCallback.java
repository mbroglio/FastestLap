package com.the_coffe_coders.fastestlap.repository.constructor;

import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;

public interface ConstructorCallback {
    void onConstructorLoaded(Constructor constructor);

    void onError(Exception e);
}
