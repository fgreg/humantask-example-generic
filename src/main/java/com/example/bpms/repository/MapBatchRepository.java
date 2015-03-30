package com.example.bpms.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.example.bpms.Batch;

/**
 * Simple implementation backed by a Map
 * 
 * @author Frank
 *
 */
public class MapBatchRepository implements BatchRepository {

	private final Map<Long, Batch> batchIdToProcId = new ConcurrentHashMap<>();
	
	@Override
	public void saveBatch(Batch batch) {
		batchIdToProcId.put(batch.getBatchId(), batch);
	}

	@Override
	public Batch findById(long batchId) {
		return batchIdToProcId.get(batchId);
	}

	@Override
	public List<Batch> findByUserId(String userId) {
		List<Batch> results = new ArrayList<>();
		
		for(Entry<Long, Batch> entry : batchIdToProcId.entrySet()){
			if(userId.equals(entry.getValue().getUserId())){
				results.add(entry.getValue());
			}
		}
		
		return results;
	}

}
