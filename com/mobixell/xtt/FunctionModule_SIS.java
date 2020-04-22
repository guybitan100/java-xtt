package com.mobixell.xtt;

import com.mobixell.xtt.sis.SubscriberInformationServiceImpl;
//import java.io.*;
//import org.omg.PortableServer.*;
import org.omg.CosNaming.*;
//import org.omg.CORBA.OctetSeqHelper;
//import org.omg.CORBA.StringSeqHelper;

import java.util.Vector;


/**
 * FunctionModule_SIS.
 *
 * @author      Roger Soder
 * @version     $Revision: 1.9 $
 */
public class FunctionModule_SIS extends AbstractFunctionModule_Jacorb
{
    public static final String tantau_sccsid = "@(#)$Id: FunctionModule_SIS.java,v 1.9 2010/05/05 08:12:37 rajesh Exp $";

    private static org.omg.CORBA.ORB serverorb=null;
    
    private static Object shutdownKey=new Object();
    private boolean properShutdown=false;

    private Vector<NamingContextExt> contexts=new Vector<NamingContextExt>();
    private Vector<NameComponent[]> names=new Vector<NameComponent[]>();

    public void initialize()
    {
        super.initialize();
        XTTProperties.printDebug(this.getClass().getName()+".initialize(): clearing variables");
        SubscriberInformationServiceImpl.initialize();
    }
    /**
     * Returns the Configuration Options as a String ready to copy/paste in a configuration file
     *
     */
    public String getConfigurationOptions()
    {
        return "    <!-- function module SIS -->"
        +"\n    <sis>"
        +"\n        <ServiceName>SwisscomMobile/Subscription3/AllServers/SIS_Service</ServiceName>"
        +"\n        <!-- time to wait on a \"wait\" function before continuing -->"
        +"\n        <WaitTimeout>32000</WaitTimeout>"
        +"\n    </sis>";
    }
    
    /**
     * returns the getClass().getName() of this object. For debug reasons only.
     */
    public String toString()
    {
        return this.getClass().getName();
    }

