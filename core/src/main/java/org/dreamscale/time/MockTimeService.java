package org.dreamscale.time;

import java.time.LocalDateTime;

public class MockTimeService implements TimeService {

    private LocalDateTime now;

    public MockTimeService() {
        now = LocalDateTime.of(2016, 1, 1, 0, 0);
    }

    @Override
    public LocalDateTime now() {
        return now;
    }

    public LocalDateTime daysInFuture(int days) {
        return now.plusDays(days);
    }

    public LocalDateTime hoursInFuture(int hours) {
        return now.plusHours(hours);
    }

    public LocalDateTime minutesInFuture(int minutes) {
        return now.plusMinutes(minutes);
    }

    public LocalDateTime secondsInFuture(int seconds) {
        return now.plusSeconds(seconds);
    }

    public MockTimeService plusHour() {
        plusHours(1);
        return this;
    }

    public MockTimeService plusDays(int days) {
        now = now.plusDays(days);
        return this;
    }

    public MockTimeService plusHours(int hours) {
        now = now.plusHours(hours);
        return this;
    }

    public MockTimeService plusMinutes(int minutes) {
        now = now.plusMinutes(minutes);
        return this;
    }

    public MockTimeService plusSeconds(int seconds) {
        now = now.plusSeconds(seconds);
        return this;
    }

    public MockTimeService advanceTime(int hours, int minutes, int seconds) {
        return plusHours(hours)
                .plusMinutes(minutes)
                .plusSeconds(seconds);
    }

}
