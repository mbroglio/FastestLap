package com.the_coffe_coders.fastestlap.domain.nation;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Nation nation = (Nation) o;
        return Objects.equals(nationId, nation.nationId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(nationId);
    }
}
