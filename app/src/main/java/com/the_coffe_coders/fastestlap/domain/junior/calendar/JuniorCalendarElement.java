package com.the_coffe_coders.fastestlap.domain.junior.calendar;

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
public class JuniorCalendarElement implements Parcelable {
    public static final Creator<JuniorCalendarElement> CREATOR = new Creator<>() {
        @Override
        public JuniorCalendarElement createFromParcel(Parcel in) {
            return new JuniorCalendarElement(in);
        }

        @Override
        public JuniorCalendarElement[] newArray(int size) {
            return new JuniorCalendarElement[size];

        }
    };

    private String circuit;
    private String feature_date;
    private String sprint_date;
    private String round;
    private String nation_flag_url;


    protected JuniorCalendarElement(Parcel in) {
        circuit = in.readString();
        feature_date = in.readString();
        sprint_date = in.readString();
        round = in.readString();
        nation_flag_url = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(circuit);
        dest.writeString(feature_date);
        dest.writeString(sprint_date);
        dest.writeString(round);
        dest.writeString(nation_flag_url);
    }
}
