package com.the_coffe_coders.fastestlap.domain.junior.standings;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.List;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor(onConstructor_ = @Ignore)
@NoArgsConstructor
@Getter
@Setter
@ToString

@Entity(tableName = "JuniorConstructorStandings")
public class JuniorConstructorStandings implements Parcelable {

    public static final Creator<JuniorConstructorStandings> CREATOR = new Creator<>() {
        @Override
        public JuniorConstructorStandings createFromParcel(Parcel in) {
            return new JuniorConstructorStandings(in);
        }

        @Override
        public JuniorConstructorStandings[] newArray(int size) {
            return new JuniorConstructorStandings[size];
        }
    };

    @PrimaryKey
    @NonNull
    private String series;
    private List<JuniorConstructorStandingsElement> constructorStandings;

    protected JuniorConstructorStandings(Parcel in) {
        series = Objects.requireNonNull(in.readString());
        constructorStandings = in.createTypedArrayList(JuniorConstructorStandingsElement.CREATOR);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(series);
        dest.writeTypedList(constructorStandings);
    }

}
