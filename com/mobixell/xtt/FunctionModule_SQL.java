package com.mobixell.xtt;

import java.sql.*;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;


/**
 * Provides JDBC database access.The class provides methods to connect database,
 * select data,insert data,update,delete data.
 * While accessing the database on IPv6 network we need to make sure database is capable of handling
 * IPv6.In case of postgres database we need to enable IPv6 support. i.e we need to make suitable configuration
 * changes in pg_hba file.
 *
 * @version     $Id: FunctionModule_SQL.java,v 1.5 2010/03/18 05:30:39 rajesh Exp $
 */
public class FunctionModule_SQL extends FunctionModule
{
    private Map<String,SQLConnection> connections = Collections.synchronizedMap(new HashMap<String,SQLConnection>());

    public static final String tantau_sccsid = "@(#)$Id: FunctionModule_SQL.java,v 1.5 2010/03/18 05:30:39 rajesh Exp $";

    /*
     * This is inner class to manage database operations like creating connection,close connection
     * commit and roll back transactions.
     */
    private class SQLConnection
    {
        Connection connection=null;
        
        private String name = null;
        /**
         * This class constructor
         */
        public SQLConnection(String name)
        {
            this.name=name;
        }
        
        /**
         * Get connection name.
         */
        public String getName()
        {
            return name;
        }
        
        /**
         * Method to create connection to database.
         * @param databaseURI - Database URL.
         * @param username    - Database user name.
         * @param password    - Database password.
         * @param commit      - Boolean value for commit.
         * @throws SQLException - It throws SQLException. 
         */
        public void connect(String databaseURI, String username, String password, boolean commit) throws SQLException
        {
            //Connect to database.
        	connection = DriverManager.getConnection(databaseURI,username,password);
        	//Setting the auto commit. 
            connection.setAutoCommit(commit);
        }
        /**
         * Close the connection.
         * @throws SQLException - It throws SQLException.
         */
        public void close() throws SQLException
        {
            connection.close();
        }
        /**
         * Commit the transaction.
         * @throws SQLException - It throws SQLException.
         */
        public void commit() throws SQLException
        {
            connection.commit();
        }
        /**
         * Roll back the transaction.
         * @throws SQLException - It throws SQLException.
         */
        public void rollback() throws SQLException
        {
            connection.rollback();
        }
        /**
         * Create the statement.
         * @return - It returns statement 
         * @throws SQLException - It throws SQLException.
         */
        public Statement createStatement() throws SQLException
        {
            Statement stmt=connection.createStatement();
            stmt.setEscapeProcessing(true);
            return stmt;
        }
    }

