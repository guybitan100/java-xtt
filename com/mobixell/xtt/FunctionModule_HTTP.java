package com.mobixell.xtt;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * FunctionModule_HTTP provides HTTP and HTTPS GET functions.
 *
 * @author Roger Soder
 * @version $Id: FunctionModule_HTTP.java,v 1.65 2010/05/05 08:12:36 rajesh Exp $
 */
public class FunctionModule_HTTP extends FunctionModule {
    private Map<String, HTTPConnection> connections = Collections.synchronizedMap(new HashMap<String, HTTPConnection>());
    private WebServer s = null;
    private Thread ws = null;
    private HTTPConnection defaultConnection = null;

    /**
     * clears and reinitializes all the variables. Does reset the proxy.
     */
    public void initialize() {
        defaultConnection = new HTTPConnection("DEFAULT", "HTTP");
        defaultConnection.readConfiguration();

        WebWorker.setServerSendHeader(new LinkedHashMap<String, String>());
        WebWorker.init();
        WebServer.resetWorkerId();
        WebWorker.setBufferOutputSize(XTTProperties.getIntProperty("BUFFEROUTPUTSIZE"));
        XTTProperties.printDebug(this.getClass().getName() + ".initialize(): clearing variables");
    }


    /**
     * constructor sets proxy.
     */
    public FunctionModule_HTTP() {
        // Do not du this, Parser will initialize!
        //initialize();
    }

    /**
     * Overriden from superclass to add the WebServer and WebWorker version numbers.
     *
     * @see WebServer
     */
    public void printVersion() {
        super.printVersion();
        XTTProperties.printDebug(this.getClass().getName() + ": HTTPHelper: " + parseVersion(HTTPHelper.tantau_sccsid));
        XTTProperties.printDebug(this.getClass().getName() + ": WebServer : " + parseVersion(WebServer.tantau_sccsid));
        XTTProperties.printDebug(this.getClass().getName() + ": WebWorker : " + parseVersion(WebWorker.tantau_sccsid));
    }

    public void showVersions() {
        super.showVersions();
        System.out.println(ConvertLib.createString(this.getClass().getName() + ": HTTPHelper: ", SHOWLENGTH) + parseVersion(HTTPHelper.tantau_sccsid));
        System.out.println(ConvertLib.createString(this.getClass().getName() + ": WebServer : ", SHOWLENGTH) + parseVersion(WebServer.tantau_sccsid));
        System.out.println(ConvertLib.createString(this.getClass().getName() + ": WebWorker : ", SHOWLENGTH) + parseVersion(WebWorker.tantau_sccsid));
    }

    /**
     * Returns the Configuration Options as a String ready to copy/paste in a configuration file
     */
    public String getConfigurationOptions() {
        return "    <!-- function module HTTP -->"
                + "\n    <Webserver>"
                + "\n        <!-- the listening port of the internal webserver -->"
                + "\n        <Port>80</Port>"
                + "\n        <!-- the listening port of the internal secure-webserver -->"
                + "\n        <SecurePort>443</SecurePort>"
                + "\n        <!-- the directory where the files are stored for downloading -->"
                + "\n        <Root>webroot</Root>"
                + "\n        <!-- timeout on client connections to the webserver -->"
                + "\n        <Timeout>30000</Timeout>"
                + "\n        <!-- time to wait on a \"wait\" function before continuing -->"
                + "\n        <waitTimeout>30000</waitTimeout>"
                + "\n        <!--"
                + "\n        <enableCertCheck/>"
                + "\n        -->"
                + "\n    </Webserver>"
                + "\n    <!-- function module HTTP -->"
                + "\n    <Http>"
                + HTTPConnection.getConfigurationOptions()
                + "\n    </Http>";
    }

