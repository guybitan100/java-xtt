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
public class WebWorkerExtensionTest implements WebWorkerExtension 
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
        XTTProperties.printInfo("WebWorkerExtensionTest("+myWorker.getMyServerPort()+"/"+myWorker.getWorkerId()+"): adding 'x-xtt-extension: true! header");
        headersToSend.put("x-xtt-extension","true");
        return false;
    }
}