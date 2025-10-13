package com.the_coffe_coders.fastestlap.domain.grand_prix;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@Entity
public class QualifyingResult implements Parcelable {

    public static final Creator<QualifyingResult> CREATOR = new Creator<>() {
        @Override
        public QualifyingResult createFromParcel(Parcel in) {
            return new QualifyingResult(in);
        }

        @Override
        public QualifyingResult[] newArray(int size) {
            return new QualifyingResult[size];
        }
    };

    @PrimaryKey(autoGenerate = true)
    private int uid;
    private String position;
    private Driver driver;
    private Constructor constructor;
    private String q1;
    private String q2;
    private String q3;

    protected QualifyingResult(Parcel in) {
        uid = in.readInt();
        position = in.readString();
        driver = in.readParcelable(Driver.class.getClassLoader());
        constructor = in.readParcelable(Constructor.class.getClassLoader());
        q1 = in.readString();
        q2 = in.readString();
        q3 = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(uid);
        dest.writeString(position);
        dest.writeParcelable(driver, flags);
        dest.writeParcelable(constructor, flags);
        dest.writeString(q1);
        dest.writeString(q2);
        dest.writeString(q3);
    }
}
