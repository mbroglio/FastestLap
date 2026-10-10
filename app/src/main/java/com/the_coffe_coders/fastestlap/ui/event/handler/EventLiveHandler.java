package com.the_coffe_coders.fastestlap.ui.event.handler;

import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Practice;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Session;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.domain.f1.track.Track;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.ui.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ui.NavigationUtils;

import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

import java.util.List;
import java.util.Locale;

/**
 * EventLiveHandler
 *
 * Gestisce la visualizzazione della card live per la sessione corrente o imminente,
 * indirizzando verso LiveActivity con tutti i parametri necessari per il Live Timing.
 */
public class EventLiveHandler {
    private static final String TAG = "EventLiveHandler";

    private final AppCompatActivity activity;
    private final View rootView;
    private final LoadingScreen loadingScreen;

    private String trackId;
    private String eventTitle;
    private String totalLaps;
    private String sessionName;
    private String sessionType = "race";
    private String sessionPart = "";
    private String circuitTimeZone;
    private String trackImageUrl;

    public EventLiveHandler(AppCompatActivity activity, View rootView, LoadingScreen loadingScreen) {
        this.activity = activity;
        this.rootView = rootView;
        this.loadingScreen = loadingScreen;
    }

    public void setupLiveSession(WeeklyRace weeklyRace, Track track, String trackId, Session activeSession, Session nextEvent) {
        this.trackId = trackId;
        this.trackImageUrl = null;
        if (track != null) {
            if (track.getTrack_minimal_layout_url() != null && !track.getTrack_minimal_layout_url().trim().isEmpty()) {
                this.trackImageUrl = track.getTrack_minimal_layout_url().trim();
            } else if (track.getTrack_full_layout_url() != null && !track.getTrack_full_layout_url().trim().isEmpty()) {
                this.trackImageUrl = track.getTrack_full_layout_url().trim();
            } else if (track.getTrack_pic_url() != null && !track.getTrack_pic_url().trim().isEmpty()) {
                this.trackImageUrl = track.getTrack_pic_url().trim();
            }
        }
        if (this.trackImageUrl == null && weeklyRace != null && weeklyRace.getTrack() != null) {
            Track wt = weeklyRace.getTrack();
            if (wt.getTrack_minimal_layout_url() != null && !wt.getTrack_minimal_layout_url().trim().isEmpty()) {
                this.trackImageUrl = wt.getTrack_minimal_layout_url().trim();
            } else if (wt.getTrack_full_layout_url() != null && !wt.getTrack_full_layout_url().trim().isEmpty()) {
                this.trackImageUrl = wt.getTrack_full_layout_url().trim();
            } else if (wt.getTrack_pic_url() != null && !wt.getTrack_pic_url().trim().isEmpty()) {
                this.trackImageUrl = wt.getTrack_pic_url().trim();
            }
        }
        this.eventTitle = (weeklyRace != null && weeklyRace.getRaceName() != null)
                ? weeklyRace.getRaceName().toUpperCase()
                : "FORMULA 1 GRAND PRIX";

        List<Session> sessions = (weeklyRace != null) ? weeklyRace.getSessions() : null;
        Session targetSession = activeSession != null ? activeSession : nextEvent;

        if (targetSession != null) {
            String sid = targetSession.getClass().getSimpleName();
            if (targetSession.isPractice()) {
                Practice p = (Practice) targetSession;
                if (p.getNumber() <= 0 && sessions != null) {
                    int pIdx = 1;
                    for (Session s : sessions) {
                        if (s == targetSession) {
                            p.setNumber(pIdx);
                            break;
                        }
                        if (s != null && s.isPractice()) pIdx++;
                    }
                }
                sid = p.getPractice();
                sessionType = "practice";
                sessionPart = "FP" + (p.getNumber() > 0 ? p.getNumber() : 1);
            } else if (targetSession.isSprintQualifying()) {
                sid = "SprintQualifying";
                sessionType = "qualifying";
                sessionPart = "SQ";
            } else if (targetSession.isQualifying()) {
                sid = "Qualifying";
                sessionType = "qualifying";
                sessionPart = "Q";
            } else if (targetSession.isSprint()) {
                sid = "Sprint";
                sessionType = "sprint";
                sessionPart = "SPRINT";
            } else {
                sid = "Race";
                sessionType = "race";
                sessionPart = "RACE";
            }

            String langTags = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().toLanguageTags();
            if (langTags != null && langTags.toLowerCase(Locale.ROOT).startsWith("it")) {
                sessionName = Constants.SESSION_NAMES_ITA.getOrDefault(sid, sid);
            } else {
                sessionName = Constants.SESSION_NAMES_ENG.getOrDefault(sid, sid);
            }
        } else {
            sessionName = "Sessione di Pista";
            sessionType = "race";
            sessionPart = "RACE";
        }

        totalLaps = (track != null) ? track.getLaps() : null;
        circuitTimeZone = (track != null && track.getLocation() != null) ? track.getLocation().getZoneId() : null;

        boolean isSessionLive = activeSession != null;
        long sessionElapsedSeconds = 0;
        if (activeSession != null && activeSession.getStartDateTime() != null) {
            try {
                long startMillis = ZonedDateTime.of(activeSession.getStartDateTime(), ZoneId.systemDefault()).toInstant().toEpochMilli();
                long currentMillis = System.currentTimeMillis();
                if (currentMillis >= startMillis) {
                    sessionElapsedSeconds = (currentMillis - startMillis) / 1000L;
                }
            } catch (Exception ignored) {}
        }

        updateLiveCard(isSessionLive, sessionElapsedSeconds);
    }

    public void activateLive() {
        updateLiveCard(true, 0L);
    }

    public void updateLiveCard(boolean isLive, long sessionElapsedSeconds) {
        View liveSession = rootView.findViewById(R.id.event_live_card);
        View noLiveSession = rootView.findViewById(R.id.event_not_live_card);

        if (liveSession == null || noLiveSession == null) {
            return;
        }

        liveSession.setVisibility(View.GONE);
        noLiveSession.setVisibility(View.VISIBLE);

        Log.i(TAG, "setupLiveSession configurato: trackImageUrl=" + this.trackImageUrl + ", trackId=" + this.trackId);

        /* Card non live (permette comunque il collegamento diretto al flusso SignalR Live)
        noLiveSession.setOnClickListener(v -> NavigationUtils.navigateToLivePage(
                activity,
                eventTitle,
                totalLaps,
                trackId,
                trackImageUrl,
                true,
                sessionName,
                sessionType,
                sessionPart,
                circuitTimeZone,
                0L
        ));

        // Card sessione live
        ImageView liveIcon = rootView.findViewById(R.id.live_icon);
        if (liveIcon != null && isLive) {
            Animation pulse = AnimationUtils.loadAnimation(activity, R.anim.pulse_dynamic);
            liveIcon.startAnimation(pulse);
        }

        liveSession.setOnClickListener(v -> NavigationUtils.navigateToLivePage(
                activity,
                eventTitle,
                totalLaps,
                trackId,
                trackImageUrl,
                true,
                sessionName,
                sessionType,
                sessionPart,
                circuitTimeZone,
                sessionElapsedSeconds
        ));

        Log.i(TAG, "EventLiveHandler configurato: isLive=" + isLive + ", session=" + sessionName);
         */
        loadingScreen.hideLoadingScreen();
    }
}
