package com.mobixell.xtt;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Extension Interface use with url paramter extension=full.class.name
 *
 * @author Gavin Cattell
 * @version 1.0
 */
public class WebWorkerExtensionPushInitiator implements WebWorkerExtension 
{
    // return true if you want to skip the executen of the rest of the WebWorker code.
    public boolean execute(
          WebWorker myWorker
        , HashMap<String,Vector<String>> receivedHeaders
        , ByteArrayWrapper receivedBody
        , StringBuffer responseCodeLine
        , HashMap<String,String> headersToSend
        , ByteArrayWrapper responseBody
        , PrintStream ps
        )
    {
        String bodyAsString = new String(receivedBody.getArray());
        String newResponseBody = "";
        String pushId = null;
        Pattern pattern = Pattern.compile("push-id=\"(.*?)\"");
        Matcher matcher = pattern.matcher(bodyAsString);
        
        XTTProperties.printDebug("PushInitiator: HTTP Server addon START");
        
        try
        {
            if(matcher.find())
            {
                pushId = matcher.group(1);
            }
            
            responseCodeLine.delete(responseCodeLine.indexOf(" "),responseCodeLine.length());
            responseCodeLine.append(" 202 Accepted");
            
            if(pushId == null)
            {
                XTTProperties.printFail("PushInitiator: Didn't get a PUSH ID in the message.");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                newResponseBody = "<!DOCTYPE pap PUBLIC \"-//OMA//DTD PAP 2.1//EN\" \"http://www.openmobilealliance.org/tech/DTD/pap_2.1.dtd\" [<?wap-pap-ver supported-versions=\"2.1,2.0,1.*\"?>]><pap><badmessage-response code=\"2000\" desc=\"No push id sent\"></badmessage-response></pap>";
                responseBody.setArray(ConvertLib.createBytes(newResponseBody));
            }
            else
            {
                newResponseBody = "<!DOCTYPE pap PUBLIC \"-//OMA//DTD PAP 2.1//EN\" \"http://www.openmobilealliance.org/tech/DTD/pap_2.1.dtd\" [<?wap-pap-ver supported-versions=\"2.1,2.0,1.*\"?>]><pap><resultnotification-response code=\"1000\" desc=\"XTT accepted your notification\"></resultnotification-response></pap>";
                responseBody.setArray(ConvertLib.createBytes(newResponseBody));                
            }
        }
        catch(Exception e)
        {
            XTTProperties.printFail("PushInitiator: Error parsing message");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);    
            }    
        }
        
        XTTProperties.printDebug("PushInitiator: HTTP Server addon END");        
        return false;
    }
}