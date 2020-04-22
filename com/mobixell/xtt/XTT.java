package com.mobixell.xtt;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.SplashScreen;
import java.io.File;
import java.io.OutputStream;
import java.util.Vector;
import java.util.Iterator;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;

import org.jdom.Document;

import com.mobixell.xtt.gui.main.XTTGui;
import com.mobixell.xtt.hive.Drone;
import com.mobixell.xtt.hive.Hive;
import com.mobixell.xtt.util.OSUtils;
/**
 * XTT Runs the main processes for XTT.
 *
 * @author      Gavin Cattell & Guy Bitan
 * @version     $Revision: 1.43 $
 */
public class XTT
{

    public static final String tantau_sccsid = "@(#)$Id: XTT.java,v 1.43 2010/07/09 10:50:32 mlichtin Exp $";
    private static Parser parser=null;public static Parser getParser(){return parser;}
    private static int failedTests = 0;
    private static XTTStatusServer statusServer = null;
    private static boolean testRunning = false;
    private Vector<String> loadConfig=new Vector<String>();
    private boolean doSelfTest = false;
    private File exportCSVFile=null;
    /**
    *
    * Run the main application
    */
    public static void main(String a[])
    {
        try
        {
            java.security.Provider xtt_provider = new XTTCrypto();
            checkVersion();
            checkRoot();
            setColor();
            XTT application = new XTT(a);
        }
        catch(Exception e)
        {
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
            System.exit(XTTProperties.getExitCode());
        }
    }
    public static void setColor()
    {
    	 UIManager.put("Frame.background",        new Color(0xf6, 0xf6, 0xf6));
    	 UIManager.put("OptionPane.background",   new Color(0xf6, 0xf6, 0xf6));
         UIManager.put("Panel.background",        new Color(0xf6, 0xf6, 0xf6));
         UIManager.put("Button.background",       new Color(0xf6, 0xf6, 0xf6)); 
         UIManager.put("ScrollPane.background",   new Color(0xf6, 0xf6, 0xf6));
         UIManager.put("Label.background",        new Color(0xf6, 0xf6, 0xf6));
         UIManager.put("ToggleButton.background", new ColorUIResource(0xf6, 0xf6, 0xf6));
         UIManager.put("MenuBar.background",      new ColorUIResource(0xf6, 0xf6, 0xf6)); 
         UIManager.put("Viewport.background",     new ColorUIResource(0xf6, 0xf6, 0xf6)); 
         UIManager.put("ComboBox.listBackground", new ColorUIResource(0xf6, 0xf6, 0xf6));
         UIManager.put("ComboBox.background",     new ColorUIResource(0xf6, 0xf6, 0xf6));
         UIManager.put("Table.background",        new ColorUIResource(0xf6, 0xf6, 0xf6));
         UIManager.put("RadioButton.background",  new ColorUIResource(0xf6, 0xf6, 0xf6));
         UIManager.put("List.background",         new ColorUIResource(0xf6, 0xf6, 0xf6));
         UIManager.put("TableHeader.background",  new ColorUIResource(0xf0, 0xf0, 0xf6));
         UIManager.put("Table.background",        new ColorUIResource(0xf6, 0xf6, 0xf6));
         UIManager.put("SplitPane.background",    new ColorUIResource(0xf6, 0xf6, 0xf6));
         UIManager.put("ScrollBar.background",    new ColorUIResource(0xf6, 0xf6, 0xf6));
         UIManager.put("CheckBox.background",     new ColorUIResource(0xf6, 0xf6, 0xf6));
    }
    public static void checkVersion()
    {
        String version=System.getProperties().getProperty("java.specification.version");
        String[] versionParts=version.split("\\.");
        int major=Integer.decode(versionParts[0]);
        int minor=Integer.decode(versionParts[1]);
        if(major>1||(major==1&&minor>6))
        {
            XTTProperties.printWarn("WARNING: XTT has not been tested with java "+version+" use 1.6"
                +"\n  java.class.version"+"           : "+System.getProperties().getProperty("java.class.version")
                +"\n  java.vm.specification.version"+": "+System.getProperties().getProperty("java.vm.specification.version")
                +"\n  java.specification.version"+"   : "+System.getProperties().getProperty("java.specification.version")
                +"\n  java.version"+"                 : "+System.getProperties().getProperty("java.version")
                +"\n  java.vm.version"+"              : "+System.getProperties().getProperty("java.vm.version"));
        }
        
    }
    public static void checkRoot()
    {
    	XTTProperties.printDebug(
    							"Host: " + OSUtils.getHostName() +"\n" + 
    							"   IP: " + OSUtils.getIpAddr() +"\n" +
    						    "   LoginUser: " + OSUtils.getLoginUser() +"\n" + 
		    					"   OSArch: " + OSUtils.getOSArch()+"\n" + 
		    					"   OSName: " + OSUtils.getOSName()+"\n" +
		    					"   OSVersion: " + OSUtils.getOSVer()+"\n"
		    					);
    	if (!OSUtils.checkRootUser())
    	{
    		 XTTProperties.printWarn("OS Login User must be root user !");
    		 JOptionPane.showMessageDialog(null,"OS Login User must be root user","XTT Warning",JOptionPane.WARNING_MESSAGE);
    		 System.exit(XTTProperties.getExitCode());
    	}
    }

