package com.mobixell.xtt;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;

/**
 * Extension Interface use with url paramter extension=full.class.name
 *
 * @author Roger Soder
 * @version 1.0
 */
public class WebWorkerExtensionEWS implements WebWorkerExtension
{
    public static final String tantau_sccsid = "@(#)$Id: WebWorkerExtensionEWS.java,v 1.2 2008/09/01 13:42:28 rsoder Exp $";
    private static LinkedHashMap<String,Vector<String>> receivedServerHeader=new LinkedHashMap<String,Vector<String>>();
    private static Object subscribekey=new Object();
    private static int subscribecount=0;
    private static Object getfolderkey=new Object();
    private static int getfoldercount=0;
    private static Map<String,FolderStatus> folderStatus = Collections.synchronizedMap(new HashMap<String,FolderStatus>());
    private static int overrideReturnCode=0;
    private static String overrideReturnMessage=null;
    private static int serverResponseDelay=0;
    private static String recievedURL="null";

    // return true if you want to skip the execution of the rest of the WebWorker code.
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
        synchronized(receivedServerHeader)
        {
            receivedServerHeader.clear();
            receivedServerHeader.putAll(receivedHeaders);
        }
        String storeHeaderString=receivedHeaders.get(null).get(0);
        String firstLine[]=storeHeaderString.split("\\s+",4);
        recievedURL=firstLine[1];
        
        int returnCode = 200;
        String returnMessage = "OK";
        String request=ConvertLib.createString(receivedBody.getArray());
        String response=null;
        
        String principalName=null;
        Pattern p=Pattern.compile("<PrincipalName>(.*?)</PrincipalName>",Pattern.DOTALL);
        Matcher m=p.matcher(request);
        if(m.find())
        {
            principalName=m.group(1);
        }
         
        String distinguishedFolderId=null;
        p=Pattern.compile("DistinguishedFolderId Id=\"(.*?)\"/>",Pattern.DOTALL);
        m=p.matcher(request);
        if(m.find())
        {
            distinguishedFolderId=m.group(1);
        }

        String date=""+(System.currentTimeMillis()/1000);//HTTPHelper.createHTTPDate();
        FolderStatus r=getFolderStatus(principalName,distinguishedFolderId);
        int unreadCount=r.getUnread();
        int readCount=r.getRead();

