package com.the_coffe_coders.fastestlap.domain.nation;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity(tableName = "Nation")
public class Nation {
    @PrimaryKey(autoGenerate = true)
    private long uid;
    private String nationId;
    private String abbreviation;
    private String nation_flag_url;
}
