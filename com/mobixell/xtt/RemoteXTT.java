package com.mobixell.xtt;

import java.net.ServerSocket;
import java.net.Socket;

import java.util.Vector;

import java.util.Calendar;
import java.util.GregorianCalendar;


import com.mobixell.xtt.hive.Drone;
import com.mobixell.xtt.hive.Hive;
import com.mobixell.xtt.util.OSUtils;

/**
 * RemoteXTT Runs the commands that interact directly with remote terminal
 *
 * @author      Gavin Cattell
 * @version     $Revision: 1.48 $
 */
public class RemoteXTT
{
    public static final int DEFAULTPORT = 8999;
    
    private int port = DEFAULTPORT; //Port to get commands on
    private String ipAddr = null; 
    
    private int workerId = 0;
    
    private Vector<String> allowedIpList = new Vector<String>(); //List of IPs allowed, any missing parts on the end are wild cards

    private static Calendar calendar = new GregorianCalendar();

    private Hive theHive;
    
    
    public RemoteXTT(String[]args)
    {
        loadCommandLineConfiguration(args);

        prepareRemote();
        prepareHive();

        listen();
    }

    private void listen()
    {
        ServerSocket servsock = null;
        Socket sock;
        try
        {
        	 servsock = new ServerSocket();
        	 servsock.setReuseAddress(true);   
        	 if (ipAddr=="null" || ipAddr==null) 
             {
             	ipAddr= OSUtils.getIpAddr();
             	servsock.bind(new java.net.InetSocketAddress(port));
             	
             }
             else
             {
            	 servsock.bind(new java.net.InetSocketAddress(ipAddr,port));
             }        	
            XTTProperties.setRemoteXTTPort(port);
            System.out.println("Ready; Listening on port " + port);
        }
        catch (java.io.IOException e)
        {
            System.out.println("Error getting listen socket: " + e);
            System.exit(1);
        }
        while (true)
        {
            try
            {
                sock=servsock.accept();
                workerId++;                
                String ip = sock.getInetAddress().getHostAddress();

                calendar.setTimeInMillis(System.currentTimeMillis());
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minute = calendar.get(Calendar.MINUTE);
                int second = calendar.get(Calendar.SECOND);
                int millisecond = calendar.get(Calendar.MILLISECOND);

                XTTProperties.printInfo("["+workerId+"]:" +"Connection at: " + ConvertLib.intToString(hour,2)+":"+ConvertLib.intToString(minute,2)+":"+ConvertLib.intToString(second,2)+"."+ConvertLib.intToString(millisecond,3));
                XTTProperties.printInfo("["+workerId+"]:" +sock);

                new RemoteXTTWorker(sock,ipAllowed(ip),workerId).start();
            }
            catch (java.io.IOException e)
            {
                System.out.println("Error getting connection: " + e);
            }
            catch (Exception e)
            {
                System.out.println("Error: " + e);
                e.printStackTrace();
            }
        }
    }

    public static void main(String[]args)
    {
        XTTProperties.setXTTBuildVersion();
        System.out.println("Version: " + FunctionModule.parseVersion(RemoteXTT.tantau_sccsid)+" Build: "+XTTProperties.getXTTBuildVersion());
        RemoteXTT app = new RemoteXTT(args);
    }

    private boolean ipAllowed(String ip)
    {
        for (int i=0;i<allowedIpList.size();i++)
        {
            if(ip.startsWith(allowedIpList.get(i)))
            {
                return true;
            }
        }
        return false;
    }

    public void addAllowedIp(String ip)
    {
        allowedIpList.add(ip);
    }

    private void prepareRemote()
    {
        if(XTTConfiguration.getNumberofPermanentLocalConfigurations() > 0)
        {
            org.jdom.Element[] allowedIPs = null;
            allowedIPs = XTTXML.getElements("whitelist/ip");
            allowedIpList.clear();
            if((allowedIPs != null)&&(allowedIPs.length != 0))
            {
                for (int i=0;i<allowedIPs.length;i++)
                {
                    try
                    {
                        addAllowedIp(allowedIPs[i].getText());
                        XTTProperties.printInfo("IP Added: " + allowedIPs[i].getText());
                    }
                    catch(NullPointerException npe)
                    {}
                }
            }
        }
    }

