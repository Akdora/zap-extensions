# Passive Scan Rules - Alpha #
The following alpha quality passive scan rules are included in this add-on:
## ASP.NET ViewState Disclosure ##
An ASP.NET ViewState was disclosed by the application/web server
## ASP.NET ViewState Integrity ##
The application does not use a Message Authentication Code (MAC) to protect the integrity of the ASP.NET
ViewState, which can be tampered with by a malicious client
## Base64 Disclosure ##
Base64 encoded data was disclosed by the application/web server
## Missing Content Security Policy Header ##
This checks response headers for the presence of a Content Security Policy header.
## Cookie poisoning ##
This check looks at user-supplied input in query string parameters and POST data to identify where cookie
parameters might be controlled. This is called a cookie poisoning attack, and becomes exploitable when
an attacker can manipulate the cookie in various ways. In some cases this will not be exploitable, however,
allowing URL parameters to set cookie values is generally considered a bug.
## Cross Domain Misconfiguration ##
Passively scan responses for Cross Domain MisConfigurations, which relax the Same Origin Policy in the
web browser, for instance.<br>The current implementation looks at excessively permissive CORS headers.<br>
<h2>Directory Browsing</h2>
Passively scan responses for signatures that are indicative that Directory Browsing is possible.<br>
<h2>Example File Passive Scanner</h2>
This implements an example passive scan rule that loads strings from a file that the user can edit.<br>For<br>
more details see: <a href='http://zaproxy.blogspot.co.uk/2014/04/hacking-zap-3-passive-scan-rules.html'>http://zaproxy.blogspot.co.uk/2014/04/hacking-zap-3-passive-scan-rules.html</a>
<h2>Example Simple Passive Scanner</h2>
This implements a very simple example passive scan rule.<br>For more details see: <a href='http://zaproxy.blogspot.co.uk/2014/04/hacking-zap-3-passive-scan-rules.html'>http://zaproxy.blogspot.co.uk/2014/04/hacking-zap-3-passive-scan-rules.html</a>
<h2>Hash Disclosure</h2>
Passively scan for password hashes disclosed by the web server. <br>Various formats are including, including<br>
some formats such as MD4, MD5, and SHA<code>*</code>, which are sometimes used for purposes other than to contain<br>
password hashes.<br>
<h2>HeartBleed</h2>
Passively scans for HTTP header responses that may indicate that the server is vulnerable to the critical<br>
HeartBleed OpenSSL vulnerability.<br>
<h2>HTTP to HTTPS insecure transition in form post</h2>
This check looks for insecure HTTP pages that host HTTPS forms. The issue is that an insecure HTTP page<br>
can easily be hijacked through MITM and the secure HTTPS form can be replaced or spoofed.<br>
<h2>HTTPS to HTTP insecure transition in form post</h2>
This check identifies secure HTTPS pages that host insecure HTTP forms. The issue is that a secure page<br>
is transitioning to an insecure page when data is uploaded through a form. The user may think they're<br>
submitting data to a secure page when in fact they are not.<br>
<h2>Insecure Component</h2>
Passively scans the server response headers and body generator content for product versions, which are<br>
then cross-referenced against a list of product versions known to be vulnerable to various CVEs.<br>A list<br>
of ranked CVEs and CVSS severity scores is output for each product noted to be vulnerable.<br>Currently,<br>
the following server side products are supported:<br>Apache Tomcat application server (limited functionality<br>
due to limited information leakage by Tomcat)<br>Apache web server<br>mod<code>_</code>auth<code>_</code>radius Apache module<br>mod<code>_</code>fcgid<br>
Apache module<br>mod<code>_</code>imap Apache module<br>mod<code>_</code>jk Apache module<br>mod<code>_</code>perl Apache module<br>mod<code>_</code>python Apache module<br>mod<code>_</code>ssl<br>
Apache module<br>OpenSSL Apache module<br>Perl Apache module<br>Python Apache module<br>IBM HTTP Server<br>JBoss application<br>
server<br>Jetty web server / application server<br>LiteSpeed web server<br>Lighttpd web server<br>Microsoft IIS web server<br>Netscape<br>
Enterprise web server<br>Nginx web server<br>OpenCMS<br>Oracle Application Server<br>Oracle Web Cache<br>PHP<br>Phusion<code>_</code>Passenger<br>Squid<br>
proxy server<br>Sun One web server<br>Sun Java System Web Server<br>TornadoServer web server<br>WordPress<br>
<h2>Image Location Scanner</h2>

