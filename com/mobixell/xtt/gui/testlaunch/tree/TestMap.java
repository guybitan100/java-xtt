package com.mobixell.xtt.gui.testlaunch.tree;

import java.awt.Font;
import java.io.File;
import java.io.FileWriter;
import java.util.Enumeration;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.mobixell.xtt.XTTProperties;
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
import com.mobixell.xtt.gui.testlaunch.tree.nodes.ThreadNode;
import com.mobixell.xtt.gui.testlaunch.tree.nodes.TreeNodeRenderer;
import com.mobixell.xtt.gui.testlaunch.tree.nodes.AssetNode.NodeType;

public class TestMap
{
	public  JTree tree;
	private String name;
	private String description;
	private String stepsDesc;
	private String designer;
	private String creationDate;
	private String level;
	private String xfwVersion;
	private String qcID;
	private File file;
	private TestNode rootNode = null;
	private DefaultTreeModel treeModel;
	private TreePath path;
	private StringBuffer testXml;
	private String filePath="";
	private FileNode fileNode=null;
	private FolderNode folderNode=null;
	private  boolean isDirty =false; 
	private static AutoSaveWorker autoSave = null;
	private boolean isCopyThread = false;
	private boolean isCopyLoop = false;
	
	public TestMap()
	{
		rootNode = new TestNode("Test");
		rootNode.setDescription("Test Description: ");
		rootNode.setSelected(true);
		treeModel = new DefaultTreeModel(rootNode);
		tree = new JTree(treeModel);
		tree.setFont(new Font(Font.SERIF, Font.PLAIN, 15));
		tree.setShowsRootHandles(true);
		tree.setCellRenderer(new TreeNodeRenderer());
		tree.setSelectionPath(new TreePath(rootNode.getPath()));
		
		autoSave = new AutoSaveWorker();
        autoSave.start();
	}

	public String getName()
	{
		return name;
	}

	public void setName(String testName)
	{
		name = testName;
		getTestNode().setName(testName);
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String testDescription)
	{
		description = testDescription;
		getTestNode().setDescription(testDescription);
	}
	
	public String getStepsDesc()
	{
		return stepsDesc;
	}

	public void setStepsDesc(String stepsDesc)
	{
		this.stepsDesc = stepsDesc;
		getTestNode().setStepsDesc(stepsDesc);
	}

	public String getDesigner()
	{
		return designer;
	}

	public void setDesigner(String designer)
	{
		this.designer = designer;
		getTestNode().setDesigner(designer);
	}

	public String getCreationDate()
	{
		return creationDate;
	}

	public void setCreationDate(String creationDate)
	{
		this.creationDate = creationDate;
		getTestNode().setCreationDate(creationDate);
	}

	public String getLevel()
	{
		return level;
	}

	public void setLevel(String level)
	{
		this.level = level;
		getTestNode().setLevel(level);
	}

	public String getXfwVersion()
	{
		return xfwVersion;
	}

	public void setXfwVersion(String xfwVersion)
	{
		this.xfwVersion = xfwVersion;
		getTestNode().setXfwVersion(xfwVersion);
	}

	public String getQcID()
	{
		return qcID;
	}

	public void setQcID(String qcID)
	{
		this.qcID = qcID;
		getTestNode().setQcID(qcID);
	}
	public TreePath getRootPath()
	{
		path = new TreePath (getTestNode().getPath());
	    return path;
	}
	public StringBuffer updateAndGetTestXml(boolean includeUnSelected) throws Exception
	{
		testXml= getXmlTest(includeUnSelected);
		return testXml;
	}
	public StringBuffer updateAndGetTestXml() throws Exception
	{
		testXml= getXmlTest(true);
		return testXml;
	}
	public void updateXml(boolean includeUnSelected) throws Exception
	{
		testXml= getXmlTest(includeUnSelected);
	}
	public StringBuffer getXml() throws Exception
	{
		return testXml;
	}
	public TreePath pathByAddingChild(Object child)
	{
		path = new TreePath (getTestNode()).pathByAddingChild(child);
	    return path;
	}
	public void removeAllSteps() throws Exception
	{
		getTestNode().removeAllChildren();
	}
	public void removeRootChildrens() throws Exception
	{
		getTestNode().removeRootAllChildrens();
	}
	public boolean removeUnselected() throws Exception
	{
		return getTestNode().removeRootUnselectedChildrens();
	}
	public void setSelectionPaths(TreePath[] paths)
	{
		tree.setSelectionPaths(paths);
	}
	public void setSelectionPath(TreePath path)
	{
		tree.setSelectionPath(path);
	}
	public TreePath getSelectionPath()
	{
		return tree.getSelectionPath();
	}

