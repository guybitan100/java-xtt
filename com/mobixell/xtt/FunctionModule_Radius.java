package com.mobixell.xtt;

import java.net.Inet6Address;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import com.mobixell.xtt.radius.RadiusServer;
import com.mobixell.xtt.radius.RadiusWorker;
import com.theorem.radius3.AV;
import com.theorem.radius3.Attribute;
import com.theorem.radius3.AttributeList;
import com.theorem.radius3.AttributeName;
import com.theorem.radius3.PacketType;
import com.theorem.radius3.RADIUSClient;

/**
 * FunctionModule_Radius provides functions for Radius Logon and Radius Logoff.
 * It uses the radclient3.jar radius3 class api from theorem.com
 *
 * @author      Roger Soder
 * @version     $Id: FunctionModule_Radius.java,v 1.15 2010/05/05 08:12:37 rajesh Exp $
 * @see         <A HREF="http://www.axlradius.com/clientdocs/docs/index.html">Radius3</A>
 */
public class FunctionModule_Radius extends FunctionModule
{
    // Stores the RADIUSClient object. Is reused every time you use a RadiusLogon and RadiusLogoff
    private RADIUSClient radius=null;
    private String sessionId=null;
    private Vector<Attribute> additionalAttributes=null;
    private AttributeName attributeDictionary = new AttributeName(); //A dictrionary of names vs tags
    private boolean isIPv6 = false;
    /**
     * Re-initialize the internal variables. Sets only a new sessionId in this implementation..
     *
     */
    public FunctionModule_Radius()
    {
    }
    /**
     * Returns the Configuration Options as a String ready to copy/paste in a configuration file
     *
     */
    public String getConfigurationOptions()
    {
        return "        <!-- function module RADIUS -->"
            +"\n    <Radius>"
            +"\n        <!-- the radius server ip address -->"
            +"\n        <Ip>127.0.0.1</Ip>"
            +"\n        <!-- the radius server listening port -->"
            +"\n        <Port>1812</Port>"
            +"\n        <!-- the remote radius server listening port -->"
            +"\n        <RemotePort>1813</RemotePort>"
            +"\n        <!-- the radius server password -->"
            +"\n        <Secret>tttester</Secret>"
            +"\n        <!-- the radius server connection timeout -->"
            +"\n        <Timeout>30000</Timeout>"
            +"\n    </Radius>";
    }    
    public void printVersion()
    {
        super.printVersion();
        XTTProperties.printDebug(this.getClass().getName()+": RadiusServer: "+parseVersion(RadiusServer.tantau_sccsid));
        XTTProperties.printDebug(this.getClass().getName()+": RadiusWorker: "+parseVersion(RadiusWorker.tantau_sccsid));
    }
    public void showVersions()
    {
        super.showVersions();
        System.out.println(ConvertLib.createString(this.getClass().getName()+": RadiusServer: ",SHOWLENGTH) + parseVersion(RadiusServer.tantau_sccsid));
        System.out.println(ConvertLib.createString(this.getClass().getName()+": RadiusWorker: ",SHOWLENGTH) + parseVersion(RadiusWorker.tantau_sccsid));
    }

    /**
     * Constructor initializes the communitcation to the RadiusServer.
     * Prints out a warning if it can not create the RADIUSClient object.
     * Does not fail however because you ma not be using radius.
     */
    public void initialize()
    {
        additionalAttributes=null;
        RadiusWorker.clearAdditionalAttributes();
        sessionId="XTT_SendRadius " + new Date().toString();
        try
        {

            String ip = XTTProperties.getProperty("RADIUS/IP");
            int port = XTTProperties.getIntProperty("RADIUS/PORT");
            int remoteport = XTTProperties.getIntProperty("RADIUS/REMOTEPORT");
            if(remoteport==-1)remoteport=port;
            
            String secret = XTTProperties.getProperty("RADIUS/SECRET");
            int timeout = XTTProperties.getIntProperty("RADIUS/TIMEOUT");

            if((ip.equals("null"))||(remoteport == -1)||(secret.equals("null"))||(timeout == -1))
            {
                XTTProperties.printWarn(this.getClass().getName()+": radius wasn't started, no configuration found");
            }
            else
            {
                // Print out some Verbose level information
                XTTProperties.printVerbose(this.getClass().getName()+": setting radius host: "
                    + ip
                    +":"+ remoteport);
                // Create the RADIUSClient
                radius = new RADIUSClient(ip
                                         ,remoteport
                                         ,secret
                                         ,timeout);
                // Set if you want to se all the debug output on the RADIUSClient object
                // which we want in tracing level debug
                radius.setDebug(XTTProperties.printDebug((String)null));
                // Print out that we sucessfully initialized the redius object
                XTTProperties.printVerbose(this.getClass().getName()+": radius initialized");
            }
        } catch (SocketException se)
        {
            // In case we can not create the RADIUSClient object print only a warning
            // because we may not actually use the radius commands
            XTTProperties.printWarn(this.getClass().getName()+": unable to set radius configuration - check settings");
            if(XTTProperties.printDebug(null))
                XTTProperties.printException(se);
                //se.printStackTrace();
            radius=null;
            return;
        } catch (UnknownHostException uhe)
        {
            // In case we can not create the RADIUSClient object print only a warning
            // because we may not actually use the radius commands
            XTTProperties.printWarn(this.getClass().getName()+": unable to set radius configuration - check settings");
            if(XTTProperties.printDebug(null))
                XTTProperties.printException(uhe);
                //uhe.printStackTrace();
            radius=null;
            return;
        } catch (Exception ex)
        {
            // In case we can not create the RADIUSClient object print only a warning
            // because we may not actually use the radius commands
            XTTProperties.printWarn(this.getClass().getName()+": unable to set radius configuration - check settings");
            if(XTTProperties.printDebug(null))
                XTTProperties.printException(ex);
                //ex.printStackTrace();
            radius=null;
        }
        XTTProperties.printDebug(this.getClass().getName()+".initialize(): clearing variables");
    }

