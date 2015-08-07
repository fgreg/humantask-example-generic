package com.example.bpms.audit;

import java.util.List;

import org.kie.api.runtime.manager.audit.AuditService;
import org.kie.api.runtime.manager.audit.ProcessInstanceLog;

public interface CustomAuditService extends AuditService {

	/**
	 * Get all ProcessInstanceLog for the given process instance id.
	 * 
	 * @param processInstanceId The process instance id.
	 * @return
	 */
	public List<ProcessInstanceLog> findByProcessInstanceId(long processInstanceId);
	
}
