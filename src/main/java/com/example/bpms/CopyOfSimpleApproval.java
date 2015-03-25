package com.example.bpms;

import java.util.HashMap;
import java.util.Map;

import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.ProcessInstance;

public class CopyOfSimpleApproval implements Runnable {

	private SupplyItem item;
	private RuntimeEngine runtimeEngine;
	
	
	public CopyOfSimpleApproval(SupplyItem item, RuntimeEngine runtimeEngine){
		this.item = item;
		this.runtimeEngine = runtimeEngine;
	}
	
	@Override
	public void run() {
		KieSession ksession = runtimeEngine.getKieSession();
		Map<String, Object> params = new HashMap<>();
		params.put("supplyItem", item);
		ProcessInstance proc = ksession.startProcess("com.example.bpms.simplesupplyitemapproval", params);
		
		
		System.out.println("Started process:" + proc);
		
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
