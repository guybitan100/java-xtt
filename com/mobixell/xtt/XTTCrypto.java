package com.mobixell.xtt;

/**
 * Additional Cryptography  Providers.
 *
 * @version $Revision: 1.1 $
 * @author  Gavin Cattell
 */
public final class XTTCrypto extends java.security.Provider
{
    private static final String NAME = "XTTCrypto";
    private static final String INFO    = "XTT Additional Cryptography  Providers";
    private static final double VERSION = 1.0;
    
    public XTTCrypto() 
    {
        super(NAME, VERSION, INFO);
        put("MessageDigest.MD4", "com.mobixell.xtt.MD4");
    }
        
}