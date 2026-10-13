package com.the_coffe_coders.fastestlap.domain.f1.constructor;

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
public class ConstructorSeasonStats implements Parcelable {
    private String wins;
    private String podiums;
    private String dnfs;
    private String poles;
    private String season_position;
    private String season_points;

    @Exclude
    private ArrayList<EventResultConstructor> season_results = new ArrayList<>();

    @Exclude
    public ArrayList<EventResultConstructor> getSeason_results() {
        return season_results;
    }

    @Exclude
    public void setSeason_results(ArrayList<EventResultConstructor> season_results) {
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

    private static ArrayList<EventResultConstructor> parseSeasonResults(Object obj) {
        ArrayList<EventResultConstructor> resultList = new ArrayList<>();
        if (obj == null) {
            return resultList;
        }
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            for (Object item : list) {
                if (item instanceof Map) {
                    resultList.add(mapToEventResultConstructor((Map<?, ?>) item));
                } else if (item instanceof EventResultConstructor) {
                    resultList.add((EventResultConstructor) item);
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
            Map<Integer, EventResultConstructor> roundMap = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                try {
                    int round = Integer.parseInt(String.valueOf(entry.getKey()));
                    if (round > maxRound) {
                        maxRound = round;
                    }
                    Object val = entry.getValue();
                    if (val instanceof Map) {
                        roundMap.put(round, mapToEventResultConstructor((Map<?, ?>) val));
                    } else if (val instanceof EventResultConstructor) {
                        roundMap.put(round, (EventResultConstructor) val);
                    }
                } catch (NumberFormatException ignored) {}
            }
            for (int i = 0; i <= maxRound; i++) {
                resultList.add(roundMap.get(i));
            }
        }
        return resultList;
    }

    private static EventResultConstructor mapToEventResultConstructor(Map<?, ?> map) {
        if (map == null) return null;
        EventResultConstructor res = new EventResultConstructor();
        Object q1 = map.get("qualifying_driver_1");
        if (q1 != null) res.setQualifying_driver_1(String.valueOf(q1));
        Object q2 = map.get("qualifying_driver_2");
        if (q2 != null) res.setQualifying_driver_2(String.valueOf(q2));
        Object sq1 = map.get("sprint_qualifying_driver_1");
        if (sq1 != null) res.setSprint_qualifying_driver_1(String.valueOf(sq1));
        Object sq2 = map.get("sprint_qualifying_driver_2");
        if (sq2 != null) res.setSprint_qualifying_driver_2(String.valueOf(sq2));
        Object r1 = map.get("race_driver_1");
        if (r1 != null) res.setRace_driver_1(String.valueOf(r1));
        Object r2 = map.get("race_driver_2");
        if (r2 != null) res.setRace_driver_2(String.valueOf(r2));
        Object sr1 = map.get("sprint_race_driver_1");
        if (sr1 != null) res.setSprint_race_driver_1(String.valueOf(sr1));
        Object sr2 = map.get("sprint_race_driver_2");
        if (sr2 != null) res.setSprint_race_driver_2(String.valueOf(sr2));
        Object abbr = map.get("abbreviation");
        if (abbr != null) res.setAbbreviation(String.valueOf(abbr));
        return res;
    }

    protected ConstructorSeasonStats(Parcel in) {
        wins = in.readString();
        podiums = in.readString();
        dnfs = in.readString();
        poles = in.readString();
        season_position = in.readString();
        season_points = in.readString();
        season_results = in.createTypedArrayList(EventResultConstructor.CREATOR);
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

    public static final Creator<ConstructorSeasonStats> CREATOR = new Creator<>() {
        @Override
        public ConstructorSeasonStats createFromParcel(Parcel in) {
            return new ConstructorSeasonStats(in);
        }

        @Override
        public ConstructorSeasonStats[] newArray(int size) {
            return new ConstructorSeasonStats[size];
        }
    };
}
