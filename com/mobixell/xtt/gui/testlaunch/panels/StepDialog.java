package com.mobixell.xtt.gui.testlaunch.panels;
import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.jdom.Document;

import com.mobixell.xtt.gui.testlaunch.TestLauncherGui;
import com.mobixell.xtt.gui.testlaunch.tree.TreeTestController;
import com.mobixell.xtt.images.ImageCenter;

public class StepDialog extends JDialog implements ActionListener,KeyListener {
	public static StepDialog dialog = null;
	private static final long serialVersionUID = 2392150017532491923L;
	public Document document = null;
	public String name = "";
	public JTextField nameTextField = null;
    public JButton bSaveTest =null;
    public JPanel pButtonsContent=null; 
	public StepDialog(JFrame parent,String name) 
	{
		super(parent);
		Image im = Toolkit.getDefaultToolkit().getImage(ImageCenter.getInstance().getImageUrl(ImageCenter.ICON_GREEN));
        this.setIconImage(im);
		setTitle("Modify Step Name");
		this.name = name;
		initCommonFileds();
		initTextFileds(false);
		this.pack();
		this.setLocationRelativeTo(null); // center it
		this.setVisible(false);
	}
	public static void showDialog(JFrame parent,String name) throws Exception
	{
			dialog = new StepDialog(parent,name);
			dialog.initTextFileds(false);
			dialog.pack();
			dialog.setLocationRelativeTo(null); // center it
			dialog.setVisible(true);
	}

	public void actionPerformed(ActionEvent e) 
	{
	if (e.getActionCommand().equals("RESET")) 
	{
			initTextFileds(true);
			this.pack();
			this.setLocationRelativeTo(null); // center it
			setVisible(true);
		}
		if (e.getActionCommand().equals("SAVE")) 
		{
			save ();
		}
	}
	public void save ()
	{
		try {
			name = nameTextField.getText();
			TreeTestController.changeStepName(name);
				setVisible(false);
		
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		setVisible(false);
	}
public void initCommonFileds()
{
	nameTextField = new JTextField();
	nameTextField.addKeyListener(this);
	nameTextField.setColumns(TestLauncherGui.DEFAULT_COL);
	
	bSaveTest = new JButton("Save & Exit", ImageCenter.getInstance()
			.getImage(ImageCenter.ICON_SAVE));
	bSaveTest.setActionCommand("SAVE");
	bSaveTest.addActionListener(this);

	pButtonsContent = new JPanel();
	pButtonsContent.add(bSaveTest);
	
	add(nameTextField, BorderLayout.CENTER);
	add(pButtonsContent, BorderLayout.SOUTH);
}
public void initTextFileds(boolean isClean)
{
	if(isClean)
	{
		nameTextField.setText("");
		name="";
	}
	else
		nameTextField.setText(name);
}
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	public void keyTyped(KeyEvent e){}
	public void keyPressed(KeyEvent e)
	{
					if (e.getKeyCode() == KeyEvent.VK_ENTER)
					{
						try
						{
							save ();
						}
						catch (Exception e1)
						{
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
		
	}
	public void keyReleased(KeyEvent e){}
}