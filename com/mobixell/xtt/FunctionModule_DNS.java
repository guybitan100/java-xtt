package com.mobixell.xtt;

import java.util.Vector;
import org.jdom.Element;

/**
 * FunctionModule_DNS.
 * <p/>
 * Returns an IP for A or AAAA requests, first checking the internal XTT DNS name store, then doing a lookup via the Java interface to the local machines DNS servers.
 * <br/>
 * <br/>Domain names can be resolved to any IP4 or IP6 address.
 *
 * <br/>To configure the entries the XTT configuration must contain a node like:
 * <br/><pre>&lt;DNSServer&gt;
 *     &lt;StartOnLoad/&gt;
 *     &lt;entry&gt;
 *         &lt;name&gt;xtt724.com&lt;/name&gt;
 *         &lt;!--ip&gt;192.168.1.1&lt;/ip--&gt;
 *     &lt;/entry&gt;
 *     &lt;entry&gt;
 *         ...
 *     &lt;/entry&gt;
 * &lt;/DNSServer&gt;</pre>
 * <br/>Additional domains are added by specifying new entry nodes (the ip address being optional, where SYSTEM/IP is taken from the configuration otherwise).
 *
 * @author      Gavin Cattell
 * @version     $Revision: 1.20 $
 */
public class FunctionModule_DNS extends FunctionModule
{
    private static DNSServer s = null;
    private String localIp;
    public void initialize()
    {
        String autostart=XTTProperties.getProperty("DNSSERVER/STARTONLOAD");
        if(autostart.equals("") || ConvertLib.textToBoolean(autostart))
        {
            if(DNSServer.checkSocket())
            {
                startDNSServer(new String[]{"startDNSServer"});
            }
        }
        else
        {
            if(!DNSServer.checkSocket())
            {
                stopDNSServer(new String[]{"stopDNSServer"});
            }
        }

        Element[] entries = null;
        entries = XTTXML.getElements("dnsserver/nodebugdomains/name");
        DNSServer.resetExcludedDomains();
        if((entries != null)&&(entries.length != 0))
        {
            String domain=null;
            for (int i=0;i<entries.length;i++)
            {
                try
                {
                    domain = entries[i].getText();
                    DNSServer.addExcludedDomain(domain);
                } catch(NullPointerException npe)
                {
                }
            }
        }
        XTTProperties.printDebug(this.getClass().getName()+".initialize(): clearing variables");
    }

    public FunctionModule_DNS()
    {
    }

    /**
     * Adds an entry to the internal name table.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> is the domain name.
     *                     <br><code>parameters[2]</code> [optional] is the IP to resolve to, if not specified SYSTEM/IP is taken from the configuration.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     */
    public int addEntry(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if((parameters==null) || (parameters.length<2))
        {
            XTTProperties.printFail(this.getClass().getName()+": addEntry: Domain [IP]");
            return XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
        }
        else if (s != null)
        {
            String ip=null;
            String domainName=null;
            if(parameters.length < 3)
            {
                ip=XTTProperties.getProperty("system/ip");
            }
            else
            {
                ip=parameters[2];
            }

            domainName = parameters[1];

            DNSServer.addEntry(domainName,ip);
        }
        else
        {
            XTTProperties.printFail(parameters[0]+": addEntry: You must have a running DNS Server before you can add an entry");
            status = XTTProperties.FAILED;
            XTTProperties.setTestStatus(status);
        }
        return status;
    }

    /**
     * Remove an entry from the internal name table.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> is the domain name to remove.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     */
    public int removeEntry(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if((parameters==null) || (parameters.length<2))
        {
            XTTProperties.printFail(this.getClass().getName()+": removeEntry: Domain");
            return XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
        }
        else if (s != null)
        {
            String domainName=null;

            domainName = parameters[1];

            DNSServer.removeEntry(domainName);
        }
        else
        {
            XTTProperties.printFail(parameters[0]+": removeEntry: You must have a running DNS Server before you can remove an entry");
            status = XTTProperties.FAILED;
            XTTProperties.setTestStatus(status);
        }
        return status;
    }

