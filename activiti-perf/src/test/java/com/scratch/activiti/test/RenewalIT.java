package com.scratch.activiti.test;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.test.ActivitiRule;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:activiti-spring.cfg.xml")
public class RenewalIT {
	
	@Inject
	private RuntimeService runtimeService;
	@Inject
	private TaskService taskService;
	@Inject
	private RepositoryService repositoryService;
	
	@Inject
	@SuppressWarnings("unused")
	private ActivitiRule rule;
	
	@Before
	public void before() {
		this.repositoryService.createDeployment()
			.addClasspathResource("renewalProcess.bpmn")
			.addClasspathResource("renewal.bpmn").deploy();
	}
	
	@Test
	public void happyPath() throws InterruptedException {
		final Map<String, Object> variableMap = new HashMap<String, Object>();
		
		variableMap.put("count", 0);
		variableMap.put("breakoutCount", 5);
		variableMap.put("continue", true);
		variableMap.put("subId", "My First Sub");
		variableMap.put("renewalDate", new DateTime().toString());
		
		final String uid = UUID.randomUUID().toString();
		System.err.println("Starting process...");
		final ProcessInstance processInstance = this.runtimeService.startProcessInstanceByKey("subRenewalProcess", uid, variableMap);
		
		while (this.runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult() != null) {
			Thread.sleep(100);
		}
		
		assertEquals(0, this.taskService.createTaskQuery().list().size());
	}
}
