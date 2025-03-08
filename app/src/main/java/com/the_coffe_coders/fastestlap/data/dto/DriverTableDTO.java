package com.the_coffe_coders.fastestlap.data.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@AllArgsConstructor
public class DriverTableDTO {
    String driverId;
    String season;
    @SerializedName("Drivers")
    List<DriverDTO> driverDTOList;
}
