package com.the_coffe_coders.fastestlap.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@AllArgsConstructor
public class ConstructorStandingsTableDTO {
    private String season;
    private String round;
    private List<ConstructorStandingsDTO> StandingsLists;
}