    public void startSISServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": startSISServer:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": startSISServer: ServiceName");
            return;
        }
        if(parameters.length>2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+":"+NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": ServiceName");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            String serviceName=null;
            if(parameters.length==2)
            {
                serviceName=parameters[1];
            } else
            {
                serviceName=XTTProperties.getProperty("SIS/SERVICENAME");
            }
            if(serviceName.equals("null"))
            {
                serviceName="SubsSelectedInfoAccess.service";
            }
            XTTProperties.printInfo(parameters[0] + ": starting '"+serviceName+"'.");
            XTTProperties.printDebug(parameters[0] + ": starting now: '"+serviceName+"'");
            serverorb = org.omg.CORBA.ORB.init(new String[]{}, null);
            try
            {
                org.omg.PortableServer.POA poa =
                org.omg.PortableServer.POAHelper.narrow(serverorb.resolve_initial_references("RootPOA"));
                poa.the_POAManager().activate();
                org.omg.CORBA.Object subsSelectedInfoAccessService = poa.servant_to_reference(new SubscriberInformationServiceImpl());

                // register server with naming context
                NamingContextExt nc = NamingContextExtHelper.narrow(serverorb.resolve_initial_references("NameService"));

                String[] context=serviceName.split("/");
                NamingContextExt subContext=nc;
                NameComponent[] name = null;
                for(int i=0;i<context.length-1;i++)
                {
                    // look up an object
                    //name = new NameComponent[1];
                    name = subContext.to_name(context[i]);
                    try
                    {
                        subContext = NamingContextExtHelper.narrow( subContext.bind_new_context( name ));
                        contexts.add(subContext);
                        names.add(name);
                    } catch (org.omg.CosNaming.NamingContextPackage.AlreadyBound ab)
                    {
                        XTTProperties.printDebug(parameters[0] + ": context already bound: "+context[i]);
                        
                        name = subContext.to_name(context[i]);//contextName);
                        subContext = NamingContextExtHelper.narrow( subContext.resolve( name ));
                        contexts.add(subContext);
                        names.add(name);
                        
                        //subContext.unbind( name );
                        //subContext = NamingContextExtHelper.narrow( subContext.bind_new_context( name ));

                    }
                }
                names.add(subContext.to_name(context[context.length-1]));
                subContext.bind(subContext.to_name(context[context.length-1]),subsSelectedInfoAccessService);
                Thread t=new RunServer();
                t.start();

            } catch ( org.omg.CosNaming.NamingContextPackage.InvalidName in)
            {
                in.printStackTrace();
                XTTProperties.printFail(parameters[0] + ": invalid name for service: "+serviceName);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(in);
                }
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } catch ( Exception e )
            {
                XTTProperties.printFail(parameters[0] + ": error starting CORBA service: "+e.getMessage());
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
    }
    private class RunServer extends Thread
    {
        public RunServer()
        {
            super("SISRunServer");
        }
        public void run()
        {
            XTTProperties.printDebug("RunServer.run: Server ORB running!");
            serverorb.run();
            XTTProperties.printDebug("RunServer.run: Server ORB done!");
            synchronized(shutdownKey)
            {
                properShutdown=true;
                shutdownKey.notifyAll();
            }
            
        }
    }
    public void stopSISServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": stopSISServer:"+NO_ARGUMENTS);
            return;
        }
        if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        }
        XTTProperties.printInfo(parameters[0] + ": shutting down.");
        XTTProperties.printDebug(parameters[0] + ": shutting down now:");
        synchronized(shutdownKey)
        {
            properShutdown=false;
        }

        try
        {
            NamingContextExt nc = NamingContextExtHelper.narrow(serverorb.resolve_initial_references("NameService"));
            NamingContextExt subContext=nc;
            NameComponent[] name = null;
            for(int i=names.size()-1;i>0;i--)
            {
                // look up an object
                //name = new NameComponent[1];
                name = (NameComponent[])names.get(i);
                subContext = (NamingContextExt)contexts.get(i-1);
                subContext.unbind( name );
            }
            name = (NameComponent[])names.get(0);
            nc.unbind(name);
            contexts.clear();
            names.clear();
         } catch (Exception e)
         {
            XTTProperties.printWarn(parameters[0] + ": error unbinding contexts, shutting down anyway:\n"+e.getMessage());
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
         }

        serverorb.shutdown(false);
        synchronized(shutdownKey)
        {
            int wait=XTTProperties.getIntProperty("SIS/WAITTIMEOUT");
            try
            {
                XTTProperties.printVerbose(parameters[0] + ": waiting on shutdown to finish");
                shutdownKey.wait(10000);
            } catch (Exception e){}
            XTTProperties.printVerbose(parameters[0] + ": propper shutdown done: "+properShutdown);

        }
    }

    /**
     * wait for a SIS call on the SISServer
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code> argument is the number of calls received total since starting the SISServer
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see com.mobixell.xtt.sis.SubscriberInformationServiceImpl#waitForSISCalls(int)
     */
    public boolean waitForSISCalls(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForSISCalls: numCalls");
            return false;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": numCalls");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return false;
        } else
        {
            try
            {
                int calls=Integer.parseInt(parameters[1]);
                XTTProperties.printInfo(parameters[0] + ": waiting for "+calls+" SIS calls on SISServer");
                SubscriberInformationServiceImpl.waitForSISCalls(calls);
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
    public boolean waitForTimeoutSISCalls(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForTimeoutSISCalls: timeoutms");
            XTTProperties.printFail(this.getClass().getName()+": waitForTimeoutSISCalls: timeoutms maxPreviousCalls");
            return false;
        }
        if(parameters.length<2||parameters.length>3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": timeoutms");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": timeoutms maxPreviousCalls");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return false;
        } else
        {
            try
            {
                int timeout=Integer.parseInt(parameters[1]);
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
                XTTProperties.printInfo(parameters[0] + ": waiting for "+timeout+"ms and NO SIS calls on SIS server");
                SubscriberInformationServiceImpl.waitForTimeoutSISCalls(timeout,maxnumber);
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
     * wait for a specific number of lookups for phone numbers on the SISServer
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code> argument is the number of calls received total since starting the SISServer
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see com.mobixell.xtt.sis.SubscriberInformationServiceImpl#waitForSISCalls(int)
     */
    public boolean waitForSISLookups(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForSISLookups: numCalls");
            return false;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": numCalls");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return false;
        } else
        {
            try
            {
                int calls=Integer.parseInt(parameters[1]);
                XTTProperties.printInfo(parameters[0] + ": waiting for "+calls+" SIS calls on SISServer");
                SubscriberInformationServiceImpl.waitForSISLookups(calls);
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
     * wait for no lookups for phone numbers on the SISServer
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code> argument is the number of calls received total since starting the SISServer
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see com.mobixell.xtt.sis.SubscriberInformationServiceImpl#waitForSISCalls(int)
     */
    public boolean waitForTimeoutSISLookups(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForTimeoutSISLookups: timeoutms");
            XTTProperties.printFail(this.getClass().getName()+": waitForTimeoutSISLookups: timeoutms maxPreviousLookups");
            return false;
        }
        if(parameters.length<2||parameters.length>3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": timeoutms");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": timeoutms maxPreviousLookups");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return false;
        } else
        {
            try
            {
                int timeout=Integer.parseInt(parameters[1]);
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
                XTTProperties.printInfo(parameters[0] + ": waiting for "+timeout+"ms and NO SIS lookups on SIS server");
                SubscriberInformationServiceImpl.waitForTimeoutSISLookups(timeout,maxnumber);
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
     * wait for specified amount of miliseconds before sending each response
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code> argument is the delay in miliseconds
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see com.mobixell.xtt.sis.SubscriberInformationServiceImpl#waitForSISCalls(int)
     */
    public boolean setSISResponseDelay(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setSISResponseDelay: responsedelayms");
            return false;
        }
        if(parameters.length<2||parameters.length>3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": responsedelayms");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return false;
        } else
        {
            try
            {
                int timeout=Integer.parseInt(parameters[1]);
                XTTProperties.printInfo(parameters[0] + ": setting SIS server response delay to "+timeout+"ms");
                SubscriberInformationServiceImpl.setResponseDelay(timeout);
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
     * wait for (specified amount*number of calls) of miliseconds before sending each response
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code> argument is the delay in miliseconds
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see com.mobixell.xtt.sis.SubscriberInformationServiceImpl#waitForSISCalls(int)
     */
    public boolean setSISPerCallDelay(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setSISPerCallDelay: responsedelayms");
            return false;
        }
        if(parameters.length<2||parameters.length>3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": responsedelayms");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return false;
        } else
        {
            try
            {
                int timeout=Integer.parseInt(parameters[1]);
                XTTProperties.printInfo(parameters[0] + ": setting SIS server per call delay to "+timeout+"ms");
                SubscriberInformationServiceImpl.setPerCallDelay(timeout);
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
     * Set the number that is used as input for the decoding of parameters instead of the supplied phone number. Example 41790000009.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code> the override number for the SISServer
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see com.mobixell.xtt.sis.SubscriberInformationServiceImpl#setOverrideNumber(String)
     */
    public boolean setOverrideNumber(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setOverrideNumber:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": setOverrideNumber: overrideNumber");
            return false;
        }
        if(parameters.length<1||parameters.length>2)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": overrideNumber");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return false;
        } else
        {
            try
            {
                String number=null;
                if(parameters.length>=2)
                {
                    number=parameters[1];
                    long lon=Long.parseLong(number);
                }
                XTTProperties.printInfo(parameters[0] + ": setting SIS server override number to "+number);
                SubscriberInformationServiceImpl.setOverrideNumber(number);
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
}