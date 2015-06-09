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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.directory.fortress.core.SecurityException;
import org.apache.directory.fortress.realm.J2eePolicyMgr;
import org.apache.directory.fortress.realm.J2eePolicyMgrFactory;
import org.apache.directory.fortress.realm.TcPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;

/**
 * This class runs on a URL classloader and provides Fortress runtime security services for the Tomcat container.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class TcAccessMgrImpl implements TcAccessMgr
{
    private static final String CLS_NM = TcAccessMgrImpl.class.getName();
    
    /** The logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( CLS_NM );
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
            LOG.info( "{} constructor <{}>", CLS_NM, incrementCtr() );
        }
        catch ( SecurityException se )
        {
            String error = CLS_NM + " constructor caught SecurityException=" + se;
            LOG.error( error );
            se.printStackTrace();
            throw new java.lang.RuntimeException( error, se );
        }
    }

    private static int incrementCtr()
    {
        return count++;
    }
    

    /**
     * Perform user authentication and evaluate password policies.
     *
     * @param userId   Contains the userid of the user signing on.
     * @param password Contains the user's password.
     * @return Principal which contains the Fortress RBAC session data.
     */
    public Principal authenticate( String userId, char[] password )
    {
        TcPrincipal principal = null;
        
        try
        {
            // If a 'default.roles' property set in config, user them
            if ( CollectionUtils.isNotEmpty( defaultRoles ) )
            {
                principal = j2eeMgr.createSession( userId, password, defaultRoles );
                LOG.debug( "{}.authenticate userId [{}], with default roles[{}], successful", CLS_NM, userId, defaultRoles );
            }
            else
            {
                principal = j2eeMgr.createSession(userId, password);
                LOG.debug( "{}.authenticate userId [{}], successful", CLS_NM, userId );
            }
        }
        catch (SecurityException se)
        {
            LOG.warn( "{}.authenticate userId <{}> caught SecurityException=", CLS_NM, userId, se );
        }
        
        return principal;
    }

    
    /**
     * Determine if given Role is contained within User's Tomcat Principal object.  This method does not need to hit
     * the ldap server as the User's activated Roles are loaded into {@link TcPrincipal#setContext(java.util.HashMap)}
     *
     * @param principal Contains User's Tomcat RBAC Session data that includes activated Roles.
     * @param roleName  Maps to {@code org.apache.directory.fortress.core.model.Role#name}.
     * @return True if Role is found in TcPrincipal, false otherwise.
     */
    public boolean hasRole( Principal principal, String roleName )
    {
        boolean result = false;
        String userId = principal.getName();
        
        try
        {
            if ( j2eeMgr.hasRole( principal, roleName ) )
            {
                LOG.debug( "{}.hasRole userId [{}], role[{}], successful", CLS_NM, principal.getName(), roleName );
                result = true;
            }
            else
            {
                LOG.debug( "{}.hasRole userId [{}], role[{}], failed", CLS_NM, principal.getName(), roleName );
            }
        }
        catch ( SecurityException se )
        {
            LOG.warn( "{}.hasRole userId <{}> role <{}> caught SecurityException= {}", CLS_NM, userId, roleName, se);
        }

        return result;
    }

    /**
     * When the 'defaultRoles' parameter is set on realm proxy config (e.g. in server.xml or context.xml) it will be used to pass into
     * createSession calls into Fortress.  This will scope the roles to be considered for activation to this particular set.
     *
     * @param szDefaultRoles contains a String containing comma delimited roles names.
     */
    public void setDefaultRoles( String szDefaultRoles )
    {
        if( StringUtils.isNotEmpty( szDefaultRoles ) )
        {
            defaultRoles = Arrays.asList( szDefaultRoles.split( "\\s*,\\s*" ) );
            LOG.info( "DEFAULT ROLES: {}", defaultRoles );
        }
    }
}
