package com.mobixell.xtt.gui.testlaunch.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.mobixell.xtt.ModuleList;
import com.mobixell.xtt.gui.testlaunch.SwingUtils;
import com.mobixell.xtt.gui.testlaunch.TestLauncherGui;
import com.mobixell.xtt.gui.testlaunch.tree.nodes.AssetNode;
import com.mobixell.xtt.gui.testlaunch.tree.nodes.FunctionNode;
import com.mobixell.xtt.gui.testlaunch.tree.nodes.ModuleFuncNode;
import com.mobixell.xtt.gui.testlaunch.tree.nodes.ModuleNode;
import com.mobixell.xtt.gui.testlaunch.tree.nodes.TreeNodeRenderer;
import com.mobixell.xtt.gui.testlaunch.tree.nodes.AssetNode.NodeType;
import com.mobixell.xtt.images.ImageCenter;

/**
 * Display a file system in a JTree view
 * 
 * @version $Id: FileTree.java,v 1.9 2004/02/23 03:39:22 ian Exp $
 * @author Ian Darwin
 */
public class StepsTreePanel extends JPanel implements MouseListener, TreeSelectionListener, ActionListener
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static JTree tree;
	public ModuleList moduleList;
	private static DefaultTreeModel treeModel;
	private static ModuleNode nModuleRootNode;
	private JTextArea textFuncDesc = new JTextArea();

	/**
	 * Construct a FileTree
	 * 
	 * @throws Exception
	 * 
	 */
	public StepsTreePanel(ModuleList modulesList) throws Exception
	{
		this.moduleList = modulesList;
		setLayout(new BorderLayout());
		nModuleRootNode = new ModuleNode("Modules");
		treeModel = new DefaultTreeModel(nModuleRootNode);
		tree = new JTree(treeModel);
		tree.addMouseListener(this);
		tree.addTreeSelectionListener(this);
		tree.setCellRenderer(new TreeNodeRenderer());
		tree.addTreeSelectionListener(this);

		textFuncDesc.setBackground(Color.lightGray);
		textFuncDesc.setFont(new Font(Font.SERIF, Font.PLAIN, 16));
		textFuncDesc.setEditable(false);
		textFuncDesc.setRows(6);
		textFuncDesc.setColumns(20);
		textFuncDesc.setLineWrap(true);
		textFuncDesc.setBorder(BorderFactory.createBevelBorder(1));
		textFuncDesc.setBackground(Color.white);

		JScrollPane scrollDesc = new JScrollPane();

		scrollDesc = SwingUtils.getJScrollPaneWithWaterMark(
				ImageCenter.getInstance().getAwtImage(ImageCenter.ICON_SCENARIO_TREE_BG), textFuncDesc);

		addNodes();
		// Make a tree list with all the nodes, and make it a JTree
		JScrollPane treeView = SwingUtils.getJScrollPaneWithWaterMark(
				ImageCenter.getInstance().getAwtImage(ImageCenter.ICON_TEST_TREE_BG), tree);

		// Lastly, put the JTree into a JScrollPane.
		JScrollPane scrollpane = new JScrollPane();
		scrollpane.getViewport().add(treeView);

		add(scrollDesc, BorderLayout.NORTH);
		add(scrollpane,BorderLayout.CENTER);
	}

	void addNodes() throws Exception
	{
		ModuleNode moduleNode;
		for (int ind = 0; ind < moduleList.getModules().size(); ind++)
		{
			moduleNode = new ModuleNode(moduleList.getModules().get(ind).toString());
			treeModel.insertNodeInto(moduleNode, nModuleRootNode, ind);
			Vector<String> functions = moduleList.getModuleFunctions(moduleList.getModules().get(ind).toString());
			Collections.sort(functions, String.CASE_INSENSITIVE_ORDER);
			for (int j = 0; j < functions.size(); j++)
			{
				treeModel.insertNodeInto(new FunctionNode(functions.get(j)), moduleNode, j);
			}
		}
		((DefaultTreeModel) tree.getModel()).reload();
		tree.setSelectionPath(new TreePath(nModuleRootNode.getPath()));
	}

	private void performAction(MouseEvent me) throws Exception
	{
		int x = me.getX();
		int y = me.getY();
		// save the path for future use
		TreePath clickedPath = tree.getPathForLocation(x, y);
		if (clickedPath == null) return;

		// save the selected node
		AssetNode currentNode = (AssetNode) clickedPath.getLastPathComponent();
		if (me.getButton() == MouseEvent.BUTTON1)
		{
			if (currentNode.getType().equals(NodeType.FUNCTION))
			{
				if (me.getClickCount() == 2)
				{
					TestLauncherGui.parameterDialog.init(true, ((ModuleNode) currentNode.getParent()).getName(),
							currentNode.getName());
				}
				else
				{
					Vector<?> v = (Vector<?>) moduleList.getModuleFunctionsDescriptions(
							((ModuleNode) currentNode.getParent()).getName(), currentNode.getName());
					if (v != null)
					{
						StringBuffer t = new StringBuffer();
						for (int i = 0; i < v.size(); i++)
						{
							t.append((String) v.get(i) + "\n");
						}
						textFuncDesc.setText(t.toString());
					}
					else
					{
						textFuncDesc.setText("");
					}
				}
			}
		}
	}
	public static void showModuleFunction(AssetNode node)
	{
		if (node!=null)
		{
			ModuleNode folderNode = new ModuleNode("Modules");
			ModuleNode moduleNode = new ModuleNode(((ModuleFuncNode)node).getModule()); 
			FunctionNode funcNode = new FunctionNode(((ModuleFuncNode)node).getFunction());
			TreePath tp = new TreePath(folderNode).pathByAddingChild(moduleNode).pathByAddingChild(funcNode);
			tree.expandPath(tp);
		}
	}
	public void mousePressed(MouseEvent e)
	{
		try
		{
			performAction(e);
		}
		catch (Exception e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	public void expandAll(TreePath parent, boolean expand) throws Exception
	{
		// Traverse children
		AssetNode node = (AssetNode) parent.getLastPathComponent();
		if (node.getChildCount() >= 0)
		{
			for (Enumeration<?> e = node.children(); e.hasMoreElements();)
			{
				AssetNode n = (AssetNode) e.nextElement();
				TreePath path = parent.pathByAddingChild(n);
				expandAll(path, expand);
			}
		}
		// Expansion or collapse must be done bottom-up
		if (expand)
		{
			tree.expandPath(parent);
		}
		else
		{
			tree.collapsePath(parent);
		}
	}
	public void valueChanged(TreeSelectionEvent e)
	{
		// save the selected node
		if (((AssetNode) e.getPath().getLastPathComponent()).getType().equals(NodeType.FUNCTION))
		{
			FunctionNode currentNode = (FunctionNode) e.getPath().getLastPathComponent();
			Vector<?> v = (Vector<?>) moduleList.getModuleFunctionsDescriptions(((ModuleNode) currentNode.getParent()).getName(), currentNode.getName());
			if (v != null)
			{
				StringBuffer t = new StringBuffer();
				for (int i = 0; i < v.size(); i++)
				{
					t.append((String) v.get(i) + "\n");
				}
				textFuncDesc.setText(t.toString());
			}
			else
			{
				textFuncDesc.setText("");
			}
		}
	}
	public void actionPerformed(ActionEvent e){}
	public void mouseClicked(MouseEvent e){}
	public void mouseReleased(MouseEvent e){}
	public void mouseEntered(MouseEvent e){}
	public void mouseExited(MouseEvent e){}
}
