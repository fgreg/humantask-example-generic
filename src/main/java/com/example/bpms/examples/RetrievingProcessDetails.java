package com.example.bpms.examples;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jbpm.process.audit.ProcessInstanceLog;
import org.kie.api.runtime.manager.RuntimeManager;

import com.example.bpms.Batch;
import com.example.bpms.EnvironmentManager;
import com.example.bpms.SupplyItem;
import com.example.bpms.async.ApproverBob;
import com.example.bpms.async.ApproverJohn;
import com.example.bpms.async.SendBatchItemForProcessing;
import com.example.bpms.audit.CustomAuditLogService;
import com.example.bpms.repository.BatchRepository;
import com.example.bpms.repository.MapBatchRepository;

/**
 * This example shows how to retrieve process information using an audit log service.
 * 
 * @author Frank
 *
 */
public class RetrievingProcessDetails {
	
	public static void main(String[] args) {
		
		EnvironmentManager simpleEnv = EnvironmentManager.get();
		
		RuntimeManager manager = simpleEnv.start("com/example/bpms/simplesupplyitemapproval.bpmn2");
		
		try{
			new RetrievingProcessDetails()
				.setBatchRepository(new MapBatchRepository())
				.setAuditLogService(simpleEnv.getAuditLogService())
				.startSimulation(manager);
		}finally{
			simpleEnv.stop();
		}
		System.out.println("done.");
	}
	
	private BatchRepository batchRepo;
	private CustomAuditLogService auditService;
	
	
	public RetrievingProcessDetails setBatchRepository(BatchRepository batchRepo){
		this.batchRepo = batchRepo;
		return this;
	}
	
	public RetrievingProcessDetails setAuditLogService(CustomAuditLogService auditService){
		this.auditService = auditService;
		return this;
	}
	
	public void startSimulation(final RuntimeManager manager){
		
		SupplyItem item1 = new SupplyItem();
		item1.setDescription("Red Shoes");
		SupplyItem item2 = new SupplyItem();
		item2.setDescription("Red Hat");
		
		//Asynchronously submit two items for approval. This is simulating what would happen
		//when supply items are being received via a RESTful Endpoint.
		
		//We are going to submit two 'batches'. Each batch will have 1 item.
		ExecutorService exec = Executors.newCachedThreadPool();
		
		exec.submit(new SendBatchItemForProcessing(batchRepo, true, item1, manager));
		exec.submit(new SendBatchItemForProcessing(batchRepo, true, item2, manager));
		
		exec.shutdown();
		try {
			exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		//Two processes have been submitted and are now running.
		//Get all the batches sally has submitted
		List<Batch> sallysBatches = batchRepo.findByUserId("sally");
		
		//Look up the process status for those batches
		for(Batch b : sallysBatches){
			ProcessInstanceLog proc = auditService.findByProcessInstanceId(b.getProcId()).get(0);
			System.out.println( "Process id: " + proc.getId()
					+ "\n\t" + "Process Name: " + proc.getProcessName()
					+ "\n\t" + "Process Start: " +  proc.getStart()
					+ "\n\t" + "Process End: " + proc.getEnd()
					+ "\n\t" + "Process Status: " +  proc.getStatus()
			);
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
		
		//The processes should now be complete
		//Look up the process status for those batches
		for(Batch b : sallysBatches){
			ProcessInstanceLog proc = auditService.findByProcessInstanceId(b.getProcId()).get(0);
			System.out.println( "Process id: " + proc.getId()
					+ "\n\t" + "Process Name: " + proc.getProcessName()
					+ "\n\t" + "Process Start: " +  proc.getStart()
					+ "\n\t" + "Process End: " + proc.getEnd()
					+ "\n\t" + "Process Status: " +  proc.getStatus()
			);
		}
        
	}
	
}
