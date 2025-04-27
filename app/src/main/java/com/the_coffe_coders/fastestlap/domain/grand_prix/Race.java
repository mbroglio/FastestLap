package com.the_coffe_coders.fastestlap.domain.grand_prix;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.threeten.bp.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString
@Entity
public class Race extends Session implements Parcelable {

    public List<RaceResult> raceResults;
    public LocalDateTime dateTime;//TODO REMOVE ?
    @PrimaryKey(autoGenerate = true)
    private int uid;
    private String season;
    private String round;
    private String url;
    private String raceName;
    private Track track;

    public Race(String season, String round, String url, String raceName, Track track, List<RaceResult> raceResults, String sessionId, Boolean isFinished, Boolean isUnderway, String date, String time) {
        super(date, time);
        this.season = season;
        this.round = round;
        this.url = url;
        this.raceName = raceName;
        this.track = track;
        this.raceResults = raceResults;
        setEndDateTime();
    }

    public Race() {
        raceResults = new ArrayList<>();
    }

    protected Race(Parcel in) {
        raceResults = in.createTypedArrayList(RaceResult.CREATOR);
        uid = in.readInt();
        season = in.readString();
        round = in.readString();
        url = in.readString();
        raceName = in.readString();
    }

    public static final Creator<Race> CREATOR = new Creator<Race>() {
        @Override
        public Race createFromParcel(Parcel in) {
            return new Race(in);
        }

        @Override
        public Race[] newArray(int size) {
            return new Race[size];
        }
    };

    public int getRoundAsInt() {
        return Integer.parseInt(round);
    }

    public List<RaceResult> getResults() {
        return raceResults;
    }

    public void setResults(List<RaceResult> raceResults) {
        this.raceResults = raceResults;
    }

    public void addResult(RaceResult result) {
        this.raceResults.add(result);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeTypedList(raceResults);
        dest.writeInt(uid);
        dest.writeString(season);
        dest.writeString(round);
        dest.writeString(url);
        dest.writeString(raceName);
    }
}
