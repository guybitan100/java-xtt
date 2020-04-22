/*
 * Copyright 2005-2010 Ignis Software Tools Ltd. All rights reserved.
 */
package com.mobixell.xtt.gui.actionItems;

import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.JPanel;
import com.mobixell.xtt.gui.main.XTTGui;
import com.mobixell.xtt.gui.mapping.XttMapping;
import com.mobixell.xtt.gui.testlaunch.TestLauncherGui;
import com.mobixell.xtt.gui.testlaunch.panels.FileTreePanel;

public class RenameFileAction extends IgnisAction {
	
	private static final long serialVersionUID = 1L;
	
	private static RenameFileAction action;
	JPanel parent = null;
	private RenameFileAction(){
		super();
		putValue(Action.NAME, XttMapping.getInstance().getRenameFileItem());
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getRenameFileButton());
		putValue(Action.ACTION_COMMAND_KEY, "rename-File");
	}
	
	public static RenameFileAction getInstance(JPanel parent){
		if (action == null){
			action =  new RenameFileAction();
		}
		action.parent=parent;
		return action;
	}
	public void actionPerformed(ActionEvent e) 
	{
		if (parent.getClass().equals(FileTreePanel.class))
		TestLauncherGui.getFileTreePanel().renameFile(true);
		else
		XTTGui.getRunnerTreePanel().renameFile(true);
	}
}
