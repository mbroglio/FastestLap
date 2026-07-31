package com.the_coffe_coders.fastestlap.domain.f1.result;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Modello di dominio per gli stint dei pneumatici restituiti dall'endpoint /v1/stints di OpenF1.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Stint implements Parcelable {

    public static final Creator<Stint> CREATOR = new Creator<Stint>() {
        @Override
        public Stint createFromParcel(Parcel in) {
            return new Stint(in);
        }

        @Override
        public Stint[] newArray(int size) {
            return new Stint[size];
        }
    };


    private int meetingKey;
    private int sessionKey;
    private int stintNumber;
    private int driverNumber;
    private Integer lapStart;
    private Integer lapEnd;
    private String compound;
    private Integer tyreAgeAtStart;

    protected Stint(Parcel in) {
        meetingKey = in.readInt();
        sessionKey = in.readInt();
        stintNumber = in.readInt();
        driverNumber = in.readInt();
        lapStart = (Integer) in.readValue(Integer.class.getClassLoader());
        lapEnd = (Integer) in.readValue(Integer.class.getClassLoader());
        compound = in.readString();
        tyreAgeAtStart = (Integer) in.readValue(Integer.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeInt(meetingKey);
        parcel.writeInt(sessionKey);
        parcel.writeInt(stintNumber);
        parcel.writeInt(driverNumber);
        parcel.writeValue(lapStart);
        parcel.writeValue(lapEnd);
        parcel.writeString(compound);
        parcel.writeValue(tyreAgeAtStart);
    }
}
