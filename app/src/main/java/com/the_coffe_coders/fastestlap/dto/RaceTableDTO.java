package com.the_coffe_coders.fastestlap.dto;


import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class RaceTableDTO {
    private String season;
    private String round;
    @SerializedName("Circuit")
    private CircuitDTO circuit;
    @SerializedName("Races")
    private List<RaceDTO> races;

    public RaceTableDTO(String season, String round, CircuitDTO circuit, List<RaceDTO> races) {
        this.season = season;
        //this.circuit = circuit;
        this.races = races;
        this.round = round;
    }
}
