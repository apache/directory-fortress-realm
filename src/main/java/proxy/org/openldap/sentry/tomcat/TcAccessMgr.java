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
     * the ldap server as the User's activated Roles are loaded into {@link org.openldap.sentry.tomcat.TcPrincipal#setContext(java.util.HashMap)}
     *
     * @param principal Contains User's Tomcat RBAC Session data that includes activated Roles.
     * @param role  Maps to {@code org.openldap.fortress.rbac.Role#name}.
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

    /**
     * When the 'defaultRoles' parameter is set on realm proxy config (e.g. in server.xml or context.xml) it will be used to pass into
     * createSession calls into Fortress.  This will scope the roles to be considered for activation to this particular set.
     *
     * @param roles contains a String containing comma delimited roles names.
     */
    public void setDefaultRoles(String roles);
}