        if(request.indexOf("<Subscribe")>=0)
        {
            String statusFrequency=null;
            p=Pattern.compile("StatusFrequency>(.*?)<",Pattern.DOTALL);
            m=p.matcher(request);
            if(m.find())
            {
                statusFrequency=m.group(1);
            }
        
            String url=null;
            p=Pattern.compile("URL>(.*?)<",Pattern.DOTALL);
            m=p.matcher(request);
            if(m.find())
            {
                url=m.group(1);
            }

            String subcsriptionID=ConvertLib.base64Encode("XTT SUBSC "+principalName+" "+distinguishedFolderId+" "+date,false);
            //String subcsriptionID=ConvertLib.base64Encode("XTT SUBSCRIBE "+principalName+" "+distinguishedFolderId);//+" "+date,false);
            String watermark=ConvertLib.base64Encode("XTT WMARK "+date,false);
            XTTProperties.setVariable("EWSSERVER/"+principalName+"/"+distinguishedFolderId+"/SUBCSRIPTIONID",subcsriptionID);
            XTTProperties.setVariable("EWSSERVER/"+principalName+"/"+distinguishedFolderId+"/WATERMARK",watermark);
            XTTProperties.setVariable("EWSSERVER/"+principalName+"/"+distinguishedFolderId+"/DATE",date);
            XTTProperties.setVariable("EWSSERVER/"+principalName+"/"+distinguishedFolderId+"/STATUSFREQUENCY",statusFrequency);
            XTTProperties.setVariable("EWSSERVER/"+principalName+"/"+distinguishedFolderId+"/URL",url);

            response ="<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                +"\n"+"<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">"
                +"\n"+"<soap:Header>"
                +"\n"+"    <t:ServerVersionInfo MajorVersion=\"8\" MinorVersion=\"1\" MajorBuildNumber=\"240\" MinorBuildNumber=\"5\" xmlns:t=\"http://schemas.microsoft.com/exchange/services/2006/types\" />"
                +"\n"+"</soap:Header>"
                +"\n"+"<soap:Body>"
                +"\n"+"<m:SubscribeResponse xmlns:t=\"http://schemas.microsoft.com/exchange/services/2006/types\" xmlns:m=\"http://schemas.microsoft.com/exchange/services/2006/messages\">"
                +"\n"+"    <m:ResponseMessages>"
                +"\n"+"        <m:SubscribeResponseMessage ResponseClass=\"Success\">"
                +"\n"+"            <m:ResponseCode>NoError</m:ResponseCode>"
                +"\n"+"            <m:SubscriptionId>"+subcsriptionID+"</m:SubscriptionId>"
                +"\n"+"            <m:Watermark>"+watermark+"=</m:Watermark>"
                +"\n"+"        </m:SubscribeResponseMessage>"
                +"\n"+"    </m:ResponseMessages>"
                +"\n"+"</m:SubscribeResponse>"
                +"\n"+"</soap:Body>"
                +"\n"+"</soap:Envelope>"
                ;
            synchronized (subscribekey)
            {
                subscribecount++;
                subscribekey.notifyAll();
            }
        } else
        {
            String folderID=ConvertLib.base64Encode("XTT FOLDER "+principalName+" "+distinguishedFolderId+" "+date,false);
            XTTProperties.setVariable("EWSSERVER/"+principalName+"/"+distinguishedFolderId+"/FDATE",date);
            XTTProperties.setVariable("EWSSERVER/"+principalName+"/"+distinguishedFolderId+"/FOLDERID",folderID);
            // parent folder id should be different, but meh
            response ="<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                +"\n"+"<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">"
                +"\n"+"<soap:Header>"
                +"\n"+"    <t:ServerVersionInfo MajorVersion=\"8\" MinorVersion=\"1\" MajorBuildNumber=\"240\" MinorBuildNumber=\"5\" xmlns:t=\"http://schemas.microsoft.com/exchange/services/2006/types\" />"
                +"\n"+"</soap:Header>"
                +"\n"+"<soap:Body>"
                +"\n"+"<m:GetFolderResponse xmlns:t=\"http://schemas.microsoft.com/exchange/services/2006/types\" xmlns:m=\"http://schemas.microsoft.com/exchange/services/2006/messages\">"
                +"\n"+"    <m:ResponseMessages>"
                +"\n"+"        <m:GetFolderResponseMessage ResponseClass=\"Success\">"
                +"\n"+"            <m:ResponseCode>NoError</m:ResponseCode>"
                +"\n"+"            <m:Folders>"
                +"\n"+"                <t:Folder>"
                +"\n"+"                    <t:FolderId Id=\""+folderID+"\" ChangeKey=\"AQAAABQAAACEq00ciZPCR7JKeN65lUrVAAAEQg==\" />"
                +"\n"+"                    <t:ParentFolderId Id=\""+folderID+"\" ChangeKey=\"AQAAAA==\" />"
                +"\n"+"                    <t:FolderClass>IPF.Note</t:FolderClass>"
                +"\n"+"                    <t:DisplayName>"+distinguishedFolderId+"</t:DisplayName>"
                +"\n"+"                    <t:TotalCount>"+(readCount+unreadCount)+"</t:TotalCount>"
                +"\n"+"                    <t:ChildFolderCount>0</t:ChildFolderCount>"
                +"\n"+"                    <t:UnreadCount>"+unreadCount+"</t:UnreadCount>"
                +"\n"+"                </t:Folder>"
                +"\n"+"            </m:Folders>"
                +"\n"+"        </m:GetFolderResponseMessage>"
                +"\n"+"    </m:ResponseMessages>"
                +"\n"+"</m:GetFolderResponse>"
                +"\n"+"</soap:Body>"
                +"\n"+"</soap:Envelope>"
                ;
            synchronized (getfolderkey)
            {
                getfoldercount++;
                getfolderkey.notifyAll();
            }
        }
        
