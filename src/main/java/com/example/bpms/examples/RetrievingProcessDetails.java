package com.example.bpms.examples;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.manager.audit.NodeInstanceLog;
import org.kie.api.runtime.manager.audit.ProcessInstanceLog;

import com.example.bpms.Batch;
import com.example.bpms.EnvironmentManager;
import com.example.bpms.SupplyItem;
import com.example.bpms.async.ApproverBob;
import com.example.bpms.async.ApproverJohn;
import com.example.bpms.async.SubmitterSally;
import com.example.bpms.audit.CustomAuditService;
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
	private CustomAuditService auditService;
	
	
	public RetrievingProcessDetails setBatchRepository(BatchRepository batchRepo){
		this.batchRepo = batchRepo;
		return this;
	}
	
	public RetrievingProcessDetails setAuditLogService(CustomAuditService auditService){
		this.auditService = auditService;
		return this;
	}
	
	public void startSimulation(final RuntimeManager manager){
		
		//Asynchronously submit two items for approval. This is simulating what would happen
		//when supply items are being received via a RESTful Endpoint.
		
		//We are going to submit three 'batches'. Each batch will have 1 item except the last.
		ExecutorService exec = Executors.newCachedThreadPool();
		
		exec.submit(new SubmitterSally(batchRepo, true, manager, new SupplyItem().setDescription("Red Shoes")));
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {}
		exec.submit(new SubmitterSally(batchRepo, true, manager, new SupplyItem().setDescription("Red Hat")));
		exec.submit(new SubmitterSally(batchRepo, true, manager, new SupplyItem[]{ 
				new SupplyItem().setDescription("Rose Hat")
			,	new SupplyItem().setDescription("Maroon Hat")
			,	new SupplyItem().setDescription("Mauve Hat")
			,	new SupplyItem().setDescription("Cherry Hat")
			,	new SupplyItem().setDescription("Scarlet Hat")
		}));
		
		exec.shutdown();
		try {
			exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		//Processes have been submitted and are now running.
		//Get all the batches sally has submitted
		List<Batch> sallysBatches = batchRepo.findByUserId("sally");
		
		//Look up the process status for those batches
		for(Batch b : sallysBatches){
			System.out.println("### Batch " + b.getBatchId() + " ###");
			for(Long procId : b.getProcessInstanceIds()){
				ProcessInstanceLog proc = auditService.findByProcessInstanceId(procId).get(0);
				System.out.println( "\tProcess id: " + proc.getProcessId()
						+ "\n\t\t" + "Process Name: " + proc.getProcessName()
						+ "\n\t\t" + "Process Start: " +  proc.getStart()
						+ "\n\t\t" + "Process End: " + proc.getEnd()
						+ "\n\t\t" + "Process Status: " +  proc.getStatus()
				);
				List<? extends NodeInstanceLog> nodes = auditService.findNodeInstances(procId);
				for(NodeInstanceLog node : nodes){
					System.out.println("Node " + node.toString() + "{{WI}} " + node.getWorkItemId());
				}
			}
		}
		
		
		//Now simulate two different users trying to approve or reject the tasks.
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
			System.out.println("### Batch " + b.getBatchId() + " ###");
			for(Long procId : b.getProcessInstanceIds()){
				ProcessInstanceLog proc = auditService.findByProcessInstanceId(procId).get(0);
				System.out.println( "\tProcess id: " + proc.getProcessId()
						+ "\n\t\t" + "Process Name: " + proc.getProcessName()
						+ "\n\t\t" + "Process Start: " +  proc.getStart()
						+ "\n\t\t" + "Process End: " + proc.getEnd()
						+ "\n\t\t" + "Process Status: " +  proc.getStatus()
				);
			}
		}
        
	}
	
}
