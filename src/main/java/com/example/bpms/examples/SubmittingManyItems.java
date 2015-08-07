package com.example.bpms.examples;

import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.internal.runtime.manager.context.CorrelationKeyContext;
import org.kie.internal.runtime.manager.context.ProcessInstanceIdContext;

import com.example.bpms.EnvironmentManager;
import com.example.bpms.SupplyItem;
import com.example.bpms.async.ApproverBob;
import com.example.bpms.async.ApproverJohn;
import com.example.bpms.async.SendItemForProcessing;
import com.example.bpms.examples.mbeans.SubmittingManyItemsMXBean;

/**
 * This example is submitting a very large number of processes. The performance in terms of time from start
 * to finish is not representative of a production system as we are relying on H2. However, what this shows
 * is that a single JVM is capable of submitting thousands of processes and completing thousands of
 * tasks.
 * 
 * @author Frank
 *
 */
public class SubmittingManyItems implements SubmittingManyItemsMXBean{
	
	private RuntimeManager manager;
	private AtomicBoolean shutdown = new AtomicBoolean(false);
	private int numRequests = 1300;
	private static final EnvironmentManager simpleEnv = EnvironmentManager.get();
	
	public static void main(String[] args) throws Exception {
		
		RuntimeManager manager = simpleEnv.start("com/example/bpms/simplesupplyitemapproval.bpmn2");
		
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer(); 
        ObjectName name = new ObjectName("com.example:type=SubmittingManyItems"); 
        SubmittingManyItems mbean = new SubmittingManyItems(manager); 
        mbs.registerMBean(mbean, name); 
        
        while(!mbean.isShutdown());
	}
	
	public SubmittingManyItems(RuntimeManager manager){
		this.manager = manager;
	}
	
	public void setNumberOfRequests(int numberOfRequests){
		this.numRequests = numberOfRequests;
	}
	public int getNumberOfRequests(){
		return numRequests;
	}
	
	public void startSimulation(){
		
		RuntimeEngine runtimeEngine = manager.getRuntimeEngine(ProcessInstanceIdContext.get());
		
		//Build numRequests requests to send an item for processing
		Collection<SendItemForProcessing> requests = new LinkedList<SendItemForProcessing>();
		for(int x = 0; x < numRequests; x++){
			SupplyItem item = new SupplyItem();
			String description = x%2==0?"Red Hat " + x:"Red Shoes " + x;
			item.setDescription(description);
			
			requests.add(new SendItemForProcessing(item, manager));
		}
		
		//Asynchronously submit numRequests items for approval. This is simulating what would happen
		//when supply items are being received via a RESTful Endpoint.
		ExecutorService exec = Executors.newCachedThreadPool();
		
		{ //Submit one request so that the users are inserted into the DB correctly.
			SupplyItem item = new SupplyItem();
			String description = "Broken Hat";
			item.setDescription(description);
			Future<ProcessInstance> fut = exec.submit(new SendItemForProcessing(item, manager));
			while(!fut.isDone());
		}
		
		
		long start = System.currentTimeMillis();
		try {
			exec.invokeAll(requests);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		exec.shutdown();
		try {
			exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		long end = System.currentTimeMillis();
		
		System.out.println("Submitted all processes in " + (end - start) + "ms");
		
		//Now that Two supply items have been submitted, simulate two different users trying to approve
		//or reject the tasks.
		
		exec = Executors.newCachedThreadPool();
		
		start = System.currentTimeMillis();
		exec.submit(new ApproverBob(manager, true));
		exec.submit(new ApproverJohn(manager, true));
		
		exec.shutdown();
		
		try {
			exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		end = System.currentTimeMillis();
		
		System.out.println("Handled all tasks in " + (end - start) + "ms");
		
        // -----------
        manager.disposeRuntimeEngine(runtimeEngine);
	}
	
	public boolean isShutdown(){
		return shutdown.get();
	}
	
	public void end(){
		simpleEnv.stop();
		shutdown.set(true);
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
