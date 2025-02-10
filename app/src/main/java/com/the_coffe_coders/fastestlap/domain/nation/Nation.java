package com.the_coffe_coders.fastestlap.domain.nation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
@AllArgsConstructor
public class Nation {
    private String nationId;
    private String abbreviation;
    private String nation_flag_url;
}
