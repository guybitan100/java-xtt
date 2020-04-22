package com.mobixell.xtt;

import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Vector;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * FunctionModule_Push.
 *
 * @author      Gavin Cattell
 * @version     $Revision: 1.19 $
 */
public class FunctionModule_Push extends FunctionModule
{
    //private final static String CRLF     = "\r\n";
    public static final String tantau_sccsid = "@(#)$Id: FunctionModule_Push.java,v 1.19 2010/05/05 08:12:37 rajesh Exp $";
    private LinkedHashMap<String,String> sendHeader           = new LinkedHashMap<String,String>();
    private static LinkedHashMap<String,Vector<String>> receiveHeader = new LinkedHashMap<String,Vector<String>>();
    private static byte[] serverResponse = null;
    private static String PIResponse = null;
    private String serverResponseCode[]  = new String[]{"","not yet initialized"};
    //private String ResponseCode  = new String[]{"","not yet initialized"};
    private String postData             = null;

    private HTTPConnection defaultConnection = null;


    public static byte[] getBody()
    {
        return serverResponse;
    }

    public static String getPIResponse()
    {
        return PIResponse;
    }

    public void viewBody(String parameters[])
    {
        System.out.println(ConvertLib.createString(getBody()));
    }

    public static LinkedHashMap<String,Vector<String>> getHeader()
    {
        return receiveHeader;
    }

