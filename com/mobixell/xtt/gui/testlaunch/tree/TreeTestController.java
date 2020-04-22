package com.mobixell.xtt.gui.testlaunch.tree;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.ChangedCharSetException;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import com.mobixell.xtt.Parser;
import com.mobixell.xtt.XTT;
import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.XTTXML;
import com.mobixell.xtt.gui.ConfigurationEditor;
import com.mobixell.xtt.gui.XMLFileFilter;
import com.mobixell.xtt.gui.actionItems.AddSubTestAction;
import com.mobixell.xtt.gui.actionItems.AddToLoopAction;
import com.mobixell.xtt.gui.actionItems.CopyLoopAction;
import com.mobixell.xtt.gui.actionItems.CopyStepAction;
import com.mobixell.xtt.gui.actionItems.CopyThreadAction;
import com.mobixell.xtt.gui.actionItems.OpenLoopManagerAction;
import com.mobixell.xtt.gui.actionItems.PasteLoopAction;
import com.mobixell.xtt.gui.actionItems.PasteStepAction;
import com.mobixell.xtt.gui.actionItems.PasteThreadAction;
import com.mobixell.xtt.gui.actionItems.OpenThreadManagerAction;
import com.mobixell.xtt.gui.actionItems.AddToThreadAction;
import com.mobixell.xtt.gui.actionItems.ChangeStepNameAction;
import com.mobixell.xtt.gui.actionItems.ChangeSubTestAction;
import com.mobixell.xtt.gui.actionItems.CheckTreeAction;
import com.mobixell.xtt.gui.actionItems.CollapseTreeAction;
import com.mobixell.xtt.gui.actionItems.DuplicateStepAction;
import com.mobixell.xtt.gui.actionItems.EditConfParamAction;
import com.mobixell.xtt.gui.actionItems.ExpandTreeAction;
import com.mobixell.xtt.gui.actionItems.MoveDownAction;
import com.mobixell.xtt.gui.actionItems.MoveToBottomAction;
import com.mobixell.xtt.gui.actionItems.MoveToTopAction;
import com.mobixell.xtt.gui.actionItems.MoveUpAction;
import com.mobixell.xtt.gui.actionItems.RemoveFromLoopAction;
import com.mobixell.xtt.gui.actionItems.RemoveItemAction;
import com.mobixell.xtt.gui.actionItems.RunStepAction;
import com.mobixell.xtt.gui.actionItems.ShowXmlTestAction;
import com.mobixell.xtt.gui.actionItems.SuspendParamAction;
import com.mobixell.xtt.gui.actionItems.UnCheckTreeAction;
import com.mobixell.xtt.gui.actionItems.RemoveFromThreadAction;
import com.mobixell.xtt.gui.actionItems.UnSuspendParamAction;
import com.mobixell.xtt.gui.testlaunch.SwingUtils;
import com.mobixell.xtt.gui.testlaunch.TestLauncherGui;
import com.mobixell.xtt.gui.testlaunch.WaitDialog;
import com.mobixell.xtt.gui.testlaunch.panels.LoopDialog;
import com.mobixell.xtt.gui.testlaunch.panels.StepDialog;
import com.mobixell.xtt.gui.testlaunch.panels.ThreadDialog;
import com.mobixell.xtt.gui.testlaunch.panels.TreePanel;
import com.mobixell.xtt.gui.testlaunch.panels.XmlDialog;
import com.mobixell.xtt.gui.testlaunch.table.LoopDataItem;
import com.mobixell.xtt.gui.testlaunch.table.ParamDataItem;
import com.mobixell.xtt.gui.testlaunch.table.ThreadDataItem;
import com.mobixell.xtt.gui.testlaunch.tree.nodes.AssetNode;
import com.mobixell.xtt.gui.testlaunch.tree.nodes.FileNode;
import com.mobixell.xtt.gui.testlaunch.tree.nodes.FolderNode;
import com.mobixell.xtt.gui.testlaunch.tree.nodes.LoopNode;
import com.mobixell.xtt.gui.testlaunch.tree.nodes.ModuleFuncNode;
import com.mobixell.xtt.gui.testlaunch.tree.nodes.ParamGroupNode;
import com.mobixell.xtt.gui.testlaunch.tree.nodes.ParamNode;
import com.mobixell.xtt.gui.testlaunch.tree.nodes.StepNode;
import com.mobixell.xtt.gui.testlaunch.tree.nodes.SubtestNode;
import com.mobixell.xtt.gui.testlaunch.tree.nodes.TestNode;
import com.mobixell.xtt.gui.testlaunch.tree.nodes.AssetNode.NodeType;
import com.mobixell.xtt.gui.testlaunch.tree.nodes.ThreadNode;
import com.mobixell.xtt.log.LogViewer;
import com.mobixell.xtt.util.OSUtils;
public class TreeTestController implements KeyListener,TreeSelectionListener,MouseListener
{
	private JPopupMenu popupMenu = new JPopupMenu();
	HashMap<String, TreePath> expandedPaths = new HashMap<String, TreePath>();
	private static TestLauncherGui testLauncherGui = null;
	public static String xttlog = "";
	public static boolean isRepeat;
	public static int repeatAmount;
	public static TestMap test = null;
	public static ThreadDialog threadNodeMap;
	public static boolean isCopyToClipBoard = false;
	public enum ActionType
	{
		COLLAPSE, 
		EXPAND, 
		CHECK, 
		UNCHECK, 
		DOWN, 
		UP, 
		DELETE, 
		TO_BOTTOM, 
		TO_TOP, 
		DELETE_ALL, 
		NEW_TEST,
		SAVE_TEST, 
		SAVEAS_TEST, 
		ADD_THREAD,
		ADD_LOOP,
		ADD_TO_THREAD,
		ADD_TO_LOOP,
		REMOVE_FROM_THREAD, 
		REMOVE_FROM_LOOP,
		SUSPEND_PARAM, 
		UNSUSPEND_PARAM, 
		Duplicate_STEP, 
		COPY_STEP_TO_CLIPBOARD,
		COPY_THREAD_TO_CLIPBOARD,
		COPY_LOOP_TO_CLIPBOARD,
		PASTE_STEP_FROM_CLIPBOARD,
		PASTE_THREAD_FROM_CLIPBOARD,
		PASTE_LOOP_FROM_CLIPBOARD,
		RUN_STEP, 
		OPEN_TEST, 
		OPEN_LOG, 
		SHOW_XML,
		RUN_TEST,
		CHANGE_STEP_NAME,
		SEARCH;
	}
	int hotspot = new JCheckBox().getPreferredSize().width;

