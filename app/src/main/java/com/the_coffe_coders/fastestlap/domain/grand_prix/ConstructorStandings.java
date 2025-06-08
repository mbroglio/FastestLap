package com.the_coffe_coders.fastestlap.domain.grand_prix;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

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
@Entity(tableName = "ConstructorStandings")
public class ConstructorStandings {
    @PrimaryKey(autoGenerate = true)
    private int uid;
    private String season;
    private String round;
    @SerializedName("ConstructorStandings")
    private List<ConstructorStandingsElement> ConstructorStandings;
}
