package com.the_coffe_coders.fastestlap.domain.f1.driver;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Map;

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
public class DriverSeasonStats implements Parcelable {
    private String wins;
    private String podiums;
    private String dnfs;
    private String poles;
    private String season_position;
    private String season_points;
    private Map<String, EventResultDriver> season_results;

    protected DriverSeasonStats(Parcel in) {
        wins = in.readString();
        podiums = in.readString();
        dnfs = in.readString();
        poles = in.readString();
        season_position = in.readString();
        season_points = in.readString();
        season_results = in.readHashMap(EventResultDriver.class.getClassLoader());
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeString(wins);
        parcel.writeString(podiums);
        parcel.writeString(dnfs);
        parcel.writeString(poles);
        parcel.writeString(season_position);
        parcel.writeString(season_points);
        parcel.writeMap(season_results);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<DriverSeasonStats> CREATOR = new Creator<>() {
        @Override
        public DriverSeasonStats createFromParcel(Parcel in) {
            return new DriverSeasonStats(in);
        }

        @Override
        public DriverSeasonStats[] newArray(int size) {
            return new DriverSeasonStats[size];
        }
    };

}
