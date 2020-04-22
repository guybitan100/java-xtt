package com.mobixell.xtt;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.text.DecimalFormat;
import java.util.Random;

public class VODAPAMIWorker extends Thread  implements BillingWorker
{
    // True if the Worker should stop
    private boolean stop = false;
    // ID of the Worker
    private int id;

    private Socket s = null;

    // Number of instances running
    private static int instances=0;
    private static final String version="PMS 4.3";

    // Key for the instances and the current message id
    private static Object key = new Object();

    private BufferedReader in;
    private BufferedWriter out;
    private DecimalFormat codeFormat = new DecimalFormat("#####");

    private static HashMap<String,String> allowRetries = new HashMap<String,String>();
    private static HashMap<String,String> debitRetries = new HashMap<String,String>();

    private BillingServer parent = null;

    private static Random rgen = new Random();

    public VODAPAMIWorker (int id,Socket s,BillingServer parent)
    {
        codeFormat.setMinimumIntegerDigits(5);
        codeFormat.setMaximumIntegerDigits(5);
        this.s = s;
        this.id = id;
        this.parent = parent;
    }

    public int getWorkerId()
    {
        return id;
    }

     /**
      * Start the worker thread
      */
     public synchronized void run()
     {
         handleClient();
     }

     /**
      * Handles the VodaPAMI request
      * @throws IOException
      */
    private void handleClient()
    {
        // Increase the current count of running workers
        synchronized (key)
        {
            instances++;
            XTTProperties.printDebug("VODAPAMIWorker("+getWorkerId()+"): New Client ("+s.getInetAddress().getHostAddress() +":"+ s.getPort()+") handled by " +id+" instances "+instances);
            key.notify();
        }
        try
        {
            in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));

            StringBuffer logLine = new StringBuffer();

