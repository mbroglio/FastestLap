package com.the_coffe_coders.fastestlap.domain.junior.result;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.List;
import java.util.Objects;

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

@Entity(tableName = "JuniorResult")
public class JuniorResult implements Parcelable {
    public static final Creator<JuniorResult> CREATOR = new Creator<>() {
        @Override
        public JuniorResult createFromParcel(Parcel in) {
            return new JuniorResult(in);
        }

        @Override
        public JuniorResult[] newArray(int size) {
            return new JuniorResult[size];
        }
    };

    @PrimaryKey
    @NonNull
    private String series;
    private List<JuniorResultElement> results;

    protected JuniorResult(Parcel in) {
        series = Objects.requireNonNull(in.readString());
        results = in.createTypedArrayList(JuniorResultElement.CREATOR);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(series);
        dest.writeTypedList(results);
    }
}
