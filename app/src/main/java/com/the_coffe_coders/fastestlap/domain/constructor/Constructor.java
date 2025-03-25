package com.the_coffe_coders.fastestlap.domain.constructor;

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
public class Constructor {
    private String constructorId;
    private String url;
    private String name;
    private String nationality;

    // Bio Data
    private String car_pic_url;
    private String chassis;
    private List<String> drivers;
    private String first_entry;
    private String full_name;
    private String hq;
    private String podiums;
    private String power_unit;
    private List<ConstructorHistory> team_history;
    private String team_logo_url;
    private String team_logo_minimal_url;
    private String team_principal;
    private String wins;
    private String world_championships;

    public String getDriverOneId() {
        return drivers.get(0);
    }

    public String getDriverTwoId() {
        return drivers.get(1);
    }
}
