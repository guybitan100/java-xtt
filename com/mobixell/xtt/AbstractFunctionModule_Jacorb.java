package com.mobixell.xtt;


/**
 * AbstractFunctionModule_Jacorb.
 *
 * @author      Roger Soder
 * @version     $Revision: 1.5 $
 */
public abstract class AbstractFunctionModule_Jacorb extends FunctionModule
{
    public static final String tantau_sccsid = "@(#)$Id: AbstractFunctionModule_Jacorb.java,v 1.5 2008/03/17 08:48:11 rsoder Exp $";

    private Object nameKey=new Object();

    private static Object shutdownKey=new Object();
    private boolean properShutdown=false;

    private String[] args=new String[]{};

    private int namingTimeout=0;

    public void initialize()
    {
        // Always this way
        System.getProperties().setProperty("org.omg.CORBA.ORBClass","org.jacorb.orb.ORB");
        System.getProperties().setProperty("org.omg.CORBA.ORBSingletonClass","org.jacorb.orb.ORBSingleton");


        // Depending on configuration
        String jacorb_naming_ior_filename=XTTProperties.getProperty("JACORB/NAMING/IORFILENAME");
        if(jacorb_naming_ior_filename.equals("null"))
        {
            System.getProperties().setProperty("jacorb.naming.ior_filename","./NS_Ref");
        } else
        {
            System.getProperties().setProperty("jacorb.naming.ior_filename",jacorb_naming_ior_filename);
        }

        String jacorb_naming_time_out=XTTProperties.getProperty("JACORB/NAMING/TIMEOUT");
        if(jacorb_naming_time_out.equals("null"))
        {
            System.getProperties().setProperty("jacorb.naming.time_out","0");
        } else
        {
            System.getProperties().setProperty("jacorb.naming.time_out",jacorb_naming_time_out);
        }

        String jacorb_naming_port=XTTProperties.getProperty("JACORB/NAMING/PORT");
        if(jacorb_naming_port.equals("null"))
        {
            args=new String[]{};
        } else
        {
            args=new String[]{"-DOAPort="+jacorb_naming_port};
        }

        String jacorb_properties=XTTProperties.getProperty("JACORB/PROPERTIES");
        if(jacorb_properties.equals("null"))
        {
            System.getProperties().setProperty("custom.props","./jacorb.properties");
        } else
        {
            System.getProperties().setProperty("custom.props",jacorb_properties);
        }

        String java_endorsed_dirs=XTTProperties.getProperty("JACORB/JAVAENDORSEDDIRS");
        if(java_endorsed_dirs.equals("null"))
        {
            System.getProperties().setProperty("java.endorsed.dirs","./lib");
        } else
        {
            System.getProperties().setProperty("java.endorsed.dirs",java_endorsed_dirs);
        }


        String home=XTTProperties.getProperty("JACORB/HOME");
        if(home.equals("null"))
        {
            System.getProperties().setProperty("jacorb.home",".");
        } else
        {
            System.getProperties().setProperty("jacorb.home",home);
        }

        String verbosity=XTTProperties.getProperty("JACORB/LOG/VERBOSITY");
        if(verbosity.equals("null"))
        {
            System.getProperties().setProperty("jacorb.log.default.verbosity","1");
            System.getProperties().setProperty("jacorb.poa.log.verbosity"    ,"1");
            System.getProperties().setProperty("jacorb.naming.log.verbosity" ,"1");
        } else
        {
            System.getProperties().setProperty("jacorb.log.default.verbosity",verbosity);
            System.getProperties().setProperty("jacorb.poa.log.verbosity"    ,verbosity);
            System.getProperties().setProperty("jacorb.naming.log.verbosity" ,verbosity);
        }

        String useXTTLogger=XTTProperties.getProperty("JACORB/LOG/USEXTTLOGGER");
        if(!useXTTLogger.equals("null"))
        {
            System.getProperties().setProperty("jacorb.log.loggerFactory" ,"com.mobixell.xtt.Jacorb_AvalonFrameworkLoggerFactory");
        }


    }

