package com.mobixell.xtt;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Vector;

/**
 * Extension Interface use with url paramter extension=full.class.name
 *
 * @author Roger Soder
 * @version 1.0
 */
public class WebWorkerExtensionEcho implements WebWorkerExtension 
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
        XTTProperties.printInfo("WebWorkerExtensionEcho("+myWorker.getMyServerPort()+"/"+myWorker.getWorkerId()+"): doing an echo");
        
        for(String header : receivedHeaders.keySet())
        {
            if (headersToSend.get(header) == null)
            {
                Vector<String> values = receivedHeaders.get(header);
                String concat = "";
                for(int i=0;i< values.size();i++)
                {
                    concat += values.get(i);
                }
                headersToSend.put("echo-"+header,concat);
            }
                
            
        }
        
        responseCodeLine.delete(responseCodeLine.indexOf(" "),responseCodeLine.length());
        responseCodeLine.append(" 200 OK");
        
        
        String newResponseBody = "<html><head><title>Done</title></head><body>I'm done, check the headers<br/>" + headersToSend.get("echo-null") + "</body></html>";
        responseBody.setArray(ConvertLib.createBytes(newResponseBody));                    
        //headersToSend.put("x-xtt-extension","true");
        return false;
    }
}