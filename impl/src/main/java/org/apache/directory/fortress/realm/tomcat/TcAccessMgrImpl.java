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
    private String contextId;

    /**
     * Constructor for the TcAccessMgrImpl object
     */
    public TcAccessMgrImpl()
    {
        try
        {
            j2eeMgr = J2eePolicyMgrFactory.createInstance( );
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
     * {@inheritDoc}
     */
    @Override
    public Principal authenticate( String userId, char[] password )
    {
        TcPrincipal principal = null;

        System.out.println("BREAK1");
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
     * {@inheritDoc}
     */
    @Override
    public boolean hasRole( Principal principal, String roleName )
    {
        boolean result = false;
        String userId = principal.getName();

        System.out.println("BREAK2 user:" + userId + ", role" + roleName);
        try
        {
            if ( j2eeMgr.hasRole( principal, roleName ) )
            {
                System.out.println("BREAK3 user:" + userId + ", role" + roleName);

                LOG.debug( "{}.hasRole userId [{}], role[{}], successful", CLS_NM, principal.getName(), roleName );
                result = true;
            }
            else
            {
                System.out.println("BREAK4 user:" + userId + ", role" + roleName);
                LOG.debug( "{}.hasRole userId [{}], role[{}], failed", CLS_NM, principal.getName(), roleName );
            }
        }
        catch ( SecurityException se )
        {
            LOG.warn( "{}.hasRole userId <{}> role <{}> caught SecurityException= {}", CLS_NM, userId, roleName, se);
        }

        System.out.println("BREAK5 user:" + userId + ", role" + roleName);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDefaultRoles( String szDefaultRoles )
    {
        if( StringUtils.isNotEmpty( szDefaultRoles ) )
        {
            defaultRoles = Arrays.asList( szDefaultRoles.split( "\\s*,\\s*" ) );
            LOG.info( "DEFAULT ROLES: {}", defaultRoles );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setContextId( String contextId )
    {
        this.contextId = contextId;
        j2eeMgr.setContextId( contextId );
        LOG.info( "CONTEXT ID: {}", contextId );
    }
}