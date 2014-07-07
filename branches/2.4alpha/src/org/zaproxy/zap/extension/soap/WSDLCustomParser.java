package org.zaproxy.zap.extension.soap;

import groovy.xml.MarkupBuilder;

import java.awt.EventQueue;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.httpclient.URI;
import org.apache.log4j.Logger;
import org.codehaus.groovy.runtime.metaclass.MissingPropertyExceptionNoStack;
import org.parosproxy.paros.control.Control;
import org.parosproxy.paros.extension.history.ExtensionHistory;
import org.parosproxy.paros.model.HistoryReference;
import org.parosproxy.paros.model.Model;
import org.parosproxy.paros.network.HttpHeader;
import org.parosproxy.paros.network.HttpMessage;
import org.parosproxy.paros.network.HttpRequestHeader;
import org.parosproxy.paros.network.HttpSender;
import org.parosproxy.paros.view.View;
import org.zaproxy.zap.network.HttpRequestBody;

import com.predic8.schema.ComplexType;
import com.predic8.schema.Element;
import com.predic8.schema.Schema;
import com.predic8.wsdl.AbstractBinding;
import com.predic8.wsdl.Binding;
import com.predic8.wsdl.BindingOperation;
import com.predic8.wsdl.Definitions;
import com.predic8.wsdl.Operation;
import com.predic8.wsdl.Part;
import com.predic8.wsdl.Port;
import com.predic8.wsdl.PortType;
import com.predic8.wsdl.Service;
import com.predic8.wsdl.WSDLParser;
import com.predic8.wstool.creator.RequestCreator;
import com.predic8.wstool.creator.SOARequestCreator;

public class WSDLCustomParser {

	private static final Logger log = Logger.getLogger(WSDLCustomParser.class);
	private static int keyIndex = -1;
	private ImportWSDL wsdlSingleton=ImportWSDL.getInstance();
	
	public WSDLCustomParser(){
		
	}
	
	/* Method called from external classes to import a WSDL file from an URL. */
	public void extUrlWSDLImport(final String url, final String threadName){
		if (url == null || url.trim().length() <= 0 ) return;
		//log.debug("Importing WSDL file from URL: "+url);
		Thread t = new Thread(){
			@Override
			public void run() {
				// Thread name: THREAD_PREFIX + threadId++
				this.setName(threadName);
	    		parseWSDLUrl(url);
			}
    		
    	};
    	t.start();
	}	
	
	/* Method called from external classes to import a WSDL file from an URL. */
	public void extUrlWSDLImport(final String url){
		parseWSDLUrl(url);
	}
	
	/* Method called from external classes to import a WSDL file from a local file. */
	public void extFileWSDLImport(final File file, final String threadName){
		Thread t = new Thread(){
			@Override
			public void run() {
				this.setName(threadName);
	    		parseWSDLFile(file);
			}
    		
    	};
    	t.start();
	}
	
	/* Generates WSDL definitions from a WSDL file and then it calls parsing functions. */
	private void parseWSDLFile(File file) {
		if (file == null) return ;
		try {
			if (View.isInitialised()) {
				// Switch to the output panel, if in GUI mode
				View.getSingleton().getOutputPanel().setTabFocus();
			}
			
			// WSDL file parsing.
	        WSDLParser parser = new WSDLParser();
			final String path = file.getAbsolutePath();
	        Definitions wsdl = parser.parse(path);
			parseWSDL(wsdl);
	        
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} 
	}
	
