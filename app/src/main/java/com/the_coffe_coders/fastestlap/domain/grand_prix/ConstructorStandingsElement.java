package com.the_coffe_coders.fastestlap.domain.grand_prix;

import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;

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
public class ConstructorStandingsElement {
    private String position;
    private String positionText;
    private String points;
    private Constructor Constructor;
    private String wins;
}
