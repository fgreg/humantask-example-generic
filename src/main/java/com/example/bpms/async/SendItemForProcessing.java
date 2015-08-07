package com.example.bpms.async;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.jbpm.workflow.instance.WorkflowRuntimeException;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.internal.runtime.manager.context.EmptyContext;

import com.example.bpms.SupplyItem;

/**
 * Submits an Item for processing by starting a process.
 * 
 * @author Frank
 *
 */
public class SendItemForProcessing implements Callable<ProcessInstance> {

	private final SupplyItem item;
	private final RuntimeManager manager;
	
	
	public SendItemForProcessing(SupplyItem item, RuntimeManager manager){
		this(false, item, manager);
	}
	
	public SendItemForProcessing(boolean processAuditing, SupplyItem item, RuntimeManager manager){
		this.item = item;
		this.manager = manager;
	}
	
	@Override
	public ProcessInstance call() throws Exception {
		
		RuntimeEngine runtimeEngine = manager.getRuntimeEngine(EmptyContext.get());
		KieSession ksession = runtimeEngine.getKieSession();
		
		Map<String, Object> params = new HashMap<>();
		params.put("supplyItem", item);
		
		ProcessInstance proc;
		try{
			proc = ksession.startProcess("com.example.bpms.simplesupplyitemapproval", params);
		}catch(WorkflowRuntimeException e){
			//Retry once. Starting a process could result in transient error when new users are added to the system.
			//https://bugzilla.redhat.com/show_bug.cgi?id=1208056
			proc = ksession.startProcess("com.walmart.bpms.simplesupplyitemapproval", params);
		}
		
		System.out.println("Started process:" + proc);
		
		manager.disposeRuntimeEngine(runtimeEngine);
		
		return proc;
	}

}
