package com.scratch.activiti.test;

import java.util.Date;
import java.util.List;

import org.activiti.engine.ActivitiOptimisticLockingException;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.activiti.engine.impl.jobexecutor.AcquireJobsRunnable;
import org.activiti.engine.impl.jobexecutor.AcquiredJobs;
import org.activiti.engine.impl.jobexecutor.GetUnlockedTimersByDuedateCmd;
import org.activiti.engine.impl.jobexecutor.JobExecutor;
import org.activiti.engine.impl.persistence.entity.TimerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestAquireJobsRunnable extends AcquireJobsRunnable {
	private static final Logger LOG = LoggerFactory
			.getLogger(TestAquireJobsRunnable.class);

	public TestAquireJobsRunnable(final JobExecutor jobExecutor) {
		super(jobExecutor);
	}

	@Override
	public synchronized void run() {
		doRun();
	}

	public void doRun() {

		LOG.info("{} starting to acquire jobs", this.jobExecutor.getName());

		final CommandExecutor commandExecutor = this.jobExecutor
				.getCommandExecutor();

		while (!this.isInterrupted) {
			try {
				final long start = System.currentTimeMillis();
				final int maxJobsPerAcquisition = this.jobExecutor
						.getMaxJobsPerAcquisition();

				final AcquiredJobs acquiredJobs = commandExecutor
						.execute(this.jobExecutor.getAcquireJobsCmd());

				final long elapsedTime = (System.currentTimeMillis() - start);

				int totalJobs = 0;
				for (final List<String> jobIds : acquiredJobs.getJobIdBatches()) {
					((TestJobExecutor) this.jobExecutor).doExecuteJobs(jobIds);
					totalJobs += jobIds.size();
				}

				System.err.printf("Took %s ms to aquire %s job(s).\n",
						elapsedTime, totalJobs);

				// if all jobs were executed
				this.millisToWait = this.jobExecutor.getWaitTimeInMillis();
				final int jobsAcquired = acquiredJobs.getJobIdBatches().size();
				if (jobsAcquired < maxJobsPerAcquisition) {

					this.isJobAdded = false;

					// check if the next timer should fire before the normal
					// sleep time is over
					final Date duedate = new Date(this.jobExecutor.getCurrentTime()
							.getTime() + this.millisToWait);
					final List<TimerEntity> nextTimers = commandExecutor
							.execute(new GetUnlockedTimersByDuedateCmd(duedate,
									new Page(0, 1)));

					if (!nextTimers.isEmpty()) {
						final long millisTillNextTimer = nextTimers.get(0)
								.getDuedate().getTime()
								- this.jobExecutor.getCurrentTime().getTime();
						if (millisTillNextTimer < this.millisToWait) {
							this.millisToWait = millisTillNextTimer;
						}
					}

				} else {
					this.millisToWait = 0;
				}
			} catch (final ActivitiOptimisticLockingException optimisticLockingException) {
				// See http://jira.codehaus.org/browse/ACT-1390
				if (LOG.isDebugEnabled()) {
					LOG.debug(
							"Optimistic locking exception during job acquisition. If you have multiple job executors running against the same database, "
									+ "this exception means that this thread tried to acquire a job, which already was acquired by another job executor acquisition thread."
									+ "This is expected behavior in a clustered environment. "
									+ "You can ignore this message if you indeed have multiple job executor acquisition threads running against the same database. "
									+ "Exception message: {}",
							optimisticLockingException.getMessage());
				}
			} catch (final Throwable e) {
				LOG.error("exception during job acquisition: {}",
						e.getMessage(), e);
				this.millisToWait *= this.waitIncreaseFactor;
				if (this.millisToWait > this.maxWait) {
					this.millisToWait = this.maxWait;
				} else if (this.millisToWait == 0) {
					this.millisToWait = this.jobExecutor.getWaitTimeInMillis();
				}
			}

			if ((this.millisToWait > 0) && (!this.isJobAdded)) {
				try {
					if (LOG.isDebugEnabled()) {
						LOG.debug(
								"job acquisition thread sleeping for {} millis",
								this.millisToWait);
					}
					synchronized (this.MONITOR) {
						if (!this.isInterrupted) {
							this.isWaiting.set(true);
							this.MONITOR.wait(this.millisToWait);
						}
					}

					if (LOG.isDebugEnabled()) {
						LOG.debug("job acquisition thread woke up");
					}
				} catch (final InterruptedException e) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("job acquisition wait interrupted");
					}
				} finally {
					this.isWaiting.set(false);
				}
			}
		}

		LOG.info("{} stopped job acquisition", this.jobExecutor.getName());
	}

}
