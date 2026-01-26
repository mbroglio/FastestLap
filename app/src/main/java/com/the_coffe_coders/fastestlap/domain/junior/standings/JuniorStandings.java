package com.the_coffe_coders.fastestlap.domain.junior.standings;


import androidx.room.Entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter

@Entity(tableName = "JuniorStandings")
public class JuniorStandings {
    private JuniorConstructorStandings constructors;

}
