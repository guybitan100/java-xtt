package com.mobixell.xtt;

import java.util.HashMap;
import org.jdom.Element;
import java.util.Enumeration;

/**
 * FunctionModule_SNMP.
 * <p>
 * Functions for testing XTT consistency.
 *
 * @author      Gavin Cattell
 * @version     $Revision: 1.14 $
 */
public class FunctionModule_SNMP extends FunctionModule
{
    private static javax.management.snmp.manager.SnmpEventReportDispatcher trapAgent = null;
    private static TrapListener trapListener = null;
    private static Thread trapThread = null;
    private boolean showAllTraps = false;
    private HashMap<String,Trap> traps = new HashMap<String,Trap>();
    private final static int STDPORT=1620;

    public FunctionModule_SNMP()
    {
        addTraps(XTTXML.getElements("snmp/fail/trap"),XTTProperties.FAIL);
        addTraps(XTTXML.getElements("snmp/warn/trap"),XTTProperties.WARN);
        addTraps(XTTXML.getElements("snmp/info/trap"),XTTProperties.INFO);
        addTraps(XTTXML.getElements("snmp/verbose/trap"),XTTProperties.VERBOSE);
        addTraps(XTTXML.getElements("snmp/debug/trap"),XTTProperties.DEBUG);

        String startOnLoad = XTTProperties.getProperty("snmp/STARTONLOAD");
        if(startOnLoad.equals("") || ConvertLib.textToBoolean(startOnLoad))
        {
            if(trapThread == null)
            {
                startTrapListener(new String[]{"startTrapListener"});
            }
        }
        showAllTraps = !XTTProperties.getProperty("snmp/showalltraps").equals("null");
    }

    private void addTraps(Element[] list,int level)
    {
        if((list==null)||(list.length<=0))
        {
            return;
        }
        else
        {
            String message = null;
            String oid = null;
            for(int i=0;i<list.length;i++)
            {
                try
                {
                    message = XTTXML.getChild("message",list[i]).getText();
                }
                catch(java.lang.NullPointerException npe)
                {
                    message = null;
                }
                try
                {
                    oid = XTTXML.getChild("oid",list[i]).getText();
                }
                catch(java.lang.NullPointerException npe)
                {
                    XTTProperties.printFail("SNMP: There was no OID found for this trap, skipping");
                    continue;
                }

                traps.put(oid,new Trap(level,message));
            }
        }
    }

