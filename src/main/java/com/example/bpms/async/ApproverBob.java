package com.example.bpms.async;

import java.util.List;
import java.util.Map;

import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.runtime.manager.context.ProcessInstanceIdContext;

import com.example.bpms.SupplyItem;

/**
 * Runnable that simulates a user who can approve tasks.
 * 
 * @author Frank
 *
 */
public class ApproverBob implements Runnable{

	private RuntimeManager manager;
	private boolean silence;
	
	public ApproverBob(RuntimeManager manager){
		this(manager, false);
	}
	
	public ApproverBob(RuntimeManager manager, boolean silence){
		this.manager = manager;
		this.silence = silence;
	}
	
	@Override
	public void run() {
		
		RuntimeEngine runtimeEngine = manager.getRuntimeEngine(ProcessInstanceIdContext.get());
		TaskService taskService = runtimeEngine.getTaskService();
		
        // Bob always approves Red Hats
        {
            List<TaskSummary> list = taskService.getTasksAssignedAsPotentialOwner("bob", "en-UK");
            for (TaskSummary taskSummary : list) {
                try{
                	taskService.claim(taskSummary.getId(), "bob");
	                print("bob claims a task : taskId = " + taskSummary.getId());
            	}catch(Throwable ex){
            		print("bob could not claim task : taskId = " + taskSummary.getId());
            		continue;
            	}
                taskService.start(taskSummary.getId(), "bob");
                print("bob starts a task : taskId = " + taskSummary.getId());
                Map<String,Object> content = taskService.getTaskContent(taskSummary.getId());
                SupplyItem taskItem = (SupplyItem) content.get("supplyItemInput");
                if(taskItem.getDescription().equals("Red Hat")){
                	content.put("approved", true);
                }else{
                	content.put("approved", false);
                }
                taskService.complete(taskSummary.getId(), "bob", content);
            }
        }
        
        manager.disposeRuntimeEngine(runtimeEngine);
        
	}
	
	private void print(String message){
		if(!silence){
			System.out.println(message);
		}
	}
	
	public static boolean isCause(Class<? extends Throwable> expected, Throwable exc) {
		return expected.isInstance(exc) || (exc != null && isCause(expected, exc.getCause()));
	}

}
