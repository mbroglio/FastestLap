package com.the_coffe_coders.fastestlap.domain.junior.calendar;

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

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter

@Entity(tableName = "JuniorCalendar")
public class JuniorCalendar implements Parcelable {
    public static final Creator<JuniorCalendar> CREATOR = new Creator<>() {
        @Override
        public JuniorCalendar createFromParcel(Parcel in) {
            return new JuniorCalendar(in);
        }

        @Override
        public JuniorCalendar[] newArray(int size) {
            return new JuniorCalendar[size];
        }
    };

    @PrimaryKey
    @NonNull
    public String series;
    private List<JuniorCalendarElement> events;


    protected JuniorCalendar(Parcel in) {
        series = Objects.requireNonNull(in.readString());
        events = in.createTypedArrayList(JuniorCalendarElement.CREATOR);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(series);
        dest.writeTypedList(events);
    }
}
