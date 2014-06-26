package com.scratch.activiti.test;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.repository.DeploymentBuilder;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

import com.google.common.primitives.Ints;
import com.scratch.activiti.renewal.RenewalDateCalculator;

@Component
public class ProcessLoader {

	private static Logger LOGGER = LoggerFactory.getLogger(RenewalLoadIT.class);

	private static final int MAX_PROCESSES_PER_BATCH = 1000;

	private final RuntimeService runtimeService;
	private final RepositoryService repositoryService;

	@Inject
	public ProcessLoader(final RuntimeService runtimeService, final RepositoryService repositoryService) {
		this.runtimeService = runtimeService;
		this.repositoryService = repositoryService;
	}

	public int load(final int numActiveInstances, final int idStart, final String...bpmnFileNames) throws Exception {
		DeploymentBuilder deployment = this.repositoryService.createDeployment();
		for (final String bpmnFileName : bpmnFileNames) {
			deployment = deployment.addClasspathResource(bpmnFileName);
		}
		deployment.deploy();

		final String endDate = RenewalDateCalculator.ISO_DATE_TIME_FORMATTER
				.print(new DateTime());

		final int idEnd = idStart + (numActiveInstances - 1);
		final int insertionTotal = startProcessInstances(idStart, idEnd,
				new DateTime(endDate));
		return insertionTotal;
	}

	private int startProcessInstances(final int rangeStart, final int rangeEnd,
			final DateTime activationStartDateTime) throws Exception {
		final String activationDateString = activationStartDateTime.toString();

		final long startSubmitTime = System.currentTimeMillis();
		final ExecutorService executorService = Executors
				.newFixedThreadPool(20);
		final ExecutorCompletionService<Boolean> completionService = new ExecutorCompletionService<Boolean>(
				executorService);

		final AtomicInteger currentId = new AtomicInteger(rangeStart);

		int totalInserted = 0;
		do {
			totalInserted += insertProcesses(currentId, rangeEnd,
					MAX_PROCESSES_PER_BATCH, activationDateString,
					completionService);
			LOGGER.info("Inserted {} process instances...", totalInserted);
		} while (currentId.get() <= rangeEnd);

		final long totalSubmitTime = calcTimeDelta(startSubmitTime);
		LOGGER.info("Took {} to submit {} processes.", totalSubmitTime,
				totalInserted);

		return totalInserted;
	}

	private int insertProcesses(final AtomicInteger currentId, final int maxId,
			final int maxInsertionCount, final String activationDateString,
			final ExecutorCompletionService<Boolean> completionService)
			throws InterruptedException, ExecutionException {

		int insertedTaskCount = 0;
		do {

			final int subId = currentId.incrementAndGet();
			final String businessKey = Integer.toString(subId);

			completionService.submit(new Callable<Boolean>() {
				@Override
				public Boolean call() {
					final Map<String, Object> variableMap = new HashMap<String, Object>();

					variableMap.put("subId", subId);
					variableMap.put("count", 0);
					variableMap.put("breakoutCount", maxId);
					variableMap.put("continue", true);
					variableMap.put("renewalDate", activationDateString);

					ProcessLoader.this.runtimeService
							.startProcessInstanceByKey("subRenewalProcess",
									businessKey, variableMap);

					return true;
				}
			});

			insertedTaskCount++;
		} while (currentId.get() < maxId
				&& insertedTaskCount < maxInsertionCount);

		int completed = 0;
		while (completed < insertedTaskCount) {
			final Future<Boolean> future = completionService.take();
			assertTrue(future.get()); // will throw an exception
			++completed;
		}

		return completed;
	}

	static long calcTimeDelta(final long start) {
		return System.currentTimeMillis() - start;
	}

	public static void main(final String[] args) throws Exception {
		if (args.length < 4) {
			usage();
		}

		final String dbName = args[0];
		final Integer numActiveInstances = Ints.tryParse(args[1]);
		final Integer idStart = Ints.tryParse(args[2]);
		final String[] bpmnFiles = StringUtils.split(args[3], ",");

		if (StringUtils.isBlank(dbName) || numActiveInstances == null || idStart == null || bpmnFiles.length < 1) {
			usage();
		}

		// Hackity, hack, hack :(
		System.setProperty("databaseName", dbName);

		@SuppressWarnings("resource")
		final ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"/activiti-spring.cfg.xml"); // TODO: move to arg
		final ProcessLoader loader = applicationContext
				.getBean(ProcessLoader.class);
		loader.load(numActiveInstances, idStart, bpmnFiles);
	}

	private static void usage() {
		System.err
				.printf("USAGE: java %s <numActiveInstances> <idStart> <bpmnFile1,bpmnfile2,...,bpmnFileN>.\n", ProcessLoader.class.getName());
		System.exit(1);
	}

}
