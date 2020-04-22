package com.mobixell.xtt;

import java.util.Vector;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.UnknownHostException;

public class DNSEntry
{
    public static java.util.Map<String,DNSEntry> dnsEntries = java.util.Collections.synchronizedMap(new java.util.HashMap<String,DNSEntry>());
    private String hostname = null;
    private byte[] ip4Address = new byte[0];
    private byte[] ip6Address = new byte[0];
    private Vector<ByteArrayWrapper> naptrResourceRecords = new Vector<ByteArrayWrapper>();
    /**
     * Constructs a new <code>DNSEntry</code> with the given host name.
     */
    public DNSEntry(String hostname)
    {
        hostname = hostname.toLowerCase();
        this.hostname = hostname;
    }
    public static boolean isIp4Host (String hostname) throws UnknownHostException
    {
    	hostname = hostname.toLowerCase();
        DNSEntry dnse = dnsEntries.get(hostname);
        if (dnse.ip4Address.length==4)
        	return true;
        else
        	return false;
    }
    /**
     * Takes a <code>Vector<String></code> containing the domain name parts, and tries to find an IPv4 match.
     * If no match is found in the internal DNS store, then <code>Inet4Address.getByName</code> is used.
     * @return the raw IPv4 address of the qname.
     * @throws UnknownHostException - if no IPv4 address for the host could be found.
     */
    public static byte[] resolveAddressToIp4(Vector<String> qname) throws UnknownHostException
    {
        byte[] rawIp = new byte[0];
        try
        {
            rawIp = getRawIp4Address(qname);
        }
        catch(UnknownHostException uhe)
        {
            //XTTProperties.printDebugException(uhe);
        }

        if(rawIp.length != 4)
        {
            rawIp = Inet4Address.getByName(ConvertLib.getStringFromVector(qname,".")).getAddress();
        }

        return rawIp;
    }

    /**
     * Determines the IPv4 address of a host, given the host's name.
     * The search only takes place within the local XTT entries.
     * @return the raw IPv4 address of the qname.
     * @throws UnknownHostException - if no IPv4 address for the host could be found in the internal entries.
     */
    public static byte[] getRawIp4Address(Vector<String> qname) throws UnknownHostException
    {
        StringBuffer hostname = new StringBuffer();
        //Take the full qname in lowercase.
        hostname.append(ConvertLib.getStringFromVector(qname,".").toLowerCase());
        byte[] rawIp = new byte[0];
        for(int i=0;i<qname.size();i++)
        {
            try
            {
                rawIp = dnsEntries.get(hostname.toString()).getRawIp4Address();
                if(rawIp.length == 4)
                {
                    break;
                }
            }
            catch(UnknownHostException uhe)
            {
                //XTTProperties.printDebugException(uhe);
            }
            catch(NullPointerException npe)
            {
                //XTTProperties.printDebugException(npe);
            }
            //Remove each part from most to least significant, if no match was found.
            hostname.delete(0,qname.get(i).length()+1);
        }

        //Check we found a host, then return it (or an exception).
        if(rawIp.length == 4)
        {
            return rawIp;
        }
        else
        {
            throw new UnknownHostException("Unknown host '" + hostname + "'");
        }
    }
    /**
     * Determines the IPv4 address of a host, given the host's name.
     * The search only takes place within the local XTT entries.
     * @return the raw IPv4 address of the hostname.
     * @throws UnknownHostException - if no IPv4 address for the host could be found in the internal entries.
     */
    public static byte[] getRawIp4Address(String hostname) throws UnknownHostException
    {
        try
        {
            hostname = hostname.toLowerCase();
            return dnsEntries.get(hostname).getRawIp4Address();
        }
        catch(NullPointerException npe)
        {
            throw new UnknownHostException("Unknown host '" + hostname + "'");
        }
    }
    /**
     * Returns the raw IPv4 address of this <code>DNSEntry</code> object.
     *
     * @return the raw IPv4 address of this object.
     * @throws UnknownHostException - if no IPv4 address for the host could be found in the internal entries.
     */
    public byte[] getRawIp4Address() throws UnknownHostException
    {
        if(ip4Address.length == 4)
        {
            return ip4Address;
        }
        else
        {
            throw new UnknownHostException("Unknown host '" + hostname + "'");
        }
    }

