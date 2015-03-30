package com.example.bpms;

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
	private long procId;
	
	public long getProcId() {
		return procId;
	}
	public Batch setProcId(long procId) {
		this.procId = procId;
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