	/* Generates WSDL definitions from a WSDL string and then it calls parsing functions. */
	private void parseWSDLUrl(String url){
		if (url == null || url.trim().equals("")) return;
		try {
			if (View.isInitialised()) {
				// Switch to the output panel, if in GUI mode
				View.getSingleton().getOutputPanel().setTabFocus();
			}
			/* Sends a request to retrieve remote WSDL file's content. */
			HttpMessage httpRequest = new HttpMessage(new URI(url, false));
	        HttpSender sender = new HttpSender(
					Model.getSingleton().getOptionsParam().getConnectionParam(),
					true,
					HttpSender.MANUAL_REQUEST_INITIATOR);
	        try {
				sender.sendAndReceive(httpRequest, true);
			} catch (IOException e) {
				log.error("Unable to send WSDL request.", e);
				return;
			}
	        
	        /* Checks response content. */
	        if (httpRequest.getResponseBody() != null){      	
		        String content = httpRequest.getResponseBody().toString();	
		        if (content == null || content.trim().length() <= 0){
		        	//log.warn("URL response from WSDL file request has no body content.");
		        }else{
		        	// WSDL parsing.
			        WSDLParser parser = new WSDLParser();
			        InputStream contentI = new ByteArrayInputStream(content.getBytes("UTF-8"));
			        Definitions wsdl = parser.parse(contentI);
			        contentI.close();
					parseWSDL(wsdl);
		        }  
	        }
		} catch (Exception e) {
			log.error("There was an error while parsing WSDL from URL. ", e);
		} 
	}


	/* Parses WSDL definitions and identifies endpoints and operations. */
	private void parseWSDL(Definitions wsdl){
        StringBuilder sb = new StringBuilder();
        List<Service> services = wsdl.getServices();
        keyIndex++;
        
        /* Endpoint identification. */
        for(Service service : services){
        	for(Port port: service.getPorts()){
		        Binding binding = port.getBinding();
		        AbstractBinding innerBinding = binding.getBinding();
		        String soapPrefix = innerBinding.getPrefix();
		        int soapVersion = detectSoapVersion(wsdl, soapPrefix); // SOAP 1.X, where X is represented by this variable.			        
		        /* If the binding is not a SOAP binding, it is ignored. */
		        String style = detectStyle(innerBinding);
		        if(style != null && (style.equals("document") || style.equals("rpc")) ){
		        	
			        List<BindingOperation> operations = binding.getOperations();
			        String endpointLocation = port.getAddress().getLocation().toString();
				    sb.append("\n|-- Port detected: "+port.getName()+" ("+endpointLocation+")\n");
				    
		    	    /* Identifies operations for each endpoint.. */
	    	        for(BindingOperation bindOp : operations){
	    	        	sb.append("|\t|-- SOAP 1."+soapVersion+" Operation: "+bindOp.getName());
	    	        	/* Adds this operation to the global operations chart. */
	    	        	recordOperation(keyIndex, bindOp);	    	        	
	    	        	/* Identifies operation's parameters. */
	    	        	List<Part> requestParts = detectParameters(wsdl, bindOp);    	        			    	        	    	        	   	        			    	        	
	    	        	/* Set values to parameters. */
	    	        	HashMap<String, String> formParams = fillParameters(requestParts); 
	    	        	/* Connection test for each operation. */
	    	        	/* Basic message creation. */
	    	        	SOAPMsgConfig soapConfig = new SOAPMsgConfig(wsdl, soapVersion, formParams, port, bindOp);
	    	        	HttpMessage requestMessage = createSoapRequest(soapConfig);
	    	        	sendSoapRequest(keyIndex, requestMessage, sb);	
	    	        } //bindingOperations loop
		        } //Binding check if
        	}// Ports loop
        }        
        printOutput(sb);
	}
	
	/* Detects SOAP version used in a binding, given the wsdl content and the soap binding prefix. */
	private int detectSoapVersion(Definitions wsdl, String soapPrefix){
        String soapNamespace = wsdl.getNamespace(soapPrefix).toString();
        if(soapNamespace.trim().equals("http://schemas.xmlsoap.org/wsdl/soap12/")){
        	return 2;
        }else{
        	return 1;
        }
	}
	
	private String detectStyle(AbstractBinding binding){
        try{
        	String r = binding.getProperty("style").toString();
        	binding.getProperty("transport");
        	return r.trim();
        }catch (MissingPropertyExceptionNoStack e){
        	// It has no style or transport property, so it is not a SOAP binding.
        	log.info("No style or transport property detected", e);
        	return null;
        }
	}
	
