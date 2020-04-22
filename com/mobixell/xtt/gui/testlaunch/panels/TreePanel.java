package com.mobixell.xtt.gui.testlaunch.panels;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import com.mobixell.xtt.gui.actionItems.AddSubTestAction;
import com.mobixell.xtt.gui.actionItems.AutoSaveTestAction;
import com.mobixell.xtt.gui.actionItems.OpenLoopManagerAction;
import com.mobixell.xtt.gui.actionItems.OpenThreadManagerAction;
import com.mobixell.xtt.gui.actionItems.CheckTreeAction;
import com.mobixell.xtt.gui.actionItems.CollapseTreeAction;
import com.mobixell.xtt.gui.actionItems.ExpandTreeAction;
import com.mobixell.xtt.gui.actionItems.MoveDownAction;
import com.mobixell.xtt.gui.actionItems.MoveToBottomAction;
import com.mobixell.xtt.gui.actionItems.MoveToTopAction;
import com.mobixell.xtt.gui.actionItems.MoveUpAction;
import com.mobixell.xtt.gui.actionItems.OpenBroseTestAction;
import com.mobixell.xtt.gui.actionItems.RemoveAllItemsAction;
import com.mobixell.xtt.gui.actionItems.RemoveItemAction;
import com.mobixell.xtt.gui.actionItems.RunTestAction;
import com.mobixell.xtt.gui.actionItems.SaveAsTestAction;
import com.mobixell.xtt.gui.actionItems.SaveTestAction;
import com.mobixell.xtt.gui.actionItems.SearchAction;
import com.mobixell.xtt.gui.actionItems.ShowLogAction;
import com.mobixell.xtt.gui.actionItems.StopTestAction;
import com.mobixell.xtt.gui.actionItems.UnCheckTreeAction;
import com.mobixell.xtt.gui.testlaunch.ProgressController;
import com.mobixell.xtt.gui.testlaunch.SwingUtils;
import com.mobixell.xtt.gui.testlaunch.TestLauncherGui;
import com.mobixell.xtt.gui.testlaunch.tree.TreeTestController;
import com.mobixell.xtt.images.ImageCenter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
/**
 * @author guyb
 * 
 */
