package com.mobixell.xtt;

import java.util.LinkedHashMap;

/**
 * FunctionModule_STI provides HTTP and HTTPS GET functions.
 *
 * @author      Roger Soder
 * @version     $Id: FunctionModule_STI.java,v 1.14 2009/06/05 12:38:20 rsoder Exp $
 */
public class FunctionModule_STI extends FunctionModule
{
    private STIServer s                     = null;
    private Thread ws                       = null;
    private final static String CRLF        = "\r\n";
    private LinkedHashMap<String,String> sendHeader = new LinkedHashMap<String,String>();

    /**
     * clears and reinitializes all the variables. Does not reset the gateway.
     */
    public void initialize()
    {
        sendHeader           = new LinkedHashMap<String,String>();
        STIWorker.setServerSendHeader(new LinkedHashMap<String,String>());
        STIWorker.init();
        STIServer.resetWorkerId();
        XTTProperties.printDebug(this.getClass().getName()+".initialize(): clearing variables");
    }

    /**
     * constructor sets gateway.
     */
    public FunctionModule_STI()
    {
    }

    /**
     * Returns the Configuration Options as a String ready to copy/paste in a configuration file
     *
     */
    public String getConfigurationOptions()
    {
        return "    <!-- function module STI -->"
            +"\n    <STIServer>"
            +"\n        <!-- the listening port of the internal stiserver -->"
            +"\n        <Port>8989</Port>"
            +"\n        <!-- the listening port of the internal secure-stiserver -->"
            +"\n        <SecurePort>8443</SecurePort>"
            +"\n        <!-- the directory where the files are stored for downloading -->"
            +"\n        <Root>webroot</Root>"
            +"\n        <!-- timeout on client connections to the stiserver -->"
            +"\n        <Timeout>30000</Timeout>"
            +"\n    </STIServer>";
    }

    /**
     * Overriden from superclass to add the STIServer and STIWorker version numbers.
     *
     * @see STIServer
     * @see STIWorker
     */
    public void printVersion()
    {
        super.printVersion();
        XTTProperties.printDebug(this.getClass().getName()+": STIServer: "+parseVersion(STIServer.tantau_sccsid));
        XTTProperties.printDebug(this.getClass().getName()+": STIWorker: "+parseVersion(STIWorker.tantau_sccsid));
    }
    public void showVersions()
    {
        super.showVersions();
        System.out.println(ConvertLib.createString(this.getClass().getName()+": STIServer: ",SHOWLENGTH) + parseVersion(STIServer.tantau_sccsid));
        System.out.println(ConvertLib.createString(this.getClass().getName()+": STIWorker: ",SHOWLENGTH) + parseVersion(STIWorker.tantau_sccsid));
    }

