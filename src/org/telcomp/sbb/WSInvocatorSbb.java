package org.telcomp.sbb;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.slee.ActivityContextInterface;
import javax.slee.Address;
import javax.slee.RolledBackContext;
import javax.slee.SbbContext;
import javax.slee.facilities.TimerEvent;
import javax.slee.facilities.TimerFacility;
import javax.slee.facilities.TimerOptions;
import javax.slee.facilities.TimerPreserveMissed;
import javax.slee.nullactivity.NullActivity;
import javax.slee.nullactivity.NullActivityContextInterfaceFactory;
import javax.slee.nullactivity.NullActivityFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.java.client.slee.resource.http.HttpClientActivity;
import net.java.client.slee.resource.http.HttpClientActivityContextInterfaceFactory;
import net.java.client.slee.resource.http.HttpClientResourceAdaptorSbbInterface;
import net.java.client.slee.resource.http.event.ResponseEvent;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.telcomp.events.EndWSInvocatorEvent;
import org.telcomp.events.StartWSInvocatorEvent;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public abstract class WSInvocatorSbb implements javax.slee.Sbb {
	
	private HttpClientActivityContextInterfaceFactory httpClientAci;
	private HttpClientResourceAdaptorSbbInterface raSbbInterface;
	private TimerFacility timerFacility;
	private NullActivityContextInterfaceFactory nullACIFactory;
	private NullActivityFactory nullActivityFactory;
	
	private static final String dynamicWebServiceIP = "http://190.90.112.7:8084/";
	//private static final String dynamicWebServiceIP = "http://192.168.190.55:8084/";
	private static final String dynamicWebServicePath = "dynamics-web-service/Axis2Servlet?";

	@SuppressWarnings("rawtypes")
	public void onStartWSInvocatorEvent(StartWSInvocatorEvent event, ActivityContextInterface aci) {
		this.setActivityFlow(aci);
		String serviceWSDL = event.getServiceWSDL();
		String operationName = event.getOperationName();
		this.setOperationName(operationName);
		HashMap<String, String> operationInputs = event.getOperationInputs();
		this.setServiceInputs(operationInputs);
		String inputs = "&";
		Iterator it = operationInputs.entrySet().iterator();
		
		while(it.hasNext()){
			Map.Entry e = (Map.Entry)it.next();
			inputs = inputs.concat(e.getKey() + "=" + e.getValue() + "&");
		}
		
		//Changing spaces for %20 and removing non-breaking white spaces
		inputs = inputs.substring(0, inputs.length()-1);
		String inputs0 = inputs.replaceAll("%", "%25");
		String inputs1 = inputs0.replaceAll(" ", "%20");
		String inputs2 = inputs1.replace("\u00A0", "");
		String inputs3 = inputs2.replaceAll("\\r\\n|\\n|\\r", "%20");
		
		
		System.out.println("CURRENT WSDL: "+serviceWSDL);
		System.out.println("OPERATION NAME: "+operationName);
		System.out.println("PARAMETERS: "+inputs);
		
		HttpGet getRequest = new HttpGet(dynamicWebServiceIP+dynamicWebServicePath+"service="+serviceWSDL
				+"&operation="+operationName+inputs3);
		
		try {
			HttpClientActivity clientActivity = raSbbInterface.createHttpClientActivity(true, null);
			ActivityContextInterface clientAci = httpClientAci.getActivityContextInterface(clientActivity);
			clientAci.attach(sbbContext.getSbbLocalObject());
			clientActivity.execute(getRequest, dynamicWebServiceIP+dynamicWebServicePath+"service="+serviceWSDL
					+"&operation="+operationName+inputs2);
			System.out.println("INVOKING WEB SERVICE...");
			NullActivity timerBus = this.nullActivityFactory.createNullActivity();
			ActivityContextInterface timerBusACI;
			timerBusACI = this.nullACIFactory.getActivityContextInterface(timerBus);
			timerBusACI.attach(sbbContext.getSbbLocalObject());
			TimerOptions options = new TimerOptions();
			options.setPreserveMissed(TimerPreserveMissed.ALL);
			this.timerFacility.setTimer(timerBusACI, null, System.currentTimeMillis()+10000, options);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	public void onTimerEvent(TimerEvent event, ActivityContextInterface aci) {
		aci.detach(sbbContext.getSbbLocalObject());
		if(!this.getAnsweredRequest()){
			System.out.println("Request Timeout!!");
			System.out.println("***************************************************");
			
			EndWSInvocatorEvent endEvent = new EndWSInvocatorEvent(null, this.getOperationName(), false);
			this.fireEndWSInvocatorEvent(endEvent, this.getActivityFlow(), null);
			aci.detach(this.sbbContext.getSbbLocalObject());
			this.getActivityFlow().detach(this.sbbContext.getSbbLocalObject());
			//boolean flag = false;
			//int i = 0;
			//String newOpName = "";
			/*try {
				Path filePath = Paths.get("/home/julian/JSLEE-Workspace/WebServiceInvocator/lib/Repository-services-backup.txt");
				List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
				while(!flag){
					if(lines.get(i).indexOf(this.getCurrentWsdl()) >= 0){
						this.setCurrentWsdl(lines.get(i+1).substring(0, lines.get(i+1).indexOf("%")));
						newOpName = lines.get(i+1).substring(lines.get(i+1).indexOf("%")+1);
						flag = true;
					} else{
						i++;
					}
				}
				
				startWSInvocatorEvent startWS = new startWSInvocatorEvent(this.getCurrentWsdl(), newOpName, this.getServiceInputs(), null);
				this.fireStartWSInvocatorEvent(startWS, this.getActivityFlow(), null);
				
			} catch (IOException e) {
				e.printStackTrace();
			}*/
		}
	}

	public void onResponseEvent(ResponseEvent event, ActivityContextInterface aci) {
		this.setAnsweredRequest(true);
		HttpResponse response = event.getHttpResponse();
		if(response != null){
			if(response.getStatusLine().getStatusCode() == 200){
				System.out.println("200 OK Response received!!");
				try {
					String responseBody = EntityUtils.toString(response.getEntity());
					//System.out.println(responseBody);
					DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
					DocumentBuilder db = dbf.newDocumentBuilder();
					Document doc = db.parse(new InputSource(new ByteArrayInputStream(responseBody.getBytes("utf-8"))));
					doc.getDocumentElement().normalize();
					HashMap<String, List<String>> operationOutputs = this.getXMLData(doc);
					//System.out.println(operationOutputs);
					EndWSInvocatorEvent endEvent = new EndWSInvocatorEvent(operationOutputs, this.getOperationName(), true);
					this.fireEndWSInvocatorEvent(endEvent, this.getActivityFlow(), null);
					aci.detach(this.sbbContext.getSbbLocalObject());
					this.getActivityFlow().detach(this.sbbContext.getSbbLocalObject());
				} catch (ParseException e) {
					e.printStackTrace();
				} catch (IOException e) {		
					e.printStackTrace();
				} catch (ParserConfigurationException e) {
					e.printStackTrace();
				} catch (SAXException e) {
					e.printStackTrace();
				}
			} else if(response.getStatusLine().getStatusCode() == 500){	
				System.out.println("Gateway Error!!!");
				System.out.println("***************************************************");
				this.setAnsweredRequest(true);
				
				EndWSInvocatorEvent endEvent = new EndWSInvocatorEvent(null, this.getOperationName(), false);
				this.fireEndWSInvocatorEvent(endEvent, this.getActivityFlow(), null);
				aci.detach(this.sbbContext.getSbbLocalObject());
				this.getActivityFlow().detach(this.sbbContext.getSbbLocalObject());
				
				
				
				/*boolean flag = false;
				int i = 0;
				String newOpName = "";
				
				try {
					Path filePath = Paths.get("/home/julian/JSLEE-Workspace/WebServiceInvocator/lib/Repository-services-backup.txt");
					List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
					while(!flag){
						if(lines.get(i).indexOf(this.getCurrentWsdl()) >= 0){
							this.setCurrentWsdl(lines.get(i+1).substring(0, lines.get(i+1).indexOf("%")));
							newOpName = lines.get(i+1).substring(lines.get(i+1).indexOf("%")+1);
							flag = true;
						} else{
							i++;
						}
					}
					
					startWSInvocatorEvent startWS = new startWSInvocatorEvent(this.getCurrentWsdl(), newOpName, this.getServiceInputs(), null);
					this.fireStartWSInvocatorEvent(startWS, this.getActivityFlow(), null);
					
				} catch (IOException e) {
					e.printStackTrace();
				}*/
			}
		} else{
			System.out.println("No Response received from Repository App!!!!");
			
			this.setAnsweredRequest(true);
			EndWSInvocatorEvent endEvent = new EndWSInvocatorEvent(null, this.getOperationName(), false);
			this.fireEndWSInvocatorEvent(endEvent, this.getActivityFlow(), null);
			aci.detach(this.sbbContext.getSbbLocalObject());
			this.getActivityFlow().detach(this.sbbContext.getSbbLocalObject());
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public HashMap<String, List<String>> getXMLData(Document doc){
		HashMap<String, List<String>> response = new HashMap();
		String pName, pValue = null;
		Element rootElement = doc.getDocumentElement();
		NodeList outputs = rootElement.getElementsByTagName("output");
		for(int i=0; i<outputs.getLength(); i++){
			Element e1 = (Element)outputs.item(i);
			NodeList nl = e1.getElementsByTagName("name");
			Element e2 = (Element)nl.item(0);
			pName = this.getDataFromElement(e2);
			nl = e1.getElementsByTagName("value");
			e2 = (Element)nl.item(0);
			pValue = this.getDataFromElement(e2);
			if(response.containsKey(pName)){
				List<String> values = response.get(pName);
				values.add(pValue);
				response.put(pName, values);
			} else{
				List<String> values = new ArrayList<String>();
				values.add(pValue);
				response.put(pName, values);
			}
		}
		return response;
	}
	
	public String getDataFromElement(Element e) {
	    Node child = e.getFirstChild();
	    if (child instanceof CharacterData) {
	       CharacterData cd = (CharacterData) child;
	       return cd.getData();
	    }
	    return "?";
	}

	public abstract void setActivityFlow(ActivityContextInterface aci);
	public abstract ActivityContextInterface getActivityFlow();
	public abstract void setAnsweredRequest(boolean answeredRequest);
	public abstract boolean getAnsweredRequest();
	public abstract void setCurrentWsdl(String wsdl);
	public abstract String getCurrentWsdl();
	public abstract void setServiceInputs(HashMap<String, String> inputs);
	public abstract HashMap<String, String> getServiceInputs();
	public abstract void setOperationName(String operationName);
	public abstract String getOperationName();
	
	
	// TODO: Perform further operations if required in these methods.
	public void setSbbContext(SbbContext context) { 
		this.sbbContext = context;
		try {
			Context ctx = (Context) new InitialContext().lookup("java:comp/env");
			httpClientAci = (HttpClientActivityContextInterfaceFactory) ctx.lookup("slee/resources/http-client/acifactory");
			raSbbInterface = (HttpClientResourceAdaptorSbbInterface) ctx.lookup("slee/resources/http-client/sbbinterface");
			timerFacility = (TimerFacility) ctx.lookup("slee/facilities/timer");
            nullACIFactory = (NullActivityContextInterfaceFactory)ctx.lookup("slee/nullactivity/activitycontextinterfacefactory");
            nullActivityFactory = (NullActivityFactory)ctx.lookup("slee/nullactivity/factory");
		} catch (NamingException e) {
			e.printStackTrace();
		}
	}
    public void unsetSbbContext() { this.sbbContext = null; }
    
    // TODO: Implement the lifecycle methods if required
    public void sbbCreate() throws javax.slee.CreateException {}
    public void sbbPostCreate() throws javax.slee.CreateException {}
    public void sbbActivate() {}
    public void sbbPassivate() {}
    public void sbbRemove() {}
    public void sbbLoad() {}
    public void sbbStore() {}
    public void sbbExceptionThrown(Exception exception, Object event, ActivityContextInterface activity) {}
    public void sbbRolledBack(RolledBackContext context) {}
	
	public abstract void fireEndWSInvocatorEvent (EndWSInvocatorEvent event, ActivityContextInterface aci, Address address);
	public abstract void fireStartWSInvocatorEvent (StartWSInvocatorEvent event, ActivityContextInterface aci, Address address);

	
	/**
	 * Convenience method to retrieve the SbbContext object stored in setSbbContext.
	 * 
	 * TODO: If your SBB doesn't require the SbbContext object you may remove this 
	 * method, the sbbContext variable and the variable assignment in setSbbContext().
	 *
	 * @return this SBB's SbbContext object
	 */
	
	protected SbbContext getSbbContext() {
		return sbbContext;
	}

	private SbbContext sbbContext; // This SBB's SbbContext

}