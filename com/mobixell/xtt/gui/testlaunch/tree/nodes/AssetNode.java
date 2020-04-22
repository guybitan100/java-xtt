/*
 * Copyright 2005-2010 Ignis Software Tools Ltd. All rights reserved.
 */
package com.mobixell.xtt.gui.testlaunch.tree.nodes;

import java.io.File;
import java.util.Enumeration;
import java.util.Vector;
import java.util.logging.Logger;
import javax.swing.tree.DefaultMutableTreeNode;

import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.gui.testlaunch.panels.LoopDialog;
import com.mobixell.xtt.gui.testlaunch.panels.ThreadDialog;
/**
 * Representing Node at the Tests tree.
 * 
 * 
 * @author guy.bitan
 * 
 */
public abstract class AssetNode extends DefaultMutableTreeNode
{
private static final long serialVersionUID = -7663450817941582253L;
protected static Logger log = Logger.getLogger(AssetNode.class.getName());

	public boolean isSelected;
	protected boolean isBselected;
	private boolean isMandatory = false;
	private int status = XTTProperties.NOT_RUNNING;
	private String id = "";
	private static Vector<String[]> loadErrors = new Vector<String[]>();
	/**
	 * Type of the node.
	 */
	private NodeType type;
	/**
	 * Name of the node.
	 */
	private String name;
	
	public static enum NodeType {
		ROOT,STEP,MOD_FUN, PARMS,SUBTEST,PARAMGROUP,FOLDER,FILE,LIST,MODULE,FUNCTION,THREAD,LOOP
	}
	public static void initFailLoadClassVector() 
	{
		loadErrors = null;
	}
	public static boolean isErrorsFound() 
	{
		return (loadErrors != null);
	}
	public static Vector<String[]> getLoadsErrors() 
	{
		return loadErrors;
	}
	public AssetNode(Object userObject,NodeType nodeType) 
	{
		super(userObject,true);
		isSelected = true;
		this.type=nodeType;
		this.name = (String) userObject;
	}
	protected void initChildren(Object[] child) throws Exception
	{
		if (children == null)
		{
			children = new Vector<Object>();
		}
		for (int i = 0; i < child.length; i++)
		{
			if (child[i] instanceof File)
			{
				// children.addElement(new ScriptNode(this, rs));
			}
		}
	}
	protected Object getRootUserObject() 
	{
		if (parent instanceof TestNode) 
		{
			return getUserObject();
		} else 
		{
			return ((AssetNode) parent).getRootUserObject();
		}
	}
	protected int getTestsCount() {
		if(children == null){
			return 0;
		}
		int size = children.size();
		int count = 0;
		for (int i = 0; i < size; i++) {
			count += ((AssetNode) children.elementAt(i)).getTestsCount();
		}
		return count;
	}
	public Enumeration<Object> getAllChildren() 
	{
		Vector<Object> tmp = new Vector<Object>();
		get(this, tmp);
		return tmp.elements();
	}
	public int getChildrenCount() {
		Vector<Object> tmp = new Vector<Object>();
		get(this, tmp);
		return tmp.size();
	}
	public int getSelectedChildrenCount() {return getSelectedChildrenCount(this);}
	// Get all selected children
	public int getSelectedChildrenCount(AssetNode node) {

		int slectedCount=0;
		Enumeration <Object> enChildNode = node.getAllChildren();
   	  while (enChildNode.hasMoreElements()) 
   	  {
   		  AssetNode childNode = (AssetNode)enChildNode.nextElement();
   		  if(childNode.isSelected())
   			slectedCount++;
   	  }
		return slectedCount;
	}
	public int getParentSelectedChildrenCount() {return getParentSelectedChildrenCount(this);}
	// Get all selected children
	public int getParentSelectedChildrenCount(AssetNode node)
	{
		int slectedCount = 0;
		Enumeration<AssetNode> enChildNode = ((AssetNode) node.getParent()).children();
		while (enChildNode.hasMoreElements())
		{
			AssetNode childNode = (AssetNode) enChildNode.nextElement();
			if (childNode.isSelected()) slectedCount++;
		}
		return slectedCount;
	}
	public boolean removeRootUnselectedChildrens()
	{
		boolean isRemove =false;
		Enumeration<AssetNode> enChildNode = this.children();
		while (enChildNode.hasMoreElements())
		{
			AssetNode childNode = (AssetNode) enChildNode.nextElement();
			if (!childNode.isSelected())
			{
				if (childNode.isType(NodeType.THREAD))
				{
					ThreadDialog.removeFromThread(childNode.getName());
				}
				if (childNode.isType(NodeType.LOOP))
				{
					LoopDialog.removeFromLoop(childNode.getName());
				}
				this.remove(childNode);
				enChildNode = this.children();
				isRemove =true;
			}
		}
		return isRemove;
	}

