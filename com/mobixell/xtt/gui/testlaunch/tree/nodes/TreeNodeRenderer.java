/*
 * Copyright 2005-2010 Ignis Software Tools Ltd. All rights reserved.
 */
package com.mobixell.xtt.gui.testlaunch.tree.nodes;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.gui.testlaunch.tree.nodes.AssetNode.NodeType;
import com.mobixell.xtt.images.ImageCenter;

/**
 * NodeRenderer class This class implements the look of the node in the tree
 */
public class TreeNodeRenderer implements TreeCellRenderer{ 

	protected boolean bSelected = false;
	protected boolean bFocus = false;
	public AssetNode currentNode;
	public final Color THREAD_COLOR = new Color(55,155,218);
	public final Color TEST_PASS_COLOR = new Color(0,164,0);
	public final Color TEST_FAILED_COLOR = new Color(255,62,62);
	public final Color SUBTEST_COLOR = new Color(0, 124, 223);
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean isSelected, boolean expanded,
			boolean leaf, int row, boolean hasFocus) {
		 
		TreeJPanel panel = new TreeJPanel();
		bSelected = isSelected;
		String text ="";
		bFocus = hasFocus;
		panel.setEnabled(tree.isEnabled());
		panel.check.setSelected(((AssetNode) value).isSelected());
		panel.setBackground(Color.white);
		panel.check.setBackground(Color.white);
        currentNode = (AssetNode) value;
        String stringValue = tree.convertValueToText(value, isSelected, expanded, leaf, row, hasFocus);
        panel.label.setText(stringValue);
        panel.label.setFont(tree.getFont()); 
			if (leaf) 
			{
				panel.label.setIcon(UIManager.getIcon("Tree.closedIcon"));
			} 
			else if (expanded) 
			{
				panel.label.setIcon(UIManager.getIcon("Tree.openIcon"));
			} 
			else 
			{
				panel.label.setIcon(UIManager.getIcon("Tree.closedIcon"));
			}
			switch(currentNode.getType()) 
			{
			
					case ROOT:
						switch(currentNode.getStatus()) 
						{
							case XTTProperties.RUNNING:
								panel.label.setIcon(ImageCenter.getInstance().getImage(ImageCenter.ICON_TEST));
								break;
							case XTTProperties.NOT_RUNNING:
								panel.label.setIcon(ImageCenter.getInstance().getImage(ImageCenter.ICON_TEST));
								break;
							case XTTProperties.PASSED:
								panel.label.setForeground(TEST_PASS_COLOR);
								panel.label.setIcon(ImageCenter.getInstance().getImage(ImageCenter.ICON_STEP_OK));
								break;
							case XTTProperties.FAILED:
								panel.label.setForeground(TEST_FAILED_COLOR);
							    XTTProperties.getXTTGui().updateIcon(false);
								panel.label.setIcon(ImageCenter.getInstance().getImage(ImageCenter.ICON_STEP_FAILER));
								break;
						}
						 if (currentNode.getStatus() > XTTProperties.RUNNING)
						 {
							 panel.label.setForeground(TEST_FAILED_COLOR);
							 panel.label.setIcon(ImageCenter.getInstance().getImage(ImageCenter.ICON_STEP_EXCEPTION));
						 }
					break;
					case THREAD:
					case LOOP:
						switch(currentNode.getStatus()) 
						{
							case XTTProperties.RUNNING:
								tree.scrollPathToVisible(new TreePath(currentNode.getPath()));
								tree.setSelectionPath(new TreePath(currentNode.getPath()));
								panel.label.setIcon(ImageCenter.getInstance().getImage(ImageCenter.ICON_STEP_RUN));
								break;
								
							case XTTProperties.NOT_RUNNING:
								if (currentNode.isType(NodeType.THREAD))
								panel.label.setIcon(ImageCenter.getInstance().getImage(ImageCenter.ICON_THREAD));
								if (currentNode.isType(NodeType.LOOP))
									panel.label.setIcon(ImageCenter.getInstance().getImage(ImageCenter.ICON_FOR_LOOP));
								break;
								
							case XTTProperties.PASSED:
								panel.label.setIcon(ImageCenter.getInstance().getImage(ImageCenter.ICON_STEP_OK));
								break;	
								
							case XTTProperties.FAILED:
								 panel.label.setForeground(TEST_FAILED_COLOR);
								 panel.label.setIcon(ImageCenter.getInstance().getImage(ImageCenter.ICON_STEP_FAILER));
								break;
						}
						if (currentNode.getStatus()>XTTProperties.RUNNING)
						{
							panel.label.setIcon(ImageCenter.getInstance().getImage(ImageCenter.ICON_STEP_EXCEPTION));
						}
							text =  panel.label.getText() + " Steps: " + currentNode.getChildCount();
							currentNode.setId(getNodeIndex(tree, currentNode));
							
							if (currentNode.isType(NodeType.THREAD))
								panel.label.setText("("+ currentNode.getId() + ") "+ text);
							if (currentNode.isType(NodeType.LOOP))
								panel.label.setText("("+ currentNode.getId() + ") "+ text + " Start: " + ((LoopNode)currentNode).getStart() + ", Stop: " + ((LoopNode)currentNode).getStop() + ", Step: " + ((LoopNode)currentNode).getStep());
							
							panel.label.setFont(new Font(Font.SERIF, Font.BOLD, 16));
							panel.label.setForeground(THREAD_COLOR);
							bSelected = isSelected;
							currentNode.setBselected(isSelected);
						break;
					case STEP:
						switch(currentNode.getStatus()) 
						{
							case XTTProperties.RUNNING:
								panel.label.setIcon(ImageCenter.getInstance().getImage(ImageCenter.ICON_STEP_RUN));
								tree.setSelectionPath(new TreePath(currentNode.getPath()));
								tree.scrollPathToVisible(new TreePath(currentNode.getPath()));
								break;
							case XTTProperties.NOT_RUNNING:
									if (((AssetNode) currentNode.getParent()).getType().equals(NodeType.THREAD))
									{
										panel.label.setIcon(ImageCenter.getInstance().getImage(ImageCenter.ICON_THREAD));
									}
									else if (((AssetNode) currentNode.getParent()).getType().equals(NodeType.LOOP))
									{
										panel.label.setIcon(ImageCenter.getInstance().getImage(ImageCenter.ICON_FOR_LOOP));
									}
									else
									{
										panel.label.setForeground(new Color(0, 0, 64));
										if (currentNode.isMandatory())
											panel.label.setIcon(ImageCenter.getInstance().getImage(ImageCenter.ICON_STEP_MANDATORY_NOT_RUNNING));
										else
										panel.label.setIcon(ImageCenter.getInstance().getImage(ImageCenter.ICON_STEP_NOT_RUNNING));
									}
								break;
								
							case XTTProperties.PASSED:
								panel.label.setIcon(ImageCenter.getInstance().getImage(ImageCenter.ICON_STEP_OK));
								break;	
							case XTTProperties.FAILED:
								 panel.label.setForeground(TEST_FAILED_COLOR);
								 panel.label.setIcon(ImageCenter.getInstance().getImage(ImageCenter.ICON_STEP_FAILER));
								break;
						}
						if (currentNode.getStatus()>XTTProperties.RUNNING)
						{
							panel.label.setIcon(ImageCenter.getInstance().getImage(ImageCenter.ICON_STEP_EXCEPTION));
						}
							text = panel.label.getText();
							currentNode.setId(getNodeIndex(tree, currentNode));
								panel.label.setText("(" +currentNode.getId() + ") " + text);
							panel.label.setFont(new Font(Font.SERIF, Font.BOLD, 16));
							bSelected = isSelected;
							currentNode.setBselected(isSelected);
						break;
					case MOD_FUN:
						panel.label.setFont(new Font(Font.SERIF, Font.PLAIN, 14));
							if (((StepNode)currentNode.getParent()).isSelected)
								panel.label.setIcon(ImageCenter.getInstance().getImage(ImageCenter.ICON_GREEN));
							else 
								panel.label.setIcon(ImageCenter.getInstance().getImage(ImageCenter.ICON_RED));
						break;
					case PARMS:
						bSelected = isSelected;
						panel.label.setIcon(ImageCenter.getInstance().getImage(ImageCenter.ICON_CHANGE_SUT));
						panel.label.setFont(new Font(Font.SERIF, Font.PLAIN, 14));
						if (!currentNode.isSelected) panel.label.setForeground(Color.GRAY);
						break;
					case PARAMGROUP:
						panel.label.setIcon(ImageCenter.getInstance().getImage(ImageCenter.ICON_TEST_PASS));
						if (!currentNode.isSelected) panel.label.setForeground(Color.GRAY);
						break;
					case SUBTEST:
						panel.label.setFont(new Font(Font.SERIF,Font.PLAIN, 14));
						panel.label.setForeground(SUBTEST_COLOR);
						if (((StepNode)currentNode.getParent()).isSelected)
							panel.label.setIcon(ImageCenter.getInstance().getImage(ImageCenter.ICON_GREEN));
						else
							panel.label.setIcon(ImageCenter.getInstance().getImage(ImageCenter.ICON_RED));	
						break;
					case MODULE:
						panel.label.setFont(new Font(Font.SERIF,0, 14));
						panel.label.setIcon(ImageCenter.getInstance().getImage(ImageCenter.ICON_PATH));
						break;
					case FUNCTION:
						panel.label.setIcon(ImageCenter.getInstance().getImage(ImageCenter.ICON_DIR));
						panel.label.setFont(new Font(Font.SERIF,0, 14));
						break;
					case FOLDER:
						panel.label.setFont(new Font(Font.SERIF,0, 14));
						break;
					case FILE:
						panel.label.setFont(new Font(Font.SERIF, 0, 14));
						if (((FileNode)currentNode).getFileType().equalsIgnoreCase("xml"))
							panel.label.setIcon(ImageCenter.getInstance().getImage(ImageCenter.ICON_SUT));
						else
							panel.label.setIcon(ImageCenter.getInstance().getImage(ImageCenter.ICON_SUT_EDIT));	
						break;
					}
		return panel;
	}
	/* This class will hold the text and tree table */
	class TreeLabel extends JLabel {

		private static final long serialVersionUID = 1L;
		Color color = null;
		public void setColor(Color color) 
		{
			this.color = color;
			setForeground(color);
		}
		@Override
		public void paint(Graphics g) 
		{
			if ((getText()) != null) 
			{
				Icon currentI = getIcon();
				if (bSelected) {
					setForeground(Color.white);
					Color bColor = Color.black;
					if (!bFocus) 
					{
						bColor = bSelected ? Color.lightGray : Color.white;
					}
					g.setColor(bColor);
					Dimension d = getPreferredSize();
					int imageOffset = 0;
					currentI = getIcon();
					if (currentI != null) 
					{
						imageOffset = currentI.getIconWidth() + Math.max(0, getIconTextGap() - 1);
					}
						g.fillRect(imageOffset, 0, d.width - imageOffset, d.height);
				}
			}

			super.paint(g);
		}
	}

	class TreeJPanel extends JPanel {

		private static final long serialVersionUID = 1L;
		
		public JCheckBox check;
		public TreeLabel label;
		boolean isLeaf;
		public TreeJPanel() 
		{
			check = new JCheckBox();
			label = new TreeLabel();
	        setBackground(Color.white);
			label.setBackground(Color.white);
            check.setBackground(Color.white);
			label.setOpaque(false);
			check.setOpaque(false);
			add(label);
			add(check);
		}

		public Dimension getPreferredSize() {
				Dimension d_check = check.getPreferredSize();
				Dimension d_label = label.getPreferredSize();
				if (currentNode!=null && currentNode.getType().equals(NodeType.STEP) || currentNode.getType().equals(NodeType.THREAD)) 
				{
				return new Dimension(d_check.width + d_label.width, (d_check.height < d_label.height ? d_label.height
						: d_check.height));
				}
				else
				{
					remove(check);
					return new Dimension(d_label.width, d_label.height);
				}
		}

		public void doLayout() {
				Dimension d_check = check.getPreferredSize();
				Dimension d_label = label.getPreferredSize();

				int y_check = 0;
				int y_label = 0;
				if (currentNode!=null && currentNode.getType().equals(NodeType.STEP)|| currentNode.getType().equals(NodeType.THREAD))
				{
					check.setLocation(0, y_check);
					check.setBounds(0, y_check, d_check.width+4, d_check.height);
					label.setLocation(d_check.width, y_label);
					label.setBounds(d_check.width, y_label, d_label.width, d_label.height-4);
				}
				else
				{
					remove(check);
					label.setLocation(0, y_label);
					label.setBounds(0, y_label, d_label.width+3, d_label.height-4);
				}
				
			}
		}
	public String getNodeIndex(JTree tree, TreeNode node) { 
	    TreeNode root = (TreeNode) tree.getModel().getRoot(); 
	    if (node == root) { 
	        return ""; 
	    } 
	    TreeNode parent = node.getParent(); 
	    if (parent == null) { 
	        return null; 
	    } 
	    String parentIndex= getNodeIndex(tree, parent); 
	    if (parentIndex == null) { 
	        return null; 
	    } 
	    if (parentIndex.equals(""))
	    	return ""+(parent.getIndex(node)+1);	
	    else
	    	return parentIndex + "." + (parent.getIndex(node)+1);
	} 

}
