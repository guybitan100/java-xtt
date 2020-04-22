package com.mobixell.xtt.gui.main;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.Vector;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;
import com.mobixell.xtt.FunctionModule;
import com.mobixell.xtt.FunctionModule_Remote;
import com.mobixell.xtt.ModuleList;
import com.mobixell.xtt.Parser;
import com.mobixell.xtt.XTT;
import com.mobixell.xtt.XTTConfiguration;
import com.mobixell.xtt.XTTConfigurationLocalPermanent;
import com.mobixell.xtt.XTTConfigurationRemote;
import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.gui.ChangeTracing;
import com.mobixell.xtt.gui.ConfigurationEditor;
import com.mobixell.xtt.gui.MessageListener;
import com.mobixell.xtt.gui.SaveByteStringAsBinary;
import com.mobixell.xtt.gui.VerticalFlowLayout;
import com.mobixell.xtt.gui.ViewHive;
import com.mobixell.xtt.gui.testlaunch.WDRemoteXTTWorker;
import com.mobixell.xtt.gui.testlaunch.ProgressController;
import com.mobixell.xtt.gui.testlaunch.TestLauncherGui;
import com.mobixell.xtt.gui.testlaunch.panels.ProcessListDialog;
import com.mobixell.xtt.gui.testlaunch.panels.ProductsListDialog;
import com.mobixell.xtt.gui.testlaunch.panels.ProgressPanel;
import com.mobixell.xtt.gui.testlaunch.panels.RunnerFileTreePanel;
import com.mobixell.xtt.images.ImageCenter;
/**
 * Central Swing GUI Class for XTT
 *
 * @author      Roger Soder
 * @version     $Id: XTTGui.java,v 1.78 2009/05/29 11:01:21 rsoder Exp $
 */
public class XTTGui extends JFrame implements WindowListener,ActionListener
{
	private static final long serialVersionUID = 2324986019887486853L;
	public static final String tantau_sccsid = "@(#)$Id: XTTGui.java,v 1.78 2009/05/29 11:01:21 rsoder Exp $";
    private XTTGui xttgui                    = this;
    private TestLauncherGui testLauncher = null;
    public ConfigurationBar configurationBar= null;
    private XTTMenuBar menuBar               = null;
    private boolean enabled                  = true;
    private TestList testlist                = null;
    private static RunnerFileTreePanel     runnerTreePanel    = null;
    private TestListBar testlistbar          = null;
    private JMenu configmenu                 = null;
    private int failedCount                  = 0;
    private int passedCount                  = 0;
    private boolean doabort                  = false;
    private boolean doforceabort             = false;
    private ModuleList modules               = null;
    private Parser parser                    = null;
    
    ChangeTracing changeTracing              = null;

