package com.example.bpms.examples.mbeans;

public interface SubmittingManyItemsMXBean {

	public void startSimulation();
	public void end();
	
	public void setNumberOfRequests(int numberOfRequests);
	public int getNumberOfRequests();
	
}