    /**
     * Open a SQL connection to a specified server.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> connection name,
     *                     <br><code>parameters[2]</code> JDBC driver class to use, see JDBC documentation
     *                     <br><code>parameters[3]</code> database URI, see JDBC documentation,
     *                     <br><code>parameters[4]</code> username, see JDBC documentation,
     *                     <br><code>parameters[5]</code> password, see JDBC documentation,
     *                     <br><code>parameters[6]</code> optional: autocommit on connection (default=true),
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void openConnection(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": openConnection: name driverClass databaseURI username password");
            XTTProperties.printFail(this.getClass().getName()+": openConnection: name driverClass databaseURI username password autoCommit");
            return;
        } else if (parameters.length <6||parameters.length >7)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": name driverClass databaseURI username password");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": name driverClass databaseURI username password autoCommit");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            //Storing the parameters in local variables.
        	String name=parameters[1];
            String driverClass=parameters[2];
            String databaseURI=parameters[3];
            String username=parameters[4];
            String password=parameters[5];

            boolean commit=true;
            
            //Get the boolean value for auto commit parameter i.e parameter[6]
            if(parameters.length>6)commit=ConvertLib.textToBoolean(parameters[6]);
            
            try
            {
                Class.forName(driverClass);
                
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0]+": error getting driver class '"+driverClass+"'");
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            try
            {
                //Creating the instance of SQLConnection.
            	SQLConnection c=new SQLConnection(name);
                //Connect to database.
                c.connect(databaseURI,username,password,commit);
                //Put the newly created database connection in map.
                connections.put(name.toLowerCase(),c);
                XTTProperties.printInfo(parameters[0]+": Connection '"+name+"' opened to "+databaseURI);
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0]+": error getting connection '"+name+"' to "+databaseURI+"/"+username+"/"+password);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
    }

    /**
     * Close a SQL connection.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> connection name,
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void closeConnection(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": closeConnection: name");
            return;
        } else if (parameters.length !=2)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": name");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            //Get the connection from map.
        	SQLConnection c=connections.get(parameters[1].toLowerCase());
            if(c == null)
            {
                XTTProperties.printFail(parameters[0]+": connection '"+parameters[1]+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            try
            {
                //Close the connection
            	c.close();
                XTTProperties.printInfo(parameters[0]+": Connection '"+c.getName()+"' closed");
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0]+": error closing connection '"+c.getName()+"'");
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
    }

    /**
     * Commit update on a SQL connection. Only works when autocommit is set to false.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> connection name,
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void commit(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": commit: name");
            return;
        } else if (parameters.length !=2)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": name");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            //Get the connection from map.
        	SQLConnection c=connections.get(parameters[1].toLowerCase());
            if(c == null)
            {
                XTTProperties.printFail(parameters[0]+": connection '"+parameters[1]+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            try
            {
                //Commit the transaction.
            	c.commit();
                XTTProperties.printInfo(parameters[0]+": Connection '"+c.getName()+"' commited");
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0]+": error commiting connection '"+c.getName()+"'");
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
    }

    /**
     * Rollback update on a SQL connection. Only works when autocommit is set to false.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> connection name,
     *                     <br><code>parameters[2]</code> JDBC driver class to use, see JDBC documentation
     *                     <br><code>parameters[3]</code> database URI, see JDBC documentation,
     *                     <br><code>parameters[4]</code> username, see JDBC documentation,
     *                     <br><code>parameters[5]</code> password, see JDBC documentation,
     *                     <br><code>parameters[6]</code> optional: autocommit on connection (default=true),
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void rollback(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": rollback: name");
            return;
        } else if (parameters.length !=2)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": name");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            //Get the connection from map.
        	SQLConnection c=connections.get(parameters[1].toLowerCase());
            if(c == null)
            {
                XTTProperties.printFail(parameters[0]+": connection '"+parameters[1]+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            try
            {
                //Roll back the transaction.s
            	c.rollback();
                XTTProperties.printInfo(parameters[0]+": Connection '"+c.getName()+"' rolled back");
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0]+": error rolling back connection '"+c.getName()+"'");
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
    }
    
    /**
     * Execute a SQL SELECT query. Stores the results in:
     * <pre>
     * [variable]/columns           -> Number of Columns
     * [variable]/0/[colnum]        -> Column Name
     * [variable]/name/[column]     -> Column Name
     * [variable]/type/[column]     -> Column data type
     * [variable]/rows              -> Number of rows
     * [variable]/length            -> Number of rows
     * [variable]/loop              -> Number of rows plus 1
     * [variable]/[row]/0           -> Row number
     * [variable]/[row]/[colnum]    -> row/column data
     * </pre>
     * Actual row data starts at 1 and column data starts at 1 like the ResultSet of JDBC.
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> connection name,
     *                     <br><code>parameters[2]</code> variable to store the JDBC result,
     *                     <br><code>parameters[3]</code> SQL SELECT statement.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void query(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": query: name variableName sqlQuery");
            return;
        } else if (parameters.length !=4)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": name variableName sqlQuery");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            //Get the connection from the map stored during creating the connection.
        	//parameter[1] is connection name passed through test case.
        	SQLConnection c=connections.get(parameters[1].toLowerCase());
            if(c == null)
            {
                XTTProperties.printFail(parameters[0]+": connection '"+parameters[1]+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            try
            {
                // Starting at 1 for column number and 1 for row number because ResultSet uses this numbers
                String variable=parameters[2];
                //Create the statement.
                Statement stmt=c.createStatement();
                //Execute the query.
                ResultSet rs = stmt.executeQuery(parameters[3]);

                StringBuffer debug=new StringBuffer("  names");
                StringBuffer debugpart=new StringBuffer("  types");
                //Getting the resultset information.
                ResultSetMetaData rsm=rs.getMetaData();
                //Getting the total column count returned by the query.
                int numColumns=rsm.getColumnCount();
                int numRows=0;
                XTTProperties.setVariable(variable+"/columns",""+numColumns);
                //Printing the column names returned by the query
                for(int i=1;i<=numColumns;i++)
                {
                    XTTProperties.setVariable(variable+"/0/"+i,""+rsm.getColumnName(i));
                    XTTProperties.setVariable(variable+"/name/"+i,""+rsm.getColumnName(i));
                    debug.append(";"+rsm.getColumnName(i));
                    XTTProperties.setVariable(variable+"/type/"+i,""+getSQLTypeName(rsm.getColumnType(i)));
                    debugpart.append(";"+getSQLTypeName(rsm.getColumnType(i)));
                }
                debug.append("\n"+debugpart);
                
                //Getting the data from resultset.
                while (rs.next()) 
                {
                    numRows++;
                    debug.append("\n  "+numRows);
                    XTTProperties.setVariable(variable+"/"+numRows+"/"+0,""+numRows);
                    for(int i=1;i<=numColumns;i++)
                    {
                        XTTProperties.setVariable(variable+"/"+numRows+"/"+i,rs.getString(i));
                        debug.append(";"+rs.getString(i));
                    }
	            }
                XTTProperties.setVariable(variable+"/rows",""+numRows);
                XTTProperties.setVariable(variable+"/length",""+numRows);
                XTTProperties.setVariable(variable+"/loop",""+(numRows+1));
	            if(XTTProperties.printDebug(null))
	            {
                    XTTProperties.printInfo(parameters[0]+": Query '"+c.getName()+"': "+parameters[3]+"\n  returned "+numRows+"/"+numColumns+" rows/cols stored in "+variable+"/1/1 to "+variable+"/"+numRows+"/"+numColumns+" \n"+debug);
                } else
                {
                    XTTProperties.printInfo(parameters[0]+": Query '"+c.getName()+"': "+parameters[3]+"\n  returned "+numRows+"/"+numColumns+" rows/cols");
                }
                //Close the result set.
	            rs.close();
	            //close the statement.
                stmt.close();
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0]+": error query connection '"+c.getName()+"'");
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
    }

    /**
     * Execute a SQL UPDATE/DELETE/INSERT request.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> connection name,
     *                     <br><code>parameters[2]</code> variable to store the JDBC result code,
     *                     <br><code>parameters[3]</code> SQL UPDATE statement.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void update(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": update: name variableName sqlUpdate");
            return;
        } else if (parameters.length !=4)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": name variableName sqlUpdate");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            //Get the connection.
        	SQLConnection c=connections.get(parameters[1].toLowerCase());
            if(c == null)
            {
                XTTProperties.printFail(parameters[0]+": connection '"+parameters[1]+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            try
            {
                String variable=parameters[2];
                //Create the statement.
                Statement stmt=c.createStatement();
                //Execute the query.
                int result = stmt.executeUpdate(parameters[3]);

                XTTProperties.setVariable(variable,""+result);
                XTTProperties.printInfo(parameters[0]+": Update '"+c.getName()+"': "+parameters[3]+"\n  returned "+result);
                //Close the statement.
                stmt.close();
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0]+": error update connection '"+c.getName()+"'");
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
    }    
    /**
     * Get the SQLtype
     * @param type - It takes type as int.
     * @return - It returns the string of SQL data type.
     */
    private String getSQLTypeName(int type)
    {
        switch(type)
        {
            case Types.ARRAY :        return "ARRAY";
            case Types.BIGINT:        return "BIGINT";
            case Types.BINARY:        return "BINARY";
            case Types.BIT:           return "BIT";
            case Types.BLOB:          return "BLOB";
            case Types.BOOLEAN:       return "BOOLEAN";
            case Types.CHAR:          return "CHAR";
            case Types.CLOB:          return "CLOB";
            case Types.DATALINK:      return "DATALINK";
            case Types.DATE:          return "DATE";
            case Types.DECIMAL:       return "DECIMAL";
            case Types.DISTINCT:      return "DISTINCT";
            case Types.DOUBLE:        return "DOUBLE";
            case Types.FLOAT:         return "FLOAT";
            case Types.INTEGER:       return "INTEGER";
            case Types.JAVA_OBJECT:   return "JAVA_OBJECT";
            case Types.LONGVARBINARY: return "LONGVARBINARY";
            case Types.LONGVARCHAR:   return "LONGVARCHAR";
            case Types.NULL:          return "NULL";
            case Types.NUMERIC:       return "NUMERIC";
            case Types.OTHER:         return "OTHER";
            case Types.REAL:          return "REAL";
            case Types.REF:           return "REF";
            case Types.SMALLINT:      return "SMALLINT";
            case Types.STRUCT:        return "STRUCT";
            case Types.TIME:          return "TIME";
            case Types.TIMESTAMP:     return "TIMESTAMP";
            case Types.TINYINT:       return "TINYINT";
            case Types.VARBINARY:     return "VARBINARY";
            case Types.VARCHAR:       return "VARCHAR";
            default:            return "UNKNOWN";
        }
    }
    
    /*
    public void test(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": test:"+NO_ARGUMENTS);
            return;
        } else if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else 
        {
            try
            {
                Class.forName("oracle.jdbc.OracleDriver");
                Connection con = DriverManager.getConnection("jdbc:oracle:thin:@pangan.len.tantau.com:1521:pangan","XMG_RPS", "xmg");

                Statement stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT SCR_IDENTIFIER,SCR_USERID FROM OAM_SUBSCRIBER");
                while (rs.next()) 
                {
	                System.out.println(rs.getString("SCR_IDENTIFIER")+":"+rs.getString("SCR_USERID"));
	            }
	        } catch (Exception ex)
	        {
	            ex.printStackTrace();
	        }
        }
    }*/


    /**
     * clear all variables.
     */
    public void initialize()
    {
        XTTProperties.printDebug(this.getClass().getName()+".initialize(): clearing variables");
    }


    /**
     * For Debug resons.
     *
     * @return      String containing the classname of this FunctionModule.
     */
    public String toString()
    {
        return this.getClass().getName();
    }

}
