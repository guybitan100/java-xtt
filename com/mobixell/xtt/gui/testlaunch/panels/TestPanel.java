package com.mobixell.xtt.gui.testlaunch.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import org.jdom.Document;
import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.gui.testlaunch.TestLauncherGui;
import com.mobixell.xtt.gui.testlaunch.tree.TreeTestController;
import com.mobixell.xtt.images.ImageCenter;
import com.mobixell.xtt.util.OSUtils;

public class TestPanel extends JPanel implements ActionListener,KeyListener,ItemListener
{
	private static final long serialVersionUID = 2392150017532491923L;
	public Document document = null;
	public JComboBox testLevelCombo;
	public JLabel xfwVersionLabel;
	public JComboBox xfwVersionCombo;
	public JTextArea descTextArea = null;
	public JTextArea stepsTextArea = null;
	public JTextField nameTextField = null;
	public JTextField qcIDTextField = null;
	public JLabel qcIDLabel = null;
	public JLabel designerLabel = null;
	public JLabel designerNameLabel = null;
	public JLabel creationDateLabel = null;
	public JLabel creationDateValueLabel = null;
	public JLabel testLevelLabel = null;
	public JButton bReset = null;
	public JScrollPane scrollPaneDesc = null;
	public JScrollPane scrollPaneSteps = null;
	private JPanel namePanel;
	private JPanel descPanel;
	public boolean isTestDetailsOk = false;
	
	public boolean isTestDetailsOk()
	{
		return isTestDetailsOk;
	}

	public void setTestDetailsOk(boolean isTestDetailsOk)
	{
		this.isTestDetailsOk = isTestDetailsOk;
	}