	public TreeTestController(TestLauncherGui testLauncher)
	{
		TreeTestController.setTestLauncherGui(testLauncher);
		test = new TestMap();
		addAllListeners();
	}
	public void addAllListeners()
	{
		test.getTree().addTreeSelectionListener(this);
		test.getTree().addMouseListener(this);
		test.getTree().addKeyListener(this);
	}
	public void removeAllListeners()
	{
		test.getTree().removeTreeSelectionListener(this);
		test.getTree().removeMouseListener(this);
		test.getTree().removeKeyListener(this);
	}
	public static void setTestDirty(boolean isDirty)
	{
		test.setDirty(isDirty);
	}
	public static boolean isTestDirty()
	{
		return test.isDirty();
	}
	public void mousePressed(MouseEvent e)
	{
		try
		{
			showPopup(e);
		}
		catch (Exception e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		setToolTipText(e);
	}
	public void setToolTipText(MouseEvent e)
	{
		AssetNode node = null;
		TreePath path = test.getPathForLocation(e.getX(), e.getY());
		if (path != null)
		{
			node = (AssetNode) path.getLastPathComponent();
		}
		if (path != null)
		{
			if (node.getType().equals(NodeType.ROOT))
			{
				test.setToolTipText("<html>" + node.getName() + "<br>" + ((TestNode) node).getDescription() + "</html>");
			}
			else
			{
				test.setToolTipText(node.getName());
			}
		}
	}
	public void handleTreeActions(ActionType type)
	{
		SwingUtils.setBusyCursor(TreeTestController.getTestLauncherGui(), true);
		switch (type)
		{
			case DELETE:
				try
				{
					removeCurrentNodeAction();
				}
				catch (Exception ex)
				{
					XTTProperties.printDebug("Gui: Fail to remove tree element");
				}
				break;
			case DOWN:
				try
				{
					moveDownAction();
				}
				catch (Exception e1)
				{
					XTTProperties.printDebug("Gui: Fail to move down tree element");
				}
				break;
			case UP:
				try
				{
					moveUpAction();
				}
				catch (Exception e1)
				{
					XTTProperties.printDebug("Gui: Fail to move up tree element");
				}
				break;
			case TO_BOTTOM:
				try
				{
					moveToBottomAction();
				}
				catch (Exception e1)
				{
					XTTProperties.printDebug("Gui: Fail to move tree element to bottom");
				}
				break;
			case TO_TOP:
				try
				{
					moveToTopAction();
				}
				catch (Exception e1)
				{
					XTTProperties.printDebug("Gui: Fail to move tree element to top");
				}
				break;
			case DELETE_ALL:
				try
				{
					deleteAllAction();
				}
				catch (Exception e1)
				{
					XTTProperties.printDebug("Gui: Fail to remove all elemets");
				}
				break;
			case Duplicate_STEP:
				try
				{
					duplicateStepAction();
				}
				catch (Exception e1)
				{
					XTTProperties.printDebug("Gui: Fail to duplicate step");
				}
				break;
			case COPY_STEP_TO_CLIPBOARD:
				try
				{
					isCopyToClipBoard=true;
					copyStepAction();
				}
				catch (Exception e1)
				{
					XTTProperties.printDebug("Gui: Fail to copy step");
				}
				break;
			case PASTE_STEP_FROM_CLIPBOARD:
				try
				{
					pasteAction();
				}
				catch (Exception e1)
				{
					XTTProperties.printDebug("Gui: Fail to paste step");
				}
				break;
			case COPY_THREAD_TO_CLIPBOARD:
				try
				{
					isCopyToClipBoard=true;
					copyThreadAction();
				}
				catch (Exception e1)
				{
					XTTProperties.printDebug("Gui: Fail to copy step");
				}
				break;
			case PASTE_THREAD_FROM_CLIPBOARD:
				try
				{
					pasteAction();
				}
				catch (Exception e1)
				{
					XTTProperties.printDebug("Gui: Fail to paste step");
				}
				break;
			case COPY_LOOP_TO_CLIPBOARD:
				try
				{
					isCopyToClipBoard=true;
					copyLoopAction();
				}
				catch (Exception e1)
				{
					XTTProperties.printDebug("Gui: Fail to copy loop");
				}
				break;
			case PASTE_LOOP_FROM_CLIPBOARD:
				try
				{
					pasteAction();
				}
				catch (Exception e1)
				{
					XTTProperties.printDebug("Gui: Fail to paste loop");
				}
				break;
			case RUN_STEP:
				try
				{
					runStepAction();
				}
				catch (Exception e1)
				{
					XTTProperties.printDebug("Gui: Fail to run step");
				}
				break;
			case RUN_TEST:
				try
				{
					runTestAction();
				}
				catch (Exception e1)
				{
					XTTProperties.printDebug("Gui: Fail to run test");
				}
				break;
			case NEW_TEST:
				try
				{
					ThreadDialog.resetDialog();
					TestLauncherGui.setExternalTest(false);
					TestLauncherGui.setNewTest(true);
					newTest();
				}
				catch (Exception e1)
				{
					XTTProperties.printDebug("Gui: Fail to open test");
				}
				break;
			case OPEN_TEST:
				try
				{
					TestLauncherGui.setExternalTest(true);
					openBrowseTest();
				}
				catch (Exception e1)
				{
					XTTProperties.printDebug("Gui: Fail to open test");
				}
				break;
			case SAVE_TEST:
				try
				{
					saveTest();
				}
				catch (Exception e1)
				{
					XTTProperties.printDebug("Gui: Fail to save test");
				}
				break;
			case SAVEAS_TEST:
				try
				{
					TestLauncherGui.setExternalTest(false);
					TestLauncherGui.setNewTest(false);
					saveAsTest();
				}
				catch (Exception e1)
				{
					XTTProperties.printDebug("Gui: Fail to save as test");
				}
				break;
			case ADD_THREAD:
				try
				{
					addThreadAction();
				}
				catch (Exception e1)
				{
					XTTProperties.printDebug("Gui: Fail to add thread");
				}
				break;
			case ADD_LOOP:
				try
				{
					addLoopAction();
				}
				catch (Exception e1)
				{
					XTTProperties.printDebug("Gui: Fail to add loop");
				}
				break;	
			case ADD_TO_THREAD:
				try
				{
					addToThreadAction();
				}
				catch (Exception e1)
				{
					XTTProperties.printDebug("Gui: Fail to add to thread");
				}
			break;
			case ADD_TO_LOOP:
				try
				{
					addToLoopAction();
				}
				catch (Exception e1)
				{
					XTTProperties.printDebug("Gui: Fail to add to loop");
				}
			break;
			case REMOVE_FROM_THREAD:
				try
				{
					removeFromThreadAction();
				}
				catch (Exception e1)
				{
					XTTProperties.printDebug("Gui: Fail to remove from thread");
				}
			break;
			case REMOVE_FROM_LOOP:
				try
				{
					removeFromLoopAction();
				}
				catch (Exception e1)
				{
					XTTProperties.printDebug("Gui: Fail to remove from loop");
				}
			break;
			case CHANGE_STEP_NAME:
				try
				{
					changeStepNameAction();
				}
				catch (Exception e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			break;
			case SUSPEND_PARAM:
				try
				{
					suspendParamAction();
				}
				catch (Exception e1)
				{
					XTTProperties.printDebug("Gui: Fail to suspend parameter");
				}
				break;
			case UNSUSPEND_PARAM:
				try
				{
					unSuspendAction();
				}
				catch (Exception e1)
				{
					XTTProperties.printDebug("Gui: Fail to unsuspend parameter");
				}
				break;
			case OPEN_LOG:
				try
				{
					openXttLogAction();
				}
				catch (Exception e1)
				{
					XTTProperties.printDebug("Gui: Fail to show log");
				}
				break;
			case SHOW_XML:
				try
				{
					showXmlTestAction();
				}
				catch (Exception e1)
				{
					XTTProperties.printDebug("Gui: Fail to show xml test");
				}
				break;
			case SEARCH:
				try
				{
					SearchTreeDialog.showSearchTreeDialog(testLauncherGui);
				}
				catch (Exception e)
				{
					XTTProperties.printDebug("Gui: Fail to open search dialog");
				}
				break;
		}
		SwingUtils.setBusyCursor(TreeTestController.getTestLauncherGui(), false);
	}
	
	public static void handleRootTreeActions(ActionType type)
	{
		SwingUtils.setBusyCursor(TreeTestController.getTestLauncherGui(), true);
		switch (type)
		{
			case COLLAPSE:
				try
				{
					test.expandAll(false);
				}
				catch (Exception e1)
				{
					XTTProperties.printDebug("Gui: Fail to collapse tree");
				}
				break;
			case EXPAND:
				try
				{
					test.expandAll(true);
				}
				catch (Exception e1)
				{
					XTTProperties.printDebug("Gui: Fail to expand tree");
				}
				break;
			case CHECK:
				try
				{
					rootCheckPerformAction(true);
				}
				catch (Exception e1)
				{
					XTTProperties.printDebug("Gui: Fail to check tree");
				}
				break;
			case UNCHECK:
				try
				{
					rootCheckPerformAction(false);
				}
				catch (Exception e1)
				{
					XTTProperties.printDebug("Gui: Fail to uncheck tree");
				}
				break;
		}
		SwingUtils.setBusyCursor(TreeTestController.getTestLauncherGui(), false);
	}
	public static void updateTestTree(String sTestName, String sQCID, String sTestDesc, String sStepsDesc,
			String designer, String creationDate, String testLevel, String xfwVersion) throws Exception
	{
		 test.setName(sTestName);
		 test.setQcID(sQCID);
		 test.setDescription(sTestDesc);
		 test.setStepsDesc(sStepsDesc);
		 test.setDesigner(designer);
		 test.setCreationDate(creationDate);
		 test.setLevel(testLevel);
		 test.setXfwVersion(xfwVersion);
		 test.updateTestNode();
	}
	public static String getTestName() throws Exception
	{
		return test.getName();
	}
	public static String getTestDescription() throws Exception
	{
		return test.getDescription();
	}
	public static void setTestDescription(String description) throws Exception
	{
		test.setDescription(description);
	}
	public static String getTestNameDescription() throws Exception
	{
		return test.getName() + "," + test.getDescription();
	}
	public static String getTestStepsDesc() throws Exception
	{
		return test.getStepsDesc();
	}
	public static String getTestDesigner() throws Exception
	{
		return test.getTestNode().getDesigner();
	}
	public static String getTestCreationDate() throws Exception
	{
		return test.getTestNode().getCreationDate();
	}
	public static String getTestLevel() throws Exception
	{
		return test.getTestNode().getTestLevel();
	}
	public static void changeTestLevel(String sTestLevel) throws Exception
	{
		test.setLevel(sTestLevel);
		test.updateTestNode();
	}
	public static String getTestXfwVersion() throws Exception
	{
		return test.getXfwVersion();
	}
	public static String getQCID() throws Exception
	{
		return test.getQcID();
	}
	public static void changeDesigner(String sDesigner) throws Exception
	{
		test.setDesigner(sDesigner);
		test.updateTestNode();
	}
	public static void changecreationdate(String sCreationdate) throws Exception
	{
		test.setCreationDate(sCreationdate);
		test.updateTestNode();
	}
	public static void changeQCID(String sQCID) throws Exception
	{
		test.setQcID(sQCID);
		test.updateTestNode();
	}
	public static void changeTestName(String sTestName) throws Exception
	{
		test.setName(sTestName);
		test.updateTestNode();
	}
	public static void changeTestDesc(String sTestDesc) throws Exception
	{
		test.setDescription(sTestDesc);
		test.updateTestNode();
	}
	public static void changeStepNameAction()throws Exception
	{
		StepDialog.showDialog(testLauncherGui, TreeTestController.getStepName());
	}
	public static void createThreadNodeFromXml(String threadName,Element xmlThread)throws Exception
	{
		List<?> steps = xmlThread.getChildren();
		Iterator<?> itS = steps.iterator();
		Element function = null;
		String functionName =null;
		String stepName = null;
		String moduleName=null;
		boolean isMandatory = false;
		// Get the name attribute of the function
		// if there is no name abort running of test
		if (threadName == null || threadName.equals(""))
		{
			if (xmlThread.getAttribute("threadname")!=null)
			{
				threadName = xmlThread.getAttribute("threadname").toString();
			}
			threadName = "Thread";
		}
		ThreadDataItem tdi = new ThreadDataItem(true, threadName);
		ThreadNode nThreadNode = new ThreadNode(threadName);
		tdi.setNode(nThreadNode);
		
		while (itS.hasNext())
		{
			function = (Element) itS.next();
			if (function.getName().toLowerCase().equals("stepname"))
			{
				stepName = function.getText();
			}
			else if (function.getName().toLowerCase().equals("mandatory"))
			{
				try
				{
				isMandatory = Boolean.parseBoolean(function.getText());
				} catch (Exception e){isMandatory=false;}
			}
			else if (function.getName().toLowerCase().equals("subtest"))
			{
				if (stepName == "") 
					nThreadNode.add(createSubTestNode(function.getText().trim(),function.getText().trim(),isMandatory));
				else 
					nThreadNode.add(createSubTestNode(function.getText().trim(), stepName,isMandatory));				
			}
			else if (function.getName().toLowerCase().equals("function"))
			{
				// Get the name attribute of the function
				functionName =Parser.getAttribute(function, "name");
				// if there is no name abort running of test
				if (functionName == null)
				{
					// XTTProperties.printFail(this.getClass().getName()+".runTest(): function attribute 'name' not found");
					XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_PARSER_ERROR);
				}
				// Get the module attribute of the function
				moduleName =Parser.getAttribute(function, "module");
				// if there is no module abort running of test
				if (moduleName == null)
				{
					XTTProperties.printFail("openTest(): module='name' not found on function " + functionName);
					XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_PARSER_ERROR);
				}
				nThreadNode.add(createStepNode(stepName,false, moduleName, functionName, getParameters(function)));
			}
		}
		test.insertNodeInto(nThreadNode, test.getTestNode(), test.getChildCount());
		ThreadDialog.updateThreadList(tdi);
	}
	public static ThreadNode createThreadNodeFromThreadXml(ThreadNode nThreadNode,Element xmlThread)throws Exception
	{
		List<?> steps = xmlThread.getChildren();
		Iterator<?> itS = steps.iterator();
		Element function = null;
		String functionName =null;
		String stepName = null;
		String moduleName=null;
		boolean isMandatory = false;
		String threadName ="";
		while (itS.hasNext())
		{
			function = (Element) itS.next();
			
			if (function.getName().toLowerCase().equals("threadname"))
			{
				threadName = function.getText();
				ThreadDataItem tdi = new ThreadDataItem(true, threadName);
				nThreadNode = new ThreadNode(threadName);
				tdi.setNode(nThreadNode);
				ThreadDialog.updateThreadList(tdi);
			}
			if (function.getName().toLowerCase().equals("thread"))
			{
				createThreadNodeFromThreadXml(nThreadNode,function);
			}
			if (function.getName().toLowerCase().equals("stepname"))
			{
				stepName = function.getText();
			}
			else if (function.getName().toLowerCase().equals("mandatory"))
			{
				try
				{
				isMandatory = Boolean.parseBoolean(function.getText());
				} catch (Exception e){isMandatory=false;}
			}
			else if (function.getName().toLowerCase().equals("subtest"))
			{
				if (stepName == "") 
					nThreadNode.add(createSubTestNode(function.getText().trim(),function.getText().trim(),isMandatory));
				else 
					nThreadNode.add(createSubTestNode(function.getText().trim(), stepName,isMandatory));				
			}
			else if (function.getName().toLowerCase().equals("function"))
			{
				// Get the name attribute of the function
				functionName =Parser.getAttribute(function, "name");
				// if there is no name abort running of test
				if (functionName == null)
				{
					// XTTProperties.printFail(this.getClass().getName()+".runTest(): function attribute 'name' not found");
					XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_PARSER_ERROR);
				}
				// Get the module attribute of the function
				moduleName =Parser.getAttribute(function, "module");
				// if there is no module abort running of test
				if (moduleName == null)
				{
					XTTProperties.printFail("openTest(): module='name' not found on function " + functionName);
					XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_PARSER_ERROR);
				}
				nThreadNode.add(createStepNode(stepName,false, moduleName, functionName, getParameters(function)));
			}
		}
		
		return nThreadNode;
	}
	public static void createLoopNodeFromXml(Element xmlLoop)throws Exception
	{
		List<?> steps = xmlLoop.getChildren();
		Iterator<?> itS = steps.iterator();
		Element function = null;
		String functionName =null;
		String stepName = null;
		String moduleName=null;
		String loopName =null;
		String start =null;
		String stop =null;
		String step =null;
		boolean isMandatory = false;
		// Get the name attribute of the function
		// if there is no name abort running of test
			if (Parser.getAttribute(xmlLoop, "name")!=null)
			{
				loopName = Parser.getAttribute(xmlLoop, "name");
				start =Parser.getAttribute(xmlLoop, "start");
				stop = Parser.getAttribute(xmlLoop, "stop");
				step = Parser.getAttribute(xmlLoop, "step");
			}
			else
			{
			loopName = "Loop";
			start ="0";
			stop = "1";
			step = "1";
			}
		LoopDataItem tdi = new LoopDataItem(true, loopName,start,stop,step);
		LoopNode nLoopNode = new LoopNode(loopName,start,stop,step);
		tdi.setNode(nLoopNode);
		
		while (itS.hasNext())
		{
			function = (Element) itS.next();
			if (function.getName().toLowerCase().equals("stepname"))
			{
				stepName = function.getText();
			}
			else if (function.getName().toLowerCase().equals("mandatory"))
			{
				try
				{
				isMandatory = Boolean.parseBoolean(function.getText());
				} catch (Exception e){isMandatory=false;}
			}
			else if (function.getName().toLowerCase().equals("subtest"))
			{
				if (stepName == "") 
					nLoopNode.add(createSubTestNode(function.getText().trim(),function.getText().trim(),isMandatory));
				else 
					nLoopNode.add(createSubTestNode(function.getText().trim(), stepName,isMandatory));				
			}
			else if (function.getName().toLowerCase().equals("function"))
			{
				// Get the name attribute of the function
				functionName =Parser.getAttribute(function, "name");
				// if there is no name abort running of test
				if (functionName == null)
				{
					// XTTProperties.printFail(this.getClass().getName()+".runTest(): function attribute 'name' not found");
					XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_PARSER_ERROR);
				}
				// Get the module attribute of the function
				moduleName =Parser.getAttribute(function, "module");
				// if there is no module abort running of test
				if (moduleName == null)
				{
					XTTProperties.printFail("openTest(): module='name' not found on function " + functionName);
					XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_PARSER_ERROR);
				}
				nLoopNode.add(createStepNode(stepName,false, moduleName, functionName, getParameters(function)));
			}
		}
		test.insertNodeInto(nLoopNode, test.getTestNode(), test.getChildCount());
		LoopDialog.updateLoopList(tdi);
	}
	public static LoopNode createLoopNodeFromLoopXml(LoopNode nLoopNode,Element xmlLoop)throws Exception
	{
		List<?> steps = xmlLoop.getChildren();
		Iterator<?> itS = steps.iterator();
		Element function = null;
		String functionName =null;
		String stepName = null;
		String moduleName=null;
		boolean isMandatory = false;
		String loopName ="";
		while (itS.hasNext())
		{
			function = (Element) itS.next();
			
			if (function.getName().toLowerCase().equals("loop"))
			{
				loopName = Parser.getAttribute(function, "name");
				String start = Parser.getAttribute(function, "start");
				String stop =  Parser.getAttribute(function, "stop");
				String step =  Parser.getAttribute(function, "step");
				LoopDataItem tdi = new LoopDataItem(true, loopName,start,stop,step);
				nLoopNode = new LoopNode(loopName,start,stop,step);
				tdi.setNode(nLoopNode);
				LoopDialog.updateLoopList(tdi);
			}
			if (function.getName().toLowerCase().equals("loop"))
			{
				createLoopNodeFromLoopXml(nLoopNode,function);
			}
			if (function.getName().toLowerCase().equals("stepname"))
			{
				stepName = function.getText();
			}
			else if (function.getName().toLowerCase().equals("mandatory"))
			{
				try
				{
				isMandatory = Boolean.parseBoolean(function.getText());
				} catch (Exception e){isMandatory=false;}
			}
			else if (function.getName().toLowerCase().equals("subtest"))
			{
				if (stepName == "") 
					nLoopNode.add(createSubTestNode(function.getText().trim(),function.getText().trim(),isMandatory));
				else 
					nLoopNode.add(createSubTestNode(function.getText().trim(), stepName,isMandatory));				
			}
			else if (function.getName().toLowerCase().equals("function"))
			{
				// Get the name attribute of the function
				functionName = Parser.getAttribute(function, "name");
				// if there is no name abort running of test
				if (functionName == null)
				{
					// XTTProperties.printFail(this.getClass().getName()+".runTest(): function attribute 'name' not found");
					XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_PARSER_ERROR);
				}
				// Get the module attribute of the function
				moduleName = Parser.getAttribute(function, "module");
				// if there is no module abort running of test
				if (moduleName == null)
				{
					XTTProperties.printFail("openTest(): module='name' not found on function " + functionName);
					XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_PARSER_ERROR);
				}
				nLoopNode.add(createStepNode(stepName,false, moduleName, functionName, getParameters(function)));
			}
		}
		
		return nLoopNode;
	}
	public static AssetNode createStepNodeFromXml(Element elementXmlStep)throws Exception
	{
		Iterator<?> itS  = elementXmlStep.getChildren().iterator();
		Element function = null;
		String functionName =null;
		String stepName = null;
		String moduleName=null;
		boolean isMandatory = false;
		AssetNode node = null;
		while (itS.hasNext())
		{
			function = (Element) itS.next();
			if (function.getName().toLowerCase().equals("stepname"))
			{
				stepName = function.getText();
			}
			else if (function.getName().toLowerCase().equals("mandatory"))
			{
				try
				{
				isMandatory = Boolean.parseBoolean(function.getText());
				} catch (Exception e){isMandatory=false;}
			}
			else if (function.getName().toLowerCase().equals("subtest"))
			{
				if (stepName == "") 
					node= createSubTestNode(function.getText().trim(),function.getText().trim(),isMandatory);
				else 
					node= createSubTestNode(function.getText().trim(), stepName,isMandatory);				
			}
			else if (function.getName().toLowerCase().equals("function"))
			{
				// Get the name attribute of the function
				functionName =Parser.getAttribute(function, "name");
				// if there is no name abort running of test
				if (functionName == null)
				{
					// XTTProperties.printFail(this.getClass().getName()+".runTest(): function attribute 'name' not found");
					XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_PARSER_ERROR);
				}
				// Get the module attribute of the function
				moduleName =Parser.getAttribute(function, "module");
				// if there is no module abort running of test
				if (moduleName == null)
				{
					XTTProperties.printFail("openTest(): module='name' not found on function " + functionName);
					XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_PARSER_ERROR);
				}
				node = createStepNode(stepName,false, moduleName, functionName, getParameters(function));
			}
		}
		return node;
	}
	public static void createStep(String sStepName, Boolean isMandatory,String sModule, String sFunction, Vector<ParamDataItem> vec_items,boolean isExpand)
			throws Exception
	{
		try
		{
		StepNode nStepNode = createStepNode(sStepName,isMandatory, sModule, sFunction, vec_items);
		test.insertNodeInto(nStepNode, test.getTestNode(), test.getChildCount());
//		/System.out.println("Step Node: " + nStepNode.getName()+" Type: " + nStepNode.getType() + " Parent: " + nStepNode.getParent() + " Added.");
		if (isExpand) test.expandAll(true);
		test.setSelectionPath(new TreePath(nStepNode.getPath()));
		handelSelectedToolBar();
		}
		catch (Exception e) {e.printStackTrace();}
	}
	public static StepNode createStepNode(String sStepName,Boolean isMandatory, String sModule, String sFunction, Vector<ParamDataItem> vec_items)
			throws Exception
	{	
		if (sStepName == "" || sStepName==null) 
			sStepName = sModule + "/" + sFunction;
		
		Vector<ParamNode> vec_param_node = new Vector<ParamNode>();
		StepNode nStepNode = new StepNode(test.getTestNode(), sStepName);
		nStepNode.setSelected(true);
		ModuleFuncNode nModuleFuncNode = new ModuleFuncNode(nStepNode, sModule + "/" + sFunction);
		nModuleFuncNode.setModule(sModule);
		nModuleFuncNode.setFunction(sFunction);
		nStepNode.add(nModuleFuncNode);
		if (vec_items != null)
		{
			Iterator<ParamDataItem> it = vec_items.iterator();
			ParamDataItem current = null;
			ParamNode nParamNode = null;
			while (it.hasNext())
			{
				current = it.next();

				if (current.use)
				{
					if (current.value != "")
					{
						nParamNode = new ParamNode(nModuleFuncNode, "<" + current.type + ">" + current.value + "</"
								+ current.type + ">");
						if (current.group)  
							nParamNode.setInGroup(true);
						else				
							nParamNode.setInGroup(false);
						vec_param_node.add(nParamNode);
					}
					else
					{
						nParamNode = new ParamNode(nModuleFuncNode, "<" + current.type + ">" + "</" + current.type+ ">");
						if (current.group)  
							nParamNode.setInGroup(true);
						
						else				
							nParamNode.setInGroup(false);
					 vec_param_node.add(nParamNode);
					}
				}
			}
			if (vec_param_node.size() > 0)
			{
				Iterator<ParamNode> iter = vec_param_node.iterator();
				ParamNode currentNode = null;
				boolean isAddGroup = false;
				ParamGroupNode nParamGroupNode =null;
				while (iter.hasNext())
				{
					currentNode = iter.next();
					if (currentNode.isInGroup() && !isAddGroup)
					{
						nParamGroupNode = new ParamGroupNode(nModuleFuncNode, "Parameter Group");
						nModuleFuncNode.add(nParamGroupNode);
						nParamGroupNode.add(currentNode);
						isAddGroup = true;
					}
					else
					{
						if (currentNode.isInGroup() && isAddGroup)
							nParamGroupNode.add(currentNode);
						else 
							nModuleFuncNode.add(currentNode);
					}
				}
			}
		}
		nStepNode.setMandatory(isMandatory,true);
		return nStepNode;
	}
	public static void createSubTest(String subTestPath, String stepName,boolean isMandatory,boolean isExpand) throws Exception
	{
		StepNode nStepTreeNode = createSubTestNode(subTestPath, stepName,isMandatory);
		test.insertNodeInto(nStepTreeNode, test.getTestNode(), test.getChildCount());
		if (isExpand) test.expandAll(true);
		test.setSelectionPath(new TreePath(nStepTreeNode.getPath()));
		handelSelectedToolBar();
	}
	public static StepNode createSubTestNode(String subTestPath, String stepName,boolean isMandatory) throws Exception
	{
		StepNode nStepNode = new StepNode(test.getTestNode(), stepName);
		nStepNode.setSelected(true);
		SubtestNode nSubTestNode = new SubtestNode(nStepNode, subTestPath);
		nStepNode.add(nSubTestNode);
		nStepNode.setMandatory(isMandatory, true);
		return nStepNode;
	}
	public static void newTest() throws Exception
	{
		resetTest();
		testLauncherGui.setTestTabEnabled(true);
		if (TestLauncherGui.getTabParams().getSelectedIndex() == 0)
		{
			if (TestLauncherGui.getFileTreePanel().getSelectedPath(true) != null)
			{
				TestLauncherGui.getTabParams().setSelectedIndex(1);
			}
			else
			{
				showError("Please select test location!", "Please select test location!");
			}
		}
		else
		{
			TestLauncherGui.getTabParams().setSelectedIndex(0);
		}
	}

	public static void openBrowseTest() throws Exception
	{
		if (XTT.isTestRunning())
		{
			showError("Run Error", "Tests are running, can't add until they finished!");
			return;
		}
		try
		{
			JFileChooser fc = new JFileChooser(System.getProperty("user.dir") + "/tests/XMP");
			fc.setFileFilter(new XMLFileFilter());
			// In response to a button click:
			int returnVal = fc.showOpenDialog(null);
			if (returnVal == JFileChooser.APPROVE_OPTION)
			{
				TestLauncherGui.testPanel.resetTestPanel();
				test.setFileNode(null);
				openTest(fc.getSelectedFile().getName(), fc.getSelectedFile().getPath());
				TestLauncherGui.getFileTreePanel().expandToFile(getTestFileNode());
			}
		}
		catch (Exception e)
		{
			showError("Test File Set Error", e.getMessage());
		}
	}
	public static void openTest() throws Exception
	{
		openTest(test.getFileNode().getName(),test.getFileNode().getFileFullPath());
	}
	public static void openTest(File testFile) throws Exception
	{
		openTest(testFile.getName(),testFile.getCanonicalPath());
	}
	public static void openTest(String fileName, String filePath) throws Exception
	{
		File file = new File(filePath);
		setTestFileNode(new FileNode(fileName, file.getCanonicalPath(),file.getPath()));
		ThreadDialog.resetDialog();
		TestLauncherGui.setNewTest(false);
		org.jdom.Document document = null;
		org.jdom.input.SAXBuilder parser = new org.jdom.input.SAXBuilder();
		try
		{
			document = parser.build(file.getAbsolutePath());
		}
		catch (Exception e)
		{
			showError("Run Error", "The parser can't read this file format");
			return;
		}
		if (!checkIfTestFile(document.getRootElement()))
		{
			XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
			String xmlString = outputter.outputString(document);
            ConfigurationEditor ed=new ConfigurationEditor(TestLauncherGui.getXttgui(),null,xmlString,false);
		}
		else
		{
			resetTest();
			parseTest(document);
			if (test.getName().equalsIgnoreCase("NewTest")) 
				changeTestName(fileName.substring(0,fileName.indexOf(".xml")));
		}
		test.setSelectionPath(test.getRootPath());
		test.setFilePath(filePath);
	}
	public static boolean checkIfTestFile(Element root) throws Exception
	{
		if (root.getName().toLowerCase().equals("test"))
		{
			return true;
		}
		else
			return false;
	}
	public static void resetTest() throws Exception
	{
		TestLauncherGui.testPanel.resetTestPanel();

		test.setName("");
		test.setDescription("");
		test.setStepsDesc("");
		test.setLevel("");
		test.setQcID("");
		test.setDesigner("");
		test.setCreationDate("");
		test.setLevel("");		
		test.removeAllSteps();
		test.getTestNode().setStatus(XTTProperties.NOT_RUNNING);
		test.reload();
		testLauncherGui.setTestTabEnabled(true);
		setTestDirty(false);		
	}
	public static void parseTest(org.jdom.Document testDoc) throws Exception
	{
		try
		{
			Element root = testDoc.getRootElement();
			if (root.getName().toLowerCase().equals("test"))
			{
				SwingUtils.setBusyCursor(testLauncherGui, true);
				WaitDialog.launchWaitDialog("Open test ...", null);
				HashMap<String, String> localVars = new HashMap<String, String>();
				localVars.put("currenttest/testpath", XTTProperties.getCurrentTestPath());
				test.removeAllSteps();
				test.reload();
				parseXmlTest(root, localVars);				
				WaitDialog.endWaitDialog();
				SwingUtils.setBusyCursor(testLauncherGui, false);
			}
			else
			{
				XTTProperties.printFail("addTest(): This file is not an XTT test case file");
			}
		}
		catch (Exception ex)
		{
			XTTProperties.printFail("Could not add test");
		}
	}
	public static boolean parseXmlTest(Element element, HashMap<String, String> localVars) throws Exception
	{
		// FunctionModule module=null;
		List<?> functions = element.getChildren();
		Iterator<?> itF = functions.iterator();
		Element function = null;
		String testName = getTestName();	
		String functionName = null;
		String moduleName = null;
		String stepName=null;
		String threadName=null;
		boolean isMandatory = false;
		// boolean functionNotFound=true;
		while (itF.hasNext())
		{
			function = (Element) itF.next();
			if (function.getName().toLowerCase().equals("loop"))
			{
				createLoopNodeFromXml(function);	
			}
			else if (function.getName().toLowerCase().equals("conditional")){}
			else if (function.getName().toLowerCase().equals("while")){}
			else if (function.getName().toLowerCase().equals("testlevel"))
			{
				changeTestLevel(function.getText());
			}
			else if (function.getName().toLowerCase().equals("qcid"))
			{
				changeQCID(function.getText());
			}
			else if (function.getName().toLowerCase().equals("designer"))
			{
				changeDesigner(function.getText());
			}
			else if (function.getName().toLowerCase().equals("creationdate"))
			{
				changecreationdate(function.getText());
			}
			else if (function.getName().toLowerCase().equals("description"))
			{
				changeTestDesc(function.getText().replaceAll("\\n", "").replaceAll("\\t",""));
			}
			else if (function.getName().toLowerCase().equals("div"))
			{
				String divcomment =Parser.getAttribute(function, "comment");
				if (divcomment != null)
				{
					XTTProperties.printExternal("Div:\n" + divcomment);
				}
				divcomment =Parser.getAttribute(function, "fail");
				if (divcomment != null)
				{
					XTTProperties.printFail("Div:\n" + divcomment);
				}
				divcomment =Parser.getAttribute(function, "warn");
				if (divcomment != null)
				{
					XTTProperties.printWarn("Div:\n" + divcomment);
				}
				divcomment =Parser.getAttribute(function, "info");
				if (divcomment != null)
				{
					XTTProperties.printInfo("Div:\n" + divcomment);
				}
				divcomment =Parser.getAttribute(function, "verbose");
				if (divcomment != null)
				{
					XTTProperties.printVerbose("Div:\n" + divcomment);
				}
				divcomment =Parser.getAttribute(function, "debug");
				if (divcomment != null)
				{
					XTTProperties.printDebug("Div:\n" + divcomment);
				}
				if (parseXmlTest(function, localVars))
				{
					return true;
				}
			}
			else if (function.getName().toLowerCase().equals("name"))
			{
				testName = function.getText().trim();
			}
			else if (function.getName().toLowerCase().equals("configuration")){}
			else if (function.getName().toLowerCase().equals("threadname"))
			{
				threadName = function.getText();
			}
			else if (function.getName().toLowerCase().equals("thread"))
			{
				createThreadNodeFromXml(threadName,function);
			}
			else if (function.getName().toLowerCase().equals("stepname"))
			{
				stepName = function.getText();
			}
			else if (function.getName().toLowerCase().equals("mandatory"))
			{
				try
				{
					isMandatory = Boolean.parseBoolean(function.getText());
				}
				catch (Exception e)
				{
					isMandatory = false;
				}
			}
			else if (function.getName().toLowerCase().equals("subtest"))
			{
				if (stepName == "" || stepName==null) 
					createSubTest(function.getText().trim(),function.getText().trim(),isMandatory,false);
				else 
					createSubTest(function.getText().trim(),stepName,isMandatory,false);
			}
			else if (function.getName().toLowerCase().equals("function"))
			{
				// Get the name attribute of the function
				functionName =Parser.getAttribute(function, "name");
				// if there is no name abort running of test
				if (functionName == null)
				{
					// XTTProperties.printFail(this.getClass().getName()+".runTest(): function attribute 'name' not found");
					XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_PARSER_ERROR);
					return true;
				}
				// Get the module attribute of the function
				moduleName =Parser.getAttribute(function, "module");
				// if there is no module abort running of test
				if (moduleName == null)
				{
					XTTProperties.printFail("openTest(): module='name' not found on function " + functionName);
					XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_PARSER_ERROR);
					return true;
				}
				createFromParseXmlStep(stepName,isMandatory,functionName,moduleName, function,false);
			}
		}
		changeTestName(testName);	
		test.expandAll(true);
		return true;
	}
