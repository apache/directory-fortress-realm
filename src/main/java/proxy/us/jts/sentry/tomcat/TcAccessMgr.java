/*
 * Copyright (c) 2009-2014, JoshuaTree. All Rights Reserved.
 */

package us.jts.sentry.tomcat;

import java.security.Principal;

/**
 * This interface prescribes runtime methods that are needed to perform security in Tomcat.  The
 * interface is implemented by Fortress Tomcat Proxy and sentry implementation and allows isolation of
 * runtime dependencies from Tomcat's system classpath..
 *
 * @author Shawn McKinney
 */
public interface TcAccessMgr
{
    /**
     * Determine if given Role is contained within User's Tomcat Principal object.  This method does not need to hit
     * the ldap server as the User's activated Roles are loaded into {@link us.jts.sentry.tomcat.TcPrincipal#setContext(java.util.HashMap)}
     *
     * @param principal Contains User's Tomcat RBAC Session data that includes activated Roles.
     * @param role  Maps to {@code us.jts.fortress.rbac.Role#name}.
     * @return True if Role is found in TcPrincipal, false otherwise.
     */
    public boolean hasRole(Principal principal, String role);


    /**
     * Perform user authentication and evaluate password policies.
     *
     * @param userId   Contains the userid of the user signing on.
     * @param password Contains the user's password.
     * @return Principal which contains the Fortress RBAC session data.
     */
    public Principal authenticate(String userId, char[] password);
}


