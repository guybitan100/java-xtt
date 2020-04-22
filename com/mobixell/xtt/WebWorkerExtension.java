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
public interface WebWorkerExtension
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
        );
}