   /**
     * starts the STIServer as an instance of STIServer.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the STIServer port,
     *                      <br><code>parameters[2]</code> is the STIServer root directory,
     *                      <br><code>parameters[3]</code> argument is the STIServer timeout.
     *                      <br>If only <code>parameters[0]</code> is submitted
     *                     the parameters will be taken from the configuration xml document in XTTProperties.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see STIServer
     * @see XTTProperties
     */
    public boolean startSTIServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": startSTIServer:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": startSTIServer: port");
            XTTProperties.printFail(this.getClass().getName()+": startSTIServer: port rootDirectory timeOut");
            return false;
        }
        return startSTIServer(parameters,false);
    }

    /**
     * starts the STIServer as an instance of STIServer with SSL-Encryption.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the STIServer port,
     *                      <br><code>parameters[2]</code> is the STIServer root directory,
     *                      <br><code>parameters[3]</code> argument is the STIServer timeout.
     *                      <br>If only <code>parameters[0]</code> is submitted
     *                     the parameters will be taken from the configuration xml document in XTTProperties.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see STIServer
     * @see XTTProperties
     */
    public boolean startSecureSTIServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": startSecureSTIServer:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": startSecureSTIServer: port");
            XTTProperties.printFail(this.getClass().getName()+": startSecureSTIServer: port rootDirectory timeOut");
            return false;
        }
        return startSTIServer(parameters,true);
    }

    private boolean startSTIServer(String parameters[],boolean secure)
    {
        String securetext="non-secure";
        if(secure)securetext="secure";
        if(parameters.length == 1)
        {
            try
            {
                XTTProperties.printVerbose(parameters[0] + ": Starting "+securetext+" STIServer");
                String porttype="";
                if(secure)porttype="SECURE";
                int port=0;
                if(parameters.length>1)
                {
                    port=Integer.parseInt(parameters[1]);
                } else
                {
                    port=XTTProperties.getIntProperty("STISERVER/"+porttype+"PORT");
                }
                s = new STIServer(port, XTTProperties.getProperty("STISERVER/ROOT"),XTTProperties.getIntProperty("STISERVER/TIMEOUT"),secure);
                ws=(new Thread(s, "STIServer"));
                ws.start();
                XTTProperties.printDebug(parameters[0] + ": Started "+securetext+" STIServer");
                return true;
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0]+": "+e.getClass().getName()+": "+e.getMessage());
                if(XTTProperties.printDebug(null))
                {
                    e.printStackTrace();
                }
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        } else if (parameters.length == 4)
        {
            try
            {
                XTTProperties.printVerbose(parameters[0] + ": Starting "+securetext+" STIServer");
                s = new STIServer(Integer.parseInt(parameters[1]), parameters[2], Integer.parseInt(parameters[3]),secure);
                ws=(new Thread(s, "STIServer"));
                ws.start();
                XTTProperties.printDebug(parameters[0] + ": Started "+securetext+" STIServer");
                return true;
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": '"+parameters[1]+"' or '"+parameters[3]+"' is NOT a correct number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        } else
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": port");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": port rootDirectory timeOut");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        }
        return false;

    }


    /**
     * stops all/one STIServers and all it's threads.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name, no additional parameters are requeired, the optional <code>parameters[1]</code> argument is the STIServer port
     *                     of the STIServer to stop, if omitted all running servers are stopped.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see STIServer#closeSocket(String)
     * @see STIServer#closeSockets()
     */
    public boolean stopSTIServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": stopSTIServer:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": stopSTIServer: port");
            return false;
        }
        if(parameters.length==2)
        {
            try
            {
                XTTProperties.printInfo(parameters[0] + ": Stopping STIServer on port "+parameters[1]);
                STIServer.closeSocket(parameters[1]);
                return true;
            }catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        } else
        {
            XTTProperties.printWarn(parameters[0] + ": Stopping all STIServers");
            try
            {
                STIServer.closeSockets();
                return true;
            }catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
        return false;

    }
    /**
     * stops the STIServer and all it's threads.
     *
     * @see FunctionModule_STI#stopSTIServer(java.lang.String[])
     */
    public boolean stopSecureSTIServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": stopSecureSTIServer:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": stopSecureSTIServer: port");
            return false;
        }
        return stopSTIServer(parameters);
    }

    /**
     * Overrides the normal HTTP Return code with a custom code.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> is an integer representing the http code (0 or less means no override).
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see STIWorker#setOverrideReturnCode(int)
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
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": headerFieldKey");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            try
            {
                STIWorker.setOverrideReturnCode(Integer.parseInt(parameters[1]));
                if(Integer.parseInt(parameters[1])>0)
                {
                    XTTProperties.printInfo(parameters[0] + ": setting OverrideReturnCode to "+parameters[1]);
                } else
                {
                    XTTProperties.printInfo(parameters[0] + ": disabling OverrideReturnCode");
                }
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": '"+parameters[1]+"' is NOT a correct number");
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
     * Overrides the normal HTTP Return code with a custom code.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> is an integer representing the http code (0 or less means no override).
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see STIWorker#setOverrideReturnCode(int)
     */
    public void setServerSoapReturnCode(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setServerSoapReturnCode: soapReturnCode");
            return;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": soapReturnCode");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            try
            {
                STIWorker.setOverrideSoapReturnCode(Integer.parseInt(parameters[1]));
                if(Integer.parseInt(parameters[1])>0)
                {
                    XTTProperties.printInfo(parameters[0] + ": setting OverrideSoapReturnCode to "+parameters[1]);
                } else
                {
                    XTTProperties.printInfo(parameters[0] + ": disabling OverrideSoapReturnCode");
                }
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": '"+parameters[1]+"' is NOT a correct number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } catch (Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": error setting OverrideSoapReturnCode");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
    }

    /**
     * remove all the headers that are to be sent from the server to the client.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name, no additional parameters are requeired.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see STIWorker#setServerSendHeader
     */
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
            STIWorker.setServerSendHeader(new LinkedHashMap<String,String>());
        }
    }

    /**
     * set the http headers to be sent from the server to the client.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the header key,
     *                      <br><code>parameters[2]</code> argument is the header value.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     *
     * @see STIWorker#getServerSendHeader
     */
    public void setServerHeader(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setServerHeader: headerFieldKey headerFieldValue");
            return;
        }
        HTTPHelper.setHeader(STIWorker.getServerSendHeader(),parameters);
    }

    /**
     * compare the http headers received by the server from the client with a value which is required.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the header key,
     *                      <br><code>parameters[2]</code> argument is the header value.
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
        HTTPHelper.checkHeader(STIWorker.getServerHeader(),parameters,false);
    }
    /**
     * compare the http headers received by the server from the client with a value which it is prohibited.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the header key,
     *                      <br><code>parameters[2]</code> argument is the header value.
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
        HTTPHelper.checkHeader(STIWorker.getServerHeader(),parameters,true);
    }

    /**
     * check a http header received by the server from the client does contain a value.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the header key,
     *                      <br><code>parameters[2]</code> argument is the header value part.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void checkServerHeaderPart(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkServerHeaderPart: headerFieldKey expectedValuePart");
            return;
        }
        HTTPHelper.checkHeaderPart(STIWorker.getServerHeader(),parameters,false);
    }
    /**
     * check a http header received by the server from the client does NOT contain a value.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the header key,
     *                      <br><code>parameters[2]</code> argument is the header value part.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void checkServerHeaderNotPart(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkServerHeaderNotPart: headerFieldKey expectedValueNotPartOfHeader");
            return;
        }
        HTTPHelper.checkHeaderPart(STIWorker.getServerHeader(),parameters,true);
    }


    /**
     * compare the url received by the server from the client with a value.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the url value.
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
            if(surl.equals(STIWorker.getServerRecievedURL()))
            {
                XTTProperties.printInfo(parameters[0]+": found URL "+STIWorker.getServerRecievedURL());
            } else
            {
                XTTProperties.printFail(parameters[0]+": found URL "+STIWorker.getServerRecievedURL()+" wanted "+surl);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
    }

    /**
     * query if the URL of a http request recieved by the server contains a specified regular expression value.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the variable name to store the result in,
     *                      <br><code>parameters[2]</code> argument is the java reqular expression pattern.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public boolean queryServerURL(String[] parameters)
    {
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": queryServerURL: variableName regularExpression");
            return false;
        }
        if(parameters.length!=3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName regularExpression");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return false;
        }
        else
        {
            XTTProperties.printDebug(parameters[0]+": regex: '"+parameters[2]+"'");
            return ConvertLib.queryString(parameters[0],STIWorker.getServerRecievedURL(),parameters[2],parameters[1]);
        }
    }

    /**
     * query if the body of a http response contains a specified regular expression value.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the filename of the file on the server relative to the STIServer root (CONFIGURATION/STIServer/ROOT),
     *                      <br><code>parameters[2]</code> argument is the content of the file.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see STIWorker#setCacheFile(String,String,String)
     */
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
            STIWorker.setCacheFile(XTTProperties.getProperty("STIServer/ROOT"),parameters[1],parameters[2]);
        }
    }
    /**
     * query if the body of a http response contains a specified regular expression value.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the root directory of the STIServer used (like CONFIGURATION/STIServer/ROOT),
     *                      <br><code>parameters[2]</code> argument is the filename of the file on the server relative to the STIServer root,
     *                      <br><code>parameters[3]</code> argument is the content of the file.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see STIWorker#setCacheFile(String,String,String)
     */
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
            STIWorker.setCacheFile(parameters[1],parameters[2],parameters[3]);
        }
    }
    /**
     * enable certificate checking on https connections.
     *
     * @param parameters   array of String containing the parameters. <br><code>parameters[0]</code> argument is always
     *                     the method name, no additional parameters are requeired.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see STIWorker#clearCache()
     */
    public void clearCache(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": clearCache:"+NO_ARGUMENTS);
            return;
        }
        if(parameters.length>1)
        {
            XTTProperties.printWarn(parameters[0] + ":"+NO_ARGUMENTS);
        }
        STIWorker.clearCache();
    }

    /**
     * wait for a specified number of HTTP requests on the STIServer.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the number of requests.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see STIWorker#waitForRequests(int)
     */
    public boolean waitForRequests(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForRequests: numRequests");
            return false;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": numRequests");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return false;
        } else
        {
            try
            {
                int messages=Integer.parseInt(parameters[1]);
                XTTProperties.printInfo(parameters[0] + ": waiting for "+messages+" HTTP-Requests received on STIServer");
                STIWorker.waitForRequests(messages);
                return true;
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
        return false;
    }

    /**
     * wait for timeout and no HTTP requests on the STIServer.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the timeout in ms.
     *                     <br><code>parameters[2]</code> argument is the number of requests.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see STIWorker#waitForRequests(int)
     */
    public boolean waitForTimeoutRequests(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForTimeoutRequests: timeoutms");
            XTTProperties.printFail(this.getClass().getName()+": waitForTimeoutRequests: timeoutms maxPreviousRequests");
            return false;
        }
        if(parameters.length<2||parameters.length>3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": timeoutms");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": timeoutms maxPreviousRequests");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return false;
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
                        return false;
                    }
                }
                XTTProperties.printInfo(parameters[0] + ": waiting for "+timeoutms+"ms and NO requests on STIServer");
                STIWorker.waitForTimeoutRequests(timeoutms,maxnumber);
                return true;
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
        return false;
    }

   /**
     * returns the getClass().getName() of this object. For debug reasons only.
     */
    public String toString()
    {
        return this.getClass().getName();
    }

    /**
     * Called for selftest purposes to see if this FunctionModules resources are avaiable.
     *
     */
    public String checkResources()
    {
        int securePort = XTTProperties.getIntProperty("STISERVER/SECUREPORT");
        int standardPort = XTTProperties.getIntProperty("STISERVER/PORT");

        String resourceString = null;

        try
        {
            if(standardPort>0)
            {
                java.net.ServerSocket stndrdPrt = new java.net.ServerSocket(standardPort);
                stndrdPrt.close();
            }
        } catch(Exception be)
        {
            resourceString = "" + this.getClass().getName() + ":"+RESOURCE_PORT+" '" +  standardPort + "'";
        }

        try
        {
            if(securePort>0)
            {
                java.net.ServerSocket scrPrt =  new java.net.ServerSocket(securePort);
                scrPrt.close();
            }
        } catch(Exception be)
        {
            if(resourceString==null)
            {
                resourceString = "" + this.getClass().getName() + ":"+RESOURCE_PORT+" '" +  securePort + "'";
            } else
            {
                resourceString += ",'" +  securePort + "'";
            }
        }

        if(resourceString == null)
        {
            resourceString = "" + this.getClass().getName() + ":"+RESOURCE_OK;
        }

        return resourceString;
    }


    public static final String tantau_sccsid = "@(#)$Id: FunctionModule_STI.java,v 1.14 2009/06/05 12:38:20 rsoder Exp $";
}