    /**
     * returns the getClass().getName() of this object. For debug reasons only.
     */
    public abstract String toString();



    public void printVersion()
    {
        XTTProperties.printDebug(getClass().getSuperclass().getName()+": "+parseVersion(tantau_sccsid));
            try
            {
                XTTProperties.printDebug("org.jacorb.naming.NameServer: "+parseVersion(Class.forName("org.jacorb.naming.NameServer").getField("tantau_sccsid").get(this).toString()));
            } catch (java.lang.IllegalAccessException iae)
            {
                XTTProperties.printDebug("org.jacorb.naming.NameServer: custom version missing");
            } catch (java.lang.NoSuchFieldException iae)
            {
                XTTProperties.printDebug("org.jacorb.naming.NameServer: custom version missing");
            } catch (java.lang.ClassNotFoundException cnfe)
            {
                XTTProperties.printDebug("org.jacorb.naming.NameServer: custom version missing");
            }
        XTTProperties.printDebug(this.getClass().getName()+": "+getVersion());
    }


    /**
     * Start the Jacorb Name Server.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name,
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void startJacorbNameServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": startJacorbNameServer: no arguments required");
            return;
        }
        if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ": missing arguments: no arguments required");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        } else
        {
            XTTProperties.printInfo(parameters[0] + ": starting.");
            Thread t=new NameServer();
            t.start();
            try
            {
                Thread.sleep(500);
            } catch(Exception e){}
        }
    }

    /**
     * Stop the Jacorb Name Server.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name,
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void stopJacorbNameServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": stopJacorbNameServer: no arguments required");
            return;
        }
        if(parameters.length>1)
        {
            XTTProperties.printWarn(parameters[0] + ": no arguments required: omitting arguments");
        }
        XTTProperties.printInfo(parameters[0] + ": shutting down.");
        NS ns=new NS();
        ns.shutdown();
    }

    private class NS extends org.jacorb.naming.NameServer
    {
        public void shutdown()
        {
            try
            {
                /*
                orb.shutdown( true );
                */
                synchronized(shutdownKey)
                {
                    properShutdown=false;
                }
                XTTProperties.printVerbose("org.jacorb.naming.NameServer: shutting down now:");
                ((org.omg.CORBA.ORB)this.getClass().getField("orb").get(this)).shutdown(true);
                synchronized(shutdownKey)
                {
                    int wait=XTTProperties.getIntProperty("SIS/WAITTIMEOUT");
                    try
                    {
                        shutdownKey.wait(10000);
                    } catch (Exception e)
                    {
                        XTTProperties.printWarn("org.jacorb.naming.NameServer: shutdown not working properly");
                    }
                    XTTProperties.printVerbose("org.jacorb.naming.NameServer: propper shutdown done: "+properShutdown);
                }
            } catch (java.lang.IllegalAccessException iae)
            {
                XTTProperties.printWarn("org.jacorb.naming.NameServer: not supported: custom version missing.");
            } catch (java.lang.NoSuchFieldException iae)
            {
                XTTProperties.printWarn("org.jacorb.naming.NameServer: not supported: custom version missing.");
            }
        }
    }

    private class NameServer extends Thread
    {
        public NameServer()
        {
            super("JacorbNameServer");
        }
        public void run()
        {
            synchronized(nameKey)
            {
                try
                {
                    XTTProperties.printVerbose("org.jacorb.naming.NameServer: starting now:");
                    org.jacorb.naming.NameServer.main(args);
                    XTTProperties.printVerbose("org.jacorb.naming.NameServer: stopping done!");
                    synchronized(shutdownKey)
                    {
                        properShutdown=true;
                        shutdownKey.notifyAll();
                    }
                } catch (java.lang.IllegalArgumentException ex)
                {
                }
            }

        }
    }

    public String checkResources()
    {
        int standardPort = XTTProperties.getIntProperty("JACORB/NAMING/PORT");

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
}