package com.mobixell.xtt;

import java.util.LinkedHashMap;
import java.util.Vector;

/**
 * FunctionModule_ICAP provides functions to itneract with the ICAP Server.
 *
 * @author      Roger Soder
 * @version     $Id: FunctionModule_ICAP.java,v 1.8 2009/06/05 12:38:19 rsoder Exp $
 * @see ICAPServer
 * @see ICAPWorker
 */
public class FunctionModule_ICAP extends FunctionModule
{
    private ICAPServer s                     = null;
    private Thread ws                       = null;
    private LinkedHashMap<String,String> methods=null;

    /**
     * clears and reinitializes all the variables.
     */
    public void initialize()
    {
        new LinkedHashMap<String,String>();
        ICAPWorker.setServerSendICAPHeader(new LinkedHashMap<String,String>());
        ICAPWorker.setServerSendREQHeader( new LinkedHashMap<String,String>());
        ICAPWorker.setServerSendRESPHeader(new LinkedHashMap<String,String>());
        ICAPWorker.setServerSendOptions(   new LinkedHashMap<String,String>());

        methods=new LinkedHashMap<String,String>();
        org.jdom.Element[] elements=XTTXML.getElements("ICAPSERVER/OPTIONS/METHODS/REQMOD/URL");
        for(int i=0;elements!=null&&i<elements.length;i++)
        {
            if(elements[i].getName().equalsIgnoreCase("url"))
            {
                methods.put(elements[i].getText(),"REQMOD");
            }
        }
        elements=XTTXML.getElements("ICAPSERVER/OPTIONS/METHODS/RESPMOD/URL");
        for(int i=0;elements!=null&&i<elements.length;i++)
        {
            if(elements[i].getName().equalsIgnoreCase("url"))
            {
                methods.put(elements[i].getText(),"RESPMOD");
            }
        }
        String method=XTTProperties.getProperty("ICAPSERVER/OPTIONS/METHODS/DEFAULT");
        if(method==null||method.equals("null"))method="REQMOD";
        ICAPWorker.setDefaultMethod(method);
        String fullStreaming=XTTProperties.getProperty("ICAPSERVER/OPTIONS/DISABLEFULLSTREAMING");
        if(fullStreaming==null||fullStreaming.equals("null"))
        {
            ICAPWorker.setEnableFullStreaming(true);
        } else
        {
            ICAPWorker.setEnableFullStreaming(false);
        }
        String keepAlive=XTTProperties.getProperty("ICAPSERVER/OPTIONS/ENABLEKEEPALIVE");
        if(keepAlive==null||keepAlive.equals("null"))
        {
            ICAPWorker.setDefaultKeepAlive(false);
        } else
        {
            ICAPWorker.setDefaultKeepAlive(true);
        }

        ICAPWorker.init();
        ICAPServer.resetWorkerId();
        XTTProperties.printDebug(this.getClass().getName()+".initialize(): clearing variables");
    }

    /**
     * constructor.
     */
    public FunctionModule_ICAP()
    {
    }

    /**
     * Returns the Configuration Options as a String ready to copy/paste in a configuration file
     *
     */
    public String getConfigurationOptions()
    {
        return "    <!-- function module ICAP -->"
            +"\n    <ICAPServer>"
            +"\n        <!-- the listening port of the internal icapserver -->"
            +"\n        <Port>1344</Port>"
            +"\n        <!-- the listening port of the internal secure-icapserver -->"
            +"\n        <SecurePort>4344</SecurePort>"
            +"\n        <!-- timeout on client connections to the icapserver -->"
            +"\n        <Timeout>30000</Timeout>"
            +"\n        <!-- timeout on client connections to the icapserver -->"
            +"\n        <WaitTimeout>30000</WaitTimeout>"
            +"\n        <Options>"
            +"\n            <!-- disable full streaming mode -->"
            +"\n            <!--disableFullStreaming/-->"
            +"\n            <!-- enable default keep alive -->"
            +"\n            <!--enableKeepAlive/-->"
            +"\n            <methods>"
            +"\n                <!-- if not set default is REQMOD -->"
            +"\n                <default>RESPMOD</default>"
            +"\n                <!-- Test is done with startsWith and a loop so do not use too many -->"
            +"\n                <reqmod>"
            +"\n                    <url>/reqmod</url>"
            +"\n                    <url>/type1</url>"
            +"\n                </reqmod>"
            +"\n                <respmod>"
            +"\n                    <url>/respmod</url>"
            +"\n                </respmod>"
            +"\n            </methods>"
            +"\n        </Options>"
            +"\n     </ICAPServer>";
    }

