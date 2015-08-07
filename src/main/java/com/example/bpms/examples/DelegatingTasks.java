package com.example.bpms.examples;

import java.util.HashMap;
import java.util.Map;

import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.runtime.manager.context.EmptyContext;
import org.kie.internal.runtime.manager.context.ProcessInstanceIdContext;

import com.example.bpms.EnvironmentManager;
import com.example.bpms.SupplyItem;

/**
 * This example shows the how to delegate a task.
 * 
 * 
 * @author Frank
 *
 */
public class DelegatingTasks {
	
	public static void main(String[] args) {
		
		EnvironmentManager simpleEnv = EnvironmentManager.get();
		
		RuntimeManager manager = simpleEnv.start("com/example/bpms/simplesupplyitemapproval.bpmn2");
		
		try{
			new DelegatingTasks().startSimulation(manager);
		}finally{
			simpleEnv.stop();
		}
		System.out.println("done.");
	}
	
	public void startSimulation(final RuntimeManager manager){
		
		RuntimeEngine runtimeEngine = manager.getRuntimeEngine(EmptyContext.get());
		
		SupplyItem item1 = new SupplyItem();
		item1.setDescription("Red Shoes");
		SupplyItem item2 = new SupplyItem();
		item2.setDescription("Red Hat");
		
		
		//Start a process for the two items
		startSimpleSupplyItemApproval(item1, manager);
		startSimpleSupplyItemApproval(item2, manager);
		
		TaskService taskService = runtimeEngine.getTaskService();
		
        // Simulate a user claiming a task
		TaskSummary ts = taskService.getTasksAssignedAsPotentialOwner("bob", "en-UK").get(0);
		taskService.claim(ts.getId(), "bob");
		
		//Bob has claimed a task but then left for a two week vacation. Debbie now
		//delegates his task to John.
		for(TaskSummary summary : taskService.getTasksAssignedAsBusinessAdministrator("Administrators", "en-UK")){
			Status taskStatus = summary.getStatus();
			switch(taskStatus){
				case Reserved:
					taskService.delegate(summary.getId(), "debbie", "john");
					System.out.println("Debbie delegated task " + summary.getId() + " to John");
					break;
				default:
					break;
			}
		}
		
		//Now john has a task assigned explicitly to him so he is THE owner
		for(TaskSummary summary : taskService.getTasksOwned("john", "en-UK")){
			System.out.println("John is the owner of task " + summary.getId());
		}
		
		//john is also the potential owner of both tasks
		for(TaskSummary summary : taskService.getTasksAssignedAsPotentialOwner("john", "en-UK")){
			System.out.println("John is the potential owner of task " + summary.getId());
		}

        manager.disposeRuntimeEngine(runtimeEngine);
	}
	
	public ProcessInstance startSimpleSupplyItemApproval(SupplyItem item, RuntimeManager manager) {
		RuntimeEngine runtimeEngine = manager.getRuntimeEngine(ProcessInstanceIdContext.get());
		KieSession ksession = runtimeEngine.getKieSession();
		Map<String, Object> params = new HashMap<>();
		params.put("supplyItem", item);
		ProcessInstance proc = ksession.startProcess("com.example.bpms.simplesupplyitemapproval", params);

		System.out.println("Started process:" + proc);

		manager.disposeRuntimeEngine(runtimeEngine);

		return proc;
	}
	
	
}
