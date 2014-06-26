package com.scratch.activiti.renewal;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.apache.commons.lang.math.NumberUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("renewalService")
public class RenewalService implements JavaDelegate {
	private static Logger LOGGER = LoggerFactory
			.getLogger(RenewalService.class);

	@Override
	public void execute(final DelegateExecution execution) throws Exception {
		final String subId = String.valueOf(execution.getVariable("subId"));

		LOGGER.info("Executing renewal for sub {} @ {}. \n", subId,
				new DateTime());

		// TODO: Ick Need some utils for converting process variables to correct types...
		final int count = NumberUtils.toInt(String.valueOf(execution.getVariable("count"))) + 1;
		execution.setVariable("count", count);
	}
}
