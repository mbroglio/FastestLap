package com.the_coffe_coders.fastestlap.domain;

import com.the_coffe_coders.fastestlap.domain.constructor.ConstructorAPIResponse;
import com.the_coffe_coders.fastestlap.domain.driver.DriverAPIResponse;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandings;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandings;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;
import com.the_coffe_coders.fastestlap.domain.grand_prix.RaceResult;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;

import java.util.List;

public abstract class Result {
    private Result() {
    }

    public boolean isSuccess() {
        return !(this instanceof Error);
    }

    /**
     * Class that represents a successful action during the interaction
     * with a Web Service or a local database.
     */
    public static final class DriverSuccess extends Result {
        private final DriverAPIResponse driverAPIResponse;

        public DriverSuccess(DriverAPIResponse driverStandingsAPIResponse) {
            this.driverAPIResponse = driverStandingsAPIResponse;
        }

        public DriverAPIResponse getData() {
            return driverAPIResponse;
        }
    }

    public static final class ConstructorSuccess extends Result {
        private final ConstructorAPIResponse constructorAPIResponse;

        public ConstructorSuccess(ConstructorAPIResponse constructorStandingsAPIResponse) {
            this.constructorAPIResponse = constructorStandingsAPIResponse;
        }

        public ConstructorAPIResponse getData() {
            return constructorAPIResponse;
        }
    }

    public static final class DriverStandingsSuccess extends Result {
        private final DriverStandings driverStandings;

        public DriverStandingsSuccess(DriverStandings driverStandings) {
            this.driverStandings = driverStandings;
        }

        public DriverStandings getData() {
            return driverStandings;
        }
    }

    public static final class ConstructorStandingsSuccess extends Result {
        private final ConstructorStandings constructorStandings;

        public ConstructorStandingsSuccess(ConstructorStandings constructorStandings) {
            this.constructorStandings = constructorStandings;
        }

        public ConstructorStandings getData() {
            return constructorStandings;
        }
    }

    public static final class RaceResultSuccess extends Result {
        private final List<RaceResult> raceResultList;

        public RaceResultSuccess(List<RaceResult> raceResultList) {
            this.raceResultList = raceResultList;
        }

        public List<RaceResult> getData() {
            return raceResultList;
        }
    }

    public static final class RaceSuccess extends Result {
        private final List<Race> raceList;

        public RaceSuccess(List<Race> raceList) {
            this.raceList = raceList;
        }

        public List<Race> getData() {
            return raceList;
        }
    }

    public static final class WeeklyRaceSuccess extends Result {
        private final List<WeeklyRace> weeklyRaceList;

        public WeeklyRaceSuccess(List<WeeklyRace> weeklyRaceList) {
            this.weeklyRaceList = weeklyRaceList;
        }

        public List<WeeklyRace> getData() {
            return weeklyRaceList;
        }
    }

    /*
        public static final class UserSuccess extends Result {
            private final User user;
            public UserSuccess(User user) {
                this.user = user;
            }
            public User getData() {
                return user;
            }
        }

        /**
         * Class that represents an error occurred during the interaction
         * with a Web Service or a local database.
         */
    public static final class Error extends Result {
        private final String message;

        public Error(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }


}