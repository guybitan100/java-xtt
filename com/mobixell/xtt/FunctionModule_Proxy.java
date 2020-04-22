package com.mobixell.xtt;

import java.util.HashMap;

/**
 * FunctionModule_Proxy provides Simple HTTP Proxy.
 *
 * @author      Roger Soder
 * @version     $Id: FunctionModule_Proxy.java,v 1.14 2010/03/18 05:30:39 rajesh Exp $
 */
public class FunctionModule_Proxy extends FunctionModule
{
    private HashMap<String,ProxyServer> proxyMap=new HashMap<String,ProxyServer>();

    /**
     * clears and reinitializes all the variables. Does reset the Proxy.
     */
    public void initialize()
    {
        proxyMap=new HashMap<String,ProxyServer>();
        ProxyWorker.init();
        ProxyServer.resetWorkerId();    }
    /**
     * Returns the Configuration Options as a String ready to copy/paste in a configuration file
     *
     */
    public String getConfigurationOptions()
    {
        return "    <!-- function modules PROXY -->"
        +"\n    <HttpProxy>"
        +"\n        <default>"
        +"\n            <!-- the listening port of the default proxyserver -->"
        +"\n            <Port>8088</Port>"
        +"\n            <!-- the name the default proxyserver will append to the Via header -->"
        +"\n            <Name>proxy1</Name>"
        +"\n            <!-- default timeout on client connections to the proxyserver -->"
        +"\n            <Timeout>10000</Timeout>"
        +"\n        </default>"
        +"\n    </HttpProxy>";
    }
    
    /**
     * Overriden from superclass to add the SIPServer and SIPWorker version numbers.
     * 
     * @see ProxyServer
     * @see ProxyWorker  
     */
    public void printVersion()
    {
        super.printVersion();
        XTTProperties.printDebug(this.getClass().getName()+": ProxyServer: "+parseVersion(ProxyServer.tantau_sccsid));
        XTTProperties.printDebug(this.getClass().getName()+": ProxyWorker: "+parseVersion(ProxyWorker.tantau_sccsid));
    }
    public void showVersions()
    {
        super.showVersions();
        System.out.println(ConvertLib.createString(this.getClass().getName()+": ProxyServer: ",SHOWLENGTH) + parseVersion(ProxyServer.tantau_sccsid));
        System.out.println(ConvertLib.createString(this.getClass().getName()+": ProxyWorker: ",SHOWLENGTH) + parseVersion(ProxyWorker.tantau_sccsid));
    }

    /**
     * starts a new proxy server.
     *
     * @param parameters   array of String containing the parameters. 
     *                     <br><code>parameters[0]</code> argument is always the method name, 
     *                     <br><code>parameters[1]</code> argument is the Proxy ip.
     *                     <br><code>parameters[2]</code> argument is the Proxy listening port, 
     *                     <br><code>parameters[3]</code> is the proxy name to append to the via header, 
     *                     <br><code>parameters[4]</code> argument is the proxy timeout.
     *                     <br>If only <code>parameters[0]</code> is submitted
     *                     the parameters will be taken from the configuration xml document in XTTProperties.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int startProxyServer(String parameters[])
    {
    	int status = XTTProperties.PASSED;
    	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": startProxyServer:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": startProxyServer: ip,port,name,timeOut");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        int port=0;
        int timeout=0;
        String name="";
        String ip="";
        if(parameters.length==1)
        {
            port=XTTProperties.getIntProperty("HTTPPROXY/DEFAULT/PORT");
            name=XTTProperties.getProperty("HTTPPROXY/DEFAULT/NAME");
            timeout=XTTProperties.getIntProperty("HTTPPROXY/DEFAULT/TIMEOUT");
        } else if (parameters.length>=4)
        {
        	ip=parameters[1];  
            name=parameters[3];
            try
            {
                port=Integer.parseInt(parameters[2]);
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": '"+parameters[2]+"' is NOT a correct number");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
            }            
            if (parameters.length==5)
            {
            	 try
                 {
            		 timeout=Integer.parseInt(parameters[4]);
                 } 
            	 catch (NumberFormatException nfe)
                 {
                     XTTProperties.printFail(parameters[0]+": '"+parameters[4]+"' is NOT a correct number");
                     status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                     XTTProperties.setTestStatus(status);
                 }
            }
        } else
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": port name timeOut");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
        }
        try
        {
            XTTProperties.printInfo(parameters[0] + ": Starting Proxy Server: "+port+","+name+","+timeout);
            ProxyServer ps = new ProxyServer(ip,port,name,timeout);
            ps.start();
            proxyMap.put(name.toLowerCase(),ps);
            XTTProperties.printDebug(parameters[0] + ": Started Proxy Server");
            return status;
        } catch(Exception e)
        {
            XTTProperties.printFail(parameters[0]+": "+e.getClass().getName()+": "+e.getMessage());
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
            status=XTTProperties.FAILED;
            XTTProperties.setTestStatus(status);
        }
        return status;
    }

    /**
     * stops an instance of the proxy server..
     *
     * @param parameters   array of String containing the parameters. 
     *                      <br><code>parameters[0]</code> argument is always the method name, 
     *                      <br><code>parameters[1]</code> is the proxy name.
     *                     <br>If only <code>parameters[0]</code> is submitted
     *                     the parameter will be taken from the configuration xml document in XTTProperties.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int stopProxyServer(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": stopProxyServer:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": stopProxyServer: name");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        String name="";
        if(parameters.length !=2)
        {
            XTTProperties.printWarn(parameters[0] + ": Using default settings");
            name=XTTProperties.getProperty("HTTPPROXY/DEFAULT/NAME");
        }
        else
        {
            name=parameters[1];
        }
        try
        {
            XTTProperties.printVerbose(parameters[0] + ": Stopping Proxy Server "+name);
            ProxyServer ps=proxyMap.get(name.toLowerCase());
            ps.stopGracefully();
            //XTTProperties.printDebug(parameters[0] + ": Stopped Proxy Server");
            return status;
        }
        catch(Exception e)
        {
            XTTProperties.printFail(parameters[0] + " " + e.getClass().getName());
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
            status=XTTProperties.FAILED;
            XTTProperties.setTestStatus(status);
        }
        return status;

    }


    /**
     * For Debug reasons here.
     *
     * @return      String containing the name of the current FunctionModule
     */
    public String toString()
    {
        return this.getClass().getName();
    }

    /**
     * Called for self test purposes to see if this FunctionModules resources are available.
     *
     */
    public String checkResources()
    {
        int standardPort = XTTProperties.getIntProperty("HTTPPROXY/DEFAULT/PORT");

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
        
        if(resourceString == null)
        {
            resourceString = "" + this.getClass().getName() + ":"+RESOURCE_OK;
        }
       
        return resourceString;
    }

    public static final String tantau_sccsid = "@(#)$Id: FunctionModule_Proxy.java,v 1.14 2010/03/18 05:30:39 rajesh Exp $";
}
