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
import java.util.Objects;

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
public class Driver {
    @PrimaryKey(autoGenerate = true)
    private long uid;
    private String driverId; // A
    private String permanentNumber; // A
    private String code; // A
    private String url; // TODO: Remove DTO
    private String givenName; // F/A
    private String familyName; // F/A
    private String dateOfBirth; // F/A
    private String nationality; // TODO: Remove DTO

    // Bio Data
    private String best_result; // F
    private String birth_place; // F
    private String championships; // F
    @Ignore // Check if it generates errors
    private List<DriverHistory> driver_history; // F
    private String driver_pic_url; // F
    private String height; // F
    private String podiums; // F
    private String racing_number_pic_url; // F
    private String team_id; // F
    private String weight; // F

    public int getDriverAge() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate birthDate = LocalDate.parse(dateOfBirth, formatter);
        LocalDate currentDate = LocalDate.now();
        return Period.between(birthDate, currentDate).getYears();
    }

    public String getDriverId(){
        return driverId;
    }

    public String getDriverAgeAsString() {
        return String.valueOf(getDriverAge());
    }
}
