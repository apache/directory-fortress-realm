/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.fortress.realm.tomcat;

import java.security.Principal;

/**
 * This interface prescribes runtime methods that are needed to perform security in Tomcat.  The
 * interface is implemented by Fortress Tomcat Proxy and sentry implementation and allows isolation of
 * runtime dependencies from Tomcat's system classpath..
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface TcAccessMgr
{
    /**
     * Determine if given Role is contained within User's Tomcat Principal object.  This method does not need to hit
     * the ldap server as the User's activated Roles are loaded into {@link org.apache.directory.fortress.realm.tomcat.TcPrincipal#setContext(java.util.HashMap)}
     *
     * @param principal Contains User's Tomcat RBAC Session data that includes activated Roles.
     * @param role  Maps to {@code org.apache.directory.fortress.core.model.Role#name}.
     * @return True if Role is found in TcPrincipal, false otherwise.
     */
    boolean hasRole( Principal principal, String role );


    /**
     * Perform user authentication and evaluate password policies.
     *
     * @param userId   Contains the userid of the user signing on.
     * @param password Contains the user's password.
     * @return Principal which contains the Fortress RBAC session data.
     */
    Principal authenticate( String userId, char[] password );

    /**
     * When the 'defaultRoles' parameter is set on realm proxy config (e.g. in server.xml or context.xml) it will be used to pass into
     * createSession calls into Fortress.  This will scope the roles to be considered for activation to this particular set.
     *
     * @param roles contains a String containing comma delimited roles names.
     */
    void setDefaultRoles( String roles );
}