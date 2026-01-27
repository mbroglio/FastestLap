package com.the_coffe_coders.fastestlap.domain.junior.result;

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

public class FeatureRace implements Parcelable {
    public static final Creator<FeatureRace> CREATOR = new Creator<>() {
        @Override
        public FeatureRace createFromParcel(Parcel in) {
            return new FeatureRace(in);
        }

        @Override
        public FeatureRace[] newArray(int size) {
            return new FeatureRace[size];
        }
    };

    private String fastest_lap;
    private String pole_position;
    private List<JuniorSessionResultElement> order;

    protected FeatureRace(Parcel in) {
        fastest_lap = in.readString();
        pole_position = in.readString();
        order = in.createTypedArrayList(JuniorSessionResultElement.CREATOR);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(fastest_lap);
        dest.writeString(pole_position);
        dest.writeTypedList(order);
    }

    public List<JuniorSessionResultElement> getPodium() {
        return order.subList(0, 3);
    }
}