	/* Record the given operation in the global chart. */
	private void recordOperation(int wsdlID, BindingOperation bindOp){
    	String soapActionName = "";
    	try{
    		soapActionName = bindOp.getOperation().getSoapAction();    	        			
    	}catch(NullPointerException e){
    		// SOAP Action not defined for this operation.
    		log.info("No SOAP Action defined for this operation.", e);
    		return;
    	}
    	if(!soapActionName.trim().equals("")){
    		wsdlSingleton.putAction(wsdlID,soapActionName);
    	}	
	}
	
	private List<Part> detectParameters(Definitions wsdl, BindingOperation bindOp){
		for(PortType pt : wsdl.getPortTypes()){
    		for(Operation op : pt.getOperations()){
    			if (op.getName().trim().equals(bindOp.getName().trim())){
    				return op.getInput().getMessage().getParts();
    			}
    		}
    	}
		return null;
	}
	
	private HashMap<String, String> fillParameters(List<Part> requestParts){
		HashMap<String, String> formParams = new HashMap<String, String>();
    	for(Part part : requestParts){
    		if (part.getName().equals("parameters")){
    			final String elementName = part.getElement().getName();
        		ComplexType ct = (ComplexType) part.getElement().getEmbeddedType();
        		/* Handles when ComplexType is not embedded but referenced by 'type'. */
        		if (ct == null){
        			Element element = part.getElement();
        			Schema currentSchema = element.getSchema();
        			ct = (ComplexType) currentSchema.getType(element.getType());
        		}			    	        			
        		for (Element e : ct.getSequence().getElements()) {
        			final String paramName = e.getName().trim();
        			log.info("Detected parameter name: "+paramName);
        			String paramType = e.getType().getQualifiedName().trim();
        			if(paramType.contains(":")){
        				String[] stringParts = paramType.split(":");
        				paramType = stringParts[stringParts.length-1];
        			}
        			/* Parameter value depends on parameter type. */
        			if(paramType.equals("string")){
	        			formParams.put("xpath:/"+elementName.trim()+"/"+paramName, "paramValue");
	        		}else if(paramType.equals("int") || paramType.equals("double") ||
	        				paramType.equals("long")){
	        			formParams.put("xpath:/"+elementName.trim()+"/"+paramName, "0");
	        		}else if(paramType.equals("date")){
	        			Date date = new Date();
	        			SimpleDateFormat dt1 = new SimpleDateFormat("CCYY-MM-DD");
	        			String dateS = dt1.format(date);
	        			formParams.put("xpath:/"+elementName.trim()+"/"+paramName, dateS);
	        		}else if(paramType.equals("dateTime")){
	        			Date date = new Date();
	        			SimpleDateFormat dt1 = new SimpleDateFormat("CCYY-MM-DDThh:mm:ssZ");
	        			String dateS = dt1.format(date);
	        			formParams.put("xpath:/"+elementName.trim()+"/"+paramName, dateS);
	        		}
        		}
    		}
    	}
    	return formParams;
	}

