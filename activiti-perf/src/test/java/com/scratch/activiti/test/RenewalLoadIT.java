package com.scratch.activiti.test;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import org.activiti.engine.RuntimeService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:activiti-spring.cfg.xml")
//@TransactionConfiguration
public class RenewalLoadIT extends AbstractTransactionalJUnit4SpringContextTests {
	private static final int ACTIVE_ID_START = 110000000;
	private static final int WAITING_ID_START = 0;

	@Inject
	private RuntimeService runtimeService;
	@Inject
	private ProcessLoader loader;
	@Inject
	private ProcessRunner runner;

	@Test
// TODO: fixme, threads and spring tx don't work well since tx are held in Threadlocals and the insertion process in the loader runs on multiple threads.
//	@Transactional
	public void insertProcesses_currentStartDate() throws Exception {
		final int numActiveInstances = 10;
		final int idStart = ACTIVE_ID_START;
		final int idEnd = idStart + numActiveInstances;

		final int insertionTotal = this.loader.load(numActiveInstances, idStart, "renewalProcess.bpmn", "renewal.bpmn");
		assertEquals(idEnd - idStart, insertionTotal);
	}

	@Test
	public void insertProcesses_startDateOneYearInFuture() throws Exception {
		final int numWaitingInstances = 10000000;
		final int idStart = WAITING_ID_START;
		final int idEnd = idStart + numWaitingInstances;

		final int insertionTotal = this.loader.load(numWaitingInstances, idStart, "renewalProcess.bpmn", "renewal.bpmn");
		assertEquals(idEnd - idStart, insertionTotal);
	}

	@Test
	public void startExecutingAllActiveProcesses()
			throws Exception {
		this.runner.executeAllActiveProcesses("renewalProcess.bpmn", "renewal.bpmn");

		final long activeCount = this.runtimeService.createProcessInstanceQuery().active().count();
		System.err.printf("active %s \n", activeCount);

		final long suspendedCount = this.runtimeService.createProcessInstanceQuery().suspended().count();
		System.err.printf("suspended %s \n", suspendedCount);

		final long totalCount = this.runtimeService.createProcessInstanceQuery().count();
		System.err.printf("total %s \n", totalCount);
	}

/*	@Test
	public void startProcessInstancesWithSpecifiedTime()
			throws Exception {

		final int numRenewals = 1000;

		final DateTime now = new DateTime();
		final String jobUpdate = String.format("UPDATE act_ru_job SET DUEDATE_ = '%s' WHERE ID_ IN (SELECT ID_ FROM (SELECT ID_ FROM act_ru_job WHERE PROCESS_INSTANCE_ID_ IN (SELECT ID_ FROM act_hi_procinst WHERE CONVERT(BUSINESS_KEY_,UNSIGNED INTEGER) <= %s)) AS X)", this.jobDateTimeFormatter.print(now), numRenewals);
		final String varUpdate = String.format("UPDATE act_ru_variable set TEXT_ = '%s' WHERE NAME_ = 'renewalDate' AND proc_inst_id_ IN (SELECT ID_ FROM act_hi_procinst WHERE CONVERT(BUSINESS_KEY_,UNSIGNED INTEGER) <= %s)", this.varDateTimeFormatter.print(now), numRenewals);

		final SqlSession session = this.sessionFactory.openSession();
		final int updatedJobs = session.update(jobUpdate);
		assertEquals(numRenewals, updatedJobs);
		final int updatedVars = session.update(varUpdate);
		assertEquals(numRenewals, updatedVars);

		this.engineConfig.getJobExecutor().start();

		while (true) {
			Thread.sleep(1000);
		}
	}
	private void deleteAllActiveProcessInstances() {
		final List<ProcessInstance> active = this.runtimeService
				.createProcessInstanceQuery().list();
		for (final ProcessInstance processInstance : active) {
			this.runtimeService.deleteProcessInstance(processInstance.getId(),
					"test reason");
		}

		assertEquals(0, this.runtimeService.createProcessInstanceQuery()
				.count());
	}
*/
}
