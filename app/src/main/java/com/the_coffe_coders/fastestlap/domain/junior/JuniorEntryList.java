package com.the_coffe_coders.fastestlap.domain.junior;

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

@Entity(tableName = "JuniorEntryList")
public class JuniorEntryList implements Parcelable {
    public static final Creator<JuniorEntryList> CREATOR = new Creator<>() {
        @Override
        public JuniorEntryList createFromParcel(Parcel in) {
            return new JuniorEntryList(in);
        }

        @Override
        public JuniorEntryList[] newArray(int size) {
            return new JuniorEntryList[size];
        }
    };


    @PrimaryKey
    @NonNull
    public String series;
    private List<JuniorTeam> teams;


    protected JuniorEntryList(Parcel in) {
        series = Objects.requireNonNull(in.readString());
        teams = in.createTypedArrayList(JuniorTeam.CREATOR);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(series);
        dest.writeTypedList(teams);
    }
}
