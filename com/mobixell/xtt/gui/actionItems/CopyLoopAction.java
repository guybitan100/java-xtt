/*
 * Copyright 2005-2010 Ignis Software Tools Ltd. All rights reserved.
 */
package com.mobixell.xtt.gui.actionItems;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import com.mobixell.xtt.gui.mapping.XttMapping;
import com.mobixell.xtt.gui.testlaunch.panels.TreePanel;
import com.mobixell.xtt.gui.testlaunch.tree.TreeTestController.ActionType;
import com.mobixell.xtt.images.ImageCenter;

public class CopyLoopAction extends IgnisAction {
	
	private static final long serialVersionUID = 1L;
	private JPanel parent = null;
	private static CopyLoopAction action;
	
	private CopyLoopAction(){
		super();
		putValue(Action.NAME, XttMapping.getInstance().getCopyLoopItem());
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getCopyLoopButton());
		putValue(Action.SMALL_ICON, ImageCenter.getInstance().getImage(ImageCenter.ICON_COPY_TESTS));
		putValue(Action.LARGE_ICON_KEY, ImageCenter.getInstance().getImage(ImageCenter.ICON_COPY_TESTS));
		putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
		putValue(Action.ACTION_COMMAND_KEY, "copy-loop");
	}
	
	public static CopyLoopAction getInstance(JPanel parent){
		if (action == null){
			action =  new CopyLoopAction();
		}
		action.parent=parent;
		return action;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		((TreePanel)(parent)).treeTestController.handleTreeActions(ActionType.COPY_LOOP_TO_CLIPBOARD);
	}
}