    /**
     * Overriden from superclass to add the ICAPServer and ICAPWorker version numbers.
     *
     * @see ICAPServer
     * @see ICAPWorker
     */
    public void printVersion()
    {
        super.printVersion();
        XTTProperties.printDebug(this.getClass().getName()+": ICAPServer: "+parseVersion(ICAPServer.tantau_sccsid));
        XTTProperties.printDebug(this.getClass().getName()+": ICAPWorker: "+parseVersion(ICAPWorker.tantau_sccsid));
    }
    public void showVersions()
    {
        super.showVersions();
        System.out.println(ConvertLib.createString(this.getClass().getName()+": ICAPServer: ",SHOWLENGTH) + parseVersion(ICAPServer.tantau_sccsid));
        System.out.println(ConvertLib.createString(this.getClass().getName()+": ICAPWorker: ",SHOWLENGTH) + parseVersion(ICAPWorker.tantau_sccsid));
    }

   /**
     * starts the ICAPServer as an instance of ICAPServer.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the ICAPServer port,
     *                      <br><code>parameters[2]</code> is the ICAPServer root directory,
     *                      <br><code>parameters[3]</code> argument is the ICAPServer timeout.
     *                      <br>If only <code>parameters[0]</code> is submitted
     *                     the parameters will be taken from the configuration xml document in XTTProperties.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see ICAPServer
     * @see XTTProperties
     */
    public boolean startICAPServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": startICAPServer:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": startICAPServer: port");
            XTTProperties.printFail(this.getClass().getName()+": startICAPServer: port timeOut");
            return false;
        }
        return startICAPServer(parameters,false);
    }

    /**
     * starts the ICAPServer as an instance of ICAPServer with SSL-Encryption.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the ICAPServer port,
     *                      <br><code>parameters[2]</code> is the ICAPServer root directory,
     *                      <br><code>parameters[3]</code> argument is the ICAPServer timeout.
     *                      <br>If only <code>parameters[0]</code> is submitted the parameters will be taken from the configuration xml document in XTTProperties.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see ICAPServer
     * @see XTTProperties
     */
    public boolean startSecureICAPServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": startSecureICAPServer:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": startSecureICAPServer: port");
            XTTProperties.printFail(this.getClass().getName()+": startSecureICAPServer: port timeOut");
            return false;
        }
        return startICAPServer(parameters,true);
    }

    private boolean startICAPServer(String parameters[],boolean secure)
    {
        String securetext="non-secure";
        if(secure)securetext="secure";
        if(parameters.length == 1||parameters.length == 2)
        {
            try
            {
                XTTProperties.printVerbose(parameters[0] + ": Starting "+securetext+" ICAPServer");
                String porttype="";
                if(secure)porttype="SECURE";
                int port=0;
                if(parameters.length>1)
                {
                    port=Integer.parseInt(parameters[1]);
                } else
                {
                    port=XTTProperties.getIntProperty("ICAPSERVER/"+porttype+"PORT");
                }
                s = new ICAPServer(port, ".",XTTProperties.getIntProperty("ICAPSERVER/TIMEOUT"),secure,methods);
                ws=(new Thread(s, "ICAPServer"));
                ws.start();
                XTTProperties.printDebug(parameters[0] + ": Started "+securetext+" ICAPServer");
                return true;
            }
            catch(Exception e)
            {
                XTTProperties.printFail(parameters[0]+": "+e.getClass().getName()+": "+e.getMessage());
                if(XTTProperties.printDebug(null))
                {
                    e.printStackTrace();
                }
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        } else if(parameters.length == 3)
        {
            try
            {
                XTTProperties.printVerbose(parameters[0] + ": Starting "+securetext+" ICAPServer");
                s = new ICAPServer(Integer.parseInt(parameters[1]), ".", Integer.parseInt(parameters[2]),secure,methods);
                ws=(new Thread(s, "ICAPServer"));
                ws.start();
                XTTProperties.printDebug(parameters[0] + ": Started "+securetext+" ICAPServer");
                return true;
            }
            catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        } else
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": port");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": port timeOut");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        }
        return false;

    }


    /**
     * stops all/one ICAPServers and all it's threads.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      no additional parameters are requeired, the optional <code>parameters[1]</code> argument is the ICAPServer port
     *                     of the ICAPServer to stop, if omitted all running servers are stopped.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see ICAPServer#closeSocket(String)
     * @see ICAPServer#closeSockets()
     */
    public boolean stopICAPServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": stopICAPServer:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": stopICAPServer: port");
            return false;
        }
        if(parameters.length==2)
        {
            try
            {
                XTTProperties.printInfo(parameters[0] + ": Stopping ICAPServer on port "+parameters[1]);
                ICAPServer.closeSocket(parameters[1]);
                return true;
            }catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        } else
        {
            XTTProperties.printWarn(parameters[0] + ": Stopping all ICAPServers");
            try
            {
                ICAPServer.closeSockets();
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
     * stops the ICAPServer and all it's threads.
     *
     * @see FunctionModule_ICAP#stopICAPServer(java.lang.String[])
     */
    public boolean stopSecureICAPServer(String parameters[])
    {
        return stopICAPServer(parameters);
    }

    /**
     * Overrides the normal ICAP Return code with a custom code and message.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> is an integer representing the http code (0 or less means no override).
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see ICAPWorker#setOverrideReturnCode(int,String)
     */
    public void setServerReturnCode(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setServerReturnCode: icapReturnCode");
            XTTProperties.printFail(this.getClass().getName()+": setServerReturnCode: icapReturnCode icapReturnMessage");
            return;
        }
        if(parameters.length<2||parameters.length>3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": icapReturnCode");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": icapReturnCode icapReturnMessage");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            String code=parameters[1];
            String message="";
            if(parameters.length>2)message=parameters[2];
            try
            {
                ICAPWorker.setOverrideReturnCode(Integer.parseInt(code),message);
                if(Integer.parseInt(code)>0)
                {
                    XTTProperties.printInfo(parameters[0] + ": setting OverrideReturnCode to "+code+" "+message);
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
     * Overrides the HTTP parts of the response, mainly headers and body. Headers have to be added as lines of text. The Body as base64 encoded content. 
     * The number of chunks the body is to be devided into can be specified. The last chunk will be at most numchunks-1 bytes bigger than the rest. 
     * overrideType can be either req or res, default is taken from the original ICAP http headers.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> the http header block,
     *                      <br><code>parameters[2]</code> the content of the response base64 encoded,
     *                      <br><code>parameters[3]</code> number of chunks for the body,
     *                      <br><code>parameters[4]</code> the override type either res or req.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see ICAPWorker#setOverrideReturnCode(int,String)
     */
    public void setServerOverrideResponse(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setServerOverrideResponse:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": setServerOverrideResponse: httpHeaderBlock");
            XTTProperties.printFail(this.getClass().getName()+": setServerOverrideResponse: httpHeaderBlock base64Content");
            XTTProperties.printFail(this.getClass().getName()+": setServerOverrideResponse: httpHeaderBlock base64Content numChunks");
            XTTProperties.printFail(this.getClass().getName()+": setServerOverrideResponse: httpHeaderBlock base64Content numChunks overrideType");
            return;
        }
        if(parameters.length<1||parameters.length>5)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+":"+NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": httpHeaderBlock");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": httpHeaderBlock base64Content");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": httpHeaderBlock base64Content numChunks");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": httpHeaderBlock base64Content numChunks overrideType");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            try
            {
                if(parameters.length==1)
                {
                    ICAPWorker.setOverrideHTTP(null,null,null);
                    return;
                }
                byte[] headers=null;
                Vector<ByteArrayWrapper> body=null;
                int numChunks=1;
                String bodyType=null;

                // Headers
                java.io.BufferedReader lines=new java.io.BufferedReader(new java.io.StringReader(parameters[1]));
                String line="";
                StringBuffer headersB=new StringBuffer();
                while((line=lines.readLine())!=null)
                {
                    headersB.append(line+"\r\n");
                }
                headersB.append("\r\n");
                headers=ConvertLib.createBytes(headersB.toString());
        
                // NumChunks
                if(parameters.length>3)numChunks=Integer.decode(parameters[3]);
                if(numChunks<=0)numChunks=1;
                // Body
                byte[] base64Content=new byte[0];
                if(parameters.length>2&&parameters[2].length()>0)
                {
                    base64Content=ConvertLib.base64Decode(parameters[2]);
                    if(numChunks>base64Content.length)numChunks=base64Content.length;
                    body=new Vector<ByteArrayWrapper>();
                    int chunkSize=base64Content.length/numChunks;
                    int end=0;
                    int start=0;
                    for(int i=0;i<numChunks;i++)
                    {
                        if(i==numChunks-1)
                        {
                            end=base64Content.length;
                        } else
                        {
                            end=start+chunkSize;
                        }
                        body.add(new ByteArrayWrapper(base64Content,start,end));
                        start=end;
                    }
                    //System.out.println(body);
                }

                
                // overrideType
                if(parameters.length>4)
                {
                    if(parameters[4].equalsIgnoreCase("res"))
                    {
                        bodyType="res";
                    } else if(parameters[4].equalsIgnoreCase("req"))
                    {
                        bodyType="req";
                    } else
                    {
                        XTTProperties.printFail(parameters[0] + ": invalid body type: "+parameters[4]+" (allowed res or req)");
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                        return;
                    }
                }

                XTTProperties.printInfo(parameters[0] + ": setting server override response");
                ICAPWorker.setOverrideHTTP(headers,body,bodyType);
            } catch (IllegalStateException nfe)
            {
                XTTProperties.printFail(parameters[0] + ": not allowed in full streaming mode");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } catch (Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": error overriding response");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
    }

    /**
     * Turn off/on full streaming mode.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> is an integer representing the http code (0 or less means no override).
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see ICAPWorker#setEnableFullStreaming(boolean)
     */
    public void disableFullStreaming(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": disableFullStreaming: booleanDoDisable");
            return;
        }
        if(parameters.length<2||parameters.length>3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": booleanDoDisable");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            String fullStreaming=parameters[1];
            if(ConvertLib.textToBoolean(fullStreaming))
            {
                ICAPWorker.setEnableFullStreaming(false);
                XTTProperties.printInfo(parameters[0] + ": full streaming turned off");
            } else
            {
                ICAPWorker.setEnableFullStreaming(true);
                XTTProperties.printInfo(parameters[0] + ": full streaming turned on");
            }
        }
    }
    public void enableKeepAlive(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": enableKeepAlive: defaultKeepAlive");
            return;
        }
        if(parameters.length<2||parameters.length>3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": defaultKeepAlive");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            String fullStreaming=parameters[1];
            if(ConvertLib.textToBoolean(fullStreaming))
            {
                ICAPWorker.setDefaultKeepAlive(true);
                XTTProperties.printInfo(parameters[0] + ": default Keep-Alive turned on");
            } else
            {
                ICAPWorker.setDefaultKeepAlive(false);
                XTTProperties.printInfo(parameters[0] + ": default Keep-Alive turned off");
            }
        }
    }

    public void setFlushLevel(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setFlushLevel: intFlushLevel");
            return;
        }
        if(parameters.length<2||parameters.length>3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": intFlushLevel");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            try
            {
                int flushLevel=Integer.decode(parameters[1]);
                ICAPWorker.setFlushLevel(flushLevel);
                XTTProperties.printInfo(parameters[0] + ": setting flush level to "+flushLevel);
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
    }


    /**
     * Remove all the ICAP headers that are to be sent from the server to the client.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name, no additional parameters are requeired.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see ICAPWorker#setServerSendICAPHeader
     */
    public void clearServerICAPHeader(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": clearServerICAPHeader:"+NO_ARGUMENTS);
            return;
        }
        if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            XTTProperties.printInfo(parameters[0] + ": clearing header");
            ICAPWorker.setServerSendICAPHeader(new LinkedHashMap<String,String>());
        }
    }
    /**
     * Remove all the HTTP Request headers that are to be sent from the server to the client.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name, no additional parameters are requeired.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see ICAPWorker#setServerSendREQHeader
     */
    public void clearServerREQHeader(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": clearServerREQHeader:"+NO_ARGUMENTS);
            return;
        }
        if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            XTTProperties.printInfo(parameters[0] + ": clearing header");
            ICAPWorker.setServerSendREQHeader(new LinkedHashMap<String,String>());
        }
    }
    /**
     * Remove all the HTTP Response headers that are to be sent from the server to the client.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name, no additional parameters are requeired.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see ICAPWorker#setServerSendRESPHeader
     */
    public void clearServerRESPHeader(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": clearServerRESPHeader:"+NO_ARGUMENTS);
            return;
        }
        if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            XTTProperties.printInfo(parameters[0] + ": clearing header");
            ICAPWorker.setServerSendRESPHeader(new LinkedHashMap<String,String>());
        }
    }
    /**
     * Remove all the ICAP Option headers that are to be sent from the server to the client.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name, no additional parameters are requeired.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see ICAPWorker#setServerSendOptions
     */
    public void clearServerOptionsHeader(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": clearServerOptionsHeader:"+NO_ARGUMENTS);
            return;
        }
        if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            XTTProperties.printInfo(parameters[0] + ": clearing header");
            ICAPWorker.setServerSendOptions(new LinkedHashMap<String,String>());
        }
    }

    /**
     * Set the ICAP headers to be sent from the server to the client.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the header key,
     *                      <br><code>parameters[2]</code> argument is the header value.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     *
     * @see ICAPWorker#setServerSendICAPHeader
     */
    public void setServerICAPHeader(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setServerICAPHeader: headerFieldKey headerFieldValue");
            return;
        }
        HTTPHelper.setHeader(ICAPWorker.getServerSendICAPHeader(),parameters);
    }
    /**
     * Set the HTTP Request headers to be sent from the server to the client.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the header key,
     *                      <br><code>parameters[2]</code> argument is the header value.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     *
     * @see ICAPWorker#getServerSendREQHeader
     */
    public void setServerREQHeader(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setServerREQHeader: headerFieldKey headerFieldValue");
            return;
        }
        HTTPHelper.setHeader(ICAPWorker.getServerSendREQHeader(),parameters);
    }
    /**
     * Set the HTTP Response headers to be sent from the server to the client.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the header key,
     *                      <br><code>parameters[2]</code> argument is the header value.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     *
     * @see ICAPWorker#getServerSendRESPHeader
     */
    public void setServerRESPHeader(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setServerRESPHeader: headerFieldKey headerFieldValue");
            return;
        }
        HTTPHelper.setHeader(ICAPWorker.getServerSendRESPHeader(),parameters);
    }
    /**
     * Set the ICAP Options headers to be sent from the server to the client.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the header key,
     *                      <br><code>parameters[2]</code> argument is the header value.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     *
     * @see ICAPWorker#getServerSendRESPHeader
     */
    public void setServerOptionsHeader(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setServerOptionsHeader: headerFieldKey headerFieldValue");
            return;
        }
        HTTPHelper.setHeader(ICAPWorker.getServerSendOptions(),parameters);
    }

    /**
     * Compare the ICAP headers received by the server from the client with a value which is required.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code> argument is the header key, <code>parameters[2]</code>
     *                     argument is the header value.
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void checkServerICAPHeader(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkServerICAPHeader: headerFieldKey expectedValue");
            return;
        }
        HTTPHelper.checkHeader(ICAPWorker.getServerICAPHeader(),parameters,false);
    }
    /**
     * Compare the HTTP Request headers received by the server from the client with a value which is required.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code> argument is the header key, <code>parameters[2]</code>
     *                     argument is the header value.
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void checkServerREQHeader(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkServerREQHeader: headerFieldKey expectedValue");
            return;
        }
        HTTPHelper.checkHeader(ICAPWorker.getServerREQHeader(),parameters,false);
    }
    /**
     * Compare the HTTP Response headers received by the server from the client with a value which is required.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code> argument is the header key, <code>parameters[2]</code>
     *                     argument is the header value.
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void checkServerRESPHeader(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkServerRESPHeader: headerFieldKey expectedValue");
            return;
        }
        HTTPHelper.checkHeader(ICAPWorker.getServerRESPHeader(),parameters,false);
    }
    /**
     * Compare the ICAP headers received by the server from the client with a value which it is prohibited.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the header key,
     *                      <br><code>parameters[2]</code> argument is the header value.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void checkServerICAPHeaderNot(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkServerICAPHeaderNot: headerFieldKey expectedValue");
            return;
        }
        HTTPHelper.checkHeader(ICAPWorker.getServerICAPHeader(),parameters,true);
    }
    /**
     * Compare the HTTP Request headers received by the server from the client with a value which it is prohibited.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the header key,
     *                      <br><code>parameters[2]</code> argument is the header value.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void checkServerREQHeaderNot(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkServerREQHeaderNot: headerFieldKey expectedValue");
            return;
        }
        HTTPHelper.checkHeader(ICAPWorker.getServerREQHeader(),parameters,true);
    }
    /**
     * Compare the HTTP Response headers received by the server from the client with a value which it is prohibited.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the header key,
     *                      <br><code>parameters[2]</code> argument is the header value.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void checkServerRESPHeaderNot(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkServerRESPHeaderNot: headerFieldKey expectedValue");
            return;
        }
        HTTPHelper.checkHeader(ICAPWorker.getServerRESPHeader(),parameters,true);
    }

    /**
     * Check an ICAP header received by the server from the client does contain a value.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the header key,
     *                      <br><code>parameters[2]</code> argument is the header value part.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void checkServerICAPHeaderPart(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkServerICAPHeaderPart: headerFieldKey expectedValuePart");
            return;
        }
        HTTPHelper.checkHeaderPart(ICAPWorker.getServerICAPHeader(),parameters,false);
    }
    /**
     * Check a HTTP Request header received by the server from the client does contain a value.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the header key,
     *                      <br><code>parameters[2]</code> argument is the header value part.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void checkServerREQHeaderPart(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkServerREQHeaderPart: headerFieldKey expectedValuePart");
            return;
        }
        HTTPHelper.checkHeaderPart(ICAPWorker.getServerREQHeader(),parameters,false);
    }
    /**
     * Check a HTTP Request header received by the server from the client does contain a value.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the header key,
     *                      <br><code>parameters[2]</code> argument is the header value part.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void checkServerRESPHeaderPart(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkServerRESPHeaderPart: headerFieldKey expectedValuePart");
            return;
        }
        HTTPHelper.checkHeaderPart(ICAPWorker.getServerRESPHeader(),parameters,false);
    }
    /**
     * Check an ICAP header received by the server from the client does NOT contain a value.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the header key,
     *                      <br><code>parameters[2]</code> argument is the header value part.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void checkServerICAPHeaderNotPart(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkServerICAPHeaderNotPart: headerFieldKey expectedValueNotPartOfHeader");
            return;
        }
        HTTPHelper.checkHeaderPart(ICAPWorker.getServerICAPHeader(),parameters,true);
    }
    /**
     * Check a HTTP Request header received by the server from the client does NOT contain a value.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the header key,
     *                      <br><code>parameters[2]</code> argument is the header value part.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void checkServerREQHeaderNotPart(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkServerREQHeaderNotPart: headerFieldKey expectedValueNotPartOfHeader");
            return;
        }
        HTTPHelper.checkHeaderPart(ICAPWorker.getServerREQHeader(),parameters,true);
    }
    /**
     * Check a HTTP Response header received by the server from the client does NOT contain a value.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the header key,
     *                      <br><code>parameters[2]</code> argument is the header value part.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void checkServerRESPHeaderNotPart(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkServerRESPHeaderNotPart: headerFieldKey expectedValueNotPartOfHeader");
            return;
        }
        HTTPHelper.checkHeaderPart(ICAPWorker.getServerRESPHeader(),parameters,true);
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
            if(surl.equals(ICAPWorker.getServerRecievedURL()))
            {
                XTTProperties.printInfo(parameters[0]+": found URL "+ICAPWorker.getServerRecievedURL());
            } else
            {
                XTTProperties.printFail(parameters[0]+": found URL "+ICAPWorker.getServerRecievedURL()+" wanted "+surl);
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
            return ConvertLib.queryString(parameters[0],ICAPWorker.getServerRecievedURL(),parameters[2],parameters[1]);
        }
    }

    /*
     * query if the body of a http response contains a specified regular expression value.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the filename of the file on the server relative to the ICAPServer root (CONFIGURATION/ICAPServer/ROOT),
     *                      <br><code>parameters[2]</code>
     *                     argument is the content of the file.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see ICAPWorker#setCacheFile(String,String,String)
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
        } else
        {
            ICAPWorker.setCacheFile(XTTProperties.getProperty("ICAPServer/ROOT"),parameters[1],parameters[2]);
        }
    }*/
    /*
     * query if the body of a http response contains a specified regular expression value.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the root directory of the ICAPServer used (like CONFIGURATION/ICAPServer/ROOT),
     *                     <br><code>parameters[2]</code> argument is the filename of the file on the server relative to the ICAPServer root,
     *                     <br><code>parameters[3]</code> argument is the content of the file.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see ICAPWorker#setCacheFile(String,String,String)
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
            ICAPWorker.setCacheFile(parameters[1],parameters[2],parameters[3]);
        }
    }*/
    /*
     *
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always
     *                     the method name, no additional parameters are requeired.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see ICAPWorker#clearCache()
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
            ICAPWorker.clearCache();
        }
    }*/

    /**
     * Wait for a specified number of ICAP requests on the ICAPServer.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the number of requests.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see ICAPWorker#waitForRequests(int)
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
                XTTProperties.printInfo(parameters[0] + ": waiting for "+messages+" HTTP-Requests received on ICAPServer");
                ICAPWorker.waitForRequests(messages);
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
     * Wait for a specified number of ICAP REQMOD requests on the ICAPServer.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the number of requests.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see ICAPWorker#waitForRequests(int)
     */
    public boolean waitForREQMOD(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForREQMOD: numREQMOD");
            return false;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": numREQMOD");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return false;
        } else
        {
            try
            {
                int messages=Integer.parseInt(parameters[1]);
                XTTProperties.printInfo(parameters[0] + ": waiting for "+messages+" HTTP-REQMOD received on ICAPServer");
                ICAPWorker.waitForREQMOD(messages);
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
     * Wait for a specified number of ICAP RESPMOD requests on the ICAPServer.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the number of requests.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see ICAPWorker#waitForRequests(int)
     */
    public boolean waitForRESPMOD(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForRESPMOD: numRESPMOD");
            return false;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": numRESPMOD");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return false;
        } else
        {
            try
            {
                int messages=Integer.parseInt(parameters[1]);
                XTTProperties.printInfo(parameters[0] + ": waiting for "+messages+" HTTP-RESPMOD received on ICAPServer");
                ICAPWorker.waitForRESPMOD(messages);
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
     * Wait for a specified number of ICAP OPTIONS requests on the ICAPServer.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the number of requests.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see ICAPWorker#waitForRequests(int)
     */
    public boolean waitForOPTIONS(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForOPTIONS: numOPTIONS");
            return false;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": numOPTIONS");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return false;
        } else
        {
            try
            {
                int messages=Integer.parseInt(parameters[1]);
                XTTProperties.printInfo(parameters[0] + ": waiting for "+messages+" HTTP-OPTIONS received on ICAPServer");
                ICAPWorker.waitForOPTIONS(messages);
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
     * Wait for a timeout and no ICAP requests on the ICAPServer.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the timeout in ms.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see ICAPWorker#waitForRequests(int)
     */
    public boolean waitForTimeoutRequests(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForTimeoutRequests: timeoutms");
            return false;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": timeoutms");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return false;
        } else
        {
            try
            {
                int timeoutms=Integer.parseInt(parameters[1]);
                XTTProperties.printInfo(parameters[0] + ": waiting for "+timeoutms+"ms and HTTP-Requests received on ICAPServer");
                ICAPWorker.waitForTimeoutRequests(timeoutms);
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
     * Wait for a timeout and no ICAP REQMOD requests on the ICAPServer.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the timeout in ms.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see ICAPWorker#waitForRequests(int)
     */
    public boolean waitForTimeoutREQMOD(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForTimeoutREQMOD: timeoutms");
            return false;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": timeoutms");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return false;
        } else
        {
            try
            {
                int timeoutms=Integer.parseInt(parameters[1]);
                XTTProperties.printInfo(parameters[0] + ": waiting for "+timeoutms+"ms and HTTP-REQMOD received on ICAPServer");
                ICAPWorker.waitForTimeoutREQMOD(timeoutms);
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
     * Wait for a timeout and no ICAP RESPMOD requests on the ICAPServer.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the timeout in ms.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see ICAPWorker#waitForRequests(int)
     */
    public boolean waitForTimeoutRESPMOD(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForTimeoutRESPMOD: timeoutms");
            return false;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": timeoutms");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return false;
        } else
        {
            try
            {
                int timeoutms=Integer.parseInt(parameters[1]);
                XTTProperties.printInfo(parameters[0] + ": waiting for "+timeoutms+"ms and HTTP-RESPMOD received on ICAPServer");
                ICAPWorker.waitForTimeoutRESPMOD(timeoutms);
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
     * Wait for a timeout and no ICAP OPTIONS requests on the ICAPServer.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the timeout in ms.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see ICAPWorker#waitForRequests(int)
     */
    public boolean waitForTimeoutOPTIONS(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForTimeoutOPTIONS: timeoutms");
            return false;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": timeoutms");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return false;
        } else
        {
            try
            {
                int timeoutms=Integer.parseInt(parameters[1]);
                XTTProperties.printInfo(parameters[0] + ": waiting for "+timeoutms+"ms and HTTP-OPTIONS received on ICAPServer");
                ICAPWorker.waitForTimeoutOPTIONS(timeoutms);
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
 
     public void lastWorkerIdToVariable(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setVariable: variableName");
            XTTProperties.printFail(this.getClass().getName()+": setVariable: variableName serverPort");
            return;
        }
        if(parameters.length<2||parameters.length>3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName serverPort");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            int port=0;
            if(parameters.length != 3)
            {
                    port=XTTProperties.getIntProperty("ICAPSERVER/PORT");
            }
            else
            {
                try
                {
                    port=Integer.parseInt(parameters[2]);
                } catch (NumberFormatException nfe)
                {
                    XTTProperties.printFail(parameters[0] + ": '"+parameters[2]+"' is NOT a number");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                }
            }
            int numworkers=ICAPServer.getLastWorkerId(port+"");
            XTTProperties.printInfo(parameters[0]+": stored '"+numworkers+"' to "+parameters[1]);
            XTTProperties.setVariable(parameters[1],""+numworkers);
        }
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
        int securePort = XTTProperties.getIntProperty("ICAPSERVER/SECUREPORT");
        int standardPort = XTTProperties.getIntProperty("ICAPSERVER/PORT");

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

    public static final String tantau_sccsid = "@(#)$Id: FunctionModule_ICAP.java,v 1.8 2009/06/05 12:38:19 rsoder Exp $";
}