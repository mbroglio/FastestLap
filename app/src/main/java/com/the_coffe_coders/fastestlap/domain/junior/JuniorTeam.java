package com.the_coffe_coders.fastestlap.domain.junior;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class JuniorTeam {
    private String name;
    private JuniorDriver[] drivers;
    private String teamLogoUrl;
}