    private void prepareHive()
    {
        if(!XTTProperties.getQuietProperty("SYSTEM/HIVE/DISABLE").equals(""))
        {
            String hiveGroup = "230.20.7.24";
            theHive = new Hive(hiveGroup,9882,Drone.REMOTEDRONE);
            theHive.start();
        }
    }

    private void loadCommandLineConfiguration(String [] a)
    {
        boolean hasConfig=false;
        try
        {
            for (int i = 0; i<a.length ; i++) //Loop around the arguments
            {
                if (a[i].equalsIgnoreCase("--help")) //Check each argument
                {
                    showHelp();
                }
                else if ((a[i].equalsIgnoreCase("--config"))||(a[i].equalsIgnoreCase("-c")))
                {
                    if (!a[++i].endsWith(".xml"))
                    {
                        XTTProperties.printFail("Invalid file type, must be .XML");
                    }
                    else
                    {
                        XTTProperties.loadMainConfiguration(a[i]);
                        hasConfig=true;
                    }
                }
                else if ((a[i].equalsIgnoreCase("-i"))||(a[i].equalsIgnoreCase("--ip"))||(a[i].equalsIgnoreCase("-ip")))
                {
                    XTTProperties.printFail("--ip is deprecated please use -c with a config xml file like this:\n"
                        +"\n   <remoteconfiguration>"
                        +"\n       <!-- all machines thar are allowed to connect to RemoteXTT "
                        +"\n            either ip addresses or subnets -->"
                        +"\n       <whitelist>"
                        +"\n           <ip>127.0.0.1</ip>"
                        +"\n           <ip>172.20.</ip>"
                        +"\n       </whitelist>"
                        +"\n       <system>"
                        +"\n           <hive>"
                        +"\n               <!-- IMPORTANT! DO SET THIS TO CONTACT INFORMATION, "
                        +"\n                    A NAME OF WHO IS RUNNING THIS REMOTE XTT       -->"
                        +"\n               <comment>RemoteXTT - Unknown Person Responsible</comment>"
                        +"\n           </hive>"
                        +"\n           <!-- ip address of the system -->"
                        +"\n           <ip>"+java.net.InetAddress.getLocalHost().getHostAddress()+"</ip>"
                        +"\n       </system>"
                        +"\n   </remoteconfiguration>"
                        +"\n");
                        showHelp();
                    
                }
                else if ((a[i].equalsIgnoreCase("-li"))||(a[i].equalsIgnoreCase("--localip")))
                {
                    try
                    {
                        ipAddr = a[++i];
                    }
                    catch(NumberFormatException e)
                    {
                        XTTProperties.printFail("" + a[i] + " isn't a valid ip address, ip must be a valid string");
                    }
                    XTTProperties.printInfo("Setting listen port to: " + port);
                }
                else if ((a[i].equalsIgnoreCase("-p"))||(a[i].equalsIgnoreCase("--port")))
                {
                    try
                    {
                        port = Integer.parseInt(a[++i]);
                    }
                    catch(NumberFormatException e)
                    {
                        XTTProperties.printFail("" + a[i] + " isn't a valid number, port must be a valid integer");
                    }
                    XTTProperties.printInfo("Setting listen port to: " + port);
                }
                else
                {
                    showHelp();
                }
            }
        }
        catch (Exception e)
        {
            System.out.println("Error: Invalid command line option");
            e.printStackTrace();
            showHelp(); //If the command line options were bad, show the help
        }
        if(!hasConfig)
        {
            System.out.println("Error: please specify configuration");
            showHelp(); //If the command line options were bad, show the help
        }
    }

    /**
    *
    * print out the help
    */
    private void showHelp()
    {
        System.out.println("XTT Remote Test Tool (XTT)  - 724 Solutions");
        System.out.println("");
        System.out.println("");
        System.out.println("Usage:");
        System.out.println("   --help\t\tThis help");
        System.out.println("");
        System.out.println("   --config <file>\tLoads the Remote XTT configuration");
        System.out.println("   -c <file>\t\tLoads the Remote XTT configuration");
        System.out.println("");
        System.out.println("   --port <number>\tLoads Remote XTT on specified port, default="+DEFAULTPORT);
        System.out.println("   -p <number>\t\tLoads Remote XTT on specified port, default="+DEFAULTPORT);
        System.out.println("");

        System.exit(1); //Exit and fail if you had to show this help
    }

    public static final String tantau_sccsid = "@(#)$Id: RemoteXTT.java,v 1.48 2010/07/09 10:50:32 mlichtin Exp $";
}
