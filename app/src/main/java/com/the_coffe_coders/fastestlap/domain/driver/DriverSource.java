package com.the_coffe_coders.fastestlap.domain.driver;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public class DriverSource implements Parcelable {
    private String id;
    private String name;

    public DriverSource() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DriverSource that = (DriverSource) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
    }

    public void readFromParcel(Parcel source) {
        this.name = source.readString();
    }

    protected DriverSource(Parcel in) {
        this.name = in.readString();
    }

    public static final Parcelable.Creator<DriverSource> CREATOR = new Parcelable.Creator<DriverSource>() {
        @Override
        public DriverSource createFromParcel(Parcel source) {
            return new DriverSource(source);
        }

        @Override
        public DriverSource[] newArray(int size) {
            return new DriverSource[size];
        }
    };
}