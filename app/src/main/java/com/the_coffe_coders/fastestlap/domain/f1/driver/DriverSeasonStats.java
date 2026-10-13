package com.the_coffe_coders.fastestlap.domain.f1.driver;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.PropertyName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
public class DriverSeasonStats implements Parcelable {
    private String wins;
    private String podiums;
    private String dnfs;
    private String poles;
    private String season_position;
    private String season_points;

    @Exclude
    private ArrayList<EventResultDriver> season_results = new ArrayList<>();

    @Exclude
    public ArrayList<EventResultDriver> getSeason_results() {
        return season_results;
    }

    @Exclude
    public void setSeason_results(ArrayList<EventResultDriver> season_results) {
        this.season_results = season_results;
    }

    @PropertyName("season_results")
    public Object getSeason_results_firebase() {
        return season_results;
    }

    @PropertyName("season_results")
    public void setSeason_results_firebase(Object obj) {
        this.season_results = parseSeasonResults(obj);
    }

    private static ArrayList<EventResultDriver> parseSeasonResults(Object obj) {
        ArrayList<EventResultDriver> resultList = new ArrayList<>();
        if (obj == null) {
            return resultList;
        }
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            for (Object item : list) {
                if (item instanceof Map) {
                    resultList.add(mapToEventResultDriver((Map<?, ?>) item));
                } else if (item instanceof EventResultDriver) {
                    resultList.add((EventResultDriver) item);
                } else {
                    resultList.add(null);
                }
            }
        } else if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            if (map.isEmpty()) {
                return resultList;
            }
            int maxRound = 0;
            Map<Integer, EventResultDriver> roundMap = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                try {
                    int round = Integer.parseInt(String.valueOf(entry.getKey()));
                    if (round > maxRound) {
                        maxRound = round;
                    }
                    Object val = entry.getValue();
                    if (val instanceof Map) {
                        roundMap.put(round, mapToEventResultDriver((Map<?, ?>) val));
                    } else if (val instanceof EventResultDriver) {
                        roundMap.put(round, (EventResultDriver) val);
                    }
                } catch (NumberFormatException ignored) {}
            }
            for (int i = 0; i <= maxRound; i++) {
                resultList.add(roundMap.get(i));
            }
        }
        return resultList;
    }

    private static EventResultDriver mapToEventResultDriver(Map<?, ?> map) {
        if (map == null) return null;
        EventResultDriver res = new EventResultDriver();
        Object q = map.get("qualifying");
        if (q != null) res.setQualifying(String.valueOf(q));
        Object sq = map.get("sprint_qualifying");
        if (sq != null) res.setSprint_qualifying(String.valueOf(sq));
        Object r = map.get("race");
        if (r != null) res.setRace(String.valueOf(r));
        Object sr = map.get("sprint_race");
        if (sr != null) res.setSprint_race(String.valueOf(sr));
        Object abbr = map.get("abbreviation");
        if (abbr != null) res.setNation_abbr(String.valueOf(abbr));
        return res;
    }

    protected DriverSeasonStats(Parcel in) {
        wins = in.readString();
        podiums = in.readString();
        dnfs = in.readString();
        poles = in.readString();
        season_position = in.readString();
        season_points = in.readString();
        season_results = in.createTypedArrayList(EventResultDriver.CREATOR);
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeString(wins);
        parcel.writeString(podiums);
        parcel.writeString(dnfs);
        parcel.writeString(poles);
        parcel.writeString(season_position);
        parcel.writeString(season_points);
        parcel.writeTypedList(season_results);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<DriverSeasonStats> CREATOR = new Creator<>() {
        @Override
        public DriverSeasonStats createFromParcel(Parcel in) {
            return new DriverSeasonStats(in);
        }

        @Override
        public DriverSeasonStats[] newArray(int size) {
            return new DriverSeasonStats[size];
        }
    };
}
