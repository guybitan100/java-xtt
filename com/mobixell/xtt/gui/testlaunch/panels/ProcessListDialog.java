/*
 * Copyright 2005-2010 Ignis Software Tools Ltd. All rights reserved.
 */
package com.mobixell.xtt.gui.testlaunch.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import com.mobixell.xtt.FunctionModule_Remote;
import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.gui.testlaunch.SwingUtils;
import com.mobixell.xtt.gui.testlaunch.WaitDialog;
import com.mobixell.xtt.images.ImageCenter;

/**
 *
 * @author Guy.Bitan
 * 
 */
public class ProcessListDialog {
	
	JLabel sortMessageLabel;
	
	private static ProcessListDialog ref;

	// collectios of info
	Vector<String> processNames;
	Vector<String> processStatus;
	Vector<String> processRole;
	Vector<String> processMachine;


	// dialog itself.
	ProcessListTableDialog tableDialog;
	FunctionModule_Remote fmr;

//	/private static Logger log = Logger.getLogger(Reader.class.getName());

	/**
	 * The constructor is creating the processlist and init the Table Dialog.
	 * 
	 */
	public ProcessListDialog() 
	{
		SwingUtils.setBusyCursor(XTTProperties.getXTTGui().getTestLauncher(), true);
   	 	WaitDialog.launchWaitDialog("Getting list...",null);
		fmr = new FunctionModule_Remote();
		fmr.setProcesses();
		SwingUtils.setBusyCursor(XTTProperties.getXTTGui().getTestLauncher(), false);
		WaitDialog.endWaitDialog();
		showWindow();
	}

	/**
	 * singletone mechanizem.
	 * 
	 * @return {@link ProcessListDialog}
	 */
	public static ProcessListDialog getInstance()
	{
		if (ref == null) {
			ref = new ProcessListDialog();
		}
		else
		ref.showWindow();
		return ref;
	
	}

	/**
	 * collecting data to both vectors.
	 */
	private void getProcessList() {
		// get the classpath list

		/**
		 * break the process list according to the File.pathSeparatorChar (Linux = :,
		 * Win = ;)
		 */
		
        String[][] process = fmr.getProcesses();
       
        if (process!=null)
        {
		processNames = new Vector<String>();
		processStatus = new Vector<String>();
		processRole = new Vector<String>();
		processMachine = new Vector<String>();

		for (int i = 0; i < process.length; i++) {
					processNames.add("  "  + process[i][0]);
					processStatus.add("  " + process[i][1]);
					processRole.add("  "   + process[i][2]);
					processMachine.add("  "+ process[i][5]);
			}
        }
	}

	/**
	 * checking if the Dialog is already on, if not , showing it.
	 * 
	 */
	public void showWindow() {
		getProcessList();
		if (tableDialog==null)
		tableDialog = new ProcessListTableDialog();
		if (!tableDialog.isVisible()) {
			tableDialog.setVisible(true);
		}
	}

	class ProcessListTableDialog extends JFrame implements ActionListener {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		JTable table;

		JPanel mainPanel;

		Object[][] data;

		JTextField textFind; // text field for "finding" process.

