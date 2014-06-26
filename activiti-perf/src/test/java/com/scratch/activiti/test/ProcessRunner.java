package com.scratch.activiti.test;

import java.io.IOException;

import javax.inject.Inject;

import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.repository.DeploymentBuilder;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class ProcessRunner {

	private static Logger LOGGER = LoggerFactory.getLogger(ProcessRunner.class);

	private final ProcessEngineConfiguration engineConfig;
	private final RuntimeService runtimeService;

	private final RepositoryService repositoryService;

	@Inject
	public ProcessRunner(final RepositoryService repositoryService, final RuntimeService runtimeService, final ProcessEngineConfiguration engineConfig) {
		this.repositoryService = repositoryService;
		this.runtimeService = runtimeService;
		this.engineConfig = engineConfig;
	}

	@Test
	public void executeAllActiveProcesses(final String...bpmnFileNames)
			throws Exception {
		DeploymentBuilder deployment = this.repositoryService.createDeployment();
		for (final String bpmnFileName : bpmnFileNames) {
			deployment = deployment.addClasspathResource(bpmnFileName);
		}
		deployment.deploy();

		this.engineConfig.getJobExecutor().start();

		final long activeCount = this.runtimeService.createProcessInstanceQuery().active().count();
		LOGGER.info("active {} \n", activeCount);

		waitForProcessInstanceCompletion(new DateTime(), activeCount);


		final long suspendedCount = this.runtimeService.createProcessInstanceQuery().suspended().count();
		LOGGER.info("suspended {} \n", suspendedCount);

		final long totalCount = this.runtimeService.createProcessInstanceQuery().count();
		LOGGER.info("total {} \n", totalCount);
	}

	private void waitForProcessInstanceCompletion(final DateTime startDateTime,
			final long totalActiveCount) throws InterruptedException, IOException {
		while (this.runtimeService.createProcessInstanceQuery().list().size() > 0) {
			Thread.sleep(100);
		}

		final long totalProcessingTime = ProcessLoader.calcTimeDelta(startDateTime
				.getMillis());
		logProcessingTimes(totalActiveCount, totalProcessingTime);
	}

	private void logProcessingTimes(final long totalActiveCount,
			final long totalProcessingTime) throws IOException {
		System.err.printf(
				"Took %s to complete %s processes.\n",
				totalProcessingTime, totalActiveCount);
//		final PrintWriter writer = new PrintWriter(new FileWriter(new File(
//				"/junk/actiti_times.csv"), true));
//		try {
//			writer.printf("%s, %s\n", totalProcessingTime, totalActiveCount);
//		} finally {
//			writer.close();
//		}
	}

	public static void main(final String[] args) throws Exception {
		if (args.length < 2) {
			System.err
					.printf("USAGE: java %s <activitiDbName> <bpmnFile1,bpmnfile2,...,bpmnFileN>.\n", ProcessLoader.class.getName());
			System.exit(1);
		}

		final String dbName = args[0];
		// Hackity, hack, hack :(
		System.setProperty("databaseName", dbName);

		final String[] bpmnFiles = StringUtils.split(args[1], ",");

		@SuppressWarnings("resource")
		final ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"/activiti-spring.cfg.xml");
		final ProcessRunner loader = applicationContext
				.getBean(ProcessRunner.class);
		loader.executeAllActiveProcesses(bpmnFiles);
	}

}
