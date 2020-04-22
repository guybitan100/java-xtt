package com.mobixell.xtt.gui.testlaunch.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Vector;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellRenderer;

import com.mobixell.xtt.ModuleList;
import com.mobixell.xtt.Parser;
import com.mobixell.xtt.gui.testlaunch.table.ParamDataItem;
import com.mobixell.xtt.gui.testlaunch.table.ParamTableModel;
import com.mobixell.xtt.gui.testlaunch.table.ParamHeaderRenderer;
import com.mobixell.xtt.gui.testlaunch.tree.TreeTestController;
import com.mobixell.xtt.images.ImageCenter;

public class ParamDialog extends JDialog implements ActionListener,KeyListener
{
	private static final long serialVersionUID = -5967383890282447522L;
	   private Vector<ParamDataItem> vec_items = null;
	   private JTable 		   	     table=null;
	   public  ModuleList 	   		 modules;
	   private ParamTableModel 		 paramTableModel;
	   private JTextField            textField =null;
	   private JButton 		   		 bAddParam = null;
	   private JButton 		   		 bAddEditParamToTest = null;
	   private JButton 		  		 bResetAll = null;
	   private JCheckBox	         checkIsMandatory =null;
	   private JPanel 		   		 pButtons  = null;
	   private JPanel      	   		 panel = null;
	   private JComboBox 	   		 modulesCombo=null;
	   private JComboBox 	   		 functionsCombo=null;
	   private String    	   		 sModule=null;
	   private String    	   		 sFunction=null;
	   private JPanel    	  		 panelModFunc = null;
	   private JCheckBox             checkIsUpdateStepName =null;
	public ParamDialog (JFrame parent,Vector <ParamDataItem> vec_items,ModuleList modules)
	{
		super(parent);
		this.vec_items = vec_items;
		this.modules = modules;
		Image im = Toolkit.getDefaultToolkit().getImage(ImageCenter.getInstance().getImageUrl(ImageCenter.ICON_ARROW_RIGHT_YELLOW));
        this.setIconImage(im);
	}
	public void init(Boolean isDispaly,String sModule,String sFunction)
	{
		     this.sModule = sModule;
		     this.sFunction = sFunction;
			 textField = new JTextField(sModule+ "/" + sFunction);
			 textField.addKeyListener(this);
			 setTitle(sModule+ "/" + sFunction);
			 textField.setToolTipText(sModule+ "/" + sFunction + " description");
			 bAddEditParamToTest=new JButton("Add To Test",ImageCenter.getInstance().getImage(ImageCenter.ICON_ARROW_RIGHT_YELLOW));
		     bAddEditParamToTest.setActionCommand("ADDTOTEST");
		     bAddEditParamToTest.setToolTipText("Add step to tree");
		     bAddEditParamToTest.setPreferredSize(new Dimension(130, 10));
		     
		     initCommon(true,false);
    	
        if (isDispaly)
        {
        	this.pack();
        	this.setLocationRelativeTo(getParent()); //center it 
        	setVisible(true);
        }
        else setVisible(false);
	}
	public void edit(boolean isMandatory, String sModule,String sFunction,Vector<ParamDataItem> vec_items) 
	{   	
			checkIsUpdateStepName = new JCheckBox("Update step name");
			checkIsUpdateStepName.setSelected(false);
			checkIsUpdateStepName.setVisible(false);
			checkIsUpdateStepName.setFont(new Font(Font.DIALOG, Font.TRUETYPE_FONT, 13));
		     
		    modulesCombo=new JComboBox(vectorCopy(modules.getModules()));
		    modulesCombo.setMaximumRowCount(20);
	        modulesCombo.setFont((new Font(Font.SERIF, Font.PLAIN, 16)));
	        modulesCombo.setSelectedItem(sModule);
	        modulesCombo.setBackground(Color.white);
	        
	        functionsCombo=new JComboBox(vectorCopy(modules.getModuleFunctions((String)modulesCombo.getSelectedItem())));
	        functionsCombo.setMaximumRowCount(20);        
	        functionsCombo.setBackground(Color.white);
	        functionsCombo.setFont((new Font(Font.SERIF, Font.PLAIN, 16)));
	        functionsCombo.setSelectedItem(sFunction);
	        
	        panelModFunc=new JPanel(new FlowLayout());
	        panelModFunc.add(modulesCombo);
	        panelModFunc.add(functionsCombo);
	        panelModFunc.add(checkIsUpdateStepName);
	        
	         String title = sModule + "/" + sFunction;
			 setTitle(title);
			 bAddEditParamToTest=new JButton("Update Test",ImageCenter.getInstance().getImage(ImageCenter.ICON_ARROW_RIGHT_YELLOW));
		     bAddEditParamToTest.setActionCommand("UPDATE");
		     bAddEditParamToTest.setToolTipText("Set step to tree");
		     this.vec_items=vec_items;
		     initCommon(false,true);    	
		     paintMandatory (isMandatory);
		     functionsCombo.addActionListener(this);
		     functionsCombo.setActionCommand("FUNCTIONS");
		     modulesCombo.addActionListener(this);
		     modulesCombo.setActionCommand("MODULES");
		     this.pack();
             this.setLocationRelativeTo(null); //center it 
        	 setVisible(true);
	}
	public void initCommon(boolean isInitVec,Boolean isEdit) 
	{
		 
		if (!isEdit)
		{
		 textField.setFont(new Font(Font.SERIF, Font.PLAIN, 12));
		 textField.setColumns(2);
		}
		 
		 JPanel tablePanel = constructTablePanel(isInitVec);
	     bAddParam=new JButton("Add parameter",ImageCenter.getInstance().getImage(ImageCenter.ICON_ADD_IF));
	     bAddParam.setActionCommand("ADDPARAM");
	     bAddParam.setToolTipText("Add parameter to list");
	     bAddParam.addActionListener(this);
	     bAddParam.setPreferredSize(new Dimension(115, 25));
	     
	     bResetAll=new JButton("Reset all",ImageCenter.getInstance().getImage(ImageCenter.ICON_CLEAR));
	     bResetAll.setActionCommand("RESET");
	     bResetAll.addActionListener(this);
	     bResetAll.setToolTipText("Clean all parameters");
	     bResetAll.setPreferredSize(new Dimension(95, 25));
	     
	     bAddEditParamToTest.addActionListener(this);
	     bAddEditParamToTest.setPreferredSize(new Dimension(130, 25));
	     
	     checkIsMandatory=new JCheckBox("Regular step",ImageCenter.getInstance().getImage(ImageCenter.ICON_ARROW_RIGHT_BLUE));
	     checkIsMandatory.setFont(new Font(Font.DIALOG, Font.TRUETYPE_FONT, 12));
	     checkIsMandatory.setForeground(new Color(0,129,223));
	     checkIsMandatory.setPreferredSize(new Dimension(120, 25));
	     checkIsMandatory.setActionCommand("MANDATORY");
	     checkIsMandatory.setToolTipText("Set as mandatory/regular step");
	     checkIsMandatory.addActionListener(this);
	     
	     pButtons=new JPanel();
	     pButtons.add(checkIsMandatory);
	     pButtons.add(bAddEditParamToTest);
	     pButtons.add(bAddParam);
	     pButtons.add(bResetAll);
	     //Init in the first time
	     if  (panel==null)
	     {
			 panel=new JPanel(new BorderLayout());
	     }
	     //If Edit mode
	     else
	     {
			 panel.removeAll();
	     }
	     
	     if (isEdit)
	    	 panel.add(panelModFunc,BorderLayout.NORTH);
	     
	     panel.add(tablePanel,BorderLayout.CENTER);
	     panel.add(pButtons,BorderLayout.SOUTH);
	     add(panel,BorderLayout.SOUTH);
	}

