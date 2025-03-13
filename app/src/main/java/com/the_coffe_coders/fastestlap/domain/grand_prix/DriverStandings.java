package com.the_coffe_coders.fastestlap.domain.grand_prix;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Entity(tableName = "DriverStandings")
public class DriverStandings {
    @PrimaryKey(autoGenerate = true)
    private long uid;
    private String season;
    private String round;
    private List<DriverStandingsElement> driverStandingsElements;
}
