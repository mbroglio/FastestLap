package com.the_coffe_coders.fastestlap.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.AllArgsConstructor;

@Setter
@Getter
@ToString
@AllArgsConstructor
public class ConstructorStandingsDTO {
    private String season;
    private String round;
    private List<ConstructorStandingsElementDTO> ConstructorStandings;
}