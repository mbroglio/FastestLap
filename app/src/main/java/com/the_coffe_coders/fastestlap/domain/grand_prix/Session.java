package com.the_coffe_coders.fastestlap.domain.grand_prix;

import android.util.Log;

import androidx.annotation.NonNull;

import com.the_coffe_coders.fastestlap.util.Constants;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
public abstract class Session {
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private SessionStatus sessionStatus;

    public Session(String date, String time) {
        setStartDateTime(date, time);
        setSessionStatus();
    }
    public void setStartDateTime(String date, String time) {
        if (!time.contains("Z")) {
            time = time.concat("Z");
        }
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(date + "T" + time + "[UTC]");
        ZoneId localZone = ZoneId.systemDefault();
        // Convertire al fuso orario locale
        ZonedDateTime localZonedDateTime = zonedDateTime.withZoneSameInstant(localZone);
        this.startDateTime = localZonedDateTime.toLocalDateTime();
    }
    public void setEndDateTime() {
        Log.i("Session", this.getClass().getSimpleName());
        @SuppressWarnings("ConstantConditions")
        int duration = Constants.SESSION_DURATION.get(this.getClass().getSimpleName());
        this.endDateTime = getStartDateTime().plusMinutes(duration);
    }

    public String getTime() {
        String start = startDateTime.toLocalTime().toString();
        String end = endDateTime.toLocalTime().toString();

        return start + " - " + end;
    }

    public String getStartingTime() {
        return getStartDateTime().toLocalTime().toString();
    }

    public Boolean isFinished() {
        return true;
    }

    public Boolean isUnderway() {
        return true;
    }

    public void setSessionStatus() {
        if ((endDateTime != null) && endDateTime.isBefore(LocalDateTime.now())) {
            sessionStatus = SessionStatus.FINISHED;
        } else if (this.startDateTime.isAfter(LocalDateTime.now())) {
            sessionStatus = SessionStatus.NOT_STARTED;
        } else {
            sessionStatus = SessionStatus.IN_PROGRESS;
        }
    }
}