	public TreePath[] getSelectionPaths()
	{
		return tree.getSelectionPaths();
	}
	public File getFile()
	{
		return file;
	}

	public void setFile(File file)
	{
		this.file = file;
	}

	public JTree getTree()
	{
		return tree;
	}
	public TreeModel getModel()
	{
		return tree.getModel();
	}
	public TestNode getTestNode()
	{
		return (TestNode) tree.getModel().getRoot();
	}
	public Object getLastPathComponent()
	{
		return tree.getSelectionPath().getLastPathComponent();
	}
	public void updateNode(TreeNode node)
	{
		((DefaultTreeModel) getModel()).nodeChanged(node);
	}
	public void updateTestNode()
	{
		((DefaultTreeModel) getModel()).nodeChanged(getTestNode());
	}
	public void insertNodeInto (MutableTreeNode newChild,
                               MutableTreeNode parent, int index)
	{
		treeModel.insertNodeInto(newChild, parent, index);
	}
	public int getChildCount() {
		return getTestNode().getChildCount();
	}
	public Object getLastSelectedPathComponent()
	{
		return tree.getLastSelectedPathComponent();
	}
	public void expandAll(boolean expand) throws Exception
	{
		expandSelected(getRootPath(),expand);
		setSelectionPath(getRootPath());
	}
	
	public void expandSelected(TreePath parent, boolean expand) throws Exception
	{
		// Traverse children
		AssetNode node = (AssetNode) parent.getLastPathComponent();
		if (node.getChildCount() >= 0)
		{
			for (Enumeration<?> e = node.children(); e.hasMoreElements();)
			{
				AssetNode n = (AssetNode) e.nextElement();
				TreePath path = parent.pathByAddingChild(n);
				expandSelected(path, expand);
			}
		}
		// Expansion or collapse must be done bottom-up
		if (expand)
		{
			expandPath(parent);
		}
		else
		{
			collapsePath(parent);
		}
	}
	public void repaint()
	{
		tree.revalidate();
		tree.repaint();
	}

	public void expandPath(TreePath path)
	{
		tree.expandPath(path);
	}

	public void collapsePath(TreePath path)
	{
		tree.collapsePath(path);
	}

	public void setToolTipText(String text)
	{
		tree.setToolTipText(text);
	}
	public TreePath getPathForLocation(int x, int y)
	{
		return tree.getPathForLocation(x, y);
	}
	public void reload ()
	{
		((DefaultTreeModel) tree.getModel()).reload();
	}
	private StringBuffer getXmlTest(boolean includeUnSelected) throws Exception
	{
		StringBuffer sbTest = new StringBuffer();
		setStepsDesc(getAllSteps());
		synchronized (sbTest)
		{
		sbTest.append("<test>\n");
		sbTest.append("<name>" + getName() + "</name>\n");
		sbTest.append("<qcid>" + getQcID() + "</qcid>\n");
		sbTest.append("<description>\n" + getDescription() + "\n" + "</description>\n");
		sbTest.append("<steps>\n" + getStepsDesc() + "</steps>\n");
		sbTest.append("<designer>" + getDesigner() + "</designer>\n");
		sbTest.append("<creationdate>" + getCreationDate() + "</creationdate>\n");
		sbTest.append("<testlevel>" + getLevel() + "</testlevel>\n");
		sbTest.append("<xfwVersion>" + getXfwVersion() + "</xfwVersion>\n");
		}
		for (Enumeration<?> eTestChild = getTestNode().children(); eTestChild.hasMoreElements();)
		{
			AssetNode nAssetNode = (AssetNode) eTestChild.nextElement();
			if (nAssetNode.isType(NodeType.STEP))
			{
				getXmlFromStepNode(sbTest, (StepNode) nAssetNode, includeUnSelected);
			}
			else if (nAssetNode.isType(NodeType.THREAD))
			{
				if (nAssetNode.isSelected || includeUnSelected)
				{
					sbTest.append("<threadname>" + ((ThreadNode) nAssetNode).getName() + "</threadname>\n");
					sbTest.append("<thread>\n");
					for (Enumeration<?> eThredChild = nAssetNode.children(); eThredChild.hasMoreElements();)
					{
						StepNode nStepNode = (StepNode) eThredChild.nextElement();
						getXmlFromStepNode(sbTest, nStepNode, includeUnSelected);
					}
					sbTest.append("</thread>\n");
				}
			}
			else if (nAssetNode.isType(NodeType.LOOP))
			{
				if (nAssetNode.isSelected || includeUnSelected)
				{
					sbTest.append("<loop name=" + "\"" + 
							     ((LoopNode) nAssetNode).getName() +
							      "\"" +" start= " +"\"" + 
							      ((LoopNode) nAssetNode).getStart()+
							      "\"" +" stop= "+"\"" + 
							      ((LoopNode) nAssetNode).getStop() +
							      "\"" + " step= " +"\"" +
							      ((LoopNode) nAssetNode).getStep() +
							      "\"" +" >\n");
					for (Enumeration<?> eThredChild = nAssetNode.children(); eThredChild.hasMoreElements();)
					{
						StepNode nStepNode = (StepNode) eThredChild.nextElement();
						getXmlFromStepNode(sbTest, nStepNode, includeUnSelected);
					}
					sbTest.append("</loop>\n");
				}
			}
		}
		sbTest.append("</test>");
		return sbTest;
	}
	public StringBuffer getXmlStep(boolean isExec) throws Exception
	{
		StepNode nStepNode = (StepNode) getLastPathComponent();
		StringBuffer sbTest = new StringBuffer();
		if (isExec)
		{
		sbTest.append("<!--\n" + getName() + "\n" + getDescription() + "\n"
				+ getStepsDesc() + "\n-->" + "\n");
		sbTest.append("<test>\n");
		}
		else
		{
			sbTest.append("<step>\n");
		}
		// stepDesc = "<!--" + nStepNode.getName() + "-->\n";
		getXmlFromStepNode(sbTest,nStepNode,false);
		if (isExec) 
			sbTest.append("</test>");
		else
			sbTest.append("</step>");
		return sbTest;
	}