		public ProcessListTableDialog() {
			setTitle("XMP Process List");

			setIconImage(ImageCenter.getInstance().getAwtImage(ImageCenter.ICON_SCENARIO));

			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			int screenHeight = screenSize.height;
			int screenWidth = screenSize.width;
			setLocation(screenWidth / 4, screenHeight / 5);
			Dimension d = new Dimension((int) (screenWidth / 2.5), (int) (screenHeight / 2.5));
			setPreferredSize(d);
			data = new Object[processNames.size()][4];

			for (int i = 0; i < processNames.size(); i++) {
				data[i][0] = processNames.get(i);
				data[i][1] = processStatus.get(i);
				data[i][2] = processRole.get(i);
				data[i][3] = processMachine.get(i);
			}

			textFind = new JTextField(10);

			table = new JTable(data, new String[] { "Process Name", "Status", "ROLE" ,"Machine"});
			table.setDefaultRenderer(Object.class, new CustomTableCellRenderer());
			DefaultTableCellRenderer tcrColumn = new DefaultTableCellRenderer();
			table.getTableHeader().setDefaultRenderer(new ProcessTableHeaderRendrer());

			// aligen to left the versions column
			tcrColumn.setHorizontalAlignment(SwingConstants.LEFT);
			table.getColumnModel().getColumn(1).setCellRenderer(tcrColumn);
			table.getColumnModel().getColumn(2).setCellRenderer(tcrColumn);

			// set the size of coulmn 0
			TableColumn column = table.getColumnModel().getColumn(0);
			table.setSize(d);
			column.setPreferredWidth(200);
			column = table.getColumnModel().getColumn(1);
			column.setPreferredWidth(200);
			column = table.getColumnModel().getColumn(2);
			column.setPreferredWidth(200);
			column = table.getColumnModel().getColumn(3);
			column.setPreferredWidth(200);

			// "block" the table from writing
			table.setEnabled(false);

			mainPanel = new JPanel();
			mainPanel.setLayout(new BorderLayout());
			mainPanel.setPreferredSize(d);
			mainPanel.setMinimumSize(d);
			//ImageIcon icon = ImageCenter.getInstance().getImage(ImageCenter.ICON_AQUA_LOGO);

			// define button and add a action listner to it.
			JButton findButton = new JButton("Find Process");
			findButton.addActionListener(this);
			
			JButton refreshButton = new JButton("Refresh");
			refreshButton.addActionListener(this);
			
			JPanel findPanel = new JPanel();
			findPanel.setLayout(new GridBagLayout());
			findPanel.add(findButton,0);
			findPanel.add(textFind,1);
			JPanel findPanel2 = new JPanel();
			findPanel2.setLayout(new BorderLayout());
			findPanel2.add(findPanel,BorderLayout.WEST);		
			findPanel2.add(refreshButton,BorderLayout.EAST);
			mainPanel.add(findPanel2,BorderLayout.NORTH);
			
			JButton sortButton = new JButton("Sort Process");
			sortButton.addActionListener(this);
			sortMessageLabel= new JLabel("The process appear in the same order they are loaded");	
			sortMessageLabel.setVisible(true);
	
			JPanel sortPanel = new JPanel();
			sortPanel.setLayout(new GridBagLayout());
			sortPanel.add(sortButton,0);
			sortPanel.add(sortMessageLabel,1);
			JPanel sortPanel2 = new JPanel();
			sortPanel2.setLayout(new BorderLayout());
			sortPanel2.add(sortPanel,BorderLayout.WEST);			
			mainPanel.add(sortPanel2,BorderLayout.SOUTH);
			
			JScrollPane tableScroll = new JScrollPane(table,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			tableScroll.getViewport().setBackground(new Color(0xf6, 0xf6, 0xf6));

			mainPanel.add(tableScroll, BorderLayout.CENTER);
			mainPanel.setBackground(new Color(0xf6, 0xf6, 0xf6));
			//setMinimumSize(d);////
			getContentPane().add(mainPanel);
			setAlwaysOnTop(true);
			pack();
		}

		public void showFrame() {
			setVisible(true);
		}

		int sortCounter=0;		
		Vector<String> tmpProcessNames;
		Vector<String> tmpStatus ;
		Vector<String> tmpRole;
		Vector<String> tmpMachine;
		public void actionPerformed(ActionEvent e) {
			
			tmpProcessNames = new Vector<String>();
			tmpStatus = new Vector<String>();
			tmpRole = new Vector<String>();
			tmpMachine = new Vector<String>();
			
			if (e.getActionCommand() == "Find Process") {
				sortMessageLabel.setVisible(false);
				findProcess();				
			}						
			if (e.getActionCommand() == "Sort Process") {				
				sortProcess();
			}	
			if (e.getActionCommand() == "Refresh" ) 
			{
				SwingUtils.setBusyCursor(this, true);
				if (XTTProperties.isRemoteXTTRunning)
				{
					fmr.setProcesses();
					getProcessList();
					tableDialog = new ProcessListTableDialog();
				}
				SwingUtils.setBusyCursor(this, false);
			}
		}
		
		public void findProcess(){
			
			if(!textFind.getText().trim().equals("")){
				for(int i=0 ;i<processNames.size();i++){
					tmpProcessNames.add(i, processNames.get(i));
					tmpStatus.add(i, processStatus.get(i));	
					tmpRole.add(i, processRole.get(i));
					tmpMachine.add(i, processMachine.get(i));
				}
				int tmpIndex= 0;
				for(int i=0 ;i<tmpProcessNames.size();i++){
					if ((processNames.get(i).toString().trim()).indexOf(textFind.getText().trim()) != -1) {
						tmpProcessNames.setElementAt(processNames.get(i), tmpIndex);
						tmpStatus.setElementAt(processStatus.get(i), tmpIndex);
						tmpRole.setElementAt(processRole.get(i), tmpIndex);
						tmpMachine.setElementAt(processMachine.get(i), tmpIndex);
						tmpIndex++;
					}
				}
				
				for(int i=0 ;i<tmpProcessNames.size();i++){
					if ((processNames.get(i).toString().trim()).indexOf(textFind.getText().trim()) == -1) {
						tmpProcessNames.setElementAt(processNames.get(i), tmpIndex);
						tmpStatus.setElementAt(processStatus.get(i), tmpIndex);
						tmpRole.setElementAt(processRole.get(i), tmpIndex);
						tmpMachine.setElementAt(processMachine.get(i), tmpIndex);
						tmpIndex++;
					}
				}
					
				for (int i = 0; i < table.getRowCount(); i++) {					
					table.setValueAt( tmpProcessNames.elementAt(i).toString(), i, 0);
					table.setValueAt( tmpStatus.elementAt(i).toString(), i, 1);
					table.setValueAt(tmpRole.elementAt(i).toString(), i, 2);
					table.setValueAt(tmpMachine.elementAt(i).toString(), i, 3);
					
				}
				
				for (int i = 0; i < table.getRowCount(); i++) {
					if (!textFind.getText().equals("")) {
						String newValue = tmpProcessNames.get(i).toString();
						if ((table.getValueAt(i, 0).toString().trim()).indexOf(textFind.getText().trim()) != -1) {
							table.setValueAt("<bold>" + newValue, i, 0);
						} else {
							table.setValueAt(newValue, i, 0);
						}
					}
				}
			}else{
				sortCounter=-1;
				sortProcess();
			}
		}
		
		public void sortProcess(){
			sortCounter++;
			for(int i=0 ;i<processNames.size();i++){
				tmpProcessNames.add(i, processNames.get(i));
				tmpStatus.add(i, processStatus.get(i));
				tmpRole.add(i, processRole.get(i));	
				tmpMachine.add(i, processMachine.get(i));
			}
						
			if(sortCounter%3==1){
				sortMessageLabel.setText("Sort by ABC");
				sortMessageLabel.setVisible(true);
				for(int i=0 ;i<tmpProcessNames.size()-1;i++){					
					for(int j=0 ;j<tmpProcessNames.size()-1;j++){							
						if(tmpProcessNames.elementAt(j).toString().compareTo(tmpProcessNames.elementAt(j+1).toString())>0){									
							
							String tmp = tmpProcessNames.elementAt(j);
							tmpProcessNames.setElementAt(tmpProcessNames.elementAt(j+1), j);
							tmpProcessNames.setElementAt(tmp, j+1);
							
							tmp = tmpStatus.elementAt(j);
							tmpStatus.setElementAt(tmpStatus.elementAt(j+1), j);
							tmpStatus.setElementAt(tmp, j+1);
							
							tmp = (String) tmpRole.elementAt(j);
							tmpRole.setElementAt(tmpRole.elementAt(j+1), j);
							tmpRole.setElementAt(tmp, j+1);
							
							tmp = (String) tmpMachine.elementAt(j);
							tmpMachine.setElementAt(tmpMachine.elementAt(j+1), j);
							tmpMachine.setElementAt(tmp, j+1);			
						}
					}	
				}
			}
			
			if(sortCounter%3==2){	
				sortMessageLabel.setText("Sort by ZYX");
				sortMessageLabel.setVisible(true);
				for(int i=0 ;i<tmpProcessNames.size()-1;i++){					
					for(int j=0 ;j<tmpProcessNames.size()-1;j++){
						if(tmpProcessNames.elementAt(j).toString().compareTo(tmpProcessNames.elementAt(j+1).toString())<0){								
							String tmp = tmpProcessNames.elementAt(j);
							tmpProcessNames.setElementAt(tmpProcessNames.elementAt(j+1), j);
							tmpProcessNames.setElementAt(tmp, j+1);
							
							tmp = tmpStatus.elementAt(j);
							tmpStatus.setElementAt(tmpStatus.elementAt(j+1), j);
							tmpStatus.setElementAt(tmp, j+1);
							
							tmp = (String) tmpRole.elementAt(j);
							tmpRole.setElementAt(tmpRole.elementAt(j+1), j);
							tmpRole.setElementAt(tmp, j+1);
							
							tmp = (String) tmpMachine.elementAt(j);
							tmpMachine.setElementAt(tmpMachine.elementAt(j+1), j);
							tmpMachine.setElementAt(tmp, j+1);	
						}
					}	
				}
			}
			
			if(sortCounter%3==0){
				sortMessageLabel.setText("The process appear in the same order they are loaded");
				sortMessageLabel.setVisible(true);

				 tmpProcessNames = processNames;
				 tmpStatus = processStatus;
				 tmpRole = processRole;
				 tmpMachine = processMachine;
				
			}
			
			
			for (int i = 0; i < table.getRowCount(); i++) {					
				table.setValueAt( tmpProcessNames.elementAt(i).toString(), i, 0);
				table.setValueAt( tmpStatus.elementAt(i).toString(), i, 1);
				table.setValueAt( tmpRole.elementAt(i).toString(), i, 2);
				table.setValueAt( tmpMachine.elementAt(i).toString(), i, 3);
			}
			
			for (int i = 0; i < table.getRowCount(); i++) {
				if (!textFind.getText().equals("")) {
					String newValue = tmpProcessNames.get(i).toString();
					if ((table.getValueAt(i, 0).toString().trim()).indexOf(textFind.getText().trim()) != -1) {
						table.setValueAt("<bold>" + newValue, i, 0);
					} else {
						table.setValueAt(newValue, i, 0);
					}
				}
			}

		}

	}

