package com.the_coffe_coders.fastestlap.domain.grand_prix;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;

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
@Entity
public class RaceResult {
    @PrimaryKey(autoGenerate = true)
    private int uid;
    private String position;
    private String points;
    private Driver driver;
    private Constructor constructor;
    private String grid;
    private String laps;
    private String status;
}