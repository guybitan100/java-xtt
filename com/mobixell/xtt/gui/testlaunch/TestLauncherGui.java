package com.mobixell.xtt.gui.testlaunch;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import com.mobixell.xtt.XTT;
import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.Parser;
import com.mobixell.xtt.ModuleList;
import com.mobixell.xtt.StringOutputStream;
import java.util.Vector;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;

import com.mobixell.xtt.gui.UpdateXTT;
import com.mobixell.xtt.gui.actionItems.ExitAction;
import com.mobixell.xtt.gui.actionItems.NewFolderAction;
import com.mobixell.xtt.gui.actionItems.OpenBroseTestAction;
import com.mobixell.xtt.gui.actionItems.NewTestAction;
import com.mobixell.xtt.gui.actionItems.SaveAsTestAction;
import com.mobixell.xtt.gui.actionItems.SaveTestAction;
import com.mobixell.xtt.gui.actionItems.ShowRunnerAction;
import com.mobixell.xtt.gui.main.XTTGui;
import com.mobixell.xtt.gui.testlaunch.panels.FileTreePanel;
import com.mobixell.xtt.gui.testlaunch.panels.StepsTreePanel;
import com.mobixell.xtt.gui.testlaunch.panels.ParamDialog;
import com.mobixell.xtt.gui.testlaunch.panels.TestPanel;
import com.mobixell.xtt.gui.testlaunch.panels.TreePanel;
import com.mobixell.xtt.gui.testlaunch.table.ParamDataItem;
import com.mobixell.xtt.gui.testlaunch.tree.AutoSaveWorker.Application;
import com.mobixell.xtt.gui.testlaunch.tree.TreeTestController;
import com.mobixell.xtt.gui.testlaunch.tree.nodes.AssetNode;
import com.mobixell.xtt.gui.testlaunch.tree.nodes.FileNode;
import com.mobixell.xtt.gui.testlaunch.tree.nodes.FolderNode;
import com.mobixell.xtt.images.ImageCenter;

/**
 * @author      Guy.Bitan
 * @version     $Id: Test Launcher.java,v 1.7 2011/06/11 13:34:36 rsoder Exp $
 * @see         com.mobixell.xtt.gui.main.XTTGui
 */

public class TestLauncherGui extends JFrame implements ChangeListener, WindowListener,ActionListener
{
	private static final long serialVersionUID = 8371605747939716597L;
	public static final String tantau_sccsid = "@(#)$Id: TestLauncher.java,v 1.7 2009/06/11 13:34:36 rsoder Exp $";
    protected StringOutputStream xttlog=new StringOutputStream();
    protected org.jdom.Document document=null;
    protected static JTabbedPane tabs=null;
    private static boolean isNewTest = false;
    private static boolean isExternalTest = false;
    private static FileTreePanel fileTreePanel;
    //Menu
    private JMenu menuFile = null;
    private JMenu helpmenu=null;
    private JMenu menuRunner = null;
    private JMenuBar menuBar;
    private JMenuItem menuItemSave;
    private JMenuItem menuItemShow;
    private JMenuItem menuItemSaveAs;
    private JMenuItem menuItemOpen;
    private JMenu menuNew;
    private JMenuItem menuItemNewTest;
    private JMenuItem menuItemNewFolder;
    //public static LogViewer ed;
    private Vector<ParamDataItem> vec_items=new Vector<ParamDataItem>();

    private static XTTGui xttgui       = null;
    public Parser parser       = null;
    public static ParamDialog parameterDialog= null;
    private static StepsTreePanel moduleTreePanel= null;
    private static TreePanel treePanel= null;
    public static TestPanel testPanel = null;
    public static final Color DEFAULT_COLOR = new Color(2,34,60);
    public static final Font DEFAULT_FONT = new Font(Font.SERIF,0, 16);
    public static final int DEFAULT_COL = 52;
    public static final int DEFAULT_ROW = 14;
   
