package com.the_coffe_coders.fastestlap.domain.grand_prix;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TrackHistory {
    private List<String> podium;
    private List<String> team;
    private String year;
    private String raceHighlightsUrl;
}
