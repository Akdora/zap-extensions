/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
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
package org.zaproxy.zap.extension.ascanrules;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Random;
import org.apache.log4j.Logger;
import org.parosproxy.paros.core.scanner.AbstractAppParamPlugin;
import org.parosproxy.paros.core.scanner.Alert;
import org.parosproxy.paros.core.scanner.Category;
import org.parosproxy.paros.network.HttpMessage;

/**
 * Active Plugin for Code Injection testing and verification.
 * https://www.owasp.org/index.php/Code_Injection
 * 
 * @author yhawke (2013)
 */
public class CodeInjectionPlugin extends AbstractAppParamPlugin {

    // PHP control Token used to verify the vulnerability
    private static final String PHP_CONTROL_TOKEN = "zap_token";
    private static final String PHP_ENCODED_TOKEN = "chr(122).chr(97).chr(112).chr(95).chr(116).chr(111).chr(107).chr(101).chr(110)";
    
    // PHP payloads for Code Injection testing
    // to avoid reflective values mis-interpretation
    // we evaluate the content value inside the response
    // concatenating single ascii characters using the chr function
    // In this way we can avoid some input checking like backslash or apics
    private static final String[] PHP_PAYLOADS = {
        "\";print(" + PHP_ENCODED_TOKEN + ");$var=\"",
        "';print(" + PHP_ENCODED_TOKEN + ");$var='",
        "${@print(" + PHP_ENCODED_TOKEN + ")}",
        "${@print(" + PHP_ENCODED_TOKEN + ")}\\",
        ";print(" + PHP_ENCODED_TOKEN + ");"
    };

    // ASP payloads for Code Injection testing
    // to avoid reflective values mis-interpretation
    // we evaluate the content value inside the response
    // multiplicating two random 7-digit numbers
    private static final String[] ASP_PAYLOADS = {
        "\"+response.write([{0}*{1})+\"",
        "'+response.write({0}*{1})+'",
        "response.write({0}*{1})"
    };
    
    // Logger instance
    private static final Logger log 
            = Logger.getLogger(CodeInjectionPlugin.class);
    
    /**
     * Get the unique identifier of this plugin
     * @return this plugin identifier
     */
    @Override
    public int getId() {
        return 90019;    
    }

    /**
     * Get the name of this plugin
     * @return the plugin name
     */
    @Override
    public String getName() {
        return "Server Side Code Injection Plugin";
    }
    
    /**
     * Give back specific pugin dependancies (none for this)
     * @return the list of plugins that need to be executed before
     */
    @Override
    public String[] getDependency() {
        return new String[]{};
    }

    /**
     * Get the description of the vulnerbaility when found
     * @return the vulnerability description
     */
    @Override
    public String getDescription() {
        return "A code injection may be possible including custom code that will be evaluated by the scripting engine";
    }

    /**
     * Give back the categorization of the vulnerability 
     * checked by this plugin (it's an injection category for CODEi)
     * @return a category from the Category enum list 
     */    
    @Override
    public int getCategory() {
        return Category.INJECTION;
    }

    /**
     * Give back a general solution for the found vulnerability
     * @return the solution that can be put in place
     */
    @Override
    public String getSolution() {
        return "Do not trust client side input, even if there is client side validation in place.\n"
                + "In general, type check all data on the server side and "
                + "escape all data received from the client.\n"
                + "Avoid the use of eval() functions combined with user input data.";
    }

    /**
     * Reports all links and documentation which refers to this vulnerability
     * @return a string based list of references
     */
    @Override
    public String getReference() {
        return "http://cwe.mitre.org/data/definitions/94.html\n"
                + "https://www.owasp.org/index.php/Direct_Dynamic_Code_Evaluation_('Eval_Injection')";
    }

    /**
     * http://cwe.mitre.org/data/definitions/94.html
     * @return the official CWE id
     */
    @Override
    public int getCweId() {
        return 94;
    }

    /**
     * Seems no WASC defined for this
     * @return the official WASC id
     */
    @Override
    public int getWascId() {
        return 0;
    }

    /**
     * Give back the risk associated to this vulnerability (high)
     * @return the risk according to the Alert enum
     */
    @Override
    public int getRisk() {
        return Alert.RISK_HIGH;
    }    