public class TreePanel extends JPanel implements ActionListener
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2310527114376892998L;
	private JToolBar toolBar;
	public static JScrollPane treeView;
	public Toolkit toolkit = Toolkit.getDefaultToolkit();
	public TreeTestController treeTestController;
	public static JButton upButton, topButton, downButton, bottomButton, deleteButton;
	public static JButton openTestButton, saveTestButton, saveAsTestButton, addSubTestButton, addThreadButton,addLoopButton,
			deleteAllButton,stopTestButton;
	public static JButton collapseTreeButton, expandTreeButton, checkTreeButton, unCheckTreeButton, showLogButton,
			runTestButton,searchButton;
	public static JToggleButton autoSaveButton;
	public static JCheckBox repeatCheckBox;
	public static JTextField repeatAmount;
	public static JTextField repeatAmountLeft;
	private JLabel repAmountLeft;
    public ProgressPanel progressPanel;
    public ProgressController progressController;
	public TreePanel(TestLauncherGui testLauncher)
	{
		super(new BorderLayout());
		toolBar = createToolBar();
		progressPanel = new ProgressPanel();
		progressController = new ProgressController(progressPanel);
		progressController.start();
		treeTestController = new TreeTestController(testLauncher);
		treeView = SwingUtils.getJScrollPaneWithWaterMark(
				ImageCenter.getInstance().getAwtImage(ImageCenter.ICON_TEST_TREE_BG), TreeTestController.test.getTree());
		treeView.setAutoscrolls(true);
		// Add the split pane to this panel.
		add(toolBar, BorderLayout.NORTH);
		add(treeView, BorderLayout.CENTER);
		add(progressPanel,BorderLayout.SOUTH);
	}

	public static void setEnabledAll(boolean isEnabled)
	{
		if (isEnabled)
		{
			TreeTestController.handelSelectedToolBar();
		}
		if (!isEnabled)
		{
			collapseTreeButton.setEnabled(false);
			expandTreeButton.setEnabled(false);
			checkTreeButton.setEnabled(false);
			unCheckTreeButton.setEnabled(false);
			upButton.setEnabled(false);
			topButton.setEnabled(false);
			downButton.setEnabled(false);
			bottomButton.setEnabled(false);
			deleteButton.setEnabled(false);
			deleteAllButton.setEnabled(false);
			saveTestButton.setEnabled(false);
			autoSaveButton.setEnabled(false);
			saveAsTestButton.setEnabled(false);
			saveTestButton.setEnabled(false);
			addSubTestButton.setEnabled(false);
			addThreadButton.setEnabled(false);
			addLoopButton.setEnabled(false);
			openTestButton.setEnabled(false);
			runTestButton.setEnabled(false);
			stopTestButton.setEnabled(false);
			repeatCheckBox.setEnabled(false);
			repeatAmountLeft.setEnabled(false);
			repeatAmount.setEnabled(false);
			searchButton.setEnabled(false);
		}
	}
	private JToolBar createToolBar()
	{
		toolBar = SwingUtils.getJToolBarWithBgImage("scenario toolbar", JToolBar.HORIZONTAL, ImageCenter.getInstance()
				.getImage(ImageCenter.ICON_TOP_TOOLBAR_BG));
		toolBar.setLayout(new FlowLayout(FlowLayout.LEFT, 1, 1));
		toolBar.setFloatable(false);
		toolBar.setRollover(true);
		
		collapseTreeButton = toolBar.add(CollapseTreeAction.getInstance());
		collapseTreeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		collapseTreeButton.setEnabled(false);

		expandTreeButton = toolBar.add(ExpandTreeAction.getInstance());
		expandTreeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		expandTreeButton.setEnabled(false);

		checkTreeButton = toolBar.add(CheckTreeAction.getInstance());
		checkTreeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		checkTreeButton.setEnabled(false);

		unCheckTreeButton = toolBar.add(UnCheckTreeAction.getInstance());
		unCheckTreeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		unCheckTreeButton.setEnabled(false);

		upButton = toolBar.add(MoveUpAction.getInstance(this));
		upButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		upButton.setEnabled(false);

		topButton = toolBar.add(MoveToTopAction.getInstance(this));
		topButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		topButton.setEnabled(false);

		downButton = toolBar.add(MoveDownAction.getInstance(this));
		downButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		downButton.setEnabled(false);

		bottomButton = toolBar.add(MoveToBottomAction.getInstance(this));
		bottomButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		bottomButton.setEnabled(false);

		deleteButton = toolBar.add(RemoveItemAction.getInstance(this));
		deleteButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		deleteButton.setEnabled(false);

		toolBar.addSeparator(new Dimension(10, 0));

		deleteAllButton = toolBar.add(RemoveAllItemsAction.getInstance(this));
		deleteAllButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		deleteAllButton.setEnabled(false);
		
		addSubTestButton = toolBar.add(AddSubTestAction.getInstance());
		addSubTestButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		addSubTestButton.setEnabled(false);

		addThreadButton = toolBar.add(OpenThreadManagerAction.getInstance(this));
		addThreadButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		addThreadButton.setEnabled(false);
		
		addLoopButton = toolBar.add(OpenLoopManagerAction.getInstance(this));
		addLoopButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		addLoopButton.setEnabled(false);
		
		toolBar.addSeparator(new Dimension(10, 0));
		
		searchButton = toolBar.add(SearchAction.getInstance(this));
		searchButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		searchButton.setEnabled(false);

		JToolBar runToolBar = SwingUtils.getJToolBarWithBgImage("Run Toolbar", JToolBar.HORIZONTAL, ImageCenter
				.getInstance().getImage(ImageCenter.ICON_RUN_TOOLBAR_BG));

		runToolBar.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 1));
		runToolBar.setFloatable(false);
		runToolBar.setRollover(true);

		repeatAmountLeft = new JTextField("0");
		repeatAmountLeft.setColumns(3);
		repeatAmountLeft.setEnabled(false);
		repeatAmountLeft.setToolTipText("Number of Repeats Left");
		repeatAmountLeft.setName("REPEAT_LEFT_NAME");
		
		repAmountLeft = new JLabel("Left");

		runTestButton = runToolBar.add(RunTestAction.getInstance(this));
		runTestButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		runTestButton.setEnabled(false);
		runTestButton.setMnemonic(KeyEvent.VK_F5);
		runToolBar.addSeparator(new Dimension(10, 0));

		stopTestButton = runToolBar.add(StopTestAction.getInstance(this));
		stopTestButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		stopTestButton.setEnabled(false);
		stopTestButton.setMnemonic(KeyEvent.VK_F5);
		runToolBar.addSeparator(new Dimension(5, 0));
		
		repeatCheckBox = new JCheckBox("Repeat", false);
		repeatCheckBox.setToolTipText("Repeat test");
		repeatCheckBox.setOpaque(false);
		repeatCheckBox.addActionListener(this);

		repeatAmount = new JTextField();
		repeatAmount.addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent e)
			{
				try
				{
					Integer.parseInt(repeatAmount.getText());
					if (repeatAmount.getText().equals("0"))
						repeatAmount.setText("");
				}
				catch (Exception e1)
				{

					repeatAmount.setText("");
				}
			}
			public void keyPressed(KeyEvent e){}			
			public void keyTyped(KeyEvent e){}
		});
		repeatAmount.setColumns(3);
		repeatAmount.setEnabled(false);
		repeatAmount.setToolTipText("Number of tests Repeats");

		runToolBar.add(repeatCheckBox);
		runToolBar.addSeparator(new Dimension(5, 0));
		runToolBar.add(repeatAmount);
		runToolBar.addSeparator(new Dimension(5, 0));
		runToolBar.add(repAmountLeft);
		runToolBar.add(repeatAmountLeft);

		runToolBar.addSeparator(new Dimension(15, 0));
		showLogButton = runToolBar.add(ShowLogAction.getInstance(this));
		showLogButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		showLogButton.setEnabled(false);
		
		toolBar.addSeparator(new Dimension(10, 0));
		toolBar.add(runToolBar);
		toolBar.addSeparator(new Dimension(10, 0));
		
		openTestButton = toolBar.add(OpenBroseTestAction.getInstance(this));
		openTestButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		openTestButton.setEnabled(false);
		toolBar.addSeparator(new Dimension(5, 0));
		
		saveTestButton = toolBar.add(SaveTestAction.getInstance(this));
		saveTestButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		saveTestButton.setEnabled(false);
		toolBar.addSeparator(new Dimension(5, 0));
		
		
		saveAsTestButton = toolBar.add(SaveAsTestAction.getInstance(this));
		saveAsTestButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		saveAsTestButton.setEnabled(false);
		toolBar.addSeparator(new Dimension(5, 0));
		
		autoSaveButton = new JToggleButton();
		autoSaveButton.setAction(AutoSaveTestAction.getInstance());
		autoSaveButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		autoSaveButton.setEnabled(false);
		toolBar.add(autoSaveButton);
		toolBar.addSeparator(new Dimension(5, 0));

		return toolBar;
	}
	public void addAllListeners()
	{
		treeTestController.addAllListeners();
	}
	public void removeAllListeners()
	{
		treeTestController.removeAllListeners();
	}
	public void actionPerformed(ActionEvent e)
	{
		if (repeatCheckBox.isSelected())
		{
			repeatAmount.setEnabled(true);
		}
		else
		{
			repeatAmount.setText("");
			repeatAmount.setEnabled(false);
		}

	}

}
