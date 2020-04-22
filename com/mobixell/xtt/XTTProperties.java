package com.mobixell.xtt;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.text.SimpleDateFormat;
import java.util.Vector;
import java.util.Iterator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Properties;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.io.File;
import java.io.PrintStream;
import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.IOException;
import com.mobixell.xtt.gui.main.XTTGui;
import com.mobixell.xtt.hive.Hive;
import com.mobixell.xtt.util.MiscUtils;
/**
 * XTTProperties provides functions to be used throughout XTT, like printWarn, accessing propeties, etc.
 *
 * @author      Gavin Cattell
 * @version     $Revision: 1.115 $
 */
public class XTTProperties
{
    //Constants
    /**
     * Value of a passed test.
     */
    public final static int UNATTEMPTED = -1;
    public final static int PASSED = 0;
    /**
     * Value of a failed test.
     */
    public final static int FAILED = 1;
    public final static int NOT_RUNNING = 2;
    public final static int RUNNING = 3;
    /**
     * Exit values to return based on an error, only use 1-127!
     */
    public final static int FAILED_WITH_CORE = 127;
    public final static int FAILED_WITH_INVALID_ARGUMENTS = 20;
    public final static int FAILED_WITH_PARSER_ERROR = 25;
    
    
    public final static int FAILED_WITH_MISSING_ARGUMENTS = 30;
    public final static int FAILED_NO_ARGUMENTS = 100;
    public final static int FAILED_FUNCTION_NOT_EXIST = 101;
    public final static int FAILED_UNKNOWN = 102;
    public final static int FAILED_WITH_MISSING_FILE = 40;
    public final static int FAILED_WITH_INVALID_CONFIG_VALUE = 50;
    public final static int FAILED_WITH_MISSING_CONFIG_VALUE = 55;

    public final static String JARFILE="xtt.jar";
    private static String XTTBuildVersion=null;
    private static String XTTBuild=null;
    private static String XTTBuildTimeStamp=null;
    public static final String PRIVATEVERSION="Private Build";
    public static boolean isRemoteXTTRunning = false;
    private static final String VERSIONPREFIX="2.0.";
    private static FunctionModule_Remote fmr;
    /**
     * The string which is printed infront of a {@link #printFail(java.lang.String) printFail(java.lang.String)}
     */
    public static String FAILCOMMENT = "!!!!! ";

    /**
     * The string which is printed infront of a {@link #printFail(java.lang.String) printFail(java.lang.String)}
     */
    public static String DELIMITER = ",";

    /**
     * Value of debug level tracing
     */
    public final static int DEBUG = 5;
    /**
     * Value of verbose level tracing
     */
    public final static int VERBOSE = 4;
    /**
     * Value of info level tracing
     */
    public final static int INFO = 3;
    /**
     * Value of warn level tracing
     */
    public final static int WARN = 2;
    /**
     * Value of fail level tracing
     */
    public final static int FAIL = 1;

    private static PrintStream errorOut = System.err;
    //private static OutputRedirector outputRedirector=null;

    private static boolean startOnLoad = true;

    private static int tracing = DEBUG; //Default tracing level, I need this for the redirected output
    private static int logTracing = 0;
    private static boolean logTransactions = false;
    private static boolean logExternal = true;

    private static Vector<XTTTest> testList = null; //Stores the fileName and the test names against test results
    private static XTTTest currentTest = null;     //The current test running
    private static String defaultTestName = "";   //The current test running
    private static String defaultTestPath = "";  //The current test running

    private static String logName = "";
    private static String logDir = "logs";
    static String xmpVersion=null;
    private static boolean logHTML = false;
    private static HashMap<String,String> traceFormatLibrary = null;

    private static boolean doFormatting = false;
    private static final int traceFormatOptions = 8;
    private static String[] traceFormatStringComments = new String[traceFormatOptions+1];
    private static int[] traceFormatOptionOrder = new int[traceFormatOptions];
    private static Calendar calendar = new GregorianCalendar();

    private static XTTGui xttgui=null;
    //If xtt is null during a run, this means a standalone server is running.
    private static XTT xtt=null;
    private static Hive theHive=null;
    private static MultiOutputStream multiOut=new MultiOutputStream(System.out);
    //private static StringOutputStream logStream=null;
    private static StringOutputStream startLog=new StringOutputStream();
    private static StringOutputStream stopLog=new StringOutputStream();

    private static Iterator<XTTTest> iterateTestList=null;

    private static boolean logToMemory=true;

    private static int networklagdelay=100;

    public static final String tantau_sccsid = "@(#)$Id: XTTProperties.java,v 1.115 2009/09/10 13:33:26 rsoder Exp $";

    // for performance hack for variables
    public static HashMap<String,String> variableStore=new HashMap<String,String>();

    private static String charSet="UTF-8";

    private static String exportDelimiter=",";

    private static String xttFilePath = (new File("")).getAbsolutePath()+File.separator;
    
    private static int usedRemoteXTTPort=0;
    public static void setRemoteXTTPort(int port)
    {
        usedRemoteXTTPort=port;
    }
    public static int getRemoteXTTPort()
    {
        return usedRemoteXTTPort;
    }
    

    public static String getXTTFilePath()
    {
        return xttFilePath;
    }
    public static String getExportDelimiter()
    {
        return exportDelimiter;
    }
    public static String getCharSet()
    {
        return charSet;
    }

    public static int getNetworkLagDelay()
    {
        return networklagdelay;
    }

    public static void setLogTransactions(boolean t)
    {
        logTransactions=t;
        if(xttgui!=null)
        {
            xttgui.setLogTransaction(t);
        }
    }
    public static boolean getLogTransactions()
    {
        return logTransactions;
    }
    public static void setLogExternal(boolean t)
    {
        logExternal=t;
        if(xttgui!=null)
        {
            xttgui.setLogExternal(t);
        }
    }
    public static boolean getLogExternal()
    {
        return logExternal;
    }