    /**
     * Clears the addition parameters.
     *
     */
    public int clearAdditionalAttributes(String parameters[])
    {
    	int status = XTTProperties.PASSED;
    	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": clearAdditionalAttributes:"+NO_ARGUMENTS);
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            additionalAttributes = null;
            XTTProperties.printInfo(parameters[0] + ": Clearing additional attributes");
        }
        return status;
    }
    
    /**
     * Clears Server addition parameters.
     *
     */
    public int clearAdditionalServerAttributes(String parameters[])
    {
    	int status = XTTProperties.PASSED;
    	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": clearAdditionalServerAttributes:"+NO_ARGUMENTS);
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
        	RadiusWorker.clearAdditionalAttributes();
            XTTProperties.printInfo(parameters[0] + ": Clearing Server additional attributes");
        }
        return status;
    }

    /**
     * Add additional attributes to send with radius.
     *
     */
    public int addAdditionalAttributes(String parameters[])
    {
    	int status = XTTProperties.PASSED;
    	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": addAdditionalAttributes: attributeTag");
            XTTProperties.printFail(this.getClass().getName()+": addAdditionalAttributes: attributeTag attributeValue");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length>3||parameters.length<2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": attributeTag");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": attributeTag attributeValue");
            status =XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            if(additionalAttributes==null)additionalAttributes=new Vector<Attribute>();
            int attributeTag = -1;
            String attributeName = null;
            try
            {
                attributeTag = Integer.parseInt(parameters[1]);
                attributeName = attributeDictionary.lookup(attributeTag);
            }
            catch(java.lang.NumberFormatException npe)
            {
                attributeName = parameters[1];
                attributeTag = attributeDictionary.lookup(attributeName);
                if(attributeTag == 0)
                {
                    XTTProperties.printFail(parameters[0] + ": " + parameters[1] + " isn't a number, or the name of an attribute");
                    
                    status =XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                    XTTProperties.setTestStatus(status);
                    return status;
                }
            }
            //XTTProperties.printDebug(parameters[0] + ": adding " + attributeName + "(" + attributeTag + ")")
            if(parameters.length>2)
            {
                //Attribute a = new Attribute();
                int dataType = Attribute.getDataType(attributeTag);

                switch (dataType)
                {
                    case Attribute.DATA_TYPE_STRING:
                        //XTTProperties.printVerbose(parameters[0] + ": supported: " + attributeTag + " is of type String");
                        XTTProperties.printInfo(parameters[0]+": setting Additional Attribute "+ attributeName + "(" + attributeTag + ")" +" to: " + parameters[2]);
                        additionalAttributes.add(new Attribute(attributeTag, ConvertLib.createBytes(parameters[2])));
                        break;

                    case Attribute.DATA_TYPE_INTEGER:
                        int num = 0;
                        try
                        {
                            num = Integer.parseInt(parameters[2]);
                        }
                        catch(java.lang.NumberFormatException nfe)
                        {
                            XTTProperties.printFail(parameters[0] + ": " + parameters[2] + " isn't a number");
                            status =XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                            XTTProperties.setTestStatus(status);
                            break;
                        }
                        XTTProperties.printInfo(parameters[0]+": setting Additional Attribute "+ attributeName + "(" + attributeTag + ")" +" to: " + num);
                        additionalAttributes.add(new Attribute(attributeTag, num));
                        break;

                    case Attribute.DATA_TYPE_OCTETS:
                        byte[] value = new byte[1];
                        //XTTProperties.setTestStatus(XTTProperties.FAILED);
                        try
                        {
                            XTTProperties.printDebug(parameters[2]);
                            XTTProperties.printInfo(parameters[0]+": setting Additional Attribute "+ attributeName + "(" + attributeTag + ")" +" to: " + parameters[2] + "(byte)");
                            additionalAttributes.add(new Attribute(attributeTag, ConvertLib.getByteArrayFromHexString(parameters[2])));
                        }
                        catch (java.lang.NumberFormatException nfe)
                        {
                            if(XTTProperties.printDebug(null))
                            {
                                XTTProperties.printException(nfe);
                            }
                            XTTProperties.printFail(parameters[0] + ": " + parameters[2] + " isn't an Octet (write in 0xFF format)");
                            status =XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                            XTTProperties.setTestStatus(status);
                            break;
                        }
                        //XTTProperties.printFail(parameters[0] + ": not supported: " + attributeTag + " is of type Octet");
                        break;

                    case Attribute.DATA_TYPE_IPADDRESS:
                        try
                        {
                            //TODO: need to modify for ipv6
                            String [] parts = parameters[2].split("\\056");
                            if  (( (parts.length != 4)
                                || ( (Integer.parseInt(parts[0]) < 0) || (Integer.parseInt(parts[0]) > 255) )
                                || ( (Integer.parseInt(parts[1]) < 0) || (Integer.parseInt(parts[1]) > 255) )
                                || ( (Integer.parseInt(parts[2]) < 0) || (Integer.parseInt(parts[2]) > 255) )
                                || ( (Integer.parseInt(parts[3]) < 0) || (Integer.parseInt(parts[3]) > 255) )
                            )   && !parameters[2].contains(":"))
                            {
                                XTTProperties.printFail(parameters[0] + ": " + parameters[2] + " isn't an IP");
                                status =XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                                XTTProperties.setTestStatus(status);
                                break;
                            }
                            else if(parameters[2].contains(":"))
                            {
                                byte[] ip = getIpv6bytes (parameters[2]);
                                
                            }
                            else
                            {
                                byte[] ip = new byte[4];
                                ip[0] = (byte) Integer.parseInt(parts[0]);
                                ip[1] = (byte) Integer.parseInt(parts[1]);
                                ip[2] = (byte) Integer.parseInt(parts[2]);
                                ip[3] = (byte) Integer.parseInt(parts[3]);

                                XTTProperties.printInfo(parameters[0]+": setting Additional Attribute "+attributeTag+" to: " + parameters[2]);
                                additionalAttributes.add(new Attribute(attributeTag, ip));
                            }
                        }
                        catch(NullPointerException npe)
                        {
                            XTTProperties.printFail(parameters[0] + ": " + parameters[2] + " isn't an IP");
                            status =XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                            XTTProperties.setTestStatus(status);
                            break;
                        }
                        catch(UnknownHostException uhe)
                        {
                            XTTProperties.printFail(parameters[0] + ": " + parameters[2] + " isn't valid IP");
                            status =XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                            XTTProperties.setTestStatus(status);
                            break;   
                        }
                        break;

                    case Attribute.DATA_TYPE_DATE:
                        XTTProperties.printFail(parameters[0] + ": not supported: " + attributeTag + " is of type Date");
                        status =XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                        XTTProperties.setTestStatus(status);
                        break;

                    case Attribute.DATA_TYPE_TUNNEL:
                        XTTProperties.printFail(parameters[0] + ": not supported: " + attributeTag + " is of type Tunnel");
                        status =XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                        XTTProperties.setTestStatus(status);
                        break;

                    default:
                        XTTProperties.printFail(parameters[0] + ": not supported: " + attributeTag + " is of unknown type");
                        status =XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                        XTTProperties.setTestStatus(status);
                        break;
                }
                //XTTProperties.printInfo(parameters[0]+": setting Additional Attribute "+attributeTag+" to: " + parameter);
                // Actually set the Header Key and Value
                //postData.put(attributeTag,);
            } else
            {
                XTTProperties.printInfo(parameters[0]+": removing Additional Attribute "+attributeTag);
                // Actually remove the Header Key and Value
                additionalAttributes.remove(attributeTag);
                if(additionalAttributes.isEmpty())
                {
                    additionalAttributes=null;
                }
            }
        }
        return status;
    }
    public int addAdditionalServerAttributes(String parameters[])
    {
    	int status = XTTProperties.PASSED;
    	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": addAdditionalServerAttributes: attributeTag attributeValue");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length>3||parameters.length<3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": attributeTag attributeValue");
            status =XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            if(additionalAttributes==null)additionalAttributes=new Vector<Attribute>();
            int attributeTag = -1;
            String attributeName = null;
            try
            {
                attributeTag = Integer.parseInt(parameters[1]);
                attributeName = attributeDictionary.lookup(attributeTag);
            }
            catch(java.lang.NumberFormatException npe)
            {
                attributeName = parameters[1];
                attributeTag = attributeDictionary.lookup(attributeName);
                if(attributeTag == 0)
                {
                    XTTProperties.printFail(parameters[0] + ": " + parameters[1] + " isn't a number, or the name of an attribute");
                    status =XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                    XTTProperties.setTestStatus(status);
                    return status;
                }
            }
            //XTTProperties.printDebug(parameters[0] + ": adding " + attributeName + "(" + attributeTag + ")")
            if(parameters.length>2)
            {
                //Attribute a = new Attribute();
                int dataType = Attribute.getDataType(attributeTag);

                switch (dataType)
                {
                    case Attribute.DATA_TYPE_STRING:
                        //XTTProperties.printVerbose(parameters[0] + ": supported: " + attributeTag + " is of type String");
                        XTTProperties.printInfo(parameters[0]+": setting Additional Attribute "+ attributeName + "(" + attributeTag + ")" +" to: " + parameters[2]);
                        RadiusWorker.addAdditionalAttributes((new Attribute(attributeTag, ConvertLib.createBytes(parameters[2]))).getAttribute());
                        break;

                    case Attribute.DATA_TYPE_INTEGER:
                        int num = 0;
                        try
                        {
                            num = Integer.parseInt(parameters[2]);
                        }
                        catch(java.lang.NumberFormatException nfe)
                        {
                            XTTProperties.printFail(parameters[0] + ": " + parameters[2] + " isn't a number");
                            status =XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                            XTTProperties.setTestStatus(status);
                            break;
                        }
                        XTTProperties.printInfo(parameters[0]+": setting Additional Attribute "+ attributeName + "(" + attributeTag + ")" +" to: " + num);
                        RadiusWorker.addAdditionalAttributes((new Attribute(attributeTag, num)).getAttribute());
                        break;

                    case Attribute.DATA_TYPE_OCTETS:
                        byte[] value = new byte[1];
                        //XTTProperties.setTestStatus(XTTProperties.FAILED);
                        try
                        {
                            XTTProperties.printDebug(parameters[2]);
                            XTTProperties.printInfo(parameters[0]+": setting Additional Attribute "+ attributeName + "(" + attributeTag + ")" +" to: " + parameters[2] + "(byte)");
                            RadiusWorker.addAdditionalAttributes((new Attribute(attributeTag, ConvertLib.getByteArrayFromHexString(parameters[2]))).getAttribute());
                        }
                        catch (java.lang.NumberFormatException nfe)
                        {
                            if(XTTProperties.printDebug(null))
                            {
                                XTTProperties.printException(nfe);
                            }
                            XTTProperties.printFail(parameters[0] + ": " + parameters[2] + " isn't an Octet (write in 0xFF format)");
                            status =XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                            XTTProperties.setTestStatus(status);
                            break;
                        }
                        //XTTProperties.printFail(parameters[0] + ": not supported: " + attributeTag + " is of type Octet");
                        break;

                    case Attribute.DATA_TYPE_IPADDRESS:
                        try
                        {   //TODO: need changes for ipv6
                            String [] parts = parameters[2].split("\\056");
                            if  (( (parts.length != 4)
                                || ( (Integer.parseInt(parts[0]) < 0) || (Integer.parseInt(parts[0]) > 255) )
                                || ( (Integer.parseInt(parts[1]) < 0) || (Integer.parseInt(parts[1]) > 255) )
                                || ( (Integer.parseInt(parts[2]) < 0) || (Integer.parseInt(parts[2]) > 255) )
                                || ( (Integer.parseInt(parts[3]) < 0) || (Integer.parseInt(parts[3]) > 255) )
                            )   && !parameters[2].contains(":"))
                            {
                                XTTProperties.printFail(parameters[0] + ": " + parameters[2] + " isn't an IP");
                                status =XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                                XTTProperties.setTestStatus(status);
                                break;
                            }
                            else if(parameters[2].contains(":"))
                            {  //TODO: ipv6
                                byte[] ip = getIpv6bytes (parameters[2]); 
                            }
                            else
                            {
                                byte[] ip = new byte[4];
                                ip[0] = (byte) Integer.parseInt(parts[0]);
                                ip[1] = (byte) Integer.parseInt(parts[1]);
                                ip[2] = (byte) Integer.parseInt(parts[2]);
                                ip[3] = (byte) Integer.parseInt(parts[3]);

                                XTTProperties.printInfo(parameters[0]+": setting Additional Attribute "+attributeTag+" to: " + parameters[2]);
                                RadiusWorker.addAdditionalAttributes((new Attribute(attributeTag, ip)).getAttribute());
                            }
                        } catch(NullPointerException npe)
                        {
                            XTTProperties.printFail(parameters[0] + ": NullPointerException: " + parameters[2] + " isn't an IP");
                            status =XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                            XTTProperties.setTestStatus(status);
                            break;
                        } catch(NumberFormatException nfe)
                        {
                            XTTProperties.printFail(parameters[0] + ": NumberFormatException: " + parameters[2] + " isn't an IP");
                            status =XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                            XTTProperties.setTestStatus(status);
                            break;
                        }
                        catch(UnknownHostException uhe)
                        {
                            XTTProperties.printFail(parameters[0] + ": UnknownHostException: " + parameters[2] + " isn't valid IP");
                            status =XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                            XTTProperties.setTestStatus(status);
                            break;
                        }
                        break;

                    case Attribute.DATA_TYPE_DATE:
                        XTTProperties.printFail(parameters[0] + ": not supported: " + attributeTag + " is of type Date");
                        status =XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                        XTTProperties.setTestStatus(status);
                        break;

                    case Attribute.DATA_TYPE_TUNNEL:
                        XTTProperties.printFail(parameters[0] + ": not supported: " + attributeTag + " is of type Tunnel");
                        status =XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                        XTTProperties.setTestStatus(status);
                        break;

                    default:
                        XTTProperties.printFail(parameters[0] + ": not supported: " + attributeTag + " is of unknown type");
                        status =XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                        XTTProperties.setTestStatus(status);
                        break;
                }
                //XTTProperties.printInfo(parameters[0]+": setting Additional Attribute "+attributeTag+" to: " + parameter);
                // Actually set the Header Key and Value
                //postData.put(attributeTag,);
            }
        }
        return status;
    }

    /**
     * creates a new sessionId for Radius
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code> argument and following are not used.
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int newSessionId(String parameters[])
    {
    	int status = XTTProperties.PASSED;
    	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": newSessionId:"+NO_ARGUMENTS);
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            status =XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            sessionId="XTT_SendRadius " + new Date().toString();
            XTTProperties.printInfo(parameters[0] + ": generated new session id: "+sessionId);
        }
        return status;
    }
    /**
     * does the Logon by sending the radius information over the
     * network to the RadiusServer.
     * <p>
     * Calls the private method doRadius which uses the RADIUSClient object of
     * the Radius3 package. If the RADIUSClient was not initialized the method fails.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code> argument is the userId, <code>parameters[2]</code> is the
     *                     user phone number and the optional <code>parameters[3]</code> is the users ipAddress.
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see         <A HREF="http://www.axlradius.com/clientdocs/docs/index.html">Radius3</A>
     */
    public int radiusLogon(String parameters[])
    {
        // Print the allowed parameters
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": radiusLogon: userId phoneNumber");
            XTTProperties.printFail(this.getClass().getName()+": radiusLogon: userId phoneNumber ipAddress");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        // Acct_Status_Type for start and stop accounting records
        // logon -> 1 ; logoff -> 2
       return doRadius(parameters,AV.Acct_Status_Type.Start);
    }

    /**
     * does the Logon by sending the radius information over the
     * network to the RadiusServer.
     *
     * @param parameters   array of String containing the parameters.
     * @see  #radiusLogon
     */
    public int radiusLogoff(String parameters[])
    {
        // Print the allowed parameters
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": radiusLogoff: userId phoneNumber");
            XTTProperties.printFail(this.getClass().getName()+": radiusLogoff: userId phoneNumber ipAddress");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        // Acct_Status_Type for start and stop accounting records
        // logon -> 1 ; logoff -> 2
       return doRadius(parameters,AV.Acct_Status_Type.Stop);
    }

    /**
     * does a Interim_Update by sending the radius information over the
     * network to the RadiusServer.
     *
     * @param parameters   array of String containing the parameters.
     * @see  #radiusLogon
     */
    public int radiusUpdate(String parameters[])
    {
        // Print the allowed parameters
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": radiusUpdate: userId phoneNumber");
            XTTProperties.printFail(this.getClass().getName()+": radiusUpdate: userId phoneNumber ipAddress");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        // Acct_Status_Type for start and stop accounting records
        // logon -> 1 ; logoff -> 2
        return doRadius(parameters,AV.Acct_Status_Type.Interim_Update);
    }

    public int radiusAuthenticate(String parameters[])
    {
    	int status = XTTProperties.PASSED;
    	
        // Print the allowed parameters
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": radiusAuthenticate: actionCode userId phoneNumber");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        int action=0;
        try
        {
            action = Integer.parseInt(parameters[1]);
        }
        catch(java.lang.NumberFormatException npe)
        {
            XTTProperties.printFail(parameters[0] + ": '" + parameters[1] + "' is NOT a number");
            status =XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        }
        // Acct_Status_Type for start and stop accounting records
        // logon -> 1 ; logoff -> 2
        String[] newparameters=new String[4];
        newparameters[0]=parameters[0];
        newparameters[1]=parameters[2];
        newparameters[2]=parameters[3];
        //TODO - RN - think you can use the IPaddress from RADIUS/IP config here?
		newparameters[3]=parameters[4];
        authenticateRadius(newparameters,action);
        return status;
    }

    public int radiusDo(String parameters[])
    {
    	int status = XTTProperties.PASSED;
    	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": radiusDo: actionCode userId phoneNumber");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        // If the parameters are not the parameters we expect abort here
        if(parameters.length!=4)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": actionCode userId phoneNumber");
            status = XTTProperties.FAILED;
            XTTProperties.setTestStatus(status);
            return status;
        }
        int action=0;
        try
        {
            action = Integer.parseInt(parameters[1]);
        }
        catch(java.lang.NumberFormatException npe)
        {
            XTTProperties.printFail(parameters[0] + ": '" + parameters[1] + "' is NOT a number");
            status =XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        }

        String[] newparameters=new String[3];
        newparameters[0]=parameters[0];
        newparameters[1]=parameters[2];
        newparameters[2]=parameters[3];
        return doRadius(newparameters, action, 0);
    }

    /**
     * does the Logon/Logoff by sending the radius information over the
     * network to the RadiusServer.
     *
     * @param parameters   array of String containing the parameters.
     * @param action       int logon -> 1 ; logoff -> 2
     * @see         <A HREF="http://www.axlradius.com/clientdocs/docs/index.html">Radius3</A>
     */
    private int doRadius(String parameters[],int action)
    {
        return doRadius(parameters, action, 0);
    }

    private int doRadius(String parameters[],int action, int retries)
    {
    	int status = XTTProperties.PASSED;
    	
        // If the RADIUSClient object has not been initialized we abort here
        if(radius==null)
        {
            XTTProperties.printFail(parameters[0] + ": radius not initialized");
            XTTProperties.setTestStatus(XTTProperties.FAILED_NO_ARGUMENTS);
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        // If the parameters are not the parameters we expect abort here
        if(parameters.length<3||parameters.length>4)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": userId phoneNumber");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": userId phoneNumber ipAddress");
            status =XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        }
        // Let's try sending the radius information
        try
        {
            // Store userid and phonenumber for better reading of the code
            String userId=parameters[1];
            String phoneNumber=parameters[2];
            // Create a byte array for the IP address which the AttributeList needs
            byte[] ipAsBytes=new byte[4];
            // get the IP as a string either from configuration or parameters
            String ipAsString=null;
            if(parameters.length==4)
            {
                ipAsString=parameters[3].trim();
            } else
            {
                ipAsString=XTTProperties.getProperty("SYSTEM/IP");
            }
            String[] ipStringParts=null;
            if(!ipAsString.equals(""))
            {
                // Split our ip address into it's 4 parts e.g. ipAddress 10.21.30.29
                
                if(ipAsString.contains(":"))
                {
                    //ipAsBytes=new byte[16];
                    isIPv6 = true;
                }
                else
                {
                     ipStringParts=ipAsString.split("\\.");  
                }
                // Split our ip address into it's n parts e.g. ipAddress af10:2bc1:aa30::1229
                // Let's try converting the ip address parts to bytes
                // do it this way so we get an exception if enaything goes wrong
                try
                {
                    //TODO : add logic here for IPv6 to convert hex value  into bytevalue
                    if(isIPv6) 
                    {
                    //    ourIP = Inet6Address.getByName(ipAsString);
                       ipAsBytes =  getIpv6bytes(ipAsString);
                    }
                    else
                    {
                        ipAsBytes[0]=(new Integer(ipStringParts[0])).byteValue();
                        ipAsBytes[1]=(new Integer(ipStringParts[1])).byteValue();
                        ipAsBytes[2]=(new Integer(ipStringParts[2])).byteValue();
                        ipAsBytes[3]=(new Integer(ipStringParts[3])).byteValue();    
                    }
                   
                } catch (Exception ex)
                {
                    // So it wasn't an ip address at all, print fail, fail the test and return
                    XTTProperties.printFail(parameters[0] + ": "+ipAsString+" is not a valid ipV4 address");
                    XTTProperties.printDebug(parameters[0] + ": "+ex.getClass().getName());
                    status =XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                    XTTProperties.setTestStatus(status);
                    return status;
                }
            }

            // create the attribute list which holds the parameters for the RADIUSClient object
            AttributeList attList = new AttributeList();
            //TODO : add new attribute to support IPv6 for radius client , Ref. rfc 3162
            // Set Attributes using for RADIUSClient object.
            // Set Attributes before accounting
            // action 1 is login, 2 is logoff
            attList.addAttribute(Attribute.Acct_Status_Type, action);
            attList.addAttribute(Attribute.Acct_Session_Id, sessionId);
            if(!ipAsString.equals(""))
            {
               if(!isIPv6)
                {
                    // Set the ipv4 Address as bytes
                    attList.addAttribute(Attribute.NAS_IP_Address, ipAsBytes);
                    // add Client IP address as bytes
                    attList.addAttribute (Attribute.Framed_IP_Address, ipAsBytes); 
              
                }
                else
                {
                    //http://www.juniper.net/techpubs/software/aaa_802/sbrc/sbrc70/sw-sbrc-admin/html/Concepts13.html
                    // Set the ipv6 Attributes as bytes e.g ip is 
                    // MUST : NAS_IPv6_Address = fe80::260:8ff:feff:ffff
                    // MUST : Framed_Interface_Id = 260:8ff:feff:ffff
                    // MAY  : Framed_IPv6_Prefix = fe80::260:8ff:feff:ffff/10
                    // MAY  : Login_IPv6_Host = fe80::260:8ff:feff:ffff
                    // Framed_IPv6_Pool = ipv6-pool
                    // Framed_IPv6_Route = 2000:0:0:106::/64 2000::106:a00:20ff:fe99:a998 1
                    attList.addAttribute(Attribute.NAS_IPv6_Address, ipAsBytes);
                   // attList.addAttribute(Attribute.Framed_Interface_Id, ipAsString.split("::")[1]);
                   /* attList.addAttribute(Attribute.Framed_IPv6_Prefix, ipAsBytes);
                    attList.addAttribute(Attribute.Login_IPv6_Host, ipAsBytes);
                    attList.addAttribute(Attribute.Framed_IPv6_Route, ipAsBytes);*/
                    //Framed_IPv6_Pool can ignore this
                   // attList.addAttribute(Attribute.Framed_IPv6_Pool, "ipv6-pool".getBytes());
                   
                }
            }
            // authenticate only
            attList.addAttribute(Attribute.Service_Type, 8);
            if(!userId.equals(""))
            {
                // Set the userId
                attList.addAttribute(Attribute.User_Name, userId);
            }
            // add Calling Station ID
            if(!phoneNumber.equals(""))
            {            
                attList.addAttribute (Attribute.Calling_Station_Id, phoneNumber);
            }

            if(additionalAttributes != null)
            {
                Iterator it=additionalAttributes.iterator();
                Attribute attribute = null;

                while(it.hasNext())
                {
                    attribute=(Attribute)it.next();
                    XTTProperties.printDebug(parameters[0]+ ": adding " + attribute.toString());
                    attList.addAttribute(attribute);
                }
            }

            // print the info
            XTTProperties.printInfo(parameters[0] + ": sending radius information for "+userId+" "+phoneNumber+" "+ipAsString);
            XTTProperties.printDebug(parameters[0] + ": radius debug info:");
            // reset the RADIUSClient object
            radius.reset();
            // TODO : here need to check wheather RADIUSClient handle IPv6 or not
            // Do the new radius
            int result = radius.accounting(attList);
            // If we don'g get the propper resonse fail
            if (result != PacketType.Accounting_Response)
            {
                if(radius.getError()==radius.ERROR_DUPLICATE)
                {
                    XTTProperties.printWarn(parameters[0] + ": sending radius duplicate response received: "+ new PacketType().getName(result)+" Error:"+radius.getError());
                } else
                {
                    XTTProperties.printFail(parameters[0] + ": sending radius failed, response received: "+ new PacketType().getName(result)+" Error:"+radius.getError());
                    status = XTTProperties.FAILED;
                    XTTProperties.setTestStatus(status);
                }
            }
            XTTProperties.printDebug(parameters[0] + ": sending radius END");
        } catch (Exception e)
        {
            if(retries > 0)
            {
                XTTProperties.printInfo(parameters[0] + ": retrying radius send " + retries + " more time(s)");
                return doRadius(parameters, action, --retries);
            }
            else
            {
                // Obviously something unexpected happend. print fail, fail test and exit
                XTTProperties.printFail(parameters[0] + ": sending radius failed: "+e.getClass().getName());
                XTTProperties.printFail(parameters[0] + ": check server configuration if client is allowed to send radius");
                if(XTTProperties.printDebug(null))
                    XTTProperties.printException(e);
                    //e.printStackTrace();
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }
    private int authenticateRadius(String parameters[],int action)
    {
    	int status = XTTProperties.PASSED;
    	
        // If the RADIUSClient object has not been initialized we abort here
        if(radius==null)
        {
            XTTProperties.printFail(parameters[0] + ": radius not initialized");
            status =XTTProperties.FAILED_NO_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        }
        // If the parameters are not the parameters we expect abort here
        if(parameters.length<3||parameters.length>4)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": userId phoneNumber");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": userId phoneNumber ipAddress");
            status =XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        }
        // Let's try sending the radius information
        try
        {
            // Store userid and phonenumber for better reading of the code
            String userId=parameters[1];
            String phoneNumber=parameters[2];
            
            // Create a byte array for the IP address which the AttributeList needs
            byte[] ipAsBytes=new byte[4];
            // get the IP as a string either from configuration or parameters
            String ipAsString=null;
            if(parameters.length==4)
            {
                ipAsString=parameters[3].trim();
            } else
            {
                ipAsString=XTTProperties.getProperty("SYSTEM/IP");
            }
            if(!ipAsString.equals(""))
            {
                // Split our ip address into it's 4 parts
                isIPv6 = false;
                String[] ipStringParts=null;
                if(ipAsString.contains(":"))
                {
                    isIPv6 = true;
                }
                else
                {
                     ipStringParts=ipAsString.split("\\.");  
                } 
                // Let's try converting the ip address parts to bytes
                // do it this way so we get an exception if enaything goes wrong
                try
                {
                    if(isIPv6) 
                    {
                        ipAsBytes =  getIpv6bytes(ipAsString);
                    }
                    else
                    {
                        ipAsBytes[0]=(new Integer(ipStringParts[0])).byteValue();
                        ipAsBytes[1]=(new Integer(ipStringParts[1])).byteValue();
                        ipAsBytes[2]=(new Integer(ipStringParts[2])).byteValue();
                        ipAsBytes[3]=(new Integer(ipStringParts[3])).byteValue();  
                    }
                } catch (Exception ex)
                {
                    // So it wasn't an ip address at all, print fail, fail the test and return
                    XTTProperties.printFail(parameters[0] + ": "+ipAsString+" is not a valid ipV4 address");
                    XTTProperties.printDebug(parameters[0] + ": "+ex.getClass().getName());
                    status =XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                    XTTProperties.setTestStatus(status);
                    return status;
                }
            }

            // create the attribute list which holds the parameters for the RADIUSClient object
            AttributeList attList = new AttributeList();

            // Set Attributes using for RADIUSClient object.
            // Set Attributes before accounting
            // action 1 is login, 2 is logoff
            //attList.addAttribute(Attribute.Acct_Status_Type, action);
            //attList.addAttribute(Attribute.Acct_Session_Id, sessionId);
            if(!ipAsString.equals(""))
            {
                if(!isIPv6)
                {
                    // Set the ipAddress as bytes
                    attList.addAttribute(Attribute.NAS_IP_Address, ipAsBytes); 
                    // add Client IP address as bytes
                    attList.addAttribute (Attribute.Framed_IP_Address, ipAsBytes);
                }
                else
                {
                    // Set the IPv6 ipAddress as bytes
                    attList.addAttribute(Attribute.NAS_IPv6_Address, ipAsBytes); //MUST
                   // attList.addAttribute(Attribute.Login_IPv6_Host, ipAsString); //optional
                   // attList.addAttribute(Attribute.Framed_IPv6_Pool, "ipv6-pool".getBytes());//optional
                }
                    
             
            }
            // authenticate only
            attList.addAttribute(Attribute.Service_Type, 8);
            if(!userId.equals(""))
            {
                // Set the userId
                attList.addAttribute(Attribute.User_Name, userId);
            }
            // add Calling Station ID
            if(!phoneNumber.equals(""))
            {            
                attList.addAttribute (Attribute.Calling_Station_Id, phoneNumber);
            }

            if(additionalAttributes != null)
            {
                Iterator it=additionalAttributes.iterator();
                Attribute attribute = null;

                while(it.hasNext())
                {
                    attribute=(Attribute)it.next();
                    XTTProperties.printDebug(parameters[0]+ ": adding " + attribute.toString());
                    attList.addAttribute(attribute);
                }
            }

            // print the info
            XTTProperties.printInfo(parameters[0] + ": sending radius information for "+userId+" "+phoneNumber+" "+ipAsString);
            XTTProperties.printDebug(parameters[0] + ": radius debug info:");
            // reset the RADIUSClient object
            radius.reset();
            //TODO: Here need to check wheather RADIUSCLIENT handle IPv6 or not
            // Do the new radius
            int result = radius.authenticate(attList);
            // If we don'g get the propper resonse fail
            if (result != PacketType.Access_Accept )
            {
                if(radius.getError()==radius.ERROR_DUPLICATE)
                {
                    XTTProperties.printWarn(parameters[0] + ": sending radius duplicate response received: "+ new PacketType().getName(result)+" Error:"+radius.getError());
                } else
                {
                    //System.out.println("Error String"+radius.getErrorString());
                    //System.out.println("Secret Password"+ConvertLib.createString(radius.getPassword()));
                    //System.out.println("Secret Password"+radius.getAttributes().getSize());
                    //for(int i = 0; i < radius.getAttributes().size(); i++){
                        //System.out.println(radius.getAttributes().getStringAttribute(i));
                        
                    //}
                    XTTProperties.printFail(parameters[0] + ": sending radius failed, response received: "+ new PacketType().getName(result)+" Error:"+radius.getError());
                    status =XTTProperties.FAILED;
                    XTTProperties.setTestStatus(status);
                }
            }
            XTTProperties.printDebug(parameters[0] + ": sending radius END");
        } catch (Exception e)
        {
                // Obviously something unexpected happend. print fail, fail test and exit
                XTTProperties.printFail(parameters[0] + ": sending radius failed: "+e.getClass().getName());
                XTTProperties.printFail(parameters[0] + ": check server configuration if client is allowed to send radius");
                if(XTTProperties.printDebug(null))
                    XTTProperties.printException(e);
                    //e.printStackTrace();
                status =XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            //}
        }
        return status;
    }

    public int startRadiusServer(String parameters[])
    {
    	int status = XTTProperties.PASSED;
    	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": startRadiusServer:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": startRadiusServer: port timeout secret");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }

        if(parameters.length==1)
        {
            try
            {
                XTTProperties.printVerbose(parameters[0] + ": Starting Radius Server");

                int port        = XTTProperties.getIntProperty("RADIUS/PORT");
                int timeout     = XTTProperties.getIntProperty("RADIUS/TIMEOUT");
                String secret   = XTTProperties.getProperty("RADIUS/SECRET");

                RadiusServer s = new RadiusServer(port,timeout,secret);
                s.start();
                XTTProperties.printDebug(parameters[0] + ": Started Radius Server");
                return status;
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0]+": "+e.getClass().getName()+": "+e.getMessage());
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                status =XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        } else if (parameters.length==4)
        {
            int i=1;
            try
            {
                XTTProperties.printVerbose(parameters[0] + ": Starting Radius Server");

                int port        = (Integer.decode(parameters[i])).intValue();
                int timeout     = (Integer.decode(parameters[++i])).intValue();
                String secret   = parameters[3];

                RadiusServer s = new RadiusServer(port,timeout,secret);
                s.start();
                XTTProperties.printDebug(parameters[0] + ": Started Radius Server");
                return status;
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR: '"+parameters[i]+"' is not a number");
                XTTProperties.setTestStatus(status);
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                status =XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        } else
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": port timeout secret");
            status =XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        }
        return status;
    }

    public int stopRadiusServer(String parameters[])
    {
    	int status = XTTProperties.PASSED;
    	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": stopRadiusServer:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": stopRadiusServer: port");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length==2)
        {
            try
            {
                XTTProperties.printInfo(parameters[0] + ": Stopping RadiusServer on port "+parameters[1]);
                RadiusServer.closeSocket(parameters[1]);
                return status;
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                status =XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        } else
        {
            XTTProperties.printWarn(parameters[0] + ": Stopping all RadiusServers");
            try
            {
                RadiusServer.closeSockets();
                return status;
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                status =XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
    
    private byte[] getIpv6bytes(String ipAsString) throws UnknownHostException
    {
        return  Inet6Address.getByName(ipAsString).getAddress();
    }
   
    /**
     * returns the getClass().getName() of this object. For debug reasons only.
     */
    public String toString()
    {
        return this.getClass().getName();
    }
    public static final String tantau_sccsid = "@(#)$Id: FunctionModule_Radius.java,v 1.15 2010/05/05 08:12:37 rajesh Exp $";
}