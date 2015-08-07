package com.example.bpms.examples;

import java.util.List;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.runtime.manager.context.EmptyContext;

import com.example.bpms.EnvironmentManager;
import com.example.bpms.SupplyItem;

/**
 * This example utilizes a simple Camel route which starts the BPM process. Fist some transformation
 * and data enrichment is handled in the Camel route. Then the data is pass as an input parameter
 * to the business process.
 * 
 * @author Frank
 *
 */
public class CamelToBPMProcess {
	
	public static void main(String[] args) {
		
		EnvironmentManager simpleEnv = EnvironmentManager.get();
		RuntimeManager manager = simpleEnv.start("com/example/bpms/simplesupplyitemapproval.bpmn2");
		
		
		try{
			new CamelToBPMProcess().startSimulation(manager, simpleEnv.getCamelContext());
		}finally{
			simpleEnv.stop();
		}
		System.out.println("done.");
	}
	
	public void startSimulation(final RuntimeManager manager, final CamelContext context){
		
		//Send two supply items as JSON to the camel route
		StringBuilder json = new StringBuilder();
		json.append("{")
				.append("\"com.example.bpms.SupplyItems\": {")
					.append("\"items\": [")
						.append("{")
							.append("\"com.example.bpms.SupplyItem\": [")
								.append("{")
									.append("\"description\": \"Red Hat\"")
								.append("},")
								.append("{")
									.append("\"description\": \"Red Shoes\"")
								.append("}")
							.append("]")
						.append("}")
					.append("]")
				.append("}")
			.append("}");
		
		ProducerTemplate template = context.createProducerTemplate();
		template.sendBody("direct:supplyitem", json.toString());
		
		
		//Two tasks should be available for approving now
		RuntimeEngine runtimeEngine = manager.getRuntimeEngine(EmptyContext.get());
		
		TaskService taskService = runtimeEngine.getTaskService();
		
        // Simulate a user approving items that cost $10
        {
            List<TaskSummary> list = taskService.getTasksAssignedAsPotentialOwner("bob", "en-UK");
            for (TaskSummary taskSummary : list) {
                System.out.println("bob starts a task : taskId = " + taskSummary.getId());
                taskService.start(taskSummary.getId(), "bob");
                Map<String,Object> content = taskService.getTaskContent(taskSummary.getId());
                SupplyItem taskItem = (SupplyItem) content.get("supplyItemInput");
                
                if(taskItem.getCost().equals(Double.valueOf(10.0))){
                	content.put("approved", true);
                }else{
                	content.put("approved", false);
                }
                taskService.complete(taskSummary.getId(), "bob", content);
            }
        }
	}
	
}
