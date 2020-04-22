package com.mobixell.xtt;

import java.util.LinkedHashMap;

/**
 * FunctionModule_STI provides HTTP and HTTPS GET functions.
 *
 * @author      Roger Soder
 * @version     $Id: FunctionModule_MMSC.java,v 1.21 2010/03/18 12:08:34 rajesh Exp $
 */
public class FunctionModule_MMSC extends FunctionModule
{
    private MMSCServer s                     = null;
    private Thread ws                       = null;
    private final static String CRLF        = "\r\n";
    private LinkedHashMap<String,String> sendHeader = new LinkedHashMap<String,String>();

    /**
     * clears and reinitializes all the variables.
     */
    public void initialize()
    {
        sendHeader           = new LinkedHashMap<String,String>();
        MMSCWorker.setServerSendHeader(new LinkedHashMap<String,String>());
        MMSCWorker.setXMLNSENV(XTTProperties.getProperty("MMSCSERVER/XMLNSENV"));
        MMSCWorker.setXMLNSMM7(XTTProperties.getProperty("MMSCSERVER/XMLNSMM7"));
        MMSCWorker.setMM7Version(XTTProperties.getProperty("MMSCSERVER/MM7VERSION"));
        MMSCWorker.init();
        MMSCServer.resetWorkerId();
        XTTProperties.printDebug(this.getClass().getName()+".initialize(): clearing variables");
    }

    /**
     * constructor.
     */
    public FunctionModule_MMSC()
    {
    }

    /**
     * Returns the Configuration Options as a String ready to copy/paste in a configuration file
     *
     */
    public String getConfigurationOptions()
    {
        return "     <!-- function module MMSC -->"
            +"\n     <MMSCServer>"
            +"\n        <!-- the listening port of the internal webserver -->"
            +"\n        <Port>4007</Port>"
            +"\n        <!-- the listening port of the internal secure-webserver -->"
            +"\n        <SecurePort>8443</SecurePort>"
            +"\n        <!-- the directory where the files are stored for downloading -->"
            +"\n        <Root>webroot</Root>"
            +"\n        <!-- timeout on client connections to the webserver -->"
            +"\n        <Timeout>30000</Timeout>"
            +"\n        <xmlnsenv>http://schemas.xmlsoap.org/soap/envelope/</xmlnsenv>"
            +"\n        <xmlnsmm7>http://www.3gpp.org/ftp/Specs/archive/23_series/23.140/schema/REL-5-MM7-1-2</xmlnsmm7>"
            +"\n        <MM7Version>5.3.0</MM7Version>"
            +"\n        <MM7XSD>REL-5-MM7-1-2.xsd</MM7XSD>"
            +"\n        <SOAPXSD>soapEnvelope.xsd</SOAPXSD>"
            +"\n     </MMSCServer>";
    }

    /**
     * Overriden from superclass to add the MMSCServer and MMSCWorker version numbers.
     *
     * @see MMSCServer
     * @see MMSCWorker
     */
    public void printVersion()
    {
        super.printVersion();
        XTTProperties.printDebug(this.getClass().getName()+": MMSCServer: "+parseVersion(MMSCServer.tantau_sccsid));
        XTTProperties.printDebug(this.getClass().getName()+": MMSCWorker: "+parseVersion(MMSCWorker.tantau_sccsid));
    }
    public void showVersions()
    {
        super.showVersions();
        System.out.println(ConvertLib.createString(this.getClass().getName()+": MMSCServer: ",SHOWLENGTH) + parseVersion(MMSCServer.tantau_sccsid));
        System.out.println(ConvertLib.createString(this.getClass().getName()+": MMSCWorker: ",SHOWLENGTH) + parseVersion(MMSCWorker.tantau_sccsid));
    }

