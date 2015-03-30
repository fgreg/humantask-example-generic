package com.example.bpms.examples;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.internal.runtime.manager.context.EmptyContext;

import com.example.bpms.EnvironmentManager;
import com.example.bpms.SupplyItem;
import com.example.bpms.async.ApproverBob;
import com.example.bpms.async.ApproverJohn;
import com.example.bpms.async.SendItemForProcessing;

/**
 * This example is showing asynchronous process starting. This is meant to simulate
 * an application where processes are being started from RESTful endpoints. 
 * 
 * @author Frank
 *
 */
public class AsynchonousProcessSubmission {
	
	public static void main(String[] args) {
		
		EnvironmentManager simpleEnv = EnvironmentManager.get();
		
		RuntimeManager manager = simpleEnv.start("com/example/bpms/simplesupplyitemapproval.bpmn2");
		
		try{
			new AsynchonousProcessSubmission().startSimulation(manager);
		}finally{
			simpleEnv.stop();
		}
		System.out.println("done.");
	}
	
	public void startSimulation(final RuntimeManager manager){
		
//		initDbTables(manager);
		
		SupplyItem item1 = new SupplyItem();
		item1.setDescription("Red Shoes");
		SupplyItem item2 = new SupplyItem();
		item2.setDescription("Red Hat");
		
		//Asynchronously submit two items for approval. This is simulating what would happen
		//when supply items are being received via a RESTful Endpoint.
		ExecutorService exec = Executors.newCachedThreadPool();
		
		exec.submit(new SendItemForProcessing(item1, manager));
		exec.submit(new SendItemForProcessing(item2, manager));
		
		exec.shutdown();
		try {
			exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		//Now that Two supply items have been submitted, simulate two different users trying to approve
		//or reject the tasks.
		
		exec = Executors.newCachedThreadPool();
		
		exec.submit(new ApproverBob(manager));
		exec.submit(new ApproverJohn(manager));
		
		exec.shutdown();
		
		try {
			exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        
	}
	
	
	//This is only necessary because we are using Hibernate to generate the schema asynchrounously.
	private void initDbTables(RuntimeManager manager) {
		RuntimeEngine runtimeEngine = manager.getRuntimeEngine(EmptyContext.get());
		KieSession ksession = runtimeEngine.getKieSession();
		Map<String, Object> params = new HashMap<>();
		SupplyItem item1 = new SupplyItem();
		item1.setDescription("Red Shoes");
		params.put("supplyItem", item1);
		ProcessInstance proc = ksession.startProcess("com.example.bpms.simplesupplyitemapproval", params);

		ksession.abortProcessInstance(proc.getId());
		manager.disposeRuntimeEngine(runtimeEngine);
	}
	
}
