package org.telcomp.events;

import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.io.Serializable;

public final class EndWSInvocatorEvent implements Serializable {

	private final long id;
	private static final long serialVersionUID = 1L;
	
	private HashMap<String, List<String>> operationOutputs = new HashMap<String, List<String>>();
	private String operationName;
	private boolean success;

	public EndWSInvocatorEvent(HashMap<String, List<String>> operationOutputs, String operationName, boolean success) {
		id = new Random().nextLong() ^ System.currentTimeMillis();
		this.operationOutputs = operationOutputs;
		this.operationName = operationName;
		this.setSuccess(success);
	}

	public boolean equals(Object o) {
		if (o == this) return true;
		if (o == null) return false;
		return (o instanceof EndWSInvocatorEvent) && ((EndWSInvocatorEvent)o).id == id;
	}
	
	public int hashCode() {
		return (int) id;
	}
	
	public HashMap<String, List<String>> getOperationOutputs(){
		return this.operationOutputs;
	}
	
	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}
	
	public String getOperationName(){
		return this.operationName;
	}
	
	public void setOperationName(String operationName){
		this.operationName = operationName;
	}

	public String toString() {
		return "endWSInvocatorEvent[" + hashCode() + "]";
	}
}
