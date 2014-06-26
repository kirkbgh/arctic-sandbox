package com.scratch.activiti.test;

import java.util.List;

import org.activiti.engine.impl.cmd.AcquireJobsCmd;
import org.activiti.spring.SpringJobExecutor;
import org.springframework.core.task.TaskExecutor;

public class TestJobExecutor extends SpringJobExecutor {


	public TestJobExecutor() {
		super();
	}

	public TestJobExecutor(final TaskExecutor taskExecutor) {
		super(taskExecutor);
	}

	@Override
	public void start() {
		ensureInitialization();

		super.start();
	}

	@Override
	protected void ensureInitialization() {
		acquireJobsCmd = new AcquireJobsCmd(this);
		acquireJobsRunnable = new TestAquireJobsRunnable(this);
	}

	public void doExecuteJobs(final List<String> jobIds) {
		executeJobs(jobIds);
	}
}
