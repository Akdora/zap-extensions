# Active Scan Rules - alpha #
The following alpha quality active scan rules are included in this add-on:
## Buffer Overflow ##
Looks for indicators of buffer overflows in complied code. It does this by putting out large strings
of input text and look for code crash and abnormal session closure.
## Cookie Slack Detector ##
Tests cookies to detect if some have no effect on response size when omitted, especially cookies containing
the name "session" or "userid"
## Example File Active Scanner ##
This implements an example active scan rule that loads strings from a file that the user can edit.<br>For<br>
more details see: <a href='http://zaproxy.blogspot.co.uk/2014/04/hacking-zap-4-active-scan-rules.html'>http://zaproxy.blogspot.co.uk/2014/04/hacking-zap-4-active-scan-rules.html</a>
<h2>Example Simple Active Scanner</h2>
This implements a very simple example active scan rule.<br>For more details see: <a href='http://zaproxy.blogspot.co.uk/2014/04/hacking-zap-4-active-scan-rules.html'>http://zaproxy.blogspot.co.uk/2014/04/hacking-zap-4-active-scan-rules.html</a>
<h2>Expression Language Injection</h2>
The software constructs all or part of an expression language (EL) statement in a Java Server Page (JSP)<br>
using externally-influenced input from an upstream component, but it does not neutralize or incorrectly<br>
neutralizes special elements that could modify the intended EL statement before it is executed. In certain<br>
versions of Spring 3.0.5 and earlier, there was a vulnerability (CVE-2011-2730) in which Expression Language<br>
tags would be evaluated twice, which effectively exposed any application to EL injection. However, even<br>
for later versions, this weakness is still possible depending on configuration.<br>
<h2>Source Code Disclosure - File Inclusion</h2>
Uses local file inclusion techniques to scan for files containing source code on the web server.<br>
<h2>Source Code Disclosure - Git</h2>
Uses Git source code repository metadata to scan for files containing source code on the web server.<br>
<h2>HTTPS As HTTP Scanner</h2>
This active scanner attempts to access content that was originally accessed via HTTPS (SSL/TLS) via HTTP.