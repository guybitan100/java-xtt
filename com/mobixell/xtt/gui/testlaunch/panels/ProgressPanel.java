package com.mobixell.xtt.gui.testlaunch.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.UIManager;
import com.mobixell.xtt.gui.testlaunch.ProgressListener;

/**
 * @author guy.bitan
 * 
 */
public class ProgressPanel extends JPanel implements ProgressListener {
	private static final long serialVersionUID = 4564376213589613600L;

	JProgressBar testProgressBar;
	private final String CURRENT_TEST_TEXT = "Current Test ";
	public ProgressPanel() {

		UIManager.put("ProgressBar.background", new Color(0xf6, 0xf6, 0xf6));
		UIManager.put("ProgressBar.foreground", new Color(0x8e, 0xa1, 0xb0));

		testProgressBar = new JProgressBar();
		testProgressBar.setMinimum(0);
		testProgressBar.setStringPainted(true);
		testProgressBar.setBorderPainted(true);
		setLayout(new BorderLayout());
		add(BorderLayout.NORTH, testProgressBar);
		testProgressBar.setString(CURRENT_TEST_TEXT + "0 sec.");

		UIManager.put("ProgressBar.background", new Color(0xe1, 0xe4, 0xe6));
		UIManager.put("ProgressBar.background", new Color(0xf6, 0xf6, 0xf6));

		setBorder(BorderFactory.createEmptyBorder(10, 26, 10, 26));

		setBackground(new Color(0xe1, 0xe4, 0xe6));
	}

	public void setTestMaxTime(long time) {
		testProgressBar.setMaximum((int) time);
	}

	public void updateTimes(long testTime) 
	{
		testProgressBar.setValue((int) testTime);
		testProgressBar.setString(CURRENT_TEST_TEXT + getTimeString(testTime));
	}
	private String getTimeString(long time) 
	{
		StringBuffer sb = new StringBuffer();
		if (time >= 0) {
			sb.append(time / 1000);
		}
		sb.append(" sec.");
		return sb.toString();
	}
}
