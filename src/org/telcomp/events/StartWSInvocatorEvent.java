package org.telcomp.events;

import java.util.HashMap;
import java.util.Random;
import java.io.Serializable;

public final class StartWSInvocatorEvent implements Serializable {

	private final long id;
	private static final long serialVersionUID = 1L;
	
	private String serviceWSDL;
	private String operationName;
	private HashMap<String, String> operationInputs = new HashMap<String, String>();
	private String backupFile;

	@SuppressWarnings("unchecked")
	public StartWSInvocatorEvent(HashMap<String, ?> hashMap) {
		id = new Random().nextLong() ^ System.currentTimeMillis();
		this.serviceWSDL = (String) hashMap.get("serviceWSDL");
		this.operationName = (String) hashMap.get("operationName");
		this.operationInputs = (HashMap<String, String>) hashMap.get("operationInputs");
		this.backupFile = (String) hashMap.get("backupFile");
	}
	
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o == null) return false;
		return (o instanceof StartWSInvocatorEvent) && ((StartWSInvocatorEvent)o).id == id;
	}
	
	public int hashCode() {
		return (int) id;
	}
	
	public String getServiceWSDL(){
		return this.serviceWSDL;
	}
	
	public String getOperationName(){
		return this.operationName;
	}
	
	public HashMap<String, String> getOperationInputs(){
		return this.operationInputs;
	}
	
	public String getBackupFile(){
		return this.backupFile;
	}
	
	public String toString() {
		return "startWSInvocatorEvent[" + hashCode() + "]";
	}
}
