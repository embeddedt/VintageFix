package org.embeddedt.vintagefix.util;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TimeUtil {
    private static final TimeUnit[] UNITS = TimeUnit.values();

    private static TimeUnit chooseUnit(long duration, TimeUnit unit) {
        for (int i = UNITS.length - 1; i > unit.ordinal(); i--) {
            if (UNITS[i].convert(duration, unit) > 0) {
                return UNITS[i];
            }
        }
        return unit;
    }

    private static String abbreviateTime(TimeUnit unit) {
        switch (unit) {
            case NANOSECONDS:
                return "ns";
            case MICROSECONDS:
                return "us";
            case MILLISECONDS:
                return "ms";
            case SECONDS:
                return "s";
            case MINUTES:
                return "min";
            case HOURS:
                return "h";
            case DAYS:
                return "d";
            default:
                throw new IllegalArgumentException();
        }
    }

    public static String stringifyTime(long duration, TimeUnit unit) {
        return stringifyTime(duration, unit, chooseUnit(duration, unit));
    }

    public static String stringifyTime(long duration, TimeUnit unit, TimeUnit displayUnit) {
        double displayDuration = ((double)duration) / unit.convert(1, displayUnit);
        return String.format(Locale.ROOT, "%.4g %s", displayDuration, abbreviateTime(displayUnit));
    }
}
