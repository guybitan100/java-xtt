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
import com.mobixell.xtt.images.ImageCenter;

public class NewFolderAction extends IgnisAction {
	
	private static final long serialVersionUID = 1L;
	private static NewFolderAction action;
	JPanel parent = null;
	
	private NewFolderAction(){
		super();
		putValue(Action.NAME, XttMapping.getInstance().getCreateFolderButton());
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getCreateFolderMenuItem());
		putValue(Action.SMALL_ICON, ImageCenter.getInstance().getImage(ImageCenter.ICON_DIR));
		putValue(Action.LARGE_ICON_KEY, ImageCenter.getInstance().getImage(ImageCenter.ICON_DIR));
		putValue(Action.ACTION_COMMAND_KEY, "create-folder");
	}
	
	public static NewFolderAction getInstance(JPanel parent){
		if (action == null){
			action =  new NewFolderAction();
		}
		action.parent=parent;
		return action;
	}
	
	public void actionPerformed(ActionEvent e) 
	{
		if (parent==null || parent.getClass().equals(FileTreePanel.class))
		TestLauncherGui.getFileTreePanel().createFolder();
		else
		XTTGui.getRunnerTreePanel().createFolder();
	}
}
