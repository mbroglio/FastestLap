package com.the_coffe_coders.fastestlap.data;

import com.the_coffe_coders.fastestlap.domain.race_result.Race;

import java.util.List;

public interface IRaceRepository {
    List<Race> findAll();

    Race find(String id);
}

