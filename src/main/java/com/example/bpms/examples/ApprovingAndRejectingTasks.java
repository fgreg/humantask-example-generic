package com.example.bpms.examples;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbpm.services.task.utils.ContentMarshallerHelper;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Content;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.runtime.manager.context.EmptyContext;
import org.kie.internal.runtime.manager.context.ProcessInstanceIdContext;

import com.example.bpms.EnvironmentManager;
import com.example.bpms.SupplyItem;

/**
 * This example shows the basics of working with the task service.
 * 
 * I can start a task, do some work based on the content of the task, and then complete it.
 * 
 * @author Frank
 *
 */
public class ApprovingAndRejectingTasks {
	
	public static void main(String[] args) {
		
		EnvironmentManager simpleEnv = EnvironmentManager.get();
		
		RuntimeManager manager = simpleEnv.start("com/example/bpms/simplesupplyitemapproval.bpmn2");
		
		try{
			new ApprovingAndRejectingTasks().startSimulation(manager);
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
		
        // Simulate a user approving one item and rejecting the other
        {
            List<TaskSummary> list = taskService.getTasksAssignedAsPotentialOwner("bob", "en-UK");
            for (TaskSummary taskSummary : list) {
                System.out.println("bob starts a task : taskId = " + taskSummary.getId());
                taskService.start(taskSummary.getId(), "bob");
                System.out.println(taskService.getTaskById(taskSummary.getId()).getPeopleAssignments().getPotentialOwners());
                Map<String,Object> content = taskService.getTaskContent(taskSummary.getId());
                System.out.println(content);
                SupplyItem taskItem = (SupplyItem) content.get("supplyItemInput");
                
                
                if(taskItem.getDescription().equals("Red Hat")){
                	content.put("approved", true);
                }else{
                	content.put("approved", false);
                }
                taskService.complete(taskSummary.getId(), "bob", content);
            }
        }
        
        //Able to get basic information about who completed the task
        for (TaskSummary taskSummary : taskService.getTasksAssignedAsPotentialOwnerByStatus("bob", Arrays.asList(Status.Completed), "en-UK")){
        	System.out.println("Completed task: " + taskSummary + "\n\t" 
        			+ taskSummary.getName() + "\n\t" 
        			+ taskSummary.getActualOwner() + "\n\t" 
        			+ taskSummary.getActivationTime()
        			);
        }

        // -----------
        manager.disposeRuntimeEngine(runtimeEngine);
	}
	
	@SuppressWarnings("unchecked")
	public static Map<String, Object> getTaskContent(TaskService taskService, Long taskId) {

		Task task = taskService.getTaskById(taskId);
		
		Content contentById = taskService.getContentById(task.getTaskData().getDocumentContentId());
		Object unmarshalledObject = ContentMarshallerHelper.unmarshall(contentById.getContent(), null, null);
		if (!(unmarshalledObject instanceof Map)) {
			throw new IllegalStateException(" The Task Content Needs to be a Map in order to use this method and it was: " + unmarshalledObject.getClass());

		}
		Map<String, Object> content = (Map<String, Object>) unmarshalledObject;

		return content;
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
