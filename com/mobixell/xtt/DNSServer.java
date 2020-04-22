package com.mobixell.xtt;

import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Vector;

import com.mobixell.xtt.util.OSUtils;

public class DNSServer extends Thread
{
    private static DatagramSocket socket = null;
    private int port = 53;
    private static String localIP=null;

    private static java.util.Set<String> excludeset=java.util.Collections.synchronizedSet(new java.util.HashSet<String>());
   
    public DNSServer(String ipAddr)
    {
        try
        {
        	  if (ipAddr=="null" || ipAddr==null) 
              {
        		  localIP = OSUtils.getIpAddr();
        		  socket = new DatagramSocket(port);
              }
              else
              {
            	  localIP=ipAddr;
            	  socket = new DatagramSocket((new java.net.InetSocketAddress(localIP, port)));
              }
        	
            XTTProperties.printInfo("DNSServer ip: " + localIP  + " listening on port " + port);
        }
        catch(java.net.BindException be)
        {
            if(port == 53)
            {
                XTTProperties.printFail("DNSServer: Port already in use.");
            }
            else
            {
                XTTProperties.printFail("DNSServer: Port("+port+") already in use.");
            }
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
        catch(Exception e)
        {
            XTTProperties.printException(e);
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }          
    }
    
    public static void addEntry(String domainName, String ip)
    {
        try
        {
            DNSEntry.setIpAddress(domainName,ip);
            XTTProperties.printInfo("DNSServer: addEntry: adding '" + domainName + "' -> " + ip);        
        }
        catch(UnknownHostException uhe)
        {
            XTTProperties.printInfo("DNSServer: addEntry: '" + ip + "' isn't a valid IP.");     
            XTTProperties.setTestStatus(XTTProperties.FAILED);    
        }
    }

    public static void removeEntry(String domainName)
    {
        DNSEntry.remove(domainName);
        XTTProperties.printInfo("DNSServer: removeEntry: removing '" + domainName + "'");
    }

    public static byte[] resolveAddress(Vector<String> qname) throws UnknownHostException
    {
        return DNSEntry.resolveAddressToIp4(qname);
    }

    public static byte[] resolveAddress(String hostname) throws UnknownHostException
    {
        //Turn the full hostname into a Vector of parts (better for doing lookups on)
        Vector<String> qname = new Vector<String>(java.util.Arrays.asList(hostname.split("\\056")));
        return resolveAddress(qname);
    }
    public static byte[] resolveIPv6Address(Vector<String> qname) throws UnknownHostException
    {
        return DNSEntry.resolveAddressToIp6(qname);
    }

    public static Boolean isIPv4Host(String hostname) throws UnknownHostException
    {
        return DNSEntry.isIp4Host(hostname);
    }
    public static Boolean isHostExist(String hostname) throws UnknownHostException
    {
    	hostname = hostname.toLowerCase();
        DNSEntry dnse = DNSEntry.dnsEntries.get(hostname);
       if (dnse==null) return false;
       else return true;
    }
    public static byte[] resolveIPv6Address(String hostname) throws UnknownHostException
    {
        //Turn the full hostname into a Vector of parts (better for doing lookups on)
        Vector<String> qname = new Vector<String>(java.util.Arrays.asList(hostname.split("\\056")));
        return resolveIPv6Address(qname);
    }
    public static InetAddress resolveAddressToInetAddress(String hostname) throws UnknownHostException
    {
        byte[] resolvedIp = resolveAddress(hostname);

        InetAddress inetAdd = null;
        inetAdd = InetAddress.getByAddress(resolvedIp);

        return inetAdd;
    }
    public static InetAddress resolveIPv6AddressToInetAddress(String hostname) throws UnknownHostException
    {
        byte[] resolvedIp = resolveIPv6Address(hostname);

        InetAddress inetAdd = null;
        inetAdd = InetAddress.getByAddress(resolvedIp);

        return inetAdd;
    }

    public static boolean checkSocket()
    {
        return (socket==null);
    }

    public void run()
    {
        byte[] buf = new byte[1024];
        try
        {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            while((true)&&(!checkSocket()))
            {
                socket.receive(packet);
                //System.out.println("Got something");
                new DNSWorker(packet,socket).start();
                buf = new byte[1024];
                packet = new DatagramPacket(buf, buf.length);
            }
        }
        catch(SocketException e)
        {
            //
        }
        catch(Exception e)
        {
            XTTProperties.printException(e);
        }
    }

    public void stopGracefully() throws java.lang.Exception
    {

        XTTProperties.printDebug("DNSServer: close socket");
        socket.close();
        this.join();

    }

    public static void main(String[] a)
    {
        String domain       ="";
        String ip           ="";
        String localip = null;
        String tracing      ="";

        XTTProperties.setTracing(XTTProperties.DEBUG);

        try
        {
            for (int i = 0; i<a.length ; i++) //Loop around the arguments
            {
                if (a[i].equalsIgnoreCase("--help")) //Check each argument
                {
                    showHelp();
                }
                else if ((a[i].equalsIgnoreCase("--tracing"))||(a[i].equalsIgnoreCase("-t")))
                {
                    tracing=a[++i];
                }
                else if ((a[i].equalsIgnoreCase("--entry"))||(a[i].equalsIgnoreCase("-e")))
                {
                    domain = a[++i].toLowerCase();
                    if(((i+1)<a.length)&&(!a[i+1].startsWith("-")))
                    {
                        ip=a[++i];
                    }
                    else
                    {
                        ip=InetAddress.getLocalHost().getHostAddress();
                    }
                    XTTProperties.printInfo("DNSServer: Adding Entry: " + domain + "=" + ip);
                    DNSEntry.setIpAddress(domain,ip);
                }
                else if (a[i].equalsIgnoreCase("--exclude"))
                {
                    addExcludedDomain(a[++i]);
                }
                else if ((a[i].equalsIgnoreCase("--li"))||(a[i].equalsIgnoreCase("-localip")))
                {
                	localip= a[++i];
                }
                else
                {
                    showHelp();
                }
            }
            if(tracing.equalsIgnoreCase(""))
            {
                XTTProperties.setTracing(XTTProperties.DEBUG);
            }
            else if(tracing.equalsIgnoreCase("f")||tracing.equalsIgnoreCase("fail"))
            {
                XTTProperties.setTracing(XTTProperties.FAIL);
            }
            else if(tracing.equalsIgnoreCase("w")||tracing.equalsIgnoreCase("warn"))
            {
                XTTProperties.setTracing(XTTProperties.WARN);
            }
            else if(tracing.equalsIgnoreCase("i")||tracing.equalsIgnoreCase("info"))
            {
                XTTProperties.setTracing(XTTProperties.INFO);
            }
            else if(tracing.equalsIgnoreCase("v")||tracing.equalsIgnoreCase("verbose"))
            {
                XTTProperties.setTracing(XTTProperties.VERBOSE);
            }
            else if(tracing.equalsIgnoreCase("d")||tracing.equalsIgnoreCase("debug"))
            {
                XTTProperties.setTracing(XTTProperties.DEBUG);
            }
            else
            {
                throw new Exception("Invalid option: "+tracing);
            }

            XTTProperties.printDebug("Version: DNSServer: "+FunctionModule.parseVersion(DNSServer.tantau_sccsid)+" DNSWorker: "+FunctionModule.parseVersion(DNSWorker.tantau_sccsid));
            DNSServer dnsServer = new DNSServer(localip);
            dnsServer.run();
        }
        catch(Exception e)
        {
            System.out.println("Error: Invalid command line option");
            XTTProperties.printException(e);
            showHelp(); //If the command line options were bad, show the help
        }
    }
    private static void showHelp()
    {
        System.out.println("eXtreme Test Tool (XTT) - DNS-Server Simulator - Mobixell");
        System.out.println("Version: DNSServer: "+FunctionModule.parseVersion(DNSServer.tantau_sccsid)+" DNSWorker: "+FunctionModule.parseVersion(DNSWorker.tantau_sccsid));
        System.out.println("");
        System.out.println("");
        System.out.println("Usage:");
        System.out.println("\t--help\n\t\tThis help");
        System.out.println("");
        System.out.println("\t-t <level>, --tracing <level>");
        System.out.println("\t\tSpecify the tracing level to use,\n\t\tsupported: fail, warn, info, verbose, debug");
        System.out.println("");
        System.out.println("\t-e <domain> [<IP>], --entry <domain> [<IP>]");
        System.out.println("\t\tAdd a DNS entry");
        System.out.println("");
        System.out.println("\t--exclude <domain>");
        System.out.println("\t\tExclude a domain from outputting info to the screen (the domain will still be handled normally)");
        System.out.println("");

        System.exit(1); //Exit and fail if you had to show this help
    }
    public static boolean notExcludedFromDebug(Vector<String> qname)
    {
        //XTTProperties.printDebug("DNSServer: Not excluded from Debug? "+name+" "+!excludeset.contains(name));

        String name ="";
        boolean found = false;

        for(int i=0;i<qname.size();i++)
        {
            name = "";
            for (int j=i;j<(qname.size()-1);j++)
            {
                name += qname.get(j) + ".";
            }
            name += qname.get(qname.size()-1);
            name = name.toLowerCase();

            found = excludeset.contains(name);
            if(found)
            {
                break;
            }
        }
        return !found;
    }
    public static void addExcludedDomain(String name)
    {
        XTTProperties.printDebug("DNSServer: Excluding from Debug: "+name);
        excludeset.add(name);
    }
    public static void resetExcludedDomains()
    {
        excludeset=java.util.Collections.synchronizedSet(new java.util.HashSet<String>());
    }
    public static final String tantau_sccsid = "@(#)$Id: DNSServer.java,v 1.10 2008/02/21 15:08:59 gcattell Exp $";
}