/*
 * Copyright 2005-2010 Ignis Software Tools Ltd. All rights reserved.
 */
package com.mobixell.xtt.gui.actionItems;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import com.mobixell.xtt.gui.main.XTTGui;
import com.mobixell.xtt.gui.mapping.XttMapping;
import com.mobixell.xtt.gui.testlaunch.TestLauncherGui;
import com.mobixell.xtt.gui.testlaunch.panels.FileTreePanel;
import com.mobixell.xtt.images.ImageCenter;

public class CopyFolderAction extends IgnisAction {
	
	private static final long serialVersionUID = 1L;
	
	private static CopyFolderAction action;
	JPanel parent = null;
	
	private CopyFolderAction(){
		super();
		putValue(Action.NAME, XttMapping.getInstance().getCopyFolderItem());
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getCopyFolderButton());
		putValue(Action.SMALL_ICON, ImageCenter.getInstance().getImage(ImageCenter.ICON_COPY_FOLDER));
		putValue(Action.LARGE_ICON_KEY, ImageCenter.getInstance().getImage(ImageCenter.ICON_COPY_FOLDER));
		putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
		putValue(Action.ACTION_COMMAND_KEY, "copy-Folder");
	}
	
	public static CopyFolderAction getInstance(JPanel parent){
		if (action == null){
			action =  new CopyFolderAction();
		}
		action.parent=parent;
		return action;
	}
	public void actionPerformed(ActionEvent e) 
	{
		if (parent.getClass().equals(FileTreePanel.class))
		TestLauncherGui.getFileTreePanel().copySourceFolder();
		else
		XTTGui.getRunnerTreePanel().copySourceFolder();
	}
}
