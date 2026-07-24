package com.the_coffe_coders.fastestlap.domain.nation;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
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
@AllArgsConstructor(onConstructor_ = @Ignore)
@NoArgsConstructor
@Entity(tableName = "Nation")
public class Nation {
    private long uid;
    @PrimaryKey
    @NonNull
    private String nationId = "";
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
