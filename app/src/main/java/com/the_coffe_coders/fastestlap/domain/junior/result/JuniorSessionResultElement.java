package com.the_coffe_coders.fastestlap.domain.junior.result;

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

public class JuniorSessionResultElement implements Parcelable {
    public static final Creator<JuniorSessionResultElement> CREATOR = new Creator<>() {
        @Override
        public JuniorSessionResultElement createFromParcel(Parcel in) {
            return new JuniorSessionResultElement(in);
        }

        @Override
        public JuniorSessionResultElement[] newArray(int size) {
            return new JuniorSessionResultElement[size];
        }
    };

    private String driver;
    private String position;

    protected JuniorSessionResultElement(Parcel in) {
        driver = in.readString();
        position = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(driver);
        dest.writeString(position);
    }
}