	/* Generates a SOAP request associated to the specified binding operation. */
	public HttpMessage createSoapRequest(SOAPMsgConfig soapConfig){
		
		/* Retrieving configuration variables. */
		Definitions wsdl = soapConfig.getWsdl();
		HashMap<String,String> formParams= soapConfig.getParams();
		Port port = soapConfig.getPort();
		int soapVersion = soapConfig.getSoapVersion();
		BindingOperation bindOp = soapConfig.getBindOp();
		
		/* Start message crafting. */
    	StringWriter writerSOAPReq = new StringWriter();
    	
    	SOARequestCreator creator = new SOARequestCreator(wsdl, new RequestCreator(), new MarkupBuilder(writerSOAPReq));
        creator.setBuilder(new MarkupBuilder(writerSOAPReq));
        creator.setDefinitions(wsdl);
        creator.setFormParams(formParams);
        creator.setCreator(new RequestCreator());
    	
    	try{
    		Binding binding = port.getBinding();
	        creator.createRequest(binding.getPortType().getName(),
	               bindOp.getName(), binding.getName());
	            	        	
	        //log.info("[ExtensionImportWSDL] "+writerSOAPReq);
	        /* HTTP Request. */
	        String endpointLocation = port.getAddress().getLocation().toString();
	        HttpMessage httpRequest = new HttpMessage(new URI(endpointLocation, false));
	        /* Body. */
	        HttpRequestBody httpReqBody = httpRequest.getRequestBody();
	        /* [MARK] Not sure if all servers would handle this encoding type. */
	        httpReqBody.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n"+ writerSOAPReq.getBuffer().toString());
	        httpRequest.setRequestBody(httpReqBody);
	        /* Header. */    		       
	        HttpRequestHeader httpReqHeader = httpRequest.getRequestHeader();
	        httpReqHeader.setMethod("POST");
	        /* Sets headers according to SOAP version. */
	        if(soapVersion == 1){
		        httpReqHeader.setHeader(HttpHeader.CONTENT_TYPE, "text/xml; charset=UTF-8");
		        httpReqHeader.setHeader("SOAPAction", bindOp.getOperation().getSoapAction());
	        }else if (soapVersion == 2){
	        	String contentType = "application/soap+xml; charset=UTF-8";
	        	String action = bindOp.getOperation().getSoapAction();
	        	if(!action.trim().equals(""))
	        		contentType += "; action="+action;
	        	httpReqHeader.setHeader(HttpHeader.CONTENT_TYPE, contentType);
	        }
	        httpReqHeader.setContentLength(httpReqBody.length());
	        httpRequest.setRequestHeader(httpReqHeader);
	        /* Saves the message and its configuration. */
	        wsdlSingleton.putConfiguration(httpRequest, soapConfig);
	        return httpRequest;
    	}catch (Exception e){
    		log.error("Unable to generate request for operation '"+bindOp.getName()+"'\n"+ e.getMessage(), e);
    		return null;
    	}
	}
	
	/* Sends a given SOAP request. File is needed to record its associated ops, and stringBuilder logs
	 * the output message.
	 */
	private void sendSoapRequest(int wsdlID, HttpMessage httpRequest, StringBuilder sb){
		if (httpRequest == null) return;
        /* Connection. */
        HttpSender sender = new HttpSender(
				Model.getSingleton().getOptionsParam().getConnectionParam(),
				true,
				HttpSender.MANUAL_REQUEST_INITIATOR);
        try {
			sender.sendAndReceive(httpRequest, true);
		} catch (IOException e) {
			log.error("Unable to communicate with SOAP server. Server may be not available.");
			log.debug("Trace:", e);
		}
		wsdlSingleton.putRequest(wsdlID, httpRequest);
		persistMessage(httpRequest);
		if (sb != null) sb.append(" (Status code: "+ httpRequest.getResponseHeader().getStatusCode() +")\n");
	}
	
	private static void persistMessage(final HttpMessage message) {
		// Add the message to the history panel and sites tree
		final HistoryReference historyRef;

		try {
			historyRef = new HistoryReference(Model.getSingleton().getSession(), HistoryReference.TYPE_ZAP_USER, message);			
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			return;
		}

		final ExtensionHistory extHistory = (ExtensionHistory) Control.getSingleton()
				.getExtensionLoader().getExtension(ExtensionHistory.NAME);
		if (extHistory != null) {
			EventQueue.invokeLater(new Runnable() {
				@Override
				public void run() {
					extHistory.addHistory(historyRef);
					Model.getSingleton().getSession().getSiteTree().addPath(historyRef, message);	
				}
			});
		}
	}
	
	/* Prints output string in output panel. */
	private void printOutput(StringBuilder sb){
        if (View.isInitialised()) {
			final String str = sb.toString();
			EventQueue.invokeLater(new Runnable() {
				@Override
				public void run() {
					View.getSingleton().getOutputPanel().append(str);
				}}
			);
		}	
	}
}