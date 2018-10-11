package org.dreamscale.time

import org.dreamscale.time.TimeConverter
import spock.lang.Specification

import java.time.Duration


class TimeConverterSpec extends Specification {

    def "should format duration"() {
        given:
        Duration duration = Duration.ofDays(2).plusHours(3).plusMinutes(4).plusSeconds(5);

        expect:
        TimeConverter.toFormattedDuration(duration) == "2d 3h 4m"
    }
    
    def "should not print units if duration is zero"() {
        given:
        Duration duration = Duration.ofMinutes(2).plusSeconds(3)

        expect:
        TimeConverter.toFormattedDuration(duration) == "2m 3s"
    }

}
