package com.the_coffe_coders.fastestlap.domain.driver;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import org.threeten.bp.LocalDate;
import org.threeten.bp.Period;
import org.threeten.bp.format.DateTimeFormatter;

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
@Entity(tableName = "Driver")
public class Driver implements Parcelable {
    public static final Creator<Driver> CREATOR = new Creator<>() {
        @Override
        public Driver createFromParcel(Parcel in) {
            return new Driver(in);
        }

        @Override
        public Driver[] newArray(int size) {
            return new Driver[size];
        }
    };
    @PrimaryKey(autoGenerate = true)
    private long uid;
    @Getter
    // F = Firebase A = API

    private String driverId; // A
    private String permanentNumber; // A
    private String code; // A
    private String url;
    private String givenName; // F/A
    private String familyName; // F/A
    private String dateOfBirth; // F/A
    private String nationality;
    private String best_result; // F
    private String birth_place; // F
    private String championships; // F
    private List<DriverHistory> driver_history; // F
    private String driver_pic_url; // F
    private String height; // F
    private String podiums; // F
    private String racing_number_pic_url; // F
    private String team_id; // F
    private String weight;// F
    private String first_entry;//F

    protected Driver(Parcel in) {
        uid = in.readLong();
        driverId = in.readString();
        permanentNumber = in.readString();
        code = in.readString();
        url = in.readString();
        givenName = in.readString();
        familyName = in.readString();
        dateOfBirth = in.readString();
        nationality = in.readString();
        best_result = in.readString();
        birth_place = in.readString();
        championships = in.readString();
        driver_pic_url = in.readString();
        height = in.readString();
        podiums = in.readString();
        racing_number_pic_url = in.readString();
        team_id = in.readString();
        weight = in.readString();
        first_entry = in.readString();
    }

    public int getDriverAge() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate birthDate = LocalDate.parse(dateOfBirth, formatter);
        LocalDate currentDate = LocalDate.now();
        return Period.between(birthDate, currentDate).getYears();
    }

    public String getDriverAgeAsString() {
        return String.valueOf(getDriverAge());
    }

    public String getFullName() {
        return givenName + " " + familyName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLong(uid);
        dest.writeString(driverId);
        dest.writeString(permanentNumber);
        dest.writeString(code);
        dest.writeString(url);
        dest.writeString(givenName);
        dest.writeString(familyName);
        dest.writeString(dateOfBirth);
        dest.writeString(nationality);
        dest.writeString(best_result);
        dest.writeString(birth_place);
        dest.writeString(championships);
        dest.writeString(driver_pic_url);
        dest.writeString(height);
        dest.writeString(podiums);
        dest.writeString(racing_number_pic_url);
        dest.writeString(team_id);
        dest.writeString(weight);
        dest.writeString(first_entry);
    }
}
