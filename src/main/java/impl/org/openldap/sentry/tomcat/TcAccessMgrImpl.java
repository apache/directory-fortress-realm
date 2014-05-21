/*
 * This work is part of OpenLDAP Software <http://www.openldap.org/>.
 *
 * Copyright 1998-2014 The OpenLDAP Foundation.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted only as authorized by the OpenLDAP
 * Public License.
 *
 * A copy of this license is available in the file LICENSE in the
 * top-level directory of the distribution or, alternatively, at
 * <http://www.OpenLDAP.org/license.html>.
 */

package org.openldap.sentry.tomcat;

import org.openldap.fortress.SecurityException;
import org.openldap.fortress.util.attr.VUtil;
import org.openldap.sentry.J2eePolicyMgr;
import org.openldap.sentry.J2eePolicyMgrFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;

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
    // If this field gets set, use for all subsequent calls to authenticate:
    private List<String> defaultRoles;

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
            // If a 'default.roles' property set in config, user them
            if( VUtil.isNotNullOrEmpty( defaultRoles ))
            {
                prin = j2eeMgr.createSession( userId, password, defaultRoles );
                if (log.isEnabledFor(Level.DEBUG))
                {
                    log.debug(CLS_NM + ".authenticate userId [" + userId + "], with default roles [" + defaultRoles + "], successful");
                }
            }
            else
            {
                prin = j2eeMgr.createSession(userId, password);
                if (log.isEnabledFor(Level.DEBUG))
                {
                    log.debug(CLS_NM + ".authenticate userId [" + userId + "] successful");
                }
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
     * @param roleName  Maps to {@code org.openldap.fortress.rbac.Role#name}.
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

    /**
     * When the 'defaultRoles' parameter is set on realm proxy config (e.g. in server.xml or context.xml) it will be used to pass into
     * createSession calls into Fortress.  This will scope the roles to be considered for activation to this particular set.
     *
     * @param szDefaultRoles contains a String containing comma delimited roles names.
     */
    public void setDefaultRoles(String szDefaultRoles)
    {
        if( VUtil.isNotNullOrEmpty( szDefaultRoles ))
        {
            defaultRoles = Arrays.asList(szDefaultRoles.split("\\s*,\\s*"));
            log.info( "DEFAULT ROLES: " + defaultRoles );
        }
    }
}
