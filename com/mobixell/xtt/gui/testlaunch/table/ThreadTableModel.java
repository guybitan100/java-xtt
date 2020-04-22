package com.mobixell.xtt.gui.testlaunch.table;

import java.util.Vector;

import javax.swing.table.AbstractTableModel;


	public class ThreadTableModel extends AbstractTableModel 
    {
        /**
		 * 
		 */
		private static final long serialVersionUID = -4550063364356949659L;
		public static final int STATUS=0;
        public static final int THREADNAME=1;
        public Vector<ThreadDataItem> vec_items=new Vector<ThreadDataItem>();
        
public   ThreadTableModel ( Vector <ThreadDataItem> vec_items)
{
	this.vec_items = vec_items;
}
        public int getColumnCount() 
        { 
            return 2; 
        }
        public int getRowCount() 
        { 
            return vec_items.size();
        }
        public Object getValueAt(int row, int col) 
        { 
        	ThreadDataItem crnt=vec_items.get(row);
            switch(col)
            {
                case STATUS:
                    return crnt.getStatus();
                case THREADNAME:
                    return crnt.getName();
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
                case THREADNAME:
                    return "Thread Name";
                default:
                    return "";
            }
        }
        //The table row/col events are here 
        public void setValueAt(Object value, int row, int col) 
        {
        	ThreadDataItem crnt=vec_items.get(row);
            switch(col)
            {
                case STATUS:
                	crnt.setStatus((Boolean)value);
                    break;
                case THREADNAME:
                	crnt.setStatus(true);
                	crnt.setName((String)value);                    
                    break;
                default:
            }
            fireTableRowsUpdated(row, row);
        }
    
        public boolean isCellEditable(int row, int col) 
        {
        	ThreadDataItem crnt=vec_items.get(row);
        
        	 switch(col)
             {
                case STATUS:
                    return false;
				case THREADNAME:
					if (crnt.getStatus())
					 return true;
					else
						return false;
            }        	
        	 fireTableRowsUpdated(row, row);
        	  return true;
    }
    }
