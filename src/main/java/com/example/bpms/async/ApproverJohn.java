package com.example.bpms.async;

import java.util.List;
import java.util.Map;

import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.runtime.manager.context.EmptyContext;

import com.example.bpms.SupplyItem;

/**
 * Runnable that simulates a user who can approve tasks.
 * 
 * @author Frank
 *
 */
public class ApproverJohn implements Runnable{

	private RuntimeManager manager;
	
	public ApproverJohn(RuntimeManager manager){
		this.manager = manager;
	}
	
	@Override
	public void run() {
		
		RuntimeEngine runtimeEngine = manager.getRuntimeEngine(EmptyContext.get());
		TaskService taskService = runtimeEngine.getTaskService();
		
        // John always rejects Red Hats
        {
            List<TaskSummary> list = taskService.getTasksAssignedAsPotentialOwner("john", "en-UK");
            for (TaskSummary taskSummary : list) {
            	try{
            		taskService.claim(taskSummary.getId(), "john");
	                System.out.println("john claims a task : taskId = " + taskSummary.getId());
            	}catch(Throwable ex){
            		System.out.println("john could not claim task : taskId = " + taskSummary.getId());
            		continue;
            	}
            	taskService.start(taskSummary.getId(), "john");
                System.out.println("john starts a task : taskId = " + taskSummary.getId());
                Map<String,Object> content = taskService.getTaskContent(taskSummary.getId());
                SupplyItem taskItem = (SupplyItem) content.get("supplyItemInput");
                if(taskItem.getDescription().equals("Red Hat")){
                	content.put("approved", false);
                }else{
                	content.put("approved", true);
                }
                taskService.complete(taskSummary.getId(), "john", content);
            }
        }
        
        manager.disposeRuntimeEngine(runtimeEngine);
        
	}
	
	public static boolean isCause(Class<? extends Throwable> expected, Throwable exc) {
		return expected.isInstance(exc) || (exc != null && isCause(expected, exc.getCause()));
	}

}
