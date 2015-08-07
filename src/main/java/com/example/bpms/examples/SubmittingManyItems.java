package com.example.bpms.examples;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.runtime.manager.context.CorrelationKeyContext;
import org.kie.internal.runtime.manager.context.EmptyContext;

import com.example.bpms.EnvironmentManager;
import com.example.bpms.SupplyItem;

/**
 * This example is submitting a very large number of processes. The performance in terms of time from start
 * to finish is not representative of a production system as we are relying on H2. However, what this shows
 * is that a single JVM is capable of submitting thousands of processes and completing thousands of
 * tasks.
 * 
 * @author Frank
 *
 */
public class SubmittingManyItems {
	
	public static void main(String[] args) {
		
		EnvironmentManager simpleEnv = EnvironmentManager.get();
		
		RuntimeManager manager = simpleEnv.start("com/example/bpms/simplesupplyitemapproval.bpmn2");
		
		try{
			new SubmittingManyItems().startSimulation(manager);
		}finally{
			simpleEnv.stop();
		}
		System.out.println("done.");
	}
	
	public void startSimulation(final RuntimeManager manager){
		
		RuntimeEngine runtimeEngine = manager.getRuntimeEngine(EmptyContext.get());
		
		
		//Submit 10000 items for processing serially
		for(int x = 0; x < 10; x++){
			SupplyItem item = new SupplyItem();
			String description = x%2==0?"Red Hat " + x:"Red Shoes " + x;
			item.setDescription(description);
			
			startSimpleSupplyItemApproval(item, manager);
		}
		
		TaskService taskService = runtimeEngine.getTaskService();
		
        // Simulate a user approving one item and rejecting the other
        {
            List<TaskSummary> list = taskService.getTasksAssignedAsPotentialOwner("bob", "en-UK");
            for (TaskSummary taskSummary : list) {
                taskService.start(taskSummary.getId(), "bob");
                Map<String,Object> content = taskService.getTaskContent(taskSummary.getId());
                SupplyItem taskItem = (SupplyItem) content.get("supplyItemInput");
                if(taskItem.getDescription().contains("Red Hat")){
                	content.put("approved", true);
                }else{
                	content.put("approved", false);
                }
                taskService.complete(taskSummary.getId(), "bob", content);
            }
        }

        // -----------
        manager.disposeRuntimeEngine(runtimeEngine);
	}
	
	public ProcessInstance startSimpleSupplyItemApproval(SupplyItem item, RuntimeManager manager) {
		RuntimeEngine runtimeEngine = manager.getRuntimeEngine(CorrelationKeyContext.get());
		KieSession ksession = runtimeEngine.getKieSession();
		Map<String, Object> params = new HashMap<>();
		params.put("supplyItem", item);
		ProcessInstance proc = ksession.startProcess("com.example.bpms.simplesupplyitemapproval", params);

		manager.disposeRuntimeEngine(runtimeEngine);

		return proc;
	}
	
	
}
