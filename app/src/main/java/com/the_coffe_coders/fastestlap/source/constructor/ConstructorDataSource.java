package com.the_coffe_coders.fastestlap.source.constructor;

import com.the_coffe_coders.fastestlap.repository.constructor.ConstructorCallback;

public interface ConstructorDataSource {
    void getConstructor(String constructorId, ConstructorCallback callback);
}
