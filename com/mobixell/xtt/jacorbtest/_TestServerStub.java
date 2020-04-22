package com.mobixell.xtt.jacorbtest;

import com.mobixell.xtt.jacorbtest.TestServerOperations;


/**
 *	Generated from IDL interface "TestServer"
 *	@author JacORB IDL compiler V 2.2.3, 10-Dec-2005
 */

public class _TestServerStub
	extends org.omg.CORBA.portable.ObjectImpl
	implements com.mobixell.xtt.jacorbtest.TestServer
{
	private String[] ids = {"IDL:com/mobixell/xtt/jacorbtest/TestServer:1.0"};
	public String[] _ids()
	{
		return ids;
	}

	public final static java.lang.Class _opsClass = com.mobixell.xtt.jacorbtest.TestServerOperations.class;
	public java.lang.String generic(org.omg.CORBA.Any a)
	{
		while(true)
		{
		if(! this._is_local())
		{
			org.omg.CORBA.portable.InputStream _is = null;
			try
			{
				org.omg.CORBA.portable.OutputStream _os = _request( "generic", true);
				_os.write_any(a);
				_is = _invoke(_os);
				java.lang.String _result = _is.read_string();
				return _result;
			}
			catch( org.omg.CORBA.portable.RemarshalException _rx ){}
			catch( org.omg.CORBA.portable.ApplicationException _ax )
			{
				String _id = _ax.getId();
				throw new RuntimeException("Unexpected exception " + _id );
			}
			finally
			{
				this._releaseReply(_is);
			}
		}
		else
		{
			org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke( "generic", _opsClass );
			if( _so == null )
				throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
			TestServerOperations _localServant = (TestServerOperations)_so.servant;
			java.lang.String _result;			try
			{
			_result = _localServant.generic(a);
			}
			finally
			{
				_servant_postinvoke(_so);
			}
			return _result;
		}

		}

	}

}
