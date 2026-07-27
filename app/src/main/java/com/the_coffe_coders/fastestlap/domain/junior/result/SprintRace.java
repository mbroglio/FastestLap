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

public class SprintRace implements Parcelable {
    public static final Creator<SprintRace> CREATOR = new Creator<>() {
        @Override
        public SprintRace createFromParcel(Parcel in) {
            return new SprintRace(in);
        }

        @Override
        public SprintRace[] newArray(int size) {
            return new SprintRace[size];
        }
    };

    private String status;
    private String fastest_lap;
    private String pole_position;
    private List<JuniorSessionResultElement> order;

    protected SprintRace(Parcel in) {
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

    public boolean isCompleted() {
        return status.equalsIgnoreCase("completed");
    }

    public boolean isCancelled() {
        return status.equalsIgnoreCase("cancelled");
    }

    public boolean isYetToStart() {
        return status.equalsIgnoreCase("not_started");
    }
}
