package org.dreamscale.time;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class LocalDateTimeService implements TimeService {

	@Override
	public LocalDateTime now() {
		return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
	}

}
