/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2014 The ZAP development team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.zaproxy.zap.extension.sequence;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

import org.apache.log4j.Logger;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.control.Control;
import org.parosproxy.paros.core.scanner.AbstractPlugin;
import org.parosproxy.paros.core.scanner.ScannerHook;
import org.parosproxy.paros.extension.ExtensionAdaptor;
import org.parosproxy.paros.extension.ExtensionHook;
import org.parosproxy.paros.network.HttpMessage;
import org.zaproxy.zap.extension.ascan.ExtensionActiveScan;
import org.zaproxy.zap.extension.script.ExtensionScript;
import org.zaproxy.zap.extension.script.ScriptType;
import org.zaproxy.zap.extension.script.ScriptWrapper;
import org.zaproxy.zap.extension.script.SequenceScript;

public class ExtensionSequence extends ExtensionAdaptor implements ScannerHook {

	private ExtensionScript extScript;
	private ExtensionActiveScan extActiveScan;
	public static final Logger logger = Logger.getLogger(ExtensionSequence.class);
	public static final ImageIcon ICON = new ImageIcon(ExtensionSequence.class.getResource("/org/zaproxy/zap/extension/sequence/resources/icons/script-sequence.png"));
	public static final String TYPE_SEQUENCE = "sequence";

	private List<ScriptWrapper> enabledScripts = null;

	public ExtensionSequence() {
		super("ExtensionSequence");
		this.setOrder(29);
	}

	@Override
	public String getAuthor() {
		return Constant.ZAP_TEAM;
	}

	@Override
	public void scannerComplete() {
		//Reset the sequence extension
		this.enabledScripts = null;
		getExtActiveScan().setIncludedSequenceScripts(null);
	}

	@Override
	public void hook(ExtensionHook extensionhook) {
		super.hook(extensionhook);
		//Create a new sequence script type and register
		ScriptType type = new ScriptType(TYPE_SEQUENCE, "script.type.sequence", ICON, false);
		getExtScript().registerScriptType(type);
		
		extensionhook.getHookMenu().addPopupMenuItem(new SequencePopupMenuItem(this));

		//Add class as a scannerhook (implements the scannerhook interface)
		extensionhook.addScannerHook(this);
	}

	@Override
	public void beforeScan(HttpMessage msg, AbstractPlugin plugin) {
		//If the HttpMessage has a HistoryReference with an ID that is also in the HashMap of the Scanner,
		//then the message has a specific Sequence script to scan.
		SequenceScript seqScr = getIncludedSequenceScript(msg);

		//If any script was found, send all the requests prior to the message to be scanned.
		if(seqScr!= null) {
			HttpMessage newMsg = seqScr.runSequenceBefore(msg, plugin);
			updateMessage(msg, newMsg);
		}
	}

	@Override
	public void afterScan(HttpMessage msg, AbstractPlugin plugin) {
		//If the HttpMessage has a HistoryReference with an ID that is also in the HashMap of the Scanner,
		//then the message has a specific Sequence script to scan.
		SequenceScript seqScr = getIncludedSequenceScript(msg);

		//If any script was found, send all the requests after the message that was scanned.
		if(seqScr!= null) {
			seqScr.runSequenceAfter(msg, plugin);
		}
	}

	private SequenceScript getIncludedSequenceScript(HttpMessage msg) {
		List<ScriptWrapper> sequences = getSelectedSequenceScripts();
		for(ScriptWrapper wrapper: sequences) {
			try {
				SequenceScript seqScr = getExtScript().getInterface(wrapper, SequenceScript.class); 
				if(seqScr != null) {
					if(seqScr.isPartOfSequence(msg)) {
						return seqScr;
					}
				}
			} catch (Exception e) {
				logger.debug("Exception occurred, while trying to fetch Included Sequence Script: " + e.getMessage());
			}
		}
		return null;
	}
	
	public void setDirectScanScript(ScriptWrapper script) {
		enabledScripts = new ArrayList<ScriptWrapper>();
		enabledScripts.add(script);
	}

	private void updateMessage(HttpMessage msg, HttpMessage newMsg) {
		msg.setRequestHeader(newMsg.getRequestHeader());
		msg.setRequestBody(newMsg.getRequestBody());
		msg.setCookies(new ArrayList<HttpCookie>());
	}

	private ExtensionScript getExtScript() {
		if(extScript == null) {
			extScript = (ExtensionScript) Control.getSingleton().getExtensionLoader().getExtension(ExtensionScript.class);
		}
		return extScript;
	}

	private ExtensionActiveScan getExtActiveScan(){
		if(extActiveScan == null){
			extActiveScan = (ExtensionActiveScan) Control.getSingleton().getExtensionLoader().getExtension(ExtensionActiveScan.class);
		}
		return extActiveScan;
	}

	private List<ScriptWrapper> getSelectedSequenceScripts() {
		if(enabledScripts == null) {
			enabledScripts = getExtActiveScan().getIncludedSequenceScripts();
			if(enabledScripts == null) {
				enabledScripts = new ArrayList<ScriptWrapper>();
			}
		}
		return enabledScripts;
	}
}