	public StringBuffer getXmlThread() throws Exception
	{
		ThreadNode nThreadNode = (ThreadNode) getLastPathComponent();
		StringBuffer sbTest = new StringBuffer();
		sbTest.append("<thread>\n");

		sbTest.append("<threadname>" + ((ThreadNode) nThreadNode).getName() + "</threadname>\n");
		sbTest.append("<thread>\n");

		for (Enumeration<?> eTestChild = nThreadNode.children(); eTestChild.hasMoreElements();)
		{
			StepNode nStepNode = (StepNode) eTestChild.nextElement();
			getXmlFromStepNode(sbTest, nStepNode, false);
		}
		sbTest.append("</thread>\n");
		sbTest.append("</thread>\n");
		return sbTest;
	}
	public StringBuffer getXmlLoop() throws Exception
	{
		LoopNode nLoopNode = (LoopNode) getLastPathComponent();
		StringBuffer sbTest = new StringBuffer();
		// <loop name="connection" start="1" stop="6" step="1">
		sbTest.append("<loop>\n");
		sbTest.append("<loop name=" + "\"" 
		+ nLoopNode.getName()+"\"" 
	    + " start= " +"\"" 
		+ nLoopNode.getStart()
		+ "\"" +" stop= "
		+ "\"" + nLoopNode.getStop() 
		+"\"" + " step= " +"\"" 
		+nLoopNode.getStep() 
		+"\"" +" >\n");
		for (Enumeration<?> eTestChild = nLoopNode.children(); eTestChild.hasMoreElements();)
		{
			StepNode nStepNode = (StepNode) eTestChild.nextElement();
			getXmlFromStepNode(sbTest, nStepNode, false);
		}
		sbTest.append("</loop>\n");
		sbTest.append("</loop>\n");
		return sbTest;
	}
	public StringBuffer getXmlFromStepNode(StringBuffer sbTest, StepNode nStepNode,boolean includeUnSelected) throws Exception
	{
		String funcMod = "";
		String funcModEnd = "";
		String parameters = "";
		String stepName = "";
		String stepId = "";
		String isMandatory = "";
		boolean isParameter = false;
		
		setStepsDesc(getAllSteps());
		
		isMandatory = "<mandatory>" + Boolean.toString(nStepNode.isMandatory()) +"</mandatory>\n";
		stepName =    "<stepname>" + nStepNode.getName() + "</stepname>\n";
		stepId =      "<stepid>" + nStepNode.getId() + "</stepid>\n";
		
		
		if (nStepNode.isSelected || includeUnSelected)
		{
			AssetNode assetNode = (AssetNode) nStepNode.getChildAt(0);

			if (assetNode.getType().equals(NodeType.MOD_FUN))
			{
				ModuleFuncNode nModFuncNode = (ModuleFuncNode) nStepNode.getChildAt(0);

				funcMod = "<function name=" + "\"" + nModFuncNode.getFunction() + "\"" + " module=" + "\""
						+ nModFuncNode.getModule() + "\"";

				for (Enumeration<?> eModFuncChild = nModFuncNode.children(); eModFuncChild.hasMoreElements();)
				{
					AssetNode node = (AssetNode) eModFuncChild.nextElement();

					if (node.getType().equals(NodeType.PARMS))
					{
						if (node.isSelected)
						{
							ParamNode nParametrNode = (ParamNode) node;
							isParameter = true;
							parameters = parameters + nParametrNode.getName() + "\n";
						}
					}
					// If group
					else
					{
						ParamGroupNode nParamGroupNode = (ParamGroupNode) node;
						if (nParamGroupNode.getSelectedChildrenCount() > 0)
						{
							parameters = parameters + "<parameter>\n";
							for (Enumeration<?> eGroupParam = nParamGroupNode.children(); eGroupParam.hasMoreElements();)
							{
								ParamNode nParametrNode = (ParamNode) eGroupParam.nextElement();
								if (nParametrNode.isSelected)
								{
									isParameter = true;
									parameters = parameters + nParametrNode.getName() + "\n";
								}
							}
						}
						if (nParamGroupNode.getSelectedChildrenCount() > 0)
						{
							parameters = parameters + "</parameter>\n";
						}
						else nParamGroupNode.setSelected(false);
					}
				}
			}
			// if subtest
			else
			{
				SubtestNode nSubTestNode = (SubtestNode) assetNode;
				funcMod = funcMod + "<subtest>" + nSubTestNode.getName() + "</subtest>\n";
			}
			if (!isParameter && funcMod.indexOf("subtest") < 0) funcModEnd = "/>\n";
			else if (isParameter && funcMod.indexOf("subtest") < 0) funcModEnd = "</function>\n";

			if (funcModEnd.indexOf("function") >= 0 && funcMod.indexOf("subtest") < 0) 
				sbTest.append(isMandatory + stepName + stepId + funcMod+ ">\n" + parameters + funcModEnd);
			else 
				sbTest.append(isMandatory + stepName + stepId + funcMod + funcModEnd + parameters);

			isParameter = false;
			funcModEnd = "";
			stepName = "";
			funcMod = "";
			parameters = "";
		}
		return sbTest;
	}

