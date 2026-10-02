package com.the_coffe_coders.fastestlap.util.calendar;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.FileProvider;
import androidx.core.os.LocaleListCompat;

import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Practice;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Session;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.domain.f1.track.Track;
import com.the_coffe_coders.fastestlap.util.Constants;

import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Utility class to export Grand Prix sessions into the device calendar app.
 * Generates an RFC 5545 standard .ics (iCalendar) package containing all weekend
 * sessions and opens it with system calendar applications (e.g. Google Calendar)
 * allowing one-click bulk import without requiring WRITE_CALENDAR runtime permissions.
 */
public class CalendarUtils {

    private CalendarUtils() {
        // Utility class, no instantiation
    }

    /**
     * Exports all upcoming sessions of a single race weekend into a single .ics file
     * and opens it in the device calendar app (e.g. Google Calendar) so the user can
     * import all events at once with a single confirmation.
     *
     * @param context    The Android context.
     * @param weeklyRace The race weekend whose sessions will be exported.
     */
    public static void addWeekendToCalendar(Context context, WeeklyRace weeklyRace) {
        List<WeeklyRace> singleRaceList = Collections.singletonList(weeklyRace);
        addRacesToCalendar(context, singleRaceList);
    }

    public static void addRacesToCalendar(Context context, List<WeeklyRace> weeklyRaces) {
        if (weeklyRaces == null || weeklyRaces.isEmpty()) return;

        // Build ICS content representing all sessions
        String ics = buildIcsContent(context, weeklyRaces);

        // Save file into app-specific external downloads directory (no runtime permission required)
        File downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (downloadsDir == null) downloadsDir = context.getCacheDir();

        // Build a clean, human-readable filename based on the first race name (or timestamp fallback)
        File icsFile = getIcsFile(weeklyRaces, downloadsDir);

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
                @SuppressWarnings("deprecation")
                long downloadId = dm.addCompletedDownload(icsFile.getName(), "FastestLap calendar export",
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

    @NonNull
    private static File getIcsFile(List<WeeklyRace> weeklyRaces, File downloadsDir) {
        String baseName = "fastestlap_calendar";
        if (!weeklyRaces.isEmpty() && weeklyRaces.get(0) != null && weeklyRaces.get(0).getRaceName() != null) {
            if (weeklyRaces.size() == 1) {
                // Single weekend: use the race name (e.g. "fastestlap_bahrain_gp.ics")
                baseName = "fastestlap_" + weeklyRaces.get(0).getRaceName()
                        .toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9]+", "_")
                        .replaceAll("_+$", "");
            }
            // Multiple weekends: keep generic name
        }
        String fileName = baseName + ".ics";
        return new File(downloadsDir, fileName);
    }

    /**
     * Builds an iCalendar (.ics) file content representing all sessions of the given races.
     */
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
        sb.append("X-WR-CALNAME:FastestLap\r\n");
        sb.append("X-WR-CALDESC:Calendario Gran Premi e Sessioni F1 FastestLap\r\n");
        sb.append("X-WR-TIMEZONE:UTC\r\n");

        boolean isIt = isItalianLanguage(context);

        for (WeeklyRace weeklyRace : weeklyRaces) {
            if (weeklyRace == null) continue;
            List<Session> sessions = weeklyRace.getSessions();
            if (sessions == null) continue;
            for (Session session : sessions) {
                if (session == null || session.getStartDateTime() == null) continue;
                // Skip sessions that have already concluded
                if (session.isFinished()) continue;

                ZonedDateTime startZdt = session.getStartDateTime().atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC);

                ZonedDateTime endZdt;
                if (session.getEndDateTime() != null) {
                    endZdt = session.getEndDateTime().atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC);
                } else {
                    endZdt = startZdt.plusHours(2);
                }

                String uid = UUID.randomUUID().toString() + "@fastestlap";
                String summary = buildSessionLabel(context, session, weeklyRace.getRaceName());
                String location = buildLocation(weeklyRace);

                String description = "FastestLap · " + (weeklyRace.getRaceName() != null ? weeklyRace.getRaceName() : "Formula 1")
                        + (isIt ? "\nStagione: " : "\nSeason: ") + weeklyRace.getSeason()
                        + " · Round " + weeklyRace.getRound()
                        + (!location.isEmpty() ? ((isIt ? "\nCircuito: " : "\nCircuit: ") + location) : "");

                sb.append("BEGIN:VEVENT\r\n");
                sb.append("UID:").append(uid).append("\r\n");
                sb.append("DTSTAMP:").append(ZonedDateTime.now(ZoneOffset.UTC).format(fmt)).append("\r\n");
                sb.append("DTSTART:").append(startZdt.format(fmt)).append("\r\n");
                sb.append("DTEND:").append(endZdt.format(fmt)).append("\r\n");
                sb.append("SUMMARY:").append(escapeText(summary)).append("\r\n");
                sb.append("DESCRIPTION:").append(escapeText(description)).append("\r\n");
                if (!location.isEmpty()) {
                    sb.append("LOCATION:").append(escapeText(location)).append("\r\n");
                }
                sb.append("STATUS:CONFIRMED\r\n");
                sb.append("SEQUENCE:0\r\n");
                sb.append("END:VEVENT\r\n");
            }
        }

        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }

    /**
     * Builds a clean, complete location string avoiding null values or "Singapore, null".
     */
    private static String buildLocation(WeeklyRace weeklyRace) {
        if (weeklyRace == null || weeklyRace.getTrack() == null) return "";
        Track track = weeklyRace.getTrack();

        String circuitName = track.getTrackName();
        String locality = null;
        String country = null;

        if (track.getLocation() != null) {
            locality = track.getLocation().getLocality();
            country = track.getLocation().getCountry();
        }
        if ((country == null || country.trim().isEmpty() || country.equalsIgnoreCase("null"))
                && track.getCountry() != null && !track.getCountry().trim().isEmpty() && !track.getCountry().equalsIgnoreCase("null")) {
            country = track.getCountry();
        }

        StringBuilder sb = new StringBuilder();
        if (circuitName != null && !circuitName.trim().isEmpty() && !circuitName.equalsIgnoreCase("null")) {
            sb.append(circuitName.trim());
        }

        if (locality != null && !locality.trim().isEmpty() && !locality.equalsIgnoreCase("null")) {
            String cleanLoc = locality.trim();
            if (!sb.toString().toLowerCase(Locale.ROOT).contains(cleanLoc.toLowerCase(Locale.ROOT))) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(cleanLoc);
            }
        }

        if (country != null && !country.trim().isEmpty() && !country.equalsIgnoreCase("null")) {
            String cleanCountry = country.trim();
            if (!sb.toString().toLowerCase(Locale.ROOT).contains(cleanCountry.toLowerCase(Locale.ROOT))) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(cleanCountry);
            }
        }

        return sb.toString();
    }

    /**
     * Checks if current active language (in app or system) is Italian.
     */
    private static boolean isItalianLanguage(Context context) {
        try {
            LocaleListCompat appLocales = AppCompatDelegate.getApplicationLocales();
            String langTag = appLocales.toLanguageTags();
            if (langTag != null && !langTag.isEmpty()) {
                return langTag.toLowerCase(Locale.ROOT).startsWith("it");
            }
        } catch (Exception ignored) {}

        if (context != null) {
            try {
                String sysLang = context.getResources().getConfiguration().getLocales().get(0).getLanguage();
                if (sysLang != null && sysLang.toLowerCase(Locale.ROOT).startsWith("it")) {
                    return true;
                }
            } catch (Exception ignored) {}
        }

        return Locale.getDefault().getLanguage().toLowerCase(Locale.ROOT).startsWith("it");
    }

    /**
     * Escape iCalendar text per simple rules (backslash, semicolon, comma, newline).
     */
    private static String escapeText(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\n", "\\n");
    }

    /**
     * Builds a human-readable title for the calendar event.
     * Uses the session name maps from Constants, respecting the active app/device locale.
     * e.g. "Singapore Grand Prix - Prova Libera 1"
     */
    private static String buildSessionLabel(Context context, Session session, String raceName) {
        String sessionKey;
        if (session instanceof Practice) {
            Practice practice = (Practice) session;
            int num = practice.getNumber() > 0 ? practice.getNumber() : 1;
            sessionKey = "Practice" + num;
        } else {
            sessionKey = session.getClass().getSimpleName();
        }

        boolean isItalian = isItalianLanguage(context);
        String sessionName;
        if (isItalian) {
            sessionName = Constants.SESSION_NAMES_ITA.getOrDefault(sessionKey, sessionKey);
        } else {
            sessionName = Constants.SESSION_NAMES_ENG.getOrDefault(sessionKey, sessionKey);
        }

        String validRaceName = raceName != null && !raceName.trim().isEmpty() ? raceName.trim() : "Formula 1";
        return validRaceName + " - " + sessionName;
    }
}