    /**
     * set the proxy for http and https requests.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> connection name (or not present for default),
     *                   <br><code>parameters[2]</code> argument is the proxy ipAddress,
     *                   <br><code>parameters[3]</code> is the proxy port.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int enableProxy(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": enableProxy: proxyAddress proxyPort");
            XTTProperties.printFail(this.getClass().getName() + ": enableProxy: connection proxyAddress proxyPort");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length < 3 || parameters.length > 4) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": proxyAddress proxyPort");
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": connection proxyAddress proxyPort");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else {
            try {
                int offset = 1;
                HTTPConnection connection = HTTPHelper.getConnection(connections, defaultConnection, parameters);
                if (parameters.length == 3) {
                    offset = 0;
                    //httpProxyHost=parameters[1+offset];
                    //httpProxyPort=parameters[2+offset];
                }
                connection.setProxy(parameters[1 + offset], parameters[2 + offset]);

                XTTProperties.printInfo(parameters[0] + ": setting proxy configuration to " + parameters[1 + offset] + ":" + parameters[2 + offset]);
            } catch (Exception e) {
                XTTProperties.printWarn(parameters[0] + ": unable to set proxy configuration - check parameters - " + e.getClass().getName());
            }
        }
        return status;
    }

    public int setClientTimeout(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": setClientTimeout: timeoutms");
            XTTProperties.printFail(this.getClass().getName() + ": setClientTimeout: connection timeoutms");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length < 2 || parameters.length > 3) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": timeoutms");
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": connection timeoutms");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else {
            try {
                int offset = 1;
                HTTPConnection connection = HTTPHelper.getConnection(connections, defaultConnection, parameters);
                if (parameters.length == 2) {
                    offset = 0;
                    //httpProxyHost=parameters[1+offset];
                    //httpProxyPort=parameters[2+offset];
                }
                connection.setTimeout(Integer.decode(parameters[1 + offset]));

                XTTProperties.printInfo(parameters[0] + ": setting client timeout to " + parameters[1 + offset] + "ms");
            } catch (NumberFormatException nfe) {
                XTTProperties.printFail(parameters[0] + ": '" + parameters[1] + "' is NOT a correct number");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
            } catch (Exception e) {
                XTTProperties.printWarn(parameters[0] + ": unable to set client timeout - check parameters - " + e.getClass().getName());
            }
        }
        return status;
    }

    /**
     * unset the proxy for http and https requests.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name, no additional arguments required
     *                   <br><code>parameters[1]</code> connection name (or not present for default),
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int disableProxy(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": disableProxy:" + NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName() + ": disableProxy: connection");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length > 2) {
            XTTProperties.printFail(parameters[0] + ":" + NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": connection");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else {
            try {
                HTTPHelper.getConnection(connections, defaultConnection, parameters).setProxy(null, null);
                XTTProperties.printInfo(parameters[0] + ": unsetting proxy configuration");
            } catch (Exception uhe) {
                XTTProperties.printFail(parameters[0] + ":: Unable to set Proxy: " + uhe.getMessage());
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
                if (XTTProperties.printDebug(null)) {
                    XTTProperties.printException(uhe);
                }
            }
        }
        return status;
    }

    public int createConnection(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": createConnection: name");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length != 2) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": name");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else {
            String name = parameters[1];
            try {
                HTTPConnection connection = defaultConnection.createConnection(name);
                connections.put(name.toLowerCase(), connection);
                HTTPHelper.getConnection(connections, defaultConnection, parameters);
                XTTProperties.printInfo(parameters[0] + ": created connection '" + name + "'");
            } catch (Exception uhe) {
                XTTProperties.printFail(parameters[0] + ":: Unable to set create connection '" + name + "': " + uhe.getMessage());
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
                if (XTTProperties.printDebug(null)) {
                    XTTProperties.printException(uhe);
                }
            }
        }
        return status;
    }

    /**
     * starts the webserver as an instance of WebServer.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> argument is the WebServer port,
     *                   <br><code>parameters[2]</code> is the WebServer root directory,
     *                   <br><code>parameters[3]</code> argument is the WebServer timeout.
     *                   <br>If only <code>parameters[0]</code> is submitted
     *                   the parameters will be taken from the configuration xml document in XTTProperties.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebServer
     * @see XTTProperties
     */
    public int startWebServer(String parameters[]) {
        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": startWebServer:" + NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName() + ": startWebServer: ip");
            XTTProperties.printFail(this.getClass().getName() + ": startWebServer: port");
            XTTProperties.printFail(this.getClass().getName() + ": startWebServer: port rootDirectory timeOut");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        return startWebServer(parameters, false);

    }

    /**
     * starts the webserver as an instance of WebServer with SSL-Encryption.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> argument is the WebServer port,
     *                   <br><code>parameters[2]</code> is the WebServer root directory,
     *                   <br><code>parameters[3]</code> argument is the WebServer timeout.
     *                   <br>If only <code>parameters[0]</code> is submitted the parameters will be taken from the configuration xml document in XTTProperties.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebServer
     * @see XTTProperties
     */
    public int startSecureWebServer(String parameters[]) {
        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": startSecureWebServer:" + NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName() + ": startSecureWebServer: port");
            XTTProperties.printFail(this.getClass().getName() + ": startSecureWebServer: port rootDirectory timeOut");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        return startWebServer(parameters, true);
    }

    private int startWebServer(String parameters[], boolean secure) {
        int status = XTTProperties.PASSED;

        String securetext = "non-secure";
        if (secure) securetext = "secure";
        if (parameters.length == 1 || parameters.length == 2) {
            try {
                XTTProperties.printVerbose(parameters[0] + ": Starting " + securetext + " Web Server");
                String porttype = "";
                if (secure) porttype = "SECURE";
                int port = 0;
                String ip = null;
                if (parameters.length > 1) {
                    ip = parameters[1];
                } else if (parameters.length > 3) {
                    port = Integer.parseInt(parameters[2]);
                } else if (parameters.length < 2) {
                    ip = XTTProperties.getProperty("WEBSERVER/IP");
                }
                if (parameters.length < 3) {
                    port = XTTProperties.getIntProperty("WEBSERVER/" + porttype + "PORT");
                }
                //Guy only one webserver can we run with the same ip/port
                if (s != null) {
                    if (s.getPort() == port && ip == s.getIp()) {
                        if (s.isAlive()) {
                            s.stopGracefully();
                        }
                    }
                }

                s = new WebServer(ip, port, XTTProperties.getProperty("WEBSERVER/ROOT"), XTTProperties.getIntProperty("WEBSERVER/TIMEOUT"), secure);
                ws = (new Thread(s, "WebServer"));
                ws.start();
                XTTProperties.printDebug(parameters[0] + ": Started " + securetext + " Web Server");
                return XTTProperties.PASSED;
            } catch (FileNotFoundException fnfe) {
                XTTProperties.printFail(parameters[0] + ": FileNotFoundException " + fnfe.getMessage());
                if (XTTProperties.printDebug(null)) {
                    XTTProperties.printException(fnfe);
                }
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
            } catch (Exception e) {
                XTTProperties.printFail(parameters[0] + ": " + e.getClass().getName() + ": " + e.getMessage());
                if (XTTProperties.printDebug(null)) {
                    XTTProperties.printException(e);
                }
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        } else if (parameters.length == 4) {
            try {
                XTTProperties.printVerbose(parameters[0] + ": Starting " + securetext + " Web Server");
                s = new WebServer(Integer.parseInt(parameters[1]), parameters[2], Integer.parseInt(parameters[3]), secure);
                ws = (new Thread(s, "WebServer"));
                ws.start();
                XTTProperties.printDebug(parameters[0] + ": Started " + securetext + " Web Server");
                return XTTProperties.PASSED;
            } catch (FileNotFoundException fnfe) {
                XTTProperties.printFail(parameters[0] + ": FileNotFoundException " + fnfe.getMessage());
                if (XTTProperties.printDebug(null)) {
                    XTTProperties.printException(fnfe);
                }
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
            } catch (Exception e) {
                XTTProperties.printFail(parameters[0] + ": " + e.getClass().getName() + ": " + e.getMessage());
                if (XTTProperties.printDebug(null)) {
                    XTTProperties.printException(e);
                }
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        } else {
            XTTProperties.printFail(parameters[0] + ":" + NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": port");
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": port rootDirectory timeOut");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        }
        return status;

    }


    /**
     * stops all/one webservers and all it's threads.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name, no additional parameters are requeired,
     *                   <br>the optional <code>parameters[1]</code> argument is the WebServer port of the WebServer to stop, if omitted all running servers are stopped.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebServer#closeSocket(String)
     * @see WebServer#closeSockets()
     */
    public int stopWebServer(String parameters[]) {
        int status = XTTProperties.PASSED;
        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": stopWebServer:" + NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName() + ": stopWebServer: port");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length == 2) {
            try {
                XTTProperties.printInfo(parameters[0] + ": Stopping WebServer on port " + parameters[1]);
                WebServer.closeSocket(parameters[1]);
                return XTTProperties.PASSED;
            } catch (Exception e) {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        } else {
            XTTProperties.printInfo(parameters[0] + ": Stopping all WebServers");
            try {
                WebServer.closeSockets();
                return XTTProperties.PASSED;
            } catch (Exception e) {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;

    }

    /**
     * stops the webserver and all it's threads.
     *
     * @see FunctionModule_HTTP#stopWebServer(java.lang.String[])
     */
    public int stopSecureWebServer(String parameters[]) {
        return stopWebServer(parameters);
    }

    /**
     * Overrides the normal HTTP Return code with a custom code.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> is an integer representing the http code (0 or less means no override).
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebWorker#setOverrideReturnCode(int)
     */
    public int setServerReturnCode(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": setServerReturnCode: httpReturnCode");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length != 2) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": headerFieldKey");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else {
            try {
                WebWorker.setOverrideReturnCode(Integer.parseInt(parameters[1]));
                if (Integer.parseInt(parameters[1]) > 0) {
                    XTTProperties.printInfo(parameters[0] + ": setting OverrideReturnCode to " + parameters[1]);
                } else {
                    XTTProperties.printInfo(parameters[0] + ": disabling OverrideReturnCode");
                }
            } catch (NumberFormatException nfe) {
                XTTProperties.printFail(parameters[0] + ": '" + parameters[1] + "' is NOT a number");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
            } catch (Exception e) {
                XTTProperties.printFail(parameters[0] + ": error setting OverrideReturnCode");
                XTTProperties.printException(e);
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }

    /**
     * Overrides the normal HTTP Return Message with a custom code.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> is the Message after the response code including the whitespace between message and response code (for "HTTP/1.1 200 Ok" it would be " Ok").
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebWorker#setOverrideReturnCode(int)
     */
    public int setServerReturnMessage(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": setServerReturnMessage:" + NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName() + ": setServerReturnMessage: httpReturnMessage");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length < 1 || parameters.length > 2) {
            XTTProperties.printFail(parameters[0] + ":" + NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": httpReturnMessage");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else {
            if (parameters.length == 1) {
                XTTProperties.printInfo(parameters[0] + ": disabling OverrideReturnMessage");
                WebWorker.setOverrideReturnMessage(null);
            } else {
                XTTProperties.printInfo(parameters[0] + ": setting OverrideReturnMessage to '" + parameters[1] + "'");
                WebWorker.setOverrideReturnMessage(parameters[1]);
            }
        }
        return status;
    }

    /**
     * Set the default WebWorkerExtension on a server. The class has to extend com.mobixell.xtt.WebWorkerExtension<br>Note: Start the WebServer first!
     * This creates ONE instance per server port that is reused in contrast to the url parameter extension which creates a new instance every time.
     * <br>Example of the url extension meachnism:
     * <br>&lt;function name="sendGetRequest" module="HTTP"&gt;
     * <br>&nbsp;&nbsp;&nbsp;&nbsp;&lt;parameter&gt;http://127.0.0.1/?extension=com.mobixell.xtt.WebWorkerExtensionTest&lt;/parameter&gt;
     * <br>&lt;/function&gt;
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> is an integer representing the port the server runs on.
     *                   <br><code>parameters[1]</code> is the Name of the class. Example: com.mobixell.xtt.WebWorkerExtensionTest.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebServer#setDefaultWebWorkerExtension(String, String)
     */
    public int setDefaultWebWorkerExtension(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": setDefaultWebWorkerExtension: port extensionClass");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length != 3) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": port extensionClass");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else {
            int port = 0;
            try {
                port = Integer.decode(parameters[1]);
                WebServer.setDefaultWebWorkerExtension("" + port, parameters[2]);
            } catch (ClassNotFoundException cnfe) {
                XTTProperties.printFail(parameters[0] + ": ClassNotFoundException: '" + parameters[2] + "' not found");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
                if (XTTProperties.printDebug(null)) {
                    XTTProperties.printException(cnfe);
                }
                return status;
            } catch (ClassCastException cce) {
                XTTProperties.printFail(parameters[0] + ": ClassCastException: '" + parameters[2] + "' not extending WebWorkerExtension");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
                if (XTTProperties.printDebug(null)) {
                    XTTProperties.printException(cce);
                }
                return status;
            } catch (NullPointerException npe) {
                XTTProperties.printFail(parameters[0] + ": WebServer running on port '" + port + "' not found");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
                if (XTTProperties.printDebug(null)) {
                    XTTProperties.printException(npe);
                }
                return status;
            } catch (NumberFormatException nfe) {
                XTTProperties.printFail(parameters[0] + ": '" + parameters[1] + "' is NOT a number");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
            } catch (Exception ex) {
                XTTProperties.printFail(parameters[0] + ": " + ex.getClass().getName() + ": '" + parameters[2] + "' error");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
                if (XTTProperties.printDebug(null)) {
                    XTTProperties.printException(ex);
                }
                return status;
            }
        }
        return status;
    }

    /**
     * Set if the server should request a client certificate or not.
     * Note: The certificate must be allowed by java (i.e. not a test certificate) or the request will fail.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> is an integer representing the port the server runs on.
     *                   <br><code>parameters[1]</code> is the boolean of whether the certificate should be requested or not.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int setRequestClientCertificate(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": setRequestClientCertificate: port boolean");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length != 3) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": port boolean");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else {
            int port = 0;
            try {
                port = Integer.decode(parameters[1]);
                WebServer.setRequestClientCertificate("" + port, ConvertLib.textToBoolean(parameters[2]));
            } catch (NullPointerException npe) {
                XTTProperties.printFail(parameters[0] + ": WebServer running on port '" + port + "' not found");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
                if (XTTProperties.printDebug(null)) {
                    XTTProperties.printException(npe);
                }
                return status;
            } catch (NumberFormatException nfe) {
                XTTProperties.printFail(parameters[0] + ": '" + parameters[1] + "' is NOT a number");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
            } catch (Exception ex) {
                XTTProperties.printFail(parameters[0] + ": " + ex.getClass().getName() + ": '" + parameters[2] + "' error");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
                if (XTTProperties.printDebug(null)) {
                    XTTProperties.printException(ex);
                }
                return status;
            }
        }
        return status;
    }

    /**
     * remove all the headers that are to be sent from the client to the server.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name, no additional parameters are requeired.
     *                   <br><code>parameters[1]</code> connection name (or not present for default),
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int clearHeader(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": clearHeader:" + NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName() + ": clearHeader: connection");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length < 1 || parameters.length > 2) {
            XTTProperties.printFail(parameters[0] + ":" + NO_ARGUMENTS);
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else {
            HTTPHelper.getConnection(connections, defaultConnection, parameters).getRequestHeader().clear();
            XTTProperties.printInfo(parameters[0] + ": clearing header");
        }
        return status;
    }

    /**
     * remove all the headers that are to be sent from the server to the client.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name, no additional parameters are requeired.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebWorker#setServerSendHeader
     */
    public int clearServerHeader(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": clearServerHeader:" + NO_ARGUMENTS);
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length != 1) {
            XTTProperties.printFail(parameters[0] + ":" + NO_ARGUMENTS);
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else {
            XTTProperties.printInfo(parameters[0] + ": clearing header");
            WebWorker.setServerSendHeader(new LinkedHashMap<String, String>());
        }
        return status;
    }

    /**
     * set the http headers to be sent from the client to the server.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> connection name (or not present for default),
     *                   <br><code>parameters[2]</code> argument is the header key,
     *                   <br><code>parameters[3]</code> argument is the header value or not present removing.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int setHeader(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": setHeader: headerFieldKey");
            XTTProperties.printFail(this.getClass().getName() + ": setHeader: headerFieldKey headerFieldValue");
            XTTProperties.printFail(this.getClass().getName() + ": setHeader: connection headerFieldKey");
            XTTProperties.printFail(this.getClass().getName() + ": setHeader: connection headerFieldKey headerFieldValue");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        HTTPHelper.setHeader(connections, defaultConnection, parameters);
        return status;
    }

    /**
     * set the http headers to be sent from the server to the client.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> argument is the header key,
     *                   <br><code>parameters[2]</code> argument is the header value.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebWorker#getServerSendHeader
     */
    public int setServerHeader(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": setServerHeader: headerFieldKey headerFieldValue");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        HTTPHelper.setHeader(WebWorker.getServerSendHeader(), parameters);
        return status;
    }

    /**
     * compare the http headers received by the client from the server with a value which is required.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> connection name (or not present for default),
     *                   <br><code>parameters[2]</code> argument is the header key,
     *                   <br><code>parameters[3]</code> argument is the header value or not present for any value.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int checkHeader(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": checkHeader: headerFieldKey");
            XTTProperties.printFail(this.getClass().getName() + ": checkHeader: headerFieldKey expectedValue");
            XTTProperties.printFail(this.getClass().getName() + ": checkHeader: connection headerFieldKey");
            XTTProperties.printFail(this.getClass().getName() + ": checkHeader: connection headerFieldKey expectedValue");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        HTTPHelper.checkHeader(connections, defaultConnection, parameters, false);
        return status;
    }

    /**
     * compare the http headers received by the client from the server with a value which is required.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> connection name (or not present for default),
     *                   <br><code>parameters[2]</code> argument is the variable to store the result to,
     *                   <br><code>parameters[3]</code> argument is the header key,
     *                   <br><code>parameters[4]</code> argument is the regular expression.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int queryHeader(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": queryHeader: variable headerFieldKey regularExpression");
            XTTProperties.printFail(this.getClass().getName() + ": queryHeader: connection variable headerFieldKey regularExpression");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length < 4 || parameters.length > 5) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": variable headerFieldKey regularExpression");
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": connection variable headerFieldKey regularExpression");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else {
            if (parameters.length == 4) {
                HTTPHelper.queryHeader(defaultConnection.getResponseHeader(), parameters, false);
            } else {
                HTTPConnection connection = connections.get(parameters[1].toLowerCase());
                if (connection == null) {
                    connection = defaultConnection;
                    XTTProperties.printWarn(parameters[0] + ": connection '" + parameters[1] + "' not found, using default");
                } else {
                    String[] newparameters = new String[parameters.length - 1];
                    newparameters[0] = parameters[0] + "(" + connection.getName() + ")";
                    for (int i = 1; i < newparameters.length; i++) {
                        newparameters[i] = parameters[i + 1];
                    }
                    HTTPHelper.queryHeader(connection.getResponseHeader(), newparameters, false);
                }
            }
        }
        return status;
    }

    /**
     * compare the http headers received by the client from the server with a value which is required.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> connection name (or not present for default),
     *                   <br><code>parameters[2]</code> argument is the header key,
     *                   <br><code>parameters[3]</code> argument is the regular expression.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int queryHeaderNegative(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": queryHeaderNegative: headerFieldKey regularExpression");
            XTTProperties.printFail(this.getClass().getName() + ": queryHeaderNegative: connection headerFieldKey regularExpression");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length < 3 || parameters.length > 4) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": headerFieldKey regularExpression");
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": connection headerFieldKey regularExpression");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else {
            if (parameters.length == 3) {
                HTTPHelper.queryHeader(defaultConnection.getResponseHeader(), parameters, true);
            } else {
                HTTPConnection connection = connections.get(parameters[1].toLowerCase());
                if (connection == null) {
                    connection = defaultConnection;
                    XTTProperties.printWarn(parameters[0] + ": connection '" + parameters[1] + "' not found, using default");
                } else {
                    String[] newparameters = new String[parameters.length - 1];
                    newparameters[0] = parameters[0] + "(" + connection.getName() + ")";
                    for (int i = 1; i < newparameters.length; i++) {
                        newparameters[i] = parameters[i + 1];
                    }
                    HTTPHelper.queryHeader(connection.getResponseHeader(), newparameters, true);
                }
            }
        }
        return status;
    }

    /**
     * compare the http headers received by the client from the server with a value is prohibited.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> connection name (or not present for default),
     *                   <br><code>parameters[2]</code> argument is the header key,
     *                   <br><code>parameters[3]</code> argument is the header value or not present for header absence check.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int checkHeaderNot(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": checkHeaderNot: headerFieldKey");
            XTTProperties.printFail(this.getClass().getName() + ": checkHeaderNot: headerFieldKey expectedValue");
            XTTProperties.printFail(this.getClass().getName() + ": checkHeaderNot: connection headerFieldKey");
            XTTProperties.printFail(this.getClass().getName() + ": checkHeaderNot: connection headerFieldKey expectedValue");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        HTTPHelper.checkHeader(connections, defaultConnection, parameters, true);
        return status;
    }

    /**
     * compare the http headers received by the server from the client with a value which is required.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> argument is the header key,
     *                   <br><code>parameters[2]</code> argument is the header value.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int checkServerHeader(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": checkServerHeader: headerFieldKey");
            XTTProperties.printFail(this.getClass().getName() + ": checkServerHeader: headerFieldKey expectedValue");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        HTTPHelper.checkHeader(WebWorker.getServerHeader(), parameters, false);
        return status;
    }

    /**
     * query the http headers received by the server from the client with a regular expression.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> argument is the variable to store the result to,
     *                   <br><code>parameters[2]</code> argument is the header key,
     *                   <br><code>parameters[3]</code> argument is the regular expression.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int queryServerHeader(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": queryServerHeader: variable headerFieldKey regularExpression");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length < 4) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": variable headerFieldKey regularExpression");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else {
            HTTPHelper.queryHeader(WebWorker.getServerHeader(), parameters, false);
        }
        return status;
    }

    /**
     * query the http headers received by the server from the client with a regular expression.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> argument is the header key,
     *                   <br><code>parameters[2]</code> argument is the regular expression.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int queryServerHeaderNegative(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": queryServerHeaderNegative: headerFieldKey regularExpression");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length != 3) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": headerFieldKey regularExpression");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else {
            HTTPHelper.queryHeader(WebWorker.getServerHeader(), parameters, true);
        }
        return status;
    }

    /**
     * compare the http headers received by the server from the client with a value which it is prohibited.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> argument is the header key,
     *                   <br><code>parameters[2]</code> argument is the header value.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int checkServerHeaderNot(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": checkServerHeaderNot: headerFieldKey");
            XTTProperties.printFail(this.getClass().getName() + ": checkServerHeaderNot: headerFieldKey expectedValue");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        HTTPHelper.checkHeader(WebWorker.getServerHeader(), parameters, true);
        return status;
    }

    /**
     * compare the url received by the server from the client with a value.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> argument is the url value.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int checkServerURL(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": checkServerURL: URL");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length != 2) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": URL");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else {
            String surl = parameters[1];
            if (surl.equals(WebWorker.getServerRecievedURL())) {
                XTTProperties.printInfo(parameters[0] + ": found URL " + WebWorker.getServerRecievedURL());
            } else {
                XTTProperties.printFail(parameters[0] + ": found URL " + WebWorker.getServerRecievedURL() + " wanted " + surl);
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }

    /**
     * query if the URL of a http request recieved by the server contains a specified regular expression value.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> argument is the variable name to store the result in,
     *                   <br><code>parameters[2]</code> argument is the java reqular expression pattern.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int queryServerURL(String[] parameters) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": queryServerURL: variableName regularExpression");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length != 3) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": variableName regularExpression");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        } else {
            XTTProperties.printDebug(parameters[0] + ": regex: '" + parameters[2] + "'");
            boolean bStatus = ConvertLib.queryString(parameters[0], WebWorker.getServerRecievedURL(), parameters[2],
                    parameters[1]);
            if (!bStatus)
                status = XTTProperties.FAILED;
        }
        return status;
    }

    /**
     * clear the post data meant to be sent to the server.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name, no additional arguments requeired.
     *                   <br><code>parameters[1]</code> connection name (or not present for default),
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int clearPostData(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": clearPostData:" + NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName() + ": clearPostData: connection");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length != 1) {
            XTTProperties.printFail(parameters[0] + ":" + NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": connection");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else {
            HTTPHelper.getConnection(connections, defaultConnection, parameters).getPostData().clear();
            XTTProperties.printInfo(parameters[0] + ": clearing POST data");
        }
        return status;
    }

    /**
     * compare the post data received by the server from the client with a value.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> argument is the post field name,
     *                   <br><code>parameters[2]</code> argument is the post field value.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebWorker#getPostData
     */
    public int checkPostData(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": checkPostData: postFieldName postFieldValue");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length == 1) {
            if (WebWorker.getPostData() == null) {
                XTTProperties.printInfo(parameters[0] + ": no POST data submitted to server");
            } else {
                XTTProperties.printFail(parameters[0] + ": POST data received: " + WebWorker.getPostData());
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
                return status;
            }
        } else if (parameters.length != 3) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": postFieldName postFieldValue");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else {
            String functionName = parameters[0];
            LinkedHashMap<String, String> serverPostData = WebWorker.getPostData();
            if (serverPostData == null) {
                XTTProperties.printFail(functionName + ": no POST data submitted to server");
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
                return status;
            }
            String postFieldName = parameters[1].trim();
            String postFieldValue = parameters[2];
            if (serverPostData.get(postFieldName) != null
                    && (((String) serverPostData.get(postFieldName)).equals(postFieldValue))) {
                XTTProperties.printInfo(functionName + ": POST field " + postFieldName + ": " + serverPostData.get(postFieldName));
                return status;
            } else {
                if (serverPostData.get(postFieldName) == null) {
                    if (postFieldValue.equals("null")) {
                        XTTProperties.printInfo(functionName + ": POST field " + postFieldName + ": field key not found");
                        return status;
                    } else {
                        XTTProperties.printFail(functionName + ": POST field " + postFieldName + ": field key not found");
                    }
                } else {
                    String more = "";
                    if (postFieldValue.equals("null")) more = " (POST field not found)";
                    XTTProperties.printFail(functionName + ": POST field " + postFieldName + ": " + serverPostData.get(postFieldName) + " wanted " + postFieldValue + "" + more);
                }
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }

    /**
     * set the post data to be sent from the client to the server on a post request.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> connection name (or not present for default),
     *                   <br><code>parameters[2]</code> argument is the post field name,
     *                   <br><code>parameters[3]</code> argument is the post field value or not present for removing.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int setPostData(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": setPostData: postFieldName");
            XTTProperties.printFail(this.getClass().getName() + ": setPostData: postFieldName postFieldValue");
            XTTProperties.printFail(this.getClass().getName() + ": setPostData: connection postFieldName");
            XTTProperties.printFail(this.getClass().getName() + ": setPostData: connection postFieldName postFieldValue");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length > 3 || parameters.length < 2) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": postFieldName");
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": postFieldName postFieldValue");
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": connection postFieldName");
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": connection postFieldName postFieldValue");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else {
            HTTPConnection connection = connections.get(parameters[1].toLowerCase());
            int offset = 0;
            if (connection == null) {
                connection = defaultConnection;
                if (parameters.length == 4) {
                    offset = 1;
                    XTTProperties.printWarn(parameters[0] + ": connection '" + parameters[1] + "' not found, using default");
                }
            } else {
                parameters[0] = parameters[0] + "(" + connection.getName() + ")";
                offset = 1;
            }
            if (parameters.length == 3 + offset && !parameters[2 + offset].equals("null")) {
                XTTProperties.printInfo(parameters[0] + ": setting POST data " + parameters[1 + offset] + " to: " + parameters[2 + offset]);
                // Actually set the Header Key and Value
                connection.getPostData().put(parameters[1 + offset], parameters[2 + offset]);
            } else {
                XTTProperties.printInfo(parameters[0] + ": removing POST data " + parameters[1 + offset]);
                // Actually remove the Header Key and Value
                connection.getPostData().remove(parameters[1 + offset]);
            }
        }
        return status;
    }

    /**
     * set the post data to be sent from the client to the server on a post request.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> connection name (or not present for default),
     *                   <br><code>parameters[2]</code> argument is the base64 encoded byte data to set or absent for removing,
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int setBase64PostData(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": setBase64PostData:" + NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName() + ": setBase64PostData: base64PostData");
            XTTProperties.printFail(this.getClass().getName() + ": setBase64PostData: connection");
            XTTProperties.printFail(this.getClass().getName() + ": setBase64PostData: connection base64PostData");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length < 1 || parameters.length > 3) {
            XTTProperties.printFail(parameters[0] + ":" + NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": base64PostData");
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": connection");
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": connection base64PostData");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else {
            HTTPConnection connection = HTTPHelper.getConnection(connections, defaultConnection, parameters);
            int offset = 1;
            if (connection == defaultConnection) {
                offset = 0;
            }

            if ((offset == 0 && parameters.length == 1 + offset) || (parameters.length == 1 + offset && offset != 0)) {
                XTTProperties.printInfo(parameters[0] + ": removing BASE64 POST data");
                connection.setPostDataBytes(null);
            } else {
                byte[] data = ConvertLib.base64Decode(parameters[1 + offset]);
                XTTProperties.printInfo(parameters[0] + ": setting " + data.length + " bytes of decoded BASE64 data");
                connection.setPostDataBytes(data);
            }
        }
        return status;
    }

    /**
     * set the post data to be sent from the client to the server on a post request.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> connection name (or not present for default),
     *                   <br><code>parameters[2]</code> argument is the UTF-8 encoded text byte data to set or absent for removing,
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int setTextPostData(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": setTextPostData:" + NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName() + ": setTextPostData: testPostData");
            XTTProperties.printFail(this.getClass().getName() + ": setTextPostData: connection");
            XTTProperties.printFail(this.getClass().getName() + ": setTextPostData: connection textPostData");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length < 1 || parameters.length > 3) {
            XTTProperties.printFail(parameters[0] + ":" + NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": textPostData");
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": connection");
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": connection textPostData");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else {
            HTTPConnection connection = HTTPHelper.getConnection(connections, defaultConnection, parameters);
            int offset = 1;
            if (connection == defaultConnection) {
                offset = 0;
            }

            if ((offset == 0 && parameters.length == 1 + offset) || (parameters.length == 1 + offset && offset != 0)) {
                XTTProperties.printInfo(parameters[0] + ": removing text POST data");
                connection.setPostDataBytes(null);
            } else {
                byte[] data = ConvertLib.createBytes(parameters[1 + offset]);
                XTTProperties.printInfo(parameters[0] + ": setting " + data.length + " bytes of text data");
                connection.setPostDataBytes(data);
            }
        }
        return status;
    }

    /**
     * do a http POST request with the post data sent with header "content-type: application/x-www-form-urlencoded".
     * Set the header to  "content-type: multipart/form-data" to send as multipart.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> connection name (or not present for default),
     *                   <br><code>parameters[1]</code> argument is the url value.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int sendPostRequest(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": sendPostRequest: URL");
            XTTProperties.printFail(this.getClass().getName() + ": sendPostRequest: URL variableForBody");
            XTTProperties.printFail(this.getClass().getName() + ": sendPostRequest: connection URL");
            XTTProperties.printFail(this.getClass().getName() + ": sendPostRequest: connection URL variableForBody");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length < 2 || parameters.length > 3) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": URL");
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": URL variableForBody");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        }
        HTTPConnection connection = HTTPHelper.getConnection(connections, defaultConnection, parameters);
        if ((connection.getPostData() == null || connection.getPostData().isEmpty()) && connection.getPostDataBytes() == null) {
            XTTProperties.printFail(parameters[0] + ": POST data not set - please use setPostData or setBase64PostData first");
            status = XTTProperties.FAILED;
            XTTProperties.setTestStatus(status);
        } else {
            sendGetRequest(parameters, true);
        }
        return status;
    }

    /**
     * do a http GET request.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> connection name (or not present for default),
     *                   <br><code>parameters[2]</code> argument is the url value.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int sendGetRequest(String parameters[]) {
        return sendGetRequest(parameters, false);
    }

    /**
     * do a http GET request. This method makes sure that the test case is not set to FAILED in case Client receives IOException with message - 'INVALID CHUNKED BODY detected'
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> connection name (or not present for default),
     *                   <br><code>parameters[2]</code> argument is the url value.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */

    public int sendGetRequestNoFailOnException(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": sendGetRequestNoFailOnException: URL");
            XTTProperties.printFail(this.getClass().getName() + ": sendGetRequestNoFailOnException: URL variableForBody");
            XTTProperties.printFail(this.getClass().getName() + ": sendGetRequestNoFailOnException: connection URL");
            XTTProperties.printFail(this.getClass().getName() + ": sendGetRequestNoFailOnException: connection URL variableForBody");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length < 2 || parameters.length > 4) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": URL");
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": URL variableForBody");
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": connection URL");
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": connection URL variableForBody");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else {
            HTTPConnection connection = HTTPHelper.getConnection(connections, defaultConnection, parameters);
            int offset = 1;
            if (connection == defaultConnection) {
                offset = 0;
            }
            String variableForBody = null;
            if (parameters.length >= 3 + offset) {
                variableForBody = parameters[2 + offset];
            }

            try {
                connection.sendGetRequest(parameters[0], parameters[1 + offset]);

                if (variableForBody != null) {
                    XTTProperties.setVariable(variableForBody + "/BASE64", ConvertLib.base64Encode(connection.getResponse()));
                    XTTProperties.setVariable(variableForBody + "/PLAIN", ConvertLib.createString(connection.getResponse()));
                    XTTProperties.printDebug(parameters[0] + ": body stored in " + variableForBody + "/BASE64" + "\n" +
                            parameters[0] + ": body stored in " + variableForBody + "/PLAIN" + "\n");
                }
            } catch (IOException e) {
                XTTProperties.printWarn(parameters[0] + ": " + e.getClass().getName() + ": " + e.getMessage());
                if (XTTProperties.printDebug(null)) {
                    XTTProperties.printException(e);
                }

                if (e.getMessage() != null && !e.getMessage().equalsIgnoreCase("INVALID CHUNKED BODY detected")) {
                    status = XTTProperties.FAILED;
                    XTTProperties.setTestStatus(status);
                }
            } catch (Exception e) {
                XTTProperties.printWarn(parameters[0] + ": " + e.getClass().getName() + ": " + e.getMessage());
                if (XTTProperties.printDebug(null)) {
                    XTTProperties.printException(e);
                }
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }

    /**
     * disable certificate checking on https connections.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name, no additional parameters are requeired.
     *                   <br><code>parameters[1]</code> connection name (or not present for default),
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebWorker#setServerSendHeader
     */
    public int disableCertCheck(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": disableCertCheck:" + NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName() + ": disableCertCheck: connection");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length < 1 || parameters.length > 2) {
            XTTProperties.printFail(parameters[0] + ":" + NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": connection");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        }
        HTTPHelper.getConnection(connections, defaultConnection, parameters).setEnableCertcheck(false);
        XTTProperties.printInfo(parameters[0] + ": Disabling default Java Certificate Trust Manager");
        return status;
    }

    /**
     * enable certificate checking on https connections.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name, no additional parameters are requeired.
     *                   <br><code>parameters[1]</code> connection name (or not present for default),
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebWorker#setServerSendHeader
     */
    public int enableCertCheck(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": enableCertCheck:" + NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName() + ": enableCertCheck: connection");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length < 1 || parameters.length > 2) {
            XTTProperties.printFail(parameters[0] + ":" + NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": connection");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        }
        HTTPHelper.getConnection(connections, defaultConnection, parameters).setEnableCertcheck(true);
        XTTProperties.printInfo(parameters[0] + ": Enabling default Java Certificate Trust Manager");
        return status;
    }

    /**
     * disable shortening of URLs from 'http://server.com/path' to '/path' in the request.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name, no additional parameters are requeired.
     *                   <br><code>parameters[1]</code> connection name (or not present for default),
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see HTTPConnection#setEnableURLContract
     */
    public int disableURLContraction(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": disableURLContraction:" + NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName() + ": disableURLContraction: connection");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length < 1 || parameters.length > 2) {
            XTTProperties.printFail(parameters[0] + ":" + NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": connection");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        }
        HTTPHelper.getConnection(connections, defaultConnection, parameters).setEnableURLContract(false);
        XTTProperties.printInfo(parameters[0] + ": Disabling URL contraction");
        return status;
    }

    /**
     * enable shortening of URLs from 'http://server.com/path' to '/path' in the request.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name, no additional parameters are requeired.
     *                   <br><code>parameters[1]</code> connection name (or not present for default),
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see HTTPConnection#setEnableURLContract
     */
    public int enableURLContraction(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": enableURLContraction:" + NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName() + ": enableURLContraction: connection");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length < 1 || parameters.length > 2) {
            XTTProperties.printFail(parameters[0] + ":" + NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": connection");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        }
        HTTPHelper.getConnection(connections, defaultConnection, parameters).setEnableURLContract(true);
        XTTProperties.printInfo(parameters[0] + ": Enabling URL contraction");

        return status;
    }

    /**
     * disable internal redirect and download of an http redirect message (301,302, etc).
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name, no additional parameters are requeired.
     *                   <br><code>parameters[1]</code> connection name (or not present for default),
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebWorker#setServerSendHeader
     */
    public int disableFollowRedirects(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": disableFollowRedirects:" + NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName() + ": disableFollowRedirects: connection");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length < 1 || parameters.length > 2) {
            XTTProperties.printFail(parameters[0] + ":" + NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": connection");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        } else {
            HTTPHelper.getConnection(connections, defaultConnection, parameters).setFollowRedirects(false);
            XTTProperties.printInfo(parameters[0] + ": Disabling HTTP follow redirect");
        }
        return status;
    }

    /**
     * enable internal redirect and download of an http redirect message (301,302, etc).
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name, no additional parameters are requeired.
     *                   <br><code>parameters[1]</code> connection name (or not present for default),
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebWorker#setServerSendHeader
     */
    public int enableFollowRedirects(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": enableFollowRedirects:" + NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName() + ": enableFollowRedirects: connection");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length < 1 || parameters.length > 2) {
            XTTProperties.printFail(parameters[0] + ":" + NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": connection");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else {
            HTTPHelper.getConnection(connections, defaultConnection, parameters).setFollowRedirects(true);
            XTTProperties.printInfo(parameters[0] + ": Enabling HTTP follow redirect");
        }
        return status;
    }

    /**
     * enable keep-alive.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name, no additional parameters are requeired.
     *                   <br><code>parameters[1]</code> connection name (or not present for default),
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebWorker#setServerSendHeader
     */
    public int enableKeepAlive(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": enableKeepAlive:" + NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName() + ": enableKeepAlive: connection");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length < 1 || parameters.length > 2) {
            XTTProperties.printFail(parameters[0] + ":" + NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": connection");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else {
            HTTPHelper.getConnection(connections, defaultConnection, parameters).setKeepAlive(true);
            XTTProperties.printInfo(parameters[0] + ": Enabling KeepAlive.");
        }
        return status;
    }

    /**
     * disable keep-alive.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name, no additional parameters are requeired.
     *                   <br><code>parameters[1]</code> connection name (or not present for default),
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebWorker#setServerSendHeader
     */
    public int disableKeepAlive(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": disableKeepAlive:" + NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName() + ": disableKeepAlive: connection");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length < 1 || parameters.length > 2) {
            XTTProperties.printFail(parameters[0] + ":" + NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": connection");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else {
            HTTPHelper.getConnection(connections, defaultConnection, parameters).setKeepAlive(false);
            XTTProperties.printInfo(parameters[0] + ": Disabling KeepAlive.");
        }
        return status;
    }

    public int closeConnection(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": closeConnection:" + NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName() + ": closeConnection: connection");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length < 1 || parameters.length > 2) {
            XTTProperties.printFail(parameters[0] + ":" + NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": connection");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else {
            HTTPHelper.getConnection(connections, defaultConnection, parameters).closeConnection();
            XTTProperties.printInfo(parameters[0] + ": closed connection.");
        }
        return status;
    }

    /**
     * do a http GET request.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> connection name (or not present for default),
     *                   <br><code>parameters[2]</code> argument is the url value.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int sendGetRequestInBytes(String parameters[]) {
        return sendGetRequestInBytes(parameters, false);
    }

    private int sendGetRequestInBytes(String parameters[], boolean isPost) {
        int status = XTTProperties.PASSED;
        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": sendGetRequestInBytes: URL");
            XTTProperties.printFail(this.getClass().getName() + ": sendGetRequestInBytes: byteToRead");
            XTTProperties.printFail(this.getClass().getName() + ": sendGetRequestInBytes: URL variableForBody");
            XTTProperties.printFail(this.getClass().getName() + ": sendGetRequestInBytes: connection URL");
            XTTProperties.printFail(this.getClass().getName() + ": sendGetRequestInBytes: connection URL variableForBody");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length < 2 || parameters.length > 6) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": URL");
            XTTProperties.printFail(this.getClass().getName() + ": sendGetRequestInBytes: byteToRead");
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": URL variableForBody");
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": connection URL");
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": connection URL variableForBody");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else {
            HTTPConnection connection = HTTPHelper.getConnection(connections, defaultConnection, parameters);
            int offset = 1;
            if (connection == defaultConnection) {
                offset = 0;
            }
            String variableForBody = null;
            if (parameters.length >= 4 + offset) {
                variableForBody = parameters[2 + offset];
            }
            try {
                if (isPost) {
                    connection.sendPostRequest(parameters[0], parameters[1 + offset]);
                } else {
                    connection.sendGetRequestWithBreak(parameters[0], parameters[1 + offset], parameters[2 + offset]);
                }
                if (variableForBody != null) {
                    XTTProperties.setVariable(variableForBody + "/BASE64", ConvertLib.base64Encode(connection.getResponse()));
                    XTTProperties.setVariable(variableForBody + "/PLAIN", ConvertLib.createString(connection.getResponse()));
                    XTTProperties.printDebug(parameters[0] + ": body stored in " + variableForBody + "/BASE64" + "\n" +
                            parameters[0] + ": body stored in " + variableForBody + "/PLAIN" + "\n");
                }
            } catch (Exception e) {
                XTTProperties.printFail(parameters[0] + ": " + e.getClass().getName() + ": " + e.getMessage());
                if (XTTProperties.printDebug(null)) {
                    XTTProperties.printException(e);
                }
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }

    /**
     * do a http GET request.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> connection name (or not present for default),
     *                   <br><code>parameters[2]</code> argument is the url value.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int sendGetRequestLimited(String parameters[]) {
        return sendGetRequestLimited(parameters, false);
    }

    private int sendGetRequestLimited(String parameters[], boolean isPost) {
        int status = XTTProperties.PASSED;
        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": sendGetRequestLimited: URL");
            XTTProperties.printFail(this.getClass().getName() + ": sendGetRequestLimited: bufferSize[1-65536]");
            XTTProperties.printFail(this.getClass().getName() + ": sendGetRequestLimited: URL variableForBody");
            XTTProperties.printFail(this.getClass().getName() + ": sendGetRequestLimited: connection URL");
            XTTProperties.printFail(this.getClass().getName() + ": sendGetRequestLimited: connection URL variableForBody");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length < 2 || parameters.length > 6) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": URL");
            XTTProperties.printFail(this.getClass().getName() + ": sendGetRequestLimited: bufferSize");
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": URL variableForBody");
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": connection URL");
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": connection URL variableForBody");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else {
            HTTPConnection connection = HTTPHelper.getConnection(connections, defaultConnection, parameters);
            int offset = 1;
            if (connection == defaultConnection) {
                offset = 0;
            }
            String variableForBody = null;
            if (parameters.length >= 4 + offset) {
                variableForBody = parameters[2 + offset];
            }
            try {
                if (isPost) {
                    connection.sendPostRequest(parameters[0], parameters[1 + offset]);
                } else {
                    if (Integer.parseInt(parameters[2 + offset]) > 0)
                        connection.sendGetRequest(parameters[0], parameters[1 + offset], Integer.parseInt(parameters[2 + offset]));
                    else
                        connection.sendGetRequest(parameters[0], parameters[1 + offset], 65536);
                }
                if (variableForBody != null) {
                    XTTProperties.setVariable(variableForBody + "/BASE64", ConvertLib.base64Encode(connection.getResponse()));
                    XTTProperties.setVariable(variableForBody + "/PLAIN", ConvertLib.createString(connection.getResponse()));
                    XTTProperties.printDebug(parameters[0] + ": body stored in " + variableForBody + "/BASE64" + "\n" +
                            parameters[0] + ": body stored in " + variableForBody + "/PLAIN" + "\n");
                }
            } catch (Exception e) {
                XTTProperties.printFail(parameters[0] + ": " + e.getClass().getName() + ": " + e.getMessage());
                if (XTTProperties.printDebug(null)) {
                    XTTProperties.printException(e);
                }
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }

    private int sendGetRequest(String parameters[], boolean isPost) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": sendGetRequest: URL");
            XTTProperties.printFail(this.getClass().getName() + ": sendGetRequest: URL variableForBody");
            XTTProperties.printFail(this.getClass().getName() + ": sendGetRequest: connection URL");
            XTTProperties.printFail(this.getClass().getName() + ": sendGetRequest: connection URL variableForBody");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length < 2 || parameters.length > 5) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": URL");
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": URL variableForBody");
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": connection URL");
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": connection URL variableForBody");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else {
            HTTPConnection connection = HTTPHelper.getConnection(connections, defaultConnection, parameters);
            int offset = 1;
            if (connection == defaultConnection) {
                offset = 0;
            }
            String variableForBody = null;
            if (parameters.length >= 3 + offset) {
                variableForBody = parameters[2 + offset];
            }
            try {
                if (isPost) {
                    connection.sendPostRequest(parameters[0], parameters[1 + offset]);
                } else {
                    connection.sendGetRequest(parameters[0], parameters[1 + offset]);
                }
                if (variableForBody != null) {
                    XTTProperties.setVariable(variableForBody + "/BASE64", ConvertLib.base64Encode(connection.getResponse()));
                    XTTProperties.setVariable(variableForBody + "/PLAIN", ConvertLib.createString(connection.getResponse()));
                    XTTProperties.printDebug(parameters[0] + ": body stored in " + variableForBody + "/BASE64" + "\n" +
                            parameters[0] + ": body stored in " + variableForBody + "/PLAIN" + "\n");
                }
            } catch (Exception e) {
                XTTProperties.printFail(parameters[0] + ": " + e.getClass().getName() + ": " + e.getMessage());
                if (XTTProperties.printDebug(null)) {
                    XTTProperties.printException(e);
                }
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
                connection.closeConnection();
            }
        }
        return status;
    }

    /**
     * do a http GET request.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> connection name (or not present for default),
     *                   <br><code>parameters[2]</code> argument is the HTTP-METHOD value.
     *                   <br><code>parameters[3]</code> argument is a boolean indicating if POST data has to be sent.
     *                   <br><code>parameters[4]</code> argument is the URL value.
     *                   <br><code>parameters[5]</code> argument is the name of a variable to store the response to.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int sendFreeRequest(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": sendFreeRequest: METHOD hasPostBody URL variableForResponseBody ");
            XTTProperties.printFail(this.getClass().getName() + ": sendFreeRequest: connection METHOD hasPostBody URL variableForResponseBody");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length < 5 || parameters.length > 6) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": METHOD hasPostBody URL variableForResponseBody ");
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": connection METHOD hasPostBody URL variableForResponseBody");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else {
            HTTPConnection connection = HTTPHelper.getConnection(connections, defaultConnection, parameters);
            int offset = 1;
            if (connection == defaultConnection) {
                offset = 0;
            }
            String method = parameters[1 + offset];
            boolean hasBody = ConvertLib.textToBoolean(parameters[2 + offset]);
            String url = parameters[3 + offset];
            String variableForBody = parameters[4 + offset];

            try {

                connection.sendFreeRequest(parameters[0], url, method, hasBody);
                if (variableForBody != null) {
                    XTTProperties.setVariable(variableForBody + "/BASE64", ConvertLib.base64Encode(connection.getResponse()));
                    XTTProperties.setVariable(variableForBody + "/PLAIN", ConvertLib.createString(connection.getResponse()));
                    XTTProperties.printDebug(parameters[0] + ": body stored in " + variableForBody + "/BASE64" + "\n" +
                            parameters[0] + ": body stored in " + variableForBody + "/PLAIN" + "\n");
                }
            } catch (Exception e) {
                XTTProperties.printFail(parameters[0] + ": " + e.getClass().getName() + ": " + e.getMessage());
                if (XTTProperties.printDebug(null)) {
                    XTTProperties.printException(e);
                }
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }

    /**
     * compare the http response code of the last POST/GET request with a value.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> connection name (or not present for default),
     *                   <br><code>parameters[2]</code> and following are the allowed response codes.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int checkResponseCode(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": checkResponseCode: expectedValue1 expectedvalue2 ...");
            XTTProperties.printFail(this.getClass().getName() + ": checkResponseCode: connection expectedValue1 expectedvalue2 ...");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length < 2) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": expectedValue1 expectedvalue2 ...");
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": connection expectedValue1 expectedvalue2 ...");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else {
            HTTPConnection connection = HTTPHelper.getConnection(connections, defaultConnection, parameters);
            int offset = 0;
            if (connection != defaultConnection) offset = 1;
            boolean found = false;
            StringBuffer checked = new StringBuffer();
            String divider = "";
            for (int i = 1 + offset; i < parameters.length; i++) {
                try {
                    if (connection.getResponseCode() == Integer.decode(parameters[i])) {
                        found = true;
                    }
                    checked.append(divider + parameters[i]);
                    divider = ",";
                } catch (NumberFormatException nfe) {
                    XTTProperties.printFail(parameters[0] + ": ERROR: '" + parameters[i] + "' is not a number");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                    XTTProperties.setTestStatus(status);
                    return status;
                }
            }

            if (found) {
                XTTProperties.printInfo(parameters[0] + ": found " + connection.getResponseCode() + " " + connection.getResponseMessage());
            } else {
                XTTProperties.printFail(parameters[0] + ": found " + connection.getResponseCode() + " expected " + checked.toString());
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }

    /**
     * query if the body of a http response contains a specified regular expression value.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> connection name (or not present for default),
     *                   <br><code>parameters[2]</code> argument is the variable name to store the result in,
     *                   <br><code>parameters[3]</code> argument is the java reqular expression pattern.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int queryBody(String[] parameters) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": queryBody: variableName regularExpression");
            XTTProperties.printFail(this.getClass().getName() + ": queryBody: connection variableName regularExpression");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length < 3 || parameters.length > 4) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": variableName regularExpression");
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": connection variableName regularExpression");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        } else {
            HTTPConnection connection = HTTPHelper.getConnection(connections, defaultConnection, parameters);
            int offset = 1;
            if (connection == defaultConnection) {
                offset = 0;
            }
            XTTProperties.printDebug(parameters[0] + ": regex: '" + parameters[2 + offset] + "'");

            boolean bStatus = ConvertLib.queryString(parameters[0], ConvertLib.createString(connection.getResponse()), parameters[2 + offset], parameters[1 + offset]);
            if (bStatus)
                status = XTTProperties.PASSED;
            else
                status = XTTProperties.FAILED;
        }
        return status;
    }

    /**
     * query if the response message of a http response contains a specified regular expression value.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> connection name (or not present for default),
     *                   <br><code>parameters[2]</code> argument is the variable name to store the result in,
     *                   <br><code>parameters[3]</code> argument is the java reqular expression pattern.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int queryResponseMessage(String[] parameters) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": queryResponseMessage: variableName regularExpression");
            XTTProperties.printFail(this.getClass().getName() + ": queryResponseMessage: connection variableName regularExpression");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length < 3) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": variableName regularExpression");
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": connection variableName regularExpression");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        } else {
            HTTPConnection connection = HTTPHelper.getConnection(connections, defaultConnection, parameters);
            int offset = 1;
            if (connection == defaultConnection) {
                offset = 0;
            }
            XTTProperties.printDebug(parameters[0] + ": regex: '" + parameters[2 + offset] + "'");
            boolean bStatus = ConvertLib.queryString(parameters[0], connection.getResponseMessage(), parameters[2 + offset], parameters[1 + offset]);
            if (!bStatus)
                status = XTTProperties.FAILED;

        }
        return status;
    }

    /**
     * query if the body of a http response DOES NOT contain a specified regular expression value.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> connection name (or not present for default),
     *                   <br><code>parameters[2]</code> argument is the variable name to store the result in,
     *                   <br><code>parameters[3]</code> argument is the java reqular expression pattern.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int queryBodyNegative(String[] parameters) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": queryBodyNegative: regularExpression");
            XTTProperties.printFail(this.getClass().getName() + ": queryBodyNegative: connection regularExpression");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length < 2 || parameters.length > 3) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": regularExpression");
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": connection regularExpression");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        } else {
            HTTPConnection connection = HTTPHelper.getConnection(connections, defaultConnection, parameters);
            int offset = 1;
            if (connection == defaultConnection) {
                offset = 0;
            }
            XTTProperties.printDebug(parameters[0] + ": regex: '" + parameters[2 + offset] + "'");
            boolean bStatus = ConvertLib.queryStringNegative(parameters[0], ConvertLib.createString(connection.getResponse()), parameters[2 + offset]);
            if (!bStatus)
                status = XTTProperties.FAILED;
        }
        return status;
    }

    /**
     * Set a file in cache of the webserver. This can be used to generate dynamic content during a test.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> argument is the filename of the file on the server relative to the webserver root (CONFIGURATION/WEBSERVER/ROOT),
     *                   <br><code>parameters[2]</code> argument is the content of the file.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebWorker#setCacheFile(String, String, String)
     */
    public int setCacheFile(String[] parameters) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": setCacheFile: filename content");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length != 3) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": filename content");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        } else {
            WebWorker.setCacheFile(XTTProperties.getProperty("WEBSERVER/ROOT"), parameters[1], parameters[2]);
        }
        return status;
    }

    /**
     * Set a file from base64 encoded source in cache of the webserver. This can be used to generate dynamic content during a test. the base64 file will be decoded and stored as binary.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> argument is the filename of the file on the server relative to the webserver root (CONFIGURATION/WEBSERVER/ROOT),
     *                   <br><code>parameters[2]</code> argument is the content of the file.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebWorker#setCacheFileBase64(String, String, String)
     */
    public int setCacheFileBase64(String[] parameters) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": setCacheFileBase64: filename base64EncodedContent");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length != 3) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": filename base64EncodedContent");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        } else {
            WebWorker.setCacheFileBase64(XTTProperties.getProperty("WEBSERVER/ROOT"), parameters[1], parameters[2]);
        }
        return status;
    }

    /**
     * Set a file in cache of the webserver. This can be used to generate dynamic content during a test.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> argument is the root directory of the webserver used (like CONFIGURATION/WEBSERVER/ROOT),
     *                   <br><code>parameters[2]</code> argument is the filename of the file on the server relative to the webserver root,
     *                   <br><code>parameters[3]</code> argument is the content of the file.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebWorker#setCacheFile(String, String, String)
     */
    public int setCacheFileWithRoot(String[] parameters) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": setCacheFileWithRoot: serverRootDir filename content");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length != 4) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": serverRootDir filename content");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        } else {
            WebWorker.setCacheFile(parameters[1], parameters[2], parameters[3]);
        }
        return status;
    }

    /**
     * enable certificate checking on https connections.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name, no additional parameters are requeired.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebWorker#clearCache()
     */
    public int clearCache(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": clearCache:" + NO_ARGUMENTS);
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length != 1) {
            XTTProperties.printFail(parameters[0] + ":" + NO_ARGUMENTS);
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else {
            WebWorker.clearCache();
        }
        return status;
    }

    /**
     * wait for a specified number of HTTP POST requests on the Webserver.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> argument is the number of POST requests since starting the test (meaning that cheching for a second POST would be a 2 not a 1).
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebWorker#waitForPOSTs(int)
     */
    public int waitForPOSTs(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": waitForPOSTs: numPOSTs");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length != 2) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": numPOSTs");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        } else {
            try {
                int messages = Integer.parseInt(parameters[1]);
                XTTProperties.printInfo(parameters[0] + ": waiting for " + messages + " POSTs received on WebServer");
                WebWorker.waitForPOSTs(messages);
                return status;
            } catch (NumberFormatException nfe) {
                XTTProperties.printFail(parameters[0] + ": '" + parameters[1] + "' is NOT a correct number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } catch (Exception e) {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }

    /**
     * wait for a specified timeout and no HTTP POST requests on the Webserver.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> argument is the timeout in ms.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebWorker#waitForPOSTs(int)
     */
    public int waitForTimeoutPOSTs(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": waitForTimeoutPOSTs: timeoutms");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length != 2) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": timeoutms");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        } else {
            try {
                int timeoutms = Integer.parseInt(parameters[1]);
                XTTProperties.printInfo(parameters[0] + ": waiting for " + timeoutms + "ms and NO POSTs received on WebServer");
                WebWorker.waitForTimeoutPOSTs(timeoutms);
                return status;
            } catch (NumberFormatException nfe) {
                XTTProperties.printFail(parameters[0] + ": '" + parameters[1] + "' is NOT a correct number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } catch (Exception e) {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }

    /**
     * wait for a specified number of HTTP requests on the Webserver.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> argument is the number of requests since starting the test (meaning that cheching for a second request would be a 2 not a 1)..
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebWorker#waitForRequests(int)
     */
    public int waitForRequests(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": waitForRequests: numRequests");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length != 2) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": numRequests");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        } else {
            try {
                int messages = Integer.parseInt(parameters[1]);
                XTTProperties.printInfo(parameters[0] + ": waiting for " + messages + " HTTP-Requests received on WebServer");
                WebWorker.waitForRequests(messages);
                return status;
            } catch (NumberFormatException nfe) {
                XTTProperties.printFail(parameters[0] + ": '" + parameters[1] + "' is NOT a correct number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } catch (Exception e) {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }

    //	Delay the server response
    public int setServerDelayms(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": setServerDelayms: delayms");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length != 2) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": delayms");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        } else {
            try {
                int delay = Integer.parseInt(parameters[1]);
                XTTProperties.printInfo(parameters[0] + ": setting delay to " + delay + " ms");
                WebWorker.setServerDelayms(delay);
                return status;
            } catch (NumberFormatException nfe) {
                XTTProperties.printFail(parameters[0] + ": '" + parameters[1] + "' is NOT a correct number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } catch (Exception e) {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }

    public int setServerBodyDelayms(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": setServerBodyDelayms: delayms");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length != 2) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": delayms");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        } else {
            try {
                int delay = Integer.parseInt(parameters[1]);
                XTTProperties.printInfo(parameters[0] + ": setting body delay to " + delay + " ms");
                WebWorker.setServerBodyDelayms(delay);
                return status;
            } catch (NumberFormatException nfe) {
                XTTProperties.printFail(parameters[0] + ": '" + parameters[1] + "' is NOT a correct number");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
            } catch (Exception e) {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }

    public int setServerPartDelayms(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": setServerPartDelayms: delayms");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length != 2) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": delayms");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        } else {
            try {
                int delay = Integer.parseInt(parameters[1]);
                XTTProperties.printInfo(parameters[0] + ": setting body part delay to " + delay + " ms");
                WebWorker.setServerPartDelayms(delay);
                return status;
            } catch (NumberFormatException nfe) {
                XTTProperties.printFail(parameters[0] + ": '" + parameters[1] + "' is NOT a correct number");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
            } catch (Exception e) {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }

    /**
     * wait for a specified number of HTTP requests on the Webserver.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> argument is the timeout in ms.
     *                   <br><code>parameters[2]</code> argument is the number of requests.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebWorker#waitForTimeoutRequests(int, int)
     */
    public int waitForTimeoutRequests(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": waitForTimeoutRequests: timeoutms");
            XTTProperties.printFail(this.getClass().getName() + ": waitForTimeoutRequests: timeoutms maxPreviousRequests");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length < 2 || parameters.length > 3) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": timeoutms");
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": timeoutms maxPreviousRequests");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        } else {
            try {
                int timeoutms = Integer.parseInt(parameters[1]);
                int maxnumber = -1;
                if (parameters.length == 3) {
                    try {
                        maxnumber = Integer.parseInt(parameters[2]);
                    } catch (NumberFormatException nfe2) {
                        XTTProperties.printFail(parameters[0] + ": '" + parameters[2] + "' is NOT a correct number");
                        status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                        XTTProperties.setTestStatus(status);
                    }
                }
                XTTProperties.printInfo(parameters[0] + ": waiting for " + timeoutms + "ms and NO requests on WebServer");
                WebWorker.waitForTimeoutRequests(timeoutms, maxnumber);
                return status;
            } catch (NumberFormatException nfe) {
                XTTProperties.printFail(parameters[0] + ": '" + parameters[1] + "' is NOT a correct number");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
            } catch (Exception e) {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }

    public int lastWorkerIdToVariable(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": setVariable: variableName");
            XTTProperties.printFail(this.getClass().getName() + ": setVariable: variableName serverPort");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length < 2 || parameters.length > 3) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": variableName");
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": variableName serverPort");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else {
            int port = 0;
            if (parameters.length != 3) {
                port = XTTProperties.getIntProperty("WEBSERVER/PORT");
            } else {
                try {
                    port = Integer.parseInt(parameters[2]);
                } catch (NumberFormatException nfe) {
                    XTTProperties.printFail(parameters[0] + ": '" + parameters[2] + "' is NOT a number");
                    status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                    XTTProperties.setTestStatus(status);
                }
            }
            int numworkers = WebServer.getLastWorkerId(port + "");
            XTTProperties.printInfo(parameters[0] + ": stored '" + numworkers + "' to " + parameters[1]);
            XTTProperties.setVariable(parameters[1], "" + numworkers);
        }
        return status;
    }

    /**
     * Sets the size of the chunks in a POST body if chunking is enabled.
     * <p>
     */
    public int setTransferChunkSize(String[] parameters) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": setTransferChunkSize: chunkSize");
            XTTProperties.printFail(this.getClass().getName() + ": setTransferChunkSize: connection chunkSize");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length < 2 || parameters.length > 3) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": chunkSize");
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": connection chunkSize");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else {
            HTTPConnection connection = null;
            int chunkSize = 0;
            try {
                if (parameters.length == 2) {
                    chunkSize = Integer.parseInt(parameters[1]);
                    connection = defaultConnection;
                } else {
                    chunkSize = Integer.parseInt(parameters[2]);
                    connection = connections.get(parameters[1].toLowerCase());
                    if (connection == null) {
                        connection = defaultConnection;
                        XTTProperties.printWarn(parameters[0] + ": connection '" + parameters[1] + "' not found, using default");
                    }
                }

                connection.setTransferChunkSize(chunkSize);
            } catch (NumberFormatException nfe) {
                XTTProperties.printFail(parameters[0] + ": chunkSize needs to be a number");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }

    /**
     * Take out all images and links from an html document and store them under a variable. All links are stored as Absolute urls derived from the base url.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> argument is the variable to store the result.
     *                   <br><code>parameters[2]</code> argument is the url of the html document.
     *                   <br><code>parameters[3]</code> argument is the html document itself.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebWorker#waitForTimeoutRequests(int, int)
     */
    public int decodeHTML(String parameters[]) {
        int status = XTTProperties.PASSED;

        if (parameters == null) {
            XTTProperties.printFail(this.getClass().getName() + ": decodeHTML: variableName baseURL htmlDocument");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length != 4) {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": variableName baseURL htmlDocument");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        } else {
            boolean bStatus = ConvertLib.decodeHTML(parameters[0], parameters[1], parameters[2], parameters[3]);
            if (!bStatus)
                status = XTTProperties.FAILED;
        }
        return status;
    }


    /**
     * Called for selftest purposes to see if this FunctionModules resources are avaiable.
     */
    public String checkResources() {
        int securePort = XTTProperties.getIntProperty("WEBSERVER/SECUREPORT");
        int standardPort = XTTProperties.getIntProperty("WEBSERVER/PORT");

        String resourceString = null;

        try {
            if (standardPort > 0) {
                java.net.ServerSocket stndrdPrt = new java.net.ServerSocket(standardPort);
                stndrdPrt.close();
            }
        } catch (java.net.BindException be) {
            resourceString = "" + this.getClass().getName() + ":" + RESOURCE_PORT + " '" + standardPort + "'";
        } catch (java.io.IOException ioe) {
        }

        try {
            if (securePort > 0) {
                java.net.ServerSocket scrPrt = new java.net.ServerSocket(securePort);
                scrPrt.close();
            }
        } catch (java.net.BindException be) {
            if (resourceString == null) {
                resourceString = "" + this.getClass().getName() + ":" + RESOURCE_PORT + " '" + securePort + "'";
            } else {
                resourceString += ",'" + securePort + "'";
            }
        } catch (java.io.IOException ioe) {
        }

        if (resourceString == null) {
            resourceString = "" + this.getClass().getName() + ":" + RESOURCE_OK;
        }

        return resourceString;
    }

    /**
     * returns the getClass().getName() of this object. For debug reasons only.
     */
    public String toString() {
        return this.getClass().getName();
    }

    public static final String tantau_sccsid = "@(#)$Id: FunctionModule_HTTP.java,v 1.65 2010/05/05 08:12:36 rajesh Exp $";

}