    /**
     * Takes a <code>Vector<String></code> containing the domain name parts, and tries to find an IPv6 match.
     * If no match is found in the internal DNS store, then <code>Inet4Address.getByName</code> is used.
     * @return the raw IPv6 address of the qname.
     * @throws UnknownHostException - if no IPv6 address for the host could be found.
     */
    public static byte[] resolveAddressToIp6(Vector<String> qname) throws UnknownHostException
    {
        byte[] rawIp = new byte[0];
        try
        {
            rawIp = getRawIp6Address(qname);
        }
        catch(UnknownHostException uhe)
        {
            //XTTProperties.printDebugException(uhe);
        }

        if(rawIp.length != 16)
        {
            rawIp = Inet6Address.getByName(ConvertLib.getStringFromVector(qname,".")).getAddress();
        }

        return rawIp;
    }

    /**
     * Determines the IPv6 address of a host, given the host's name.
     * The search only takes place within the local XTT entries.
     * @return the raw IPv6 address of the qname.
     * @throws UnknownHostException - if no IPv6 address for the host could be found in the internal entries.
     */
    public static byte[] getRawIp6Address(Vector<String> qname) throws UnknownHostException
    {
        StringBuffer hostname = new StringBuffer();
        //Take the full qname in lowercase.
        hostname.append(ConvertLib.getStringFromVector(qname,".").toLowerCase());
        byte[] rawIp = new byte[0];
        for(int i=0;i<qname.size();i++)
        {
            try
            {
                rawIp = dnsEntries.get(hostname.toString()).getRawIp6Address();
                if(rawIp.length == 16)
                {
                    break;
                }
            }
            catch(UnknownHostException uhe)
            {
                //XTTProperties.printDebugException(uhe);
            }
            catch(NullPointerException npe)
            {
                //XTTProperties.printDebugException(npe);
            }
            //Remove each part from most to least significant, if no match was found.
            hostname.delete(0,qname.get(i).length()+1);
        }

        //Check we found a host, then return it (or an exception).
        if(rawIp.length == 16)
        {
            return rawIp;
        }
        else
        {
            throw new UnknownHostException("Unknown host '" + hostname + "'");
        }
    }
    /**
     * Determines the IPv6 address of a host, given the host's name.
     * The search only takes place within the local XTT entries.
     * @return the raw IPv6 address of the hostname.
     * @throws UnknownHostException - if no IPv6 address for the host could be found in the internal entries.
     */
    public static byte[] getRawIp6Address(String hostname) throws UnknownHostException
    {
        try
        {
            hostname = hostname.toLowerCase();
            return dnsEntries.get(hostname).getRawIp6Address();
        }
        catch(NullPointerException npe)
        {
            throw new UnknownHostException("Unknown host '" + hostname + "'");
        }
    }
    /**
     * Returns the raw IPv6 address of this <code>DNSEntry</code> object.
     *
     * @return the raw IPv6 address of this object.
     * @throws UnknownHostException - if no IPv6 address for the host could be found in the internal entries.
     */
    public byte[] getRawIp6Address() throws UnknownHostException
    {
        if(ip6Address.length == 16)
        {
            return ip6Address;
        }
        else
        {
            throw new UnknownHostException("Unknown host '" + hostname + "'");
        }
    }

