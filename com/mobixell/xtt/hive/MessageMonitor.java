package com.mobixell.xtt.hive;

import com.mobixell.xtt.XTTProperties;

public class MessageMonitor extends Thread
{
    private static final int DEFAULTRETRIES = 3;
    private static final int DEFAULTTIMEOUT = 5000;
    private Message myMessage = null;
    
    public MessageMonitor(Message myMessage)
    {
        this.myMessage = myMessage;    
    }
    
    public void run()
    {
        if(!XTTProperties.getQuietProperty("SYSTEM/HIVE/DISABLEMONITOR").equals("null"))
        {
            //We don't want to have a monitor, just return.
            return;
        }
        int retries = XTTProperties.getQuietIntProperty("SYSTEM/HIVE/MESSAGERETRIES");
        int timeout = XTTProperties.getQuietIntProperty("SYSTEM/HIVE/MESSAGETIMEOUT");
        if(retries <= 0) retries = DEFAULTRETRIES;
        if(timeout <= 0) timeout = DEFAULTTIMEOUT;
        //Hive.printDebug("MessageMonitor: Trying to get message key");
        synchronized(Message.messagekey)
        {
            //Hive.printDebug("MessageMonitor: Got message key");
            //It's already delivered, we don't need to monitor anything!
            if(myMessage.isDelivered())
            {
                //Hive.printDebug("MessageMonitor: Message already delivered. Stopping Monitor");
                return;
            }
            
            int count = 0;
            long timer = 0;
            long curTime = 0;
            int timeoutReduction = 0;
            //Hive.printDebug("MessageMonitor: While "+count+" < "+retries);
            while(count < retries)
            {
                //Hive.printDebug("MessageMonitor: Waiting " + (timeout - timeoutReduction) + "ms for message to be sent");
                timer = System.currentTimeMillis();
                try
                {
                    Message.messagekey.wait(timeout - timeoutReduction);
                    curTime = System.currentTimeMillis();

                    if(myMessage.isDelivered())
                    {
                        //Hive.printDebug("MessageMonitor: Message has been delivered. Stopping Monitor");
                        return;
                    }               
                    
                    if((curTime - timer) < timeout)
                    {
                        timeoutReduction = (int)(curTime - timer);
                    }
                    else
                    {
                        myMessage.resend();
                        count++;
                        timeoutReduction = 0;
                        //Hive.printDebug("MessageMonitor: Resending attempt: " + count + "/" + retries);                        
                    }                    
                }
                catch(InterruptedException ie)
                {
                }
            }
            myMessage.setStatus(Message.EXPIRED);
            Message.messagekey.notify();            
        }
    }
    
    public static final String tantau_sccsid = "@(#)$Id: MessageMonitor.java,v 1.5 2008/02/08 10:58:16 gcattell Exp $";
}