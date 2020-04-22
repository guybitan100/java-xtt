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
public class ProductsListDialog {
	
	JLabel sortMessageLabel;
	
	private static ProductsListDialog ref;

	// collectios of info
	Vector<String> productNames;
	Vector<String> productVersion;


	// dialog itself.
	ProductListTableDialog tableDialog;
	FunctionModule_Remote fmr;

//	/private static Logger log = Logger.getLogger(Reader.class.getName());

	/**
	 * The constructor is creating the Productlist and init the Table Dialog.
	 * 
	 */
	public ProductsListDialog() 
	{
		SwingUtils.setBusyCursor(XTTProperties.getXTTGui().getTestLauncher(), true);
   	 	WaitDialog.launchWaitDialog("Getting list...",null);
		fmr = new FunctionModule_Remote();
		fmr.initialize();
		fmr.setProductsVersion();
		SwingUtils.setBusyCursor(XTTProperties.getXTTGui().getTestLauncher(), false);
		WaitDialog.endWaitDialog();
		showWindow();
	}

	/**
	 * singletone mechanizem.
	 * 
	 * @return {@link ProductsListDialog}
	 */
	public static ProductsListDialog getInstance()
	{
		if (ref == null) {
			ref = new ProductsListDialog();
		}
		else
		ref.showWindow();
		return ref;
	
	}

	/**
	 * collecting data to both vectors.
	 */
	private void getProductList() {
		// get the classpath list

		/**
		 * break the Product list according to the File.pathSeparatorChar (Linux = :,
		 * Win = ;)
		 */
		
        String[][] products = fmr.getProducts();
        if(products!=null)
        {
		productNames = new Vector<String>();
		productVersion = new Vector<String>();

		for (int i = 0; i < products.length; i++) {
					productNames.add("  "  + products[i][0]);
					productVersion.add("  " + products[i][1]);
			}
        }
	}

	/**
	 * checking if the Dialog is already on, if not , showing it.
	 * 
	 */
	public void showWindow() {
		getProductList();
		if (tableDialog==null)
		tableDialog = new ProductListTableDialog();
		if (!tableDialog.isVisible()) {
			tableDialog.setVisible(true);
		}
	}

	class ProductListTableDialog extends JFrame implements ActionListener {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		JTable table;

		JPanel mainPanel;

		Object[][] data;

		JTextField textFind; // text field for "finding" Product.