	public void initVec()
	{
		vec_items.clear();
		vec_items.add(new ParamDataItem());
		vec_items.add(new ParamDataItem());
		vec_items.add(new ParamDataItem());
		vec_items.add(new ParamDataItem());
		vec_items.add(new ParamDataItem());
		updateTable();
	}

	public void updateVec()
	{
		if (vec_items.size() == 0)
		{
			vec_items.add(new ParamDataItem());
			vec_items.add(new ParamDataItem());
			vec_items.add(new ParamDataItem());
			vec_items.add(new ParamDataItem());
			vec_items.add(new ParamDataItem());
		}
		updateTable();
	}
	
	private void updateTable()
	{
		 table.getColumnModel().getColumn(ParamTableModel.CHECKBOX_USE).setCellEditor(table.getDefaultEditor((new Boolean(false)).getClass()));
	     table.getColumnModel().getColumn(ParamTableModel.CHECKBOX_GROUP).setCellEditor(table.getDefaultEditor((new Boolean(false)).getClass()));
	     table.getColumnModel().getColumn(ParamTableModel.DROPDOWN).setCellEditor(new DefaultCellEditor(getCombo()));
	     table.getColumnModel().getColumn(ParamTableModel.VALUE).setCellEditor(table.getDefaultEditor((new String("")).getClass()));
	     table.getColumnModel().getColumn(ParamTableModel.CHECKBOX_USE).setMaxWidth(35); 
	     table.getColumnModel().getColumn(ParamTableModel.CHECKBOX_USE).setMinWidth(35);
	     table.getColumnModel().getColumn(ParamTableModel.CHECKBOX_GROUP).setMaxWidth(45); 
	     table.getColumnModel().getColumn(ParamTableModel.CHECKBOX_GROUP).setMinWidth(45); 
	     table.getColumnModel().getColumn(ParamTableModel.DROPDOWN).setMaxWidth(120); 
	     table.getColumnModel().getColumn(ParamTableModel.DROPDOWN).setMinWidth(120);
	     paramTableModel.fireTableDataChanged();
	}
	
