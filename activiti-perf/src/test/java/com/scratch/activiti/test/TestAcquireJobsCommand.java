package com.scratch.activiti.test;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.cmd.AcquireJobsCmd;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.jobexecutor.AcquiredJobs;
import org.activiti.engine.impl.jobexecutor.JobExecutor;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestAcquireJobsCommand extends AcquireJobsCmd {
	private static final Logger LOG = LoggerFactory
			.getLogger(TestAquireJobsRunnable.class);

	private final JobExecutor jobExecutor;

	private FileWriter fileWriter;

	public TestAcquireJobsCommand(final JobExecutor jobExecutor) {
		super(jobExecutor);

		this.jobExecutor = jobExecutor;
	}

	@Override
	public AcquiredJobs execute(final CommandContext commandContext) {
		final long start = System.currentTimeMillis();

		final String lockOwner = this.jobExecutor.getLockOwner();
		final int lockTimeInMillis = this.jobExecutor.getLockTimeInMillis();
		final int maxNonExclusiveJobsPerAcquisition = this.jobExecutor
				.getMaxJobsPerAcquisition();

		final AcquiredJobs acquiredJobs = new AcquiredJobs();
		final List<JobEntity> jobs = commandContext.getJobEntityManager()
				.findNextJobsToExecute(
						new Page(0, maxNonExclusiveJobsPerAcquisition));

		for (final JobEntity job : jobs) {
			final List<String> jobIds = new ArrayList<String>();
			if (job != null && !acquiredJobs.contains(job.getId())) {
				if (job.isExclusive() && job.getProcessInstanceId() != null) {
					// acquire all exclusive jobs in the same process instance
					// (includes the current job)
					final List<JobEntity> exclusiveJobs = commandContext
							.getJobEntityManager().findExclusiveJobsToExecute(
									job.getProcessInstanceId());
					for (final JobEntity exclusiveJob : exclusiveJobs) {
						if (exclusiveJob != null) {
							lockJob(exclusiveJob, lockOwner, lockTimeInMillis);
							jobIds.add(exclusiveJob.getId());
						}
					}
				} else {
					lockJob(job, lockOwner, lockTimeInMillis);
					jobIds.add(job.getId());
				}

			}

			acquiredJobs.addJobIdBatch(jobIds);
		}

		final long end = System.currentTimeMillis() - start;
		try {
			getWriter().write(String.format("%s, %s", end, jobs.size()));
		} catch (final IOException e) {
			e.printStackTrace();
		}

		return acquiredJobs;
	}

	public FileWriter getWriter() {
		if (this.fileWriter != null) {
			return this.fileWriter;
		}

		try {
			final String databaseName = System.getProperty("databaseName");
			this.fileWriter = new FileWriter(
					String.format("/dev/junk/jobAcquisitionTimes-%s-%s.log", databaseName, System.currentTimeMillis()));
		} catch (final IOException e) {
			LOG.error(e.getMessage(), e);
			return null;
		}

		return this.fileWriter;
	}
}