    public static Vector<ByteArrayWrapper> getNaptr(Vector<String> qname) throws UnknownHostException
    {
        StringBuffer hostname = new StringBuffer();
        //Take the full qname in lowercase.
        hostname.append(ConvertLib.getStringFromVector(qname,".").toLowerCase());
        Vector<ByteArrayWrapper> foundNaptrResourceRecords = null;
        for(int i=0;i<qname.size();i++)
        {
            try
            {
                foundNaptrResourceRecords = dnsEntries.get(hostname.toString()).getNaptr();
                if(foundNaptrResourceRecords != null && foundNaptrResourceRecords.size() > 0)
                {
                    break;
                }
            }
            catch(UnknownHostException uhe)
            {
                //XTTProperties.printDebugException(uhe);
            }
            catch(NullPointerException npe)
            {
                //XTTProperties.printDebugException(npe);
            }
            //Remove each part from most to least significant, if no match was found.
            hostname.delete(0,qname.get(i).length()+1);
        }

        //Check we found a host, then return it (or an exception).
        if(foundNaptrResourceRecords != null && foundNaptrResourceRecords.size() > 0)
        {
            return foundNaptrResourceRecords;
        }
        else
        {
            throw new UnknownHostException("Unknown host '" + hostname + "'");
        }
    }
    /**
     * Returns the NAPTR resource of this <code>DNSEntry</code> object.
     *
     * @return the NAPTR resource of this object.
     * @throws UnknownHostException - if no IPv6 address for the host could be found in the internal entries.
     */
    public Vector<ByteArrayWrapper> getNaptr() throws UnknownHostException
    {
        if(naptrResourceRecords != null && naptrResourceRecords.size() > 0)
        {
            return naptrResourceRecords;
        }
        else
        {
            throw new UnknownHostException("Unknown host '" + hostname + "'");
        }
    }

    public static void setIpAddress(String hostname, String stringIp) throws UnknownHostException
    {
        hostname = hostname.toLowerCase();
        DNSEntry entry = dnsEntries.get(hostname);
        if(entry == null)
        {
            entry = new DNSEntry(hostname);
            dnsEntries.put(hostname, entry);

        }
        entry.setIpAddress(stringIp);
    }
    public void setIpAddress(String stringIp) throws UnknownHostException
    {
        byte[] rawIp = InetAddress.getByName(stringIp).getAddress();
        if(rawIp.length == 4)
        {
            setIp4Address(rawIp);
        }
        else if(rawIp.length == 16)
        {
            setIp6Address(rawIp);
        }
        else
        {
            throw new UnknownHostException("'" + stringIp + "' is not a valid IP address.");
        }
    }

    public static void setIp4Address(String hostname, String stringIp) throws UnknownHostException
    {
        hostname = hostname.toLowerCase();
        DNSEntry entry = dnsEntries.get(hostname);
        if(entry == null)
        {
            entry = new DNSEntry(hostname);
            dnsEntries.put(hostname, entry);
        }
        entry.setIp4Address(stringIp);
    }
    public void setIp4Address(String stringIp) throws UnknownHostException
    {
        byte[] rawIp = Inet4Address.getByName(stringIp).getAddress();
        if(rawIp.length == 4)
        {
            setIp4Address(rawIp);
        }
        else
        {
            throw new UnknownHostException("'" + stringIp + "' is not a valid IPv4 address.");
        }
    }
    public void setIp4Address(byte[] rawIp) throws UnknownHostException
    {
        InetAddress inetAddress = Inet4Address.getByAddress(rawIp);
        ip4Address = rawIp;
        XTTProperties.printDebug("DNS: Adding IPv4 entry for '" + hostname + "' to '" + inetAddress.getHostAddress() + "'.");
    }

    public static void setIp6Address(String hostname, String stringIp) throws UnknownHostException
    {
        hostname = hostname.toLowerCase();
        DNSEntry entry = dnsEntries.get(hostname);
        if(entry == null)
        {
            entry = new DNSEntry(hostname);
            dnsEntries.put(hostname, entry);
        }
        entry.setIp6Address(stringIp);
    }
    public void setIp6Address(String stringIp) throws UnknownHostException
    {
        byte[] rawIp = Inet6Address.getByName(stringIp).getAddress();
        if(rawIp.length == 16)
        {
            setIp4Address(rawIp);
        }
        else
        {
            throw new UnknownHostException("'" + stringIp + "' is not a valid IPv6 address.");
        }
    }
    public void setIp6Address(byte[] rawIp) throws UnknownHostException
    {
        InetAddress inetAddress = Inet6Address.getByAddress(rawIp);
        ip6Address = rawIp;
        XTTProperties.printDebug("DNS: Adding IPv6 entry for '" + hostname + "' to '" + inetAddress.getHostAddress() + "'.");
    }

