package com.the_coffe_coders.fastestlap.domain.junior.result;

import android.os.Parcel;
import android.os.Parcelable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString

public class JuniorResultElement implements Parcelable {
    public static final Creator<JuniorResultElement> CREATOR = new Creator<>() {
        @Override
        public JuniorResultElement createFromParcel(Parcel in) {
            return new JuniorResultElement(in);
        }

        @Override
        public JuniorResultElement[] newArray(int size) {
            return new JuniorResultElement[size];
        }
    };

    private int round;
    private FeatureRace feature;
    private SprintRace sprint;
    private String circuit;
    private String nationFlagUrl;

    protected JuniorResultElement(Parcel in){
        round = in.readInt();
        feature = in.readParcelable(FeatureRace.class.getClassLoader());
        sprint = in.readParcelable(SprintRace.class.getClassLoader());
        circuit = in.readString();
        nationFlagUrl = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(round);
        dest.writeParcelable(feature, flags);
        dest.writeParcelable(sprint, flags);
        dest.writeString(circuit);
        dest.writeString(nationFlagUrl);
    }

}
