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

public class CopyFileAction extends IgnisAction {
	
	private static final long serialVersionUID = 1L;
	
	private static CopyFileAction action;
	JPanel parent = null;
	private CopyFileAction(){
		super();
		putValue(Action.NAME, XttMapping.getInstance().getCopyFileItem());
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getCopyFileButton());
		putValue(Action.SMALL_ICON, ImageCenter.getInstance().getImage(ImageCenter.ICON_COPY_TESTS));
		putValue(Action.LARGE_ICON_KEY, ImageCenter.getInstance().getImage(ImageCenter.ICON_COPY_TESTS));
		putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
		putValue(Action.ACTION_COMMAND_KEY, "copy-File");
	}
	
	public static CopyFileAction getInstance(JPanel parent){
		if (action == null){
			action =  new CopyFileAction();
		}
		action.parent=parent;
		return action;
	}
	
	public void actionPerformed(ActionEvent e) 
	{
		if (parent.getClass().equals(FileTreePanel.class))
		TestLauncherGui.getFileTreePanel().copySourceFile();
		else
		XTTGui.getRunnerTreePanel().copySourceFile();
	}
}
