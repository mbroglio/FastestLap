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

public class JuniorConstructorStandingsElement implements Parcelable {

    public static final Creator<JuniorConstructorStandingsElement> CREATOR = new Creator<>() {
        @Override
        public JuniorConstructorStandingsElement createFromParcel(Parcel in) {
            return new JuniorConstructorStandingsElement(in);
        }

        @Override
        public JuniorConstructorStandingsElement[] newArray(int size) {
            return new JuniorConstructorStandingsElement[size];
        }
    };

    private String team;
    private String points;
    private String position;

    protected JuniorConstructorStandingsElement(Parcel in) {
        team = in.readString();
        points = in.readString();
        position = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(team);
        dest.writeString(points);
        dest.writeString(position);
    }
}
