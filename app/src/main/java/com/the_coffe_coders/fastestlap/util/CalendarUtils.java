package com.the_coffe_coders.fastestlap.util;

import android.content.Context;
import android.content.Intent;
import android.provider.CalendarContract;
import android.util.Log;

import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Practice;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Session;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.WeeklyRace;

import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.os.Environment;
import androidx.core.content.FileProvider;
import android.content.ClipData;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.TimeUtils;

/**
 * Utility class to export Grand Prix sessions into the device calendar app.
 * Uses CalendarContract.Events INSERT intents — no WRITE_CALENDAR permission required.
 */
public class CalendarUtils {

    private CalendarUtils() {
        // Utility class, no instantiation
    }

    /**
     * Opens the system calendar app to insert all sessions of a WeeklyRace.
     * Each session (FP1, FP2, FP3, Qualifying, Race, etc.) is inserted as a separate event.
     *
     * @param context    The Android context.
     * @param weeklyRace The race weekend whose sessions will be inserted.
     */
    public static void addWeekendToCalendar(Context context, WeeklyRace weeklyRace) {
        List<Session> sessions = weeklyRace.getSessions();
        Log.i("Calendar Utils", "sessions:\n" + sessions);
        for (Session session : sessions) {
            if (session != null) {
                String sessionLabel = buildSessionLabel(context, session, weeklyRace.getRaceName());
                insertCalendarEvent(context, sessionLabel, weeklyRace, session);
            }
        }
    }

    public static void addRacesToCalendar(Context context, List<WeeklyRace> weeklyRaces) {
        if (weeklyRaces == null || weeklyRaces.isEmpty()) return;

        // Build ICS content representing all sessions
        String ics = buildIcsContent(context, weeklyRaces);

        // Save file into app-specific external downloads directory (no runtime permission required)
        File downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (downloadsDir == null) downloadsDir = context.getCacheDir();

        String fileName = "fastestlap_events_" + Date.from(Instant.ofEpochMilli(System.currentTimeMillis())) + ".ics";
        File icsFile = new File(downloadsDir, fileName);

        try (FileOutputStream fos = new FileOutputStream(icsFile)) {
            fos.write(ics.getBytes(StandardCharsets.UTF_8));
            fos.flush();
        } catch (IOException e) {
            Log.e("Calendar Utils", "Failed to write ICS file", e);
            return;
        }

        // Register the file as a completed download so it appears in the system Downloads UI
        try {
            DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.addCompletedDownload(icsFile.getName(), "FastestLap calendar export",
                        true, "text/calendar", icsFile.getAbsolutePath(), icsFile.length(), true);
            }
        } catch (Exception e) {
            Log.w("Calendar Utils", "Could not register completed download", e);
        }

        // Share file via FileProvider (required to avoid FileUriExposedException on Android N+)
        Uri uri;
        try {
            uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", icsFile);
        } catch (Exception e) {
            // If FileProvider is not available for some reason, fall back to file:// (may crash on newer Android versions)
            uri = Uri.fromFile(icsFile);
            Log.w("Calendar Utils", "FileProvider unavailable, falling back to file:// URI", e);
        }

        Intent openIntent = new Intent(Intent.ACTION_VIEW);
        openIntent.setDataAndType(uri, "text/calendar");
        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Ensure ClipData is set when using content URI so some receivers get permissions
        try {
            ClipData clip = ClipData.newRawUri("ics", uri);
            openIntent.setClipData(clip);
        } catch (Exception ignored) {
        }

        PackageManager pm = context.getPackageManager();

