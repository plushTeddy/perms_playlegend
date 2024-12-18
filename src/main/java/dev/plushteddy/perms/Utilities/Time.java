package dev.plushteddy.perms.Utilities;

public class Time {

    public long getFutureYearExpiry() {
        return System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365 * (3000 - 1970));
    }



    public long parseDuration(String time) {
        int i = Integer.parseInt(time.substring(0, time.length() - 1));
        if (time.endsWith("m")) {
            try {
                return i * 60000L;
            } catch (NumberFormatException e) {
                return -1;
            }
        } else if (time.endsWith("h")) {
            try {
                return i * 3600000L;
            } catch (NumberFormatException e) {
                return -1;
            }
        } else if (time.endsWith("d")) {
            try {
                return i * 86400000L;
            } catch (NumberFormatException e) {
                return -1;
            }
        } else if (time.endsWith("s")) {
            try {
                return i * 1000L;
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }
}