	public TestPanel()
	{
		setLayout(new BorderLayout());
		nameTextField = new JTextField();
		nameTextField.addKeyListener(this);
		
		qcIDTextField = new JTextField(4);
		qcIDTextField.setDocument(new JTextFieldLimit(5));
		qcIDTextField.setFont(new Font(Font.MONOSPACED, 0, 13));
		qcIDTextField.addKeyListener(this);
		
		designerLabel = new JLabel();
		designerLabel.setFont(TestLauncherGui.DEFAULT_FONT);
		
		creationDateLabel = new JLabel();
		creationDateLabel.setFont(TestLauncherGui.DEFAULT_FONT);
		
		xfwVersionLabel = new JLabel();
		xfwVersionLabel.setFont(TestLauncherGui.DEFAULT_FONT);
		xfwVersionLabel.setText(" XFW Version: ");

		creationDateValueLabel = new JLabel();
		creationDateValueLabel.setFont(new Font(Font.SERIF, Font.BOLD, 16));
		creationDateLabel.setText(" Creation Date: ");

		designerLabel.setText(" Designer: ");
		designerNameLabel = new JLabel();
		designerNameLabel.setFont(new Font(Font.SERIF, Font.BOLD, 16));

		testLevelLabel = new JLabel();
		testLevelLabel.setFont(TestLauncherGui.DEFAULT_FONT);
		testLevelLabel.setText(" Test Level:       ");

		qcIDLabel = new JLabel();
		qcIDLabel.setFont(new Font(Font.SERIF, 0, 13));
		qcIDLabel.setText("       QC Id:  ");

		descTextArea = new JTextArea();
		descTextArea.addKeyListener(this);
		stepsTextArea = new JTextArea();

		namePanel = new JPanel(new BorderLayout());
		namePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Test Name:"));
		namePanel.add(nameTextField, BorderLayout.NORTH);
		
		descPanel = new JPanel(new BorderLayout());
		descPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Test Description:"));
		descPanel.setOpaque(true);
		
		testLevelCombo = new JComboBox(new String[] { "---", "Integration", "Regression", "Full Regression" });
		testLevelCombo.setFont(TestLauncherGui.DEFAULT_FONT);
		testLevelCombo.setBackground(Color.WHITE);
		testLevelCombo.addItemListener(this);

		xfwVersionCombo = new JComboBox();
		xfwVersionCombo.setMaximumRowCount(10);
		xfwVersionCombo.setFont(TestLauncherGui.DEFAULT_FONT);
		xfwVersionCombo.addActionListener(this);
		xfwVersionCombo.setActionCommand("XFWVER");
		xfwVersionCombo.setBackground(Color.WHITE);
		xfwVersionCombo.addItemListener(this);
		
		JPanel testDetailsPanel = new JPanel(new BorderLayout());
		testDetailsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Test Details:"));
		
		JPanel testDPanel1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 12));
		JPanel testDPanel2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 12));
		JPanel testDPanel3 = new JPanel(new BorderLayout());
		
		JPanel testDPanel4 = new JPanel(new BorderLayout());
		JPanel testDPanel6 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 12));
		JPanel testDPanel7 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 12));
		
		testDPanel1.add(creationDateLabel);
		testDPanel1.add(creationDateValueLabel);

		testDPanel2.add(designerLabel);
		testDPanel2.add(designerNameLabel);
		testDPanel2.add(qcIDLabel);
		testDPanel2.add(qcIDTextField);

		testDPanel3.add(testDPanel1,BorderLayout.NORTH);
		testDPanel3.add(testDPanel2,BorderLayout.SOUTH);
		
		testDPanel6.add(xfwVersionLabel);
		testDPanel6.add(xfwVersionCombo);

		testDPanel7.add(testLevelLabel);
		testDPanel7.add(testLevelCombo);

		testDPanel4.add(testDPanel6,BorderLayout.NORTH);
		testDPanel4.add(testDPanel7,BorderLayout.SOUTH);
		

		testDPanel3.setBorder(BorderFactory.createEtchedBorder());
		testDPanel4.setBorder(BorderFactory.createEtchedBorder());
		
		testDetailsPanel.add(testDPanel3, BorderLayout.CENTER);
		testDetailsPanel.add(testDPanel4, BorderLayout.NORTH);

		descTextArea.setFont(TestLauncherGui.DEFAULT_FONT);
		descTextArea.setColumns(TestLauncherGui.DEFAULT_COL);
		descTextArea.setEditable(true);
		descTextArea.setLineWrap(true);
		descTextArea.setWrapStyleWord(true);
		descTextArea.setDragEnabled(true);
		descTextArea.setRows(TestLauncherGui.DEFAULT_ROW);

		stepsTextArea.setFont(TestLauncherGui.DEFAULT_FONT);
		stepsTextArea.setColumns(TestLauncherGui.DEFAULT_COL);
		stepsTextArea.setEditable(false);
		stepsTextArea.setLineWrap(true);
		stepsTextArea.setWrapStyleWord(true);
		stepsTextArea.setDragEnabled(true);
		stepsTextArea.setRows(TestLauncherGui.DEFAULT_ROW - 2);
		stepsTextArea.setBackground(new Color(0xf6, 0xf6, 0xf6));

		scrollPaneDesc = new JScrollPane(descTextArea,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPaneSteps = new JScrollPane(stepsTextArea,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
		
		descPanel.add(scrollPaneDesc, BorderLayout.NORTH);
		descPanel.add(scrollPaneSteps, BorderLayout.CENTER);

		bReset = new JButton("Reset", ImageCenter.getInstance().getImage(ImageCenter.ICON_CLEAR));
		bReset.setActionCommand("RESET");
		bReset.addActionListener(this);
		bReset.setPreferredSize(new Dimension(20, 30));

		try
		{
			init();
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		JPanel contentPanel = new JPanel(new BorderLayout());

		contentPanel.add(namePanel, BorderLayout.NORTH);
		contentPanel.add(descPanel, BorderLayout.CENTER);

		contentPanel.setBorder(BorderFactory.createRaisedBevelBorder());
		add(testDetailsPanel, BorderLayout.NORTH);
		add(contentPanel, BorderLayout.CENTER);
		add(bReset, BorderLayout.SOUTH);
		
	}

	public void validateFields() throws Exception
	{
		String message = "";

		if (nameTextField.getText().equals(""))
		{
			message = message + "Test Name is empty!\n";
		}
		if (nameTextField.getText().indexOf("<") >= 0)
		{
			message = message + "Test Name has special char '<'\n";
		}
		if (descTextArea.getText().equals(""))
		{
			message = message + "Test Description is empty!\n";
		}
		if (descTextArea.getText().indexOf("<") >= 0)
		{
			message = message + "Test Description has special char '<'\n";
		}
		if (testLevelCombo.getSelectedItem().toString().indexOf("--") >= 0)
		{
			message = message + "Test Level didn't selected!\n";
		}
		if (!message.equalsIgnoreCase(""))
		{
			isTestDetailsOk = false;
			JOptionPane.showMessageDialog(this, message, "Test Configuration", 2);
			return;
		}
		else
		{
			isTestDetailsOk = true;
			updateTestTreeFromPanel();
		}
	}
	public void updateTestPanelFromTree(boolean isUpdateTestTree) throws Exception
	{
		testLevelCombo.removeItemListener(this);
		xfwVersionCombo.removeItemListener(this);
		
		if (isUpdateTestTree) updateTestTreeFromPanel();
		nameTextField.setText(TreeTestController.getTestName());
		descTextArea.setText(TreeTestController.getTestDescription());
		stepsTextArea.setText(TreeTestController.getAllSteps());
		
		testLevelCombo.setSelectedItem(TreeTestController.getTestLevel());
		qcIDTextField.setText(TreeTestController.getQCID());
		creationDateValueLabel.setText(TreeTestController.getTestCreationDate());
		designerNameLabel.setText(TreeTestController.getTestDesigner());
		xfwVersionCombo.setSelectedItem(TreeTestController.getTestXfwVersion());
		
		testLevelCombo.addItemListener(this);
		xfwVersionCombo.addItemListener(this);
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			   public void run() { 
				   scrollPaneDesc.getVerticalScrollBar().setValue(0);
				   scrollPaneSteps.getVerticalScrollBar().setValue(0);
			   }
			});
	}
	public void resetTestPanel() throws Exception
	{
		nameTextField.setText("NewTest");
		descTextArea.setText("");
		stepsTextArea.setText("");
		testLevelCombo.setSelectedItem("---");
		qcIDTextField.setText("");
		designerNameLabel.setText(OSUtils.getLoginUser() + " ");
		creationDateValueLabel.setText(OSUtils.getOSDateTime() + " ");
		xfwVersionCombo.setSelectedItem("");
	}
	public void actionPerformed(ActionEvent e)
	{
		if (e.getActionCommand().equals("RESET"))
		{
			try
			{
				init();
			}
			catch (Exception e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}
	public void getXFWVersion(JComboBox cb)
	{
		try
		{
			File folder = new File(OSUtils.getSystemPath() + "/tests/XMP");
			File[] listOfFiles = folder.listFiles();
			for (int i = 0; i < listOfFiles.length; i++)
			{
				if (listOfFiles[i].isDirectory())
				{
					if (listOfFiles[i].getName().indexOf("XFW") > 0) 
						cb.addItem(listOfFiles[i].getName());
				}
			}
			cb.setSelectedIndex(1);
		}
		catch (Exception e)
		{
			XTTProperties.printDebug("Test Launcher: Could't find the path to: " + OSUtils.getSystemPath()
					+ "/tests/XMP");
		}
	}

	public void getXFWFeatureName(String selectedXFWVersion, JComboBox cb)
	{
		try
		{
			File folder = new File(OSUtils.getSystemPath() + "/tests/XMP/" + selectedXFWVersion);
			File[] listOfFiles = folder.listFiles();
			cb.removeAllItems();
			cb.addItem("------");
			for (int i = 0; i < listOfFiles.length; i++)
			{
				if (listOfFiles[i].isDirectory())
				{
					if (listOfFiles[i].getName().indexOf(".svn") < 0) cb.addItem(listOfFiles[i].getName());
				}
			}
		}
		catch (Exception e)
		{
			XTTProperties.printDebug("Test Launcher: Could't find the path to: " + OSUtils.getSystemPath()
					+ "/tests/XMP");
		}
	}

	public void init() throws Exception
	{
		nameTextField.setText("NewTest");
		descTextArea.setText("");
		designerNameLabel.setText(OSUtils.getLoginUser() + " ");
		creationDateValueLabel.setText(OSUtils.getOSDateTime() + " ");
		getXFWVersion(xfwVersionCombo);
		qcIDTextField.setText("");
		xfwVersionCombo.setSelectedIndex(1);
		testLevelCombo.setSelectedIndex(0);
		updateTestTreeFromPanel();
	}

	public void updateTestTreeFromPanel() throws Exception
	{
		TreeTestController.updateTestTree(nameTextField.getText(), 
										  qcIDTextField.getText(), 
										  descTextArea.getText(), 
										  stepsTextArea.getText(), 
										  designerNameLabel.getText(),
										  creationDateValueLabel.getText(), 
										  testLevelCombo.getSelectedItem().toString(), 
										  xfwVersionCombo.getSelectedItem().toString());
	}

	public String getTestName()
	{
		return nameTextField.getText();
	}

	public String getDescription()
	{
		return descTextArea.getText();
	}


	public String getStepsDesc()
	{
		return stepsTextArea.getText();
	}

	public void keyTyped(KeyEvent e){}
	public void keyPressed(KeyEvent e){}
	public void keyReleased(KeyEvent e)
	{
		try
		{
			updateTestTreeFromPanel();
		}
		catch (Exception e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
	}

	public void itemStateChanged(ItemEvent e) {
		try
		{
			updateTestTreeFromPanel();
		}
		catch (Exception e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
}
class JTextFieldLimit extends PlainDocument
{
	private static final long serialVersionUID = 1L;
	private int limit;
	// optional uppercase conversion
	private boolean toUppercase = false;

	JTextFieldLimit(int limit)
	{
		super();
		this.limit = limit;
	}

	JTextFieldLimit(int limit, boolean upper)
	{
		super();
		this.limit = limit;
		toUppercase = upper;
	}

	public void insertString(int offset, String str, javax.swing.text.AttributeSet attr) throws BadLocationException
	{
		if (str == null) return;

		if ((getLength() + str.length()) <= limit)
		{
			if (toUppercase) str = str.toUpperCase();
			super.insertString(offset, str, attr);
		}
	}

}