	public void removeRootAllChildrens()
	{
		Enumeration<AssetNode> enChildNode = this.children();
		while (enChildNode.hasMoreElements())
		{
			AssetNode childNode = (AssetNode) enChildNode.nextElement();
			if (childNode.isType(NodeType.THREAD))
			{
				ThreadDialog.removeFromThread(childNode.getName());
			}
			this.remove(childNode);
			enChildNode = this.children();
		}
	}
	public void setSelectedChildren(boolean isSelected) 
	{
		setSelectedChildren(this,isSelected);
	}

	private void setSelectedChildren(AssetNode node, boolean isSelected)
	{
		Enumeration<Object> enChildNode = node.getAllChildren();
		while (enChildNode.hasMoreElements())
		{
			AssetNode childNode = (AssetNode) enChildNode.nextElement();
			childNode.setSelected(isSelected);
		}
	}

	public void setMandatory(boolean isMandatory, boolean isIncludeChildrens)
	{
		this.isMandatory = isMandatory;
		if (isIncludeChildrens)
		{
			Enumeration<Object> enChildNode = getAllChildren();
			while (enChildNode.hasMoreElements())
			{
				AssetNode childNode = (AssetNode) enChildNode.nextElement();
				childNode.isMandatory = isMandatory;
			}
		}
	}
	// Get all the children
		private void get(AssetNode node, Vector <Object> v) {

			if (node.children() == null) {
				return;
			}
			// copy the vector
			for (Enumeration <Object> e = node.children(); e.hasMoreElements();) {
				v.addElement(e.nextElement());
			}

			for (Enumeration <Object> e = node.children(); e.hasMoreElements();) {
				AssetNode child = (AssetNode) e.nextElement();
				get(child, v);
			}
		}
	public boolean isSelected() {
		return isSelected;
	}

	public void setSelected(boolean status) {
		isSelected = status;
	}
	public boolean isBselected() {
		return isBselected;
	}

	public void setBselected(boolean isBselected) {
		this.isBselected = isBselected;
	}

	public void toggleSelection() {
		setSelected(!isSelected);
	}

	public NodeType getType() {
		return type;
	}

	public void setType(NodeType nodeType) {
		this.type = nodeType;
	}

	public String getName() {
		return name;
	}

	public void setName(String nodeName) {
		this.name = nodeName;
		this.setUserObject(nodeName);
	}
	public boolean isType (NodeType expectedType)
	{
		if (getType().equals(expectedType))
			return true;
		else
			return false;
	}
	public boolean isParentType (NodeType expectedType)
	{
		if (((AssetNode)getParent()).getType().equals(expectedType))
			return true;
		else
			return false;
	}
		public boolean isMandatory()
		{
			return isMandatory;
		}
		public int getStatus()
		{
			return status;
		}
		public void setStatus(int status)
		{
			this.status = status;
		}
		public boolean isStatus(int status)
		{
			if (this.status==status)
			return true;
			else
				return false;
		}
		public String getId()
		{
			return id;
		}
		public void setId(String id)
		{
			this.id = id;
		}
}