    public final static int LIST_UNKNOWN     = 0;
    public final static int LIST_LOADED      = 1;
    public final static int LIST_SAVED       = 2;
    public final static int LIST_EDITED      = -1;
    public final static int LIST_FAILED      = -10;
    public final static int SUBWINDOWX=1800;
    public final static int SUBWINDOWY=900;
    private MessageListener messagelistener=new MessageListener(this);
    //private JToolBar toolBar;
    public static JButton upButton, topButton, downButton, bottomButton, deleteButton;
	public static JButton openTestButton, saveTestButton, saveAsTestButton,deleteAllButton,stopTestButton;
	public static JButton showLogButton;
	public static JCheckBox repeatCheckBox;
	public static JTextField repeatAmount;
	public static JTextField repeatAmountLeft;
   //private JLabel repAmountLeft;
    public ProgressPanel progressPanel;
    public ProgressController progressController;
    public Image im;
    public XTTGui()
    {
       updateIcon(true);

    	try {
			UIManager.setLookAndFeel(new PlasticXPLookAndFeel());
			SwingUtilities.updateComponentTreeUI(this);
		} catch (Exception e) {
			System.out.println("Error setting UI Look and Feel");
		}
        parser=XTT.getParser();
        this.setTitle("XTT - "+XTTProperties.getXTTBuildVersion()+XTTProperties.getXTTBuildTimeStamp());
        this.addWindowListener(this);
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        
        testlist=new TestList(this);
        runnerTreePanel = new RunnerFileTreePanel(testlist);
        testlistbar=new TestListBar();
        testlist.add(testlistbar,BorderLayout.SOUTH);
        menuBar=new XTTMenuBar();
        this.setJMenuBar(menuBar);
        configurationBar=new ConfigurationBar();

        new WDRemoteXTTWorker(this,true).start();
     //   toolBar = createToolBar();
        
        JPanel testListPanel = new JPanel(new BorderLayout()); 
       // testListPanel.add(toolBar,BorderLayout.NORTH);
        testListPanel.add(testlist,BorderLayout.CENTER);
        
        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(testListPanel,BorderLayout.EAST);
        this.getContentPane().add(runnerTreePanel,BorderLayout.WEST);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,runnerTreePanel,testListPanel);
        splitPane.setDividerLocation(420);
		splitPane.setOneTouchExpandable(true);
		add(splitPane, BorderLayout.CENTER);   
    }

    public static String getConfigurationOptions()
    {
    			//Get the default toolkit
    			Toolkit toolkit = Toolkit.getDefaultToolkit();
    			Dimension scrnsize = toolkit.getScreenSize();
    			int width = (int)scrnsize.getWidth()-10;
    			int height = (int)scrnsize.getHeight()-30;
				String guiwidth = "";
				String guiheigth = "";
				if (width > 0)	 guiwidth =  "\n            <width>" + width + "</width>";
				else guiwidth =  			 "\n            <width>600</width>";
				if (height > 0)  guiheigth = "\n            <heigth>" + height + "</heigth>";
				else             guiheigth = "\n            <heigth>800</heigth>";
    			
        return "\n    <!-- xtt gui configuration -->"
              +"\n    <gui>"
              +"\n        <!-- position on screen, if outside the screen it will be repositioned inside -->"
              +"\n        <position>"
              +"\n            <x>200</x>"
              +"\n            <y>20</y>"
              +"\n        </position>"
              +"\n        <!-- size of the gui when starting up -->"
              +"\n        <size>"
              +           guiwidth
              +           guiheigth
              +"\n        </size>"
              +"\n        <!-- size of the columns when starting up -->"
              +"\n        <columns>"
              +"\n            <!--filename>200</filename-->"
              +"\n            <!--description>200</description-->"
              +"\n        </columns>"
              +"\n        <messaging>"
              +"\n            <popupListEnabled/>"
              +"\n            <!--"
              +"\n            <popupMessageEnabled/>"
              +"\n            <xttToFrontEnabled/>"
              +"\n            -->"
              +"\n        </messaging>"
              +"\n        <!--exportDelimiter>;</exportDelimiter-->"
              +"\n        <!-- use on your own risk, we do not help you if you have problems when this is enabled -->"
              +"\n        <!--disableTestRunLock/-->"
              +"\n        <!-- this will force the current test to stop it's execution between functions when the test run is aborted -->"
              +"\n        <!--forcedTestAbort/-->"
              +"\n    </gui>";
    }

    public void display()
    {
        try
        {
            Rectangle virtualBounds = new Rectangle();
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            //System.out.println(ge);
            GraphicsDevice[] gs    = ge.getScreenDevices();
            for (int j = 0; j < gs.length; j++)
            {
                GraphicsDevice gd = gs[j];
                XTTProperties.printDebug("Gui: Detecting screen: "+gd);
                GraphicsConfiguration gc = gd.getDefaultConfiguration();
                virtualBounds = virtualBounds.union(gc.getBounds());
            }
            int minX=(int)virtualBounds.getX();
            int maxX=(int)virtualBounds.getWidth();
            int minY=(int)virtualBounds.getY();
            int maxY=(int)virtualBounds.getHeight();
            XTTProperties.printDebug("Gui: Virtual Bounds  : x="+minX+","+maxX+" y="+minY+","+maxY);
            int xSize=XTTProperties.getIntProperty("GUI/SIZE/WIDTH");
            int ySize=XTTProperties.getIntProperty("GUI/SIZE/HEIGTH");
            if(xSize!=-1&&ySize!=-1)
            {
                if(xSize>maxX-minX)
                {
                    xSize=maxX-minX;
                }
                if(ySize>maxY-minY)
                {
                    ySize=maxY-minY;
                }
                XTTProperties.printDebug("Gui: Setting size    : width="+xSize+" heigth="+ySize);
                this.setSize(xSize,ySize);
            } else
            {
                XTTProperties.printDebug("Gui: Setting size    : width=800 heigth=500");
                this.setSize(800,500);
            }

            String xLocation=XTTProperties.getProperty("GUI/POSITION/X");
            String yLocation=XTTProperties.getProperty("GUI/POSITION/Y");
            
            if(xLocation==null||yLocation==null||xLocation.equals("null")||yLocation.equals("null"))
            {
                this.setLocationRelativeTo(null); //center it
            } 
            else
            {
                try
                {
                    int xLoc=Integer.decode(XTTProperties.getProperty("GUI/POSITION/X")).intValue();
                    int yLoc=Integer.decode(XTTProperties.getProperty("GUI/POSITION/Y")).intValue();
                    //if(xLoc<0)xLoc=maxX+xLoc;
                    //if(yLoc<0)yLoc=maxY+yLoc;
                    if(xLoc<minX)xLoc=minX;
                    if(yLoc<minY)yLoc=minY;
                    if(xLoc>maxX-xSize)xLoc=maxX-xSize;
                    if(yLoc>maxY-ySize)yLoc=maxY-ySize;
                    this.setLocation(xLoc,yLoc);
                    XTTProperties.printDebug("Gui: Setting position: x=" + xLoc + " y=" + yLoc);
                } 
                catch (Exception e)
                {
                    this.setLocationRelativeTo(null); //center it
                }
            }
            int columnfilename=XTTProperties.getIntProperty("GUI/COLUMNS/FILENAME");
            if(columnfilename>=0)
            {
                testlist.setPreferedFilenameWidth(columnfilename);
            }
            int columndesc=XTTProperties.getIntProperty("GUI/COLUMNS/DESCRIPTION");
            if(columndesc>=0)
            {
                testlist.setPreferedDescriptionWidth(columndesc);
            }
            String property=XTTProperties.getProperty("GUI/MESSAGING/POPUPLISTENABLED");
            if(!property.equals("null"))
            {
                messagelistener.setPopupList(true);
            }
            property=XTTProperties.getProperty("GUI/MESSAGING/POPUPMESSAGEENABLED");
            if(!property.equals("null"))
            {
                messagelistener.setPopupMessage(true);
            }
            property=XTTProperties.getProperty("GUI/MESSAGING/XTTTOFRONTENABLED");
            if(!property.equals("null"))
            {
                messagelistener.setXttToFront(true);
            }
            property=XTTProperties.getProperty("GUI/DISABLETESTRUNLOCK");
            if(!property.equals("null"))
            {
                testlist.setRunModality(false);
            }
            property=XTTProperties.getProperty("GUI/FORCEDTESTABORT");
            if(!property.equals("null"))
            {
                doforceabort=true;
            }
            
        } catch (Exception ex)
        {
            XTTProperties.printWarn("Gui: error when setting custom settings: "+ex.getMessage());
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(ex);
            }
            this.pack();
            this.setLocationRelativeTo(null); //center it
        }
        try
        {
            if(parser==null)parser=new Parser();
            if(modules==null)modules=new ModuleList(parser);
        } catch (Exception ex)
        {
            //ex.printStackTrace();
            showError("Parser Error",ex.getClass().getName()+"\n"+ex.getMessage());
            return;
        }
    }

	public synchronized void updateIcon(boolean isOK) {
		if (isOK)
			im = Toolkit.getDefaultToolkit().getImage(
					ImageCenter.getInstance().getImageUrl(
							ImageCenter.ICON_XTT_GREEN));
		else
			im = Toolkit.getDefaultToolkit().getImage(
					ImageCenter.getInstance().getImageUrl(
							ImageCenter.ICON_XTT_RED));

		if (getIconImage()==null)
			setIconImage(im);	
		
		if (!getIconImage().equals(im))
		{
			setIconImage(im);
			repaint();
			if (testLauncher != null) {
				testLauncher.setIconImage(im);
				testLauncher.repaint();
			}
		}	
	}
    public void actionPerformed(ActionEvent e)
    {
        if(e.getActionCommand().equals("Exit"))
        {
           this.setVisible(false);
        }
    }

    public void doDisable()
    {
        enabled(false);
    }
    public void doEnable()
    {
        enabled(true);
    }
    private void enabled(boolean state)
    {
        enabled=state;
        menuBar.enabled(state);
    }
    
    public void doWaitForButton(String message)
    {
        JOptionPane.showMessageDialog(this,
            message,
            "XTT wait for button",
            JOptionPane.WARNING_MESSAGE);
    }

    public void windowActivated(WindowEvent e)
    {
        configurationBar.checkConfigChanged();
    }
    public void windowClosed(WindowEvent e)
    {
        if(enabled) this.setVisible(false);
    }
    public void windowClosing(WindowEvent e)
    {
        if(enabled) this.setVisible(false);
    }
    public void windowDeactivated(WindowEvent e){}
    public void windowDeiconified(WindowEvent e){}
    public void windowIconified(WindowEvent e){}
    public void windowOpened(WindowEvent e){}
    public void addTests(java.io.File[] files)
    {
        testlist.addTests(files,false);
    }
    public void showConfigurationError(String errortext)
    {
        showError("Configuration load error",errortext+"\n\nConfiguration NOT loaded!");
    }
    public void showConfigurationWarning(String errortext)
    {
        JOptionPane.showMessageDialog(this,
            errortext,
            "Configuration load warning",
            JOptionPane.WARNING_MESSAGE);
    }

    public void showError(String title,String errortext)
    {
        JOptionPane.showMessageDialog(this,
            errortext,
            title,
            JOptionPane.ERROR_MESSAGE);
    }
    public void showError(JFrame parent,String title,String errortext)
    {
        JOptionPane.showMessageDialog(parent,
            errortext,
            title,
            JOptionPane.ERROR_MESSAGE);
    }
    public void refreshConfigurations()
    {
        configurationBar.refreshConfigurations();
        if(parser!=null)parser.doInit();
    }
	public void runTestLauncher() 
	{
		if (testLauncher != null && testLauncher.isVisible()) 
		{
				testLauncher.requestFocus();
		} 
		else 
		{
			try {
				if (parser == null)  parser = new Parser();
				if (modules == null) modules = new ModuleList(parser);
				testLauncher = new TestLauncherGui(xttgui, modules, parser);
				testLauncher.requestFocus();
			} catch (Exception ex) 
			{
				ex.printStackTrace();
				showError("Parser Error",ex.getClass().getName() + "\n" + ex.getMessage());
				return;
			}
		}
	}
	private class ConfigurationBar extends JPanel implements ActionListener
    {
		private static final long serialVersionUID = -7432833498938207301L;
		JLabel configStatusLabel=new JLabel();
        JButton statusButton=new JButton("Edit");
        JButton remotesButton=new JButton("Remote");
        JPopupMenu popupConfig = new JPopupMenu();
        JPopupMenu popupRemote = new JPopupMenu();
        public ConfigurationBar()
        {
            this.setLayout(new BorderLayout());
            
            JPanel statusPanel = new JPanel(new FlowLayout());
            
            statusPanel.add(statusButton);
            statusPanel.add(configStatusLabel);
            statusPanel.add(remotesButton);
            
            statusButton.setPreferredSize(new Dimension(90+6,(int)statusButton.getPreferredSize().getHeight()));
            statusButton.setBackground(Color.RED);
            statusButton.setOpaque(true);
            statusButton.setHorizontalAlignment(SwingConstants.CENTER);
            statusButton.setActionCommand("CONFIG");
            statusButton.addActionListener(this);
            
            remotesButton.setPreferredSize(new Dimension(90+6,(int)statusButton.getPreferredSize().getHeight()));
            remotesButton.setBackground(Color.RED);
            remotesButton.setOpaque(true);
            remotesButton.setHorizontalAlignment(SwingConstants.CENTER);
            remotesButton.setActionCommand("REMOTE");
            
            this.add(statusPanel,BorderLayout.WEST);
            refreshConfigurations();
            this.add(testlist.getMemoryBar(),BorderLayout.EAST);
        }
        public void refreshConfigurations()
        {
            popupConfig.removeAll();
            configmenu.removeAll();
            
            JMenuItem menuItem=null;

            menuItem=new JMenuItem("Reload Local Configurations");
            menuItem.setActionCommand("RELOADCONFIG");
            menuItem.addActionListener(this);
            popupConfig.add(menuItem);
            
            menuItem=new JMenuItem("Reload Local Configurations");
            menuItem.setActionCommand("RELOADCONFIG");
            menuItem.addActionListener(this);
            configmenu.add(menuItem);

            menuItem=new JMenuItem("Add Local Configuration");
            menuItem.setActionCommand("ADDCONFIG");
            menuItem.addActionListener(this);
            popupConfig.add(menuItem);
            menuItem=new JMenuItem("Add Local Configuration");
            menuItem.setActionCommand("ADDCONFIG");
            menuItem.addActionListener(this);
            configmenu.add(menuItem);

            popupConfig.addSeparator();
            configmenu.addSeparator();
            
            for(XTTConfiguration config :  XTTConfigurationRemote.getConfigurations())
            {
                menuItem=new ConfigMenuItem(config);
                popupConfig.add(menuItem);
                menuItem=new ConfigMenuItem(config);
                configmenu.add(menuItem);
            }

            popupConfig.addSeparator();
            for(XTTConfiguration config :  XTTConfigurationLocalPermanent.getConfigurations())
            {
                menuItem=new ConfigMenuItem(config);
                popupConfig.add(menuItem);
                menuItem=new ConfigMenuItem(config);
                configmenu.add(menuItem);
            }
            
            String remoteStatus = "";
            
            if (XTTProperties.isRemoteXTTRunning)
            {
            	 popupRemote.removeAll();
            	 remotesButton.addActionListener(this);
            	 menuItem=new JMenuItem("XMP Process");
                 menuItem.setActionCommand("PROCESS");
                 menuItem.addActionListener(this);
                 popupRemote.add(menuItem);
                
                 menuItem=new JMenuItem("XMP Products Versions");
                 menuItem.setActionCommand("VERSIONS");
                 menuItem.addActionListener(this);
                 popupRemote.add(menuItem);
                
            	 remoteStatus = " ,Status: Running]";
            	 remotesButton.setBackground(Color.GREEN);
            }
            else
            {
            	 remotesButton.removeActionListener(this);
            	 popupRemote.removeAll();
            	 remoteStatus += " ,Status: Stoped]";
            	 remotesButton.setBackground(Color.RED);
            }
            configStatusLabel.setText(" Local [ File: "+
            							XTTConfiguration.getNumberofPermanentLocalConfigurations()
            							+" ] Remote: [ IP: " + XTTProperties.getProperty("system/remoteip") + " , Conf File: " + 
            							XTTConfiguration.getNumberofRemoteConfigurations() + remoteStatus);
            if(XTTConfiguration.getNumberofPermanentLocalConfigurations()<=0)
            {
                statusButton.setBackground(Color.RED);
                statusButton.setText(" SET ");
            } else
            {
                statusButton.setBackground(Color.GREEN);
                statusButton.setText("OK");
            }
        }
        public void actionPerformed(ActionEvent e)
        {
            if(e.getActionCommand().equals("CONFIG"))
            {
                int x=((java.awt.Component)e.getSource()).getX()+4;//- (int)((java.awt.Component)e.getSource()).getSize().getWidth();
                int y=((java.awt.Component)e.getSource()).getY()+4;
                popupConfig.show((java.awt.Component)e.getSource(),x,y);
            }
            else if(e.getActionCommand().equals("REMOTE"))
            {
            	  int x=(statusButton.getX())+8;//- (int)((java.awt.Component)e.getSource()).getSize().getWidth();
                  int y=(statusButton.getY())+4;
                  
                  if (statusButton.getBackground().equals(Color.GREEN))
                  {
                	  popupRemote.show((java.awt.Component)e.getSource(),x,y);  
                  }
            }
            else if(e.getActionCommand().equals("PROCESS"))
            {
            	 ProcessListDialog.getInstance();
            }
            else if(e.getActionCommand().equals("VERSIONS"))
            {
            	 ProductsListDialog.getInstance();
            }
            else if(e.getActionCommand().equals("RELOADCONFIG"))
            {
                if(XTT.isTestRunning())
                {
                    showError("Run Error","Tests are running, can't reload configuration!");
                    return;
                }
                XTTProperties.printDebug("Re-Loading configuration");
                XTTProperties.reloadConfiguration();
                if(XTTConfiguration.getNumberofPermanentLocalConfigurations()<=0)
                {
                    statusButton.setBackground(Color.RED);
                    statusButton.setText(" SET ");
                } else
                {
                    statusButton.setBackground(Color.GREEN);
                    statusButton.setText("OK");
                }
            }else if(e.getActionCommand().equals("ADDCONFIG"))
            {
                try
                {
                    if(parser==null)parser=new Parser();
                    ConfigurationEditor ed=new ConfigurationEditor(xttgui,null,parser.getConfigurationOptions(),true);
                } catch (Exception ex)
                {
                    showError("Parser Error",ex.getClass().getName()+"\n"+ex.getMessage());
                    return;
                    //ex.printStackTrace();
                }
            }else if(e.getActionCommand().equals("Exit"))
            {
            }
        }
        public void checkConfigChanged()
        {
            if(XTTProperties.hasConfigurationChanged())
            {
                statusButton.setBackground(Color.YELLOW);
                statusButton.setText("EDITED");
            }
        }
    }
    
    private class ConfigMenuItem extends JMenu implements ActionListener
    {
		private static final long serialVersionUID = 5567565729515881211L;
		XTTConfiguration config=null;
        public ConfigMenuItem(XTTConfiguration config)
        {
            super(config.getName()+"("+config.getFileName()+")");
            this.config=config;
            JMenuItem menuItem=new JMenuItem("Edit");
            menuItem.setActionCommand("EDIT");
            menuItem.addActionListener(this);
            this.add(menuItem);
            menuItem=new JMenuItem("Remove");
            menuItem.setActionCommand("REMOVE");
            menuItem.addActionListener(this);
            this.add(menuItem);
            try
            {
                Class<?> c=Class.forName("com.mobixell.xtt.XTTConfigurationLocalPermanent");
                if(c.isInstance(config))
                {
                    this.setText("L: "+config.getName()+"("+config.getFileName()+")");
                } else
                {
                    this.setText("R: "+config.getName()+"("+config.getFileName()+")");
                }
            } catch(Exception  ex)
            {
                ex.printStackTrace();
            }
        }
        public void actionPerformed(ActionEvent e)
        {
            if(e.getActionCommand().equals("EDIT"))
            {
                try
                {
                    if(parser==null)parser=new Parser();
                    ConfigurationEditor ed=new ConfigurationEditor(xttgui,config,parser.getConfigurationOptions(),true);
                } catch (Exception ex)
                {
                    showError("Parser Error",ex.getClass().getName()+"\n"+ex.getMessage());
                    return;
                    //ex.printStackTrace();
                }
            } else if(e.getActionCommand().equals("REMOVE"))
            {
                config.remove();
            }
        }
    }

    public String createShortFileName(String fullPath)
    {
        String dir=(new File("")).getAbsolutePath()+File.separator;
        String fname=fullPath;
        if(File.separator.equals("\\"))
        {
            if(fullPath.toLowerCase().startsWith(dir.toLowerCase()))
            {
                fname=fullPath.substring(dir.length(),fullPath.length());
            }
        } else
        {
            if(fullPath.startsWith(dir))
            {
                fname=fullPath.substring(dir.length(),fullPath.length());
            }
        }
        return fname;
    }

    public void setListStatus(int status)
    {
        testlistbar.setListStatus(status);
    }
    public void setListFileName(String filePath)
    {
        testlistbar.setFileName(filePath);
        testlist.setFileName(filePath);
    }

    public void addTestStatus(int status, int maxcount)
    {
        if(status>=XTTProperties.FAILED)
        {
            failedCount++;
        } else if(status<=XTTProperties.PASSED)
        {
            passedCount++;
        }
        testlistbar.setStatus(failedCount,passedCount, maxcount);
    }
    public void resetStatus()
    {
        failedCount=0;
        passedCount=0;
        testlistbar.setStatus(0,0,0);
    }

    public void setMemoryBar()
    {
        testlist.setMemoryBar();
    }

    public boolean doAbort()
    {
        boolean retVal=doabort;
        doabort=false;
        return retVal;

    }
    public void setDoAbort()
    {
        doabort=true;
        if(doforceabort)
        {
            Parser.setAbortTestExecution(doabort);
        }
    }

	public void setDoforceabort(boolean doforceabort) {
		this.doforceabort = doforceabort;
	}

	private class TestListBar extends JPanel
    {
		private static final long serialVersionUID = -6588096979386989994L;
		JLabel filename=new JLabel();
        JButton status=new JButton("NO LIST");
        JButton runstatus=new JButton(" UNKNOWN ");
        String fileName="";
        JPanel nameBar=new JPanel();
        public TestListBar()
        {
            this.setLayout(new BorderLayout());
            nameBar.setLayout(new BorderLayout());
            this.add(status,BorderLayout.WEST);
            status.setPreferredSize(new Dimension(90+6,(int)status.getPreferredSize().getHeight()));
            status.setBackground(Color.GRAY);
            status.setOpaque(true);
            status.setHorizontalAlignment(SwingConstants.CENTER);

            runstatus.setPreferredSize(new Dimension(160,(int)runstatus.getPreferredSize().getHeight()));
            runstatus.setBackground(Color.GRAY);
            runstatus.setOpaque(true);
            runstatus.setHorizontalAlignment(SwingConstants.CENTER);
            runstatus.addActionListener(testlist);
            runstatus.setActionCommand("LOG");
            this.add(runstatus,BorderLayout.EAST);

            this.add(nameBar,BorderLayout.CENTER);
            nameBar.add(filename,BorderLayout.CENTER);
            nameBar.add(new JLabel(" List File: "),BorderLayout.WEST);
            status.addActionListener(testlist);
            status.setActionCommand("CONFIG");
        }
        public void setFileName(String file)
        {
            this.fileName=file;
            filename.setText(file);
            if(file.equals(""))
            {
                setListStatus(0);
            } else
            {
                setListStatus(1);
            }
            setStatus(0,0,0);
        }

        public void setListStatus(int liststatus)
        {
            if(liststatus==LIST_UNKNOWN)
            {
                status.setBackground(Color.GRAY);
                status.setText("NO LIST");
            } else if (liststatus==LIST_LOADED)
            {
                status.setBackground(Color.GREEN);
                status.setText("LOADED");
            } else if (liststatus==LIST_SAVED)
            {
                status.setBackground(Color.GREEN);
                status.setText("SAVED");
            } else if(liststatus==LIST_EDITED)
            {
                status.setBackground(Color.YELLOW);
                status.setText("EDITED");
            } else
            {
                status.setBackground(Color.RED);
                status.setText("FAILED");
            }
        }

        public void setStatus(int failed,int passed,int maxcount)
        {
            if(failed>0)
            {
                runstatus.setBackground(Color.RED);
                runstatus.setText(failed+" of "+(failed+passed)+" FAILED");
            } else if(passed>0)
            {
                runstatus.setBackground(Color.GREEN);
                runstatus.setText(passed+" of "+(failed+passed)+" PASSED");
            } else
            {
                runstatus.setBackground(Color.GRAY);
                runstatus.setText(" UNKNOWN ");
            }
            runstatus.revalidate();
            runstatus.repaint();
        }
    }
    
    public void setTracing(int tracing)
    {
        if(changeTracing!=null)
        {
            changeTracing.setTracing(tracing);
        }
    }
    public void setLogExternal(boolean t)
    {
        if(changeTracing!=null)
        {
            changeTracing.setLogExternal(t);
        }
    }
    public void setLogTransaction(boolean t)
    {
        if(changeTracing!=null)
        {
            changeTracing.setLogTransaction(t);
        }
    }
    private class XTTMenuBar extends JMenuBar implements ActionListener
    {
		private static final long serialVersionUID = 3185394423987020975L;
		private JMenu filemenu=null;
		JCheckBoxMenuItem menuItemOrder=null;
        ViewHive viewhive=new ViewHive(xttgui);
        public XTTMenuBar()
        {
            filemenu=new JMenu("File");;
            this.add(filemenu);
            JMenuItem menuItem=null;
            
            menuItem=new JMenuItem("New/Load Test List");
            menuItem.setActionCommand("LOAD");
            menuItem.addActionListener(testlist);
            filemenu.add(menuItem);

            filemenu.add(testlist.getExportMenu());
            filemenu.addSeparator();

            menuItem=new JMenuItem("Exit");;
            filemenu.add(menuItem);
            menuItem.addActionListener(this);

            configmenu=new JMenu("Config");;
            this.add(configmenu);


            filemenu=new JMenu("Tools");
            this.add(filemenu);
            menuItem=new JMenuItem("Check Resources");
            menuItem.setActionCommand("RESOURCES");
            menuItem.addActionListener(this);
            filemenu.add(menuItem);

            filemenu.add(new SaveByteStringAsBinary(xttgui,"Save Encoded String as Binary"));
            filemenu.addSeparator();
            changeTracing=new ChangeTracing(xttgui,"Switch tracing temporally to...");
            filemenu.add(changeTracing);
            
            filemenu=new JMenu("List");
            this.add(filemenu);
            menuItemOrder=new JCheckBoxMenuItem("Order");
            menuItemOrder.setSelected(false);
            menuItemOrder.setActionCommand("ORDERLIST");
            menuItemOrder.addActionListener(this);
            filemenu.add(menuItemOrder);
            
            
            menuItem=new JMenuItem("View Hive");
            menuItem.setActionCommand("HIVE");
            menuItem.addActionListener(this);
            messagelistener.add(menuItem);
            messagelistener.finishMenu();
            this.add(messagelistener);
            
        }

        public void enabled(boolean state)
        {
            filemenu.setEnabled(state);
        }

        public void actionPerformed(ActionEvent e)
        {
            if(e.getActionCommand().equals("Exit"))
            {
                if(enabled)xttgui.setVisible(false);
                
            }  else if(e.getActionCommand().equals("RESOURCES"))
            {
                try
                {
                    if(XTT.isTestRunning())
                    {
                        showError("Run Error","Tests are running, can't Test Launcher!");
                        return;
                    }
                    if(parser==null)parser=new Parser();
                    XTTProperties.printDebug("Gui: Current size    : width="+((int)xttgui.getSize().getWidth())+" heigth="+((int)xttgui.getSize().getHeight()));
                    XTTProperties.printDebug("Gui: Current position: x="+((int)xttgui.getLocation().getX())+" y="+((int)xttgui.getLocation().getY()));
                    CheckResources cr=new CheckResources(xttgui);
                } catch (Exception ex)
                {
                    showError("Parser Error",ex.getClass().getName()+"\n"+ex.getMessage());
                    return;
                    //ex.printStackTrace();
                }
            } else if(e.getActionCommand().equals("HIVE"))
            {
                try
                {
                    viewhive.setVisible(true);
                } catch (Exception ex)
                {
                    showError("Hive Error",ex.getClass().getName()+"\n"+ex.getMessage());
                    return;
                }
            }
            else if(e.getActionCommand().equals("ORDERLIST")) 
            {
            	testlist.setOrderList(menuItemOrder.isSelected());
            }
        }
     }

    private class CheckResources extends JFrame implements ActionListener, WindowListener
    {
		private static final long serialVersionUID = 5072010977027393972L;
		private JPanel content=new JPanel(new VerticalFlowLayout());

        public CheckResources(XTTGui xttgui)
        {
            this.setTitle("Check Resources");
            Vector<String> resources=parser.checkResources();

            JScrollPane scroll=new JScrollPane(content);
            scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            this.getContentPane().setLayout(new BorderLayout());
            this.getContentPane().add(scroll,BorderLayout.CENTER);

            content.setMinimumSize(new Dimension(SUBWINDOWX,SUBWINDOWY));

            java.util.Iterator <String> it=resources.iterator();
            JLabel label=null;
            String text=null;
            while(it.hasNext())
            {
                text=it.next();
                label=new JLabel(text);
                if(!text.endsWith(FunctionModule.RESOURCE_OK))
                {
                    label.setForeground(Color.RED);
                }
                content.add(label);
            }
            content.revalidate();

            JButton close=new JButton("Close");
            close.setActionCommand("CLOSE");
            close.addActionListener(this);
            this.getContentPane().add(close,BorderLayout.SOUTH);

            this.pack();
            this.setLocationRelativeTo(null); //center it
            setVisible(true);
        }

        public void actionPerformed(ActionEvent e)
        {
            this.dispose();
        }
        public void windowActivated(WindowEvent e){}
        public void windowClosed(WindowEvent e)
        {
            this.setEnabled(false);
        }
        public void windowClosing(WindowEvent e)
        {
        }
        public void windowDeactivated(WindowEvent e){}
        public void windowDeiconified(WindowEvent e){}
        public void windowIconified(WindowEvent e){}
        public void windowOpened(WindowEvent e){}
    }

	public TestLauncherGui getTestLauncher() {
		return testLauncher;
	}

	public void setTestLauncher(TestLauncherGui testLauncher) {
		this.testLauncher = testLauncher;
	}

	public static RunnerFileTreePanel getRunnerTreePanel() {
		return runnerTreePanel;
	}
	public void exit (int status)
	{
		System.exit(status);
	}
	/*private JToolBar createToolBar()
	{
		toolBar = SwingUtils.getJToolBarWithBgImage("scenario toolbar", JToolBar.HORIZONTAL, ImageCenter.getInstance()
				.getImage(ImageCenter.ICON_TOP_TOOLBAR_BG));
		toolBar.setLayout(new FlowLayout(FlowLayout.LEFT, 1, 1));
		toolBar.setFloatable(false);
		toolBar.setRollover(true);
		
		upButton = toolBar.add(MoveUpAction.getInstance(testlist));
		upButton.setAlignmentX(Component.CENTER_ALIGNMENT);

		topButton = toolBar.add(MoveToTopAction.getInstance(testlist));
		topButton.setAlignmentX(Component.CENTER_ALIGNMENT);

		downButton = toolBar.add(MoveDownAction.getInstance(testlist));
		downButton.setAlignmentX(Component.CENTER_ALIGNMENT);

		bottomButton = toolBar.add(MoveToBottomAction.getInstance(testlist));
		bottomButton.setAlignmentX(Component.CENTER_ALIGNMENT);

		deleteButton = toolBar.add(RemoveItemAction.getInstance(testlist));
		deleteButton.setAlignmentX(Component.CENTER_ALIGNMENT);

		toolBar.addSeparator(new Dimension(10, 0));

		deleteAllButton = toolBar.add(RemoveAllItemsAction.getInstance(testlist));
		deleteAllButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		toolBar.addSeparator(new Dimension(10, 0));
		
		JToolBar runToolBar = SwingUtils.getJToolBarWithBgImage("Run Toolbar", JToolBar.HORIZONTAL,ImageCenter.getInstance().getImage(ImageCenter.ICON_RUN_TOOLBAR_BG));

		runToolBar.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 1));
		runToolBar.setFloatable(false);
		runToolBar.setRollover(true);

		repeatAmountLeft = new JTextField("0");
		repeatAmountLeft.setColumns(3);
		repeatAmountLeft.setToolTipText("Number of Repeats Left");
		repeatAmountLeft.setName("REPEAT_LEFT_NAME");
		
		repAmountLeft = new JLabel("Left");

		runTestButton = runToolBar.add(RunTestAction.getInstance(testlist));
		runTestButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		runTestButton.setMnemonic(KeyEvent.VK_F5);
		runToolBar.addSeparator(new Dimension(10, 0));

		stopTestButton = runToolBar.add(StopTestAction.getInstance(testlist));
		stopTestButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		stopTestButton.setMnemonic(KeyEvent.VK_F5);
		runTestButton.setEnabled(false);
		runToolBar.addSeparator(new Dimension(5, 0));
		
		repeatCheckBox = new JCheckBox("Repeat", false);
		repeatCheckBox.setToolTipText("Repeat test");
		repeatCheckBox.setOpaque(false);
		repeatCheckBox.addActionListener(this);

		repeatAmount = new JTextField();
		repeatAmount.addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent e)
			{
				try
				{
					Integer.parseInt(repeatAmount.getText());
					if (repeatAmount.getText().equals("0"))
						repeatAmount.setText("");
				}
				catch (Exception e1)
				{

					repeatAmount.setText("");
				}
			}
			public void keyPressed(KeyEvent e){}			
			public void keyTyped(KeyEvent e){}
		});
		repeatAmount.setColumns(3);
		repeatAmount.setEnabled(false);
		repeatAmount.setToolTipText("Number of tests Repeats");

		runToolBar.add(repeatCheckBox);
		runToolBar.addSeparator(new Dimension(5, 0));
		runToolBar.add(repeatAmount);
		runToolBar.addSeparator(new Dimension(5, 0));
		runToolBar.add(repAmountLeft);
		runToolBar.add(repeatAmountLeft);

		runToolBar.addSeparator(new Dimension(15, 0));
		showLogButton = runToolBar.add(ShowLogAction.getInstance(testlist));
		showLogButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		showLogButton.setEnabled(false);
		
		toolBar.addSeparator(new Dimension(10, 0));
		toolBar.add(runToolBar);
		toolBar.addSeparator(new Dimension(10, 0));
		
		openTestButton = toolBar.add(OpenBroseTestAction.getInstance(testlist));
		openTestButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		toolBar.addSeparator(new Dimension(5, 0));
		
		saveTestButton = toolBar.add(SaveTestAction.getInstance(testlist));
		saveTestButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		toolBar.addSeparator(new Dimension(5, 0));
		
		saveAsTestButton = toolBar.add(SaveAsTestAction.getInstance(testlist));
		saveAsTestButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		toolBar.addSeparator(new Dimension(5, 0));
		
		toolBar.addSeparator(new Dimension(5, 0));

		return toolBar;
	}*/
}