package com.example.bpms;

import java.io.Serializable;

public class SupplyItem implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4716400342149645470L;
	private String description;
	
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}


}