	private JPanel constructTablePanel(boolean isInitVec) 
	{
		paramTableModel = new ParamTableModel(vec_items);
		table = new JTable(paramTableModel);
		table.getTableHeader().setReorderingAllowed(false);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		if (isInitVec) initVec();
		else updateVec();

		table.getColumn("Use").setHeaderRenderer(new ParamHeaderRenderer());
		table.getColumn("Parameter").setHeaderRenderer(new ParamHeaderRenderer());
		table.getColumn("Group").setHeaderRenderer(new ParamHeaderRenderer());
		table.getColumn("Value").setHeaderRenderer(new ParamHeaderRenderer());
		table.setRowHeight(18);
		table.setColumnSelectionAllowed(false);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(table);
		scrollPane.getViewport().setPreferredSize(new Dimension(600, 215));
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(scrollPane, BorderLayout.CENTER);

		return panel;
	}
	private JComboBox getCombo()
	 {
	        return new JComboBox(Parser.getParameterList());
	 }
	
	 public JTable gettable()
	 {
	        return this.table;
	 }
	 public ParamTableModel getTableModel()
	 {
	        return this.paramTableModel;
	 }
	public void actionPerformed(ActionEvent e) {
		String[] choicesYesNo = { "Yes", "No" };
		if(e.getActionCommand().equals("RESET"))
        {
			int option = JOptionPane.showOptionDialog(null, "Are you sure do you want to reset?", "Reset patemeters warning", 0, 1, null, choicesYesNo, choicesYesNo[0]);
	        if (option == 0)
	        {
	        if (textField!=null)
			 textField.setText(sModule+ "/" + sFunction + " Description: ");
			 initVec();
             this.setVisible(true);
	        }
        }
		if(e.getActionCommand().equals("ADDPARAM"))
        {
			if (table.isEditing())
				table.getCellEditor().stopCellEditing();
            vec_items.add(new ParamDataItem());
            paramTableModel.fireTableDataChanged();
        }
        if(e.getActionCommand().equals("REMOVEPARAM"))
        {
            vec_items.remove(vec_items.lastElement());
            paramTableModel.fireTableDataChanged();
        }
        if(e.getActionCommand().equals("ADDTOTEST"))
        {
        	addToTestAction ();
        }
        if(e.getActionCommand().equals("UPDATE"))
        {
        	updateTestAction();
        }
        if(e.getActionCommand().equals("MODULES"))
        {
        	checkIsUpdateStepName.setVisible(true);
        	checkIsUpdateStepName.setSelected(true);
        	Vector<String> v=vectorCopy(modules.getModuleFunctions((String)modulesCombo.getSelectedItem()));
            if(functionsCombo!=null)
            	copyVectorToCombo(functionsCombo,v);

            String sModule = (String)modulesCombo.getSelectedItem();
            String sFunction = (String)functionsCombo.getSelectedItem();
            String title = sModule + "/" + sFunction;
			setTitle(title);
        } 
        if(e.getActionCommand().equals("FUNCTIONS"))
        {
        	 checkIsUpdateStepName.setVisible(true);
        	 checkIsUpdateStepName.setSelected(true);
        	 String sModule = (String)modulesCombo.getSelectedItem();
             String sFunction = (String)functionsCombo.getSelectedItem();
             String title = sModule + "/" + sFunction;
 			 setTitle(title);
        }
		if (e.getActionCommand().equals("MANDATORY"))
		{
			paintMandatory (checkIsMandatory.isSelected());
		}  
	}
	