private static boolean createFromParseXmlStep (String stepName,boolean isMandatory,String functionName,String moduleName,Element function,boolean isExpand) throws Exception
{
	Vector<ParamDataItem> vec_items = getParameters(function);
	createStep(stepName,isMandatory, moduleName, functionName, vec_items,isExpand);
	return true;
}
	public static String parseXml(Element parameter)
	{
		try
		{
			if (parameter.getChildren().isEmpty()) throw new Exception("No XML content in xml tag");
			if (parameter.getChildren().size() > 1) throw new Exception(
					"Only 1 direct child for xml tag allowed (used as root node)");
			Element e = (Element) ((Element) parameter.getChildren().get(0)).clone();
			e.detach();
			Document document = new Document(e);
			String value = XTTXML.stringXML(document);
			if (value.indexOf("<?xml version=\"1.0\"?>") >= 0)
			{
				value = value.replace("<?xml version=\"1.0\"?>", "&lt;?xml version=\"1.0\"?&gt;");
			}
			return value;
		}
		catch (Exception ex)
		{
			XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_PARSER_ERROR);
			if (XTTProperties.printDebug(null))
			{
				XTTProperties.printException(ex);
			}
		}
		return null;
	}
	private static Vector<ParamDataItem> getParameters (Element function) throws Exception
	{
		Vector<ParamDataItem> vec_items = null;
		List<?> functionParameters = null;
		Element parameter = null;
		List<?> parameters = null;
		functionParameters = function.getChildren();
		// Create the array of parameters for the runFunction method of
		// the function module
		// first parameter is always the function name
		if (functionParameters.size() > 0)
		{
			vec_items = new Vector<ParamDataItem>(functionParameters.size());
		}
		else vec_items = null;
		// Get the iterator over all the parameters
		// for all the parameters in the function
		for (int i = 0; i < functionParameters.size(); i++)
		{
			parameter = (Element) functionParameters.get(i);
			if (parameter.getName().equalsIgnoreCase("xml")) 
			{
				// get the parameter
				ParamDataItem currentParam = new ParamDataItem();
				currentParam.group = false;
				currentParam.use = true;
				currentParam.type = "xml";
				currentParam.value = parseXml(parameter);
				vec_items.add(currentParam);
			}
			else if (parameter.getChildren().size() == 0)
			{
				// get the parameter
				ParamDataItem currentParam = new ParamDataItem();
				currentParam.group = false;
				currentParam.use = true;
				currentParam.type = parameter.getName();
				String value = parameter.getValue();
				if (value.indexOf("<?xml version") >= 0)
				{
					value = parameter.getValue().toString().replaceAll("<", "&lt;");
					value = value.replaceAll(">", "&gt;");
				}
				if (function.getAttribute("name").getValue().indexOf("execute")>=0
				 || function.getAttribute("name").getValue().equalsIgnoreCase("setCacheFile")
				 || function.getAttribute("name").getValue().toLowerCase().indexOf("query")>=0) 
				{
					value = parameter.getValue().toString().replaceAll("<", "&lt;");
					value = value.replaceAll(">", "&gt;");
				}
				if (value.indexOf("&") >= 0 && value.indexOf("&amp;")<0 && value.indexOf("&lt;")<0 && value.indexOf("&gt;")<0)
				{
					value = parameter.getValue().toString().replaceAll("&", "&amp;");
				}
				currentParam.value = value;
				vec_items.add(currentParam);
			}
			else if (parameter.getChildren().size() > 0)
			{
				parameters = parameter.getChildren();
				for (int j = 0; j < parameters.size(); j++)
				{
					// get the parameter
					ParamDataItem currentParam = new ParamDataItem();
					currentParam.group = true;
					currentParam.use = true;
					currentParam.type = ((Element) parameters.get(j)).getName();
					
					String value = ((Element) parameters.get(j)).getValue();
					if (value.indexOf("<?xml version") >= 0)
					{
						value = value.replaceAll("<", "&lt;");
						value = value.replaceAll(">", "&gt;");
					}
					if (function.getAttribute("name").getValue().equalsIgnoreCase("executeRemoteCommand")
							|| function.getAttribute("name").getValue().equalsIgnoreCase("setCacheFile")
							|| function.getAttribute("name").getValue().toLowerCase().indexOf("query")>=0) 
					{
						value = value.toString().replaceAll("<", "&lt;");
						value = value.replaceAll(">", "&gt;");
					}
					if (value.indexOf("&") >= 0 && value.indexOf("&amp;")<0 && value.indexOf("&lt;")<0 && value.indexOf("&gt;")<0)
					{
						value = value.replaceAll("&", "&amp;");
					}
					currentParam.value = value;
					vec_items.add(currentParam);
				}
			}	
		}
		return vec_items;
	}
	public static void runStepAction()
	{
		getTestLauncherGui().runStep();
	}

	public static void runTestAction() throws Exception
	{
		isRepeat = TreePanel.repeatCheckBox.isSelected();
		test.save();
		XTTProperties.setTestList(test.getFilePath(),true);
		if (TestLauncherGui.testPanel.isTestDetailsOk)
		{
			if (isRepeat)
			{
				String sRepeatAmount = TreePanel.repeatAmount.getText();

				if (!sRepeatAmount.equalsIgnoreCase("") && sRepeatAmount != null)
				{
					repeatAmount = Integer.parseInt(TreePanel.repeatAmount.getText());
					getTestLauncherGui().runTest(repeatAmount);
				}
				else getTestLauncherGui().runTest(1);
			}
			else
			{
				getTestLauncherGui().runTest(1);
			}
		}
	}
	private static void rootCheckPerformAction(Boolean isCheck)
	{
		TreeNode node = test.getTestNode();
		if (node.getChildCount() >= 0)
		{
			for (Enumeration<?> e = node.children(); e.hasMoreElements();)
			{
				AssetNode assetNode = (AssetNode) e.nextElement();
					StepNode currentNode = (StepNode) assetNode;
					currentNode.setSelected(isCheck);
					test.updateNode(node);
			}
		}
	}
	public static void changeModFunParam(String sModule, String sFunction, Vector<ParamDataItem> vec_items)
			throws Exception
	{
		AssetNode curNode = ((AssetNode) test.getLastSelectedPathComponent());
		ModuleFuncNode nModuleFuncNode = null;
		if (curNode.getType().equals(NodeType.MOD_FUN))
		{
			nModuleFuncNode = (ModuleFuncNode) curNode;
		}
		if (curNode.getType().equals(NodeType.PARMS))
		{
			if (((AssetNode) curNode.getParent()).getType().equals(NodeType.MOD_FUN))
			{
				nModuleFuncNode = (ModuleFuncNode) curNode.getParent();
			}
			else
			{
				nModuleFuncNode = (ModuleFuncNode) curNode.getParent().getParent();
			}
		}
		nModuleFuncNode.setName(sModule + "/" + sFunction);
		nModuleFuncNode.setModule(sModule);
		nModuleFuncNode.setFunction(sFunction);
		Vector<ParamNode> vec_param_node = new Vector<ParamNode>();
		nModuleFuncNode.removeAllChildren();
		Iterator<ParamDataItem> it = vec_items.iterator();
		ParamDataItem current = null;
		ParamNode nParamNode = null;

		while (it.hasNext())
		{
			current = it.next();

			if (current.use)
			{
				if (current.value != "")
				{
					if (current.value.indexOf("xml version=")<0)						
						nParamNode = new ParamNode(nModuleFuncNode,"<"+current.type+">"+current.value+"</"+ current.type + ">");
					else
						nParamNode = new ParamNode(nModuleFuncNode,"<"+current.type+">"+current.value);	
					if (current.group)  
						nParamNode.setInGroup(true);
					else				
						nParamNode.setInGroup(false);
					    vec_param_node.add(nParamNode);
				}
				else
				{
					nParamNode = new ParamNode(nModuleFuncNode,"<"+current.type+ ">"+"</"+current.type+">");
					if (current.group)  nParamNode.setInGroup(true);
					
					else				nParamNode.setInGroup(false);
					
					vec_param_node.add(nParamNode);
				}
			}
		}
		if (vec_param_node.size() > 0)
		{
			Iterator<ParamNode> iter = vec_param_node.iterator();
			ParamNode currentNode = null;
			boolean isAddGroup = false;
			ParamGroupNode nParamGroupNode =null;
			while (iter.hasNext())
			{
				currentNode = iter.next();
				if (currentNode.isInGroup() && !isAddGroup)
				{
					nParamGroupNode = new ParamGroupNode(nModuleFuncNode, "Parameter Group");
					test.insertNodeInto(nParamGroupNode, nModuleFuncNode, nModuleFuncNode.getChildCount());
					test.insertNodeInto(currentNode, nParamGroupNode,nParamGroupNode.getChildCount());
					isAddGroup = true;
				}
				else
				{
					if (currentNode.isInGroup() && isAddGroup)
						test.insertNodeInto(currentNode, nParamGroupNode,nParamGroupNode.getChildCount());
					else 
						test.insertNodeInto(currentNode, nModuleFuncNode, nModuleFuncNode.getChildCount());
				}
			}
		}
		test.updateNode(nModuleFuncNode);
		test.reload();
		test.expandAll(true);
		test.setSelectionPath(new TreePath(nModuleFuncNode.getPath()));
		handelSelectedToolBar();
	}

	public static Vector<ParamDataItem> getModFunParam(ModuleFuncNode moduleFuncNode) throws Exception
	{
		ModuleFuncNode nModuleFuncNode;
		if (moduleFuncNode == null) 
			nModuleFuncNode = ((ModuleFuncNode) test.getTree().getLastSelectedPathComponent());
		else 
			nModuleFuncNode = moduleFuncNode;

		ParamDataItem pParamDataItem;
		String nodeValue = "";
		String parameter = "";
		String value = "";
		int endIndexEnd;
		boolean isGroup = false;
		Vector<ParamDataItem> vec_items = new Vector<ParamDataItem>(nModuleFuncNode.getChildCount());
		for (int i = 0; i < nModuleFuncNode.getChildCount(); i++)
		{
			AssetNode assetNode = (AssetNode) nModuleFuncNode.getChildAt(i);
			if (assetNode.getType().equals(NodeType.PARMS))
			{
				isGroup = false;
				ParamNode nParamNode = (ParamNode) nModuleFuncNode.getChildAt(i);
				nodeValue = nParamNode.getName();
				int endIndexStart = nodeValue.indexOf(">");
				parameter = nodeValue.substring(1, endIndexStart);

				if (!parameter.equalsIgnoreCase("xml"))
				{
					endIndexEnd = nodeValue.indexOf("</");
					value = nodeValue.substring(endIndexStart + 1, endIndexEnd);
				}
				else
				{
					endIndexEnd = nodeValue.indexOf("</xml>");
					value = nodeValue.substring(endIndexStart + 1, endIndexEnd) + "</xml>";
				}
				pParamDataItem = new ParamDataItem();
				pParamDataItem.group = isGroup;
				pParamDataItem.type = parameter;
				pParamDataItem.use = true;
				pParamDataItem.value = value;
				vec_items.add(pParamDataItem);
			}
			else if (assetNode.getType().equals(NodeType.PARAMGROUP))
			{
				isGroup = true;
				ParamGroupNode nParamGroupNode = (ParamGroupNode) nModuleFuncNode.getChildAt(i);
				for (int j = 0; j < nParamGroupNode.getChildCount(); j++)
				{

					ParamNode nParamNode = (ParamNode) nParamGroupNode.getChildAt(j);
					nodeValue = nParamNode.getName();
					int endIndexStart = nodeValue.indexOf(">");
					parameter = nodeValue.substring(1, endIndexStart);

					if (!parameter.equalsIgnoreCase("xml"))
					{
						endIndexEnd = nodeValue.indexOf("</");
						value = nodeValue.substring(endIndexStart + 1, endIndexEnd);
					}
					else
					{
						endIndexEnd = nodeValue.indexOf("</xml>");
						value = nodeValue.substring(endIndexStart + 1, endIndexEnd) + "</xml>";
					}
					pParamDataItem = new ParamDataItem();
					pParamDataItem.group = isGroup;
					pParamDataItem.type = parameter;
					pParamDataItem.use = true;
					pParamDataItem.value = value;
					vec_items.add(pParamDataItem);
				}
			}

		}
		return vec_items;
	}
	public static void suspendParamAction()
	{
		((ParamNode) test.getLastPathComponent()).setSelected(false);
	}

	public static void unSuspendAction()
	{
		((ParamNode) test.getLastPathComponent()).setSelected(true);
	}

	public static void openXttLogAction()
	{
		new LogViewer(TestLauncherGui.getXttgui(), xttlog.toString(),true);
	}
	public static void showXmlTestAction() throws JDOMException, IOException, Exception
	{
		XmlDialog.showDialog(testLauncherGui, test.updateAndGetTestXml().toString());
	}
	public synchronized static void setXttLog(String inXttlog)
	{
		xttlog = inXttlog;
	}

	/**
	 * Remove the currently selected node.
	 * 
	 * @throws Exception
	 */
	public static void removeCurrentNodeAction() throws Exception
	{
		Object[] options = { "Yes", "No" };

		int n = JOptionPane.showOptionDialog(null, "Are you sure you want to delete this item?",
				"Confirme delete steps", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options,
				options[1]);
		if (n == JOptionPane.NO_OPTION || n==-1)
		{
			return;
		}
		else
		{
			TreePath[] pathItems = test.getSelectionPaths();
			for (int ind = 0; ind < pathItems.length; ind++)
			{
				TreePath selectPath = pathItems[ind];
				AssetNode selectedNode = (AssetNode) selectPath.getLastPathComponent();

				if (selectedNode != null)
				{
					AssetNode parent = (AssetNode) (selectedNode.getParent());

					if (parent != null)
					{
						DefaultTreeModel defaultTreeModule = (DefaultTreeModel) test.getModel();
						if (parent.getChildCount() > 1)
						{
							defaultTreeModule.removeNodeFromParent(selectedNode);
						}
						else
						{
							while (parent.getChildCount() <= 1)
							{
								AssetNode grandF = (AssetNode) (parent.getParent());

								if (!parent.getType().equals(NodeType.ROOT))
								{
									defaultTreeModule.removeNodeFromParent(parent);
								}
								else
								{
									if (!selectedNode.isType(NodeType.MOD_FUN) && selectedNode.isType(NodeType.STEP) || selectedNode.isType(NodeType.THREAD) || selectedNode.isType(NodeType.LOOP))
									{
										
										test.removeAllSteps();
										test.updateTestNode();
										test.reload();
									}
								}
								parent = grandF;
								if (parent == null) break;
							}
						}
					}
					if (selectedNode.isType(NodeType.THREAD))
					{
						ThreadDialog.removeFromThread(selectedNode.getName());
					}
				}
			}
			test.setSelectionPaths(pathItems);
			handelSelectedToolBar();
		}
	}
	private static void moveToBottomAction() throws Exception
	{
		TreePath[] pathItems = getOrderSelectedPath();

		for (int ind = 0; ind < pathItems.length; ind++)
		{
			TreePath selectPath = pathItems[ind];
			AssetNode node = (AssetNode) selectPath.getLastPathComponent();
			if (node != null)
			{
				AssetNode parent = (AssetNode) (node.getParent());
				int count = parent.getChildCount();
				for (int i = 0; i < parent.getChildCount() - 1; i++)
				{
					if (node == parent.getChildAt(i))
					{
						if (i + 1 < parent.getChildCount())
						{
							parent.remove(i);
							count = parent.getChildCount();
							parent.insert(node, count);
							test.updateNode(parent);
						}
						break;
					}
				}
			}
		}
		test.reload();
		test.expandAll(true);
		test.getTree().setSelectionPaths(pathItems);
		handelSelectedToolBar();
	}
	private static void moveToTopAction() throws Exception
	{
		TreePath[] pathItems = getOrderSelectedPath();
		int j=0;
		for (int ind = 0; ind < pathItems.length; ind++)
		{
			TreePath selectPath = pathItems[ind];
			AssetNode node = (AssetNode) selectPath.getLastPathComponent();
			if (node != null)
			{
				AssetNode parent = (AssetNode) (node.getParent());
				for (int i = 0; i < parent.getChildCount(); i++)
				{
					if (node == parent.getChildAt(i))
					{
						if (i - 1 >= 0)
						{
							parent.remove(i);
							parent.insert(node, j++);
							test.updateNode(parent);
						}
						else if (ind==0) return;
						break;
					}
				}
			}
		}
		test.reload();
		test.expandAll(true);
		test.getTree().setSelectionPaths(pathItems);
		handelSelectedToolBar();
	}

	private static void moveUpAction() throws Exception
	{
		TreePath[] pathItems =  getOrderSelectedPath();

		for (int ind = 0; ind < pathItems.length; ind++)
		{
			AssetNode node = (AssetNode) pathItems[ind].getLastPathComponent();
			AssetNode parent = (AssetNode) (node.getParent());
			if (node != null && parent!=null)
			{
				for (int i = 0; i < parent.getChildCount(); i++)
				{
					if (node.equals(parent.getChildAt(i)))
					{
						if (i-1 >= 0)
						{
							parent.insert(node, i - 1);
							test.updateNode(parent);
						}
						else if (ind==0) return;
						
						break;
					}
				}
			}
		}
		test.reload();
		test.expandAll(true);
		test.setSelectionPaths(pathItems);
		handelSelectedToolBar();
	}

	private static void moveDownAction() throws Exception
	{
		TreePath[] pathItems = getOrderSelectedPath();

		for (int ind = pathItems.length - 1; ind >= 0; ind--)
		{
			AssetNode node = (AssetNode) pathItems[ind].getLastPathComponent();
			AssetNode parent = (AssetNode) (node.getParent());
			if (node != null)
			{
				for (int i = 0; i < parent.getChildCount(); i++)
				{
					if (node == parent.getChildAt(i))
					{
						if (i + 1 < parent.getChildCount())
						{
							
							parent.insert(node, i + 1);
							test.updateNode(parent);
						}
						else if (i+1==parent.getChildCount()) return;
						break;
					}
				}
			}
		}
		test.reload();
		test.expandAll(true);
		test.getTree().setSelectionPaths(pathItems);
		handelSelectedToolBar();
	}

	private static TreePath[] getOrderSelectedPath() throws Exception
	{
		TreePath[] pathItems = test.getSelectionPaths();
		SortedMap<Integer, TreePath> nodes =  new TreeMap<Integer, TreePath>() ;
		
		for (int ind = 0; ind < pathItems.length; ind++)
		{
			AssetNode node = (AssetNode) pathItems[ind].getLastPathComponent();
			AssetNode parent = (AssetNode) (node.getParent());
			if (node != null && parent!=null) 
				nodes.put(parent.getIndex(node), pathItems[ind]);
		}
		Iterator<Integer> it = nodes.keySet().iterator();
		for (int ind = 0; ind < pathItems.length; ind++)
		pathItems[ind] = (TreePath) nodes.get(it.next());

		return pathItems;
	}

	public static void deleteAllAction() throws Exception
	{
		Object[] options = { "Remove All", "Remove Unselected","Cancel" };
		int n = JOptionPane.showOptionDialog(null, "What do you want to delete?",
				"Confirme delete steps", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options,
				options[2]);
		if (n == 2)
		{
			return;
		}
		else if (n==0)
		{
			removeAllNodes();
			test.reload();
			test.expandAll(true);
			
		}
		else if (n==1)
		{
			if (removeUnselectedNodes())
			{
			test.reload();
			test.expandAll(true);
			}
		}
	}
	public static boolean removeUnselectedNodes() throws Exception
	{
		boolean isRemoved = false;
		isRemoved = test.removeUnselected();
		setTestDirty(true);
		handelSelectedToolBar();
		return isRemoved;
	}
	public static void removeAllNodes() throws Exception
	{
		test.removeRootChildrens();
		setTestDirty(true);
		handelSelectedToolBar();
	}
	private static void saveTest() throws Exception
	{
			AssetNode node = TestLauncherGui.getFileTreePanel().getSelectedNode();

			switch (node.getType())
			{
				case FOLDER:
					if (TestLauncherGui.isNewTest() && !TestLauncherGui.isExternalTest()) 
					{
						test.setFilePath(((FolderNode) node).getFolderPath() + "\\"+ getTestName() + ".xml");
						setTestFileNode(new FileNode(getTestName(), test.getFilePath(), ((FolderNode) node).getFolderPath()));
					}
					break;
				case FILE:
					if (!TestLauncherGui.isNewTest() && !TestLauncherGui.isExternalTest() ) 
					{
						test.setFilePath(((FileNode) node).getFileFullPath());
						setTestFileNode(new FileNode(getTestName(),((FileNode) node).getFileFullPath() , ((FileNode) node).getFilePath()));
					}
					else if (TestLauncherGui.isNewTest())
					{
							test.setFilePath(((FolderNode) node.getParent()).getFolderPath()+ "\\"+ getTestName() + ".xml");
							setTestFileNode(new FileNode(getTestName(), test.getFilePath(), ((FolderNode) node.getParent()).getFolderPath()));
					}
					break;
		}

		if (test.getFilePath() == null || test.getFilePath() == "")
		{
			showError("Please select test location!", "Please select test location!");
			return;
		}
		else
		{
			File file = new File(test.getFilePath());
			if (file.exists())
			{
				try
				{

					Object[] options = { "Yes, overwrite", "No, cancel" };
					int n = JOptionPane.showOptionDialog(null, "The file already exists!\n"
							+ "Are you sure you want to overwrite:\n" + file.getCanonicalPath(),
							"Confirme overwriting file", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
							options, options[1]);
					if (n == JOptionPane.YES_OPTION)
					{
						test.save();
						setTestFileNode(new FileNode(getTestName(),file.getPath() , file.getParent()));
					}
				}
				catch (Exception ex)
				{
					showError("Save Error", ex.getClass().getName() + "\n" + ex.getMessage());
				}
			}
			else
			{
				test.save();
			}
		}
	}
	public static StringBuffer getBuildXmlStep() throws Exception
	{
		return test.getXmlStep(true);
	}
	public static StringBuffer updateAndGetTestXml(boolean includeUnSelected) throws Exception
	{
	return test.updateAndGetTestXml(includeUnSelected);
	}
	public static String getAllSteps()
	{
	return test.getAllSteps();
	}

	public static void saveAsTest() throws Exception
	{
		File file = new File(test.getFilePath());
		JFileChooser fc;
		fc = new JFileChooser();
		if (file.exists())
		{
			fc.setCurrentDirectory(file.getParentFile());
			fc.setSelectedFile(file);
		}
		else
		{
			file = new File(System.getProperty("user.dir") + "/tests/xmp/" + test.getName() + ".xml");
			fc.setCurrentDirectory(file.getParentFile());
			fc.setSelectedFile(file);
		}

		fc.setDialogTitle("Save As " + test.getName() + " Test");
		fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

		int returnVal = fc.showSaveDialog(fc);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			if (fc.getSelectedFile().getAbsoluteFile().getAbsolutePath().endsWith(".xml"))
			{
				test.setFilePath(fc.getSelectedFile().getAbsoluteFile().getAbsolutePath());
				file = fc.getSelectedFile();

				if (file.exists())
				{
					try
					{
						Object[] options = { "Yes, overwrite", "No, cancel" };
						int n = JOptionPane.showOptionDialog(null, "The file already exists!\n"
								+ "Are you sure you want to overwrite:\n" + file.getCanonicalPath(),
								"Confirme overwriting file", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
								null, options, options[1]);
						if (n == JOptionPane.YES_OPTION)
						{
							test.save();
						}
						else
							return;
					}
					catch (Exception ex)
					{
						showError("Save Error", ex.getClass().getName() + "\n" + ex.getMessage());
					}
				}
				else
				{
					test.save();
				}
				TestLauncherGui.setExternalTest(true);
				test.setFileNode(null);
				openTest(fc.getSelectedFile().getName(), fc.getSelectedFile().getPath());
				TestLauncherGui.getTabParams().setSelectedIndex(1);
				TestLauncherGui.setExternalTest(false);
			}
			else showError("Wrong file extention", "Please enter valide file!!");
		}
	}
		
	private static void showError(String errortitle, String errortext)
	{
		JOptionPane.showMessageDialog(null, errortext, errortitle, JOptionPane.ERROR_MESSAGE);
	}
	public static void copyStepAction() throws Exception
	{
		 test.setCopyThread(false);
		 test.setCopyLoop(false);
		 StringBuffer xmlStep= test.getXmlStep(false);
		 java.awt.datatransfer.StringSelection contents=new java.awt.datatransfer.StringSelection(xmlStep.toString());
         java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(contents, contents);
	}
	public static void copyThreadAction() throws Exception
	{
		 test.setCopyThread(true);
		 test.setCopyLoop(false);
		 StringBuffer xmlThread= test.getXmlThread();
		 java.awt.datatransfer.StringSelection contents=new java.awt.datatransfer.StringSelection(xmlThread.toString());
         java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(contents, contents);
	}
	public static void copyLoopAction() throws Exception
	{
		 test.setCopyLoop(true);
		 test.setCopyThread(false);
		 StringBuffer xmlLoop= test.getXmlLoop();
		 java.awt.datatransfer.StringSelection contents=new java.awt.datatransfer.StringSelection(xmlLoop.toString());
         java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(contents, contents);
	}
	public static void pasteAction()
	{
		try
		{
			Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
			String selectionXml = ((String) clipboard.getContents(testLauncherGui.getTestPanel()).getTransferData(DataFlavor.stringFlavor));
			setTestDirty(true);
			if (test.isCopyThread())
			{
				pasteThreadAction(selectionXml);
			}
			else if (test.isCopyLoop())
			{
				pasteLoopAction(selectionXml);
			}
			else 
			{
				pasteStepAction(selectionXml);
			}
		}
		catch (Exception e){}
	}
	public static void pasteThreadAction(String selectionXml) 
	{
		try
		{
	        selectionXml=selectionXml.replaceAll("<threadname>","<threadname>CopyOf");
	        org.jdom.input.SAXBuilder parser = new org.jdom.input.SAXBuilder();
	     	org.jdom.Document document = parser.build(new StringReader(selectionXml));
	     	AssetNode assetNode =createThreadNodeFromThreadXml(new ThreadNode("Thread"),document.getRootElement());
	     	
	     	AssetNode parent = (AssetNode)((AssetNode) test.getLastPathComponent()).getParent();
	     	if (parent!=null)
	     	{
	     	int index = parent.getIndex((AssetNode) test.getLastPathComponent());
	     	parent.insert(assetNode, index);
	     	test.updateNode(parent);
	     	}
	     	else
	     	{
	     		test.getTestNode().insert(assetNode, 0);
	     		test.updateNode(test.getTestNode());
	     	}
	     	test.reload();
			test.expandAll(true);
			test.setSelectionPath(new TreePath(assetNode.getPath()));
			handelSelectedToolBar();
		}
		catch (Exception e){}
	}
	public static void pasteLoopAction(String selectionXml) 
	{
		try
		{
	        selectionXml=selectionXml.replaceAll("<loop name=\"","<loop name=\"CopyOf");
	        org.jdom.input.SAXBuilder parser = new org.jdom.input.SAXBuilder();
	     	org.jdom.Document document = parser.build(new StringReader(selectionXml));
	     	AssetNode assetNode =createLoopNodeFromLoopXml(new LoopNode("","","",""),document.getRootElement());
	     	
	     	AssetNode parent = (AssetNode)((AssetNode) test.getLastPathComponent()).getParent();
	     	if (parent!=null)
	     	{
	     	int index = parent.getIndex((AssetNode) test.getLastPathComponent());
	     	parent.insert(assetNode, index);
	     	test.updateNode(parent);
	     	}
	     	else
	     	{
	     		test.getTestNode().insert(assetNode, 0);
	     		test.updateNode(test.getTestNode());
	     	}
	     	test.reload();
			test.expandAll(true);
			test.setSelectionPath(new TreePath(assetNode.getPath()));
			handelSelectedToolBar();
		}
		catch (Exception e){}
	}
	public static void pasteStepAction(String selectionXml) 
	{
		try
		{
	        selectionXml=selectionXml.replaceAll("<stepname>","<stepname>CopyOf");
	        org.jdom.input.SAXBuilder parser = new org.jdom.input.SAXBuilder();
	     	org.jdom.Document document = parser.build(new StringReader(selectionXml));
	     	Element rootStep = document.getRootElement();						
	     	AssetNode assetNode =createStepNodeFromXml(rootStep);
	     	AssetNode parent = (AssetNode)((AssetNode) test.getLastPathComponent()).getParent();
	     	if (parent!=null)
	     	{
	     	int index = parent.getIndex((AssetNode) test.getLastPathComponent());
	     	parent.insert(assetNode, index);
	     	test.updateNode(parent);
	     	}
	     	else
	     	{
	     		test.getTestNode().insert(assetNode, 0);
	     		test.updateNode(test.getTestNode());
	     	}
	     	test.reload();
			test.expandAll(true);
			test.setSelectionPath(new TreePath(assetNode.getPath()));
			handelSelectedToolBar();
		}
		catch (Exception e){}
	}
	public static void duplicateStepAction() throws Exception
	{
		TreePath[] pathItems = test.getSelectionPaths();
		for (int ind = 0; ind < pathItems.length; ind++)
		{
			TreePath pathItem = pathItems[ind];
			if (pathItem != null)
			{
				AssetNode currentNode = (AssetNode) pathItem.getLastPathComponent();

				if (currentNode.isType(NodeType.STEP))
				{
					StepNode sourceStep = (StepNode) currentNode;

					AssetNode parent = (AssetNode) (sourceStep.getParent());

					StepNode nNewStepNode = new StepNode(parent, "CopyOf " + sourceStep.getName());

					int nodeInd = parent.getIndex(sourceStep);

					if (nodeInd - 1 >= 0) parent.insert(nNewStepNode, nodeInd);

					else parent.insert(nNewStepNode, 0);
					currentNode = (AssetNode) sourceStep.getChildAt(0);

					if (currentNode.getType().equals(NodeType.MOD_FUN))
					{
						ModuleFuncNode nNewModFuncNode = new ModuleFuncNode(nNewStepNode, currentNode.getName());
						nNewModFuncNode.setModule(((ModuleFuncNode) currentNode).getModule());
						nNewModFuncNode.setFunction(((ModuleFuncNode) currentNode).getFunction());
						nNewStepNode.insert(nNewModFuncNode, 0);

						for (Enumeration<?> eModFuncChild = currentNode.children(); eModFuncChild.hasMoreElements();)
						{
							AssetNode currentParamGroupNode = (AssetNode) eModFuncChild.nextElement();

							if (currentParamGroupNode.getType().equals(NodeType.PARMS))
							{
								ParamNode nParametrNode = new ParamNode(currentNode, currentParamGroupNode.getName());
								nNewModFuncNode.insert(nParametrNode, currentNode.getIndex(currentParamGroupNode));
							}
							// If group
							else
							{
								ParamGroupNode nNewParamGroupNode = new ParamGroupNode(currentNode,
										currentParamGroupNode.getName());
								nNewModFuncNode.insert(nNewParamGroupNode, currentNode.getIndex(currentParamGroupNode));

								for (Enumeration<?> eGroupParam = currentParamGroupNode.children(); eGroupParam
										.hasMoreElements();)
								{
									ParamNode nSourceParamNode = (ParamNode) eGroupParam.nextElement();

									ParamNode nParametrNode = new ParamNode(nNewParamGroupNode,
											nSourceParamNode.getName());
									nNewParamGroupNode.insert(nParametrNode,
											currentParamGroupNode.getIndex(nSourceParamNode));
								}
							}
						}
					}
					// if subtest
					else
					{
						SubtestNode nSubTestNode = new SubtestNode(nNewStepNode, currentNode.getName());
						nNewStepNode.insert(nSubTestNode, 0);
					}
					test.reload();
					test.expandAll(true);
					test.setSelectionPath(new TreePath(nNewStepNode.getPath()));
				}
			}
		}
	}
	public static void addSubTest(String subTestPath, String subTestDescription,boolean isMandatory) throws Exception
	{
		TreePath pathItem = getTestSelectedPath();
		if (pathItem == null)
		{
			test.setSelectionPath(test.getRootPath());
			pathItem = getTestSelectedPath();
		}
			AssetNode currentNode = (AssetNode) pathItem.getLastPathComponent();
			if (currentNode.getType().equals(NodeType.ROOT))
			{
				StepNode nNewStepNode = new StepNode(currentNode, subTestDescription);
				currentNode.insert(nNewStepNode, 0);
				SubtestNode subtestNode = new SubtestNode(nNewStepNode, subTestPath);
				nNewStepNode.insert(subtestNode, 0);
				nNewStepNode.setMandatory(isMandatory, true);
				test.reload();
				test.expandAll(true);
				test.setSelectionPath(new TreePath(nNewStepNode.getPath()));
			}
			if (currentNode.getType().equals(NodeType.STEP))
			{
				AssetNode parent = (AssetNode) (currentNode.getParent());
				StepNode nNewStepNode = new StepNode(parent, subTestDescription);
				int nodeInd = parent.getIndex((TreeNode) currentNode);

				if (nodeInd - 1 >= 0) parent.insert(nNewStepNode, nodeInd);
				else parent.insert(nNewStepNode, 0);
				SubtestNode subtestNode = new SubtestNode(nNewStepNode, subTestPath);
				nNewStepNode.insert(subtestNode, 0);
				nNewStepNode.setMandatory(isMandatory, true);
				test.reload();
				test.expandAll(true);
				test.setSelectionPath(new TreePath(nNewStepNode.getPath()));
		}
	}

	private static void removeFromThreadAction() throws Exception
	{
		int threadInd = 0;
		ThreadNode parentThread = null;
		StepNode sourceStep = null;
		TreePath[] pathItems = getOrderSelectedPath();
		for (int ind = 0; ind < pathItems.length; ind++)
		{
			TreePath selectPath = pathItems[ind];
			AssetNode selectedNode = (AssetNode) selectPath.getLastPathComponent();
			
			if (selectedNode != null)
			{
				if (selectedNode.isType(NodeType.STEP) && selectedNode.isParentType(NodeType.THREAD))
				{
					sourceStep = (StepNode) selectedNode;
					parentThread = (ThreadNode) (sourceStep.getParent());
					if (ind == 0)
					{
						test.getTestNode().getIndex(parentThread);
						threadInd = test.getTestNode().getIndex(parentThread)+1;
					}
					else
					{
						threadInd++;
					}
					parentThread.remove(sourceStep);
					test.getTestNode().insert(sourceStep, threadInd);
					
				}

			}
		}
		if (parentThread.getChildCount()==0)
		{
			test.getTestNode().remove(parentThread);
			ThreadDialog.removeFromThread(parentThread.getName());
		}
		test.reload();
		test.expandAll(true);
	}
	private static void removeFromLoopAction() throws Exception
	{
		int loopInd = 0;
		LoopNode parentLoop = null;
		StepNode sourceStep = null;
		TreePath[] pathItems = getOrderSelectedPath();
		for (int ind = 0; ind < pathItems.length; ind++)
		{
			TreePath selectPath = pathItems[ind];
			AssetNode selectedNode = (AssetNode) selectPath.getLastPathComponent();
			
			if (selectedNode != null)
			{
				if (selectedNode.isType(NodeType.STEP) && selectedNode.isParentType(NodeType.LOOP))
				{
					sourceStep = (StepNode) selectedNode;
					parentLoop = (LoopNode) (sourceStep.getParent());
					if (ind == 0)
					{
						test.getTestNode().getIndex(parentLoop);
						loopInd = test.getTestNode().getIndex(parentLoop)+1;
					}
					else
					{
						loopInd++;
					}
					parentLoop.remove(sourceStep);
					test.getTestNode().insert(sourceStep, loopInd);
					
				}

			}
		}
		if (parentLoop.getChildCount()==0)
		{
			test.getTestNode().remove(parentLoop);
			LoopDialog.removeFromLoop(parentLoop.getName());
		}
		test.reload();
		test.expandAll(true);
	}
	private static void addThreadAction() throws Exception
	{
		ThreadDialog.showDialog(testLauncherGui,false);
	}
	private static void addLoopAction() throws Exception
	{
		LoopDialog.showDialog(testLauncherGui,false);
	}
	private static void addToThreadAction() throws Exception
	{
		ThreadDialog.showDialog(testLauncherGui,true);
	}
	private static void addToLoopAction() throws Exception
	{
		LoopDialog.showDialog(testLauncherGui,true);
	}
	public static ThreadNode addToThread(ThreadDataItem tdi) throws Exception
	{
		int threadInd =0;
		ThreadNode nThreadNode =null;
		TestNode parent=null;
		StepNode sourceStep=null;
		TreePath[] pathItems = test.getSelectionPaths();
		int index =0;
		for (int ind = 0; ind < pathItems.length; ind++)
		{
			TreePath selectPath = pathItems[ind];
			AssetNode selectedNode = (AssetNode) selectPath.getLastPathComponent();
			if (selectedNode != null)
			{
				if (selectedNode.isType(NodeType.STEP) && selectedNode.isParentType(NodeType.ROOT))
				{
					sourceStep = (StepNode) selectedNode;
					parent = (TestNode) (sourceStep.getParent());					
					if (nThreadNode==null)
					{
						if (tdi.getStatus())
						{
							threadInd = parent.getIndex(sourceStep);
							nThreadNode = new ThreadNode(tdi.getName());
						}
						else
						{
							nThreadNode= tdi.getNode();
							threadInd = parent.getIndex(nThreadNode);
						}
					}
						nThreadNode.insert(sourceStep, index++);
						test.updateNode(nThreadNode);
				}
			}
		}
		if (tdi.getStatus())
		parent.insert(nThreadNode, threadInd);				
		test.reload();
		test.expandAll(true);
		return nThreadNode;
	}
	public static LoopNode addToLoop(LoopDataItem tdi) throws Exception
	{
		int loopInd =0;
		LoopNode nLoopNode =null;
		TestNode parent=null;
		StepNode sourceStep=null;
		TreePath[] pathItems = test.getSelectionPaths();
		int index =0;
		for (int ind = 0; ind < pathItems.length; ind++)
		{
			TreePath selectPath = pathItems[ind];
			AssetNode selectedNode = (AssetNode) selectPath.getLastPathComponent();
			if (selectedNode != null)
			{
				if (selectedNode.isType(NodeType.STEP) && selectedNode.isParentType(NodeType.ROOT))
				{
					sourceStep = (StepNode) selectedNode;
					parent = (TestNode) (sourceStep.getParent());					
					if (nLoopNode==null)
					{
						if (tdi.getStatus())
						{
							loopInd = parent.getIndex(sourceStep);
							nLoopNode = new LoopNode(tdi.getName(),tdi.getStart(),tdi.getStop(),tdi.getStep());
						}
						else
						{
							nLoopNode= tdi.getNode();
							loopInd = parent.getIndex(nLoopNode);
						}
					}
						nLoopNode.insert(sourceStep, index++);
						test.updateNode(nLoopNode);
				}
			}
		}
		if (tdi.getStatus())
		parent.insert(nLoopNode, loopInd);				
		test.reload();
		test.expandAll(true);
		return nLoopNode;
	}
	public static void changeStepName(String sStepName,boolean isMandatory) throws Exception
	{
		 changeStepMandatory(isMandatory);
		 changeStepName(sStepName);
	}
	public static void changeStepMandatory(boolean isMandatory) throws Exception
	{
		
			AssetNode child =(AssetNode)test.getLastSelectedPathComponent();
			if (child.isParentType(NodeType.STEP))
			{
				((StepNode)child.getParent()).setMandatory(isMandatory,true);
			}
			test.updateNode(child);
			setTestDirty(true);
	}
	public static void changeStepName(String sStepName) throws Exception
	{
		if (((AssetNode)test.getLastSelectedPathComponent()).isType(NodeType.STEP))
		{
		((StepNode) test.getLastSelectedPathComponent()).setName(sStepName);
		}
		else
		{
			AssetNode child =(AssetNode)test.getLastSelectedPathComponent();
			if (child.isParentType(NodeType.STEP))
			{
				((StepNode)child.getParent()).setName(sStepName);
			}
		}
		test.reload();
		test.expandAll(true);
	}
	public static String getStepName() throws Exception
	{
		return ((StepNode) test.getLastSelectedPathComponent()).getName();
	}
	public static String getThreadName() throws Exception
	{
		return ((ThreadNode) test.getLastSelectedPathComponent()).getName();
	}
	public static void updateSubTestNode(SubtestNode subTestNode) throws Exception
	{
		test.updateNode(subTestNode);
		test.reload();
		test.expandAll(true);
		test.setSelectionPath(new TreePath(subTestNode.getPath()));
	}
	public static SubtestNode getSubTestNode() throws Exception
	{
		return ((SubtestNode) test.getLastSelectedPathComponent());
	}

	public static String getModFuncName() throws Exception
	{
		return ((ModuleFuncNode) test.getLastSelectedPathComponent()).getName();
	}
	public static boolean isModFuncMandatory() throws Exception
	{
		return ((ModuleFuncNode) test.getLastSelectedPathComponent()).isMandatory();
	}
	public static Boolean verifyStepNodeExist(String sNodeToFind)
	{
		TreeNode node = (TreeNode) test.getTestNode();
		if (node.getChildCount() >= 0)
		{
			for (Enumeration<?> e = node.children(); e.hasMoreElements();)
			{
				AssetNode currentNode = (AssetNode) e.nextElement();
				if (currentNode.getName().equalsIgnoreCase(sNodeToFind)) return true;
			}
		}
		return false;
	}
	public static int getNumOfSteps()
	{
		if (test.getTree() != null) return ((TreeNode) test.getTestNode()).getChildCount();
		else return 0;
	}
	private void showPopup(MouseEvent me) throws Exception
	{
		int x = me.getX();
		int y = me.getY();
		// save the path for future use
		TreePath clickedPath = test.getTree().getPathForLocation(x, y);
		if (clickedPath == null) return;

		// save the selected node
		AssetNode currentNode = (AssetNode) clickedPath.getLastPathComponent();
		if (me.getButton() == MouseEvent.BUTTON3)
		{
			popupMenu.removeAll();
			// show the popup menu
			if (currentNode.isType(NodeType.ROOT) && TestLauncherGui.testPanel.isTestDetailsOk)
			{
				popupMenu.add(AddSubTestAction.getInstance());
				
				if(isCopyToClipBoard)
				{
					if (test.isCopyThread())
						popupMenu.add(PasteThreadAction.getInstance(TestLauncherGui.getTreePanel()));
					else if (test.isCopyLoop())
						popupMenu.add(PasteLoopAction.getInstance(TestLauncherGui.getTreePanel()));
					else
						popupMenu.add(PasteStepAction.getInstance(TestLauncherGui.getTreePanel()));	
				}
				if (currentNode.getChildCount() > 0)
				{
					test.setSelectionPath(clickedPath);
					popupMenu.add(CollapseTreeAction.getInstance());
					popupMenu.add(ExpandTreeAction.getInstance());
					popupMenu.add(CheckTreeAction.getInstance());
					popupMenu.add(UnCheckTreeAction.getInstance());
					popupMenu.add(ShowXmlTestAction.getInstance(TestLauncherGui.getTreePanel()));
					popupMenu.show(test.getTree(), x, y);
				}
				else
				{
					test.setSelectionPath(clickedPath);
					popupMenu.show(test.getTree(), x, y);
				}
			}
			else if (TestLauncherGui.testPanel.isTestDetailsOk)
			{
				if (currentNode.isType(NodeType.THREAD))
				{
					popupMenu.add(OpenThreadManagerAction.getInstance(TestLauncherGui.getTreePanel()));
					if (test.getTree().getSelectionCount() == 1)
					{
						popupMenu.add(CopyThreadAction.getInstance(TestLauncherGui.getTreePanel()));
						if (isCopyToClipBoard)
						{
							if (test.isCopyThread()) popupMenu.add(PasteThreadAction.getInstance(TestLauncherGui.getTreePanel()));
							else popupMenu.add(PasteStepAction.getInstance(TestLauncherGui.getTreePanel()));
						}
					}
				}
				if (currentNode.isType(NodeType.LOOP))
				{
					popupMenu.add(OpenLoopManagerAction.getInstance(TestLauncherGui.getTreePanel()));
					if (test.getTree().getSelectionCount() == 1)
					{
						popupMenu.add(CopyLoopAction.getInstance(TestLauncherGui.getTreePanel()));
						if (isCopyToClipBoard)
						{
							if (test.isCopyThread()) popupMenu.add(PasteLoopAction.getInstance(TestLauncherGui.getTreePanel()));
							else popupMenu.add(PasteLoopAction.getInstance(TestLauncherGui.getTreePanel()));
						}
					}
				}
				if (currentNode.isType(NodeType.STEP))
				{
					if(((AssetNode)test.getLastPathComponent()).isType(NodeType.STEP))
					if (test.getTree().getSelectionCount() == 1)
					{
						popupMenu.add(RunStepAction.getInstance(TestLauncherGui.getTreePanel()));
						popupMenu.add(ChangeStepNameAction.getInstance(TestLauncherGui.getTreePanel()));
						popupMenu.add(AddSubTestAction.getInstance());
						popupMenu.add(CopyStepAction.getInstance(TestLauncherGui.getTreePanel()));
						if(isCopyToClipBoard)
						{
							if (test.isCopyThread())
								popupMenu.add(PasteThreadAction.getInstance(TestLauncherGui.getTreePanel()));
							else
								popupMenu.add(PasteStepAction.getInstance(TestLauncherGui.getTreePanel()));	
						}
					}
						popupMenu.add(DuplicateStepAction.getInstance(TestLauncherGui.getTreePanel()));					
						
					if (currentNode.isParentType(NodeType.ROOT))
					{
						popupMenu.add(AddToThreadAction.getInstance(TestLauncherGui.getTreePanel()));
						popupMenu.add(AddToLoopAction.getInstance(TestLauncherGui.getTreePanel()));
					}
					else
					{
						if (currentNode.isParentType(NodeType.THREAD))
						popupMenu.add(RemoveFromThreadAction.getInstance(TestLauncherGui.getTreePanel()));
						else if (currentNode.isParentType(NodeType.LOOP))
						popupMenu.add(RemoveFromLoopAction.getInstance(TestLauncherGui.getTreePanel()));
					}
				}
				else if (currentNode.isType(NodeType.SUBTEST))
				{
					if (test.getTree().getSelectionCount() == 1
							&& ((AssetNode) getTestSelectedPath().getLastPathComponent()).getType().equals(
									NodeType.SUBTEST)) popupMenu.add(ChangeSubTestAction.getInstance());
				}
				else if (currentNode.isType(NodeType.MOD_FUN))
				{
					if (test.getTree().getSelectionCount() == 1
							&& ((AssetNode)getTestSelectedPath().getLastPathComponent()).getType().equals(
									NodeType.MOD_FUN)) popupMenu.add(EditConfParamAction.getInstance());
				}
				else if (currentNode.isType(NodeType.PARMS))
				{
					if (test.getTree().getSelectionCount() == 1
							&& ((AssetNode) getTestSelectedPath().getLastPathComponent()).getType().equals(
									NodeType.PARMS))
					{

						if (((ParamNode) test.getTree().getSelectionPath().getLastPathComponent()).isSelected)
						{
							popupMenu.add(SuspendParamAction.getInstance(TestLauncherGui.getTreePanel()));
						}
						else
						{
							popupMenu.add(UnSuspendParamAction.getInstance(TestLauncherGui.getTreePanel()));

						}
					}
				}
				if (currentNode.getParent().getChildCount() > 1)
				{
					popupMenu.add(MoveUpAction.getInstance(TestLauncherGui.getTreePanel()));
					popupMenu.add(MoveToTopAction.getInstance(TestLauncherGui.getTreePanel()));
					popupMenu.add(MoveDownAction.getInstance(TestLauncherGui.getTreePanel()));
					popupMenu.add(MoveToBottomAction.getInstance(TestLauncherGui.getTreePanel()));
					popupMenu.add(RemoveItemAction.getInstance(TestLauncherGui.getTreePanel()));
				}
				else
				{
					popupMenu.add(RemoveItemAction.getInstance(TestLauncherGui.getTreePanel()));
				}
				popupMenu.show(test.getTree(), x, y);
			}
		}
		else if (me.getButton() == MouseEvent.BUTTON1)
		{
			if (me.getClickCount() == 2)
			{
				if (currentNode.isType(NodeType.PARMS))
				{
					ModuleFuncNode nModuleFuncNode;
					if (currentNode.isParentType(NodeType.MOD_FUN))
					{
						nModuleFuncNode = (ModuleFuncNode) currentNode.getParent();
					}
					else
					{
						nModuleFuncNode = (ModuleFuncNode) currentNode.getParent().getParent();
					}
					Vector<ParamDataItem> vec_items = null;
					try
					{
						String sModFunc[] = nModuleFuncNode.getName().split("/");
						String sModule = sModFunc[0];
						String sFunction = sModFunc[1];
						vec_items = getModFunParam(nModuleFuncNode);
						TestLauncherGui.parameterDialog.edit(nModuleFuncNode.isMandatory(),sModule, sFunction, vec_items);
					}
					catch (Exception e1)
					{
						e1.printStackTrace();
					}
				}
			}
			performEventSelected(me.getX(), me.getY());
			handelSelectedToolBar();
		}
	}

	public static void handelSelectedToolBar()
	{
		TreePath selectPath = test.getTree().getSelectionModel().getSelectionPath();
		TreePanel.repeatCheckBox.setEnabled(true);
		TreePanel.repeatAmountLeft.setEnabled(true);
		TreePanel.repeatAmount.setEnabled(true);
		TreePanel.addThreadButton.setEnabled(true);
		TreePanel.addLoopButton.setEnabled(true);
		TreePanel.showLogButton.setEnabled(true);
		TreePanel.autoSaveButton.setEnabled(true);
		if (selectPath == null)
		{
			TreePanel.addSubTestButton.setEnabled(false);
			TreePanel.deleteButton.setEnabled(false);
			TreePanel.topButton.setEnabled(false);
			TreePanel.bottomButton.setEnabled(false);
			TreePanel.upButton.setEnabled(false);
			TreePanel.downButton.setEnabled(false);
			TreePanel.expandTreeButton.setEnabled(false);
			TreePanel.collapseTreeButton.setEnabled(false);
			TreePanel.deleteAllButton.setEnabled(false);
			TreePanel.checkTreeButton.setEnabled(false);
			TreePanel.unCheckTreeButton.setEnabled(false);
			TreePanel.saveTestButton.setEnabled(false);
			TreePanel.saveAsTestButton.setEnabled(false);
			TreePanel.openTestButton.setEnabled(true);
			TreePanel.searchButton.setEnabled(false);
			return;
		}
		else
		{
			AssetNode node = (AssetNode) selectPath.getLastPathComponent();
			TreePanel.saveTestButton.setEnabled(true);
			TreePanel.saveAsTestButton.setEnabled(true);
			TreePanel.openTestButton.setEnabled(true);
			TreePanel.searchButton.setEnabled(true);
			if (getTestLauncherGui().getTestPanel().isTestDetailsOk)
			{
				TreePanel.runTestButton.setEnabled(true);
			}
			if (node.getType().equals(NodeType.ROOT))
			{
				if (getTestLauncherGui().getTestPanel().isTestDetailsOk)
				{
					TreePanel.addSubTestButton.setEnabled(true);
				}
				if (node.getChildrenCount() > 0)
				{
					TreePanel.expandTreeButton.setEnabled(true);
					TreePanel.collapseTreeButton.setEnabled(true);
					TreePanel.deleteAllButton.setEnabled(true);
					TreePanel.checkTreeButton.setEnabled(true);
					TreePanel.unCheckTreeButton.setEnabled(true);
				}
			}
			else if (node.getType().equals(NodeType.STEP))
			{
				TreePanel.addSubTestButton.setEnabled(true);
				TreePanel.deleteAllButton.setEnabled(true);
				TreePanel.deleteButton.setEnabled(true);
				TreePanel.topButton.setEnabled(true);
				TreePanel.bottomButton.setEnabled(true);
				TreePanel.upButton.setEnabled(true);
				TreePanel.downButton.setEnabled(true);
			}
			else if (node.getType().equals(NodeType.MOD_FUN) || node.getType().equals(NodeType.SUBTEST))
			{
				TreePanel.addSubTestButton.setEnabled(false);
				TreePanel.deleteAllButton.setEnabled(true);
				TreePanel.deleteButton.setEnabled(true);
				TreePanel.topButton.setEnabled(true);
				TreePanel.bottomButton.setEnabled(true);
				TreePanel.upButton.setEnabled(true);
				TreePanel.downButton.setEnabled(true);
			}
			else if (node.getType().equals(NodeType.PARMS) || node.getType().equals(NodeType.PARAMGROUP))
			{
				TreePanel.addSubTestButton.setEnabled(false);
				TreePanel.deleteAllButton.setEnabled(true);
				TreePanel.deleteButton.setEnabled(true);
				TreePanel.topButton.setEnabled(true);
				TreePanel.bottomButton.setEnabled(true);
				TreePanel.upButton.setEnabled(true);
				TreePanel.downButton.setEnabled(true);
			}
		}
	}

	private void performEventSelected(int x, int y) throws Exception
	{
		int row = test.getTree().getRowForLocation(x, y);
		TreePath path = test.getTree().getPathForRow(row);

		if (path == null) return;

		if (x > test.getTree().getPathBounds(path).x + hotspot) return;

		AssetNode node = (AssetNode) path.getLastPathComponent();
		if (node.isType(NodeType.STEP)||node.isType(NodeType.THREAD)||node.isType(NodeType.LOOP))
		{
			boolean isSelected = !(node.isSelected());
			node.setSelected(isSelected);
			test.updateNode(node);
			if(node.isType(NodeType.THREAD))
			{	
				node.setSelectedChildren(isSelected);
				test.updateNode(node);
			}
			if (node.isType(NodeType.STEP) && node.isParentType(NodeType.THREAD))
			{
				if (((AssetNode)node.getParent()).isSelected && node.getParentSelectedChildrenCount() == 0)
				{
					((AssetNode)node.getParent()).setSelected(false);
					test.updateNode(node);
				}
				else
				{
					((AssetNode)node.getParent()).setSelected(true);
					test.updateNode(node);
				}
			}
			if (node.isType(NodeType.STEP) && node.isParentType(NodeType.LOOP))
			{
				if (((AssetNode)node.getParent()).isSelected && node.getParentSelectedChildrenCount() == 0)
				{
					((AssetNode)node.getParent()).setSelected(false);
					test.updateNode(node);
				}
				else
				{
					((AssetNode)node.getParent()).setSelected(true);
					test.updateNode(node);
				}
			}
			test.reload();
			test.expandAll(true);
		}

		if (row == 0)
		{
			test.repaint();
		}
	}

	public void keyPressed(KeyEvent e) {
		int ketCode = e.getKeyCode();
		// TODO Auto-generated method stub
		if (e.getSource().equals(test.getTree())) {
			if ((AssetNode) test.getTree().getLastSelectedPathComponent() != null) {
				if (ketCode == KeyEvent.VK_DELETE) {
					try {
						removeCurrentNodeAction();
						setTestDirty(true);
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
				if (((AssetNode) test.getLastSelectedPathComponent()).getType().equals(NodeType.ROOT)) 
				{
					if ((ketCode == KeyEvent.VK_V)
							&& ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)) {
						pasteAction();
					}
				} else if (((AssetNode) test.getLastSelectedPathComponent()).getType().equals(NodeType.STEP)) 
				{
					if ((e.getKeyCode() == KeyEvent.VK_D)
							&& ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)) {
						try {
							duplicateStepAction();
						} catch (Exception e1) {
							XTTProperties
									.printDebug("Gui: Fail to duplicate step");
						}
					}
					if ((ketCode == KeyEvent.VK_C) && ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)) {
						e.setKeyCode(44);
						try {
							if (((AssetNode) test.getLastSelectedPathComponent()).getType().equals(NodeType.STEP)) 
							{
								isCopyToClipBoard = true;
								test.setCopyLoop(false);
								test.setCopyThread(false);
								copyStepAction();
							}
						} catch (Exception e1) {
							XTTProperties.printDebug("Gui: Fail to copy step");
						}
					}
					if ((ketCode == KeyEvent.VK_V) && ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)) {
						pasteAction();
					}
					if (ketCode == KeyEvent.VK_F2) {
						try {
							changeStepNameAction();
							setTestDirty(true);
						} catch (Exception e1) {
							XTTProperties
									.printDebug("Gui: Fail to rename step");
						}
					}
				} 
				else if (((AssetNode) test.getLastSelectedPathComponent()).getType().equals(NodeType.THREAD)) 
				{
					if ((ketCode == KeyEvent.VK_C)
							&& ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)) {
						e.setKeyCode(44);
						try {
							isCopyToClipBoard = true;
							test.setCopyLoop(false);
							test.setCopyThread(true);
							copyThreadAction();
						} catch (Exception e1) {
							XTTProperties
									.printDebug("Gui: Fail to copy thread");
						}
					}
					if ((ketCode == KeyEvent.VK_V)
							&& ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)) {
						pasteAction();
					}
				}
				else if (((AssetNode) test.getLastSelectedPathComponent()).getType().equals(NodeType.LOOP)) 
				{
					if ((ketCode == KeyEvent.VK_C)
							&& ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)) {
						e.setKeyCode(44);
						try {
							isCopyToClipBoard = true;
							test.setCopyLoop(true);
							test.setCopyThread(false);
							copyLoopAction();
						} catch (Exception e1) {
							XTTProperties
									.printDebug("Gui: Fail to copy loop");
						}
					}
					if ((ketCode == KeyEvent.VK_V)
							&& ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)) {
						pasteAction();
					}
				}
				if (ketCode == KeyEvent.VK_F5) {
					try {
						runTestAction();
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
			} else if (ketCode == KeyEvent.VK_F5) {
				try {
					runTestAction();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
			if ((e.getKeyCode() == KeyEvent.VK_F)
					&& ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)) {
				try {
					SearchTreeDialog.showSearchTreeDialog(testLauncherGui);
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
	}
	public static TestLauncherGui getTestLauncherGui() {
		return testLauncherGui;
	}
	public static void setTestLauncherGui(TestLauncherGui testLauncherGui) {
		TreeTestController.testLauncherGui = testLauncherGui;
	}
	public static String getTestFileFullPath()
	{
		return test.getFilePath();
	}
	public static AssetNode getTestNode()
	{
		return test.getTestNode();
	}
	public static String getTestFileName()
	{
		return test.getFileNode().getName();
	}
	public static void setTestFilePath(String filePath)
	{
		test.setFilePath(filePath);
	}
	public static void setTestFileNode(FileNode fileNode)
	{
		test.setFileNode(fileNode);
	}
	public static FileNode getTestFileNode()
	{
		return test.getFileNode();
	}
	public static TreePath getTestSelectedPath()
	{
		return test.getSelectionPath();		
	}
	public static AssetNode getTestLastPathComponent()
	{
		return (AssetNode)test.getSelectionPath().getLastPathComponent();		
	}
	public void valueChanged(TreeSelectionEvent e)
	{
	}
	public void mouseReleased(MouseEvent e){}
	public void mouseEntered(MouseEvent e){}
	public void mouseExited(MouseEvent e){}
	public void mouseClicked(MouseEvent e){}
	public void keyTyped(KeyEvent e){}
	public void keyReleased(KeyEvent e){}
}