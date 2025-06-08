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
public class RaceResultTime implements Parcelable {
    public static final Creator<RaceResultTime> CREATOR = new Creator<>() {
        @Override
        public RaceResultTime createFromParcel(Parcel in) {
            return new RaceResultTime(in);
        }

        @Override
        public RaceResultTime[] newArray(int size) {
            return new RaceResultTime[size];
        }
    };
    private String time;
    private String millis;

    protected RaceResultTime(Parcel in) {
        time = in.readString();
        millis = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(time);
        dest.writeString(millis);
    }
}
