package com.example.bpms.audit;

import java.util.List;

import org.jbpm.process.audit.AuditLogService;
import org.jbpm.process.audit.ProcessInstanceLog;

public interface CustomAuditLogService extends AuditLogService {

	/**
	 * Get all ProcessInstanceLog for the given process instance id.
	 * 
	 * @param processInstanceId The process instance id.
	 * @return
	 */
	public List<ProcessInstanceLog> findByProcessInstanceId(long processInstanceId);
	
}
