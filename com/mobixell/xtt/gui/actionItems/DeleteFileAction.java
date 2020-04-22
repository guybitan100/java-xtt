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

public class DeleteFileAction extends IgnisAction {
	
	private static final long serialVersionUID = 1L;
	private static DeleteFileAction action;
	JPanel parent = null;
	private DeleteFileAction(){
		super();
		putValue(Action.NAME, XttMapping.getInstance().getDeleteFileButton());
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getDeleteFileMenuItem());
		putValue(Action.SMALL_ICON, ImageCenter.getInstance().getImage(ImageCenter.ICON_DELETE_ALL));
		putValue(Action.LARGE_ICON_KEY, ImageCenter.getInstance().getImage(ImageCenter.ICON_DELETE_ALL));
		putValue(Action.ACTION_COMMAND_KEY, "delete-file");
	}
	
	public static DeleteFileAction getInstance(JPanel parent){
		if (action == null){
			action =  new DeleteFileAction();
		}
		action.parent=parent;
		return action;
	}
	
	public void actionPerformed(ActionEvent e) 
	{
		if ( parent==null || parent.getClass().equals(FileTreePanel.class))
		TestLauncherGui.getFileTreePanel().deleteFileOrFolder();
		else
		XTTGui.getRunnerTreePanel().deleteFileOrFolder();
	}
}
