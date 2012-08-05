/*
 * Copyright (c) 2009-2012. Joshua Tree Software, LLC.  All Rights Reserved.
 */

package com.jts.fortress.sentry;

import com.jts.fortress.cfg.Config;
import com.jts.fortress.SecurityException;
import com.jts.fortress.GlobalErrIds;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Creates an instance of the J2eePolicyMgr object.
 * <p/>
 * The default implementation class is specified as {@link #J2EE_POLICYMGR_DEFAULT_CLASS} but can be overridden by
 * adding the {@link #J2EE_POLICYMGR_IMPLEMENTATION} config property.
 * <p/>
 *
 * @author Shawn McKinney
 * @created February 13, 2010
 */
public class J2eePolicyMgrFactory
{
    /**
     * When this optional tag, {@code realmImplementation}, is placed in Fortress properties, its class name will be the default {@code com.jts.fortress.security.RealmMgr} instance used.
     */
    private static final String J2EE_POLICYMGR_IMPLEMENTATION = "realmImplementation";
    private static final String J2EE_POLICYMGR_DEFAULT_CLASS = "com.jts.fortress.sentry.J2eePolicyMgrImpl";
    private static final String CLS_NM = J2eePolicyMgrFactory.class.getName();
    private static final Logger log = Logger.getLogger(CLS_NM);
    private static String j2eeClassName = Config.getProperty(J2EE_POLICYMGR_IMPLEMENTATION);

    /**
     * Create and return a reference to {@link J2eePolicyMgr} object.
     *
     * @return instance of {@link J2eePolicyMgr}.
     * @throws com.jts.fortress.SecurityException in the event of failure during instantiation.
     */
    public static J2eePolicyMgr createInstance()
        throws SecurityException
    {
        J2eePolicyMgr realmMgr;
        try
        {
            if (j2eeClassName == null || j2eeClassName.compareTo("") == 0)
            {
                j2eeClassName = J2EE_POLICYMGR_DEFAULT_CLASS;
                if (log.isEnabledFor(Level.DEBUG))
                {
                    log.debug(CLS_NM + ".createInstance <" + J2EE_POLICYMGR_IMPLEMENTATION + "> not found.");
                    log.debug(CLS_NM + ".createInstance use default <" + J2EE_POLICYMGR_DEFAULT_CLASS + ">");
                }
            }
            realmMgr = (J2eePolicyMgr) Class.forName(j2eeClassName).newInstance();
        }
        catch (java.lang.ClassNotFoundException e)
        {
            String error = CLS_NM + ".createInstance caught java.lang.ClassNotFoundException=" + e;
            throw new SecurityException(GlobalErrIds.FT_MGR_CLASS_NOT_FOUND, error, e);
        }
        catch (java.lang.InstantiationException e)
        {
            String error = CLS_NM + ".createInstance caught java.lang.InstantiationException=" + e;
            throw new SecurityException(GlobalErrIds.FT_MGR_INST_EXCEPTION, error, e);
        }
        catch (java.lang.IllegalAccessException e)
        {
            String error = CLS_NM + ".createInstance caught java.lang.IllegalAccessException=" + e;
            log.fatal(error);
            throw new SecurityException(GlobalErrIds.FT_MGR_ILLEGAL_ACCESS, error, e);
        }
        return realmMgr;
    }
}