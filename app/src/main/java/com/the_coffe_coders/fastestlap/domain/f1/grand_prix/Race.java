package com.the_coffe_coders.fastestlap.domain.f1.grand_prix;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.the_coffe_coders.fastestlap.domain.f1.result.QualifyingResult;
import com.the_coffe_coders.fastestlap.domain.f1.result.RaceResult;
import com.the_coffe_coders.fastestlap.domain.f1.result.Stint;
import com.the_coffe_coders.fastestlap.domain.f1.track.Track;

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

    public static final Creator<Race> CREATOR = new Creator<>() {
        @Override
        public Race createFromParcel(Parcel in) {
            return new Race(in);
        }

        @Override
        public Race[] newArray(int size) {
            return new Race[size];
        }
    };

    public List<RaceResult> raceResults;
    public List<QualifyingResult> qualifyingResults;
    public List<RaceResult> sprintResults;
    public List<Stint> raceStints;
    public List<Stint> sprintStints;
    public LocalDateTime dateTime;
    @PrimaryKey(autoGenerate = true)
    private int uid;
    private String season;
    private String round;
    private String url;
    private String raceName;
    private Track track;

    public Race(String season, String round, String url, String raceName, Track track, List<RaceResult> raceResults,
                List<QualifyingResult> qualifyingResults, List<RaceResult> sprintResults, String sessionId,
                Boolean isFinished, Boolean isUnderway, String date, String time) {
        super(date, time);
        this.season = season;
        this.round = round;
        this.url = url;
        this.raceName = raceName;
        this.track = track;
        this.raceResults = raceResults;
        this.qualifyingResults = qualifyingResults;
        this.sprintResults = sprintResults;
        this.raceStints = new ArrayList<>();
        this.sprintStints = new ArrayList<>();
        setEndDateTime();
    }

    public Race() {
        raceResults = new ArrayList<>();
        qualifyingResults = new ArrayList<>();
        sprintResults = new ArrayList<>();
        raceStints = new ArrayList<>();
        sprintStints = new ArrayList<>();
    }

    protected Race(Parcel in) {
        raceResults = in.createTypedArrayList(RaceResult.CREATOR);
        qualifyingResults = in.createTypedArrayList(QualifyingResult.CREATOR);
        sprintResults = in.createTypedArrayList(RaceResult.CREATOR);
        raceStints = in.createTypedArrayList(Stint.CREATOR);
        sprintStints = in.createTypedArrayList(Stint.CREATOR);
        uid = in.readInt();
        season = in.readString();
        round = in.readString();
        url = in.readString();
        raceName = in.readString();
    }

    public int getRoundAsInt() {
        return Integer.parseInt(round);
    }

    public List<RaceResult> getResults() {
        return raceResults;
    }

    public void setResults(List<RaceResult> raceResults) {
        this.raceResults = raceResults;
    }

    public List<Stint> getRaceStints() {
        return raceStints;
    }

    public void setRaceStints(List<Stint> raceStints) {
        this.raceStints = raceStints;
    }

    public List<Stint> getSprintStints() {
        return sprintStints;
    }

    public void setSprintStints(List<Stint> sprintStints) {
        this.sprintStints = sprintStints;
    }

    public List<Stint> getStints() {
        if (raceResults != null && !raceResults.isEmpty()) {
            return raceStints;
        } else if (sprintResults != null && !sprintResults.isEmpty()) {
            return sprintStints;
        }
        return (raceStints != null && !raceStints.isEmpty()) ? raceStints : sprintStints;
    }

    public void addResult(RaceResult result) {
        this.raceResults.add(result);
    }

    public void addQualifyingResult(QualifyingResult result) {
        this.qualifyingResults.add(result);
    }

    public void addSprintResult(RaceResult result) {
        this.sprintResults.add(result);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeTypedList(raceResults);
        dest.writeTypedList(qualifyingResults);
        dest.writeTypedList(sprintResults);
        dest.writeTypedList(raceStints);
        dest.writeTypedList(sprintStints);
        dest.writeInt(uid);
        dest.writeString(season);
        dest.writeString(round);
        dest.writeString(url);
        dest.writeString(raceName);
    }
}
