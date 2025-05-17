package com.the_coffe_coders.fastestlap.domain.constructor;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.List;

import lombok.AllArgsConstructor;
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
@AllArgsConstructor
@Entity(tableName = "Constructor")
public class Constructor implements Parcelable {
    public static final Creator<Constructor> CREATOR = new Creator<Constructor>() {
        @Override
        public Constructor createFromParcel(Parcel in) {
            return new Constructor(in);
        }

        @Override
        public Constructor[] newArray(int size) {
            return new Constructor[size];
        }
    };
    @PrimaryKey(autoGenerate = true)
    private long uid;
    private String constructorId;
    private String url;
    private String name;
    private String nationality;
    // Bio Data
    private String car_pic_url;
    private String chassis;
    private List<String> drivers;
    private String first_entry;
    private String full_name;
    private String hq;
    private String podiums;
    private String power_unit;
    @Ignore
    private List<ConstructorHistory> team_history;
    private String team_logo_url;
    private String team_logo_minimal_url;
    private String team_principal;
    private String wins;
    private String world_championships;

    protected Constructor(Parcel in) {
        uid = in.readLong();
        constructorId = in.readString();
        url = in.readString();
        name = in.readString();
        nationality = in.readString();
        car_pic_url = in.readString();
        chassis = in.readString();
        drivers = in.createStringArrayList();
        first_entry = in.readString();
        full_name = in.readString();
        hq = in.readString();
        podiums = in.readString();
        power_unit = in.readString();
        team_logo_url = in.readString();
        team_logo_minimal_url = in.readString();
        team_principal = in.readString();
        wins = in.readString();
        world_championships = in.readString();
    }

    public String getDriverOneId() {
        return drivers.get(0);
    }

    public String getDriverTwoId() {
        return drivers.get(1);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLong(uid);
        dest.writeString(constructorId);
        dest.writeString(url);
        dest.writeString(name);
        dest.writeString(nationality);
        dest.writeString(car_pic_url);
        dest.writeString(chassis);
        dest.writeStringList(drivers);
        dest.writeString(first_entry);
        dest.writeString(full_name);
        dest.writeString(hq);
        dest.writeString(podiums);
        dest.writeString(power_unit);
        dest.writeString(team_logo_url);
        dest.writeString(team_logo_minimal_url);
        dest.writeString(team_principal);
        dest.writeString(wins);
        dest.writeString(world_championships);
    }
}