		public ProductListTableDialog() {
			setTitle("XMP Product List");

			setIconImage(ImageCenter.getInstance().getAwtImage(ImageCenter.ICON_SCENARIO));

			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			int screenHeight = screenSize.height;
			int screenWidth = screenSize.width;
			setLocation(screenWidth / 4, screenHeight / 5);
			Dimension d = new Dimension((int) (screenWidth / 2.5), (int) (screenHeight / 2.5));
			setPreferredSize(d);
			data = new Object[productNames.size()][4];

			for (int i = 0; i < productNames.size(); i++) {
				data[i][0] = productNames.get(i);
				data[i][1] = productVersion.get(i);
			}

			textFind = new JTextField(10);

			table = new JTable(data, new String[] {"Product Name", "Version",});
			table.setDefaultRenderer(Object.class, new CustomTableCellRenderer());
			DefaultTableCellRenderer tcrColumn = new DefaultTableCellRenderer();
			table.getTableHeader().setDefaultRenderer(new ProductTableHeaderRendrer());

			// aligen to left the versions column
			tcrColumn.setHorizontalAlignment(SwingConstants.LEFT);
			table.getColumnModel().getColumn(1).setCellRenderer(tcrColumn);

			// set the size of coulmn 0
			TableColumn column = table.getColumnModel().getColumn(0);
			table.setSize(d);
			column.setPreferredWidth(200);
			column = table.getColumnModel().getColumn(1);
			column.setPreferredWidth(200);

			// "block" the table from writing
			table.setEnabled(false);

			mainPanel = new JPanel();
			mainPanel.setLayout(new BorderLayout());
			mainPanel.setPreferredSize(d);
			mainPanel.setMinimumSize(d);
			//ImageIcon icon = ImageCenter.getInstance().getImage(ImageCenter.ICON_AQUA_LOGO);

			// define button and add a action listner to it.
			JButton findButton = new JButton("Find Product");
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
			
			JButton sortButton = new JButton("Sort Product");
			sortButton.addActionListener(this);
			sortMessageLabel= new JLabel("The Product appear in the same order they are loaded");	
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
		Vector<String> tmpProductNames;
		Vector<String> tmpVersion ;
		public void actionPerformed(ActionEvent e) {
			
			tmpProductNames = new Vector<String>();
			tmpVersion = new Vector<String>();
			
			if (e.getActionCommand() == "Find Product") {
				sortMessageLabel.setVisible(false);
				findProduct();				
			}						
			if (e.getActionCommand() == "Sort Product") {				
				sortProduct();
			}	
			if (e.getActionCommand() == "Refresh" ) 
			{
				SwingUtils.setBusyCursor(this, true);
				if (XTTProperties.isRemoteXTTRunning)
				{			
				fmr.setProductsVersion();
				getProductList();
				tableDialog = new ProductListTableDialog();
				}
				SwingUtils.setBusyCursor(this, false);
			}
		}
		
		public void findProduct(){
			
			if(!textFind.getText().trim().equals("")){
				for(int i=0 ;i<productNames.size();i++){
					tmpProductNames.add(i, productNames.get(i));
					tmpVersion.add(i, productVersion.get(i));	
				}
				int tmpIndex= 0;
				for(int i=0 ;i<tmpProductNames.size();i++){
					if ((productNames.get(i).toString().trim()).indexOf(textFind.getText().trim()) != -1) {
						tmpProductNames.setElementAt(productNames.get(i), tmpIndex);
						tmpVersion.setElementAt(productVersion.get(i), tmpIndex);
						tmpIndex++;
					}
				}
				
				for(int i=0 ;i<tmpProductNames.size();i++){
					if ((productNames.get(i).toString().trim()).indexOf(textFind.getText().trim()) == -1) {
						tmpProductNames.setElementAt(productNames.get(i), tmpIndex);
						tmpVersion.setElementAt(productVersion.get(i), tmpIndex);
						tmpIndex++;
					}
				}
					
				for (int i = 0; i < table.getRowCount(); i++) {					
					table.setValueAt( tmpProductNames.elementAt(i).toString(), i, 0);
					table.setValueAt( tmpVersion.elementAt(i).toString(), i, 1);
					
				}
				
				for (int i = 0; i < table.getRowCount(); i++) {
					if (!textFind.getText().equals("")) {
						String newValue = tmpProductNames.get(i).toString();
						if ((table.getValueAt(i, 0).toString().trim()).indexOf(textFind.getText().trim()) != -1) {
							table.setValueAt("<bold>" + newValue, i, 0);
						} else {
							table.setValueAt(newValue, i, 0);
						}
					}
				}
			}else{
				sortCounter=-1;
				sortProduct();
			}
		}
		
		public void sortProduct(){
			sortCounter++;
			for(int i=0 ;i<productNames.size();i++){
				tmpProductNames.add(i, productNames.get(i));
				tmpVersion.add(i, productVersion.get(i));
			}
						
			if(sortCounter%3==1){
				sortMessageLabel.setText("Sort by ABC");
				sortMessageLabel.setVisible(true);
				for(int i=0 ;i<tmpProductNames.size()-1;i++){					
					for(int j=0 ;j<tmpProductNames.size()-1;j++){							
						if(tmpProductNames.elementAt(j).toString().compareTo(tmpProductNames.elementAt(j+1).toString())>0){									
							
							String tmp = tmpProductNames.elementAt(j);
							tmpProductNames.setElementAt(tmpProductNames.elementAt(j+1), j);
							tmpProductNames.setElementAt(tmp, j+1);
							
							tmp = tmpVersion.elementAt(j);
							tmpVersion.setElementAt(tmpVersion.elementAt(j+1), j);
							tmpVersion.setElementAt(tmp, j+1);
							
						}
					}	
				}
			}
			
			if(sortCounter%3==2){	
				sortMessageLabel.setText("Sort by ZYX");
				sortMessageLabel.setVisible(true);
				for(int i=0 ;i<tmpProductNames.size()-1;i++){					
					for(int j=0 ;j<tmpProductNames.size()-1;j++){
						if(tmpProductNames.elementAt(j).toString().compareTo(tmpProductNames.elementAt(j+1).toString())<0){								
							String tmp = tmpProductNames.elementAt(j);
							tmpProductNames.setElementAt(tmpProductNames.elementAt(j+1), j);
							tmpProductNames.setElementAt(tmp, j+1);
							
							tmp = tmpVersion.elementAt(j);
							tmpVersion.setElementAt(tmpVersion.elementAt(j+1), j);
							tmpVersion.setElementAt(tmp, j+1);
							
						}
					}	
				}
			}
			
			if(sortCounter%3==0){
				sortMessageLabel.setText("The Product appear in the same order they are loaded");
				sortMessageLabel.setVisible(true);

				 tmpProductNames = productNames;
				 tmpVersion = productVersion;
				
			}
			
			
			for (int i = 0; i < table.getRowCount(); i++) {					
				table.setValueAt( tmpProductNames.elementAt(i).toString(), i, 0);
				table.setValueAt( tmpVersion.elementAt(i).toString(), i, 1);
			}
			
			for (int i = 0; i < table.getRowCount(); i++) {
				if (!textFind.getText().equals("")) {
					String newValue = tmpProductNames.get(i).toString();
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
	public class ProductTableHeaderRendrer extends JLabel implements TableCellRenderer {

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
				setText("Product Name");
				break;
			case 1:
				setText("Version");
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