    public int startTrapListener(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": startTrapListener:"+NO_ARGUMENTS);
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        } else
        {
            int port = XTTProperties.getIntProperty("snmp/listenerport");
            if(port == -1)
            {
                port = STDPORT;
                XTTProperties.printVerbose(parameters[0] + ": Port not defined; setting to "+STDPORT);
            }
            try
            {
                trapAgent = new javax.management.snmp.manager.SnmpEventReportDispatcher(port);
                trapListener=new TrapListener(traps);
                trapAgent.addTrapListener(trapListener);
                trapThread = new Thread(trapAgent);
                trapThread.setPriority(Thread.MAX_PRIORITY);
                trapThread.start();
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": Error setting port");
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
                return status;
            }
        }
        return status;
    }

    public int stopTrapListener(String parameters[])
    {
    	int status = XTTProperties.PASSED;
    	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": stopTrapListener:"+NO_ARGUMENTS);
            return XTTProperties.FAILED_NO_ARGUMENTS;
        } else if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else if (trapAgent!=null)
        {
            for (Enumeration e = trapAgent.getTrapListeners() ; e.hasMoreElements() ;)
            {
                trapAgent.removeTrapListener((javax.management.snmp.manager.SnmpTrapListener)e.nextElement());
            }
            try
            {
                trapAgent.close();
                trapThread.join();
            }
            catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": Error joining thread");
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);

            }
            trapThread   = null;
            trapAgent    = null;
            trapListener = null;
            XTTProperties.printInfo(parameters[0] + ": TrapListener stopped");

        } else
        {
            XTTProperties.printFail(parameters[0] + ": TrapListener isn't running, can't stop");
            status = XTTProperties.FAILED;
            XTTProperties.setTestStatus(status);
        }
        return status;
    }

    public void initialize()
    {
        if(trapListener!=null)
        {
            trapListener.clearWaitKeys();
        }
    }

    /**
     * returns the getClass().getName() of this object. For debug reasons only.
     */
    public String toString()
    {
        return this.getClass().getName();
    }


    private class TrapListener implements javax.management.snmp.manager.SnmpTrapListener
    {
        private HashMap<String,Trap> traps = null;
        private HashMap<String,Integer> trapWaitKeys = new HashMap<String,Integer>();
        public int status = XTTProperties.PASSED;;
        public TrapListener(HashMap<String,Trap> traps)
        {
            this.traps = traps;
        }

        public void processSnmpTrapV1(javax.management.snmp.SnmpPduTrap trap)
        {
            System.out.println("Got V1 trap");
        }

        public void processSnmpTrapV2(javax.management.snmp.SnmpPduRequest trap)
        {
            //System.out.println("Got V2 trap");
            //System.out.println("Error Status: " + trap.errorStatus);
            //System.out.println("Error Index: " + trap.errorIndex);
            //System.out.println("Variables: " + trap.varBindList.length);
            //for(int i =0; i<trap.varBindList.length; i++)
            //{
            //    System.out.println("Variable(" + i + "): " + trap.varBindList[i].getStringValue());
            //}

            String trapOID = trap.varBindList[1].getStringValue();

            trap.address.getHostAddress();

            //XMP PROCESS DOWN TRAP
            if((trap.varBindList.length > 3)&&(trapOID.equals("1.3.6.1.4.1.12702.7.10000.9")))
            {
                int expectedStops = XTTProperties.getIntVariable("snmp/expectedstops");
                if(expectedStops <= 0)
                {
                    XTTProperties.printFail("XMP Process " + trap.varBindList[2].getStringValue() +" went down unexpectedly");
                    status = XTTProperties.FAILED_WITH_CORE;
                    XTTProperties.setTestStatus(status);
                }
                else
                {
                    expectedStops--;
                    XTTProperties.printVerbose("XMP Process " + trap.varBindList[2].getStringValue() +" went down, we expected this");
                    XTTProperties.setVariable("snmp/expectedstops",""+expectedStops);

                }

                //System.out.println("Process " + trap.varBindList[2].getStringValue() +" down");
            }
            //XMG PROCESS DOWN TRAP
            else if((trap.varBindList.length > 3)&&(trapOID.equals("1.3.6.1.4.1.12702.1.10006.9")))
            {
                int expectedStops = XTTProperties.getIntVariable("snmp/expectedstops");
                if(expectedStops <= 0)
                {
                    XTTProperties.printFail("XMG Process " + trap.varBindList[2].getStringValue() +" went down unexpectedly");
                    status = XTTProperties.FAILED_WITH_CORE;
                    XTTProperties.setTestStatus(status);
                }
                else
                {
                    expectedStops--;
                    XTTProperties.printVerbose("XMG Process " + trap.varBindList[2].getStringValue() +" went down, we expected this");
                    XTTProperties.setVariable("snmp/expectedstops",""+expectedStops);

                }

                //System.out.println("Process " + trap.varBindList[2].getStringValue() +" down");
            }
            //XMA PROCESS DOWN TRAP
            else if((trap.varBindList.length > 3)&&(trapOID.equals("1.3.6.1.4.1.12702.3.10000.9")))
            {
                int expectedStops = XTTProperties.getIntVariable("snmp/expectedstops");
                if(expectedStops <= 0)
                {
                    XTTProperties.printFail("XMA Process " + trap.varBindList[2].getStringValue() +" went down unexpectedly");
                    status = XTTProperties.FAILED_WITH_CORE;
                    XTTProperties.setTestStatus(status);
                }
                else
                {
                    expectedStops--;
                    XTTProperties.printVerbose("XMA Process " + trap.varBindList[2].getStringValue() +" went down, we expected this");
                    XTTProperties.setVariable("snmp/expectedstops",""+expectedStops);

                }
            }
            //XSN PROCESS DOWN TRAP
            else if((trap.varBindList.length > 3)&&(trapOID.equals("1.3.6.1.4.1.12702.6.10000.9")))
            {
                int expectedStops = XTTProperties.getIntVariable("snmp/expectedstops");
                if(expectedStops <= 0)
                {
                    XTTProperties.printFail("XSN Process " + trap.varBindList[2].getStringValue() +" went down unexpectedly");
                    status = XTTProperties.FAILED_WITH_CORE;
                    XTTProperties.setTestStatus(status);
                }
                else
                {
                    expectedStops--;
                    XTTProperties.printVerbose("XSN Process " + trap.varBindList[2].getStringValue() +" went down, we expected this");
                    XTTProperties.setVariable("snmp/expectedstops",""+expectedStops);

                }
            }
            //SLM PROCESS DOWN TRAP
            else if((trap.varBindList.length > 3)&&(trapOID.equals("1.3.6.1.4.1.12702.8.10000.8")))
            {
                int expectedStops = XTTProperties.getIntVariable("snmp/expectedstops");
                if(expectedStops <= 0)
                {
                    XTTProperties.printFail("SLM Process " + trap.varBindList[2].getStringValue() +" went down unexpectedly");
                    status = XTTProperties.FAILED_WITH_CORE;
                    XTTProperties.setTestStatus(status);
                }
                else
                {
                    expectedStops--;
                    XTTProperties.printVerbose("SLM Process " + trap.varBindList[2].getStringValue() +" went down, we expected this");
                    XTTProperties.setVariable("snmp/expectedstops",""+expectedStops);

                }
            }
            //XSAM PROCESS DOWN TRAP
            else if((trap.varBindList.length > 3)&&(trapOID.equals("1.3.6.1.4.1.12702.4.10000.8")))
            {
                int expectedStops = XTTProperties.getIntVariable("snmp/expectedstops");
                if(expectedStops <= 0)
                {
                    XTTProperties.printFail("XSAM Process " + trap.varBindList[2].getStringValue() +" went down unexpectedly");
                    status = XTTProperties.FAILED_WITH_CORE;
                    XTTProperties.setTestStatus(status);
                }
                else
                {
                    expectedStops--;
                    XTTProperties.printVerbose("XSAM Process " + trap.varBindList[2].getStringValue() +" went down, we expected this");
                    XTTProperties.setVariable("snmp/expectedstops",""+expectedStops);

                }
            }            
            else if(traps.get(trapOID) != null)
            {
                int action = traps.get(trapOID).action;
                String message = traps.get(trapOID).message;
                switch (action)
                {
                    case XTTProperties.FAIL: XTTProperties.printFail("SnmpTrap: " + message + ""); XTTProperties.setTestStatus(XTTProperties.FAILED); break;
                    case XTTProperties.WARN: XTTProperties.printWarn("SnmpTrap: " + message + ""); break;
                    case XTTProperties.INFO: XTTProperties.printInfo("SnmpTrap: " + message + ""); break;
                    case XTTProperties.VERBOSE: XTTProperties.printVerbose("SnmpTrap: " + message + ""); break;
                    case XTTProperties.DEBUG: XTTProperties.printDebug("SnmpTrap: " + message + ""); break;
                    default: XTTProperties.printFail("SnmpTrap: No such level of output '" + action + "'"); 
                    status = XTTProperties.FAILED;
                    XTTProperties.setTestStatus(status);
                    break;
                }
            }

            if(showAllTraps)
            {
                XTTProperties.printDebug("Trap: " + trap.varBindList[1].getStringValue() + " was received");
            }


            synchronized(trapWaitKeys)
            {
                Integer wakeOID=trapWaitKeys.get(trapOID);
                if(wakeOID!=null)
                {
                    trapWaitKeys.put(trapOID,new Integer(wakeOID.intValue()+1));
                } else
                {
                    trapWaitKeys.put(trapOID,new Integer(1));
                }
                String totalkey=null;
                Integer totalTraps=trapWaitKeys.get(totalkey);
                if(totalTraps!=null)
                {
                    trapWaitKeys.put(totalkey,new Integer(totalTraps.intValue()+1));
                } else
                {
                    trapWaitKeys.put(totalkey,new Integer(1));
                }
                trapWaitKeys.notifyAll();
            }
            
        }
		public void clearWaitKeys()
        {
            synchronized(trapWaitKeys)
            {
                trapWaitKeys.clear();
            }
        }

        public void processSnmpTrapV3(javax.management.snmp.SnmpScopedPduRequest trap)
        {
            System.out.println("Got V3 trap");
        }

        public int waitForTraps(int numTraps,String trapOID) throws java.lang.InterruptedException
        {
        	int status = XTTProperties.PASSED;
            int wait=XTTProperties.getIntProperty("SNMP/WAITTIMEOUT");
            int prevcount=0;
            long endtime=System.currentTimeMillis()+wait;
            long remainingwait=endtime-System.currentTimeMillis();
            String trapText=trapOID;
            if(trapText==null)trapText="any";
            synchronized(trapWaitKeys)
            {
                int trapCount=0;
                do
                {
                    Integer wakeOID=trapWaitKeys.get(trapOID);
                    if(wakeOID==null)
                    {
                        wakeOID=new Integer(0);
                        trapWaitKeys.put(trapOID,wakeOID);
                    }
                    trapCount=wakeOID.intValue();

                    if(numTraps<=0)numTraps=trapCount+1;

                    XTTProperties.printInfo("TrapListener.waitForTraps: "+trapText+": "+trapCount+"/"+numTraps);
                    if(trapCount>=numTraps)break;
                    if(wait>0)
                    {
                        prevcount=trapCount;
                        if(remainingwait>0)
                        {
                            trapWaitKeys.wait(remainingwait);
                        }
                        remainingwait=endtime-System.currentTimeMillis();
                        if(trapCount==prevcount&&remainingwait<=0)
                        {
                            XTTProperties.printFail("TrapListener.waitForTraps: "+trapText+": "+trapCount+"/"+numTraps+" timed out!");
                            status = XTTProperties.FAILED;
                            XTTProperties.setTestStatus(status);
                            trapCount=numTraps;
                            return status;
                        }
                    } else
                    {
                        trapWaitKeys.wait();
                    }
                } while(trapCount<numTraps);
                //XTTProperties.printInfo("TrapListener.waitForTraps: "+trapCount+"/"+numTraps);
            }
            return status;
        }        
    }

    private class Trap
    {
        public String message="";
        public int action=-1;

        public Trap(int action, String message)
        {
            this.action = action;
            this.message = message;
        }


    }

    public int waitForTraps(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForTraps: numTraps");
            XTTProperties.printFail(this.getClass().getName()+": waitForTraps: numTraps trapOID");
            return XTTProperties.FAILED;
        }
        if(parameters.length<2||parameters.length>3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": numTraps");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": numTraps trapOID");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
        } else if(trapListener==null)
        {
            XTTProperties.printFail(parameters[0] + ": Trap listener not running");
            status = XTTProperties.FAILED;
            XTTProperties.setTestStatus(status);
            
        } else
        {
            try
            {
                int numTraps=Integer.decode(parameters[1]);
                String trapOID=null;
                if(parameters.length>2)trapOID=parameters[2];
                XTTProperties.printInfo(parameters[0] + ": waiting for '"+trapOID+"' received on SNMP listener");
                trapListener.waitForTraps(numTraps,trapOID);
                return status;
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": '"+parameters[1]+"' is NOT a correct number");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }

   public String checkResources()
    {
        int port = XTTProperties.getIntProperty("SNMP/LISTENERPORT");
        if(port<=0)port=STDPORT;

        String resourceString = "" + this.getClass().getName() + ":"+RESOURCE_OK;
        try
        {
            String startonload=XTTProperties.getProperty("SNMP/STARTONLOAD");
            if(trapListener==null||startonload==null||startonload.equals("null"))
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

    public String getConfigurationOptions()
    {
        return "    <!-- function module SNMP -->"
        +"\n    <SNMP>"
        +"\n        <StartOnLoad>true</StartOnLoad>"
        +"\n        <!-- default ListenerPort is 1620 -->"
        +"\n        <!--ListenerPort>1620</ListenerPort-->"
        +"\n        <!-- When the SNMP Listener receives a trap with the OID a message is printed on fail level"
        +"\n             The test is also set to failed -->"
        +"\n        <!--fail>"
        +"\n          <trap>"
        +"\n             <message>Process Running</message>"
        +"\n             <OID>1.3.6.1.4.1.12702.6.10000.8</OID>"
        +"\n          </trap>"
        +"\n          <trap>"
        +"\n             <message>Example</message>"
        +"\n             <OID>1.3.6.1.4.1.12702.6.10000.6</OID>"
        +"\n          </trap>"
        +"\n        </fail-->"
        +"\n        <!-- Same as fail, but the test isn't failed, and a warn message is printed -->"
        +"\n        <!--warn>"
        +"\n          ..."
        +"\n        </warn-->"
        +"\n        <!-- Same as warn, with an info message instead -->"
        +"\n        <!--info>"
        +"\n          ..."
        +"\n        </info-->"
        +"\n        <!-- Same as info, with an verbose message instead -->"
        +"\n        <!--verbose>"
        +"\n          ..."
        +"\n        </verbose-->"
        +"\n        <!-- Same as verbose, with an debug message instead -->"
        +"\n        <!--debug>"
        +"\n          ..."
        +"\n        </debug-->"
        +"\n    </SNMP>";
    }

    public static final String tantau_sccsid = "@(#)$Id: FunctionModule_SNMP.java,v 1.14 2008/05/21 11:21:14 gcattell Exp $";
}