    public XTT(String [] a)
    {
        statusServer = new XTTStatusServer();
        
        XTTProperties.setXTT(this);
        
        XTTProperties.initOutputStream();
        XTTProperties.setXTTBuildVersion();
        
        loadCommandLineConfiguration(a); //Configure XTT

        try
        {
            if(doSelfTest)XTTProperties.setTracing(XTTProperties.FAIL);
            parser = new Parser(false);
            String generatedConfiguration = parser.getConfigurationOptions();        
            XTTConfigurationLocalPermanent baseConfiguration = new XTTConfigurationLocalPermanent();
            baseConfiguration.setDocument(generatedConfiguration);
            if(!baseConfiguration.add())
            {
                XTTProperties.printFail("Error loading base configuration");    
            }
        }
        catch(Exception e)
        {
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }                
        }
        
        Iterator<String> configs=loadConfig.iterator();
        while(configs.hasNext())
        {
            XTTProperties.loadMainConfiguration(configs.next());
        }

        if(doSelfTest)selfTest();
        
        
        if(XTTProperties.getXTTGui()!=null)
        {
            XTTProperties.getXTTGui().refreshConfigurations();
            	processSplashScreen();
            XTTProperties.getXTTGui().display();
            XTTProperties.getXTTGui().runTestLauncher();
        }
        
        parser.doInit();
        
        /*
            Don't start anything above this point, where you've finished loading the config.
        */
        
        statusServer.start();
        