    /**
     * remove all the headers that are to be sent from the client to the server.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name, no additional parameters are requeired.
     *                     <br><code>parameters[1]</code> connection name (or not present for default),
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void clearHeader(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": clearHeader:"+NO_ARGUMENTS);
            return;
        }
        if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            XTTProperties.printInfo(parameters[0] + ": clearing header");
            sendHeader=new LinkedHashMap<String,String>();
        }
    }
    
    /**
     * set the http headers to be sent from the client to the server.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> connection name (or not present for default),
     *                     <br><code>parameters[2]</code> argument is the header key,
     *                     <br><code>parameters[3]</code> argument is the header value or not present removing.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void setHeader(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setHeader: headerFieldKey headerFieldValue");
            return;
        }
        HTTPHelper.setHeader(this.sendHeader,parameters);
    }

    /**
     * clear the post data meant to be sent to the server.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name, no additional arguments requeired.
     *                     <br><code>parameters[1]</code> connection name (or not present for default),
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void clearPostData(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": clearPostData:"+NO_ARGUMENTS);
            return;
        }
        if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            XTTProperties.printInfo(parameters[0] + ": clearing POST data");
            postData=null;
        }
    }
    
    /**
     * set the post data to be sent from the client to the server on a post request.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> connection name (or not present for default),
     *                     <br><code>parameters[2]</code> argument is the post field name,
     *                     <br><code>parameters[3]</code> argument is the post field value or not present for removing.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void setPostData(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setPostData: postData");
            return;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": postData");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            XTTProperties.printDebug(parameters[0]+": setting POST data to: "+parameters[1]);
            // Actually set the Header Key and Value
            postData=parameters[1];
            
            String multipartName = null;
            try
            {
                java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("--(.*)--").matcher(postData);
                if(matcher.find())
                {
                    multipartName = matcher.group(1);
                }
            }
            catch(Exception e)
            {
                //    
            }
            
            if((multipartName != null)&&(!multipartName.equals("multipart-boundary")))
            {
                XTTProperties.printVerbose(parameters[0] + ": Multipart '"+multipartName+"' is being renamed to 'multipart-boundary'");      
                postData = postData.replaceAll(multipartName,"multipart-boundary");    
            }
            
            if(multipartName != null)
            {
                sendHeader.put("Content-Type","multipart/related; boundary=multipart-boundary; type=\"application/xml\"");    
            }
            else
            {
                XTTProperties.printVerbose(parameters[0] + ": No multipart found in body, sending as application/xml");
                sendHeader.put("Content-Type","application/xml");        
            }
        }
    }

    /**
     * replace the attribute with new value
     * 
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> attribute name,
     *                     <br><code>parameters[2]</code> new attribute value,
     */
    public void replaceAttribute(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": replaceAttribute: attribute value");
            return;
        }
        if(parameters.length!=3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": attribute value");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            if(postData.indexOf(parameters[1] + "=\"") != -1)
            {
                postData = postData.replaceAll("("+parameters[1]+"=\")(.*?)(\")",parameters[1]+"=\"" + parameters[2] + "\"");
            }
            else
            {
                XTTProperties.printFail(parameters[0] + ": No such attribute to replace: '" + parameters[1] + "'");
                XTTProperties.setTestStatus(XTTProperties.FAILED);    
            }
            //System.out.println(postData);
        }
    }

    /**
     * 
     * @param parameters
     */
    public void sendPushRequest(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendPushRequest:"+NO_ARGUMENTS);
            return;
        } else if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            HTTPConnection connection=defaultConnection;
            try
            {
                String pushIP=XTTProperties.getProperty("PUSH/PUSHIP","PUSHINITIATOR/PUSHIP","XMG/IP");

                if((pushIP.equals("null"))||(pushIP.equals("")))
                {
                    XTTProperties.printFail(parameters[0] + ": Missing configuration value PUSH/PUSHIP");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_CONFIG_VALUE);
                    return;
                }
                
                int pushPort = XTTProperties.getIntProperty("PUSH/PUSHPORT","PUSHINITIATOR/PUSHPORT","XMG/PUSHPORT");
                if(pushPort<=0)
                {
                    XTTProperties.printFail(parameters[0] + ": Missing configuration value PUSH/PUSHPORT");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_CONFIG_VALUE);
                    return;
                }

                String pushDirectory=XTTProperties.getProperty("PUSH/PUSHDIRECTORY","PUSHINITIATOR/PUSHDIRECTORY");
                if((pushIP.equals("null"))||(pushIP.equals("")))
                {
                    pushDirectory="push/PushReq";
                }

                XTTProperties.printInfo(parameters[0]+": sending Push Request to " + pushIP + ":" + pushPort+"/"+pushDirectory);

                URL url = new URL("http",pushIP,pushPort,"/"+pushDirectory);
                
                connection.getRequestHeader().clear();

                Iterator it=sendHeader.keySet().iterator();
                String key;
                while(it.hasNext())
                {
                    key=(String)it.next();
                    connection.getRequestHeader().put(key, (String)sendHeader.get(key));
                }

                XTTProperties.printDebug(parameters[0] + ": POST data line:\n"+postData);
                connection.setPostDataBytes(ConvertLib.createBytes(postData));

                connection.sendPostRequest(parameters[0],url.toExternalForm());

                receiveHeader=connection.getResponseHeader();
                serverResponseCode=new String[]{connection.getResponseCode()+"",connection.getResponseMessage()};
                serverResponse=connection.getResponse();

            } catch (MalformedURLException mux)
            {
                XTTProperties.printFail(parameters[0] + ": MalformedURLException");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return;
            } catch (Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": "+e.getClass().getName()+": "+e.getMessage());
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
    }

    /**
     * compare the http response code of the last POST/GET request with a value.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> connection name (or not present for default),
     *                     <br><code>parameters[2]</code> and following are the allowed response codes.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     *
     */
    public void checkResponseCode(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkResponseCode: expectedValue1 expectedvalue2 ...");
            return;
        }
        if(parameters.length<2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": expectedValue1 expectedvalue2 ...");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            boolean found=false;
            StringBuffer checked=new StringBuffer();
            String divider="";
            for(int i=1;i<parameters.length;i++)
            {
                if(serverResponseCode[0].equals(parameters[i]))
                {
                    found=true;
                }
                checked.append(divider+parameters[i]);
                divider=",";
            }

            if(found)
            {
                XTTProperties.printInfo(parameters[0] + ": found "+serverResponseCode[0]+" "+serverResponseCode[1]);
            } else
            {
                XTTProperties.printFail(parameters[0] + ": found "+serverResponseCode[0]+" expected "+checked.toString());
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
    }

    public void checkPAPCode(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkPAPCode: expectedValue");
            return;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": expectedValue");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            if((getBody() != null)&&(getBody().length != 0))
            {
                Pattern pattern = Pattern.compile("(response-result code=\")(.*)(\" desc)");
                Matcher matcher = pattern.matcher(ConvertLib.createString(getBody()));
                if(matcher.find())
                {
                    if(matcher.group(2).equals(parameters[1]))
                    {
                        XTTProperties.printInfo(parameters[0] + ": Correct Code: " + parameters[1]);
                    }
                    else
                    {
                        XTTProperties.printFail(parameters[0]+": found "+ matcher.group(2) + " expected " + parameters[1]);
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                    }
                }
                else
                {
                    XTTProperties.printFail(parameters[0]+": No matching pattern found");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                }
            }
            else
            {
                XTTProperties.printFail(parameters[0]+": Nothing to check");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
    }

    public String getConfigurationOptions()
    {
        return "    <!-- function module PUSH -->"
        +"\n    <Push>"
        +"\n        <PushIP>127.0.0.1</PushIP>"
        +"\n        <PushPort>1081</PushPort>"
        +"\n        <!--PushDirectory>/push/PushReq</PushDirectory-->"
        +"\n        <!-- Push Initiator listening port of FunctionModule_Push -->"
        +"\n        <port>5555</port>"
        +HTTPConnection.getConfigurationOptions()
        +"\n    </Push>";
    }

    public void initialize()
    {
        defaultConnection=new HTTPConnection("DEFAULT","PUSH");
        defaultConnection.readConfiguration();
        sendHeader.put("Content-Type","multipart/related; boundary=multipart-boundary; type=\"application/xml\"");
        sendHeader.put("Accept","application/xml");
        sendHeader.put("Push-Mode","volatile, nonsecure");

        XTTProperties.printDebug(this.getClass().getName()+".initialize(): clearing variables");
    }

    public String checkResources()
    {
        int port = XTTProperties.getIntProperty("PUSHINITIATOR/PORT");
        String resourceString = "" + this.getClass().getName() + ":"+RESOURCE_OK;
        if(port>0)
        {
            try
            {
                java.net.ServerSocket s=new java.net.ServerSocket(port);
                s.close();
            } catch(Exception e)
            {
                resourceString = "" + this.getClass().getName() + ":"+RESOURCE_PORT+" '" +  port+"'";
            }
        }
        return resourceString;
    }

    /**
     * query if the body of a PAP response contains a specified regular expression value.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the variable name to store the result in,
     *                     <br><code>parameters[2]</code> argument is the java reqular expression pattern.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public boolean queryResponse(String[] parameters)
    {
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": queryResponse: variableName regularExpression");
            return false;
        }
        if(parameters.length!=3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName regularExpression");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return false;
        }
        else
        {
            XTTProperties.printDebug(parameters[0]+": regex: '"+parameters[2]+"'");
            return ConvertLib.queryString(parameters[0],ConvertLib.createString(getBody()),parameters[2],parameters[1]);
        }
    }

    public String toString()
    {
        return this.getClass().getName();
    }

}
