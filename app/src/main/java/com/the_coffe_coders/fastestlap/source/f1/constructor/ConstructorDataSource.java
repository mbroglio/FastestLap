package com.the_coffe_coders.fastestlap.source.f1.constructor;

import com.the_coffe_coders.fastestlap.repository.f1.constructor.ConstructorCallback;

public interface ConstructorDataSource {
    void getConstructor(String constructorId, ConstructorCallback callback);
}
