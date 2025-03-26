package com.the_coffe_coders.fastestlap.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class ConstructorTableDTO {
    String constructorId;
    String season;
    @SerializedName("Constructors")
    List<ConstructorDTO> constructorDTOList;
}
