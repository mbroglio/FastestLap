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

@Entity(tableName = "JuniorDriverStandings")
public class JuniorDriverStandings implements Parcelable {

    public static final Creator<JuniorDriverStandings> CREATOR = new Creator<>() {
        @Override
        public JuniorDriverStandings createFromParcel(Parcel in) {
            return new JuniorDriverStandings(in);
        }

        @Override
        public JuniorDriverStandings[] newArray(int size) {
            return new JuniorDriverStandings[size];
        }
    };

    @PrimaryKey
    @NonNull
    public String series;
    private List<JuniorDriverStandingsElement> driverStandings;

    protected JuniorDriverStandings(Parcel in) {
        series = Objects.requireNonNull(in.readString());
        driverStandings = in.createTypedArrayList(JuniorDriverStandingsElement.CREATOR);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(series);
        dest.writeTypedList(driverStandings);
    }

}
