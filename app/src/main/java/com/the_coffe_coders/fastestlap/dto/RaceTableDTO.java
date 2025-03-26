package com.the_coffe_coders.fastestlap.dto;


import com.google.gson.annotations.SerializedName;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class RaceTableDTO {
    private String season;
    private String round;
    @SerializedName("Circuit")
    private CircuitDTO circuit;
    @SerializedName("Races")
    private List<RaceDTO> races;

    public RaceDTO getRace() {
        if (races == null || races.isEmpty()) {
            return null;
        }else
            return races.get(0);
    }
}
