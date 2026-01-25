package com.the_coffe_coders.fastestlap.mapper;


import com.the_coffe_coders.fastestlap.domain.f1.constructor.Constructor;
import com.the_coffe_coders.fastestlap.dto.standing.constructor.ConstructorDTO;

public class ConstructorMapper {
    public static Constructor toConstructor(ConstructorDTO constructorDTO) {
        Constructor constructor = new Constructor();

        constructor.setConstructorId(constructorDTO.getConstructorId());
        constructor.setUrl(constructorDTO.getUrl());
        constructor.setName(constructorDTO.getName());
        constructor.setNationality(constructorDTO.getNationality());

        return constructor;
    }
}
