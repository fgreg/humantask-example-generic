package com.example.bpms.async;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.jbpm.process.audit.AuditLoggerFactory;
import org.jbpm.process.audit.AuditLoggerFactory.Type;
import org.jbpm.workflow.instance.WorkflowRuntimeException;
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
public class SubmitterSally implements Callable<Long> {

	private final List<SupplyItem> items;
	private final RuntimeManager manager;
	private final boolean processAuditing;
	private final BatchRepository batchRepo;
	
	public SubmitterSally(BatchRepository batchRepo, RuntimeManager manager, SupplyItem item){
		this(batchRepo, false, manager, item);
	}
	
	public SubmitterSally(BatchRepository batchRepo, boolean processAuditing, RuntimeManager manager, SupplyItem... items){
		this.items = new ArrayList<>(Arrays.asList(items));
		this.manager = manager;
		this.processAuditing = processAuditing;
		this.batchRepo = batchRepo;
	}
	
	@Override
	public Long call() throws Exception {
		
		//Sally submits all batches
		//Start a batch
		Batch b = new Batch().setBatchId(Batch.nextId()).setUserId("sally");
		for(SupplyItem item : items){
			
			ProcessInstance proc = startProcess(item);
			b.addProcessInstanceId(proc.getId());
			
		}
		batchRepo.saveBatch(b);
		
		
		return b.getBatchId();
	}
	
	private ProcessInstance startProcess(SupplyItem item){
		RuntimeEngine runtimeEngine = manager.getRuntimeEngine(EmptyContext.get());
		KieSession ksession = runtimeEngine.getKieSession();
		
		//Turn on Auditing
		if(processAuditing){
			AuditLoggerFactory.newInstance(Type.JPA, ksession, null);
		}
		
		Map<String, Object> params = new HashMap<>(2);
		params.put("supplyItem", item);
		
		ProcessInstance proc;
		try{
			proc = ksession.startProcess("com.example.bpms.simplesupplyitemapproval", params);
		}catch(WorkflowRuntimeException e){
			//Retry once. Starting a process could result in transient error when new users are added to the system.
			////https://bugzilla.redhat.com/show_bug.cgi?id=1208056
			proc = ksession.startProcess("com.example.bpms.simplesupplyitemapproval", params);
		}finally{
			manager.disposeRuntimeEngine(runtimeEngine);
		}
		System.out.println("Started process: " + proc );
		
		return proc;
	}

}
