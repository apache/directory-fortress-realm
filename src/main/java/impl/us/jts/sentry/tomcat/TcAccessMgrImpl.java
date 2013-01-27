/*
 * Copyright (c) 2009-2013, JoshuaTree. All Rights Reserved.
 */

package us.jts.sentry.tomcat;

import us.jts.fortress.SecurityException;
import us.jts.sentry.J2eePolicyMgr;
import us.jts.sentry.J2eePolicyMgrFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.security.Principal;

/**
 * This class runs on a URL classloader and provides Fortress runtime security services for the Tomcat container.
 *
 * @author Shawn McKinney
 */
public class TcAccessMgrImpl implements TcAccessMgr
{
    private static final String CLS_NM = TcAccessMgrImpl.class.getName();
    private static final Logger log = Logger.getLogger(CLS_NM);
    private static int count = 0;
    private J2eePolicyMgr j2eeMgr;

    /**
     * Constructor for the TcAccessMgrImpl object
     */
    public TcAccessMgrImpl()
    {
        try
        {
            j2eeMgr = J2eePolicyMgrFactory.createInstance();
            log.info(CLS_NM + " constructor <" + count++ + ">");
        }
        catch (SecurityException se)
        {
            String error = CLS_NM + " constructor caught SecurityException=" + se;
            log.fatal(error);
            se.printStackTrace();
            throw new java.lang.RuntimeException(error, se);
        }
    }


    /**
     * Perform user authentication and evaluate password policies.
     *
     * @param userId   Contains the userid of the user signing on.
     * @param password Contains the user's password.
     * @return Principal which contains the Fortress RBAC session data.
     */
    public Principal authenticate(String userId, char[] password)
    {
        TcPrincipal prin = null;
        try
        {
            prin = j2eeMgr.createSession(userId, password);
            if (log.isEnabledFor(Level.DEBUG))
            {
                log.debug(CLS_NM + ".authenticate userId <" + userId + "> successful");
            }
        }
        catch (SecurityException se)
        {
            String warning = CLS_NM + ".authenticate userId <" + userId + "> caught SecurityException=" + se;
            log.warn(warning);
        }
        return prin;
    }


    /**
     * Determine if given Role is contained within User's Tomcat Principal object.  This method does not need to hit
     * the ldap server as the User's activated Roles are loaded into {@link TcPrincipal#setContext(java.util.HashMap)}
     *
     * @param principal Contains User's Tomcat RBAC Session data that includes activated Roles.
     * @param roleName  Maps to {@code us.jts.fortress.rbac.Role#name}.
     * @return True if Role is found in TcPrincipal, false otherwise.
     */
    public boolean hasRole(Principal principal, String roleName)
    {
        boolean result = false;
        String userId = principal.getName();
        try
        {
            if (j2eeMgr.hasRole(principal, roleName))
            {
                if (log.isEnabledFor(Level.DEBUG))
                {
                    log.debug(CLS_NM + ".hasRole userId <" + principal.getName() + "> role <" + roleName + "> successful");
                }
                result = true;
            }
            else
            {
                if (log.isEnabledFor(Level.DEBUG))
                {
                    log.debug(CLS_NM + ".hasRole userId <" + principal.getName() + "> role <" + roleName + "> failed");
                }
            }
        }
        catch (SecurityException se)
        {
            String warning = CLS_NM + ".hasRole userId <" + userId + "> role <" + roleName + "> caught SecurityException=" + se;
            log.warn(warning);
		}
		return result;
	}
}
