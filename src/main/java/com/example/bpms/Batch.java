package com.example.bpms;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Example of a batch that keeps track of which user started which processes
 * 
 * @author Frank
 *
 */
public class Batch {
	
	private static final AtomicLong ID_GEN = new AtomicLong();

	private String userId;
	private long batchId;
	private List<Long> processInstanceIds = new ArrayList<>();
	
	public List<Long> getProcessInstanceIds() {
		return new ArrayList<Long>(processInstanceIds);
	}
	public Batch setProcessInstanceIds(List<Long> processInstanceIds) {
		this.processInstanceIds = new ArrayList<Long>(processInstanceIds);
		return this;
	}
	public Batch addProcessInstanceId(long processInstanceId) {
		this.processInstanceIds.add(processInstanceId);
		return this;
	}
	
	public long getBatchId() {
		return batchId;
	}
	public Batch setBatchId(long batchId) {
		this.batchId = batchId;
		return this;
	}
	
	public static long nextId(){
		return ID_GEN.getAndIncrement();
	}
	public String getUserId() {
		return userId;
	}
	public Batch setUserId(String userId) {
		this.userId = userId;
		return this;
	}
	
	
}
