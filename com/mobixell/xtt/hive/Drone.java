package com.mobixell.xtt.hive;

import java.util.Vector;

public class Drone implements Comparable<Drone>
{
    public static final int XTTDRONE      = 0x01;
    public static final int REMOTEDRONE   = 0x02;
    public static final int UNKNOWNDRONE  = 0xF0;

    private String myIp = null;
    private int myPort = 0;
    private int myRemoteXTTPort = 0;
    private int myDroneType = UNKNOWNDRONE;
    private String myHostname = null;
    private String myVersion = null;
    private Vector<String> comments = new Vector<String>();
    private boolean inHive = true;
    private long uptime = -1;

    private java.util.GregorianCalendar lastSeen = new java.util.GregorianCalendar();

    public Drone()
    {

    }

    public void setIp(String ip)
    {
        myIp = ip;
    }

    public void setPort(int port)
    {
        myPort = port;
    }
    public void setRemoteXTTPort(int port)
    {
        myRemoteXTTPort = port;
    }

    public void setDroneType(int droneType)
    {
        myDroneType = droneType;
    }

    public void setHostname(String hostname)
    {
        myHostname = hostname;
    }
    public String getHostname()
    {
        return myHostname;
    }
    public java.util.GregorianCalendar getLastSeen()
    {
        return lastSeen;
    }
    
    public void setUptime(long currentRuntime)
    {
        uptime = currentRuntime;    
    }
    public long getUptime()
    {
        return uptime;
    }

    public void addComment(String comment)
    {
        comments.add(comment);
    }

    public String getIp()
    {
        return myIp;
    }
    public java.net.InetAddress getInetAddress()
    {
        java.net.InetAddress myInetAddress = null;
        try
        {
            myInetAddress = java.net.InetAddress.getByName(myIp);
        }
        catch(java.net.UnknownHostException uhe)
        {
            //This shouldn't happen
        }
        return myInetAddress;
    }

    public int getPort()
    {
        return myPort;
    }
    public int getRemoteXTTPort()
    {
        return myRemoteXTTPort;
    }

    public int getDroneType()
    {
        return myDroneType;
    }

    public String getComments()
    {
        StringBuffer combinedComments = new StringBuffer();
        if(comments.size() > 0)
        {
            combinedComments.append(comments.get(0));
            for(int i=1;i<comments.size();i++)
            {
                combinedComments.append("\n"+comments.get(i));
            }
        }
        return combinedComments.toString();
    }

    public void setVersion(String version)
    {
        myVersion = version;
    }
    public String getVersion()
    {
        return myVersion;
    }

    public boolean isInHive()
    {
        return inHive;
    }

    public void setIsInHive(boolean isInHive)
    {
        inHive = isInHive;
    }

    public boolean isComplete()
    {
        boolean complete=true;

        if(myIp==null) complete=false;
        if(myPort<=0) complete=false;
        if((myDroneType != XTTDRONE)&&(myDroneType != REMOTEDRONE)) complete=false;

        return complete;
    }

    public boolean equals(Object obj)
    {
        try
        {
            Drone comp = (Drone)obj;
            //XTTProperties.printDebug(toString() + " compared to " + comp.toString());
            return myIp.equals(comp.getIp());
        }
        catch(ClassCastException cce)
        {
            return false;
        }
    }

    public int hashCode()
    {
        if(myIp == null)
        {
            return new String("Not complete").hashCode();
        }
        return myIp.hashCode();
    }
    
    public int compareTo(Drone o)
    {
        if(myDroneType != o.getDroneType())
        {
            if(myDroneType == XTTDRONE)
                return -2;
            else if(myDroneType == REMOTEDRONE)
                return 2;
            //Unknown drones should NOT be in the list but just incase.
            else
                return 3;
        }
        else
        {
            int compareHostname = myHostname.compareTo(o.getHostname());
            if(compareHostname < 0)
                return -1;
            else if (compareHostname > 0)
                return 1;
            else
                return 0;
        }
    }

    public String toString()
    {
        if(isComplete())
        {
            String message = "";
            if(myDroneType==XTTDRONE)
            {
                message += "XTT Drone ";
            }
            else if(myDroneType==REMOTEDRONE)
            {
                message += "Remote XTT Drone ";
            }

            message += myIp + ":" + myPort;
            if(myHostname!=null) message += " (" + myHostname + ")";

            /*if(myVersion != null)
            {
                message += " v." + myVersion;
            }
            else
            {
                message += " v.???";
            }*/
            return message;
        }
        else
        {
            return "Drone isn\'t complete";
        }
    }
    
    public static final String tantau_sccsid = "@(#)$Id: Drone.java,v 1.10 2009/06/11 13:35:12 rsoder Exp $";
}