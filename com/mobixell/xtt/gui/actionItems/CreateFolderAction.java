/*
 * Copyright 2005-2010 Ignis Software Tools Ltd. All rights reserved.
 */
package com.mobixell.xtt.gui.actionItems;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.UIManager;
import com.mobixell.xtt.gui.main.XTTGui;
import com.mobixell.xtt.gui.mapping.XttMapping;
import com.mobixell.xtt.gui.testlaunch.TestLauncherGui;
import com.mobixell.xtt.gui.testlaunch.panels.FileTreePanel;

public class CreateFolderAction extends IgnisAction {
	
	private static final long serialVersionUID = 1L;
	private static CreateFolderAction action;
	JPanel parent = null;
	private CreateFolderAction(){
		super();
		putValue(Action.NAME, XttMapping.getInstance().getCreateFolderButton());
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getCreateFolderMenuItem());
		putValue(Action.SMALL_ICON, UIManager.getIcon("Tree.closedIcon"));
		putValue(Action.LARGE_ICON_KEY, UIManager.getIcon("Tree.closedIcon"));
		putValue(Action.ACTION_COMMAND_KEY, "create-folder");
	}
	
	public static CreateFolderAction getInstance(JPanel parent){
		if (action == null){
			action =  new CreateFolderAction();
		}
		action.parent=parent;
		return action;
	}
	
	public void actionPerformed(ActionEvent e) 
	{
		if (parent.getClass().equals(FileTreePanel.class))
		TestLauncherGui.getFileTreePanel().createFolder();
		else
		XTTGui.getRunnerTreePanel().createFolder();
	}
}
