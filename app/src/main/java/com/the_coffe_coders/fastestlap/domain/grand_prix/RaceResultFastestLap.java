package com.the_coffe_coders.fastestlap.domain.grand_prix;

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
public class RaceResultFastestLap implements Parcelable {
    private String rank;
    private String lap;
    private RaceResultTime time;
    private String driverName;
    private String constructorId;

    protected RaceResultFastestLap(Parcel in) {
        rank = in.readString();
        lap = in.readString();
        driverName = in.readString();
        constructorId = in.readString();
        time = in.readParcelable(RaceResultTime.class.getClassLoader());
    }

    public static final Creator<RaceResultFastestLap> CREATOR = new Creator<RaceResultFastestLap>() {
        @Override
        public RaceResultFastestLap createFromParcel(Parcel in) {
            return new RaceResultFastestLap(in);
        }

        @Override
        public RaceResultFastestLap[] newArray(int size) {
            return new RaceResultFastestLap[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(rank);
        dest.writeString(lap);
        dest.writeString(driverName);
        dest.writeString(constructorId);
        dest.writeParcelable(time, flags);
    }
}
