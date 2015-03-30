package com.example.bpms;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple SupplyItem
 * 
 * @author Frank
 *
 */
public class SupplyItem implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4716400342149645470L;
	
	private static final AtomicLong ID_GEN = new AtomicLong();
	
	private long id;
	private String description;
	
	public SupplyItem(){
		this(nextId());
	}
	
	public SupplyItem(long id){
		this.id = id;
	}
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	public static long nextId(){
		return ID_GEN.getAndIncrement();
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SupplyItem [id=").append(id).append(", description=").append(description).append("]");
		return builder.toString();
	}


}
