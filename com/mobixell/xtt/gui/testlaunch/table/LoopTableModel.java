package com.mobixell.xtt.gui.testlaunch.table;

import java.util.Vector;

import javax.swing.table.AbstractTableModel;


	public class LoopTableModel extends AbstractTableModel 
    {
        /**
		 * 
		 */
		private static final long serialVersionUID = -4550063364356949659L;
		public static final int STATUS=0;
        public static final int LOOPNAME=1;
        public static final int START=2;
        public static final int STOP=3;
        public static final int STEP=4;
        public Vector<LoopDataItem> vec_items=new Vector<LoopDataItem>();
        
public   LoopTableModel ( Vector <LoopDataItem> vec_items)
{
	this.vec_items = vec_items;
}
        public int getColumnCount() 
        { 
            return 5; 
        }
        public int getRowCount() 
        { 
            return vec_items.size();
        }
        public Object getValueAt(int row, int col) 
        { 
        	LoopDataItem crnt=vec_items.get(row);
            switch(col)
            {
                case STATUS:
                    return crnt.getStatus();
                case LOOPNAME:
                    return crnt.getName();
                case START:
                    return crnt.getStart();
                case STOP:
                    return crnt.getStop();
                case STEP:
                    return crnt.getStep();
                default:
                    return null;
            }
        }
        public Class<? extends Object> getColumnClass(int c) 
        {
            return getValueAt(0, c).getClass();
        }
        public String getColumnName(int col) 
        {
            switch(col)
            {
                case STATUS:
                    return "Status";
                case LOOPNAME:
                    return "Name";
                case START:
                    return "Start";
                case STOP:
                    return "Stop";
                case STEP:
                    return "Step";
                default:
                    return "";
            }
        }
        //The table row/col events are here 
        public void setValueAt(Object value, int row, int col) 
        {
        	LoopDataItem crnt=vec_items.get(row);
            switch(col)
            {
                case STATUS:
                	crnt.setStatus((Boolean)value);
                    break;
                case LOOPNAME:
                	crnt.setStatus(true);
                	crnt.setName((String)value);                    
                    break;
                case START:
                	crnt.setStatus(true);
                	crnt.setStart((String)value);                    
                    break;
                case STOP:
                	crnt.setStatus(true);
                	crnt.setStop((String)value);                    
                    break;
                case STEP:
                	crnt.setStatus(true);
                	crnt.setStep((String)value);                    
                    break;     
                default:
            }
            fireTableRowsUpdated(row, row);
        }
    
        public boolean isCellEditable(int row, int col) 
        {
        	LoopDataItem crnt=vec_items.get(row);
        
				switch (col) {
				case STATUS:
					return false;
				case LOOPNAME:
					if (crnt.getStatus())
						return true;
					else
						return false;
				case START:
					return true;
				case STOP:
					return true;
				case STEP:
					return true;
				}
        	 fireTableRowsUpdated(row, row);
        	  return true;
    }
    }
