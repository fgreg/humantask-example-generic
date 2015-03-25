package com.example.bpms;

import java.util.HashMap;
import java.util.Map;

import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.internal.runtime.manager.context.CorrelationKeyContext;

public class SimpleApproval implements Runnable {

	private final SupplyItem item;
	private final RuntimeManager manager;
	
	
	public SimpleApproval(SupplyItem item, RuntimeManager manager){
		this.item = item;
		this.manager = manager;
	}
	
	@Override
	public void run() {
		RuntimeEngine runtimeEngine = manager.getRuntimeEngine(CorrelationKeyContext.get());
		KieSession ksession = runtimeEngine.getKieSession();
		Map<String, Object> params = new HashMap<>();
		params.put("supplyItem", item);
		ProcessInstance proc = ksession.startProcess("com.example.bpms.simplesupplyitemapproval", params);
		
		
		System.out.println("Started process:" + proc);
		
		manager.disposeRuntimeEngine(runtimeEngine);
	}
	
	
//	public ProcessInstance startSubmittask(SupplyItem item, RuntimeEngine runtimeEngine){
//		KieSession ksession = runtimeEngine.getKieSession();
//		Map<String, Object> params = new HashMap<>();
//		params.put("supplyItem", item);
//		ProcessInstance proc = ksession.startProcess("com.example.bpms.simplesupplyitemapproval", params);
//		
//		System.out.println("Started process:" + proc);
//		
//		return proc;
//	}

}