    public static void initOutputStream()
    {
        try
        {
            System.setOut(new PrintStream(multiOut,true,getCharSet()));
        } catch (java.io.UnsupportedEncodingException uee)
        {
            XTTProperties.printWarn("Unsupported charset: "+XTTProperties.getCharSet());
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(uee);
            }
            System.setOut(new PrintStream(multiOut));
        }
    }

    public static void setSystemErr()
    {
        try
        {
            System.setErr(new PrintStream(multiOut,true,getCharSet()));
        } catch (java.io.UnsupportedEncodingException uee)
        {
            XTTProperties.printWarn("Unsupported charset: "+XTTProperties.getCharSet());
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(uee);
            }
            System.setErr(new PrintStream(multiOut));
        }
    }
    public static void resetSystemErr()
    {
        System.setErr(errorOut);
    }
    public static String getLog()
    {
        StringWriter output=new StringWriter();
        try
        {
            BufferedWriter out = new BufferedWriter(output);
            Vector<StringOutputStream> logs=new Vector<StringOutputStream>();
            logs.add(startLog);
            if(testList!=null)
            {
                Iterator<XTTTest> tests=testList.iterator();
                while(tests.hasNext())
                {
                    logs.add(tests.next().getLogStream());
                }
            }
            logs.add(stopLog);

            BufferedReader datain=null;
            String currentLine=null;
            Iterator<StringOutputStream> logiterator=logs.iterator();
            while(logiterator.hasNext())
            {
                datain=new BufferedReader(new StringReader(logiterator.next().toString()));
                while((currentLine=datain.readLine())!=null)
                {
                    out.write(currentLine);
                    out.newLine();
                }
            }
            out.flush();
            out.close();
        } catch (java.io.IOException ex){}
        return output.toString();
    }
    public static MultiOutputStream getOutputStream()
    {
        return multiOut;
    }
    public static void setStartOnLoad(boolean value)
    {
        //XTTXML.removeAll("STARTONLOAD");
        startOnLoad=value;
    }
    public static boolean getStartOnLoad()
    {
        return startOnLoad;
    }

    public static void setLogHTML(boolean value)
    {
        logHTML = value;
    }

    public static boolean isMemoryLoggingEnabled()
    {
        return logToMemory;
    }

    public static void setMultipleVariables(String[] store, String where, String what)
    {
        for(int i=0;i<store.length;i++)
        {
            setVariable(store[i]+where,what);
        }
    }
    public static void setVariable(String name, String content)
    {
        if(getXTT()!=null)
        {
            synchronized(variableStore)
            {
                if(name==null&&content==null)
                {
                    variableStore.clear();
                }else if(content==null)
                {
                    variableStore.remove(name.toLowerCase());
                }else
                {
                    variableStore.put(name.toLowerCase(),content);
                }
                variableStore.notifyAll();
                return;
            }
        }
    }

    public static void removeVariables(String regex)
    {
        if(getXTT()!=null)
        {
            Vector<String> keyvector=null;
            synchronized(variableStore)
            {
                keyvector=new Vector<String>(variableStore.keySet());
            }
            String key=null;
            Iterator<String> keys=keyvector.iterator();
            Pattern pattern = Pattern.compile(regex,Pattern.DOTALL|Pattern.CASE_INSENSITIVE);
            Matcher matcher = null;
            while(keys.hasNext())
            {
                key=keys.next();
                matcher=pattern.matcher(key);
                if(matcher.find())
                {
                    variableStore.remove(key);
                    //System.out.println("KEY MATCHED "+key);
                } else
                {
                    //System.out.println("KEY PASSED  "+key);
                }
            }
            return;
        }
    }

    public static void setProperty(String name, String content)
    {
        if(name.toLowerCase().startsWith("variables/"))
        {
            printWarn("setProperty: Use setVariable for setting variables, setProperty is for configuration files editing only");
        }
        XTTXML.setProperty(name,content);
    }

    /**
    * Returns a String value from an XML DOM Tree corresponding to the name argument.
    * <p>
    * The name argument can point to subnodes of nodes by adding the '/' character
    * This specifies the tree to search down, it can be as long as you want.
    * If just a single word is specified, then the first occurance of that node will be returned.
    *
    * Get property first searches through the test specific configuration, before checking the global one.
    * Returns "null" if not found
    * @param primaryName The node to get
    * @param deprecatedNames A list of nodes to search if the primaryName isn't found
    */
    public static String getProperty(String primaryName, String... deprecatedNames)
    {
        String property = getSingleQuietProperty(primaryName);

        if (property.equals("null"))
        {
            for(String name : deprecatedNames)
            {
                property = getSingleQuietProperty(name);
                if(!property.equals("null"))
                {
                    XTTProperties.printWarn("'" + property + "' found in deprecated node '" + name + "', use '" + primaryName +"' instead.");
                    return property;
                }
            }
        }

        if (property.equals("null"))
        {
            XTTProperties.printDebug("Property '" + primaryName + "' wasn't found");
        }
        return property;
    }

    /**
    * <code>getProperty</code>, but with no warning.
    *
    */
    public static String getQuietProperty(String primaryName, String... deprecatedNames)
    {
        String property = getSingleQuietProperty(primaryName);

        if (property.equals("null"))
        {
            for(String name : deprecatedNames)
            {
                property = getSingleQuietProperty(name);
                if(!property.equals("null"))
                {
                    XTTProperties.printWarn("'" + property + "' found in deprecated node '" + name + "', use '" + primaryName +"' instead.");
                    return property;
                }
            }
        }

        return property;
    }


    /**
    * Used to simplify the varargs feature of getQuietProperty and getProperty.
    *
    */
    private static String getSingleQuietProperty(String name)
    {
        return XTTXML.getQuietProperty(name);
    }

    public static String getVariable(String name)
    {
        // Performance Hack for variables
        if(name!=null)
        {
            synchronized(variableStore)
            {
                String content=(String)variableStore.get(name.toLowerCase());
                if(content==null)return "null";
                return content;
            }
        }
        return "null";
     }

    public static int getIntVariable(String name)
    {
        // Performance Hack for variables
        if(name!=null)
        {
            synchronized(variableStore)
            {
                String content = null;
                try
                {
                    content=(String)variableStore.get(name.toLowerCase());
                    if(content!=null)
                    {
                        content = content.trim();
                        return Integer.decode(content).intValue();
                    }

                }
                catch (NumberFormatException nfe)
                {
                    XTTProperties.printFail("getIntVariable: Error: The variable you wanted is not a number '" + content +"'");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_CONFIG_VALUE);
                }
            }
        }
        return -1;
     }

    /**
    * Gets the property, but tries to make it into an Integer as well.
    * <p>
    * Returns -1 if it's not found.
    * @param primaryName property to try and get
    * @see #getProperty
    */
    public static int getIntProperty(String primaryName, String... deprecatedNames)
    {
        int property = getSingleQuietIntProperty(primaryName);
        if(property == -1)
        {
            for(String name : deprecatedNames)
            {
                property = getSingleQuietIntProperty(name);
                if(property != -1)
                {
                    XTTProperties.printWarn("'" + property + "' found in deprecated node '" + name + "', use '" + primaryName +"' instead.");
                    return property;
                }
            }
        }

        if(property == -1)
        {
            XTTProperties.printDebug("Property '" + primaryName + "' wasn't found");
        }
        return property;
    }
   
     public static boolean isRemoteXTTRunning()
     {
    	 String remoteIP = getProperty("system/remoteip");
         int remotePort = XTTProperties.getIntProperty("system/remoteport");
         if(remotePort <= 0)remotePort = RemoteXTT.DEFAULTPORT;
      
     if ((!remoteIP.equals("null")) && (remotePort > 0)) 
      {
			if (MiscUtils.isPing(remoteIP)) 
			{
				try {
					//InetSocketAddress remoteAddress = new java.net.InetSocketAddress(remoteIP, remotePort);
					//isRemoteXTTRunning = RemoteXTTClient.isRemoteXTTRunning(remoteAddress);
					if (fmr==null)
					fmr = new FunctionModule_Remote();
					isRemoteXTTRunning=fmr.setProductsVersion();
				} 
				catch (Exception e) {isRemoteXTTRunning = false;}
			} else {
				isRemoteXTTRunning = false;
			}
		}
		return isRemoteXTTRunning;
     }
    /**
    * Same as getIntProperty, but with no warning.
    *
    */

    public static int getQuietIntProperty(String primaryName, String... deprecatedNames)
    {
        int property = getSingleQuietIntProperty(primaryName);

        if (property == -1)
        {
            for(String name : deprecatedNames)
            {
                property = getSingleQuietIntProperty(name);
                if(property != -1)
                {
                    XTTProperties.printWarn("'" + property + "' found in deprecated node '" + name + "', use '" + primaryName +"' instead.");
                    return property;
                }
            }
        }
        return property;
    }

    public static int getSingleQuietIntProperty(String name)
    {
        String property = "null";
        try
        {
            property = getSingleQuietProperty(name);
            if (!property.equals("null"))
            {
                property = property.trim();
                return Integer.decode(property).intValue();
            }
        }
        catch (NumberFormatException nfe)
        {
            XTTProperties.printFail("getIntProperty: Error: The property you wanted is not a number '" + property +"'");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_CONFIG_VALUE);
        }
        catch (Exception e)
        {
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
            XTTProperties.printFail("getIntProperty: Error: " + e);
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }

        return -1;
    }

    public static boolean loadMainConfiguration(String fileName)
    {
        XTTConfigurationLocalPermanent config = new XTTConfigurationLocalPermanent(fileName);
        return config.add();
    }

    public static void initializeMainConfigurations()
    {
        String property = "null";

        property = getProperty("tracing/level","tracing");
        if (!property.equals("null"))
        {
            setTracing(property);
        }
        else
        {
            setTracing(DEBUG);
        }

        property = getProperty("system/ip");
        //Only care about the IP if we have a configuration to add it to.
        if(property.equals("null")&&(XTTConfigurationLocalPermanent.getNumberOfConfigurations()>0))
        {
            String ip = null;
            try
            {
                ip = java.net.InetAddress.getLocalHost().getHostAddress();
            }
            catch(java.net.UnknownHostException uhe)
            {
                //
            }

            if(ip == null)
            {
                printFail("Error on load: couldn't determine machine IP, and none was configured");
                setTestStatus(XTTProperties.FAILED);
            }
            XTTXML.setGlobalProperty("system/ip",ip);
        }

        printInfo("SYSTEM/IP: " + XTTProperties.getProperty("system/ip"));

        //If the --disableStartOnLoad is set, remove all the startOnLoad tags you find.
        if(!XTTProperties.getStartOnLoad())
        {
            XTTXML.removeAll("STARTONLOAD");
        }

        property = getProperty("tracing/format");
        if (!property.equals("null"))
        {
            setPrintFormat(property);
        }
        /*else
        {
            setPrintFormat(null);
        }*/

        property = getProperty("tracing/showtransactions");
        if (!property.equals("null"))
        {
            setLogTransactions(true);
        }
        else
        {
            setLogTransactions(false);
        }

        property = getProperty("tracing/disableexternal");
        if (!property.equals("null"))
        {
            setLogExternal(false);
        }
        else
        {
            setLogExternal(true);
        }

        property = getProperty("log/disable");
        if (!property.equals("null"))
        {
            finishLog();
            setLogDirectory(null);
        }
        else
        {
            property = getProperty("log/disableMemoryLogMode");
            if (!property.equals("null"))
            {
                //finishLog();
                logToMemory=false;
                //if(logStream!=null)multiOut.removeOutputStream(logStream);
                //logStream=null;
            }

            property = getProperty("log/ashtml");
            if (!property.equals("null"))
            {
                if(!logToMemory)printWarn("log/disablelogToMemory ignored, can't use together with log/ashtml");
                setLogHTML(true);
                logToMemory=true;
            }
            else
            {
                setLogHTML(false);
            }

            if(logToMemory)
            {
                //logStream=new StringOutputStream();
                //multiOut.addOutputStream(logStream);
            }

            property = getProperty("log/directory");
            if (!property.equals("null"))
            {
                setLogDirectory(property);
            }
            //Only do this if we have at least one config to set it in.
            else if (XTTConfigurationLocalPermanent.getNumberOfConfigurations()>0)
            {
                //Save the log directory in the XML Document
                XTTXML.setGlobalProperty("log/directory","logs");
            }
        }
        networklagdelay=getIntProperty("system/networklagdelay");
        if(networklagdelay<0)networklagdelay=100;

        if(XTTProperties.getQuietProperty("SYSTEM/HIVE/ENABLETRACING").equals(""))
        {
            Hive.setShouldTrace(true);
        } else
        {
            Hive.setShouldTrace(false);
        }
        property = getQuietProperty("GUI/EXPORTDELIMITER");
        if (!property.equals("null"))
        {
            exportDelimiter=property;
        }
    }

    public static boolean loadTempConfiguration(String file)
    {
        XTTConfigurationLocalTemporary config = new XTTConfigurationLocalTemporary(file);
        config.add();
        //return XTTXML.loadTempConfiguration(file);
        return true;
    }

    public static boolean hasConfigurationChanged()
    {
        return XTTConfiguration.doAnyNeedUpdating();
    }

    public static void reloadConfiguration()
    {
        XTTConfiguration.updateAll();
    }

    /**
     * Parses the trace/format node.
     * <p>
     * format = item;
     * item = [character]%logitem[character|item]
     */
    private static boolean parseTraceFormat(String format)
    {
        try
        {
            if(traceFormatLibrary == null)
            {
                traceFormatLibrary = new HashMap<String,String>();
                traceFormatLibrary.put("%t","1"); //time stamps
                traceFormatLibrary.put("%f","2"); //function(line #)
                traceFormatLibrary.put("%m","3"); //message
                traceFormatLibrary.put("%l","4"); //Trace level (f,w,d,...)
                traceFormatLibrary.put("%n","5"); //Test name
                traceFormatLibrary.put("%a","6"); //time in milliseconds
                traceFormatLibrary.put("%i","7"); //Thread Name
                traceFormatLibrary.put("%j","8"); //Thread Id
                traceFormatLibrary.put("%d","9"); //time in milliseconds
            }

            String workingFormat=format;
            if((workingFormat != null)&&(workingFormat.length()>0)&&(!workingFormat.equals("null")))
            {
                String regex = "(.*?)(%{1}[^%]).*+";
                String option;
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(workingFormat);
                int i = 0;
                while(matcher.find())
                {
                    traceFormatStringComments[i] = matcher.group(1);
                    option = (String)traceFormatLibrary.get(matcher.group(2));
                    traceFormatOptionOrder[i] = Integer.parseInt(option);
                    workingFormat = workingFormat.substring(matcher.group(1).length()+matcher.group(2).length());
                    matcher = pattern.matcher(workingFormat);
                    i++;
                }
                if (i>0)
                {
                    traceFormatStringComments[i] = workingFormat;
                }
                else
                {
                    printWarn("XTTProperties: parseTraceFormat: Format not correct");
                }
            }
        }
        catch (Exception e)
        {
            printWarn("XTTProperties: parseTraceFormat: Format not correct");
            return false;
        }
        return true;
    }

    /**
    * Gets the path of the log file
    *
    */
    public static String getLogFileName()
    {
        if ((logDir == null)||(logName==null))
        {
            return null;
        }
        else
        {
            return "" + logDir + File.separator + logName;
        }
    }
    public static String setLogDirectory(String path)
    {
    	xmpVersion=null;
    	
    	if (path!=null)
    	{
    		logDir = path;
    	}
    	String pathToRet = logDir;
    	File logDirectory = new File(logDir);
    	
    	if (FunctionModule_Remote.getProducts()!=null)
      		 xmpVersion = FunctionModule_Remote.getProducts()[0][1].replace("_M00_", "_");
    	
    	 if(!logDirectory.exists())
         {
             logDirectory.mkdirs();
             
             if (xmpVersion!=null)
             {
            	 pathToRet = logDir+File.separator+xmpVersion;
            	 logDirectory = new File(pathToRet);
            	 if(!logDirectory.exists())
            	 {
            		 logDirectory.mkdirs();
            	 }
             }
         }
         else if (xmpVersion!=null)
         {
        	 pathToRet = logDir+File.separator+xmpVersion;
         	 logDirectory = new File(pathToRet);
             if(!logDirectory.exists())
             {
                 logDirectory.mkdirs();
             }
         }
    	
    	 return pathToRet;
    }
    public static String getLogDirectory()
    {
        return logDir;
    }

    public static BufferedOutputStream getNewLogStream()
    {
        if (logDir == null)
        {
            return null;
        }
        
        setLogDirectory(null);

        calendar.setTimeInMillis(System.currentTimeMillis());
        SimpleDateFormat format=new SimpleDateFormat("yyyy'-'MM'-'dd'_'kk'-'mm'-'ss",java.util.Locale.US);
        String name="log_"+format.format(calendar.getTime())+".txt";
        logName = name;
        format=new SimpleDateFormat("yyyy'-'MM'-'dd' 'kk':'mm':'ss",java.util.Locale.US);
        name=format.format(calendar.getTime());

        try
        {
            BufferedOutputStream returnStream=new BufferedOutputStream(new FileOutputStream(logDir + File.separator +logName,true));
            PrintStream out = new PrintStream(returnStream);
            out.println(logName + " " + name);
            out.println();
            out.println();
            out.flush();
            return returnStream;
        } catch(Exception e)
        {
            printWarn("Couldn't log output");
            if(printDebug(null))printException(e);
            return null;
            //Ignore
        }
    }


    /**
    * Prints information to the log file
    * <p>
    * Checks whether logging is enabled or not, and also creates files and directories
    * if they don't already exist.
    */
    private synchronized static void log()
    {
        if ((logDir == null)||(logName == null))
            return;
        if(XTTProperties.getXTTGui()!=null)
        {
            XTTProperties.getXTTGui().setMemoryBar();
        }

        String logPath =setLogDirectory(null);
        	
        if(logName.equals(""))
        {
            calendar.setTimeInMillis(System.currentTimeMillis());
            SimpleDateFormat format=new SimpleDateFormat("yyyy'-'MM'-'dd'_'kk'-'mm'-'ss",java.util.Locale.US);
            String name="log_"+format.format(calendar.getTime());


            if(logHTML){name += ".html";}
            else{name += ".txt";}
            logName = name;
            format=new SimpleDateFormat("yyyy'-'MM'-'dd' 'kk':'mm':'ss",java.util.Locale.US);
            name=format.format(calendar.getTime());

            String title = logName;
            if((testList != null) && (testList.size() == 1))
            {
                title = testList.firstElement().getTestName() + " - " + logName;
            }

            try
            {
            	
                BufferedWriter out = new BufferedWriter(new FileWriter(logPath + File.separator +logName,true));
                if(logHTML)
                {
                    String failColor=getQuietProperty("LOG/ASHTML/COLOR/FAIL");
                    if (failColor.equals("null"))
                    {
                        failColor = "red";
                    }
                    String warnColor=getQuietProperty("LOG/ASHTML/COLOR/WARN");
                    if (warnColor.equals("null"))
                    {
                        warnColor = "#FFCC00";
                    }
                    String infoColor=getQuietProperty("LOG/ASHTML/COLOR/INFO");
                    if (infoColor.equals("null"))
                    {
                        infoColor = "green";
                    }
                    String externalColor=getQuietProperty("LOG/ASHTML/COLOR/EXTERNAL");
                    if (externalColor.equals("null"))
                    {
                        externalColor = "#CC0099";
                    }
                    String verboseColor=getQuietProperty("LOG/ASHTML/COLOR/VERBOSE");
                    if (verboseColor.equals("null"))
                    {
                        verboseColor = "#B0C4DE";
                    }
                    String debugColor=getQuietProperty("LOG/ASHTML/COLOR/DEBUG");
                    if (debugColor.equals("null"))
                    {
                        debugColor = "#D8D8D8";
                    }
                    String normalColor=getQuietProperty("LOG/ASHTML/COLOR/NORMAL");
                    if (normalColor.equals("null"))
                    {
                        normalColor = "black";
                    }
                    String backgroundColor=getQuietProperty("LOG/ASHTML/COLOR/BACKGROUND");
                    if (backgroundColor.equals("null"))
                    {
                        backgroundColor = "white";
                    }
                    out.write("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">");
                    out.newLine();

                    out.write("<html><head><title>" + title + "</title>");
                    out.newLine();
                    out.write("<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\">");
                    out.newLine();
                    out.write("<style type=\"text/css\">");
                    out.newLine();
                    out.write("<!--");
                    out.newLine();
                    out.write(".normal{color:"+normalColor+";}");
                    out.newLine();
                    out.write(".external{color:"+externalColor+";}");
                    out.newLine();
                    out.write(".external:visited {color:"+externalColor+";}");
                    out.newLine();
                    out.write(".external:link {color:"+externalColor+";}");
                    out.newLine();
                    out.write(".external:active {color:"+externalColor+";}");
                    out.newLine();
                    out.write(".external:hover{color:"+externalColor+";}");
                    out.newLine();
                    out.write(".external{text-decoration:none;}");
                    out.newLine();
                    out.write(".fail{color:"+failColor+";}");
                    out.newLine();
                    out.write(".fail:visited {color:"+failColor+";}");
                    out.newLine();
                    out.write(".fail:link {color:"+failColor+";}");
                    out.newLine();
                    out.write(".fail:active {color:"+failColor+";}");
                    out.newLine();
                    out.write(".fail:hover{color:"+failColor+";}");
                    out.newLine();
                    out.write(".fail{text-decoration:none;}");
                    out.newLine();
                    out.write(".warn{color:"+warnColor+";}");
                    out.newLine();
                    out.write(".warn:visited {color:"+warnColor+";}");
                    out.newLine();
                    out.write(".warn:link {color:"+warnColor+";}");
                    out.newLine();
                    out.write(".warn:active {color:"+warnColor+";}");
                    out.newLine();
                    out.write(".warn:hover{color:"+warnColor+";}");
                    out.newLine();
                    out.write(".warn{text-decoration:none;}");
                    out.newLine();
                    out.write(".info{color:"+infoColor+";}");
                    out.newLine();
                    out.write(".info:visited {color:"+infoColor+";}");
                    out.newLine();
                    out.write(".info:link {color:"+infoColor+";}");
                    out.newLine();
                    out.write(".info:active {color:"+infoColor+";}");
                    out.newLine();
                    out.write(".info:hover{color:"+infoColor+";}");
                    out.newLine();
                    out.write(".info{text-decoration:none;}");
                    out.newLine();
                    out.write(".verbose{color:"+verboseColor+";}");
                    out.newLine();
                    out.write(".debug{color:"+debugColor+";}");
                    out.newLine();
                    out.write("body{background-color:"+backgroundColor+";color:"+normalColor+";");
                    out.newLine();
                    out.write("-->");
                    out.newLine();
                    out.write("</style>");
                    out.newLine();
                    out.write("</head><body><pre><code>");
                }
                
                out.write("<b>" + logName + " " + name+"</b>");
                if(logHTML)
                {
                    out.write("<br><span class=\"normal\">");
                    out.newLine();
                    out.write("jump to first:");
                    out.write("  <a class=\"fail\" href=\"#f0\">Fail</a>");
                    out.write(", <a class=\"info\" href=\"#i0\">Info</a>");
                    out.write(", <a class=\"warn\" href=\"#w0\">Warn</a>");
                    out.write(", <a class=\"external\" href=\"#e0\">External</a>");
                }
                out.newLine();
                out.newLine();
                out.flush();
                out.close();
            }
            catch(Exception e)
            {
                if(printDebug(null))printException(e);
                //Ignore
            }
        }

        try
        {
        	 BufferedWriter out = new BufferedWriter(new FileWriter(logPath + File.separator +logName,true));

            Vector<StringOutputStream> logs=new Vector<StringOutputStream>();
            logs.add(startLog);
            if(testList != null)
            {
                Iterator<XTTTest> tests=testList.iterator();
                while(tests.hasNext())
                {
                    logs.add(tests.next().getLogStream());
                }
            }
            logs.add(stopLog);

            BufferedReader datain=null;
            //datain=new BufferedReader(new StringReader(logStream.toString()));
            String currentLine=null;
            String linehead="";
            String linefoot="";
            Iterator<StringOutputStream> logiterator=logs.iterator();
            int failCounter=0;
            int infoCounter=0;
            int warnCounter=0;
            int extCounter=0;
            while(logiterator.hasNext())
            {
                datain=new BufferedReader(new StringReader(logiterator.next().toString()));
                while((currentLine=datain.readLine())!=null)
                {
                    if(logHTML)
                    {
                        currentLine = currentLine.replaceAll("<","&lt;");
                        currentLine = currentLine.replaceAll(">","&gt;");

                        if(currentLine.startsWith("F: "))
                        {
                            linehead="</span><span class=\"fail\"><a class=\"fail\" name=\"f"+(failCounter++)+"\" href=\"#f"+failCounter+"\">";
                            linefoot="</a>";
                        } else if(currentLine.startsWith("D: "))
                        {
                            linehead="</span><span class=\"debug\">";
                            linefoot="";
                        } else if(currentLine.startsWith("I: "))
                        {
                            linehead="</span><span class=\"info\"><a class=\"info\" name=\"i"+(infoCounter++)+"\" href=\"#i"+infoCounter+"\">";
                            linefoot="</a>";
                        } else if(currentLine.startsWith("W: "))
                        {
                            linehead="</span><span class=\"warn\"><a class=\"warn\" name=\"w"+(warnCounter++)+"\" href=\"#w"+warnCounter+"\">";
                            linefoot="</a>";
                        } else if(currentLine.startsWith("V: "))
                        {
                            linehead="</span><span class=\"verbose\">";
                            linefoot="";
                        } else if(currentLine.startsWith("E: "))
                        {
                            linehead="</span><span class=\"external\"><a class=\"external\" name=\"e"+(extCounter++)+"\" href=\"#e"+extCounter+"\">";
                            linefoot="</a>";
                        } else if(currentLine.startsWith("####"))
                        {
                            linehead="</span><span class=\"normal\">";
                            linefoot="";
                        } else
                        {
                            //linehead="<font color=\"gray\">";
                            //linefoot="</font><BR>";
                            linehead="";
                            linefoot="";
                        }
                        out.write(linehead+currentLine+linefoot);
                        out.newLine();
                    } else
                    {
                        out.write(currentLine);
                        out.newLine();
                    }
                }
            }
            if(logHTML)
            {
                out.newLine();
                out.write("</span></code></pre>");
                out.newLine();
                out.write("<p><a href=\"http://validator.w3.org/check?uri=referer\">");
                out.write("<img src=\"http://www.w3.org/Icons/valid-html401\" alt=\"Valid HTML 4.01 Transitional\" height=\"31\" width=\"88\" border=\"0\">");
                out.write("</a></p>");
                out.newLine();
                out.write("</body></html>");
                out.newLine();
            }
            out.flush();
            out.close();
        }
        catch (Exception e)
        {
            printWarn("Couldn't log output");
            if(printDebug(null))printException(e);
        }
    }
    public static void startLog()
    {
        if(XTTProperties.isMemoryLoggingEnabled()||XTTProperties.getXTTGui()!=null)
        {
            startLog.clear();
            stopLog.clear();
            XTTProperties.getOutputStream().removeOutputStream(stopLog);
            XTTProperties.getOutputStream().addOutputStream(startLog);
        }
    }
    public static void stopLog()
    {
        if(XTTProperties.isMemoryLoggingEnabled()||XTTProperties.getXTTGui()!=null)
        {
            stopLog.clear();
            XTTProperties.getOutputStream().removeOutputStream(startLog);
            XTTProperties.getOutputStream().addOutputStream(stopLog);
        }
    }
    public static void finishLog()
    {
        if(!isMemoryLoggingEnabled()||(logDir == null)||(logName == null))//||(logStream == null))
        {
            return;
        }
        if(XTTProperties.getXTTGui()!=null)
        {
            XTTProperties.getXTTGui().setMemoryBar();
        }
        log();
        if(logHTML)
        {
            try
            {
               	String logPath = setLogDirectory(null);
                BufferedInputStream fin = new BufferedInputStream(new FileInputStream(logPath+File.separator +logName));
                BufferedOutputStream fout = new BufferedOutputStream(new FileOutputStream(logPath+File.separator +"results.html"));
                byte[] buffer = new byte[4096];
                int b = fin.read(buffer);
                while(b>=0)
                {
                    fout.write(buffer, 0, b);
                    b = fin.read(buffer);
                }
                fin.close();
                fout.close();
            }
            catch (Exception e)
            {
                printWarn("Couldn't log output");
                if(printDebug(null))printException(e);
            }
        }
        logName = "";
        //logStream.clear();
        XTTProperties.getOutputStream().removeOutputStream(stopLog);
        XTTProperties.getOutputStream().removeOutputStream(startLog);
    }

    /**
    *
    * Prints out a list of test with the corresponding results.
    */
    public static int showResults()
    {
        int numberPassed = 0;
        int numberFailed = 0;
        int maxNameSize = 0;
        StringBuffer output = new StringBuffer();
        String spacers = "";

        if (testList == null)
        {
            printFail("No tests to display results of");
            return 0;
        }

        printDebug("showResults: Starting to print RESULTS data");
        output.append(System.getProperty("line.separator")+"###############################################################################"+System.getProperty("line.separator")+System.getProperty("line.separator"));
        output.append("Results:\r\n");

        for (int j=0;j<testList.size();j++)
        {
            if(testList.get(j).getTestName() == null)
            {
                String fileName = testList.get(j).getFileName();
                if (fileName.indexOf(File.separator) != -1)
                {
                    try
                    {
                        fileName = fileName.substring(fileName.lastIndexOf(File.separator) + 1,fileName.length());
                    }
                    catch(IndexOutOfBoundsException e)
                    {
                        //just ignore the error, print out the filename as is
                    }
                }
                if(fileName.length() > maxNameSize)
                {
                    maxNameSize = fileName.length();
                }
            }
            else if(testList.get(j).getTestName().length() > maxNameSize)
            {
                maxNameSize = testList.get(j).getTestName().length();
            }
        }

        for (int k=0;k<maxNameSize;k++)
        {
            spacers += " "; //add as many spaces to the buffer as there are characters in the longest name
        }

        for (int i=0;i<testList.size();i++)
        {
            if((testList.size() >= 100)&&((i+1)<10))
            {
                output.append("Test #00" + (i+1) + ": ");
            }
            else if( ((testList.size() >= 10)&&((i+1)<10)) || ((testList.size() >= 100)&&((i+1)<100)) )
            {
                output.append("Test #0" + (i+1) + ": ");
            }
            else
            {
                output.append("Test #" + (i+1) + ": ");
            }

            if(testList.get(i).getTestName() == null)
            {
                String fileName = testList.get(i).getFileName();
                if (fileName.indexOf(File.separator) != -1)
                {
                    try
                    {
                        fileName = fileName.substring(fileName.lastIndexOf(File.separator) + 1,fileName.length());
                    }
                    catch(IndexOutOfBoundsException e)
                    {
                        //just ignore the error, print out the filename as is
                    }
                }
                if (fileName.length() < maxNameSize)
                {
                    output.append(fileName + ""+ spacers.substring(0,(maxNameSize - fileName.length())) +" " + getStatusDescription(testList.get(i).getTestStatus()) + " in " + testList.get(i).getElapsedTime() +System.getProperty("line.separator"));
                }
                else
                {
                    output.append(fileName + " " + getStatusDescription(testList.get(i).getTestStatus()) + " in " + testList.get(i).getElapsedTime() +System.getProperty("line.separator"));
                }
            }
            else
            {
                if (testList.get(i).getTestName().length() < maxNameSize)
                {
                    output.append(testList.get(i).getTestName() + ""+ spacers.substring(0,(maxNameSize - testList.get(i).getTestName().length())) +" " + getStatusDescription(testList.get(i).getTestStatus()) + " in " + testList.get(i).getElapsedTime() +System.getProperty("line.separator"));
                }
                else
                {
                    output.append(testList.get(i).getTestName() + " " + getStatusDescription(testList.get(i).getTestStatus()) + " in " + testList.get(i).getElapsedTime() +System.getProperty("line.separator"));
                }
            }

            if(testList.get(i).getTestStatus() == PASSED)
            {
                numberPassed ++;
            }
            else
            {
                numberFailed ++;
            }
        }
        output.append("PASSED: " + numberPassed + "; FAILED: " + numberFailed);
        System.out.println(output);
        printDebug("showResults: Done printing RESULTS data");
        return numberFailed;
    }
    /**
    *
    * Returns the number of tests loaded to run
    */
    public static int getNumberOfTests()
    {
        if (testList==null)
            return 0;
        return testList.size();
    }
    public static int getCurrentTestNumber()
    {
        if (testList==null||currentTest==null)
            return 0;
        return testList.indexOf(currentTest);
    }
    /*
    *
    * Sets the number of the test to run.
    * <p>
    * The argument can't be greater than the number of tests loaded.
    * @param number The test number to run
    * @see #getNumberOfTests
    */
    /*public static void setTestNumber(int number)
    {
        if((number>=0)&&(number<testList.size()))
        {
            currentTest  = testList.get(number);
            printVerbose("Setting test number to: " + number);

            XTTXML.removeTemporaryLocalConfigurations();
        } else
        {
            printFail("Error setting test number; Invalid number");
        }
    }*/

    public static boolean checkConfiguration()
    {
        //You must load at least ONE configuration before running any tests.
        if(XTTConfiguration.getNumberofPermanentLocalConfigurations()==0)
        {
            return false;
        }
        return true;
    }

    public static boolean advanceTestList()
    {
        if(testList==null)return false;
        if(iterateTestList==null)iterateTestList=testList.iterator();
        if(iterateTestList.hasNext())
        {
            currentTest=iterateTestList.next();
            return true;
        } else
        {
            return false;
        }
    }
    public static void resetTestList()
    {
        iterateTestList=null;
        currentTest=null;
        if(getXTTGui()!=null)
        {
            getXTTGui().setMemoryBar();
        }
    }

    public static boolean abortTestList()
    {
        if(getXTTGui()!=null&&getXTTGui().doAbort())
        {
            printFail("Test execution aborted by GUI");
            finishLog();
            getXTTGui().showError("Aborted Tests","Execution of tests aborted");
            return true;
        } else
        {
            return false;
        }
    }

    public static void appendCurrentTestLog(String message)
    {
        if(XTTProperties.isMemoryLoggingEnabled()||XTTProperties.getXTTGui()!=null)
        {
            try
            {
                currentTest.getLogStream().write(ConvertLib.createBytes(message));
            } catch (IOException ex){}
        }
    }

    public static String getCurrentTestPath()
    {
    	
        if(testList == null||currentTest==null)
        {
        	if (defaultTestName!=null)
            {
        		int lastSlash = defaultTestPath.lastIndexOf(File.separator);

                if(lastSlash == -1)
                {
                    return "";
                }
                else
                {
                	 return defaultTestPath.substring(0,lastSlash+1);
                }
            }
        	else return "";
        }

        int lastSlash = currentTest.getFileName().lastIndexOf(File.separator);

        if(lastSlash == -1)
        {
            return "";
        }
        else
        {
            return currentTest.getFileName().substring(0,lastSlash+1);
        }
    }

    public static String getAbsoluteFilePath(String filename)
    {
        File theFile = new File(filename);
        try
        {
            int lastSlash = theFile.getAbsolutePath().lastIndexOf(File.separator);

            if(lastSlash == -1)
            {
                return "";
            }
            else
            {
                return theFile.getAbsolutePath().substring(0,lastSlash+1);
            }
        }
        catch(Exception e)
        {

        }
        return null;
    }

    /**
    * Sets the list of test to be run from the String[].
    * @param listName  File handle of the list file with all the filenames of the tests to run
    */
    public static void setTestList(File listName)
    {
        String listPath = listName.getAbsolutePath();

        if((!listName.exists())||(!listName.toString().toLowerCase().endsWith(".list")))
        {
            printFail("setTestList: List file doesn't exist or doesn't end in .list");
            return;
        }

        String[]files = XTTProperties.readFile(listPath).split("\n");

        if(listPath.lastIndexOf(File.separator) != -1)
        {
            listPath = listPath.substring(0,listPath.lastIndexOf(File.separator)+1);
        }
        else
        {
            listPath = "";
        }
        if(xttgui!=null)
        {
            xttgui.setListFileName(listName.getAbsolutePath());
        }

        testList = new Vector<XTTTest>();
        //String [][] list = new String[files.length][4];
        File [] guilist = new File[files.length];
        //int length = 0;
        int guilength=0;

        for (int i = 0; i<files.length; i++)
        {
            if((files[i].length() > 0)&&(!files[i].trim().startsWith("//")))
            {
                if(new File(listPath + files[i]).exists())
                {
                    guilist[guilength]=new File(listPath + files[i]);
                    testList.add(new XTTTest(guilist[guilength].getAbsolutePath()));
                }
                else if(new File(files[i]).exists())
                {
                    guilist[guilength]=new File(files[i]);
                    testList.add(new XTTTest(guilist[guilength].getAbsolutePath()));
                }
                else
                {
                    printFail("setTestList: couldn't find file '"+files[i]+"'");
                    guilist[guilength]=new File(files[i]);
                    guilength++;
                    continue;
                }
                guilength++;
            }
        }
        if(xttgui!=null)
        {
            File [] newguilist=new File[guilength];
            for(int i=0;i<guilength;i++)
            {
                newguilist[i]=guilist[i];
            }
            xttgui.addTests(newguilist);
        }

        String temp="Loaded files: \n";
        for(int j=0;j<testList.size();j++)
            temp+=testList.get(j).getFileName() + "\n";
        printVerbose(temp);
    }

    /**
    * Sets a single test to be run from the String.
    * @param fileName The filename of the test to be run
    */
    public static void setTestList(String fileName,boolean isSetCurrentTest)
    {
        if(new File(fileName).exists())
        {
        	testList = new Vector<XTTTest>();
        	XTTTest test =new XTTTest(new File(fileName).getAbsolutePath());
        	if (isSetCurrentTest) currentTest =test;
            testList.add(test);
            printVerbose(testList.get(0).getFileName());
        }
        else
        {
            printFail("setTestList: couldn't find file '"+fileName+"'");
        }

    }

    public static void exportTestResults(File file) throws Exception
    {
        java.io.PrintWriter out=new java.io.PrintWriter(new java.io.FileWriter(file,false));
        XTTTest t=null;
        //out.print("\"i\""+XTTProperties.getExportDelimiter());
        out.print("\"teststatus\""+XTTProperties.getExportDelimiter());
        out.print("\"short filename\""+XTTProperties.getExportDelimiter());
        out.print("\"test name\""+XTTProperties.getExportDelimiter());
        out.print("\"description\""+XTTProperties.getExportDelimiter());
        out.print("\"elapsed time\""+XTTProperties.getExportDelimiter());
        out.print("\"start time\""+XTTProperties.getExportDelimiter());
        out.println("\"end time\"");
        for(int i=0;i<testList.size();i++)
        {
            t=testList.elementAt(i);
            //out.print("\""+i+"\""+XTTProperties.getExportDelimiter());
            out.print("\""+XTTProperties.getStatusDescription(t.getTestStatus())+"\""+XTTProperties.getExportDelimiter());
            out.print("\""+ConvertLib.shortFileName(t.getFileName(),XTTProperties.getXTTFilePath())+"\""+XTTProperties.getExportDelimiter());
            out.print("\""+t.getTestName().replaceAll("\"","\"\"")+"\""+XTTProperties.getExportDelimiter());
            out.print("\""+"\""+XTTProperties.getExportDelimiter());// no description in here possible
            out.print("\""+t.getElapsedTime()+"\""+XTTProperties.getExportDelimiter());
            out.print("\""+t.getStartTime()+"\""+XTTProperties.getExportDelimiter());
            out.println("\""+t.getEndTime()+"\"");
        }
        out.flush();
        out.close();
    }

    public static void setTestList(Vector<XTTTest> list)
    {
        testList=list;
    }

    /**
    * TODO: Re-write this commend.
    * @param time    time in milliseconds that elapsed from test start to test end
    */
    public static void setCurrentTestStartTime(long time)
    {
        if(currentTest!=null)
            currentTest.setStartTime(time);
    }

    public static void setCurrentTestEndTime(long time)
    {
        if(currentTest!=null)
            currentTest.setEndTime(time);
    }

    /**
    * Sets the name of the current test.
    * @param testName The name of the test
    */
    public static void setCurrentTestName(String testName)
    {
        if(currentTest==null)
        {
            defaultTestName=testName;
        } 
        else if(currentTest.getTestName().equals(currentTest.getFileName()))
        {
            currentTest.setTestName(testName);
            //System.out.println("Tring to set name to :" + testList[currentTest][1]);
        } else
        {
            printDebug("Can't set Test Name; Name already set");
        }
    }
    public static void setCurrentTestPath(String testPath)
    {
        defaultTestPath = testPath;
    }
    /**
    * Gets the name of the current test.
    * <p>
    * Returns the name of the file if no name is set.
    */
    public static String getCurrentTestName()
    {
        if((testList == null)||(testList.size()==0)||currentTest==null)
        {
            return defaultTestName;
        }

        if(currentTest.getTestName()==null)
        {
            String fileName = "";
            try
            {
                fileName = currentTest.getFileName();
                if (fileName.indexOf(File.separator) != -1)
                {
                    fileName = fileName.substring(fileName.lastIndexOf(File.separator) + 1,fileName.length());
                }
            }
            catch(Exception e)
            {
                //just ignore the error, print out the filename as is
            }
            return fileName;
        }
        else
        {
            return currentTest.getTestName();
        }
    }

    public static int getCurrentTestStatusCode()
    {
        if(currentTest != null)
        {
            return currentTest.getTestStatus();
        }
        return UNATTEMPTED;
    }
    public static String getCurrentTestStatus()
    {
        if(currentTest != null)
        {
            return getStatusDescription(currentTest.getTestStatus());
        }
        return null;
    }

    public static String getCurrentTestFileName()
    {
        return currentTest.getFileName();
    }

    public static String getCurrentTestElapsedTime()
    {
        return currentTest.getElapsedTime();
    }

    public static void startCurrentTest()
    {
        XTTProperties.getOutputStream().removeOutputStream(startLog);
        XTTConfiguration.removeTemporaryLocalConfigurations();
        if(currentTest!=null)
            currentTest.start();
    }
    public static void endCurrentTest()
    {
        if(currentTest!=null)
            currentTest.end();
    }

    /**
    * Sets the current test status.
    * <p>
    * @param status either {@link #PASSED} or {@link #FAILED}
    */
    public static void setTestStatus(int status)
    {
        if((testList == null)||(testList.size() == 0)||(currentTest==null))
        {
            //If we're not running a standalone server then print the message
            if(xtt!=null) {
                printWarn("Trying to set test status to '" + status + "', but no tests were found");
            }
            return;
        }
        if(getStatusDescription(status) != null)
        {
            if((status == PASSED)&&(currentTest.getTestStatus() > PASSED))
            {
                printWarn("Warning you are setting a FAILED test to PASSED");
            }
            try
            {
                if(currentTest.getTestStatus()<status)
                {
                    printDebug("Setting test status to: " + getStatusDescription(status));
                    currentTest.setTestStatus(status);

                    if(getCurrentTestFailed())
                    {
                        XTTProperties.setVariable("CURRENTTEST/STATUS","FAILED");
                    }
                    XTTProperties.setVariable("CURRENTTEST/STATUS/DESCRIPTION",XTTProperties.getCurrentTestStatus());
                }
                else
                {
                    printDebug("Status is already " + getStatusDescription(currentTest.getTestStatus()) + ", not changing to "+getStatusDescription(status)+ ".");
                }
            }
            catch(IndexOutOfBoundsException e)
            {
                if(xttgui!=null)
                {
                    xttgui.showError("RUN Error",e.getClass().getName()+"\n"+e.getMessage()+"\n\nStatus not set!");
                }
                if(XTTProperties.printDebug(null))
                {
                    printException(e);
                }
            }
        }
        else
        {
            printFail("Invalid status: " + status);
        }
    }

    public static boolean getCurrentTestFailed()
    {
        if (testList != null)
        {
            return (currentTest.getTestStatus() >= FAILED);
        }
        else
        {
            return true;
        }
    }

    public static int getExitCode()
    {
        if(testList==null||!checkConfiguration())
        {
            return FAILED_WITH_MISSING_FILE;
        }

        Iterator<XTTTest> i=testList.iterator();
        XTTTest tempTest = null;
        int worstReason=PASSED;

        while(i.hasNext())
        {
            tempTest = i.next();
            if(tempTest.getTestStatus() > worstReason)
            {
                XTTProperties.printDebug("getExitCode: Reason changed to: " + tempTest.getTestStatus());
                worstReason = tempTest.getTestStatus();
            }
        }
        return worstReason;
    }

    /**
    *
    * Returns the contents of the file as a String.
    * <p>
    * If the file is empty, or doesn't exist a blank String is returned
    * @param fileName The file to load
    */
    public static String readFile(String fileName)
    {
        try
        {
            BufferedReader fileIn = new BufferedReader(new FileReader(fileName));
            StringBuffer fileContent = new StringBuffer(); //Store all the information from the file
            String temp = null; //Used each time to check if the line is null
            // Read the file in, while there's file left to read
            while ((temp = fileIn.readLine())!=null)
            {
               fileContent.append(temp);
               fileContent.append("\n");
            }
            return fileContent.toString();
        }
        catch (Exception e)
        {
            if(XTTProperties.printDebug(null))
            {
                printException(e);
            }
        }
        return "";
    }

    public static int getTracing()
    {
        return tracing;
    }

    /**
    *
    * Set the tracing level.
    * <p>
    * Sets the tracing level based of the <code>String</code> interpretation of the level.
    * @param level The level of tracing to set
    * @see #DEBUG #VERBOSE #INFO #WARN #FAIL
    */
    public static void setTracing(String level)
    {
        if(level.length()>0)
        {
            switch (level.charAt(0))
            {
                case 'd': case 'D': XTTProperties.setTracing(XTTProperties.DEBUG); break;
                case 'v': case 'V': XTTProperties.setTracing(XTTProperties.VERBOSE); break;
                case 'i': case 'I': XTTProperties.setTracing(XTTProperties.INFO); break;
                case 'w': case 'W': XTTProperties.setTracing(XTTProperties.WARN); break;
                case 'f': case 'F': XTTProperties.setTracing(XTTProperties.FAIL); break;
            }
        }
    }
    /**
    *
    * Set the tracing level.
    * <p>
    * Also check whether the level to be set is a valid level.
    * @param level The level of tracing to set
    * @see #DEBUG #VERBOSE #INFO #WARN #FAIL
    */
    public static void setTracing(int level)
    {
        if ((level>=FAIL)&&(level<=DEBUG)) //Ranges of possible values
        {
            tracing = level;
            if(level==DEBUG)
            {
                setSystemErr();
            } else
            {
                resetSystemErr();
            }
            if(xttgui!=null)
            {
                xttgui.setTracing(level);
            }
        }
        else
        {
            printFail("Invalid level: " + level + ", level set to " + tracing);
        }
    }

    public static void setPrintFormat(String format)
    {
        doFormatting = parseTraceFormat(format);
    }

    //Not currently used, because of problems being able to use different trace levels
    /*private static void setLogTracing(int level)
    {
        if ((level>=1)&&(level<=5))
        {
            logTracing = level;
        }
        else
        {
            printFail("Invalid level: " + level + ", level set to " + logTracing);
        }
    }*/

    public static void printDebugException(Exception e)
    {
        if(printDebug(null))
        {
            printException(e);
        }
    }

    public static void printException(Exception e)
    {
        printException((Throwable)e);
    }

    public static void printException(Throwable e)
    {
        StringBuffer output = new StringBuffer();

        StackTraceElement[] stackTrace = e.getStackTrace();
        output.append(e.getClass().getName()+"\n");
        if(e.getMessage() != null)
        {
            output.append(e.getMessage()+"\n");
        }
        for(int i=0;i<stackTrace.length;i++)
        {
            //System.out.println(stackTrace[i].toString());
            output.append("\t" + stackTrace[i].getClassName() + " " + stackTrace[i].getMethodName() + " (" + stackTrace[i].getLineNumber() + ")\n");
        }
        if(e.getCause()!=null)
        {
            output.append("Caused by: \n");
            System.out.print(output);
            System.out.flush();

            printException(e.getCause());
        } else
        {
            System.out.print(output);
            System.out.flush();
        }
    }


    /**
    *
    * Print out 'External' information.
    * <p>
    * 'External' level is printed as long as tracing/disableexternal isn't added in the config.
    * Also return true if External tracing is seen or logged
    * @param text The String to print out
    */
    public static boolean printExternal(String text)
    {
        if((text == null)&&(logExternal))
        {
            return true;
        }
        else if(logExternal)
        {
            if(doFormatting)
            {
                printFormatted("E",text);
            }
            else if(xtt!=null)
            {
                System.out.println("E: " + getCurrentTestName() + ": " + text);
            }
            else
            {
                System.out.println("E: " + text);
            }

            return true;
        }
        return false;
    }


    /**
    *
    * Print out 'Debug' information.
    * <p>
    * 'Debug' level is printed if the tracing level Debug
    * Also return true if DEBUG tracing is seen or logged
    * @param text The String to print out
    * @see #DEBUG
    */
    public static boolean printDebug(String text)
    {
        if((text == null)&&((tracing >= DEBUG)||(logTracing >= DEBUG)))
        {
            return true;
        }
        else if((tracing >= DEBUG)||(logTracing >= DEBUG))
        {
            /*
            * Only works on consoles that support ANSI.SYS
            *           FG BG
            *Black      30 40
            *Red        31 41
            *Green      32 42
            *Yellow     33 43
            *Blue       34 44
            *Magenta    35 45
            *Cyan       36 46
            *White      37 47
            */
            //System.out.println((char)27 + "[35m" + "D: " + getCurrentTestName() + ": " + text + (char)27 + "[0m");
            if(doFormatting)
            {
                printFormatted("D",text);
            }
            else if(xtt!=null)
            {
                System.out.println("D: " + getCurrentTestName() + ": " + text);
            }
            else
            {
                System.out.println("D: " + text);
            }

            return true;
        }
        return false;
    }

    /**
    *
    * Print out 'Verbose' information.
    * <p>
    * 'Verbose' level is printed if the tracing level is Verbose, or Debug
    * Also return true if VERBOSE tracing is seen or logged
    * @param text The String to print out
    * @see #VERBOSE
    */
    public static boolean printVerbose(String text)
    {
        if((text == null)&&((tracing >= VERBOSE)||(logTracing >= VERBOSE)))
        {
            return true;
        }
        else if((tracing >= VERBOSE)||(logTracing >= VERBOSE))
        {
            if(doFormatting)
            {
                printFormatted("V",text);
            }
            else if(xtt!=null)
            {
                System.out.println("V: " + getCurrentTestName() + ": " + text);
            }
            else
            {
                System.out.println("V: " + text);
            }
            return true;
        }
        return false;
    }


    /**
    *
    * Print out 'Info' information.
    * <p>
    * 'Info' level is printed if the tracing level is Info, Verbose, or Debug
    * Also return true if INFO tracing is seen or logged
    * @param text The String to print out
    * @see #INFO
    */
    public static boolean printInfo(String text)
    {
        if((text == null)&&((tracing >= INFO)||(logTracing >= INFO)))
        {
            return true;
        }
        else if((tracing >= INFO)||(logTracing >= INFO))
        {
            if(doFormatting)
            {
                printFormatted("I",text);
            }
            else if(xtt!=null)
            {
                System.out.println("I: " + getCurrentTestName() + ": " + text);
            }
            else
            {
                System.out.println("I: " + text);
            }
            return true;
        }
        return false;
    }
    /**
    *
    * Print out 'Warn' information.
    * <p>
    * 'Warn' level is printed if the tracing level is Warn, Info, Verbose, or Debug
    * Also return true if WARN tracing is seen or logged
    * @param text The String to print out
    * @see #WARN
    */
    public static boolean printWarn(String text)
    {
        if((text == null)&&((tracing >= WARN)||(logTracing >= WARN)))
        {
            return true;
        }
        else if((tracing >= WARN)||(logTracing >= WARN))
        {
            if(doFormatting)
            {
                printFormatted("W",text);
            }
            else if(xtt!=null)
            {
                System.out.println("W: " + getCurrentTestName() + ": " + text);
            }
            else
            {
                System.out.println("W: " + text);
            }
            return true;
        }
        return false;
    }

    /**
    *
    * Print out 'Fail' information.
    * <p>
    * 'Fail' level is printed if the tracing level is Fail, Warn, Info, Verbose, or Debug
    * Also return true if FAIL tracing is seen or logged
    * @param text The String to print out
    * @see #FAIL
    */
    public static boolean printFail(String text)
    {
        if((text == null)&&((tracing >= FAIL)||(logTracing >= FAIL)))
        {
            return true;
        }
        else if((tracing >= FAIL)||(logTracing >= FAIL))
        {

            if(doFormatting)
            {
                printFormatted("F",FAILCOMMENT + text);
            }
            else if(xtt!=null)
            {
                System.out.println("F: " + getCurrentTestName() + ": " + FAILCOMMENT + text);
            }
            else
            {
                System.out.println("F: " + text);
            }
            return true;
        }
        return false;
    }

    public static boolean printTransaction(String text)
    {
        if((text == null)&&(logTransactions))
        {
            return true;
        }
        else if(logTransactions)
        {
            if(doFormatting)
            {
                printFormatted("T",text);
            }
            else if(xtt!=null)
            {
                System.out.println("T: " + getCurrentTestName() + DELIMITER + System.currentTimeMillis() + DELIMITER + text);
            }
            else
            {
                System.out.println("T: " + text);
            }
            return true;
        }
        return false;
    }

    /**
    *
    * Print out formatted trace information.
    * <p>
    * @param level The level of tracing, {@link #FAIL}, {@link #WARN}, {@link #INFO}, {@link #VERBOSE}, {@link #DEBUG}
    * @param message The message to print out
    */
    private static void printFormatted(String level, String message)
    {
        long timeInNano = System.nanoTime();
        long timeInMillis = System.currentTimeMillis();
        
        StringBuffer buffer = new StringBuffer();
        int i=0;
        for(i=0;i<traceFormatOptions;i++)
        {
            int traceOption = traceFormatOptionOrder[i];
            if(traceOption == 0)
            {
                break;
            }
            switch (traceOption)
            {
                case 1: //time stamps
                    buffer.append(traceFormatStringComments[i]);
                    calendar.setTimeInMillis(System.currentTimeMillis());
                    int hour = calendar.get(Calendar.HOUR_OF_DAY);
                    int minute = calendar.get(Calendar.MINUTE);
                    int second = calendar.get(Calendar.SECOND);
                    int millisecond = calendar.get(Calendar.MILLISECOND);
                    buffer.append(ConvertLib.intToString(hour,2)+":"+ConvertLib.intToString(minute,2)+":"+ConvertLib.intToString(second,2)+"."+ConvertLib.intToString(millisecond,3));
                    break;
                case 2: //function(line #)
                    buffer.append(traceFormatStringComments[i]);
                    Throwable stack = new Throwable();
                    stack = stack.fillInStackTrace();
                    StackTraceElement[] backTrace = stack.getStackTrace();
                    buffer.append(backTrace[2].getClassName().substring(backTrace[2].getClassName().lastIndexOf(".")+1) + "." + backTrace[2].getMethodName() + "("+backTrace[2].getLineNumber()+")");
                    break;
                case 3: //message
                    buffer.append(traceFormatStringComments[i]);
                    buffer.append(message);
                    break;
                case 4: //Trace level (f,w,d,...)
                    buffer.append(traceFormatStringComments[i]);
                    buffer.append(level);
                    break;
                case 5: //Test name
                    buffer.append(traceFormatStringComments[i]);
                    buffer.append(getCurrentTestName());
                    break;
                case 6: //Time in milliseconds
                    buffer.append(traceFormatStringComments[i]);
                    buffer.append(timeInMillis);
                    break;
                case 7:
                    buffer.append(traceFormatStringComments[i]);
                    buffer.append(Thread.currentThread().getName());
                    break;
                case 8:
                    buffer.append(traceFormatStringComments[i]);
                    buffer.append(Thread.currentThread().getId());
                    break;
                case 9:
                    buffer.append(traceFormatStringComments[i]);
                    buffer.append(timeInNano);
                    break;                    
                default:
                    System.err.println("Unknown trace option");
            }
        }
        buffer.append(traceFormatStringComments[i]);
        System.out.println(buffer);
    }

    /**
    *
    * Returns true when the program is ready to end, otherwise it returns false
    */
    public static boolean canEnd()
    {
        //if(outputRedirector != null)
        //    return outputRedirector.canEnd();
        //else
            return true;
    }

    public static void setXTTGui(XTTGui gui)
    {
        xttgui=gui;
    }
    public static XTTGui getXTTGui()
    {
        return xttgui;
    }

    public static void setXTT(XTT x)
    {
        xtt=x;
    }
    public static XTT getXTT()
    {
        return xtt;
    }

    public static void setHive(Hive hive)
    {
        theHive = hive;
    }
    public static Hive getHive()
    {
        return theHive;
    }

    public static String getStatusDescription(int status)
    {
        switch (status)
        {
            case UNATTEMPTED:                       return "UNATTEMPTED";
            case PASSED:                            return "PASSED";
            case FAILED:                            return "FAILED";
            case FAILED_WITH_CORE:                  return "FAILED WITH CORE";
            case FAILED_WITH_MISSING_ARGUMENTS:     return "FAILED DUE TO WRONG NUMBER OF ARGUMENTS";
            case FAILED_WITH_INVALID_ARGUMENTS:     return "FAILED DUE TO INVALID ARGUMENTS";
            case FAILED_WITH_PARSER_ERROR:          return "FAILED DUE TO PARSER ERROR";
            case FAILED_WITH_MISSING_FILE:          return "FAILED DUE TO MISSING FILE";
            case FAILED_WITH_MISSING_CONFIG_VALUE:  return "FAILED DUE TO MISSING CONFIG VALUE";
            case FAILED_WITH_INVALID_CONFIG_VALUE:  return "FAILED DUE TO INVALID CONFIG VALUE";
            default:                                return null;
        }
    }

    /**
     * Wait for Variable
     */
    public static void waitForVariable(String variableName) throws java.lang.InterruptedException
    {
        int wait=XTTProperties.getIntProperty("VARIABLEWAITTIMEOUT");
        if(wait<0)wait=30000;

        long prevtime=System.currentTimeMillis();
        synchronized(variableStore)
        {
            while(variableStore.get(variableName.toLowerCase())==null)
            {
                if(wait>0)
                {
                    //prevtime=System.currentTimeMillis();
                    variableStore.wait(wait);
                    if(System.currentTimeMillis()-prevtime>=wait)
                    {
                        XTTProperties.printFail("XTTProperties.waitForVariable: '"+variableName+"' timed out!");
                        XTTProperties.printDebug("XTTProperties.waitForVariable: '" + (System.currentTimeMillis()-prevtime) + " >= " + wait );
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } else
                {
                    variableStore.wait();
                }
            }
            XTTProperties.printInfo("XTTProperties.waitForVariable: found '"+variableName+"'");
        }
    }
    public static void waitForVariable(String variableName, String value) throws java.lang.InterruptedException
    {
        int wait=XTTProperties.getIntProperty("VARIABLEWAITTIMEOUT");
        if(wait<0)wait=30000;

        long prevtime=System.currentTimeMillis();
        String foundValue=null;
        synchronized(variableStore)
        {
            while(true)
            {
                foundValue=variableStore.get(variableName.toLowerCase());
                if(value.equals(foundValue))break;
                if(wait>0)
                {
                    //prevtime=System.currentTimeMillis();
                    variableStore.wait(wait);
                    if(System.currentTimeMillis()-prevtime>=wait)
                    {
                        XTTProperties.printFail("XTTProperties.waitForVariable: '"+variableName+"' timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } else
                {
                    variableStore.wait();
                }
            }
            XTTProperties.printInfo("XTTProperties.waitForVariable: found '"+variableName+"' value '"+foundValue+"'");
        }
    }

    public static void setXTTBuildVersion()
    {
        createXTTBuildVersion();
    }
    public static String getXTTBuildVersion()
    {
        return XTTBuildVersion;
    }
    public static String getXTTBuild()
	{
		return XTTBuild;
	}
	public static String getXTTBuildTimeStamp()
    {
        return XTTBuildTimeStamp;
    }
    private static void createXTTBuildVersion()
    {
        ZipFile zipJar=null;
        XTTBuildVersion=PRIVATEVERSION;
        XTTBuildTimeStamp="";

        String classpath[]=System.getProperty("java.class.path").split(System.getProperty("path.separator"));
        for(int i=0;i<classpath.length;i++)
        {
            if(classpath[i].equals(JARFILE)||classpath[i].endsWith(File.separator+JARFILE))
            {
                try
                {
                    zipJar=new ZipFile(classpath[i]);
                } catch (Exception e1)
                {
                }
            }
        }
        if(zipJar==null)
        {
            try
            {
                zipJar=new ZipFile(new File(JARFILE));
            } catch (Exception e2)
            {
            }
        }
        try
        {
            if(zipJar!=null)
            {
                ZipEntry zipEn=zipJar.getEntry("META-INF/MANIFEST.MF");
                BufferedInputStream buf=new BufferedInputStream(zipJar.getInputStream(zipEn));
                Properties p=new Properties();
                p.load(buf);
                String build=p.get("XTT-Build").toString();
                XTTBuildTimeStamp=" - "+p.get("XTT-BuildTimeStamp").toString();
                zipJar.close();
                XTTBuild = build;
                XTTBuildVersion=VERSIONPREFIX+""+ConvertLib.intToString(Integer.parseInt(build),4);
            } else
            {
                return;
            }
        } catch (Exception e2)
        {
            return;
        }
    }
    public static String getConfigurationOptions()
    {
        return "    <name>Default Config</name>"
            +"\n    <!-- maximum number of bytes to print on screen in debug level"
            +"\n         after receiving data on a GET to a webserver -->"
            +"\n    <Bufferoutputsize>1024</Bufferoutputsize>"
            +"\n    <!-- time to wait on a variable with the waitOnVariable function in basic -->"
            +"\n    <VariableWaitTimeout>20000</VariableWaitTimeout>"
            +"\n    <!-- basic tracing configuration -->"
            +"\n    <Tracing>"
            +"\n        <!--the output level of tracing"
            +"\n            valid values are: fail, warn, info, verbose, debug"
            +"\n            or just: f,w,i,v,d -->"
            +"\n        <level>i</level>"
            +"\n        <!-- change the tracing format to a new format string"
            +"\n             format = item; item = [character]%logitem[character|item]"
            +"\n             Logitem ="
            +"\n                 %t time stamps"
            +"\n                 %f function(line #)"
            +"\n                 %m message"
            +"\n                 %l Trace level (f,w,d,...)"
            +"\n                 %n Test name"
            +"\n                 %a time in milliseconds"
            +"\n                 %d time in nanoseconds (since some fixed but arbitrary time)"            
            +"\n                 %i the name of the thread"
            +"\n                 %j the id of the thread"
            +"\n        -->"
            +"\n        <!--<format>%l: %n: %t: %m</format>-->"
            +"\n        <!-- show the transaction output for later calculation of performance -->"
            +"\n        <!--<showtransactions/>-->"
            +"\n        <!-- disable tracing output on level external, currently only used by description tag -->"
            +"\n        <!--<disableexternal>-->"
            +"\n     </Tracing>"
            +"\n    <!-- basic logging configuration -->"
            +"\n    <log>"
            +"\n        <!--the path where you want to store the logfiles"
            +"\n            the default ist ./logs -->"
            +"\n        <directory>logs</directory>"
            +"\n        <!-- enable this to disable loging completely -->"
            +"\n        <!--<disable/>-->"
            +"\n        <!-- enable this to change logfile format to HTML -->"
            +"\n        <!--<asHTML>"
            +"\n            <color>"
            +"\n                <fail>red</fail>"
            +"\n                <warn>#FFCC00</warn>"
            +"\n                <info>green</info>"
            +"\n                <external>#CC0099</external>"
            +"\n                <verbose>#B0C4DE</verbose>"
            +"\n                <debug>#D8D8D8</debug>"
            +"\n                <normal>black</normal>"
            +"\n                <background>white</background>"
            +"\n            </color>"
            +"\n        </asHTML> -->"
            +"\n        <!-- logfiles normally are written to memory and only at the end of"
            +"\n             a test run to disk, disable that and write directly to disk -->"
            +"\n        <!--<disableMemoryLogMode>-->"
            +"\n    </log>"
            +"\n    <!-- local system configuration, xtt is executed here-->"
            +"\n    <system>"
            +"\n        <!-- the ip address of the machine XTT is running on -->"
            +"\n        <Ip>"+getProperty("SYSTEM/IP")+"</Ip>"
            +"\n        <!-- the ip address of the machine RemoteXTT is running on -->"
            +"\n        <!-- if not set default is xmg/ip -->"
            +"\n        <remoteip>127.0.0.1</remoteip>"
            +"\n        <!-- the port on which RemoteXTT is listening on -->"
            +"\n        <!-- if not set default is 8999 -->"
            +"\n        <remoteport>8999</remoteport>"
            +"\n        <!-- if you have a very slow network use this to prevent"
            +"\n             modules from reading from the network to fast -->"
            +"\n        <networklagdelay>100</networklagdelay>"
            +"\n        <hive>"
            +"\n            <!-- IMPORTANT! DO SET THIS TO CONTACT INFORMATION, A NAME OF WHO IS RUNNING THIS XTT -->"
            +"\n            <comment>Unknown Person Responsible</comment>"
            +"\n            <!--assimilate/-->"
            +"\n            <!--enabletracing/-->"
            +"\n            <!--disable/-->"
            +"\n        </hive>"
            +"\n    </system>";
    }
}

