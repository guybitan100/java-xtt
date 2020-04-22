package com.mobixell.xtt.gui.testlaunch.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import org.jdom.Document;
import com.mobixell.xtt.gui.testlaunch.tree.TreeTestController;
import com.mobixell.xtt.gui.testlaunch.tree.nodes.AssetNode;
import com.mobixell.xtt.gui.testlaunch.tree.nodes.SubtestNode;
import com.mobixell.xtt.images.ImageCenter;

public class SubtestDialog extends JDialog implements ActionListener ,KeyListener{
	private static final long serialVersionUID = 2392150017532491923L;
	public Document 		  document = null;
	public String 			  testPath = null;
	public String 			  stepName = "";
	public JTextField 		  textFieldStepName = null;
	public JLabel 			  labelTestPath = null;
    public JButton 			  bAddSubTest = null;
    public JButton 			  bBrowse = null;
    public JPanel 			  pButtonsContent = null;
    public JPanel 			  pContent = null;
    private JCheckBox		  checkIsMandatory = null;
    public SubtestNode        node = null;
    private final String      SUBTESTPATH      =  System.getProperty("user.dir") + "\\tests\\XMP\\subtests";
    public static SubtestDialog dialog = null;
    
	public SubtestDialog(JFrame parent, SubtestNode node)
	{
		super(parent);
		setTitle(SUBTESTPATH);
		Image im = Toolkit.getDefaultToolkit().getImage(ImageCenter.getInstance().getImageUrl(ImageCenter.ICON_SUBTEST));
		setIconImage(im);
		setPreferredSize(new Dimension(380, 130));
		labelTestPath = new JLabel();
		pContent = new JPanel(new BorderLayout());
		if (node == null )
		{
			textFieldStepName = new JTextField();
			textFieldStepName.setText("Subtest Description: ");			
			bAddSubTest = new JButton("Add Subtest", ImageCenter.getInstance().getImage(ImageCenter.ICON_ARROW_RIGHT_YELLOW));
			pContent.add(textFieldStepName, BorderLayout.NORTH);
			pContent.add(labelTestPath, BorderLayout.CENTER);
		}
		else
		{
			this.node = node;
			testPath = node.getName();
			this.stepName = null;
			labelTestPath = new JLabel();
			bAddSubTest = new JButton("Update Subtest", ImageCenter.getInstance().getImage(ImageCenter.ICON_ARROW_RIGHT_YELLOW));
			labelTestPath.setText(testPath);
			pContent.add(labelTestPath, BorderLayout.CENTER);
		}
		bAddSubTest.setActionCommand("ADD-UPDATE");
		bAddSubTest.addActionListener(this);
		bBrowse = new JButton("Browse...", ImageCenter.getInstance().getImage(ImageCenter.ICON_IMPORT_WIZ));
		bBrowse.setActionCommand("BROWSE");
		bBrowse.addActionListener(this);
		checkIsMandatory = new JCheckBox("Regular subtest", ImageCenter.getInstance().getImage(ImageCenter.ICON_ARROW_RIGHT_BLUE));
		checkIsMandatory.setFont(new Font(Font.DIALOG, Font.TRUETYPE_FONT, 12));
		checkIsMandatory.setForeground(new Color(0, 129, 223));
		checkIsMandatory.setPreferredSize(new Dimension(130, 25));
		checkIsMandatory.setActionCommand("MANDATORY");
		checkIsMandatory.setToolTipText("Set as mandatory/regular step");
		checkIsMandatory.addActionListener(this);
		if(node!=null)
		paintMandatory(node.isMandatory());
		pButtonsContent = new JPanel();
		pButtonsContent.add(checkIsMandatory);
		pButtonsContent.add(bBrowse);
		pButtonsContent.add(bAddSubTest);
		add(pContent, BorderLayout.CENTER);
		add(pButtonsContent, BorderLayout.SOUTH);
		this.pack();
		this.setLocationRelativeTo(null); // center it
	}
	public static void showDialog(JFrame parent,SubtestNode node) throws Exception
	{
		dialog = new SubtestDialog(parent,node);
		dialog.setModalityType(ModalityType.APPLICATION_MODAL);
		dialog.setVisible(true);
	}
	public void actionPerformed(ActionEvent e)
	{
		if (e.getActionCommand().equals("BROWSE"))
		{
			JFileChooser fc;
			fc = new JFileChooser(SUBTESTPATH);
			fc.setDialogTitle("Open Subtest");
			fc.setMultiSelectionEnabled(false);
			disableButtons(fc);
			fc.setFileFilter(new FileFilter() {
				public boolean accept(File pathname)
				{
					return pathname.getName().toLowerCase().endsWith(".xml");
				}
				public String getDescription()
				{
					return "xml";
				}
			});
			fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			int returnVal = fc.showOpenDialog(fc);

			if (returnVal == JFileChooser.APPROVE_OPTION)
			{
				testPath = fc.getSelectedFile().getName();
				labelTestPath.setText(testPath);
				if (textFieldStepName != null) stepName = textFieldStepName.getText();
			}
		}
		if (e.getActionCommand().equals("ADD-UPDATE"))
		{
			TreeTestController.setTestDirty(true);
			testPath = labelTestPath.getText();
			if (textFieldStepName != null)
			{
				stepName = textFieldStepName.getText();
			}
			else stepName = null;
			try
			{
				if (stepName != null)
				{
					if (testPath == "")
					{
						setTitle("Error Subtest path is empty!!!");
						return;
					}

					TreeTestController.addSubTest(testPath, stepName, false);
					setVisible(false);
				}
				else
				{
					node.setName(testPath);
					((AssetNode)node.getParent()).setMandatory(checkIsMandatory.isSelected(), true);
					TreeTestController.updateSubTestNode(node);
					setVisible(false);
				}

				TreeTestController.handelSelectedToolBar();
			}
			catch (Exception e1)
			{
				e1.printStackTrace();
			}
		}
		if (e.getActionCommand().equals("MANDATORY"))
		{
			paintMandatory(checkIsMandatory.isSelected());
		}
	}
	public static void disableButtons(Container c) {  
        int len = c.getComponentCount();  
        for (int i = 0; i < len; i++) {  
          Component comp = c.getComponent(i);  
          if (comp instanceof JButton) 
          {  
            JButton b = (JButton) comp;  
            Icon icon = b.getIcon();             
            if (icon != null  && icon == UIManager.getIcon("FileChooser.upFolderIcon") 
            		|| icon == UIManager.getIcon("FileChooser.newFolderIcon")
            		|| icon == UIManager.getIcon("FileChooser.homeFolderIcon")){  
              b.setEnabled(false);  
             }   
          } else if (comp instanceof Container) {
        	  if (comp instanceof JComboBox) {  
                  comp.setEnabled(false);  
              }       
              disableButtons((Container) comp); 
          }  
        }  
         
    }
	public void paintMandatory(boolean isSelected)
	{
		checkIsMandatory.setSelected(isSelected);

		if (checkIsMandatory.isSelected())
		{
			checkIsMandatory.setText("Mandatory subtest");
			checkIsMandatory.setFont(new Font(Font.DIALOG, Font.TRUETYPE_FONT, 12));
			checkIsMandatory.setForeground(new Color(236, 0, 0));
			checkIsMandatory.setIcon(ImageCenter.getInstance().getImage(ImageCenter.ICON_ARROW_RIGHT_RED));
		}
		else
		{
			checkIsMandatory.setText("Regular subtest");
			checkIsMandatory.setForeground(new Color(0, 124, 223));
			checkIsMandatory.setFont(new Font(Font.DIALOG, Font.TRUETYPE_FONT, 12));
			checkIsMandatory.setIcon(ImageCenter.getInstance().getImage(ImageCenter.ICON_ARROW_RIGHT_BLUE));
		}
	}
public String getTestPath() {
	return testPath;
}
public void setTestPath(String testPath) {
	this.testPath = testPath;
}

public String getTestDesc() {
	return stepName;
}
public void setTestDesc(String testDesc) {
	this.stepName = testDesc;
}
public void keyTyped(KeyEvent e)
{
	setTitle("SubTest Configuration");
}
public void keyPressed(KeyEvent e) {}
public void keyReleased(KeyEvent e) {}
	
}