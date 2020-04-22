package com.mobixell.xtt;

import java.io.FileNotFoundException;


/**
 * FunctionModule_EWS simulates the EWS interface of a Microsoft Outlook Server.
 *
 * @author      Roger Soder
 * @version     $Id: FunctionModule_EWS.java,v 1.7 2010/03/18 12:08:34 rajesh Exp $
 */
public class FunctionModule_EWS extends FunctionModule
{
    private WebServer s             = null;
    private Thread ws               = null;
    private static String ewsclass  = "com.mobixell.xtt.WebWorkerExtensionEWS";
    /**
     * clears and reinitializes all the variables. Does reset the proxy.
     */
    public void initialize()
    {
        //WebWorkerExtensionEWS.setServerSendHeader(new LinkedHashMap<String,String>());
        WebWorkerExtensionEWS.init();
        //WebServer.resetWorkerId();

        XTTProperties.printDebug(this.getClass().getName()+".initialize(): clearing variables");
    }


    /**
     * constructor sets proxy.
     */
    public FunctionModule_EWS()
    {
        // Do not du this, Parser will initialize!
        //initialize();
    }

    /**
     * Overriden from superclass to add the WebServer and WebWorker version numbers.
     *
     * @see WebServer
     * @see WebWorker
     */
    public void printVersion()
    {
        super.printVersion();
        XTTProperties.printDebug(this.getClass().getName()+": WebWorkerExtensionEWS : "+parseVersion(WebWorkerExtensionEWS.tantau_sccsid ));
        XTTProperties.printDebug(this.getClass().getName()+": HTTPHelper            : "+parseVersion(HTTPHelper.tantau_sccsid));
        XTTProperties.printDebug(this.getClass().getName()+": WebServer             : "+parseVersion(WebServer.tantau_sccsid ));
        XTTProperties.printDebug(this.getClass().getName()+": WebWorker             : "+parseVersion(WebWorker.tantau_sccsid ));
    }
    public void showVersions()
    {
        super.showVersions();
        System.out.println(ConvertLib.createString(this.getClass().getName()+": WebWorkerExtensionEWS : ",SHOWLENGTH) + parseVersion(WebWorkerExtensionEWS.tantau_sccsid ));
        System.out.println(ConvertLib.createString(this.getClass().getName()+": HTTPHelper            : ",SHOWLENGTH) + parseVersion(HTTPHelper.tantau_sccsid));
        System.out.println(ConvertLib.createString(this.getClass().getName()+": WebServer             : ",SHOWLENGTH) + parseVersion(WebServer.tantau_sccsid ));
        System.out.println(ConvertLib.createString(this.getClass().getName()+": WebWorker             : ",SHOWLENGTH) + parseVersion(WebWorker.tantau_sccsid ));
    }

    /**
     * Returns the Configuration Options as a String ready to copy/paste in a configuration file
     *
     */
    public String getConfigurationOptions()
    {
        return "    <!-- function module EWS -->"
        +"\n    <EWSServer>"
        +"\n        <!-- the listening port of the internal webserver -->"
        +"\n        <Port>8888</Port>"
        +"\n        <!-- the listening port of the internal secure-webserver -->"
        +"\n        <SecurePort>8883</SecurePort>"
        +"\n        <!-- the directory where the files are stored for downloading -->"
        +"\n        <Root>webroot</Root>"
        +"\n        <!-- timeout on client connections to the webserver -->"
        +"\n        <Timeout>30000</Timeout>"
        +"\n        <!-- time to wait on a \"wait\" function before continuing -->"
        +"\n        <waitTimeout>30000</waitTimeout>"
        +"\n    </EWSServer>";
    }


