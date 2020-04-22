package com.mobixell.xtt;
import java.util.Formatter;
public class XTTTest
{
    private String testName=null;
    private String fileName=null;

    private int status=XTTProperties.UNATTEMPTED;

    private long startTime=0l;
    private long endTime=0l;
    
    private StringOutputStream log=new StringOutputStream();

    public XTTTest(String fileName)
    {
        setFileName(fileName);
    }

    public String getTestName()
    {
        if(testName==null)return fileName;
        return testName;
    }
    public void setTestName(String testName)
    {
        this.testName=testName;
    }

    public String getFileName()
    {
        return fileName;
    }
    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public int getTestStatus()
    {
        return status;
    }
    public void setTestStatus(int status)
    {
        this.status = status;
    }

    public long getStartTime()
    {
        return startTime;
    }
    public void setStartTime(long startTime)
    {
        this.startTime = startTime;
    }

    public long getEndTime()
    {
        return endTime;
    }
    public void setEndTime(long endTime)
    {
        this.endTime = endTime;
    }

    public void resetTest()
    {
        testName=null;
        status=XTTProperties.PASSED;
        startTime=0l;
        endTime=0l;
    }

    public String getElapsedTime()
    {
        long timeDifference = endTime - startTime;
        if(timeDifference < 0)
        {
            return "";
        }

        long hours = timeDifference/(60*60*1000);
        timeDifference%=(60*60*1000);
        long minutes = timeDifference/(60*1000);
        timeDifference%=(60*1000);
        long seconds = timeDifference/1000;
        timeDifference%=1000;
        long milliseconds = timeDifference;


        String timeStamp="";
        Formatter formatTime = new Formatter();
        
        //Formatter syntax %[argument_index$][flags][width]conversion
        timeStamp += formatTime.format("%1$03d:%2$02d:%3$02d.%4$03d",hours,minutes,seconds,milliseconds);
        
        return timeStamp;
    }

    public StringOutputStream getLogStream()
    {
        return log;
    }
    public String getLog()
    {
        return log.toString();
    }
    public void clearLog()
    {
        log.clear();
    }

    public void start()
    {
        status=XTTProperties.UNATTEMPTED;
        if(XTTProperties.isMemoryLoggingEnabled()||XTTProperties.getXTTGui()!=null)
        {
            XTTProperties.getOutputStream().addOutputStream(log);
        }
        XTTProperties.setVariable(null,null);
        
        XTTProperties.setVariable("CURRENTTEST/STATUS",XTTProperties.getStatusDescription(status));
        XTTProperties.setVariable("CURRENTTEST/STATUS/DESCRIPTION",XTTProperties.getStatusDescription(status));
        //A function to call when you start a test.
        setStartTime(System.currentTimeMillis());
        XTTProperties.setVariable("CURRENTTEST/STARTTIME",""+startTime);
    }

    public void end()
    {
        //A function to call when you end a test.
        setEndTime(System.currentTimeMillis());
        if(status<=XTTProperties.PASSED)
        {
            status=XTTProperties.PASSED;
            XTTProperties.setVariable("CURRENTTEST/STATUS",XTTProperties.getStatusDescription(status));
            
        }
        XTTProperties.getOutputStream().removeOutputStream(log);
    }
}