    /**
     * Initialize the plugin according to
     * the overall environment configuration
     */
    @Override
    public void init() {
        // do nothing
    }

    /**
     * Scan for Code Injection Vulnerabilites
     * 
     * @param msg a request only copy of the original message (the response isn't copied)
     * @param parameter the parameter name that need to be exploited
     * @param value the original parameter value
     */
    @Override
    public void scan(HttpMessage msg, String paramName, String value) {

        // Begin plugin execution
        if (log.isDebugEnabled()) {
            log.debug("Checking [" + msg.getRequestHeader().getMethod() + "][" 
                    + msg.getRequestHeader().getURI() 
                    + "], parameter [" + paramName 
                    + "] for Dynamic Code Injection vulnerabilites");
        }

        // ------------------------------------------
        // Start testing PHP Code Injection patterns
        // ------------------------------------------
        for (String phpPayload : PHP_PAYLOADS) {
            msg = getNewMsg();
            setParameter(msg, paramName, phpPayload);

            if (log.isTraceEnabled()) {
                log.trace("Testing [" + paramName + "] = [" + phpPayload + "]");
            }
            
            try {
                // Send the request and retrieve the response
                sendAndReceive(msg, false);
                
                // Check if the injected content has been evaluated and printed
                if (msg.getResponseBody().toString().contains(PHP_CONTROL_TOKEN)) {
                    // We Found IT!                     
                    // First do logging
                    log.info("[PHP Code Injection Found] on parameter [" + paramName 
                            + "] with payload [" + phpPayload + "]");
                    
                    // Now create the alert message
                    this.bingo(
                            Alert.RISK_HIGH, 
                            Alert.WARNING, 
                            getName() + " - PHP Code Injection",
                            getDescription(),
                            null,
                            paramName,
                            phpPayload, 
                            null,
                            getSolution(),
                            msg);
                    
                    // All done. No need to look for vulnerabilities on subsequent 
                    // parameters on the same request (to reduce performance impact)
                    return;                 
                }

            } catch (IOException ex) {
                //Do not try to internationalise this.. we need an error message in any event..
                //if it's in English, it's still better than not having it at all.
                log.error("PHP Code Injection vulnerability check failed for parameter ["
                    + paramName + "] and payload [" + phpPayload + "] due to an I/O error", ex);
            }
        }
            
        // ------------------------------------------
        // Continue testing ASP Code Injection patterns
        // ------------------------------------------
        Random rand = new Random();
        int bignum1 = 100000 + (int)(rand.nextFloat()*(999999 - 1000000 + 1));
        int bignum2 = 100000 + (int)(rand.nextFloat()*(999999 - 1000000 + 1));
        
        for (String aspPayload : ASP_PAYLOADS) {
            msg = getNewMsg();
            setParameter(msg, paramName, MessageFormat.format(aspPayload, bignum1, bignum2));
            
            if (log.isTraceEnabled()) {
                log.trace("Testing [" + paramName + "] = [" + aspPayload + "]");
            }

            try {
                // Send the request and retrieve the response
                sendAndReceive(msg, false);
                
                // Check if the injected content has been evaluated and printed
                if (msg.getResponseBody().toString().contains(Integer.toString(bignum1*bignum2))) {
                    // We Found IT!
                    // First do logging
                    log.info("[ASP Code Injection Found] on parameter [" + paramName 
                            + "]  with payload [" + aspPayload + "]");
                    
                    // Now create the alert message
                    this.bingo(
                            Alert.RISK_HIGH, 
                            Alert.WARNING, 
                            getName() + " - ASP Code Injection",
                            getDescription(),
                            null,
                            paramName,
                            aspPayload, 
                            null,
                            getSolution(),
                            msg);
                }

            } catch (IOException ex) {
                //Do not try to internationalise this.. we need an error message in any event..
                //if it's in English, it's still better than not having it at all.
                log.error("ASP Code Injection vulnerability check failed for parameter ["
                    + paramName + "] and payload [" + aspPayload + "] due to an I/O error", ex);
            }
        }       
    }
}