    public TestLauncherGui(XTTGui xttgui, ModuleList modules,Parser parser) throws Exception
    {
    	addWindowListener(this);
    	setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    	setTitle("Test Launcher");
        setXttgui(xttgui);
        this.parser=parser;
        Image im = Toolkit.getDefaultToolkit().getImage(ImageCenter.getInstance().getImageUrl(ImageCenter.ICON_XTT_GREEN));
        this.setIconImage(im);
        
        tabs = SwingUtils.getJTabbedPaneWithBgImage(ImageCenter.getInstance().getImage(
				ImageCenter.ICON_TABBES_TOOLBAR_BG), ImageCenter.getInstance().getImage(
				ImageCenter.ICON_TABBES_TOOLBAR_BG));
        
        tabs.addChangeListener(this);
        setTreePanel(new TreePanel(this));
        getTreePanel().setEnabled(false);
        //Menu
        menuBar = new JMenuBar();
        menuBar.setBackground(new Color(0xf6, 0xf6, 0xf6));

        menuFile=new JMenu("File");
        menuFile.setVisible(true);
        menuFile.setMnemonic(KeyEvent.VK_F);
        
        menuItemSave = new JMenuItem("Save");
        menuItemSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
        menuItemSave.getAccessibleContext().setAccessibleDescription("Save");
        menuItemSave.addActionListener(SaveTestAction.getInstance(getTreePanel()));
        
        menuItemSaveAs = new JMenuItem("Save As");
        menuItemSaveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.CTRL_MASK));
        menuItemSaveAs.getAccessibleContext().setAccessibleDescription("Save As");
        menuItemSaveAs.addActionListener(SaveAsTestAction.getInstance(getTreePanel()));
        
        menuItemOpen = new JMenuItem("Open Test");
        menuItemOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
        menuItemOpen.getAccessibleContext().setAccessibleDescription("Open Test");
        menuItemOpen.addActionListener(OpenBroseTestAction.getInstance(getTreePanel()));
        
        menuNew=new JMenu("New");
        menuNew.setMnemonic(KeyEvent.VK_N);
        
        menuItemNewTest = new JMenuItem("Test");
        menuItemNewTest.setMnemonic(KeyEvent.VK_T);
        menuItemNewTest.getAccessibleContext().setAccessibleDescription("Test");
        menuItemNewTest.addActionListener(NewTestAction.getInstance(getTreePanel()));
        menuNew.add(menuItemNewTest);
        
        menuItemNewFolder = new JMenuItem("Folder");
        menuItemNewFolder.setMnemonic(KeyEvent.VK_F);
        menuItemNewFolder.getAccessibleContext().setAccessibleDescription("Folder");
        menuItemNewFolder.addActionListener(NewFolderAction.getInstance(null));
        menuNew.add(menuItemNewFolder);
        
        JMenuItem menuItemExit=new JMenuItem("Exit");;
        menuItemExit.addActionListener(this);
        
        menuFile.add(menuNew);
        menuFile.add(menuItemOpen);
        menuFile.add(menuItemSave);
        menuFile.add(menuItemSaveAs);
        menuFile.addSeparator();
        menuFile.add(menuItemExit);
        
        menuRunner=new JMenu("Runner");
        menuRunner.setVisible(true);
        menuRunner.setMnemonic(KeyEvent.VK_R);
        
        menuItemShow = new JMenuItem("Show");
        menuItemShow.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
        menuItemShow.getAccessibleContext().setAccessibleDescription("Show Runner"); 
        menuItemShow.addActionListener(ShowRunnerAction.getInstance());
        menuRunner.add(menuItemShow);
        
        helpmenu=new JMenu("Help");;
        
        JMenuItem menuItem = new JMenuItem("About XTT");
        menuItem.setActionCommand("ABOUT");
        menuItem.addActionListener(this);
        helpmenu.add(menuItem);
        
        
        menuBar.add(menuFile);   
        menuBar.add(menuRunner);
        menuBar.add(helpmenu);
        
        this.setJMenuBar(menuBar);
       
        
        //Menu End
        this.getContentPane().setLayout(new BorderLayout());

        //Size And Location of the main window        
        int xSize=XTTProperties.getIntProperty("GUI/SIZE/WIDTH");
        int ySize=XTTProperties.getIntProperty("GUI/SIZE/HEIGTH");
        int xLocation = Integer.parseInt(XTTProperties.getProperty("GUI/POSITION/X"));
        int yLocation = Integer.parseInt(XTTProperties.getProperty("GUI/POSITION/Y"));       
        
        this.setLocation(xLocation,yLocation);
        this.setPreferredSize(new Dimension(xSize, ySize));
        
        moduleTreePanel = new StepsTreePanel(modules);
        parameterDialog = new ParamDialog(this,vec_items,modules);
        
        setTestSetsPanel(new FileTreePanel());
        
        testPanel = new TestPanel();
        
        setTestTabEnabled(false);
        //setTabsVisible();
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,tabs,getTreePanel());
        splitPane.setDividerLocation(460);
		splitPane.setOneTouchExpandable(true);
		 this.getContentPane().add(xttgui.configurationBar,BorderLayout.NORTH);
		add(splitPane, BorderLayout.CENTER);    
        this.pack();
        this.setLocationRelativeTo(null); //center it
        setVisible(true);
    }
    
    public TestLauncherGui(){}
    public void actionPerformed(ActionEvent e)
    {
        if(e.getActionCommand().equals("Exit"))
        {
           ExitAction.getInstance().exit();
        }
        else if(e.getActionCommand().equals("ABOUT"))
        {
        	
        	aboutAction();
        }
        	
    }
    public void aboutAction ()
    {
    	int availableBuild = UpdateXTT.getLastStableBuild();
    	String currentBuild = XTTProperties.getXTTBuild();
    	String version = "eXtreme Test Tool";
		version += "\nVersion: " + XTTProperties.getXTTBuildVersion();
		int n=0;
		if (availableBuild==0)
		{
			Object[] options = { "Ok"};	
			n = JOptionPane.showOptionDialog(null, version,
					"About XTT", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, ImageCenter.getInstance().getImage(ImageCenter.ICON_XTT_GREEN), options,options[0]);
		}
		else if (currentBuild.indexOf(availableBuild+"")>=0)
		{
			version += "\nYour software is up tp date.";
			Object[] options = { "Ok"};	
			n = JOptionPane.showOptionDialog(null, version,
					"About XTT", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, ImageCenter.getInstance().getImage(ImageCenter.ICON_XTT_GREEN), options,options[0]);
		}
		else
		{
			version += "\nAvailable build: " + availableBuild;
			Object[] options = { "Ok", "Update to build " + availableBuild};	
			 n = JOptionPane.showOptionDialog(null, version,
					"XTT Version", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, ImageCenter.getInstance().getImage(ImageCenter.ICON_XTT_GREEN), options,options[0]);
		}
		
		if (n==1)
		{
			try
			{
				UpdateXTT.updateJar(xttgui);
			}
			catch (Exception e1)
			{
				// TODO Auto-generated catch block
					e1.printStackTrace();
			}
		}
	}
	public void setTestTabEnabled(boolean isEnabled) throws Exception
    {
		String htmlHeader ="<html><body leftmargin=15 topmargin=8 marginwidth=15 marginheight=5> <font size=3.8 face=verdana color=#003343>"; 
		String htmlFooter ="</font></body></html>";
		tabs.removeChangeListener(this);
    	if (isEnabled)
    	{
	    	tabs.removeAll();
	        tabs.insertTab(htmlHeader + "Test Set" + htmlFooter, null, getFileTreePanel(), "", 0);
	        tabs.insertTab(htmlHeader + "Test Details"      + htmlFooter, null, testPanel, "", 1);
	        tabs.insertTab(htmlHeader + "Create Steps"     + htmlFooter, null, moduleTreePanel, "", 2);
	        tabs.setAutoscrolls(true);
	        testPanel.setTestDetailsOk(true);
	    	getTreePanel().setEnabled(true);
    	}
    	else
    	{
        	getTreePanel().setEnabled(false);
        	tabs.removeAll();
        	tabs.insertTab(htmlHeader + "Test Set" + htmlFooter, null, getFileTreePanel(), "", 0);
            tabs.setAutoscrolls(true);
            testPanel.setTestDetailsOk(false);
        }
    	tabs.addChangeListener(this);
    }

	public void runTest(int repeatAmount) 
	{
		
		if (XTT.isTestRunning()) 
		{
			getXttgui().showError("Run Error","Tests are running, can't Test Launcher!");
			return;
		}
		try
		{
			TestLauncher tlw = new TestLauncher(repeatAmount);
			tlw.runTest();
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
}
	public void runStep() 
	{
		if (XTT.isTestRunning()) 
		{
			getXttgui().showError("Run Error","Tests are running, can't Test Launcher!");
			return;
		}
		try
		{
			TestLauncher tlw = new TestLauncher(1);
			tlw.runStep();
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}

	public static JTabbedPane getTabParams()
	{
		return tabs;
	}

	public void changedTabTextColor()
	{
		try
		{
			tabs.setForegroundAt(0, Color.darkGray);
			tabs.setForegroundAt(1, Color.darkGray);
			tabs.setForegroundAt(2, Color.darkGray);
			tabs.setForegroundAt(3, Color.darkGray);
			tabs.setForegroundAt(tabs.getSelectedIndex(), Color.BLACK);
		}
		catch (Exception e){}
	}
	public void stateChanged(ChangeEvent e) {

		try {
			TreeTestController.handelSelectedToolBar();
			changedTabTextColor();
			switch (tabs.getSelectedIndex()) 
			{
			case 0:
				 SwingUtils.setBusyCursor(this, true);
				 fileTreePanel.removeSelectionListener();
				 setTitle();
				 TestLauncherGui.getFileTreePanel().refreshTree(true);
				 fileTreePanel.addSelectionListener();
				 SwingUtils.setBusyCursor(this, false);
				break;
			case 1:
				setTitle();
				testPanel.updateTestPanelFromTree(isNewTest());
				break;
			case 2:
				testPanel.updateTestPanelFromTree(isNewTest());
				testPanel.validateFields();
				if (!testPanel.isTestDetailsOk)
				{
					tabs.removeChangeListener(this);
					tabs.setSelectedIndex(1);
					tabs.addChangeListener(this);
					TestLauncherGui.getFileTreePanel().refreshTree(true);
				}
				break;
			}
			
		} 
		catch (Exception e1) 
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	public  void setTitle()
	{
		AssetNode node = TestLauncherGui.getFileTreePanel().getSelectedNode();
		String filePath = null;
		if (node != null)
		{
			switch (node.getType())
			{
				case FOLDER:
					if (isNewTest())
					{
						setTitle(((FolderNode) node).getFolderPath() + "\\" + testPanel.getTestName() + ".xml");
						TreeTestController.test.setFilePath("");
					}
					else if (node.isRoot())
					{
						if (TreeTestController.getTestFileFullPath() != "" && TreeTestController.getTestFileFullPath() != null)
						{
							setTitle(TreeTestController.getTestFileFullPath());
						}
						else
						{
							setTitle(((FolderNode) node).getFolderPath());
						}
					}
					else
					{
						filePath=((FolderNode) node).getFolderPath() + "\\" + testPanel.getTestName();
						setTitle(filePath);
						TreeTestController.test.setFilePath(filePath);
					}
					break;
				case FILE:
					if (isNewTest())
					{
						setTitle(((FolderNode) node.getParent()).getFolderPath() + "\\" + testPanel.getTestName()+ ".xml");
						TreeTestController.test.setFilePath("");
					}
					else
					{
						if (isExternalTest) 
							setTitle(TreeTestController.getTestFileFullPath());
						else 
						{
							filePath = ((FileNode) node).getFileFullPath();
							setTitle(filePath);
							TreeTestController.test.setFilePath(filePath);
						}
					}
					break;
			}
		}
	}
	public TestPanel getTestPanel() 
	{
		return testPanel;
	}
	public static FileTreePanel getFileTreePanel()
	{
		return fileTreePanel;
	}

	public static void setTestSetsPanel(FileTreePanel testSetsPanel)
	{
		TestLauncherGui.fileTreePanel = testSetsPanel;
	}

	public static boolean isNewTest()
	{
		return TestLauncherGui.isNewTest;
	}

	public static void setNewTest(boolean isNewTest)
	{
		TestLauncherGui.isNewTest = isNewTest;
	}

	public static boolean isExternalTest()
	{
		return isExternalTest;
	}

	public static void setExternalTest(boolean isExternalTest)
	{
		TestLauncherGui.isExternalTest = isExternalTest;
	}
	
	public void windowClosed(WindowEvent e)
	{
		synchronized (Application.LOCK) {   
			this.dispose(); 
	    }   
	}
	public void windowIconified(WindowEvent e){}
	public void windowDeiconified(WindowEvent e){}
	public void windowActivated(WindowEvent e){}
	public void windowDeactivated(WindowEvent e){}
	public void windowOpened(WindowEvent e){}
	public void windowClosing(WindowEvent e){
		getXttgui().setDoAbort();
		TestLauncher.repeatAmount=0;
		while (TreePanel.stopTestButton.isEnabled())
		{
			Thread.yield();
		}
		ExitAction.getInstance().exit();
	}

	public static TreePanel getTreePanel() {
		return treePanel;
	}

	public static void setTreePanel(TreePanel treePanel) {
		TestLauncherGui.treePanel = treePanel;
	}

	public static XTTGui getXttgui() {
		return xttgui;
	}

	public void setXttgui(XTTGui xttgui) {
		TestLauncherGui.xttgui = xttgui;
	}
	public void exit(int status) {
		
		xttgui.exit(status);
	}
}
