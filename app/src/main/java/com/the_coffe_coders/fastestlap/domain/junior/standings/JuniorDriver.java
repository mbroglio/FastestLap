package com.the_coffe_coders.fastestlap.domain.junior.standings;

import android.os.Parcel;
import android.os.Parcelable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString

public class JuniorDriver implements Parcelable {
    public static final Creator<JuniorDriver> CREATOR = new Creator<>() {
        @Override
        public JuniorDriver createFromParcel(Parcel in) {
            return new JuniorDriver(in);
        }

        @Override
        public JuniorDriver[] newArray(int size) {
            return new JuniorDriver[size];
        }
    };

    private String driver;
    private String number;
    private String rounds;

    protected JuniorDriver(Parcel in) {
        driver = in.readString();
        number = in.readString();
        rounds = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(driver);
        dest.writeString(number);
        dest.writeString(rounds);
    }
}
