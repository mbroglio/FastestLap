package com.the_coffe_coders.fastestlap.domain.grand_prix;

import org.threeten.bp.LocalDate;

public class WeeklyRaceClassic {
    Practice firstPractice;
    Practice secondPractice;
    Practice thirdPractice;

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
}
