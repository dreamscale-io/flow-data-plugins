package org.dreamscale.time;

import java.time.Duration;

public class TimeConverter {

    private static final int SECONDS_IN_MINUTE = 60;
    private static final int SECONDS_IN_HOUR = SECONDS_IN_MINUTE * 60;
    private static final int SECONDS_IN_DAY = SECONDS_IN_HOUR * 24;

    public static String toFormattedDuration(Duration duration) {
        if (duration == null) {
            return "[undefined]";
        }

        Duration originalDuration = duration;
        StringBuilder builder = new StringBuilder();
        duration = appendIfGreaterThanZero(builder, duration, SECONDS_IN_DAY, "d");
        duration = appendIfGreaterThanZero(builder, duration, SECONDS_IN_HOUR, "h");
        duration = appendIfGreaterThanZero(builder, duration, SECONDS_IN_MINUTE, "m");

        if (originalDuration.getSeconds() < 60 * 60) {
            appendIfGreaterThanZero(builder, duration, 1, "s");
        }
        return builder.toString();
    }

    private static Duration appendIfGreaterThanZero(StringBuilder builder, Duration duration, int secondsInUnit, String unitSuffix) {
        long seconds = duration.getSeconds();
        long value = seconds / secondsInUnit;
        if (value > 0) {
            if (builder.length() > 0) {
                builder.append(" ");
            }
            builder.append(value).append(unitSuffix);
            return duration.minusSeconds(value * secondsInUnit);
        }
        return duration;
    }

}
