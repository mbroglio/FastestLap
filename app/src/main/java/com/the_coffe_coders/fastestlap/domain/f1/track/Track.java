package com.the_coffe_coders.fastestlap.domain.f1.track;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

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
@AllArgsConstructor(onConstructor_ = @Ignore)
@Entity(tableName = "Track")
public class Track {
    private long uid;
    @PrimaryKey
    @NonNull
    private String trackId = "";
    private String url;
    private String trackName;
    private Location location;

    private String track_full_layout_url;
    @SerializedName("circuit_history")
    private List<TrackHistory> track_history;
    private String track_minimal_layout_url;
    private String track_pic_url;
    private String country;
    private String gp_long_name;
    private String lap_record;
    private String laps;
    private String length;
    private String race_distance;
    private String first_entry;
}
