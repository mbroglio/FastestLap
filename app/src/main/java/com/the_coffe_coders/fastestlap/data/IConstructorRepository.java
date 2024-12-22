package com.the_coffe_coders.fastestlap.data;

import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;

import java.util.List;

public interface IConstructorRepository {
    List<Constructor> findConstructors();

    Constructor find(String id);
}