	public String getAllSteps()
	{
		String steps = "";
		TreeNode node = (TreeNode) getTestNode();
		if (node.getChildCount() >= 0)
		{
			for (Enumeration<?> eSteps = node.children(); eSteps.hasMoreElements();)
			{
				AssetNode assetNode = (AssetNode) eSteps.nextElement();
				if (assetNode.getType().equals(NodeType.STEP))
				{
					steps = steps + assetNode.getName() + "\n";
				}
				else if (assetNode.getType().equals(NodeType.THREAD))
				{
					for (Enumeration<?> e = assetNode.children(); e.hasMoreElements();)
					{
						AssetNode threadStepsNode = (AssetNode) e.nextElement();
						steps = steps + assetNode.getName() + ": " + threadStepsNode.getName() + "\n";
					}
				}

			}
		}
		return steps;
	}
	public void setAutoSaveEnabled (boolean isWantAutoSave)
	{
		autoSave.setWantAutoSave(isWantAutoSave) ;
	}
	public synchronized void save() throws Exception
	{
		FileWriter writer = null;
		if (filePath != null && filePath != "")
		{
			try
			{
				writer = new FileWriter(filePath);
				XTTProperties.printDebug("Save File: " + filePath);
				
			}
			catch (Exception e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
		{
			writer = new FileWriter(System.getProperty("user.dir")+"\\tests\\XMP\\WorkingTest.xml");
			filePath=System.getProperty("user.dir")+"\\tests\\XMP\\WorkingTest.xml";
			XTTProperties.printDebug("Save File: " + System.getProperty("user.dir")+"\\tests\\XMP\\WorkingTest.xml");
		}
		writer.append(updateAndGetTestXml(true));
		writer.flush();
		writer.close();
		isDirty = false;
		
	}
	public FileNode getFileNode()
	{
		return fileNode;
	}

	public void setFileNode(FileNode fileNode)
	{
		this.fileNode = fileNode;
	}

	public String getFilePath()
	{
		return filePath;
	}

	public void setFilePath(String filePath)
	{
		this.filePath = filePath;
	}

	public FolderNode getFolderNode()
	{
		return folderNode;
	}

	public void setFolderNode(FolderNode folderNode)
	{
		this.folderNode = folderNode;
	}

	public synchronized boolean isDirty()
	{
		return isDirty;
	}
	public void setDirty(boolean isDirty)
	{
		this.isDirty = isDirty;
	}
	public boolean isCopyThread()
	{
		return isCopyThread;
	}

	public void setCopyThread(boolean isCopyThread)
	{
		this.isCopyThread = isCopyThread;
	}

	public boolean isCopyLoop() {
		return isCopyLoop;
	}

	public void setCopyLoop(boolean isCopyLoop) {
		this.isCopyLoop = isCopyLoop;
	}
}