   /**
     * starts the MMSCServer as an instance of MMSCServer.
     *
     * @param parameters   array of String containing the parameters. 
     *                      <br><code>parameters[0]</code> argument is always the method name, 
     *                      <br><code>parameters[1]</code> argument is the MMSCServer port, 
     *                      <br><code>parameters[2]</code> is the MMSCServer root directory,
     *                      <br><code>parameters[3]</code> argument is the MMSCServer timeout. 
     *                      <br>If only <code>parameters[0]</code> is submitted
     *                     the parameters will be taken from the configuration xml document in XTTProperties.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see MMSCServer
     * @see XTTProperties
     */
    public boolean startMMSCServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": startMMSCServer:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": startMMSCServer: port");
            XTTProperties.printFail(this.getClass().getName()+": startMMSCServer: port rootDirectory timeOut");
            return false;
        }
        return startMMSCServer(parameters,false);
    }

    /**
     * starts the MMSCServer as an instance of MMSCServer with SSL-Encryption.
     *
     * @param parameters   array of String containing the parameters. 
     *                      <br><code>parameters[0]</code> argument is always the method name, 
     *                      <br><code>parameters[1]</code> argument is the MMSCServer port, 
     *                      <br><code>parameters[2]</code> is the MMSCServer root directory,
     *                      <br><code>parameters[3]</code> argument is the MMSCServer timeout. 
     *                      <br>If only <code>parameters[0]</code> is submitted the parameters will be taken from the configuration xml document in XTTProperties.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see MMSCServer
     * @see XTTProperties
     */
    public boolean startSecureMMSCServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": startSecureMMSCServer:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": startSecureMMSCServer: port");
            XTTProperties.printFail(this.getClass().getName()+": startSecureMMSCServer: port rootDirectory timeOut");
            return false;
        }
        return startMMSCServer(parameters,true);
    }

    private boolean startMMSCServer(String parameters[],boolean secure)
    {
        String securetext="non-secure";
        if(secure)securetext="secure";
        if(parameters.length == 1||parameters.length == 2)
        {
            try
            {
                XTTProperties.printVerbose(parameters[0] + ": Starting "+securetext+" MMSCServer");
                String porttype="";
                if(secure)porttype="SECURE";
                int port=0;
                if(parameters.length>1)
                {
                    port=Integer.parseInt(parameters[1]);
                } else
                {
                    port=XTTProperties.getIntProperty("MMSCSERVER/"+porttype+"PORT");
                }
                s = new MMSCServer(port, XTTProperties.getProperty("MMSCSERVER/ROOT"),XTTProperties.getIntProperty("MMSCSERVER/TIMEOUT"),secure);
                ws=(new Thread(s, "MMSCServer"));
                ws.start();
                XTTProperties.printDebug(parameters[0] + ": Started "+securetext+" MMSCServer");
                return true;
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0]+": "+e.getClass().getName()+": "+e.getMessage());
                if(XTTProperties.printDebug(null))
                {
                    e.printStackTrace();
                }
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return false;
            }
        } else if (parameters.length==4)
        {
            try
            {
                XTTProperties.printVerbose(parameters[0] + ": Starting "+securetext+" MMSCServer");
                s = new MMSCServer(Integer.parseInt(parameters[1]), parameters[2], Integer.parseInt(parameters[3]),secure);
                ws=(new Thread(s, "MMSCServer"));
                ws.start();
                XTTProperties.printDebug(parameters[0] + ": Started "+securetext+" MMSCServer");
                return true;
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return false;
            }
        } else
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": port");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": port rootDirectory timeOut");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return false;
        }
    }

    /**
     * stops all/one MMSCServers and all it's threads.
     *
     * @param parameters   array of String containing the parameters. 
     *                      <br><code>parameters[0]</code> argument is always the method name, 
     *                      no additional parameters are requeired, the optional <code>parameters[1]</code> argument is the MMSCServer port
     *                     of the MMSCServer to stop, if omitted all running servers are stopped.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see MMSCServer#closeSocket(String)
     * @see MMSCServer#closeSockets()
     */
    public boolean stopMMSCServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": stopMMSCServer:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": stopMMSCServer: port");
            return false;
        }
        if(parameters.length==2)
        {
            try
            {
                XTTProperties.printInfo(parameters[0] + ": Stopping MMSCServer on port "+parameters[1]);
                MMSCServer.closeSocket(parameters[1]);
                return true;
            }catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        } else
        {
            XTTProperties.printWarn(parameters[0] + ": Stopping all MMSCServers");
            try
            {
                MMSCServer.closeSockets();
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
     * stops the MMSCServer and all it's threads.
     *
     * @see FunctionModule_MMSC#stopMMSCServer(java.lang.String[])
     */
    public boolean stopSecureMMSCServer(String parameters[])
    {
        return stopMMSCServer(parameters);
    }

    /**
     * Overrides the normal HTTP Return code with a custom code.
     *
     * @param parameters   array of String containing the parameters. 
     *                      <br><code>parameters[0]</code> argument is always the method name, 
     *                      <br><code>parameters[1]</code> is an integer representing the http code (0 or less means no override).
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see MMSCWorker#setOverrideReturnCode(int)
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
                MMSCWorker.setOverrideReturnCode(Integer.parseInt(parameters[1]));
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
     * Overrides the normal HTTP Return code with a custom code.
     *
     * @param parameters   array of String containing the parameters. 
     *                      <br><code>parameters[0]</code> argument is always the method name, 
     *                      <br><code>parameters[1]</code> is an integer representing the http code (0 or less means no override).
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see MMSCWorker#setOverrideReturnCode(int)
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
                MMSCWorker.setOverrideSoapReturnCode(Integer.parseInt(parameters[1]));
                if(Integer.parseInt(parameters[1])>0)
                {
                    XTTProperties.printInfo(parameters[0] + ": setting OverrideSoapReturnCode to "+parameters[1]);
                } else
                {
                    XTTProperties.printInfo(parameters[0] + ": disabling OverrideSoapReturnCode");
                }
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0] + ": '"+parameters[1]+"' is NOT a number");
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
     * Sets a delay which will delay the response by delay*workerid miliseconds, nice if you know parallel connections get opened in order so you can keep the responses in order
     *
     * @param parameters   array of String containing the parameters. 
     *                      <br><code>parameters[0]</code> argument is always the method name, 
     *                      <br><code>parameters[1]</code> is a delay which will delay the response by (delay*(workerid+1)) miliseconds.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see MMSCWorker#setPerWorkerDelay(int)
     */
    public void setPerWorkerDelay(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setPerWorkerDelay: delayMsTimesWorkerID");
            return;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": delayMsTimesWorkerID");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            try
            {
                MMSCWorker.setPerWorkerDelay(Integer.parseInt(parameters[1]));
                if(Integer.parseInt(parameters[1])>0)
                {
                    XTTProperties.printInfo(parameters[0] + ": setting per Worker delay to "+parameters[1]);
                } else
                {
                    XTTProperties.printInfo(parameters[0] + ": disabling per Worker delay");
                }
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0] + ": '"+parameters[1]+"' is NOT a number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } catch (Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": error setting per Worker delay");
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
     * @see MMSCWorker#setServerSendHeader
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
            MMSCWorker.setServerSendHeader(new LinkedHashMap<String,String>());
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
     * @see MMSCWorker#getServerSendHeader
     */
    public void setServerHeader(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setServerHeader: headerFieldKey headerFieldValue");
            return;
        }
        HTTPHelper.setHeader(MMSCWorker.getServerSendHeader(),parameters);
    }

    /**
     * compare the http headers received by the server from the client with a value which is required.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code> argument is the header key, <code>parameters[2]</code>
     *                     argument is the header value.
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
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
        HTTPHelper.checkHeader(MMSCWorker.getServerHeader(),parameters,false);
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
        HTTPHelper.checkHeader(MMSCWorker.getServerHeader(),parameters,true);
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
        HTTPHelper.checkHeaderPart(MMSCWorker.getServerHeader(),parameters,false);
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
        HTTPHelper.checkHeaderPart(MMSCWorker.getServerHeader(),parameters,true);
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
            if(surl.equals(MMSCWorker.getServerRecievedURL()))
            {
                XTTProperties.printInfo(parameters[0]+": found URL "+MMSCWorker.getServerRecievedURL());
            } else
            {
                XTTProperties.printFail(parameters[0]+": found URL "+MMSCWorker.getServerRecievedURL()+" wanted "+surl);
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
        } else
        {
            XTTProperties.printDebug(parameters[0]+": regex: '"+parameters[2]+"'");
            return ConvertLib.queryString(parameters[0],MMSCWorker.getServerRecievedURL(),parameters[2],parameters[1]);
        }
    }    

    /**
     * query if the body of a http response contains a specified regular expression value.
     *
     * @param parameters   array of String containing the parameters. 
     *                      <br><code>parameters[0]</code> argument is always the method name, 
     *                      <br><code>parameters[1]</code> argument is the filename of the file on the server relative to the MMSCServer root (CONFIGURATION/MMSCServer/ROOT), 
     *                      <br><code>parameters[2]</code>
     *                     argument is the content of the file.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see MMSCWorker#setCacheFile(String,String,String)
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
        } else
        {
            MMSCWorker.setCacheFile(XTTProperties.getProperty("MMSCServer/ROOT"),parameters[1],parameters[2]);
        }
    }
    /**
     * query if the body of a http response contains a specified regular expression value.
     *
     * @param parameters   array of String containing the parameters. 
     *                      <br><code>parameters[0]</code> argument is always the method name, 
     *                      <br><code>parameters[1]</code> argument is the root directory of the MMSCServer used (like CONFIGURATION/MMSCServer/ROOT),
     *                     <br><code>parameters[2]</code> argument is the filename of the file on the server relative to the MMSCServer root,
     *                     <br><code>parameters[3]</code> argument is the content of the file.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see MMSCWorker#setCacheFile(String,String,String)
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
            MMSCWorker.setCacheFile(parameters[1],parameters[2],parameters[3]);
        }
    }
    /**
     * enable certificate checking on https connections.
     *
     * @param parameters   array of String containing the parameters. 
     *                      <br><code>parameters[0]</code> argument is always
     *                     the method name, no additional parameters are requeired.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see MMSCWorker#clearCache()
     */
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
            MMSCWorker.clearCache();
        }
    }

    /**
     * wait for a specified number of HTTP requests on the MMSCServer.
     *
     * @param parameters   array of String containing the parameters. 
     *                      <br><code>parameters[0]</code> argument is always the method name, 
     *                      <br><code>parameters[1]</code> argument is the number of requests.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see MMSCWorker#waitForRequests(int)
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
                XTTProperties.printInfo(parameters[0] + ": waiting for "+messages+" HTTP-Requests received on MMSCServer");
                MMSCWorker.waitForRequests(messages);
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
     * wait for a timeout and no HTTP requests on the MMSCServer.
     *
     * @param parameters   array of String containing the parameters. 
     *                      <br><code>parameters[0]</code> argument is always the method name, 
     *                      <br><code>parameters[1]</code> argument is the timeout in ms.
     *                      <br><code>parameters[2]</code> optional argument is the maximum number of previously recieved requests
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see MMSCWorker#waitForRequests(int)
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
            XTTProperties.setTestStatus(XTTProperties.FAILED);
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
                    }
                }
                XTTProperties.printInfo(parameters[0] + ": waiting for "+timeoutms+"ms and NO HTTP-Requests received on MMSCServer");
                MMSCWorker.waitForTimeoutRequests(timeoutms,maxnumber);
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

    public String checkResources()
    {
        int securePort = XTTProperties.getIntProperty("MMSCSERVER/SECUREPORT");
        int standardPort = XTTProperties.getIntProperty("MMSCSERVER/PORT");

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

    public static final String tantau_sccsid = "@(#)$Id: FunctionModule_MMSC.java,v 1.21 2010/03/18 12:08:34 rajesh Exp $";
}