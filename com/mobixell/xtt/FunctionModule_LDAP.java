package com.mobixell.xtt;

import org.jdom.Element;
import org.jdom.Document;

import com.mobixell.xtt.ldap.LDAPServer;
import com.mobixell.xtt.ldap.LDAPWorker;
/**
 * FunctionModule_STI provides HTTP and HTTPS GET functions.
 *
 * @author      Roger Soder
 * @version     $Id: FunctionModule_LDAP.java,v 1.9 2010/01/15 10:22:34 rsoder Exp $
 */
public class FunctionModule_LDAP extends FunctionModule
{
    private LDAPServer s                        = null;
    private Thread ws                           = null;

    /**
     * clears and reinitializes all the variables.
     */
    public void initialize()
    {
        LDAPWorker.initialize();
        LDAPServer.resetWorkerId();
        XTTProperties.printDebug(this.getClass().getName()+".initialize(): clearing variables");
    }

    /**
     * constructor.
     */
    public FunctionModule_LDAP()
    {
    }

    /**
     * Returns the Configuration Options as a String ready to copy/paste in a configuration file
     *
     */
    public String getConfigurationOptions()
    {
        return "    <!-- function module LDAP -->"
            +"\n    <LDAPServer>"
            +"\n        <!-- the listening port of the internal ldapserver -->"
            +"\n        <Port>389</Port>"
            +"\n        <!-- the listening port of the internal secure-ldapserver -->"
            +"\n        <SecurePort>3389</SecurePort>"
            +"\n        <!-- timeout on client connections to the ldapserver -->"
            +"\n        <Timeout>30000</Timeout>"
            +"\n        <!-- time to wait on a \"wait\" function before continuing -->"
            +"\n        <waitTimeout>32000</waitTimeout>"
            +"\n        <!-- This can instead be specified in an external config file"
            +"\n        <LDAP>"
            +"\n            <logins>"
            +"\n                <login>"
            +"\n                    <name>cn=Manager,o=724.com</name>"
            +"\n                    <password>secret</password>"
            +"\n                </login>"
            +"\n                <login>"
            +"\n                    <name>null</name>"
            +"\n                    <password>null</password>"
            +"\n                </login>"
            +"\n            </logins>"
            +"\n            <directory>"
            +"\n                <entry>"
            +"\n                    <baseDN>cn=sis, o=724.com</baseDN>"
            +"\n                    <data>"
            +"\n                        <filter>login-ip=127.0.0.1</filter>"
            +"\n                        <dn>login-ip=127.0.0.1,cn=sis,o=724.com</dn>"
            +"\n                        <attribute>"
            +"\n                            <name>clid</name>"
            +"\n                            <type>0x04</type>"
            +"\n                            <value>4179442847</value>"
            +"\n                        </attribute>"
            +"\n                        <oid>2.16.840.1.113730.3.4.2</oid>"
            +"\n                    </data>"
            +"\n                    <data>"
            +"\n                        <filter>msisdn=4179442847</filter>"
            +"\n                        <dn>msisdn=4179442847,cn=sis,o=724.com</dn>"
            +"\n                        <attribute>"
            +"\n                            <name>msisdn</name>"
            +"\n                            <type>0x04</type>"
            +"\n                            <value>4179442847</value>"
            +"\n                        </attribute>"
            +"\n                        <attribute>"
            +"\n                            <name>mccmnc</name>"
            +"\n                            <type>0x04</type>"
            +"\n                            <value>228-001</value>"
            +"\n                        </attribute>"
            +"\n                        <oid>2.16.840.1.113730.3.4.2</oid>"
            +"\n                    </data>"
            +"\n                </entry>"
            +"\n            </directory>"
            +"\n        </LDAP>"
            +"\n        -->"
            +"\n    </LDAPServer>";
    }