            boolean doWhile=true;
            String buffer="";
            String response="";
            // do the loop
            while(doWhile&&s.isConnected()&&!stop)
            {
                try
                {
                    response="";
                    logLine = new StringBuffer();
                    buffer = in.readLine();
                    if(buffer == null)
                    {
                        break;
                    }
                    XTTProperties.printVerbose("VODAPAMIWorker("+getWorkerId()+"): Received: " + buffer);

                    if(buffer.lastIndexOf(';')!=(buffer.length()-1))
                    {
                        String sub = buffer.substring(buffer.lastIndexOf(';')+1,buffer.length());
                        System.err.println(">>Comment>>"+sub);
                        logLine.append(sub);
                        buffer = buffer.substring(0,buffer.lastIndexOf(';')+1);
                    }

                    String[] parts = buffer.split(",");
                    if(buffer.indexOf(';') == -1)
                    {
                        XTTProperties.printFail("VODAPAMIWorker("+getWorkerId()+"): Invalid message format, semicolon missing");
                        continue;
                    }
                    /*
                        Possible allowed commands:
                        MMS:ALLOWED,msisdn,originatorId,eventType,trigger,chargingNumber,[destinationIMIS],[destinationSubType],[vlrNumber],[sgsnAddress];
                        MMS:DEBIT,msisdn,originatorId,eventType,trigger,chargingNumber,[destinationIMIS],[destinationSubType],[vlrNumber],[sgsnAddress],auditText;
                    */

                    String messageType = parts[0];
                    if(messageType.equals("MMS:ALLOWED"))
                    {
                        int ALLOWEDPARTS = 17;
                        if(parts.length != ALLOWEDPARTS)
                        {
                            XTTProperties.printFail("VODAPAMIWorker("+getWorkerId()+"): Invalid message format, not "+ALLOWEDPARTS+" parts");
                            if(parts.length > ALLOWEDPARTS)
                                response = "C1:00006:" + codeFormat.format(parts.length - ALLOWEDPARTS) + ":Too many parameters;";
                            continue;
                        }
                        else
                        {
                            response = doAllowed(parts);
                        }
                    }
                    else if (messageType.equals("MMS:DEBIT"))
                    {
                        final int DEBITPARTS = 18;
                        if(parts.length != DEBITPARTS)
                        {
                            XTTProperties.printFail("VODAPAMIWorker("+getWorkerId()+"): Invalid message format, not "+DEBITPARTS+" parts");
                            if(parts.length > DEBITPARTS)
                                response = "C1:00006:" + codeFormat.format(parts.length - DEBITPARTS) + ":Too many parameters;";
                            continue;
                        }
                        else
                        {
                            response = doDebit(parts);
                        }
                    }
                    else if (messageType.startsWith("EXIT;"))
                    {
                        response = "C1:00000,00000, Session closed;";
                        logLine.append("<>" + response);
                        int temp = sendResponse(response);
                        if(temp != 0)
                        {
                            logLine.append(" delayed for " + temp + "sec");
                        }
                        System.out.println(logLine);
                        setStop();
                    }
                    else if (messageType.startsWith("INFO;"))
                    {

                        response = "C1:00000,00000," + version + ",pmsa01,"+instances+";";
                    }
                    else
                    {
                        XTTProperties.printFail("VODAPAMIWorker("+getWorkerId()+"): Received an unknown command");
                        response = "C1:00004:00001:Unknown command;";
                    }
                }
                catch (NullPointerException npe)
                {
                    if (XTTProperties.printDebug(null))
                    {
                        XTTProperties.printException(npe);
                    }
                    XTTProperties.printFail("VODAPAMIWorker("+getWorkerId()+"): Invalid message format");
                }
                catch (ArrayIndexOutOfBoundsException oobe)
                {
                    if (XTTProperties.printDebug(null))
                    {
                        XTTProperties.printException(oobe);
                    }
                    XTTProperties.printFail("VODAPAMIWorker("+getWorkerId()+"): Invalid message format");
                }
                finally
                {
                    if(response.equals(""))
                    {
                        response = "C1:00003:00001:Syntax violation";
                    }
                    logLine.append("<>" + response);
                    int temp = sendResponse(response);
                    if(temp != 0)
                    {
                        logLine.append(" delayed for " + temp + "sec");
                    }
                    System.out.println(logLine);
                }
            }
        }
        catch(Exception e)
        {
            //
        }
        XTTProperties.printDebug("VODAPAMIWorker("+getWorkerId()+"): Client disconnected");

        // Decrease the number of running instances
        synchronized (key)
        {
            instances--;
            key.notify();
        }
    }

    private String doAllowed(String[] message)
    {
        String response = "";
        try
        {
            String msisdn = message[1];
            String originatorId = message[2];
            int eventType = Integer.parseInt(message[3]);
            /*String trigger = message[4];
            String chargingNumber = message[5];
            String destinationIMSI = message[6];
            String destinationSubType = message[7];
            String vlrNumber = message[8];
            String sgsnAddress = message[9];*/

            if((eventType >= 0) && (eventType <= 255))
            {
                switch(eventType)
                {
                    case 250:
                        int retries = 1;
                        if(doRetry(allowRetries,msisdn,retries) != 0)
                        {
                            response = "C1:00002,00003,Insufficient Account Credit;";
                        }
                        else
                        {
                            response = "C1:00000,00000,"+msisdn+","+originatorId + ";";
                        }
                        break;
                    case 251: response = "C1:00002,00001,No such Subscriber;"; break;
                    case 252: response = "C1:00002,00002,Illegal Profile State;"; break;
                    case 253: response = "C1:00002,00003,Insufficient Account Credit;"; break;
                    case 254: response = "C1:00002,00004,Unknown event type;"; break;
                    case 247:
                        System.err.println("247 part");
                        retries = 0;
                        if(doRetry(allowRetries,msisdn,retries) != 0)
                        {
                            response = "C1:00002,00003,Insufficient Account Credit;";
                        }
                        else
                        {
                            response = "C1:00000,00000,"+msisdn+","+originatorId + ";";
                        }
                        doRetry(allowRetries,msisdn,2);
                        break;
                    default: response = "C1:00000,00000,"+msisdn+","+originatorId + ";"; break;
                }

                if(eventType == 0)
                {
                    //Don't try the rest of the ifs if you have a zero code
                }
                else if((msisdn.indexOf("555") <= 4)&&(msisdn.indexOf("555") >= 0))
                {
                    response = "C1:00002,00003,Insufficient Account Credit;";
                }
                else if((msisdn.indexOf("554") <= 4)&&(msisdn.indexOf("554") >= 0))
                {
                    int pos = msisdn.indexOf("554") + 3;
                    int retries = Integer.parseInt(""+msisdn.charAt(pos));
                    int left;
                    if((left = doRetry(allowRetries,msisdn,retries)) != 0)
                    {
                        left--;
                        XTTProperties.printInfo("VODAPAMIWorker("+getWorkerId()+"): Failing for " + left +" more tries");
                        response = "C1:00002,00003,Insufficient Account Credit;";
                    }
                }
            }
            else
            {
                XTTProperties.printFail("VODAPAMIWorker("+getWorkerId()+"): Bad event type");
                response = "C1:00002,00004,Unknown event type;";
            }
        }
        catch (Exception e)
        {
            if (XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
            XTTProperties.printFail("VODAPAMIWorker("+getWorkerId()+"): Invalid message format");
            response = "";
        }
        return response;
    }

    private String doDebit(String[] message)
    {
        String response = "";
        try
        {
            String msisdn = message[1];
            String originatorId = message[2];
            int eventType = Integer.parseInt(message[3]);
            /*String trigger = message[4];
            String chargingNumber = message[5];
            String destinationIMSI = message[6];
            String destinationSubType = message[7];
            String vlrNumber = message[8];
            String sgsnAddress = message[9];*/
            int retries = 0;

            if((eventType >= 0) && (eventType <= 255))
            {
                switch(eventType)
                {
                    //case 0: response = "C1:00002,00001,?????;"; break;
                    case 250:
                        retries = 1;
                        if(doRetry(debitRetries,msisdn,retries) != 0)
                        {
                            response = "C1:00002,00003,Insufficient Account Credit;";
                        }
                        else
                        {
                            response = "C1:00000,00000,"+msisdn+","+originatorId + ";";
                        }
                        break;
                    case 251: response = "C1:00002,00001,No such Subscriber;"; break;
                    case 252: response = "C1:00002,00002,Illegal Profile State;"; break;
                    case 253: response = "C1:00002,00003,Insufficient Account Credit;"; break;
                    case 254: response = "C1:00002,00004,Unknown event type;"; break;
                    case 255: response = "C1:00002,00005,Duplicate event ignored;"; break;
                    case 248: response = "C1:00002,00003,Insufficient Account Credit;"; break;
                    case 249:
                        retries = 1;
                        if(doRetry(debitRetries,msisdn,retries) != 0)
                        {
                            response = "C1:00002,00003,Insufficient Account Credit;";
                        }
                        else
                        {
                            response = "C1:00000,00000,"+msisdn+","+originatorId + ";";
                        }
                        break;
                    case 247:
                        response = "C1:00002,00003,Insufficient Account Credit;"; break;
                    default: response = "C1:00000,00000,"+msisdn+","+originatorId + ";"; break;
                }
                if(eventType == 0)
                {
                    //Don't try the rest of the ifs if you have a zero code
                }
                else if((msisdn.indexOf("555") <= 4)&&(msisdn.indexOf("555") >= 0))
                {
                    response = "C1:00002,00003,Insufficient Account Credit;";
                }
                else if((msisdn.indexOf("553") <= 4)&&(msisdn.indexOf("553") >= 0))
                {
                    int pos = msisdn.indexOf("553") + 3;
                    if(pos < msisdn.length())
                    {
                        retries = Integer.parseInt(""+msisdn.charAt(pos));
                        if(doRetry(debitRetries,msisdn,retries) != 0)
                        {
                            response = "C1:00002,00003,Insufficient Account Credit;";
                        }
                    }
                }

            }
            else
            {
                XTTProperties.printFail("VODAPAMIWorker("+getWorkerId()+"): Bad event type");
                response = "C1:00002,00004,Unknown event type;";
            }
        }
        catch (Exception e)
        {
            if (XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
            XTTProperties.printFail("VODAPAMIWorker("+getWorkerId()+"): Invalid message format");
            response = "";
            //continue;
        }
        return response;
    }


    private int sendResponse(String response) throws Exception
    {
        int delay = 0;
        if(parent.waterMark >= 0)
        {
            delay = rgen.nextInt(parent.extraWater);
            if(rgen.nextDouble() < (parent.percentBelow / 100d))
            {
                delay = parent.waterMark - delay;
            }
            else
            {
                delay = parent.waterMark + delay;
            }
            //System.err.println("Waiting: " + delay);
            try
            {
                if(delay > 0)
                {
                    Thread.sleep(delay);
                }
            }
            catch (Exception e)
            {
                XTTProperties.printFail("VODAPAMIWorker: exception while sleeping");
            }
        }

        try
        {
            if(s.isConnected())
            {
                out.write(response);
                out.newLine();
                out.flush();
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        return delay;
    }

    private int doRetry(HashMap<String,String> map,String number, int retries)
    {
        String value = (String)map.get(number);
        int intValue = -1;
        if (value == null)
        {
            intValue = retries;
            if(retries!=0)
            {
                map.put(number,""+retries);
            }
        }
        else
        {
            intValue = Integer.parseInt(value);
            intValue--;
            if(intValue != 0)
            {
                map.put(number,""+(intValue));
            }
            else
            {
                map.remove(number);
            }
        }
        return intValue;
    }

    public void setStop()
    {
        this.stop = true;
        try
        {
            XTTProperties.printDebug("VODAPAMIWorker: stop request for id: "+id+" -> closing socket");
            s.close();
        }
        catch(Exception e)
        {

        }
        synchronized(this)
        {
            notifyAll();
        }
    }
    public static final String tantau_sccsid = "@(#)$Id: VODAPAMIWorker.java,v 1.2 2006/07/21 17:04:29 cvsbuild Exp $";
}