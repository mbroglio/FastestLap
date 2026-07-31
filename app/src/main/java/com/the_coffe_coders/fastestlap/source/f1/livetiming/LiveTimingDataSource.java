package com.the_coffe_coders.fastestlap.source.f1.livetiming;

import com.the_coffe_coders.fastestlap.repository.f1.livetiming.RaceControlCallback;
import com.the_coffe_coders.fastestlap.repository.f1.livetiming.TeamRadioCallback;

public interface LiveTimingDataSource {

    void getTeamRadioMessages(TeamRadioCallback callback);
    void getRaceControlMessages(RaceControlCallback callback);
}
