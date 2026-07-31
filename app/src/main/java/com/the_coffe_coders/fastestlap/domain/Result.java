package com.the_coffe_coders.fastestlap.domain;

import com.the_coffe_coders.fastestlap.domain.f1.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.f1.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Race;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.domain.f1.livetiming.RaceControlMessage;
import com.the_coffe_coders.fastestlap.domain.f1.livetiming.TeamRadioMessage;
import com.the_coffe_coders.fastestlap.domain.f1.result.Stint;
import com.the_coffe_coders.fastestlap.domain.f1.standing.ConstructorStandings;
import com.the_coffe_coders.fastestlap.domain.f1.standing.DriverStandings;
import com.the_coffe_coders.fastestlap.domain.f1.track.Track;
import com.the_coffe_coders.fastestlap.domain.junior.calendar.JuniorCalendar;
import com.the_coffe_coders.fastestlap.domain.junior.result.JuniorResult;
import com.the_coffe_coders.fastestlap.domain.junior.standings.JuniorConstructorStandings;
import com.the_coffe_coders.fastestlap.domain.junior.standings.JuniorDriverStandings;
import com.the_coffe_coders.fastestlap.domain.junior.standings.JuniorEntryList;
import com.the_coffe_coders.fastestlap.domain.nation.Nation;
import com.the_coffe_coders.fastestlap.domain.user.User;

import java.util.List;

import lombok.Getter;

public abstract class Result {
    private Result() {
    }

    public boolean isSuccess() {
        return !(this instanceof Error);
    }

    public String getError() {
        if (this instanceof Error) {
            return ((Error) this).getMessage();
        }
        return null;
    }

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

    public static final class NextRaceSuccess extends Result {
        private final WeeklyRace race;

        public NextRaceSuccess(WeeklyRace race) {
            this.race = race;
        }

        public WeeklyRace getData() {
            return race;
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

    public static class RaceResultsSuccess extends Result {
        private final Race race;

        public RaceResultsSuccess(Race race) {
            this.race = race;
        }

        public Race getData() {
            return race;
        }
    }

    public static class DriversSuccess extends Result {
        private final List<Driver> drivers;

        public DriversSuccess(List<Driver> drivers) {
            this.drivers = drivers;
        }

        public List<Driver> getData() {
            return drivers;
        }
    }

    public static class ConstructorsSuccess extends Result {
        private final List<Constructor> constructors;

        public ConstructorsSuccess(List<Constructor> constructors) {
            this.constructors = constructors;
        }

        public List<Constructor> getData() {
            return constructors;
        }

    }

    public static class JuniorCalendarSuccess extends Result {
        private final JuniorCalendar calendar;

        public JuniorCalendarSuccess(JuniorCalendar calendar) {
            this.calendar = calendar;
        }

        public JuniorCalendar getData() {
            return calendar;
        }
    }

    public static class JuniorEntryListSuccess extends Result {
        private final JuniorEntryList juniorEntryList;

        public JuniorEntryListSuccess(JuniorEntryList juniorEntryList) {
            this.juniorEntryList = juniorEntryList;
        }

        public JuniorEntryList getData() {
            return juniorEntryList;
        }
    }

    public static class JuniorResultSuccess extends Result {
        private final JuniorResult juniorResult;

        public JuniorResultSuccess(JuniorResult juniorResult) {
            this.juniorResult = juniorResult;
        }

        public JuniorResult getData() {
            return juniorResult;
        }
    }

    public static class JuniorDriverStandingsSuccess extends Result {
        private final JuniorDriverStandings driverStandings;

        public JuniorDriverStandingsSuccess(JuniorDriverStandings driverStandings) {
            this.driverStandings = driverStandings;
        }

        public JuniorDriverStandings getData() {
            return driverStandings;
        }
    }

    public static class JuniorConstructorStandingsSuccess extends Result {
        private final JuniorConstructorStandings constructorStandings;

        public JuniorConstructorStandingsSuccess(JuniorConstructorStandings constructorStandings) {
            this.constructorStandings = constructorStandings;
        }

        public JuniorConstructorStandings getData() {
            return constructorStandings;
        }
    }

    public static class RaceControlSuccess extends Result {
        private final List<RaceControlMessage> messages;

        public RaceControlSuccess(List<RaceControlMessage> messages) {
            this.messages = messages;
        }

        public List<RaceControlMessage> getData() {
            return messages;
        }
    }

    public static class TeamRadioSuccess extends Result {
        private final List<TeamRadioMessage> messages;

        public TeamRadioSuccess(List<TeamRadioMessage> messages) {
            this.messages = messages;
        }

        public List<TeamRadioMessage> getData() {
            return messages;
        }
    }

    public static class StintsSuccess extends Result {
        private final List<Stint> stints;

        public StintsSuccess(List<Stint> stints) {
            this.stints = stints;
        }

        public List<Stint> getData() {
            return stints;
        }
    }

    @Getter
    public static final class Error extends Result {
        private final String message;

        public Error(String message) {
            this.message = message;
        }
    }

    @Getter
    public static class Loading extends Result {
        private final String message;

        public Loading(String message) {
            this.message = message;
        }
    }

}
