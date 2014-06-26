package com.scratch.activiti.renewal;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("renewalDateCalculator")
public class RenewalDateCalculator implements JavaDelegate {
	private static final long DEFAULT_DURATION = 1000l;

	private static final Logger LOGGER = LoggerFactory
			.getLogger(RenewalDateCalculator.class);

	public static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = ISODateTimeFormat
			.dateTime().withZone(DateTimeZone.forOffsetHours(-6));

	@Override
	public void execute(final DelegateExecution execution) throws Exception {
		Long duration = (Long) execution.getVariable("duration");
		if (duration == null) {
			duration = DEFAULT_DURATION * 60l; // 1 minute default
			//throw new IllegalArgumentException("A duration must be specified");
		}
		final String isoNextRenewalDate = ISO_DATE_TIME_FORMATTER
				.print(new DateTime().plus(duration));

		execution.setVariable("renewalDate", isoNextRenewalDate);
		LOGGER.debug("Calculated next renewalDate to {}", isoNextRenewalDate);
	}
}
