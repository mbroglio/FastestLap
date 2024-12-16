package com.the_coffe_coders.fastestlap.domain.grand_prix;

import org.threeten.bp.LocalDate;

import java.util.Collections;
import java.util.List;

public class WeeklyRaceClassic extends WeeklyRace {
    Practice secondPractice;
    Practice thirdPractice;

    WeeklyRaceClassic(String season, String round, String url, String raceName, Circuit circuit, String date, String time, Qualifying qualifying, Race finalRace, Practice firstPractice, Practice secondPractice, Practice thirdPractice) {
        super(season, round, url, raceName, circuit, date, time, qualifying, finalRace, firstPractice);
        this.secondPractice = secondPractice;
        this.thirdPractice = thirdPractice;
    }

    public WeeklyRaceClassic() {
        super();
    }
    public String getDateInterval() {
        String fullDate;
        String startingDate = this.firstPractice.getStartDateTime().toLocalDate().toString();
        String endingDate = this.firstPractice.getEndDateTime().toLocalDate().toString();

        LocalDate startDate = LocalDate.parse(startingDate);
        LocalDate endDate = LocalDate.parse(endingDate);

        if (startDate.getMonth() != endDate.getMonth()) {
            fullDate = startDate.getDayOfMonth() + " " + startDate.getMonth() + " - " + endDate.getDayOfMonth() + " " + endDate.getMonth();
        } else {
            fullDate = startDate.getDayOfMonth() + " - " + endDate.getDayOfMonth() + " " + startDate.getMonth();
        }

        return fullDate;
    }

    @Override
    public List<Session> getSessions() {
        return Collections.emptyList();
    }
}