    /**
     * Starts a DNS Server.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br>No arguments are possible for this function.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     */
    public int startDNSServer(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
        	XTTProperties.printFail(this.getClass().getName()+": startDNSServer: no arguments required");
        	 return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        else if(parameters.length<2) 
        {
        	localIp=XTTProperties.getProperty("dnsserver/dnsIp");
        }
            Element[] entries = null;
            entries = XTTXML.getElements("dnsserver/entry");
            
            if((entries == null) || (entries.length == 0))
            {
                XTTProperties.printWarn(parameters[0]+": startDNSServer: no entries found");
            }
            else
            {
                String name = "";
                String ip = "";
                for (int i=0;i<entries.length;i++)
                {
                    try
                    {
                        name = XTTXML.getChild("name",entries[i]).getText().toLowerCase();
                    }
                    catch(NullPointerException npe)
                    {
                        XTTProperties.printFail(parameters[0] + ": Missing DNS name node, skipping");
                        continue;
                    }

                    try
                    {
                        ip = XTTXML.getChild("ip",entries[i]).getText();
                    }
                    catch(NullPointerException npe)
                    {
                        ip = XTTProperties.getProperty("SYSTEM/IP");
                    }

                    try
                    {
                        DNSEntry.setIpAddress(name,ip);
                    }
                    catch(java.net.UnknownHostException uhe)
                    {
                        XTTProperties.printFail(parameters[0] + ": " + ip + " isn't an IP, skipping");
                        continue;
                    }

                    Vector<Element> naptrs = XTTXML.getChildren("naptr",entries[i]);
                    for (Element naptr : naptrs)
                    {
                        int order = -1;
                        int preference = -1;
                        String flags = null;
                        String service = null;
                        String regexp = null;
                        String replacement = null;

                        try
                        {
                            order = Integer.parseInt(XTTXML.getChild("order",naptr).getText().toLowerCase());
                        }
                        catch(NullPointerException npe){} catch(NumberFormatException nfe){}
                        try
                        {
                            preference = Integer.parseInt(XTTXML.getChild("preference",naptr).getText().toLowerCase());
                        }
                        catch(NullPointerException npe){} catch(NumberFormatException nfe){}
                        try
                        {
                            flags = XTTXML.getChild("flags",naptr).getText().toLowerCase();
                        }
                        catch(NullPointerException npe){}
                        try
                        {
                            service = XTTXML.getChild("service",naptr).getText().toLowerCase();
                        }
                        catch(NullPointerException npe){}
                        try
                        {
                            regexp = XTTXML.getChild("regexp",naptr).getText().toLowerCase();
                        }
                        catch(NullPointerException npe){}
                        try
                        {
                            replacement = XTTXML.getChild("replacement",naptr).getText().toLowerCase();
                        }
                        catch(NullPointerException npe){}

                        if(service!=null && !service.equals(""))
                        {
                            DNSEntry.setNaptr(name, order, preference, flags, service, regexp, replacement);
                        }
                        else
                        {
                            XTTProperties.printFail(parameters[0] + ": You need to specify a service for this NAPTR entry, skipping");
                        }
                    }
                }
            }
            try
            {
                s = new DNSServer(localIp);
                s.start();
                XTTProperties.printDebug(parameters[0] + ": Started DNS Server");
            }
            catch (Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
            return status;
    }

    /**
     * Stops a DNS Server.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br>No arguments are possible for this function.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     */
    public int stopDNSServer(String parameters[])
    {
    	int status = XTTProperties.PASSED;
    	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": stopDNSServer: no arguments required");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        try
        {
            XTTProperties.printVerbose(parameters[0] + ": Stopping DNS Server");
            s.stopGracefully();
            return status;
        }
        catch(Exception e)
        {
            XTTProperties.printFail(parameters[0] + ": ERROR");
            XTTProperties.printException(e);
            status = XTTProperties.FAILED;
            XTTProperties.setTestStatus(status);
        }
        return status;
    }

    public String checkResources()
    {
        int port = 53;
        String resourceString = "" + this.getClass().getName() + ":"+RESOURCE_OK;
        try
        {
            String startonload=XTTProperties.getProperty("DNSSERVER/STARTONLOAD");
            if(startonload==null||startonload.equals("null")||s==null||DNSServer.checkSocket())
            {
                java.net.DatagramSocket s=new java.net.DatagramSocket(port);
                s.close();
            }
        } catch(Exception e)
        {
            resourceString = "" + this.getClass().getName() + ":"+RESOURCE_PORT+" '" +  port+"'";
        }
        return resourceString;
    }

    /**
     * Set a delay for the DNS before sending the response. Set it to 0 or less for no delay.
     * 
     * @param parameters   array of String containing the parameters. 
     *                      <code>parameters[0]</code> argument is always the method name, 
     *                      <code>parameters[1]</code> argument is delay time in miliseconds, 
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see MMSDecoder
     */
    public int setResponseDelay(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setResponseDelay: delayinms");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": delayinms");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            try
            {
                int delay=Integer.decode(parameters[1]);
                DNSWorker.setResponseDelay(delay);
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR: '"+parameters[1]+"' is not a number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
            }            
        }
        return status;
    }
    
    public String getConfigurationOptions()
    {
        return "    <!-- function module DNS -->"
        +"\n    <DNSServer>"
        +"\n        <StartOnLoad>true</StartOnLoad>"
        +"\n        <entry>"
        +"\n            <name>xtt724.com</name>"
        +"\n            <!-- If the IP is missing system/ip is used -->"
        +"\n            <!--ip>192.168.1.1</ip-->"
        +"\n            <!--naptr>"
        +"\n                <order>10</order>"
        +"\n                <preference>10</preference>"
        +"\n                <flags>u</flags>"
        +"\n                <service>imsi+E2U</service>"
        +"\n                <regexp>!^.*$!sip:info@tele2.se!</regexp>"
        +"\n                <replacement>.</replacement>"
        +"\n            </naptr-->"
        +"\n        </entry>"
        +"\n        <!--nodebugdomains>"
        +"\n          <name>yahoo.com</name>"
        +"\n        </nodebugdomains-->"
        +"\n    </DNSServer>";
    }

    /**
     * returns the getClass().getName() of this object. For debug reasons only.
     */
    public String toString()
    {
        return this.getClass().getName();
    }
    public static final String tantau_sccsid = "@(#)$Id: FunctionModule_DNS.java,v 1.20 2009/10/22 13:08:59 rsoder Exp $";
}