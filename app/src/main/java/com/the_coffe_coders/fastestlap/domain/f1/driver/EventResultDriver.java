package com.the_coffe_coders.fastestlap.domain.f1.driver;

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
public class EventResultDriver implements Parcelable {
    private String qualifying;
    private String sprint_qualifying;
    private String race;
    private String sprint_race;

    protected EventResultDriver(Parcel in) {
        qualifying = in.readString();
        sprint_qualifying = in.readString();
        race = in.readString();
        sprint_race = in.readString();
    }

    public static final Creator<EventResultDriver> CREATOR = new Creator<>() {
        @Override
        public EventResultDriver createFromParcel(Parcel in) {
            return new EventResultDriver(in);
        }

        @Override
        public EventResultDriver[] newArray(int size) {
            return new EventResultDriver[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeString(qualifying);
        parcel.writeString(sprint_qualifying);
        parcel.writeString(race);
        parcel.writeString(sprint_race);
    }
}