	public class CustomTableCellRenderer extends DefaultTableCellRenderer {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int column) {

			Component cell = null;

			// creating the relevant component (JFieldText/StatusPanel)
			boolean isBold = (value.toString().startsWith("<bold>"));
			if (isBold) {
				cell = setJTextFieldAttr(value.toString().subSequence("<bold>".length(), value.toString().length())
						+ "", isBold);
			} else {
				cell = setJTextFieldAttr(value + "", isBold);
			}

			return cell;
		} // of getTableCellRendererComponent function

		private JTextField setJTextFieldAttr(String cellText, boolean isBold) {
			JTextField cell = new JTextField();
			cell.setText(cellText);
			cell.setBorder(BorderFactory.createEmptyBorder());
			if (isBold)
				cell.setFont(new Font("Times", Font.BOLD, 14));
			return cell;
		} // of setJTextFieldAttr

	} // of class

	/**
	 * Rendrerer for table header
	 * 
	 * @author uri.koaz
	 * 
	 */
	public class ProcessTableHeaderRendrer extends JLabel implements TableCellRenderer {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int column) {

			if (table != null) {
				JTableHeader header = table.getTableHeader();
				if (header != null) {
					setForeground(header.getForeground());
					setBackground(header.getBackground());
					setFont(header.getFont());
				}
			}

			setIcon(ImageCenter.getInstance().getImage(ImageCenter.ICON_TABLE_HEADER));

			setForeground(Color.white);

			switch (column) {
			case 0:
				setText("Process Name");
				break;
			case 1:
				setText("Status");
				break;
			case 2:
				setText("Role");
				break;
			case 3:
				setText("Machine");
				break;	
			default:
				break;
			}

			setBorder(UIManager.getBorder("TableHeader.cellBorder"));
			setHorizontalAlignment(JLabel.CENTER);

			return this;
		}

		public void paint(Graphics g) {
			Dimension size = this.getSize();
			g.drawImage(ImageCenter.getInstance().getAwtImage(ImageCenter.ICON_TABLE_HEADER), 0, 0, size.width,
					size.height, this);

			super.paint(g);
		}
	}
}