        // Prefer Google Calendar
        openIntent.setPackage("com.google.android.calendar");
        try {
            // Grant URI permission to the resolved activity (if any)
            List<ResolveInfo> resInfoList = pm.queryIntentActivities(openIntent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String pkgName = resolveInfo.activityInfo.packageName;
                context.grantUriPermission(pkgName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            context.startActivity(openIntent);
            return;
        } catch (ActivityNotFoundException | SecurityException e) {
            Log.i("Calendar Utils", "Google Calendar not available or cannot open file, falling back", e);
        }

        // Fallback: generic chooser — grant permissions to all possible handlers first
        try {
            openIntent.setPackage(null);
            List<ResolveInfo> resInfoList = pm.queryIntentActivities(openIntent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String pkgName = resolveInfo.activityInfo.packageName;
                context.grantUriPermission(pkgName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            Intent chooser = Intent.createChooser(openIntent, "Open calendar file");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(chooser);
        } catch (ActivityNotFoundException e) {
            Log.i("Calendar Utils", "No activity found to open ICS file", e);
        }
    }

    /**
     * Builds an iCalendar (.ics) file content representing all sessions of the given races.
     */
    private static String buildIcsContent(Context context, List<WeeklyRace> weeklyRaces) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("VERSION:2.0\r\n");
        sb.append("PRODID:-//FastestLap//EN\r\n");
        sb.append("CALSCALE:GREGORIAN\r\n");
        sb.append("METHOD:PUBLISH\r\n");

        for (WeeklyRace weeklyRace : weeklyRaces) {
            if (weeklyRace == null) continue;
            List<Session> sessions = weeklyRace.getSessions();
            if (sessions == null) continue;
            for (Session session : sessions) {
                if (session == null) continue;

                ZonedDateTime startZdt = session.getStartDateTime().atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC);

                ZonedDateTime endZdt;
                if (session.getEndDateTime() != null) {
                    endZdt = session.getEndDateTime().atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC);
                } else {
                    endZdt = startZdt.plusHours(2);
                }

                String uid = UUID.randomUUID().toString() + "@fastestlap";
                String summary = buildSessionLabel(context, session, weeklyRace.getRaceName());

                String description = "Round " + weeklyRace.getRound()
                        + " · " + weeklyRace.getSeason()
                        + " · " + weeklyRace.getRaceName();

                String location = "";
                if (weeklyRace.getTrack() != null && weeklyRace.getTrack().getLocation() != null) {
                    location = weeklyRace.getTrack().getLocation().getLocality()
                            + ", " + weeklyRace.getTrack().getLocation().getCountry();
                }

                sb.append("BEGIN:VEVENT\r\n");
                sb.append("UID:").append(uid).append("\r\n");
                sb.append("DTSTAMP:").append(ZonedDateTime.now(ZoneOffset.UTC).format(fmt)).append("\r\n");
                sb.append("DTSTART:").append(startZdt.format(fmt)).append("\r\n");
                sb.append("DTEND:").append(endZdt.format(fmt)).append("\r\n");
                sb.append("SUMMARY:").append(escapeText(summary)).append("\r\n");
                sb.append("DESCRIPTION:").append(escapeText(description)).append("\r\n");
                if (!location.isEmpty()) sb.append("LOCATION:").append(escapeText(location)).append("\r\n");
                sb.append("END:VEVENT\r\n");
            }
        }

        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }

    /** Escape iCalendar text per simple rules (backslash, semicolon, comma, newline). */
    private static String escapeText(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\n", "\\n");
    }

    /**
     * Builds a human-readable title for the calendar event.
     * Uses the session name maps from Constants, respecting the device locale.
     * e.g. "Monaco GP – Free Practice 1" or "Monaco GP – Prova Libera 1"
     */
    private static String buildSessionLabel(Context context, Session session, String raceName) {
        // Determine the session key for the Constants maps
        String sessionKey;
        if (session instanceof Practice) {
            Practice practice = (Practice) session;
            sessionKey = practice.getPractice(); // "Practice1", "Practice2", "Practice3"
        } else {
            sessionKey = session.getClass().getSimpleName(); // "Qualifying", "Sprint", "Race", …
        }

        // Pick the right locale map
        boolean isItalian = Locale.getDefault().getLanguage().equals("it");
        String sessionName;
        if (isItalian) {
            sessionName = Constants.SESSION_NAMES_ITA.getOrDefault(sessionKey, sessionKey);
        } else {
            sessionName = Constants.SESSION_NAMES_ENG.getOrDefault(sessionKey, sessionKey);
        }

        return raceName + " – " + sessionName;
    }

    /**
     * Fires an INSERT intent for the calendar app, pre-filling all session details.
     */
    private static void insertCalendarEvent(Context context, String title,
                                            WeeklyRace weeklyRace, Session session) {
        // Convert ThreeTenBP LocalDateTime → epoch millis
        ZonedDateTime startZdt = session.getStartDateTime().atZone(ZoneId.systemDefault());
        long startMillis = startZdt.toInstant().toEpochMilli();

        // Use endDateTime if available, otherwise fall back to a 2-hour window
        long endMillis;
        if (session.getEndDateTime() != null) {
            ZonedDateTime endZdt = session.getEndDateTime().atZone(ZoneId.systemDefault());
            endMillis = endZdt.toInstant().toEpochMilli();
        } else {
            endMillis = startMillis + 2 * 60 * 60 * 1000L;
        }

        String description = "Round " + weeklyRace.getRound()
                + " · " + weeklyRace.getSeason()
                + " · " + weeklyRace.getRaceName();

        String location = "";
        if (weeklyRace.getTrack() != null && weeklyRace.getTrack().getLocation() != null) {
            location = weeklyRace.getTrack().getLocation().getLocality()
                    + ", " + weeklyRace.getTrack().getLocation().getCountry();
        }

        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
                .putExtra(CalendarContract.Events.TITLE, title)
                .putExtra(CalendarContract.Events.DESCRIPTION, description)
                .putExtra(CalendarContract.Events.EVENT_LOCATION, location)
                .putExtra(CalendarContract.Events.AVAILABILITY,
                        CalendarContract.Events.AVAILABILITY_BUSY);

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
