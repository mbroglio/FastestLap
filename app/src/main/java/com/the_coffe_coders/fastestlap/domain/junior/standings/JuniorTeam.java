package com.the_coffe_coders.fastestlap.domain.junior.standings;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

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

public class JuniorTeam implements Parcelable {
    public static final Creator<JuniorTeam> CREATOR = new Creator<>() {
        @Override
        public JuniorTeam createFromParcel(Parcel in) {
            return new JuniorTeam(in);
        }

        @Override
        public JuniorTeam[] newArray(int size) {
            return new JuniorTeam[size];
        }
    };

    private String name;
    private List<JuniorDriver> drivers;
    private String teamLogoUrl;


    protected JuniorTeam(Parcel in) {
        name = in.readString();
        drivers = in.createTypedArrayList(JuniorDriver.CREATOR);
        teamLogoUrl = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeTypedList(drivers);
        dest.writeString(teamLogoUrl);
    }
}
