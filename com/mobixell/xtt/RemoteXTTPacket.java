package com.mobixell.xtt;

import org.jdom.Document;
import org.jdom.Element;

public class RemoteXTTPacket
{
    private Document document = null;
    
    Exception exception=null;

    public RemoteXTTPacket(Document document)
    {
        this.document = document;
    }

    public void setDocument(Document document)
    {
        this.document = document;
    }

    public Document getDocument()
    {
        return document;
    }

    public void setException(Exception exception)
    {
        this.exception = exception;
    }

    public Exception getException()
    {
        return exception;
    }

    public int getReturnCode()
    {
        int returnCode = -1;
        //You don't need to do any more checks since it will just break the 'try' if it's invalid.
        try
        {
            Element dataElement = XTTXML.getElement("response/returncode",document);
            returnCode = Integer.parseInt(dataElement.getText());
        }
        catch(Exception e)
        {
            XTTProperties.printFail("RemoteXTTPacket: Error getting the return code");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            returnCode = -1;
        }
        return returnCode;
    }
}