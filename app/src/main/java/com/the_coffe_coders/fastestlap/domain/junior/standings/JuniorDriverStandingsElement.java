package com.the_coffe_coders.fastestlap.domain.junior.standings;

import android.os.Parcel;
import android.os.Parcelable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString

public class JuniorDriverStandingsElement implements Parcelable {
    public static final Creator<JuniorDriverStandingsElement> CREATOR = new Creator<>() {
        @Override
        public JuniorDriverStandingsElement createFromParcel(Parcel in) {
            return new JuniorDriverStandingsElement(in);
        }

        @Override
        public JuniorDriverStandingsElement[] newArray(int size) {
            return new JuniorDriverStandingsElement[size];
        }
    };

    private String driver;
    private String points;
    private String position;
    private String team;


    protected JuniorDriverStandingsElement(Parcel in) {
        driver = in.readString();
        points = in.readString();
        position = in.readString();
        team = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(driver);
        dest.writeString(points);
        dest.writeString(position);
        dest.writeString(team);
    }
}
