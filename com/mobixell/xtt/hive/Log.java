package com.mobixell.xtt.hive;

import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.SimpleFilenameFilter;
import com.mobixell.xtt.ConvertLib;

import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class Log
{
    private static Map<Drone, File> logs = java.util.Collections.synchronizedMap(new HashMap<Drone, File>());

    private static GregorianCalendar rollOverEarliest = null;
    private static GregorianCalendar rollOverLatest = null;
    private static String logSuffix = "";
    //private static String logExtension = ".txt";
    private static String logDirectory = "logs";
    private static final String logDateFormat = "'-'yyyy'-'MM'-'dd'_'kk'-'mm'-'ss";

    public static void initialize()
    {
        //Check if the Logging should not be per Recipient
        String oneFileOnly = XTTProperties.getProperty("LOG/MESSAGING/ONEFILEONLY");

        logDirectory = XTTProperties.getQuietProperty("LOG/MESSAGING/DIRECTORY");
        if((logDirectory == null) || logDirectory.equals("null"))
        {
            logDirectory = XTTProperties.getQuietProperty("LOG/DIRECTORY");
            if((logDirectory == null) || logDirectory.equals("null")) 
            {
                logDirectory = "logs";
            }

            XTTProperties.printVerbose("Logging messages to '" + logDirectory + "' , set 'LOG/MESSAGING/DIRECTORY' to change."); 
        }
        

        File logDir = new File(logDirectory);
        if(!logDir.exists())
        {
            logDir.mkdirs();
        }

        //Check if the logging should use a new file per day.
        String rollOver = XTTProperties.getProperty("LOG/MESSAGING/ROLLOVEREVERY"); //SESSION/DAY/WEEK/MONTH/DAYOFWEEK/NEVER
        Hive.printDebug("Log: Roll Over Every " + rollOver);
        setRollOver(rollOver);
    }

    private static void setRollOver(String rollOverString)
    {
        try
        {
            GregorianCalendar startUpTime = new GregorianCalendar();
            int curYear = startUpTime.get(GregorianCalendar.YEAR);
            int curMonth = startUpTime.get(GregorianCalendar.MONTH);
            int curDay = startUpTime.get(GregorianCalendar.DAY_OF_MONTH);
            int curDayOfWeek = startUpTime.get(GregorianCalendar.DAY_OF_WEEK);
            int curHour = startUpTime.get(GregorianCalendar.HOUR);
            int curMinute = startUpTime.get(GregorianCalendar.MINUTE);
            int curSecond = startUpTime.get(GregorianCalendar.SECOND);
            rollOverString = rollOverString.trim();
            if((rollOverString == "null")||(rollOverString.equalsIgnoreCase("SESSION")))
            {
                rollOverEarliest = new GregorianCalendar(curYear, curMonth, curDay, curHour, curMinute, curSecond);
                rollOverLatest = rollOverEarliest;
            } else if (rollOverString.equalsIgnoreCase("DAY"))
            {
                rollOverEarliest = new GregorianCalendar(curYear, curMonth, curDay-1, 0, 0, 0);
                rollOverLatest = new GregorianCalendar(curYear, curMonth, curDay, 23, 59, 59);
            } else if (rollOverString.equalsIgnoreCase("WEEK"))
            {
                //TODO: Work out what happens when you SET the date to a day.
                rollOverEarliest = (GregorianCalendar)startUpTime.getInstance();
                rollOverEarliest.add(GregorianCalendar.DAY_OF_WEEK,(GregorianCalendar.SUNDAY-curDayOfWeek));
                rollOverEarliest.set(GregorianCalendar.HOUR,0);
                rollOverEarliest.set(GregorianCalendar.MINUTE,0);
                rollOverEarliest.set(GregorianCalendar.SECOND,0);

                rollOverLatest = (GregorianCalendar)rollOverEarliest.getInstance();
                rollOverLatest.add(GregorianCalendar.DAY_OF_WEEK,GregorianCalendar.SATURDAY-curDayOfWeek);
                rollOverLatest.set(GregorianCalendar.HOUR,23);
                rollOverLatest.set(GregorianCalendar.MINUTE,59);
                rollOverLatest.set(GregorianCalendar.SECOND,59);
            } else if (rollOverString.equalsIgnoreCase("MONTH"))
            {
                rollOverEarliest = (GregorianCalendar)startUpTime.getInstance();
                rollOverEarliest.set(GregorianCalendar.DAY_OF_MONTH,rollOverEarliest.getMinimum(GregorianCalendar.DAY_OF_MONTH));
                rollOverEarliest.set(GregorianCalendar.HOUR,0);
                rollOverEarliest.set(GregorianCalendar.MINUTE,0);
                rollOverEarliest.set(GregorianCalendar.SECOND,0);

                rollOverLatest = (GregorianCalendar)rollOverEarliest.getInstance();
                rollOverLatest.add(GregorianCalendar.DAY_OF_MONTH,rollOverLatest.getMaximum(GregorianCalendar.DAY_OF_MONTH));
                rollOverLatest.set(GregorianCalendar.HOUR,23);
                rollOverLatest.set(GregorianCalendar.MINUTE,59);
                rollOverLatest.set(GregorianCalendar.SECOND,59);
            }else if (rollOverString.equalsIgnoreCase("YEAR"))
            {
                rollOverEarliest = (GregorianCalendar)startUpTime.getInstance();
                rollOverEarliest.set(GregorianCalendar.DAY_OF_YEAR,rollOverEarliest.getMinimum(GregorianCalendar.DAY_OF_YEAR));
                rollOverEarliest.set(GregorianCalendar.HOUR,0);
                rollOverEarliest.set(GregorianCalendar.MINUTE,0);
                rollOverEarliest.set(GregorianCalendar.SECOND,0);

                rollOverLatest = (GregorianCalendar)rollOverEarliest.getInstance();
                rollOverLatest.add(GregorianCalendar.DAY_OF_YEAR,rollOverLatest.getMaximum(GregorianCalendar.DAY_OF_YEAR));
                rollOverLatest.set(GregorianCalendar.HOUR,23);
                rollOverLatest.set(GregorianCalendar.MINUTE,59);
                rollOverLatest.set(GregorianCalendar.SECOND,59);
            } /*else if (rollOverString.equalsIgnoreCase("SUNDAY"))
            {
                rollOverEarliest = GregorianCalendar.DAY_OF_WEEK;
                rollOverLatest = startUpTime.get(GregorianCalendar.DAY_OF_WEEK);
            } else if (rollOverString.equalsIgnoreCase("MONDAY"))
            {
                rollOverEarliest = GregorianCalendar.DAY_OF_WEEK;
                rollOverLatest = startUpTime.get(GregorianCalendar.DAY_OF_WEEK);
            } else if (rollOverString.equalsIgnoreCase("TUESDAY"))
            {
                rollOverEarliest = GregorianCalendar.DAY_OF_WEEK;
                rollOverLatest = startUpTime.get(GregorianCalendar.DAY_OF_WEEK);
            } else if (rollOverString.equalsIgnoreCase("WEDNESDAY"))
            {
                rollOverEarliest = GregorianCalendar.DAY_OF_WEEK;
                rollOverLatest = startUpTime.get(GregorianCalendar.DAY_OF_WEEK);
            } else if (rollOverString.equalsIgnoreCase("THURSDAY"))
            {
                rollOverEarliest = GregorianCalendar.DAY_OF_WEEK;
                rollOverLatest = startUpTime.get(GregorianCalendar.DAY_OF_WEEK);
            } else if (rollOverString.equalsIgnoreCase("FRIDAY"))
            {
                rollOverEarliest = GregorianCalendar.DAY_OF_WEEK;
                rollOverLatest = startUpTime.get(GregorianCalendar.DAY_OF_WEEK);
            } else if (rollOverString.equalsIgnoreCase("SATURDAY"))
            {
                rollOverEarliest = GregorianCalendar.DAY_OF_WEEK;
                rollOverLatest = startUpTime.get(GregorianCalendar.DAY_OF_WEEK);
            } */else if (rollOverString.equalsIgnoreCase("NEVER"))
            {
                rollOverEarliest = null;
                rollOverLatest = null;
            } else
            {
                rollOverEarliest = startUpTime;
                rollOverLatest = startUpTime;
            }

            try
            {
                SimpleDateFormat formatter=new SimpleDateFormat(logDateFormat,Locale.US);
                logSuffix= formatter.format(startUpTime.getTime());
                Hive.printDebug("MessageLog: rollOverEarliest: " + formatter.format(rollOverEarliest.getTime()));
                Hive.printDebug("MessageLog: rollOverLatest: " + formatter.format(rollOverLatest.getTime()));
            }
            catch(IllegalArgumentException iae)
            {
                Hive.printFail("Hive: Error parsing pattern");
                Hive.printDebugException(iae);
            }
        }
        catch(Exception e)
        {
            Hive.printFail("Hive Log: Error!!!!");
            Hive.printDebugException(e);
        }
    }

    public static File getPlainTextLogFileForSender(Drone sender)
    {
        return getLogFileForSender(sender, ".txt");
    }

    public static File getHtmlLogFileForSender(Drone sender)
    {
        File log = getLogFileForSender(sender, ".html");
        if(!log.exists())
        {
            String newLine = System.getProperty("line.separator");
            appendToLog(log,"<html>" + newLine + "<head>" + newLine + "<title></title>" + newLine + "</head>" + newLine + "<body>" + newLine + "</body>" + newLine + "</html>");
        }
        return log;
    }

    private static File getLogFileForSender(Drone sender, String extension)
    {
        File log = null;
        try
        {
            log = logs.get(sender);

            //If it's null then we haven't stored/found a log to store to yet.
            if(log == null)
            {
                File logDir = new File(logDirectory);
                String[] files = logDir.list(new SimpleFilenameFilter(sender.getHostname(),extension));
                GregorianCalendar fileDate = new GregorianCalendar();
                String formatString = "'" + sender.getHostname() + "-'yyyy'-'MM'-'dd'_'kk'-'mm'-'ss" + "'" + extension + "'";
                SimpleDateFormat formatter = new SimpleDateFormat(formatString,Locale.US);
                for(String filename: files)
                {
                    try
                    {
                        fileDate.setTime(formatter.parse(filename));
                        if((fileDate.compareTo(rollOverEarliest) > 0) && (fileDate.compareTo(rollOverLatest) <= 0))
                        {
                            log = new File(logDirectory + File.separator + filename);
                            logs.put(sender,log);
                        }
                    }
                    catch(java.text.ParseException pe)
                    {
                        Hive.printFail("Hive: Error parsing filename '" + filename + "': for date with format >>"+formatString+"<<.");
                        Hive.printDebugException(pe);
                    }
                }
            }
        }
        catch(Exception e)
        {
            Hive.printFail("Hive Log: Error!!!!");
            Hive.printDebugException(e);
        }

        //If the log is still null after this check, then just make a log.
        if(log == null)
        {
            log = new File(logDirectory + File.separator + sender.getHostname() + logSuffix + extension);
            logs.put(sender,log);
        }

        return log;
    }

    public static void appendToLog(File file, String message)
    {
        try
        {
            java.io.BufferedOutputStream out=new java.io.BufferedOutputStream(new java.io.FileOutputStream(file,true));
            byte[] data=ConvertLib.createBytes(message);
            out.write(data, 0, data.length);
            out.flush();
            out.close();
        }
        catch(Exception e)
        {
            Hive.printFail("Hive Log: Error!!!!");
            Hive.printDebugException(e);
        }
    }

    public static void appendToLog(File file, String message, String remove)
    {
        try
        {
            java.io.RandomAccessFile out=new java.io.RandomAccessFile(file, "rw");
            byte[] removeBytes = ConvertLib.createBytes(remove);

            long skip = out.length() - removeBytes.length;
            if(skip <= 0)
            {
                Hive.printFail("Log: Cannot log Hive messages, nothing to remove from file");
                return;
            }
            out.seek(skip);
            byte[] lastBytes = new byte[removeBytes.length];
            out.readFully(lastBytes);
            if(remove.equals(ConvertLib.createString(lastBytes)))
            {
                out.seek(skip);
                byte[] data=ConvertLib.createBytes(message);
                out.write(data, 0, data.length);
                out.close();
            }
        }
        catch(Exception e)
        {
            Hive.printFail("Hive Log: Error!!!!");
            Hive.printDebugException(e);
        }
    }
}