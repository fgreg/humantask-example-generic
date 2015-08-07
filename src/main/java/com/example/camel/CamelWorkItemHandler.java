package com.example.camel;

import java.util.Collections;

import org.apache.camel.ProducerTemplate;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;

public class CamelWorkItemHandler implements WorkItemHandler {
	
	private ProducerTemplate producer;
	
	public CamelWorkItemHandler(ProducerTemplate template){
		this.producer = template;
	}

	@Override
	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		
		String uri = (String) workItem.getParameter("URI");
		Object body = workItem.getParameter("MessageBody");
		producer.sendBody(uri, body);
		
		manager.completeWorkItem(workItem.getId(), Collections.<String, Object>emptyMap());
	}

	@Override
	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
		// cannot be aborted

	}

}