Passively scans for GPS location exposure in images during normal security assessments of websites. Image<br>
Location Scanner assists in situations where end users may post profile images and possibly give away<br>
their home location, e.g. a dating site or children's chatroom. A whitepaper on this can be found at<br>
<a href='http://veggiespam.com/ils/'>http://veggiespam.com/ils/</a>

Note: In order for this plug-in to operate, ZAP must be configured to receive and process images. To<br>
do this, go to the ZAP Options panel (Tools → Options), choose Display, and enable the checkbox<br>
for "Process images in HTTP requests/responses". In addition, ZAP images cannot be filtered out via the<br>
settings "Global Exclude URL" or the Session Properties for "Exclude from Proxy".<br>
<h2>Open redirect</h2>
Open redirects are one of the OWASP 2010 Top Ten vulnerabilities. This check looks at user-supplied input<br>
in query string parameters and POST data to identify where open redirects might be possible. Open redirects<br>
occur when an application allows user-supplied input (e.g. <a href='http://nottrusted.com'>http://nottrusted.com</a>) to control an offsite<br>
redirect. This is generally a pretty accurate way to find where 301 or 302 redirects could be exploited<br>
by spammers or phishing attacks<br>
<h2>Server Header Version Information Leak</h2>
This checks response headers for the presence of a server header that contains version details.<br>
<h2>Source Code Disclosure</h2>
Application Source Code was disclosed by the web server<br>
<h2>Missing Strict Transport Security Header</h2>
This checks HTTPS response headers for the presence of a HTTP Strict Transport Security header.<br>
<h2>Timestamp Disclosure</h2>
A timestamp was disclosed by the application/web server<br>
<h2>User controllable HTML element attribute (potential XSS)</h2>
This check looks at user-supplied input in query string parameters and POST data to identify where certain<br>
HTML attribute values might be controlled. This provides hot-spot detection for XSS (cross-site scripting)<br>
that will require further review by a security analyst to determine exploitability.<br>
<h2>User controllable charset</h2>
This check looks at user-supplied input in query string parameters and POST data to identify where Content-Type<br>
or meta tag charset declarations might be user-controlled. Such charset declarations should always be<br>
declared by the application. If an attacker can control the response charset, they could manipulate the<br>
HTML to perform XSS or other attacks.<br>
<h2>User controllable javascript event (XSS)</h2>
This check looks at user-supplied input in query string parameters and POST data to identify where certain<br>
HTML attribute values might be controlled. This provides hot-spot detection for XSS (cross-site scripting)<br>
that will require further review by a security analyst to determine exploitability.<br>
<h2>User controllable javascript property (XSS)</h2>
This check looks at user-supplied input in query string parameters and POST data to identify where URL's<br>
in certain javascript properties (e.g. createElement src) might becontrolled. This provides hot-spot<br>
detection for XSS (cross-site scripting) that will require further review by a security analyst to determine<br>
exploitability.<br>
<h2>X-Powered-By Header Information Leak</h2>
This checks response headers for the presence of X-Powered-By details.<br>
<h2>X-Backend-Server Header Information Leak</h2>
This checks response headers for the presence of X-Backend-Server details.<br>
<h2>Big Redirect</h2>
This check predicts the size of various redirect type responses and generates an alert if the response<br>
is greater than the predicted size. A large redirect response may indicate that although a redirect has<br>
taken place the page actually contained content (which may reveal sensitive information, PII, etc).