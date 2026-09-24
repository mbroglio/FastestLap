package com.the_coffe_coders.fastestlap.domain.f1.constructor;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

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
public class ConstructorSeasonStats implements Parcelable {
    private String wins;
    private String podiums;
    private String dnfs;
    private String poles;
    private String season_position;
    private String season_points;

    protected ConstructorSeasonStats(Parcel in) {
        wins = in.readString();
        podiums = in.readString();
        dnfs = in.readString();
        poles = in.readString();
        season_position = in.readString();
        season_points = in.readString();
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeString(wins);
        parcel.writeString(podiums);
        parcel.writeString(dnfs);
        parcel.writeString(poles);
        parcel.writeString(season_position);
        parcel.writeString(season_points);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ConstructorSeasonStats> CREATOR = new Creator<>() {
        @Override
        public ConstructorSeasonStats createFromParcel(Parcel in) {
            return new ConstructorSeasonStats(in);
        }

        @Override
        public ConstructorSeasonStats[] newArray(int size) {
            return new ConstructorSeasonStats[size];
        }
    };
}