    /**
     * Overriden from superclass to add the LDAPServer and LDAPWorker version numbers.
     *
     * @see LDAPServer
     * @see LDAPWorker
     */
    public void printVersion()
    {
        super.printVersion();
        XTTProperties.printDebug(this.getClass().getName()+": LDAPServer: "+parseVersion(LDAPServer.tantau_sccsid));
        XTTProperties.printDebug(this.getClass().getName()+": LDAPWorker: "+parseVersion(LDAPWorker.tantau_sccsid));
    }
    public void showVersions()
    {
        super.showVersions();
        System.out.println(ConvertLib.createString(this.getClass().getName()+": LDAPServer: ",SHOWLENGTH) + parseVersion(LDAPServer.tantau_sccsid));
        System.out.println(ConvertLib.createString(this.getClass().getName()+": LDAPWorker: ",SHOWLENGTH) + parseVersion(LDAPWorker.tantau_sccsid));
    }

   /**
     * starts the LDAPServer as an instance of LDAPServer.
     *
     * @param parameters   array of String containing the parameters. 
     *                      <br><code>parameters[0]</code> argument is always the method name, 
     *                      <br><code>parameters[1]</code> argument is the LDAPServer port, 
     *                      <br><code>parameters[2]</code> is the LDAPServer root directory,
     *                      <br><code>parameters[3]</code> argument is the LDAPServer timeout. 
     *                      <br>If only <code>parameters[0]</code> is submitted
     *                     the parameters will be taken from the configuration xml document in XTTProperties.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see LDAPServer
     * @see XTTProperties
     */
    public boolean startLDAPServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": startLDAPServer:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": startLDAPServer: ldapConfiguration");
            XTTProperties.printFail(this.getClass().getName()+": startLDAPServer: ldapConfiguration port timeOut");
            return false;
        }
        return startLDAPServer(parameters,false);
    }

    /**
     * starts the LDAPServer as an instance of LDAPServer with SSL-Encryption.
     *
     * @param parameters   array of String containing the parameters. 
     *                      <br><code>parameters[0]</code> argument is always the method name, 
     *                      <br><code>parameters[1]</code> argument is the LDAPServer port, 
     *                      <br><code>parameters[2]</code> is the LDAPServer configuration,
     *                      <br><code>parameters[3]</code> argument is the LDAPServer timeout. 
     *                      <br>If only <code>parameters[0]</code> is submitted the parameters will be taken from the configuration xml document in XTTProperties.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see LDAPServer
     * @see XTTProperties
     */
    public boolean startSecureLDAPServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": startSecureLDAPServer:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": startSecureLDAPServer: ldapConfiguration");
            XTTProperties.printFail(this.getClass().getName()+": startSecureLDAPServer: ldapConfiguration port timeOut");
            return false;
        }
        return startLDAPServer(parameters,true);
    }

    public boolean setConfiguration(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setConfiguration: ldapConfiguration");
            XTTProperties.printFail(this.getClass().getName()+": setConfiguration: ldapConfiguration port");
            return false;
        }
        if(parameters.length<2&&parameters.length>3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": ldapConfiguration");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": ldapConfiguration port");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return false;
        } else
        {
            try
            {
                Document ldapConfig=null;
                if(parameters[1]!=null)
                {
                    java.io.StringReader reader=new java.io.StringReader(parameters[1]);
                    org.jdom.input.SAXBuilder parser = new org.jdom.input.SAXBuilder();
                    ldapConfig = parser.build(reader);
                }
                int port=-1;
                if(parameters.length==2)
                {
                    XTTProperties.printInfo(parameters[0] + ": setting configuration on all LDAPServers");
                    LDAPServer.setConfiguration(ldapConfig);
                } else
                {
                    try
                    {
                        port=Integer.parseInt(parameters[2]);
                    } catch(NumberFormatException nfe)
                    {
                        XTTProperties.printFail(parameters[0] + ": parameter '"+parameters[2]+"' is not a number");
                        XTTProperties.printException(nfe);
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                        return false;
                    }
                    XTTProperties.printInfo(parameters[0] + ": setting configuration on port='"+port+"' LDAPServer");
                    LDAPServer.setConfiguration(""+port,ldapConfig);
                }
                return true;
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
        return false;
    }

    private boolean startLDAPServer(String parameters[],boolean secure)
    {
        String securetext="non-secure";
        if(secure)securetext="secure";
        String porttype="";
        if(secure)porttype="SECURE";
        if(parameters.length==4||parameters.length==2)
        {
            try
            {
                XTTProperties.printVerbose(parameters[0] + ": Starting "+securetext+" LDAPServer");

                int port=-1;
                int timeout=-1;

                if(parameters.length==2)
                {
                    timeout=XTTProperties.getIntProperty("LDAPSERVER/TIMEOUT");
                    port=XTTProperties.getIntProperty("LDAPSERVER/"+porttype+"PORT");
                } else
                {
                    port=Integer.parseInt(parameters[2]);
                    timeout=Integer.parseInt(parameters[3]);
                }
                
                Document ldapConfig=null;
                if(parameters[1]!=null)
                {
                    java.io.StringReader reader=new java.io.StringReader(parameters[1]);
                    org.jdom.input.SAXBuilder parser = new org.jdom.input.SAXBuilder();
                    ldapConfig = parser.build(reader);
                }
              //Guy only one webserver can we run with the same ip/port
				if (s != null) 
				{
					if (s.getPort() == port)
					{
						if (s.isAlive()) 
						{
							s.stopGracefully();
						}
					}
				}
                s = new LDAPServer(port, timeout,secure, ldapConfig);
                ws=(new Thread(s, "LDAPServer"));
                ws.start();
                XTTProperties.printDebug(parameters[0] + ": Started "+securetext+" LDAPServer");
                return true;
            } catch(NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0] + ": one parameter is not a number");
                XTTProperties.printException(nfe);
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        } else if(parameters.length == 1)
        {
            try
            {
                XTTProperties.printVerbose(parameters[0] + ": Starting "+securetext+" LDAPServer");
                
                Element ldapElement=XTTXML.getElement("LDAPSERVER/LDAP");

                if(ldapElement==null)
                {
                    XTTProperties.printFail(parameters[0]+": ldap configuration missing");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                    return false;
                }

                ldapElement=(Element)ldapElement.clone();
                ldapElement.detach();
                Document ldapConfig=new Document(ldapElement);
                
                XTTProperties.getProperty("LDAPSERVER/ROOT");
                //Guy only one webserver can we run with the same ip/port
				if (s != null) 
				{
					if (s.getPort() == XTTProperties.getIntProperty("LDAPSERVER/"+porttype+"PORT"))
					{
						if (s.isAlive()) 
						{
							s.stopGracefully();
						}
					}
				}
                s = new LDAPServer(XTTProperties.getIntProperty("LDAPSERVER/"+porttype+"PORT"),XTTProperties.getIntProperty("LDAPSERVER/TIMEOUT"),secure,ldapConfig);
                ws=(new Thread(s, "LDAPServer"));
                ws.start();
                XTTProperties.printDebug(parameters[0] + ": Started "+securetext+" LDAPServer");
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
        } else
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": ldapConfiguration");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": ldapConfiguration port timeOut");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        }
        return false;

    }


    /**
     * stops all/one LDAPServers and all it's threads.
     *
     * @param parameters   array of String containing the parameters. 
     *                      <br><code>parameters[0]</code> argument is always the method name, 
     *                      no additional parameters are requeired, the optional <code>parameters[1]</code> argument is the LDAPServer port
     *                     of the LDAPServer to stop, if omitted all running servers are stopped.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see LDAPServer#closeSocket(String)
     * @see LDAPServer#closeSockets()
     */
    public boolean stopLDAPServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": stopLDAPServer:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": stopLDAPServer: port");
            return false;
        }
        if(parameters.length==2)
        {
            try
            {
                XTTProperties.printInfo(parameters[0] + ": Stopping LDAPServer on port "+parameters[1]);
                LDAPServer.closeSocket(parameters[1]);
                return true;
            }catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        } else
        {
            XTTProperties.printWarn(parameters[0] + ": Stopping all LDAPServers");
            try
            {
                LDAPServer.closeSockets();
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
     * stops the LDAPServer and all it's threads.
     *
     * @see FunctionModule_LDAP#stopLDAPServer(java.lang.String[])
     */
    public boolean stopSecureLDAPServer(String parameters[])
    {
        return stopLDAPServer(parameters);
    }

    /**
     * wait for a specified number of HTTP requests on the LDAPServer.
     *
     * @param parameters   array of String containing the parameters. 
     *                      <br><code>parameters[0]</code> argument is always the method name, 
     *                      <br><code>parameters[1]</code> argument is the number of requests.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see LDAPWorker#waitForRequests(int)
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
                XTTProperties.printInfo(parameters[0] + ": waiting for "+messages+" LDAP-Requests received on LDAPServer");
                LDAPWorker.waitForRequests(messages);
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
     * wait for a specified number of HTTP requests on the LDAPServer.
     *
     * @param parameters   array of String containing the parameters. 
     *                      <br><code>parameters[0]</code> argument is always the method name, 
     *                      <br><code>parameters[1]</code> argument is the delay in ms.
     *                      <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see LDAPWorker#waitForRequests(int)
     */
    public boolean delayServerSearchResponse(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": delayServerSearchResponse: delay (ms) < 2900");
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
                XTTProperties.printInfo(parameters[0] + ": delay("+messages+") for LDAP server response");
                LDAPWorker.delaySearchResponse(messages);
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

    public boolean waitForBind(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForBind: "+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": waitForBind: name");
            return false;
        }
        if(parameters.length>2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return false;
        } else
        {
            try
            {
                if(parameters.length==2)
                {
                    XTTProperties.printInfo(parameters[0] + ": waiting for bind with name='"+parameters[1]+"' on LDAPServer");
                    LDAPWorker.waitForBind(parameters[1]);
                } else
                {
                    XTTProperties.printInfo(parameters[0] + ": waiting for bind on LDAPServer");
                    LDAPWorker.waitForBind();
                }
                return true;
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
        return false;
    }

    public boolean waitForUnBind(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForUnBind: "+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": waitForUnBind: numUnbinds");
            return false;
        }
        if(parameters.length>2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return false;
        } else
        {
            try
            {
                if(parameters.length==2)
                {
                    XTTProperties.printInfo(parameters[0] + ": waiting for "+parameters[1]+" unbinds on LDAPServer");
                    LDAPWorker.waitForUnBind(Integer.decode(parameters[1]));
                } else
                {
                    XTTProperties.printInfo(parameters[0] + ": waiting for unbind on LDAPServer");
                    LDAPWorker.waitForUnBind();
                }
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

    public boolean waitForSearchRequests(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForSearchRequest: numRequests");
            XTTProperties.printFail(this.getClass().getName()+": waitForSearchRequest: numRequests searchFilter");
            return false;
        }
        if(parameters.length>3||parameters.length<2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return false;
        } else
        {
            try
            {
                int requests=Integer.parseInt(parameters[1]);
                if(parameters.length>2)
                {
                    XTTProperties.printInfo(parameters[0] + ": waiting for "+parameters[1]+" search request with filter='"+parameters[2]+"' on LDAPServer");
                    LDAPWorker.waitForSearchRequests(requests,parameters[2]);
                } else
                {
                    XTTProperties.printInfo(parameters[0] + ": waiting for "+parameters[1]+" search request on LDAPServer");
                    LDAPWorker.waitForSearchRequests(requests);
                }
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
     * wait for a timeout and no HTTP requests on the LDAPServer.
     *
     * @param parameters   array of String containing the parameters. 
     *                      <br><code>parameters[0]</code> argument is always the method name, 
     *                      <br><code>parameters[1]</code> argument is the timeout in ms.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see LDAPWorker#waitForRequests(int)
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
                XTTProperties.printInfo(parameters[0] + ": waiting for "+timeoutms+"ms and LDAP-Requests received on LDAPServer");
                LDAPWorker.waitForTimeoutRequests(timeoutms,-1);
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
        int securePort = XTTProperties.getIntProperty("LDAPSERVER/SECUREPORT");
        int standardPort = XTTProperties.getIntProperty("LDAPSERVER/PORT");

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

    public static final String tantau_sccsid = "@(#)$Id: FunctionModule_LDAP.java,v 1.9 2010/01/15 10:22:34 rsoder Exp $";
}