        if(serverResponseDelay>0)
        {
            try
            {
                Thread.sleep(serverResponseDelay);
            } catch (Exception ex)
            {}
        }

        responseBody.setArray(ConvertLib.createBytes(response));
        headersToSend.put("Connection","close");
        headersToSend.put("X-Powered-By","ASP.NET");
        headersToSend.put("X-AspNet-Version","2.0.50727");
        headersToSend.put("Cache-Control","private, max-age=0");
        headersToSend.put("content-type","text/html");
        headersToSend.put("content-length",""+responseBody.getArray().length);
        responseCodeLine.delete(0,responseCodeLine.length());
        if(overrideReturnCode>0)returnCode=overrideReturnCode;
        if(overrideReturnMessage!=null)returnMessage=overrideReturnMessage;
        responseCodeLine.append("HTTP/1.1 "+returnCode+" "+returnMessage);
        return false;
    }

    public static void waitForSubscribe(int number) throws java.lang.InterruptedException
    {
        if(WebServer.checkSockets())
        {
            XTTProperties.printFail("WebWorkerExtensionEWS.waitForSubscribe: no instance running!");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        int wait=XTTProperties.getIntProperty("EWSSERVER/TIMEOUT");
        if(wait<0)wait=WebServer.DEFAULTTIMEOUT;
        int prevcount=0;
        synchronized(subscribekey)
        {
            while(subscribecount<number)
            {
                XTTProperties.printInfo("WebWorkerExtensionEWS.waitForSubscribe: "+subscribecount+"/"+number);
                if(wait>0)
                {
                    prevcount=subscribecount;
                    subscribekey.wait(wait);
                    if(subscribecount==prevcount)
                    {
                        XTTProperties.printFail("WebWorkerExtensionEWS.waitForSubscribe: timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } else
                {
                    subscribekey.wait();
                }
            }
            XTTProperties.printInfo("WebWorkerExtensionEWS.waitForSubscribe: "+subscribecount+"/"+number);
        }
    }
    public static void waitForTimeoutSubscribe(int timeouttime, int maxnumber) throws java.lang.InterruptedException
    {
        if(WebServer.checkSockets())
        {
            XTTProperties.printFail("WebWorkerExtensionEWS.waitForTimeoutSubscribe: no instance running!");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        int wait=timeouttime;
        int prevcount=0;
        int number=0;
        synchronized(subscribekey)
        {
            if(maxnumber>=0)
            {
                number=maxnumber+1;
            } else
            {
                number=subscribecount+1;
            }
            while(subscribecount<number)
            {
                XTTProperties.printInfo("WebWorkerExtensionEWS.waitForTimeoutSubscribe: "+subscribecount+"/"+number+" time: "+timeouttime+"ms");
                prevcount=subscribecount;
                subscribekey.wait(wait);
                if(subscribecount==prevcount)
                {
                    XTTProperties.printInfo("WebWorkerExtensionEWS.waitForTimeoutSubscribe: timed out with no requests!");
                    return;
                }
            }
            XTTProperties.printFail("WebWorkerExtensionEWS.waitForTimeoutSubscribe: request received! "+subscribecount+"/"+number+"");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
    }
    
    public static void waitForGetFolder(int number) throws java.lang.InterruptedException
    {
        if(WebServer.checkSockets())
        {
            XTTProperties.printFail("WebWorkerExtensionEWS.waitForGetFolder: no instance running!");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        int wait=XTTProperties.getIntProperty("EWSSERVER/TIMEOUT");
        if(wait<0)wait=WebServer.DEFAULTTIMEOUT;
        int prevcount=0;
        synchronized(getfolderkey)
        {
            while(getfoldercount<number)
            {
                XTTProperties.printInfo("WebWorkerExtensionEWS.waitForGetFolder: "+getfoldercount+"/"+number);
                if(wait>0)
                {
                    prevcount=getfoldercount;
                    getfolderkey.wait(wait);
                    if(getfoldercount==prevcount)
                    {
                        XTTProperties.printFail("WebWorkerExtensionEWS.waitForGetFolder: timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } else
                {
                    getfolderkey.wait();
                }
            }
            XTTProperties.printInfo("WebWorkerExtensionEWS.waitForGetFolder: "+getfoldercount+"/"+number);
        }
    }
    public static void waitForTimeoutGetFolder(int timeouttime, int maxnumber) throws java.lang.InterruptedException
    {
        if(WebServer.checkSockets())
        {
            XTTProperties.printFail("WebWorkerExtensionEWS.waitForTimeoutGetFolder: no instance running!");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        int wait=timeouttime;
        int prevcount=0;
        int number=0;
        synchronized(getfolderkey)
        {
            if(maxnumber>=0)
            {
                number=maxnumber+1;
            } else
            {
                number=getfoldercount+1;
            }
            while(getfoldercount<number)
            {
                XTTProperties.printInfo("WebWorkerExtensionEWS.waitForTimeoutGetFolder: "+getfoldercount+"/"+number+" time: "+timeouttime+"ms");
                prevcount=getfoldercount;
                getfolderkey.wait(wait);
                if(getfoldercount==prevcount)
                {
                    XTTProperties.printInfo("WebWorkerExtensionEWS.waitForTimeoutGetFolder: timed out with no requests!");
                    return;
                }
            }
            XTTProperties.printFail("WebWorkerExtensionEWS.waitForTimeoutGetFolder: request received! "+getfoldercount+"/"+number+"");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
    }

    public static void init()
    {
        synchronized (subscribekey)
        {
            subscribecount=0;
        }
        synchronized (getfolderkey)
        {
            getfoldercount=0;
        }
        folderStatus.clear();
        overrideReturnCode=0;           
        overrideReturnMessage=null;  
    }
    
    private static class FolderStatus
    {
        int unread=0;
        int read=0;
        public int getRead(){return read;}
        public void setRead(int read){this.read=read;}
        public int getUnread(){return unread;}
        public void setUnread(int unread){this.unread=unread;}
    }
    
    private FolderStatus getFolderStatus(String principalName,String distinguishedFolderId)
    {
        FolderStatus r=folderStatus.get(principalName.toLowerCase()+"/"+distinguishedFolderId.toLowerCase());
        if(r==null){return new FolderStatus();}
        return r;
    }

    public static void setRead(String principalName,String distinguishedFolderId, int read)
    {
        FolderStatus r=folderStatus.get(principalName.toLowerCase()+"/"+distinguishedFolderId.toLowerCase());
        if(r==null){r=new FolderStatus();}
        r.setRead(read);
        folderStatus.put(principalName.toLowerCase()+"/"+distinguishedFolderId.toLowerCase(),r);
    }
    public static void setUnread(String principalName,String distinguishedFolderId, int unread)
    {
        FolderStatus r=folderStatus.get(principalName.toLowerCase()+"/"+distinguishedFolderId.toLowerCase());
        if(r==null){r=new FolderStatus();}
        r.setUnread(unread);
        folderStatus.put(principalName.toLowerCase()+"/"+distinguishedFolderId.toLowerCase(),r);
    }
    public static void setOverrideReturnCode(int code)
    {
        overrideReturnCode=code;
    }
    public static void setOverrideReturnMessage(String msg)
    {
        overrideReturnMessage=msg;
    }
    public static void setServerResponseDelay(int delay)
    {
        serverResponseDelay=delay;
    }
    public static LinkedHashMap<String,Vector<String>> getServerHeader()
    {
        LinkedHashMap<String,Vector<String>> returnMap=null;
        synchronized(receivedServerHeader)
        {
            returnMap=new LinkedHashMap<String,Vector<String>>();
            returnMap.putAll(receivedServerHeader);
        }
        return returnMap;
    }
    public static String getServerRecievedURL()
    {
        return recievedURL;
    }
}
















