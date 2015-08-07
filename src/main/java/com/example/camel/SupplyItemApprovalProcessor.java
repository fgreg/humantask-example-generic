package com.example.camel;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.internal.runtime.manager.context.ProcessInstanceIdContext;

public class SupplyItemApprovalProcessor implements Processor{

	private RuntimeManager manager;
	
	public SupplyItemApprovalProcessor(RuntimeManager manager){
		this.manager = manager;
	}
	
	@Override
	public void process(Exchange exchange) throws Exception {
		RuntimeEngine runtimeEngine = manager.getRuntimeEngine(ProcessInstanceIdContext.get());
		KieSession ksession = runtimeEngine.getKieSession();
		Map<String, Object> params = new HashMap<>();
		params.put("supplyItem", exchange.getIn().getBody());
		ProcessInstance proc = ksession.startProcess("com.example.bpms.simplesupplyitemapproval", params);

		System.out.println("Started process:" + proc);

		manager.disposeRuntimeEngine(runtimeEngine);
	}

}
