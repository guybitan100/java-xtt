/*
 */
package com.mobixell.xtt.gui.actionItems;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JPanel;

import com.mobixell.xtt.gui.main.TestList;
import com.mobixell.xtt.gui.mapping.XttMapping;
import com.mobixell.xtt.gui.testlaunch.panels.TreePanel;
import com.mobixell.xtt.gui.testlaunch.tree.TreeTestController;
import com.mobixell.xtt.gui.testlaunch.tree.TreeTestController.ActionType;
import com.mobixell.xtt.images.ImageCenter;


public class MoveUpAction extends IgnisAction {

	private static final long serialVersionUID = 1L;
	private JPanel parent = null;
	private static MoveUpAction action;
	
	private MoveUpAction(){
		super();
		putValue(Action.NAME, XttMapping.getInstance().getItemMoveUpMenuItem());
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getItemMoveUpButton());
		putValue(Action.SMALL_ICON, ImageCenter.getInstance().getImage(ImageCenter.ICON_UP));
		putValue(Action.LARGE_ICON_KEY, ImageCenter.getInstance().getImage(ImageCenter.ICON_UP));
		putValue(Action.ACTION_COMMAND_KEY, "move-test-up");
	}
	
	public static MoveUpAction getInstance(JPanel parent){
		if (action == null){
			action =  new MoveUpAction();
		}
		action.parent=parent;
		return action;
	}
	public void actionPerformed(ActionEvent e) 
	{
		if (parent.getClass().equals(TreePanel.class))
		{
		((TreePanel)(parent)).treeTestController.handleTreeActions(ActionType.UP);
		TreeTestController.setTestDirty(true);
		}
		else if (parent.getClass().equals(TestList.class))
		{
			((TestList)parent).moveUp();	
		}
	}
}
