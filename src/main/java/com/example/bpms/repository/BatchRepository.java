package com.example.bpms.repository;

import java.util.List;

import com.example.bpms.Batch;

/**
 * Storage and retrieval of Batch objects
 * 
 * @author Frank
 *
 */
public interface BatchRepository {

	public void saveBatch(Batch batch);
	
	public Batch findById(long batchId);
	
	public List<Batch> findByUserId(String userId);
	
}
