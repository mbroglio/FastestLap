package com.the_coffe_coders.fastestlap.domain.race;

import android.util.Log;

import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

import java.util.ArrayList;
import java.util.List;

public class Race {
    private String season;
    private String round;
    private String url;
    private String raceName;
    private Circuit Circuit;
    private String date;
    private String time;
    private FirstPractice FirstPractice;
    private SecondPractice SecondPractice;
    private ThirdPractice ThirdPractice;
    private Qualifying Qualifying;
    private SprintQualifying SprintQualifying;
    private Sprint Sprint;

    public Race(String season, String round, String url, String raceName, Circuit circuit, String date, String time, FirstPractice firstPractice, SecondPractice secondPractice, ThirdPractice thirdPractice, Qualifying qualifying, SprintQualifying sprintQualifying, Sprint sprint) {
        this.season = season;
        this.round = round;
        this.url = url;
        this.raceName = raceName;
        Circuit = circuit;
        this.date = date;
        this.time = time;
        FirstPractice = firstPractice;
        SecondPractice = secondPractice;
        ThirdPractice = thirdPractice;
        Qualifying = qualifying;
        SprintQualifying = sprintQualifying;
        Sprint = sprint;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public String getRound() {
        return round;
    }

    public void setRound(String round) {
        this.round = round;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getRaceName() {
        return raceName;
    }

    public void setRaceName(String raceName) {
        this.raceName = raceName;
    }

    public Circuit getCircuit() {
        return Circuit;
    }

    public void setCircuit(Circuit circuit) {
        Circuit = circuit;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public FirstPractice getFirstPractice() {
        return FirstPractice;
    }

    public void setFirstPractice(FirstPractice firstPractice) {
        FirstPractice = firstPractice;
    }

    public SecondPractice getSecondPractice() {
        return SecondPractice;
    }

    public void setSecondPractice(SecondPractice secondPractice) {
        SecondPractice = secondPractice;
    }

    public ThirdPractice getThirdPractice() {
        return ThirdPractice;
    }

    public void setThirdPractice(ThirdPractice thirdPractice) {
        ThirdPractice = thirdPractice;
    }

    public Qualifying getQualifying() {
        return Qualifying;
    }

    public void setQualifying(Qualifying qualifying) {
        Qualifying = qualifying;
    }

    public SprintQualifying getSprintQualifying() {
        return SprintQualifying;
    }

    public void setSprintQualifying(SprintQualifying sprintQualifying) {
        SprintQualifying = sprintQualifying;
    }

    public Sprint getSprint() {
        return Sprint;
    }

    public void setSprint(Sprint sprint) {
        Sprint = sprint;
    }

    public String getDateInterval() {
        String fullDate;
        String startingDate = this.getFirstPractice().getDate();
        String endingDate = this.getDate();

        LocalDate startDate = LocalDate.parse(startingDate);
        LocalDate endDate = LocalDate.parse(endingDate);

        if (startDate.getMonth() != endDate.getMonth()) {
            fullDate = startDate.getDayOfMonth() + " " + startDate.getMonth() + " - " + endDate.getDayOfMonth() + " " + endDate.getMonth();
        } else {
            fullDate = startDate.getDayOfMonth() + " - " + endDate.getDayOfMonth() + " " + startDate.getMonth();
        }

        return fullDate;
    }

    public List<Session> getSessions() {
        List<Session> sessions = new ArrayList<>();
        Race race = this;

        addSession(sessions, race.getFirstPractice());
        addSession(sessions, race.getSecondPractice());
        addSession(sessions, race.getThirdPractice());
        addSession(sessions, race.getSprintQualifying());
        addSession(sessions, race.getSprint());
        addSession(sessions, race.getQualifying());

        // Add Race session
        ZonedDateTime startDateTime = ZonedDateTime.parse(
                race.getDate() + "T" + race.getTime() + "[UTC]"
        );
        sessions.add(new Session("Race", false, false, startDateTime, null));

        return sessions;
    }

    private void addSession(List<Session> sessions, Object tmpSession) {
        if (tmpSession != null) {
            String sessionId = tmpSession.getClass().getSimpleName();

            String date = null;
            String time = null;

            if (tmpSession instanceof FirstPractice) {
                date = ((FirstPractice) tmpSession).getDate();
                time = ((FirstPractice) tmpSession).getTime();
            } else if (tmpSession instanceof SecondPractice) {
                date = ((SecondPractice) tmpSession).getDate();
                time = ((SecondPractice) tmpSession).getTime();
            } else if (tmpSession instanceof ThirdPractice) {
                date = ((ThirdPractice) tmpSession).getDate();
                time = ((ThirdPractice) tmpSession).getTime();
            } else if (tmpSession instanceof SprintQualifying) {
                date = ((SprintQualifying) tmpSession).getDate();
                time = ((SprintQualifying) tmpSession).getTime();
            } else if (tmpSession instanceof Sprint) {
                date = ((Sprint) tmpSession).getDate();
                time = ((Sprint) tmpSession).getTime();
            } else if (tmpSession instanceof Qualifying) {
                date = ((Qualifying) tmpSession).getDate();
                time = ((Qualifying) tmpSession).getTime();
            }

            if (date != null && time != null) {
                ZonedDateTime startDateTime = ZonedDateTime.parse(date + "T" + time + "[UTC]");
                sessions.add(new Session(sessionId, false, false, startDateTime, null));
            }
        }
    }

    public Session findNextEvent(List<Session> sessions) {
        ZonedDateTime currentDateTime = ZonedDateTime.now(ZoneId.systemDefault());
        Session nextSession = null;
        int i = 0;

        for (Session session : sessions) {
            if (currentDateTime.isAfter(session.getStartDateTime()) && currentDateTime.isBefore(session.getEndDateTime())) {
                session.setUnderway(true);
            } else if (currentDateTime.isAfter(session.getEndDateTime())) {
                session.setUnderway(false);
                session.setFinished(true);
            }
        }

        for (Session session : sessions) {
            if (!session.isFinished()) {
                nextSession = session;
                break;
            }
        }

        return nextSession;
    }

    public boolean isUnderway() {
        for (Session session : this.getSessions()) {
            if (session.isUnderway()) {
                return true;
            }
        }

        return false;
    }
}
