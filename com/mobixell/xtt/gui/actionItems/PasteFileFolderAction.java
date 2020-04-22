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

public class PasteFileFolderAction extends IgnisAction {
	
	private static final long serialVersionUID = 1L;
	
	private static PasteFileFolderAction action;
	JPanel parent = null;
	
	private PasteFileFolderAction(){
		super();
		putValue(Action.NAME, XttMapping.getInstance().getPasteFileItem());
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getPasteFileButton());
		putValue(Action.SMALL_ICON, ImageCenter.getInstance().getImage(ImageCenter.ICON_PASTE));
		putValue(Action.LARGE_ICON_KEY, ImageCenter.getInstance().getImage(ImageCenter.ICON_PASTE));
		putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK));
		putValue(Action.ACTION_COMMAND_KEY, "paste");
	}
	
	public static PasteFileFolderAction getInstance(JPanel parent){
		if (action == null){
			action =  new PasteFileFolderAction();
		}
		action.parent=parent;
		return action;
	}
	public void actionPerformed(ActionEvent e) 
	{
		if (parent.getClass().equals(FileTreePanel.class))
		TestLauncherGui.getFileTreePanel().pasteFileFolder();
		else
		XTTGui.getRunnerTreePanel().pasteFileFolder();
	}
}
