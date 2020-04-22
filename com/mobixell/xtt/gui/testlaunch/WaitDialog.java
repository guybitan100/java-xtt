/*
 * Copyright 2005-2010 Ignis Software Tools Ltd. All rights reserved.
 */
package com.mobixell.xtt.gui.testlaunch;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.gui.testlaunch.tree.TreeTestController;
/**
 * WaitDialog This is just a frame with a JLabel in it and no buttons used for
 * displaying "Please wait..." dialogs and the like
 * 
 * @author Adam Olsen
 * @version 1.0
 * 
 */

public class WaitDialog extends JDialog {
	private static final long serialVersionUID = -2017901707408101146L;
	private Container parent;

	private String title;
	
	private String progressMessage;
	private static Object staticLock = new Object(); 
	private JProgressBar bar ;

	/**
	 * Default constructor
	 * 
	 * @param title
	 *            the message to be displayed in the window's titlebar
	 * 
	 */
	public WaitDialog(Frame parent, WaitDialogListener listener, String title) {
		super(parent, title);
		this.parent = parent;
		this.title = title;
		/*
		 * Set the dialog to be modal
		 */
		setModalityType(ModalityType.DOCUMENT_MODAL);
		setResizable(false);
		initComponents();
	}
	public static boolean isRunning() 
	{
		return dialog.isVisible();
	}
	

	public WaitDialog(Dialog parent, WaitDialogListener listener, String title) {
		super(parent, title);
		this.parent = parent;
		this.title = title;
		/*
		 * Set the dialog to be modal
		 */
		setModalityType(ModalityType.DOCUMENT_MODAL);
		initComponents();
	}
	
	public WaitDialog(Frame parent, WaitDialogListener listener, String title,String progressMessage) {
		super(parent, title);
		this.parent = parent;
		this.title = title;
		this.progressMessage = progressMessage;
		/*
		 * Set the dialog to be modal
		 */
		setModalityType(ModalityType.DOCUMENT_MODAL);
		setResizable(false);
		initComponents();
	}

	public WaitDialog(String title) {
		setTitle(title);
		this.title = title;
		/*
		 * Set the dialog to be modal
		 */
		setModalityType(ModalityType.DOCUMENT_MODAL);
		setResizable(false);
		initComponents();
	}
	private void initComponents() {
		JPanel panel = (JPanel) getContentPane();
		panel.setLayout(new BorderLayout(5, 5));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		bar = new JProgressBar();
		bar.setPreferredSize(new Dimension(200, 20));
		bar.setIndeterminate(true);
		bar.setStringPainted(true);
		
		progressMessage = (progressMessage == null)? title : progressMessage;
		bar.setString(progressMessage);
		panel.add(bar, BorderLayout.NORTH);
		pack();
		setLocationRelativeTo(parent);

		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
	}

	private static WaitDialog dialog = null;

	/**
	 * launch a wait dialog with the given title and the given listener to the cancel event
	 * @param title	the dialog title
	 * @param listener	the listener handling the cancel button press
	 */
	public synchronized static void launchWaitDialog(final String title, final WaitDialogListener listener) {
		XTTProperties.printInfo("WaitDialog - launching waitDialog with title "+title);
		if (dialog != null) { // probably some kind of error
			return;
		}
		/*
		 * Execute the open of the dialog in a thread as the dialog is modal
		 */
		dialog = new WaitDialog(TreeTestController.getTestLauncherGui(), listener, title);
		class RunWaitDiaolg extends SwingWorker<String, Object> {
	        public String doInBackground() {
	    		dialog.setVisible(true);
	        	return "";
	        } 
	    }
		new RunWaitDiaolg().execute();
		for (int i = 0 ; i < 600 ;i++){
			if (dialog.isVisible()) {
				break;
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
	}
	
	/**
	 * launch a wait dialog with the given title and the given listener to the cancel event
	 * @param title	the dialog title
	 * @param listener	the listener handling the cancel button press
	 */
	public synchronized static void launchWaitDialog(final String title, final WaitDialogListener listener, final String progressMessage,boolean cancelButton) {
		if (dialog != null) { // probably some kind of error
			return;
		}
		/*
		 * Execute the open of the dialog in a thread as the dialog is modal
		 */
		
		if (!cancelButton && progressMessage!=null){
			dialog = new WaitDialog(TreeTestController.getTestLauncherGui(), listener, title,progressMessage);
		}
		else{
			dialog = new WaitDialog(TreeTestController.getTestLauncherGui(), listener, title);
		}
		(new Thread() {
			public void run() {
				dialog.setVisible(true);
			}
		}).start();
		while (!dialog.isVisible()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
	}
	

	/**
	 * launch a wait dialog with the given title , msg and listener
	 * @param title	the title of the wait dialog
	 * @param listener	the listener for the button press action
	 * @param cancelButtonText	the text on the cancel button
	 */
	public synchronized static void launchWaitDialog(final String title, final WaitDialogListener listener, String cancelButtonText) {
		launchWaitDialog(title, listener);
	}
	
	public static void endWaitDialog() {
		synchronized (staticLock) {
			if (dialog == null && !dialog.isVisible()) {
				return;
			}
			try
			{
				dialog.setTitle("Done...");
				Thread.sleep(100);
				dialog.setVisible(false);
				dialog.dispose();
				dialog = null;
			}
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}
	}

}
