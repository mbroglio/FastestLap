package com.the_coffe_coders.fastestlap.domain.grand_prix;

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
public class Location {
    private String latitude;
    private String longitude;
    private String locality;
    private String country;
}
