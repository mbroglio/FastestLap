package com.the_coffe_coders.fastestlap.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Practice;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Qualifying;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Race;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Session;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.SessionStatus;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Sprint;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.SprintQualifying;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.WeeklyRaceSprint;
import com.the_coffe_coders.fastestlap.util.Constants;

import org.junit.Test;
import org.threeten.bp.LocalDateTime;

import java.util.List;

public class SprintSessionTest {

    @Test
    public void testSprintDurationIs75Minutes() {
        assertEquals(Integer.valueOf(75), Constants.SESSION_DURATION.get("Sprint"));
        assertEquals(Integer.valueOf(120), Constants.SESSION_DURATION.get("Race"));
    }

    @Test
    public void testSprintExtendedStatusWhenSessionExceedsNominalTimeDueToDelays() {
        // Sprint iniziata 65 minuti fa (supera i 60 min nominali ma entro la finestra massima di 75 min)
        LocalDateTime now = LocalDateTime.now();
        Sprint sprint = new Sprint("2026-10-10", "10:00:00Z");
        sprint.setStartDateTime(now.minusMinutes(65));
        sprint.setEndDateTime();
        sprint.setSessionStatus();

        // Deve risultare IN_PROGRESS durante i 65 minuti
        assertEquals(SessionStatus.IN_PROGRESS, sprint.getSessionStatus());
        assertTrue(sprint.isUnderway());
        assertFalse(sprint.isFinished());
    }

    @Test
    public void testSprintFinishesWhenExceedingMaximumDuration() {
        // Sprint iniziata 85 minuti fa: la sessione è ufficialmente conclusa (superati i 75 min)
        LocalDateTime now = LocalDateTime.now();
        Sprint sprint = new Sprint("2026-10-10", "10:00:00Z");
        sprint.setStartDateTime(now.minusMinutes(85));
        sprint.setEndDateTime();
        sprint.setSessionStatus();

        // Deve risultare FINISHED
        assertEquals(SessionStatus.FINISHED, sprint.getSessionStatus());
        assertFalse(sprint.isUnderway());
        assertTrue(sprint.isFinished());
    }

    @Test
    public void testSprintFinishesWhenLaterSessionHasStarted() {
        LocalDateTime now = LocalDateTime.now();

        Practice fp1 = new Practice("2026-10-09", "10:00:00Z", 1);
        fp1.setStartDateTime(now.minusDays(1));

        SprintQualifying sq = new SprintQualifying("2026-10-09", "14:00:00Z");
        sq.setStartDateTime(now.minusDays(1));

        Sprint sprint = new Sprint("2026-10-10", "10:00:00Z");
        sprint.setStartDateTime(now.minusHours(4));

        // Qualifiche già iniziate 10 minuti fa
        Qualifying qualifying = new Qualifying("2026-10-10", "14:00:00Z");
        qualifying.setStartDateTime(now.minusMinutes(10));

        Race race = new Race();
        race.setStartDateTime(now.plusDays(1));

        WeeklyRaceSprint weeklyRace = new WeeklyRaceSprint(sq, sprint);
        weeklyRace.setFirstPractice(fp1);
        weeklyRace.setQualifying(qualifying);
        weeklyRace.setFinalRace(race);

        List<Session> sessions = weeklyRace.getSessions();
        // In setSessions, sprint deve essere contrassegnata FINISHED perché Qualifying è già iniziata
        assertEquals(SessionStatus.FINISHED, sprint.getSessionStatus());
        // E la sessione successiva attiva deve essere Qualifying
        Session next = weeklyRace.findNextEvent(sessions);
        assertNotNull(next);
        assertEquals(qualifying, next);
    }

    @Test
    public void testSprintConcludedTransitionsNextEventToQualifying() {
        LocalDateTime now = LocalDateTime.now();

        Practice fp1 = new Practice("2026-10-09", "10:00:00Z", 1);
        fp1.setStartDateTime(now.minusDays(1));

        SprintQualifying sq = new SprintQualifying("2026-10-09", "14:00:00Z");
        sq.setStartDateTime(now.minusDays(1));

        // Sprint iniziata 85 minuti fa (ora terminata)
        Sprint sprint = new Sprint("2026-10-10", "10:00:00Z");
        sprint.setStartDateTime(now.minusMinutes(85));

        // Qualifiche programmate tra 2 ore
        Qualifying qualifying = new Qualifying("2026-10-10", "14:00:00Z");
        qualifying.setStartDateTime(now.plusHours(2));

        Race race = new Race();
        race.setStartDateTime(now.plusDays(1));

        WeeklyRaceSprint weeklyRace = new WeeklyRaceSprint(sq, sprint);
        weeklyRace.setFirstPractice(fp1);
        weeklyRace.setQualifying(qualifying);
        weeklyRace.setFinalRace(race);

        List<Session> sessions = weeklyRace.getSessions();
        // Sprint deve risultare FINISHED
        assertEquals(SessionStatus.FINISHED, sprint.getSessionStatus());
        assertTrue(sprint.isFinished());
        assertFalse(sprint.isUnderway());

        // findNextEvent deve passare alla prossima sessione: Qualifying
        Session next = weeklyRace.findNextEvent(sessions);
        assertNotNull(next);
        assertEquals(qualifying, next);
    }
}