   /**
     * starts the webserver as an instance of WebServer.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the WebServer port,
     *                     <br><code>parameters[2]</code> is the WebServer root directory,
     *                     <br><code>parameters[3]</code> argument is the WebServer timeout.
     *                     <br>If only <code>parameters[0]</code> is submitted
     *                     the parameters will be taken from the configuration xml document in XTTProperties.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebServer
     * @see XTTProperties
     */
    public void startEWSServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": startEWSServer:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": startEWSServer: port");
            XTTProperties.printFail(this.getClass().getName()+": startEWSServer: port rootDirectory timeOut");
            return;
        }
        startEWSServer(parameters,false);
    }

    /**
     * starts the EWSserver as an instance of WebServer with SSL-Encryption.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the EWSServer port,
     *                     <br><code>parameters[2]</code> is the EWSServer root directory,
     *                     <br><code>parameters[3]</code> argument is the EWSServer timeout.
     *                     <br>If only <code>parameters[0]</code> is submitted the parameters will be taken from the configuration xml document in XTTProperties.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebServer
     * @see XTTProperties
     */
    public void startSecureEWSServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": startSecureEWSServer:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": startSecureEWSServer: port");
            XTTProperties.printFail(this.getClass().getName()+": startSecureEWSServer: port rootDirectory timeOut");
            return;
        }
        startEWSServer(parameters,true);
    }

    private void startEWSServer(String parameters[],boolean secure)
    {
        String securetext="non-secure";
        if(secure)securetext="secure";
        int port=-1;
        String porttype="";
        if(parameters.length == 1||parameters.length == 2)
        {
            try
            {
                XTTProperties.printVerbose(parameters[0] + ": Starting "+securetext+" EWS Server");
                if(secure)porttype="SECURE";
                port=XTTProperties.getIntProperty("EWSSERVER/"+porttype+"PORT");
                if(parameters.length>1)
                {
                    port=Integer.parseInt(parameters[1]);
                }
                s = new WebServer(port, XTTProperties.getProperty("EWSSERVER/ROOT"),XTTProperties.getIntProperty("EWSSERVER/TIMEOUT"),secure);
                ws=(new Thread(s, "EWSServer"));
                ws.start();
                WebServer.setDefaultWebWorkerExtension(""+port,"com.mobixell.xtt.WebWorkerExtensionEWS");
                XTTProperties.printDebug(parameters[0] + ": Started "+securetext+" EWS Server");
                return;
            } catch (ClassNotFoundException cnfe)
            {
                XTTProperties.printFail(parameters[0] + ": ClassNotFoundException: '"+ewsclass+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(cnfe);
                }
            } catch (ClassCastException cce)
            {
                XTTProperties.printFail(parameters[0] + ": ClassCastException: '"+ewsclass+"' not extending WebWorkerExtension");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(cce);
                }
            } catch (NullPointerException npe)
            {
                XTTProperties.printFail(parameters[0] + ": WebServer running on port '"+port+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(npe);
                }
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0] + ": EWSSERVER/"+porttype+"PORT='"+XTTProperties.getProperty("EWSSERVER/"+porttype+"PORT")+"' is NOT a number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } catch(FileNotFoundException fnfe)
            {
                XTTProperties.printFail(parameters[0] + ": FileNotFoundException "+fnfe.getMessage());
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(fnfe);
                }
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0]+": "+e.getClass().getName()+": "+e.getMessage());
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        } else if (parameters.length == 4)
        {
            try
            {
                XTTProperties.printVerbose(parameters[0] + ": Starting "+securetext+" EWS Server");
                port=Integer.parseInt(parameters[1]);
                s = new WebServer(port, parameters[2], Integer.parseInt(parameters[3]),secure);
                ws=(new Thread(s, "EWSServer"));
                ws.start();
                WebServer.setDefaultWebWorkerExtension(""+port,ewsclass);
                XTTProperties.printDebug(parameters[0] + ": Started "+securetext+" EWS Server");
                return;
            } catch (ClassNotFoundException cnfe)
            {
                XTTProperties.printFail(parameters[0] + ": ClassNotFoundException: '"+ewsclass+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(cnfe);
                }
            } catch (ClassCastException cce)
            {
                XTTProperties.printFail(parameters[0] + ": ClassCastException: '"+ewsclass+"' not extending WebWorkerExtension");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(cce);
                }
            } catch (NullPointerException npe)
            {
                XTTProperties.printFail(parameters[0] + ": WebServer running on port '"+port+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(npe);
                }
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0] + ": '"+parameters[1]+"' or '"+parameters[3]+"' is NOT a number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } catch(FileNotFoundException fnfe)
            {
                XTTProperties.printFail(parameters[0] + ": FileNotFoundException "+fnfe.getMessage());
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(fnfe);
                }
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0]+": "+e.getClass().getName()+": "+e.getMessage());
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        } else
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": port");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": port rootDirectory timeOut");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        }
        return;

    }


    /**
     * stops all/one EWSservers and all it's threads.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name, no additional parameters are requeired,
     *                     <br>the optional <code>parameters[1]</code> argument is the EWSServer port of the WebServer to stop, if omitted all running servers are stopped.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebServer#closeSocket(String)
     * @see WebServer#closeSockets()
     */
    public void stopEWSServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": stopEWSServer: port");
            return;
        }
        stopEWSServer(parameters,false);
    }
    private void stopEWSServer(String parameters[], boolean secure)
    {
        String porttype="";
        if(secure)porttype="SECURE";
        if(parameters.length==2)
        {
            try
            {
                XTTProperties.printInfo(parameters[0] + ": Stopping EWSServer on port "+parameters[1]);
                WebServer.closeSocket(parameters[1]);
                return;
            }catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        } else
        {
            int port=XTTProperties.getIntProperty("EWSSERVER/"+porttype+"PORT");
            XTTProperties.printWarn(parameters[0] + ": Stopping "+porttype+" EWSServer on port "+port);
            try
            {
                WebServer.closeSocket(""+port);
                return;
            }catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
        return;

    }
    /**
     * stops the webserver and all it's threads.
     *
     * @see FunctionModule_HTTP#stopWebServer(java.lang.String[])
     */
    public void stopSecureEWSServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": stopSecureEWSServer: port");
            return;
        }
        stopEWSServer(parameters,true);
    }

    /**
     * Overrides the normal HTTP Return code with a custom code.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> is an integer representing the http code (0 or less means no override).
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebWorkerExtensionEWS#setOverrideReturnCode(int)
     */
    public void setServerReturnCode(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setServerReturnCode: httpReturnCode");
            return;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": httpReturnCode");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            try
            {
                WebWorkerExtensionEWS.setOverrideReturnCode(Integer.parseInt(parameters[1]));
                if(Integer.parseInt(parameters[1])>0)
                {
                    XTTProperties.printInfo(parameters[0] + ": setting OverrideReturnCode to "+parameters[1]);
                } else
                {
                    XTTProperties.printInfo(parameters[0] + ": disabling OverrideReturnCode");
                }
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0] + ": '"+parameters[1]+"' is NOT a number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } catch (Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": error setting OverrideReturnCode");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
    }

    /**
     * Overrides the normal HTTP Return Message with a custom code.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> is the Message after the response code including the whitespace between message and response code (for "HTTP/1.1 200 Ok" it would be " Ok").
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebWorkerExtensionEWS#setOverrideReturnCode(int)
     */
    public void setServerReturnMessage(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setServerReturnMessage:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": setServerReturnMessage: httpReturnMessage");
            return;
        }
        if(parameters.length<1||parameters.length>2)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": httpReturnMessage");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
                if(parameters.length==1)
                {
                    XTTProperties.printInfo(parameters[0] + ": disabling OverrideReturnMessage");
                    WebWorkerExtensionEWS.setOverrideReturnMessage(null);
                } else
                {
                    XTTProperties.printInfo(parameters[0] + ": setting OverrideReturnMessage to '"+parameters[1]+"'");
                    WebWorkerExtensionEWS.setOverrideReturnMessage(parameters[1]);
                }
        }
    }

    /**
     * Sets a delay in ms before sending back the response. This is done after the notify of the request but before setting overrides..
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> is an integer representing the http code (0 or less means no override).
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebWorkerExtensionEWS#setOverrideReturnCode(int)
     */
    public void setServerResponseDelay(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setServerResponseDelay: delayms");
            return;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": delayms");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            try
            {
                WebWorkerExtensionEWS.setServerResponseDelay(Integer.parseInt(parameters[1]));
                if(Integer.parseInt(parameters[1])>0)
                {
                    XTTProperties.printInfo(parameters[0] + ": setting response delay to "+parameters[1]+" ms");
                } else
                {
                    XTTProperties.printInfo(parameters[0] + ": disabling response delay");
                }
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0] + ": '"+parameters[1]+"' is NOT a number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } catch (Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": error setting OverrideReturnCode");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
    }

    /*
     * remove all the headers that are to be sent from the server to the client.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name, no additional parameters are requeired.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebWorkerExtensionEWS#setServerSendHeader
     */
     /*
    public void clearServerHeader(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": clearServerHeader:"+NO_ARGUMENTS);
            return;
        }
        if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            XTTProperties.printInfo(parameters[0] + ": clearing header");
            WebWorkerExtensionEWS.setServerSendHeader(new LinkedHashMap<String,String>());
        }
    }*/

    /*
     * set the http headers to be sent from the server to the client.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the header key,
     *                     <br><code>parameters[2]</code> argument is the header value.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     *
     * @see WebWorkerExtensionEWS#getServerSendHeader
     *//*
    public void setServerHeader(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setServerHeader: headerFieldKey headerFieldValue");
            return;
        }
        HTTPHelper.setHeader(WebWorkerExtensionEWS.getServerSendHeader(),parameters);
    }*/


    /**
     * compare the http headers received by the server from the client with a value which is required.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the header key,
     *                     <br><code>parameters[2]</code> argument is the header value.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void checkServerHeader(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkServerHeader: headerFieldKey");
            XTTProperties.printFail(this.getClass().getName()+": checkServerHeader: headerFieldKey expectedValue");
            return;
        }
        HTTPHelper.checkHeader(WebWorkerExtensionEWS.getServerHeader(),parameters,false);
    }

    /**
     * query the http headers received by the server from the client with a regular expression.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the variable to store the result to,
     *                     <br><code>parameters[2]</code> argument is the header key,
     *                     <br><code>parameters[3]</code> argument is the regular expression.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void queryServerHeader(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": queryServerHeader: variable headerFieldKey regularExpression");
            return;
        }
        if(parameters.length<4)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": variable headerFieldKey regularExpression");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            HTTPHelper.queryHeader(WebWorkerExtensionEWS.getServerHeader(),parameters,false);
        }
    }
    /**
     * query the http headers received by the server from the client with a regular expression.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the header key,
     *                     <br><code>parameters[2]</code> argument is the regular expression.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void queryServerHeaderNegative(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": queryServerHeaderNegative: headerFieldKey regularExpression");
            return;
        }
        if(parameters.length!=3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": headerFieldKey regularExpression");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            HTTPHelper.queryHeader(WebWorkerExtensionEWS.getServerHeader(),parameters,true);
        }
    }
    /**
     * compare the http headers received by the server from the client with a value which it is prohibited.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the header key,
     *                     <br><code>parameters[2]</code> argument is the header value.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void checkServerHeaderNot(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkServerHeaderNot: headerFieldKey");
            XTTProperties.printFail(this.getClass().getName()+": checkServerHeaderNot: headerFieldKey expectedValue");
            return;
        }
        HTTPHelper.checkHeader(WebWorkerExtensionEWS.getServerHeader(),parameters,true);
    }

    /**
     * compare the url received by the server from the client with a value.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the url value.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void checkServerURL(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkServerURL: URL");
            return;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": URL");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
             String surl=parameters[1];
            if(surl.equals(WebWorkerExtensionEWS.getServerRecievedURL()))
            {
                XTTProperties.printInfo(parameters[0]+": found URL "+WebWorkerExtensionEWS.getServerRecievedURL());
            } else
            {
                XTTProperties.printFail(parameters[0]+": found URL "+WebWorkerExtensionEWS.getServerRecievedURL()+" wanted "+surl);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
    }

    /**
     * query if the URL of a http request recieved by the server contains a specified regular expression value.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the variable name to store the result in,
     *                     <br><code>parameters[2]</code> argument is the java reqular expression pattern.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void queryServerURL(String[] parameters)
    {
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": queryServerURL: variableName regularExpression");
            return;
        }
        if(parameters.length!=3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName regularExpression");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        }
        else
        {
            XTTProperties.printDebug(parameters[0]+": regex: '"+parameters[2]+"'");
            ConvertLib.queryString(parameters[0],WebWorkerExtensionEWS.getServerRecievedURL(),parameters[2],parameters[1]);
        }
    }

    public void createSendNotification(String[] parameters)
    {
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": createSendNotification: variableName subscriptionId watermark");
            XTTProperties.printFail(this.getClass().getName()+": createSendNotification: variableName subscriptionId watermark folderId1 unreadcount1...");
            return;
        }
        System.out.println("Arg: "+parameters.length);
        System.out.println("Arg: "+((parameters.length-4)%2));
        if(parameters.length<4||((parameters.length-4)%2)!=0)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName subscriptionId watermark");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName subscriptionId watermark folderId1 unreadcount1...");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        }
        else
        {
            try
            {
                String subscriptionId=parameters[2];
                String watermark=parameters[3];
                
                String snotstart="<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                                +"\n<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">"
                                +"\n<soap:Body>"
                                +"\n<m:SendNotification xmlns:t=\"http://schemas.microsoft.com/exchange/services/2006/types\" xmlns:m=\"http://schemas.microsoft.com/exchange/services/2006/messages\">"
                                +"\n    <m:ResponseMessages>"
                                +"\n        <m:SendNotificationResponseMessage ResponseClass=\"Success\">"
                                +"\n            <m:ResponseCode>NoError</m:ResponseCode>"
                                +"\n            <m:Notification>"
                                +"\n                <t:SubscriptionId>"+subscriptionId+"</t:SubscriptionId>"
                                +"\n                <t:PreviousWatermark>"+watermark+"</t:PreviousWatermark>"
                                +"\n                <t:MoreEvents>false</t:MoreEvents>";
                String snotmid  ="";
                if(parameters.length==4)
                {
                    snotmid     ="\n                <t:StatusEvent>"
                                +"\n                    <t:Watermark>"+watermark+"</t:Watermark>"
                                +"\n                </t:StatusEvent>";
                } else
                {
                    String timestamp=HTTPHelper.createISODate();
                    StringBuffer snotmidb=new StringBuffer("");
                    String folderId=null;
                    String itemid=null;
                    int unreadcount=0;
                    for(int i=4;i<parameters.length;i+=2)
                    {
                        folderId=parameters[i];
                        unreadcount=Integer.decode(parameters[i+1]);
                        itemid=ConvertLib.base64Encode("XTT ITEM FOR "+ConvertLib.createString(ConvertLib.base64Decode(folderId)));
                        // Watermark should be counting up, but meh
                        // Parent folder if should be different too, but meh, SLM doesn't care
                        snotmidb.append(
                                 "\n                <t:ModifiedEvent>"
                                +"\n                    <t:Watermark>"+watermark+"</t:Watermark>"
                                +"\n                    <t:TimeStamp>"+timestamp+"</t:TimeStamp>"
                                +"\n                    <t:FolderId       Id=\""+folderId+"\" ChangeKey=\"AQAAAA==\" />"
                                +"\n                    <t:ParentFolderId Id=\""+folderId+"\" ChangeKey=\"AQAAAA==\" />"
                                +"\n                    <t:UnreadCount>"+unreadcount+"</t:UnreadCount>"
                                +"\n                </t:ModifiedEvent>"
                                +"\n                <t:ModifiedEvent>"
                                +"\n                    <t:Watermark>"+watermark+"</t:Watermark>"
                                +"\n                    <t:TimeStamp>"+timestamp+"</t:TimeStamp>"
                                +"\n                    <t:ItemId Id=\""+itemid+"\" ChangeKey=\"CQAAAA==\" />"
                                +"\n                    <t:ParentFolderId Id=\""+folderId+"\" ChangeKey=\"AQAAAA==\" />"
                                +"\n                </t:ModifiedEvent>"
                                );
                    }
                    snotmid=snotmidb.toString();
                }
                String snotend  ="\n            </m:Notification>"
                                +"\n        </m:SendNotificationResponseMessage>"
                                +"\n    </m:ResponseMessages>"
                                +"\n</m:SendNotification>"
                                +"\n</soap:Body>"
                                +"\n</soap:Envelope>";
                String submitnote=snotstart+snotmid+snotend;
                XTTProperties.printInfo(parameters[0]+": stored "+submitnote.length()+" characters of SOAP in "+parameters[1]);
                XTTProperties.setVariable(parameters[1],submitnote);
            } catch (Exception uhe)
            {
                XTTProperties.printFail(parameters[0] + ":: Unable to set create SOAP: "+uhe.getMessage());
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(uhe);
                }
            }
        }
    }
    
    /*
     * Set a file in cache of the webserver. This can be used to generate dynamic content during a test.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the filename of the file on the server relative to the webserver root (CONFIGURATION/WEBSERVER/ROOT),
     *                     <br><code>parameters[2]</code> argument is the content of the file.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebWorkerExtensionEWS#setCacheFile(String,String,String)
     *//*
    public void setCacheFile(String[] parameters)
    {
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setCacheFile: filename content");
            return;
        }
        if(parameters.length!=3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": filename content");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        }
        else
        {
            WebWorkerExtensionEWS.setCacheFile(XTTProperties.getProperty("WEBSERVER/ROOT"),parameters[1],parameters[2]);
        }
    }*/
    /*
     * Set a file from base64 encoded source in cache of the webserver. This can be used to generate dynamic content during a test. the base64 file will be decoded and stored as binary.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the filename of the file on the server relative to the webserver root (CONFIGURATION/WEBSERVER/ROOT),
     *                     <br><code>parameters[2]</code> argument is the content of the file.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebWorkerExtensionEWS#setCacheFile(String,String,String)
     *//*
    public void setCacheFileBase64(String[] parameters)
    {
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setCacheFileBase64: filename base64EncodedContent");
            return;
        }
        if(parameters.length!=3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": filename base64EncodedContent");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        }
        else
        {
            WebWorkerExtensionEWS.setCacheFileBase64(XTTProperties.getProperty("WEBSERVER/ROOT"),parameters[1],parameters[2]);
        }
    }*/
    /*
     * Set a file in cache of the webserver. This can be used to generate dynamic content during a test.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the root directory of the webserver used (like CONFIGURATION/WEBSERVER/ROOT),
     *                     <br><code>parameters[2]</code> argument is the filename of the file on the server relative to the webserver root,
     *                     <br><code>parameters[3]</code> argument is the content of the file.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebWorkerExtensionEWS#setCacheFile(String,String,String)
     *//*
    public void setCacheFileWithRoot(String[] parameters)
    {
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setCacheFileWithRoot: serverRootDir filename content");
            return;
        }
        if(parameters.length!=4)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": serverRootDir filename content");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        }
        else
        {
            WebWorkerExtensionEWS.setCacheFile(parameters[1],parameters[2],parameters[3]);
        }
    }*/
    /*
     * enable certificate checking on https connections.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name, no additional parameters are requeired.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebWorkerExtensionEWS#clearCache()
     *//*
    public void clearCache(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": clearCache:"+NO_ARGUMENTS);
            return;
        }
        if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            WebWorkerExtensionEWS.clearCache();
        }
    }*/

    public void setFolderReadMessages(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setFolderReadMessages: principalName distinguishedFolderId numRead");
            return;
        }
        if(parameters.length!=4)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": principalName distinguishedFolderId numRead");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            try
            {
                int messages=Integer.parseInt(parameters[3]);
                XTTProperties.printInfo(parameters[0] + ": setting read messages to "+messages+" for "+parameters[1]+" on "+parameters[2]);
                WebWorkerExtensionEWS.setRead(parameters[1],parameters[2],messages);
                return;
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": '"+parameters[3]+"' is NOT a correct number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
        return;
    }

    public void setFolderUnreadMessages(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setFolderUnreadMessages: principalName distinguishedFolderId numUnread");
            return;
        }
        if(parameters.length!=4)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": principalName distinguishedFolderId numUnread");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            try
            {
                int messages=Integer.parseInt(parameters[3]);
                XTTProperties.printInfo(parameters[0] + ": setting unread messages to "+messages+" for "+parameters[1]+" on "+parameters[2]);
                WebWorkerExtensionEWS.setUnread(parameters[1],parameters[2],messages);
                return;
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": '"+parameters[3]+"' is NOT a correct number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
        return;
    }
    
    /**
     * wait for a specified number of HTTP requests on the Webserver.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the number of requests since starting the test (meaning that cheching for a second request would be a 2 not a 1)..
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebWorkerExtensionEWS#waitForSubscribe(int)
     */
    public void waitForSubscribe(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForSubscribe: numRequests");
            return;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": numRequests");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            try
            {
                int messages=Integer.parseInt(parameters[1]);
                XTTProperties.printInfo(parameters[0] + ": waiting for "+messages+" HTTP-Requests received on WebServer");
                WebWorkerExtensionEWS.waitForSubscribe(messages);
                return;
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": '"+parameters[1]+"' is NOT a correct number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
        return;
    }
    /**
     * wait for a specified number of HTTP requests on the Webserver.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the number of requests.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebWorkerExtensionEWS#waitForTimeoutSubscribe(int,int)
     */
    public void waitForTimeoutSubscribe(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForTimeoutSubscribe: timeoutms");
            XTTProperties.printFail(this.getClass().getName()+": waitForTimeoutSubscribe: timeoutms maxPreviousRequests");
            return;
        }
        if(parameters.length<2||parameters.length>3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": timeoutms");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": timeoutms maxPreviousRequests");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            try
            {
                int timeoutms=Integer.parseInt(parameters[1]);
                int maxnumber=-1;
                if(parameters.length==3)
                {
                    try
                    {
                        maxnumber=Integer.parseInt(parameters[2]);
                    } catch (NumberFormatException nfe2)
                    {
                        XTTProperties.printFail(parameters[0]+": '"+parameters[2]+"' is NOT a correct number");
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                        return;
                    }
                }
                XTTProperties.printInfo(parameters[0] + ": waiting for "+timeoutms+"ms and NO requests on WebServer");
                WebWorkerExtensionEWS.waitForTimeoutSubscribe(timeoutms,maxnumber);
                return;
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": '"+parameters[1]+"' is NOT a correct number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
        return;

    }

    /**
     * wait for a specified number of HTTP requests on the Webserver.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the number of requests since starting the test (meaning that cheching for a second request would be a 2 not a 1)..
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebWorkerExtensionEWS#waitForGetFolder(int)
     */
    public void waitForGetFolder(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForGetFolder: numRequests");
            return;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": numRequests");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            try
            {
                int messages=Integer.parseInt(parameters[1]);
                XTTProperties.printInfo(parameters[0] + ": waiting for "+messages+" HTTP-Requests received on WebServer");
                WebWorkerExtensionEWS.waitForGetFolder(messages);
                return;
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": '"+parameters[1]+"' is NOT a correct number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
        return;
    }
    /**
     * wait for a specified number of HTTP requests on the Webserver.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the number of requests.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see WebWorkerExtensionEWS#waitForTimeoutGetFolder(int,int)
     */
    public void waitForTimeoutGetFolder(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForTimeoutGetFolder: timeoutms");
            XTTProperties.printFail(this.getClass().getName()+": waitForTimeoutGetFolder: timeoutms maxPreviousRequests");
            return;
        }
        if(parameters.length<2||parameters.length>3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": timeoutms");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": timeoutms maxPreviousRequests");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            try
            {
                int timeoutms=Integer.parseInt(parameters[1]);
                int maxnumber=-1;
                if(parameters.length==3)
                {
                    try
                    {
                        maxnumber=Integer.parseInt(parameters[2]);
                    } catch (NumberFormatException nfe2)
                    {
                        XTTProperties.printFail(parameters[0]+": '"+parameters[2]+"' is NOT a correct number");
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                        return;
                    }
                }
                XTTProperties.printInfo(parameters[0] + ": waiting for "+timeoutms+"ms and NO requests on WebServer");
                WebWorkerExtensionEWS.waitForTimeoutGetFolder(timeoutms,maxnumber);
                return;
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": '"+parameters[1]+"' is NOT a correct number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
        return;

    }
    
    /**
     * Called for selftest purposes to see if this FunctionModules resources are avaiable.
     *
     */
    public String checkResources()
    {
        int securePort = XTTProperties.getIntProperty("EWSSERVER/SECUREPORT");
        int standardPort = XTTProperties.getIntProperty("EWSSERVER/PORT");

        String resourceString = null;

        try
        {
            if(standardPort>0)
            {
                java.net.ServerSocket stndrdPrt = new java.net.ServerSocket(standardPort);
                stndrdPrt.close();
            }
        }
        catch(java.net.BindException be)
        {
            resourceString = "" + this.getClass().getName() + ": Unavailable port '" +  standardPort + "'";
        }
        catch(java.io.IOException ioe){}

        try
        {
            if(securePort>0)
            {
                java.net.ServerSocket scrPrt =  new java.net.ServerSocket(securePort);
                scrPrt.close();
            }
        }
        catch(java.net.BindException be)
        {
            if(resourceString==null)
            {
                resourceString = "" + this.getClass().getName() + ": Unavailable port  '" +  securePort + "'";
            }
            else
            {
                resourceString += ",'" +  securePort + "'";
            }
        }
        catch(java.io.IOException ioe){}

        if(resourceString == null)
        {
            resourceString = "" + this.getClass().getName() + ": OK";
        }

        return resourceString;
    }

    /**
     * returns the getClass().getName() of this object. For debug reasons only.
     */
    public String toString()
    {
        return this.getClass().getName();
    }

    public static final String tantau_sccsid = "@(#)$Id: FunctionModule_EWS.java,v 1.7 2010/03/18 12:08:34 rajesh Exp $";


}

