package com.example.bpms.async;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.jbpm.process.audit.AuditLoggerFactory;
import org.jbpm.process.audit.AuditLoggerFactory.Type;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.internal.runtime.manager.context.EmptyContext;

import com.example.bpms.Batch;
import com.example.bpms.SupplyItem;
import com.example.bpms.repository.BatchRepository;

/**
 * Starts a process and creates a batch for the item passed in.
 * 
 * @author Frank
 *
 */
public class SendBatchItemForProcessing implements Callable<Long> {

	private final SupplyItem item;
	private final RuntimeManager manager;
	private final boolean processAuditing;
	private final BatchRepository batchRepo;
	
	public SendBatchItemForProcessing(BatchRepository batchRepo, SupplyItem item, RuntimeManager manager){
		this(batchRepo, false, item, manager);
	}
	
	public SendBatchItemForProcessing(BatchRepository batchRepo, boolean processAuditing, SupplyItem item, RuntimeManager manager){
		this.item = item;
		this.manager = manager;
		this.processAuditing = processAuditing;
		this.batchRepo = batchRepo;
	}
	
	@Override
	public Long call() throws Exception {
		RuntimeEngine runtimeEngine = manager.getRuntimeEngine(EmptyContext.get());
		KieSession ksession = runtimeEngine.getKieSession();
		
		//Turn on Auditing
		if(processAuditing){
			AuditLoggerFactory.newInstance(Type.JPA, ksession, null);
		}
		
		Map<String, Object> params = new HashMap<>();
		params.put("supplyItem", item);
		ProcessInstance proc = ksession.startProcess("com.example.bpms.simplesupplyitemapproval", params);
		
		System.out.println("Started process: " + proc );
		
		manager.disposeRuntimeEngine(runtimeEngine);
		
		//Sally submits all batches
		Batch b = new Batch().setBatchId(Batch.nextId()).setProcId(proc.getId()).setUserId("sally");
		batchRepo.saveBatch(b);
		
		return b.getBatchId();
	}

}
