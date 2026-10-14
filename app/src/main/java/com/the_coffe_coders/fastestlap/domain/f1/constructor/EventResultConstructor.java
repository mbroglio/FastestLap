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
public class EventResultConstructor implements Parcelable {
    private String qualifying_driver_1;
    private String qualifying_driver_2;
    private String sprint_qualifying_driver_1;
    private String sprint_qualifying_driver_2;
    private String race_driver_1;
    private String race_driver_2;
    private String sprint_race_driver_1;
    private String sprint_race_driver_2;
    private String nation_abbr;

    protected EventResultConstructor(Parcel in) {
        qualifying_driver_1 = in.readString();
        qualifying_driver_2 = in.readString();
        sprint_qualifying_driver_1 = in.readString();
        sprint_qualifying_driver_2 = in.readString();
        race_driver_1 = in.readString();
        race_driver_2 = in.readString();
        sprint_race_driver_1 = in.readString();
        sprint_race_driver_2 = in.readString();
        nation_abbr = in.readString();
    }

    public static final Creator<EventResultConstructor> CREATOR = new Creator<>() {
        @Override
        public EventResultConstructor createFromParcel(Parcel in) {
            return new EventResultConstructor(in);
        }

        @Override
        public EventResultConstructor[] newArray(int size) {
            return new EventResultConstructor[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeString(qualifying_driver_1);
        parcel.writeString(qualifying_driver_2);
        parcel.writeString(sprint_qualifying_driver_1);
        parcel.writeString(sprint_qualifying_driver_2);
        parcel.writeString(race_driver_1);
        parcel.writeString(race_driver_2);
        parcel.writeString(sprint_race_driver_1);
        parcel.writeString(sprint_race_driver_2);
        parcel.writeString(nation_abbr);
    }
}
