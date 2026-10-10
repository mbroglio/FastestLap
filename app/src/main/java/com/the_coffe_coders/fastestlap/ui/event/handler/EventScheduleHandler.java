package com.the_coffe_coders.fastestlap.ui.event.handler;

import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Practice;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Session;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.ui.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import java.util.List;

public class EventScheduleHandler {
    private static final String TAG = "EventScheduleHandler";

    private final AppCompatActivity activity;
    private final View rootView;
    private final LoadingScreen loadingScreen;

    public interface OnSessionScheduleClickListener {
        void onSessionClick(Session session, String round);
    }

    public EventScheduleHandler(AppCompatActivity activity, View rootView, LoadingScreen loadingScreen) {
        this.activity = activity;
        this.rootView = rootView;
        this.loadingScreen = loadingScreen;
    }

    public void createWeekSchedule(WeeklyRace weeklyRace, List<Session> sessions, OnSessionScheduleClickListener clickListener) {
        View eventSchedule = rootView.findViewById(R.id.event_schedule_table);
        if (eventSchedule == null || sessions == null) {
            return;
        }

        loadingScreen.updateProgress();

        String sessionId;
        for (int i = 0; i < sessions.size(); i++) {
            Session session = sessions.get(i);
            if (session == null) continue;

            sessionId = session.getClass().getSimpleName();
            if (session.isPractice()) {
                Practice practice = (Practice) session;
                if (practice.getNumber() <= 0) {
                    practice.setNumber(i + 1);
                }
                sessionId = practice.getPractice();
            }

            Integer nameField = Constants.SESSION_NAME_FIELD.get(sessionId);
            Integer dayField = Constants.SESSION_DAY_FIELD.get(sessionId);
            Integer timeField = Constants.SESSION_TIME_FIELD.get(sessionId);

            if (nameField != null && dayField != null) {
                UIUtils.translateSchedule(activity,
                        eventSchedule.findViewById(nameField),
                        eventSchedule.findViewById(dayField),
                        sessionId,
                        session);
            }

            if (timeField != null) {
                UIUtils.setTextViewTextWithCondition(sessionId.equals("Race") || sessionId.equals("Sprint"),
                        session.getStartingTime(),
                        session.getTime(),
                        eventSchedule.findViewById(timeField));
            }

            setChequeredFlag(eventSchedule, session, weeklyRace, clickListener);
        }
    }

    private void setChequeredFlag(View view, Session session, WeeklyRace weeklyRace, OnSessionScheduleClickListener clickListener) {
        String sessionId = session.getClass().getSimpleName();
        if (session.isPractice()) {
            Practice practice = (Practice) session;
            sessionId = practice.getPractice();
        }

        Integer flagContainerId = Constants.SESSION_FLAG_CONTAINER.get(sessionId);
        Integer flagId = Constants.SESSION_FLAG_FIELD.get(sessionId);
        Integer rowId = Constants.SESSION_ROW.get(sessionId);

        View flagContainer = flagContainerId != null ? view.findViewById(flagContainerId) : null;
        View flagImage = flagId != null ? view.findViewById(flagId) : null;
        View row = rowId != null ? view.findViewById(rowId) : null;

        boolean isFinished = !Boolean.TRUE.equals(session != null ? session.isUnderway() : false)
                && ((weeklyRace != null && weeklyRace.isWeekFinished()) || (session != null && session.isFinished()));

        if (isFinished) {
            String round = weeklyRace != null ? weeklyRace.getRound() : "";

            if (flagContainer != null) {
                flagContainer.setVisibility(View.VISIBLE);
                if (!session.isPractice()) {
                    flagContainer.setClickable(true);
                    flagContainer.setFocusable(true);
                    flagContainer.setOnClickListener(v -> {
                        if (clickListener != null) {
                            clickListener.onSessionClick(session, round);
                        }
                    });
                } else {
                    flagContainer.setClickable(false);
                    flagContainer.setFocusable(false);
                    flagContainer.setOnClickListener(null);
                }
            }
            if (flagImage != null) {
                flagImage.setVisibility(View.VISIBLE);
            }
            if (!session.isPractice() && row != null) {
                row.setClickable(true);
                row.setFocusable(true);
                row.setOnClickListener(v -> {
                    if (clickListener != null) {
                        clickListener.onSessionClick(session, round);
                    }
                });
            }
        } else {
            if (flagContainer != null) {
                flagContainer.setVisibility(View.INVISIBLE);
                flagContainer.setClickable(false);
                flagContainer.setFocusable(false);
                flagContainer.setOnClickListener(null);
            }
            if (flagImage != null) {
                flagImage.setVisibility(View.INVISIBLE);
            }
            if (row != null) {
                row.setClickable(false);
                row.setFocusable(false);
                row.setOnClickListener(null);
            }
        }
    }
}
