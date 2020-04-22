package com.mobixell.xtt.hive;

import com.mobixell.xtt.XTTProperties;

public class HiveMonitor extends Thread
{
    public void run()
    {
        if(!XTTProperties.getProperty("SYSTEM/HIVE/DISABLEMONITOR").equals("null"))
        {
            //We don't want to have a monitor, just return.
            return;
        }
        synchronized (Hive.remotepacketkey)
        {
            try
            {
                if(Hive.getShouldTrace())
                {
                    XTTProperties.printDebug("HiveMonitor: Started monitoring for new Drones.");
                }
                
                //If we're in assimilate mode, we want a list, so we should get a list.
                if(XTTProperties.getXTTGui() != null)
                {
                    Hive.remotepacketkey.wait(5000);
                    if(Hive.remotepacketcount < 1)
                    {
                        if(Hive.getShouldTrace())
                        {
                            XTTProperties.printFail("HiveMonitor: No Drones found in the network.");
                        }
                        Hive.setOutOfHive();
                    }
                }
            }
            catch(InterruptedException ie)
            {
                //XTTProperties.printFail("No Drones");    
            }
            catch(Exception e)
            {
                //XTTProperties.printFail("Other wose exception");
                //XTTProperties.printException(e);    
            }
        }
        
        int lastDronereqcount = 0;
        int randomWait = 60000;
        
        synchronized (Hive.dronereqkey)
        {
            while(true)
            {
                try
                {
                    lastDronereqcount = Hive.dronereqcount;
                    //Random between 0 and 5*60*1000 (or 5minutes in milliseconds)
                    randomWait = new java.util.Random().nextInt(5*60*1000);
                    //Random wait between 5minutes and 10minutes.
                    randomWait += 5*60*1000;
                    Hive.dronereqkey.wait(randomWait);
                    if(Hive.dronereqcount == lastDronereqcount)
                    {
                        if(Hive.getShouldTrace())
                        {
                            XTTProperties.printDebug("HiveMonitor: No Drone List Requests were made during '"+randomWait+"ms, refreshing list.");
                        }                        
                        HiveWorker.refreshDroneList();
                    }
                }
                catch(InterruptedException ie)
                {
                    //XTTProperties.printFail("No Drones");    
                }
            }
        }
        
    }
    
    public static final String tantau_sccsid = "@(#)$Id: HiveMonitor.java,v 1.13 2008/02/01 12:53:42 gcattell Exp $";
}