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

public class PasteLoopAction extends IgnisAction {
	
	private static final long serialVersionUID = 1L;
	private JPanel parent = null;
	private static PasteLoopAction action;
	
	private PasteLoopAction(){
		super();
		putValue(Action.NAME, XttMapping.getInstance().getPasteLoopItem());
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getPasteLoopButton());
		putValue(Action.SMALL_ICON, ImageCenter.getInstance().getImage(ImageCenter.ICON_SCENARIO_NEW));
		putValue(Action.LARGE_ICON_KEY, ImageCenter.getInstance().getImage(ImageCenter.ICON_SCENARIO_NEW));
		putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK));
		putValue(Action.ACTION_COMMAND_KEY, "paste-loop");
	}
	
	public static PasteLoopAction getInstance(JPanel parent){
		if (action == null){
			action =  new PasteLoopAction();
		}
		action.parent=parent;
		return action;
	}
	
	public void actionPerformed(ActionEvent e) {
		((TreePanel)(parent)).treeTestController.handleTreeActions(ActionType.PASTE_LOOP_FROM_CLIPBOARD);
	}
}
