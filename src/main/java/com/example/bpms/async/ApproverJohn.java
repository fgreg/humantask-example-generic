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
public class ApproverJohn implements Runnable{

	private RuntimeManager manager;
	private boolean silence;
	
	public ApproverJohn(RuntimeManager manager){
		this(manager, false);
	}
	
	public ApproverJohn(RuntimeManager manager, boolean silence){
		this.manager = manager;
		this.silence = silence;
	}
	
	@Override
	public void run() {
		
		RuntimeEngine runtimeEngine = manager.getRuntimeEngine(ProcessInstanceIdContext.get());
		TaskService taskService = runtimeEngine.getTaskService();
		
        // John always rejects Red Hats
        {
            List<TaskSummary> list = taskService.getTasksAssignedAsPotentialOwner("john", "en-UK");
            for (TaskSummary taskSummary : list) {
            	try{
            		taskService.claim(taskSummary.getId(), "john");
	                print("john claims a task : taskId = " + taskSummary.getId());
            	}catch(Throwable ex){
            		print("john could not claim task : taskId = " + taskSummary.getId());
            		continue;
            	}
            	taskService.start(taskSummary.getId(), "john");
            	print("john starts a task : taskId = " + taskSummary.getId());
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
	
	private void print(String message){
		if(!silence){
			System.out.println(message);
		}
	}
	
	public static boolean isCause(Class<? extends Throwable> expected, Throwable exc) {
		return expected.isInstance(exc) || (exc != null && isCause(expected, exc.getCause()));
	}

}
