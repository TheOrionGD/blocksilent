package com.blocksilent.app.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TimeUtils {

    /**
     * Converts a time string (e.g. "09:00 AM", "09:00", "2:30 PM", "14:30") to minutes past midnight.
     * Returns -1 if unparseable.
     */
    public static int parseTimeToMinutes(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return -1;
        String clean = timeStr.trim();

        try {
            if (clean.toUpperCase(Locale.US).contains("AM") || clean.toUpperCase(Locale.US).contains("PM")) {
                SimpleDateFormat sdf12 = new SimpleDateFormat("hh:mm a", Locale.US);
                Date date = sdf12.parse(clean);
                if (date != null) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
                }
            }
        } catch (Exception ignored) {}

        try {
            SimpleDateFormat sdf24 = new SimpleDateFormat("HH:mm", Locale.US);
            Date date = sdf24.parse(clean);
            if (date != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
            }
        } catch (Exception ignored) {}

        try {
            String str = clean.toUpperCase(Locale.US);
            boolean isPm = str.contains("PM");
            boolean isAm = str.contains("AM");
            str = str.replace("AM", "").replace("PM", "").trim();
            String[] parts = str.split(":");
            if (parts.length >= 2) {
                int hour = Integer.parseInt(parts[0].trim());
                int min = Integer.parseInt(parts[1].trim());
                if (isPm && hour < 12) hour += 12;
                if (isAm && hour == 12) hour = 0;
                return hour * 60 + min;
            }
        } catch (Exception ignored) {}

        return -1;
    }

    /**
     * Checks whether current target time is between startTime and endTime.
     */
    public static boolean isTimeBetween(String targetTime, String startTime, String endTime) {
        int target = parseTimeToMinutes(targetTime);
        int start = parseTimeToMinutes(startTime);
        int end = parseTimeToMinutes(endTime);

        if (target < 0 || start < 0 || end < 0) return false;

        if (start <= end) {
            return target >= start && target <= end;
        } else {
            // Overlapping midnight (e.g. 23:00 to 02:00)
            return target >= start || target <= end;
        }
    }
}
