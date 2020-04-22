package com.mobixell.xtt.gui.actionItems;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import com.mobixell.xtt.gui.mapping.XttMapping;
import com.mobixell.xtt.gui.testlaunch.tree.TreeTestController;

public class ExitAction extends IgnisAction {
	
	private static final long serialVersionUID = 1L;
	
	private static ExitAction action;
	


	private ExitAction(){
		super();
		
		putValue(Action.NAME, "Exit");
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getEditParamButton());
		putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.ALT_MASK));
		putValue(Action.ACTION_COMMAND_KEY, "exit");
	}
	
	public static ExitAction getInstance(){
		if (action == null){
			action =  new ExitAction();
		}
		return action;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
			exit();
	}

	public void exit()
	{
			try
			{
				if (TreeTestController.test.isDirty())
				{
					int ans = JOptionPane.showConfirmDialog(TreeTestController.getTestLauncherGui(),
							"Do you want to save the changes you made \nin " + TreeTestController.getTestFileName() + " ?", "Save Confirmation",
							JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
					if (ans == JOptionPane.YES_OPTION)
					{
						TreeTestController.test.save();
					}
					else if (ans == JOptionPane.NO_OPTION)
						TreeTestController.test.setDirty(false);
					else
					{
						return;
					}
				}
			}
			catch (Exception e1)
			{
				TreeTestController.getTestLauncherGui().exit(-1);
			}
			TreeTestController.getTestLauncherGui().exit(0);
	}
}