	public void paintMandatory (boolean isSelected)
	{
		checkIsMandatory.setSelected(isSelected);
		
		if (checkIsMandatory.isSelected())
		{
			checkIsMandatory.setText("Mandatory step");
			checkIsMandatory.setFont(new Font(Font.DIALOG, Font.TRUETYPE_FONT, 12));
			checkIsMandatory.setForeground(new Color(236,0,0));
			checkIsMandatory.setIcon(ImageCenter.getInstance().getImage(ImageCenter.ICON_ARROW_RIGHT_RED));
		}
		else
		{
			checkIsMandatory.setText("Regular step");
			checkIsMandatory.setForeground(new Color(0,124,223));
			checkIsMandatory.setFont(new Font(Font.DIALOG, Font.TRUETYPE_FONT, 12));
			checkIsMandatory.setIcon(ImageCenter.getInstance().getImage(ImageCenter.ICON_ARROW_RIGHT_BLUE));
		}
	}
	
	public void addToTestAction ()
	{
		if (table.isEditing())
			table.getCellEditor().stopCellEditing();
    	 try {
			  TreeTestController.createStep(textField.getText(),checkIsMandatory.isSelected(),  sModule, sFunction, vec_items,true);
			  TreeTestController.setTestDirty(true);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	 setVisible(false);
	}
	public void updateTestAction ()
	{
		if (table.isEditing()) table.getCellEditor().stopCellEditing();
		try
		{
			String sModule = (String) modulesCombo.getSelectedItem();
			String sFunction = (String) functionsCombo.getSelectedItem();
			TreeTestController.changeModFunParam(sModule, sFunction, vec_items);
			if (checkIsUpdateStepName.isSelected())
			{
				TreeTestController.changeStepName(sModule + "/" + sFunction, checkIsMandatory.isSelected());
				TreeTestController.setTestDirty(true);
			}
			else
			{
				TreeTestController.changeStepMandatory(checkIsMandatory.isSelected());
			}
			 setVisible(false);
			
		}
		catch (Exception e1)
		{
			e1.printStackTrace();
		}
	}
	public void copyVectorToCombo(JComboBox cb, Vector<String> v) 
	{
		cb.removeActionListener(this);
		cb.removeAllItems();
		for (int i=0;i<v.size();i++)
		{
			cb.addItem(v.get(i));
		}
		cb.addActionListener(this);
	}
	 private Vector<String> vectorCopy(Vector<String> v)
	    {
	        if(v==null)return new Vector<String>();
	        java.util.TreeSet<String> tree=new java.util.TreeSet<String>(v);
	        v=new Vector<String>(tree);

	        Vector<String> x=new Vector<String>();
	        for(int i=0;i<v.size();i++)
	        {
	            x.add(v.get(i));
	        }
	        return x;
	    }
	 class TableRowRenderer extends JLabel implements TableCellRenderer {

			private static final long serialVersionUID = 1L;
			private Color bColor;
			Component comp = null;

			public TableRowRenderer() {

			}

			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
					int row, int column) {

				ParamTableModel model = (ParamTableModel) table.getModel();
				Object v = model.getValueAt(row, column);
				String s = null;
				if(v != null){
					s = v.toString();
				}
				
				setBorder(new LineBorder(Color.white, 1));
				setText(s);
				
				if ((row % 2) == 0) {
					bColor = new Color(0xf7, 0xfd, 0xff);
				} else {
					bColor = Color.white;
				}
				
				// if cell is selected, set background color to default cell selection background color
			     if (isSelected) {
			    	 bColor = new Color(0x99, 0xcc, 0xff);
			     }

				return this;
			}
			public void paint(Graphics g) {
				g.setColor(bColor);

				// Draw a rectangle in the background of the cell
				g.fillRect(0, 0, getWidth() - 1, getHeight() - 1);

				super.paint(g);
			}
		}
	@Override
	public void keyTyped(KeyEvent e){}
	public void keyPressed(KeyEvent e)
	{
					if (e.getKeyCode() == KeyEvent.VK_ENTER)
					{
						try
						{
							if (bAddEditParamToTest.getActionCommand().equalsIgnoreCase("UPDATE"))
							{
								updateTestAction();
							}
							else
							{
								addToTestAction();
							}
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