    public static void setNaptr(String hostname, int order, int preference, String flags, String service, String regexp, String replacement)
    {
        hostname = hostname.toLowerCase();
        DNSEntry entry = dnsEntries.get(hostname);
        if(entry == null)
        {
            entry = new DNSEntry(hostname);
            dnsEntries.put(hostname, entry);

        }
        entry.setNaptr(order, preference, flags, service, regexp, replacement);
    }
    public void setNaptr(int order, int preference, String flags, String service, String regexp, String replacement)
    {
        if(order < 1 || order > 65535)
        {
            order = 1;
        }
        if(preference < 1 || order > 65535)
        {
            preference = 1;
        }
        if(flags == null || flags.equals("")) flags="u";
        if(regexp == null || regexp.equals("")) regexp ="";
        if(replacement == null || replacement.equals("")) replacement =".";
        if(service == null || service.equals("")) service = "invalid";
        XTTProperties.printDebug("DNS: Adding NAPTR entry '" + order + " " + preference + " " + flags + " "  + service + " " + regexp + " " + replacement + "' for '" + hostname + "'");

        byte[] naptr = new byte[2 + 2 + 1 + ConvertLib.createBytes(flags).length + 1 + ConvertLib.createBytes(service).length + 1 + ConvertLib.createBytes(regexp).length + createDomainNameFromString(replacement).length];
        int start = 0;
        start = ConvertLib.addBytesToArray(naptr,start,ConvertLib.getByteArrayFromInt(order,2));
        start = ConvertLib.addBytesToArray(naptr,start,ConvertLib.getByteArrayFromInt(preference,2));
        start = ConvertLib.addBytesToArray(naptr,start,ConvertLib.getByteArrayFromInt(ConvertLib.createBytes(flags).length,1));
        start = ConvertLib.addBytesToArray(naptr,start,ConvertLib.createBytes(flags));
        start = ConvertLib.addBytesToArray(naptr,start,ConvertLib.getByteArrayFromInt(ConvertLib.createBytes(service).length,1));
        start = ConvertLib.addBytesToArray(naptr,start,ConvertLib.createBytes(service));
        start = ConvertLib.addBytesToArray(naptr,start,ConvertLib.getByteArrayFromInt(ConvertLib.createBytes(regexp).length,1));
        start = ConvertLib.addBytesToArray(naptr,start,ConvertLib.createBytes(regexp));
        //Don't support replacement yet (need to implement a parse for domains, i.e. split on '.' then add the parts with an octet lable
        start = ConvertLib.addBytesToArray(naptr,start,createDomainNameFromString(replacement));

        naptrResourceRecords.add(new ByteArrayWrapper(naptr));
    }

    public static void remove(String hostname)
    {
        dnsEntries.remove(hostname);
    }

    private static byte[] createDomainNameFromString(String domain)
    {
        if(domain == null || domain.equals("")) return ConvertLib.getByteArrayFromInt(0,1);

        String[] parts = domain.split("\\.");
        int length = 1;
        for(String part:parts)
        {
            length += ConvertLib.createBytes(part).length;
            length ++;
        }
        byte[] byteDomainName = new byte[length];
        int pointer = 0;
        for(String part:parts)
        {
            pointer = ConvertLib.addBytesToArray(byteDomainName,pointer,ConvertLib.getByteArrayFromInt(ConvertLib.createBytes(part).length,1));
            pointer = ConvertLib.addBytesToArray(byteDomainName,pointer,ConvertLib.createBytes(part));
        }
        ConvertLib.addBytesToArray(byteDomainName,pointer,ConvertLib.getByteArrayFromInt(0,1));
        return byteDomainName;
    }

    public static final String tantau_sccsid = "@(#)$Id: DNSEntry.java,v 1.3 2008/11/24 15:37:36 gcattell Exp $";
}

