package com.the_coffe_coders.fastestlap.domain.grand_prix;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;

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
@Entity
public class RaceResult implements Parcelable {
    public static final Creator<RaceResult> CREATOR = new Creator<>() {
        @Override
        public RaceResult createFromParcel(Parcel in) {
            return new RaceResult(in);
        }

        @Override
        public RaceResult[] newArray(int size) {
            return new RaceResult[size];
        }
    };
    @PrimaryKey(autoGenerate = true)
    private int uid;
    private String position;
    private String points;
    private Driver driver;
    private Constructor constructor;
    private String grid;
    private String laps;
    private String status;
    private RaceResultTime time;
    private RaceResultFastestLap fastestLap;

    protected RaceResult(Parcel in) {
        uid = in.readInt();
        position = in.readString();
        points = in.readString();
        driver = in.readParcelable(Driver.class.getClassLoader());
        constructor = in.readParcelable(Constructor.class.getClassLoader());
        grid = in.readString();
        laps = in.readString();
        status = in.readString();
        time = in.readParcelable(RaceResultTime.class.getClassLoader());
        fastestLap = in.readParcelable(RaceResultFastestLap.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(uid);
        dest.writeString(position);
        dest.writeString(points);
        dest.writeParcelable(driver, flags);
        dest.writeParcelable(constructor, flags);
        dest.writeString(grid);
        dest.writeString(laps);
        dest.writeString(status);
        dest.writeParcelable(time, flags);
        dest.writeParcelable(fastestLap, flags);
    }

    public boolean isFinished() {
        return status.equalsIgnoreCase("finished");
    }
}