        if(XTTProperties.getXTTGui()==null)
        {
            runTests(false);
            System.exit(XTTProperties.getExitCode());
        }
    }
    public void runTestFromTestLauncher(org.jdom.Document test,boolean isTestLauncher,boolean isRunAll)
    {
    	XTTXML.stringXML(test);
        testRunning = true;
        OutputStream textlogstream=null;
        
        if(XTTProperties.getXTTGui()!=null)
        {
        	XTTProperties.getXTTGui().updateIcon(true);
        }
        try
        {
            if(!XTTProperties.checkConfiguration()) 
            {
                XTTProperties.printFail("Test execution aborted since no configuration was loaded");
                throw new Exception("Test execution aborted since no configuration was loaded");
            }
            XTTProperties.startLog();
            if(!XTTProperties.isMemoryLoggingEnabled())
            {
                textlogstream=XTTProperties.getNewLogStream();
                XTTProperties.getOutputStream().addOutputStream(textlogstream);
            }
            Parser parser = new Parser();

            String timeString;
            String hashString = "########################################";

            while(XTTProperties.advanceTestList())
            {
                XTTProperties.startCurrentTest();

                XTTProperties.printDebug("Starting Test: " + XTTProperties.getCurrentTestFileName());
                if (test != null)
                {
                    parser.runTest(test,isTestLauncher,isRunAll);
                } else
                {
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_PARSER_ERROR);
                }
                XTTProperties.printDebug("Finished Running Test: " + XTTProperties.getCurrentTestFileName());

                XTTProperties.endCurrentTest();

                if(XTTProperties.printInfo(null))
                {
                    timeString = XTTProperties.getCurrentTestElapsedTime();
                    timeString = " ELAPSED TIME: " + timeString + " ";
                    int hashes = (79 - timeString.length()) / 2;
                    if( (timeString.length() % 2) == 0)  timeString += "#";
                    XTTProperties.appendCurrentTestLog(hashString.substring(0,hashes) + timeString + hashString.substring(0,hashes));
                    System.out.println(hashString.substring(0,hashes) + timeString + hashString.substring(0,hashes));
                }

                if(XTTProperties.abortTestList())
                {
                    return;
                }

            }
            XTTProperties.stopLog();
        } catch(Exception e)
        {
            e.printStackTrace();
            if(XTTProperties.getXTTGui()!=null)
            {
                XTTProperties.getXTTGui().showError("RUN Error",e.getClass().getName()+"\n"+e.getMessage()+"\n\nAborting Test Run!");
            }
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
            if(XTTProperties.getXTTGui()==null)
            {
                System.exit(XTTProperties.getExitCode());
            }
            return;
        } finally
        {
            XTTProperties.finishLog();
            if(exportCSVFile!=null)
            {
                try
                {
                    XTTProperties.exportTestResults(exportCSVFile);
                } catch (Exception e)
                {
                    XTTProperties.printException(e);
                }
            }
            testRunning = false;
            XTTProperties.resetTestList();
            if(textlogstream!=null)
            {
                XTTProperties.getOutputStream().removeOutputStream(textlogstream);
                try
                {
                    textlogstream.flush();
                    textlogstream.close();
                } catch (Exception e)
                {
                    //don't care
                }
            }
        }
    }
    public void runTests(boolean isTestLauncher)
    {
        testRunning = true;
        OutputStream textlogstream=null;
        try
        {
            if(!XTTProperties.checkConfiguration()) 
            {
                XTTProperties.printFail("Test execution aborted since no configuration was loaded");
                throw new Exception("Test execution aborted since no configuration was loaded");
            }
            Document test;
            XTTProperties.startLog();
            if(!XTTProperties.isMemoryLoggingEnabled())
            {
                textlogstream=XTTProperties.getNewLogStream();
                XTTProperties.getOutputStream().addOutputStream(textlogstream);
            }
            Parser parser = new Parser();

            String timeString;
            String hashString = "########################################";

            while(XTTProperties.advanceTestList())
            {
                XTTProperties.startCurrentTest();

                XTTProperties.printDebug("Starting Test: " + XTTProperties.getCurrentTestFileName());
                test = XTTXML.getCurrentTestDocument();
                if (test != null)
                {
                    parser.runTest(test,isTestLauncher,true);
                } else
                {
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_PARSER_ERROR);
                }
                XTTProperties.printDebug("Finished Running Test: " + XTTProperties.getCurrentTestFileName());

                XTTProperties.endCurrentTest();

                if(XTTProperties.printInfo(null))
                {
                    timeString = XTTProperties.getCurrentTestElapsedTime();
                    timeString = " ELAPSED TIME: " + timeString + " ";
                    int hashes = (79 - timeString.length()) / 2;
                    if( (timeString.length() % 2) == 0)  timeString += "#";
                    XTTProperties.appendCurrentTestLog(hashString.substring(0,hashes) + timeString + hashString.substring(0,hashes));
                    System.out.println(hashString.substring(0,hashes) + timeString + hashString.substring(0,hashes));
                }

                if(XTTProperties.abortTestList())
                {
                    return;
                }

            }
            XTTProperties.stopLog();
            if (XTTProperties.getNumberOfTests() == 0)
            {
                XTTProperties.printFail("No tests to run");
            } else
            {
                failedTests = XTTProperties.showResults();
            }

        } catch(Exception e)
        {
            e.printStackTrace();
            if(XTTProperties.getXTTGui()!=null)
            {
                XTTProperties.getXTTGui().showError("RUN Error",e.getClass().getName()+"\n"+e.getMessage()+"\n\nAborting Test Run!");
            }
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
            if(XTTProperties.getXTTGui()==null)
            {
                System.exit(XTTProperties.getExitCode());
            }
            return;
        } finally
        {
            XTTProperties.finishLog();
            if(exportCSVFile!=null)
            {
                try
                {
                    XTTProperties.exportTestResults(exportCSVFile);
                } catch (Exception e)
                {
                    XTTProperties.printException(e);
                }
            }
            testRunning = false;
            XTTProperties.resetTestList();
            if(textlogstream!=null)
            {
                XTTProperties.getOutputStream().removeOutputStream(textlogstream);
                try
                {
                    textlogstream.flush();
                    textlogstream.close();
                } catch (Exception e)
                {
                    //don't care
                }
            }
        }
    }

    /**
    *
    * loads the stuff from the command line
    */
    private void loadCommandLineConfiguration(String [] a)
    {
        if (a.length == 0)
        {
            showHelp();
        }
        if (a.length == 1&&(a[0].equalsIgnoreCase("--dumpfunctions")||a[0].equalsIgnoreCase("-d")))
        {
            try
            {
                XTTProperties.setTracing(XTTProperties.FAIL);
                XTTProperties.FAILCOMMENT="";
                XTTProperties.setPrintFormat("%m");
                parser = new Parser();
                parser.dumpFunctions();
            }
            catch(Exception e)
            {
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
            }
            System.exit(0);
        }
        try
        {
            for (int i = 0; i<a.length ; i++) //Loop around the arguments
            {
                if ((a[i].equalsIgnoreCase("--gui"))||(a[i].equalsIgnoreCase("-g")))
                {
                    try
                    {
                        XTTProperties.setXTTGui(new XTTGui());
                    } catch (Exception ex)
                    {
                        XTTProperties.printException(ex);
                        throw ex;
                    }
                } 
                else if (a[i].equalsIgnoreCase("--noStartOnLoad"))
                {
                    XTTProperties.setStartOnLoad(false);
                }
            }

            for (int i = 0; i<a.length ; i++) //Loop around the arguments
            {
                if (a[i].equalsIgnoreCase("--help")) //Check each argument
                {
                    showHelp();
                } else if (a[i].equalsIgnoreCase("--selftest"))
                {
                    doSelfTest=true;
                } else if (a[i].equalsIgnoreCase("--noStartOnLoad"))
                {
                    //we already did this
                } else if (a[i].equalsIgnoreCase("--exportCSVTestResult"))
                {
                    if(XTTProperties.getXTTGui()!=null)
                    {
                        XTTProperties.printFail("--exportCSVTestResult option will be ignored when GUI is loaded!");
                        i++;
                    } else
                    {
                        String fname=a[++i];
                        if(!fname.endsWith(".csv"))fname=fname+".csv";
                        exportCSVFile=new File(fname);
                        if(exportCSVFile.exists())
                        {
                            XTTProperties.printWarn("Warning, overwriting file: "+exportCSVFile);
                        } else
                        {
                            exportCSVFile.createNewFile();
                        }
                        if(!exportCSVFile.canWrite())
                        {
                            XTTProperties.printFail("Error, can not write file: "+exportCSVFile);
                            showHelp();
                        } else if(!exportCSVFile.isFile())
                        {
                            XTTProperties.printFail("Error, not a propper file: "+exportCSVFile);
                            showHelp();
                        }
                    }
                    
                } else if ((a[i].equalsIgnoreCase("--config"))||(a[i].equalsIgnoreCase("-c")))
                {
                    if (!a[++i].endsWith(".xml"))
                    {
                        XTTProperties.printFail("Invalid file type, must be .XML");
                        if(XTTProperties.getXTTGui()==null)showHelp();
                    } else
                    {
                        //XTTProperties.loadMainConfiguration(a[i]);
                        loadConfig.add(a[i]);
                    }
                } else if ((a[i].equalsIgnoreCase("--list"))||(a[i].equalsIgnoreCase("-l")))
                {
                    XTTProperties.setTestList(new File (a[++i]));
                } else if ((a[i].equalsIgnoreCase("--single"))||(a[i].equalsIgnoreCase("-s")))
                {
                    XTTProperties.setTestList(a[++i],false);
                } else if ((a[i].equalsIgnoreCase("--gui"))||(a[i].equalsIgnoreCase("-g")))
                {
                    //we already did this
                } else
                {
                    showHelp();
                }
            }
        }
        catch (Exception e)
        {
            System.out.println("Error: Invalid command line option");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
                e.printStackTrace();
            }
            showHelp(); //If the command line options were bad, show the help
        }

    }

    private void selfTest()
    {
        try
        {
            XTTProperties.setStartOnLoad(false);
            
            XTTProperties.setTracing(XTTProperties.FAIL);

            Parser parser = new Parser();

            int start = 0;
            int end = 0;
            String version = "";

            System.out.println("eXtreme Test Tool (XTT)  - mobixell QA");
            System.out.println("Version: "+XTTProperties.getXTTBuildVersion()+"\n\n");

            start = this.tantau_sccsid.indexOf(",v ") + 3;
            end = this.tantau_sccsid.indexOf(" ",start+1);
            version = this.tantau_sccsid.substring(start,end);
            System.out.println("XTT" +  " - " +version);

            start = XTTProperties.tantau_sccsid.indexOf(",v ") + 3;
            end = XTTProperties.tantau_sccsid.indexOf(" ",start+1);
            version = XTTProperties.tantau_sccsid.substring(start,end);
            System.out.println("XTTProperties" +  " - " +version);

            parser.showVersions();

            if(!XTTProperties.getProperty("SYSTEM/IP").equalsIgnoreCase("null"))
            {
                System.out.println("\n\nResource Check:");
                java.util.Vector<String> moduleResources = parser.checkResources();
                for(int i=0;i<moduleResources.size();i++)
                {
                    System.out.println(moduleResources.get(i));        
                }
            }
            else
            {
                System.out.println("\n\nNot checking resources since no configuration has been loaded yet.");        
            }

            System.exit(1);
        }
        catch(Exception e)
        {
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
        }

    }

    /**
    *
    * print out the help
    */
    private void showHelp()
    {
        System.out.println("eXtreme Test Tool (XTT)  - mobixell QA");
        System.out.println("Version: "+XTTProperties.getXTTBuildVersion());
        System.out.println("");
        System.out.println("");
        System.out.println("Usage:");
        System.out.println("\t-c <config>, --config <config>");
        System.out.println("\t\tSpecify the file where the configuration is");
        System.out.println("");
        System.out.println("\t--dumpfunctions\n\t\tList all available functions of FunctionModules");
        System.out.println("");
        System.out.println("\t-g, --gui");
        System.out.println("\t\tLoads the interactive XTTGui");
        System.out.println("");
        System.out.println("\t--help\n\t\tThis help");
        System.out.println("");
        System.out.println("\t--nostartonload\n\t\tRemoves any 'start on load' options in a configuration.");
        System.out.println("");
        System.out.println("\t-l <tests>,--list <tests>");
        System.out.println("\t\tSpecify a test file list");
        System.out.println("");
        System.out.println("\t-s <test>,--sinlge <test>");
        System.out.println("\t\tRuns the test <test>");
        System.out.println("");
        System.out.println("\t--selftest\n\t\tRuns self tests, and shows version numbers");
        System.out.println("");
        System.out.println("\t--exportCSVTestResult <file.csv>\n\t\tExport test result to csv file after execution");
        System.out.println("");
        System.exit(1); //Exit and fail if you had to show this help
    }
    
    public static boolean isTestRunning()
    {
        return testRunning;
    }
    
    public static boolean isXTTOk()
    {
        return statusServer.isOk();    
    }

    public static int XTTSTATUSSERVERPORT=9881;

    private class XTTStatusServer extends Thread implements Runnable
    {
        private int port = XTTSTATUSSERVERPORT;
        private java.net.ServerSocket ss = null;
        
        private Hive theHive = null;

        public XTTStatusServer()
        {
            super("XTTStatusServer");
            try
            {
                ss = new java.net.ServerSocket();
                ss.setReuseAddress(true);
                ss.bind(new java.net.InetSocketAddress(port));
            }
            catch(java.net.BindException be)
            {
                ss = null;
                XTTProperties.printFail("XTT: Another version of XTT is already running.\nKeeping this version running might causes problems.");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
            catch(Exception e)
            {
                XTTProperties.printException(e);
            }
        }

        public boolean isOk()
        {
            if(ss != null)
            {
                return ss.isBound();
            }   
            return false; 
        }

        public void run()
        {
            if(!XTTProperties.getQuietProperty("SYSTEM/HIVE/DISABLE").equals("") && (XTTProperties.getXTTGui() != null))
            {
                theHive = new Hive("230.20.7.24",9882,Drone.XTTDRONE);
                theHive.start();            
                XTTProperties.setHive(theHive);
            }

            java.net.Socket s=null;

            while (ss!=null)
            {
                s = null;
                try
                {
                    s = ss.accept();
                }
                catch (java.io.IOException ioe)
                {
                    XTTProperties.printFail("XTT: A problem occured while someone was requesting the XTT status.");
                    if(XTTProperties.printDebug(null))
                    {
                        XTTProperties.printException(ioe);
                    }
                }
                try
                {
                    //HTTPHelper.readStream("XTT",new java.io.BufferedInputStream(s.getInputStream()),100);
                    java.io.PrintStream out = new java.io.PrintStream(s.getOutputStream());
                    out.println("XTT is running OK");
                    out.flush();
                }
                catch(Exception e)
                {
                    XTTProperties.printFail("XTT: A problem occured while someone was requesting the XTT status.");
                    if(XTTProperties.printDebug(null))
                    {
                        XTTProperties.printException(e);
                    }
                }
                finally
                {
                    try
                    {
                        s.close();
                    }
                    catch(Exception e)
                    {
                        //
                    }
                }
            }
        }
    }
    /**
	 * getting the SplashScreen object in runtime, and creating a new thread
	 * that will take the splash screen and write on it the required
	 * information. the splash will be closed when the application is up.
	 */
	private static void processSplashScreen() {
		(new Thread() {
			public void run() {
				SplashScreen ss = SplashScreen.getSplashScreen();
				
				if (ss != null) {
					try {	
						Graphics2D g2d = ss.createGraphics();
						// Set text color black and font type sansserif
						// Set string location just above the separator line
						g2d.setColor(new Color(0x23, 0x1f, 0x20));
						g2d.setFont(new Font("arial", Font.BOLD, 14));
						String version = "Version ";
						version += XTTProperties.getXTTBuildVersion();
						g2d.drawString(version, 101, 209);
						ss.update();

						// Set wait message
						g2d.setColor(new Color(0x10, 0x78, 0xca));
						g2d.setFont(new Font("arial", Font.BOLD, 14));
						g2d.drawString("XTT initializing, please wait...", 101, 230);
						ss.update();

						// Set copyright message
						g2d.setColor(new Color(0x23, 0x1f, 0x20));
						g2d.setFont(new Font("arial", Font.PLAIN, 10));
						g2d.drawString("© Copyright 20011-2012 Mobixell Ltd. All rights reserved.", 101, 330);
						ss.update();

						for (int j = 0; true; j++) {
							if (j % 2 == 0) {
								g2d.setColor(new Color(0xf6, 0xf6, 0xf6));
							} else {
								g2d.setColor(new Color(0xa8, 0xb7, 0xe2));
							}
							/**
							 * create the runing square's
							 */
							for (int i = 0; i < 10; i++) {
								g2d.fillRect(101 + (i * 20), 264, 10, 10);
								ss.update();
								Thread.sleep(400);
							}
						}
					} 
					catch (Exception e) {
						XTTProperties.printDebug("Splash Screen Closed");
					}
				} else {
					XTTProperties.printDebug("Splash screen not found");
					
				}

			}
		}).start();

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			return;
		}
		
	}
}
