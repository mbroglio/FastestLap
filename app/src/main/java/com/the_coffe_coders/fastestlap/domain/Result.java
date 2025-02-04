package com.the_coffe_coders.fastestlap.domain;

import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandings;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandings;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;
import com.the_coffe_coders.fastestlap.domain.grand_prix.RaceResult;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Track;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.domain.nation.Nation;
import com.the_coffe_coders.fastestlap.domain.user.User;

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
        private final Driver driver;

        public DriverSuccess(Driver driver) {
            this.driver = driver;
        }

        public Driver getData() {
            return driver;
        }
    }

    public static final class NationSuccess extends Result {
        private final Nation nation;

        public NationSuccess(Nation nation) {
            this.nation = nation;
        }

        public Nation getData() {
            return nation;
        }
    }

    public static final class TrackSuccess extends Result {
        private final Track track;

        public TrackSuccess(Track track) {
            this.track = track;
        }

        public Track getData() {
            return track;
        }
    }

    public static final class ConstructorSuccess extends Result {
        // private final ConstructorAPIResponse constructorAPIResponse;
        private final Constructor constructor;

        public ConstructorSuccess(Constructor constructor) {
            this.constructor = constructor;
        }

        public Constructor getData() {
            return constructor;
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

    public static final class UserSuccess extends Result {
        private final User user;

        public UserSuccess(User user) {
            this.user = user;
        }

        public User getData() {
            return user;
        }
    }

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