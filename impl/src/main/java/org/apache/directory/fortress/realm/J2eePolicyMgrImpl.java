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
package org.apache.directory.fortress.realm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.security.Principal;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.directory.fortress.core.GlobalIds;
import org.apache.directory.fortress.core.ReviewMgr;
import org.apache.directory.fortress.core.ReviewMgrFactory;
import org.apache.directory.fortress.core.AccessMgr;
import org.apache.directory.fortress.core.AccessMgrFactory;
import org.apache.directory.fortress.core.SecurityException;
import org.apache.directory.fortress.core.GlobalErrIds;
import org.apache.directory.fortress.core.model.User;
import org.apache.directory.fortress.core.model.Role;
import org.apache.directory.fortress.core.model.Session;
import org.apache.directory.fortress.core.util.VUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains common functions for container managed security.  These APIs may be called by external programs as needed though the expected
 * practice for external app usage  is to call Apache Fortress Core APIs, e.g. {@link org.apache.directory.fortress.core.AccessMgr} and {@link org.apache.directory.fortress.core.ReviewMgr}.
 * This class is NOT thread safe if contextId is set.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class J2eePolicyMgrImpl implements J2eePolicyMgr
{
    private static final String CLS_NM = J2eePolicyMgrImpl.class.getName();
    private static final Logger LOG = LoggerFactory.getLogger( CLS_NM );
    private AccessMgr accessMgr;
    private ReviewMgr reviewMgr;
    private static final String SESSION = "session";
    private String contextId;

    J2eePolicyMgrImpl ()
    {
        try
        {
            accessMgr = AccessMgrFactory.createInstance( );
            reviewMgr = ReviewMgrFactory.createInstance( );
            LOG.info( "{} - constructed", CLS_NM );
        }
        catch ( SecurityException se )
        {
            LOG.error( "{} caught SecurityException={}", CLS_NM, se );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean authenticate( String userId, char[] password ) throws SecurityException
    {
        boolean result = false;
        Session session = accessMgr.authenticate( userId, password );
        
        if ( session != null )
        {
            result = true;
            LOG.debug( "{}.authenticate userId [{}], successful", CLS_NM, userId );
        }
        else
        {
            LOG.debug( "{}.authenticate userId [{}], failed", CLS_NM, userId );
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TcPrincipal createSession( String userId, char[] password ) throws SecurityException
    {
        User user = new User( userId, new String ( password ) );
        
        return createSession( user );
    }

    /**
     * {@inheritDoc}
     */
    public TcPrincipal createSession( String userId, char[] password, List<String> roles ) throws SecurityException
    {
        User user = new User( userId, new String ( password ) );
        
        // Load the passed in role list into list of User requested roles:
        if ( CollectionUtils.isNotEmpty( roles ) )
        {
            for(String role : roles)
            {
                user.setRoleName( role );
            }
        }
        
        return createSession( user );
    }

    /**
     * {@inheritDoc}
     */
    private TcPrincipal createSession( User user ) throws SecurityException
    {
        Session session = accessMgr.createSession( user, false );
        LOG.debug( "{}.createSession userId [{}], successful", CLS_NM, user.getUserId() );
        HashMap<String, Object> context = new HashMap<String, Object>();
        context.put( SESSION, session );

        // now serialize the principal:
        String ser = serialize( session );

        // Store the serialized principal inside the context hashmap
        // which allows overriden toString to return it later, from within an application thread.
        // This facilitates assertion of rbac session from the tomcat realm into the web application session.
        context.put( TcPrincipal.SERIALIZED, ser );
        
        return new TcPrincipal( user.getUserId(), context );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Session createSession( User user, boolean isTrusted ) throws SecurityException
    {
        LOG.debug( "{}.createSession userId [{}], isTrusted [{}]", CLS_NM, user.getUserId(), isTrusted );
        
        return accessMgr.createSession( user, isTrusted );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasRole( Principal principal, String roleName ) throws SecurityException
    {
        String fullMethodName = CLS_NM + ".hasRole";
        LOG.debug( "{}.hasRole userId [{}], role [{}]", CLS_NM, principal.getName(), roleName );

        // Fail closed
        boolean result = false;

        // Principal must contain a HashMap that contains a Fortress session object.
        HashMap<String, Object> context = ( ( TcPrincipal ) principal ).getContext();
        VUtil.assertNotNull( context, GlobalErrIds.SESS_CTXT_NULL, fullMethodName );

        // This Map must contain a Fortress Session:
        Session session = (Session)context.get( SESSION );
        VUtil.assertNotNull( session, GlobalErrIds.USER_SESS_NULL, fullMethodName );

        Set<String> authZRoles = accessMgr.authorizedRoles( session );
        
        if ( ( authZRoles != null ) && ( authZRoles.size() > 0 ) )
        {
            // Does the set of authorized roles contain a name matched to the one passed in?
            if ( authZRoles.contains( roleName ) )
            {
                // Yes, we have a match.
                LOG.debug( "{} userId [{}], role [{}], successful", fullMethodName, principal.getName(), roleName );
                result = true;
            }
            else
            {
                // User is not authorized in their Session..
                LOG.debug( "{} userId [{}], is not authorized role [{}]", fullMethodName, principal.getName(), roleName );
            }
        }
        else
        {
            // User does not have any authorized Roles in their Session..
            LOG.info( "{} userId [{}], role [{}], has no authorized roles", fullMethodName, principal.getName(), roleName );
        }
        
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Role readRole( String roleName ) throws SecurityException
    {
        return reviewMgr.readRole( new Role( roleName ) );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> searchRoles(String searchString, int limit ) throws SecurityException
    {
        return reviewMgr.findRoles( searchString, limit );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public User readUser( String userId ) throws SecurityException
    {
        return reviewMgr.readUser( new User( userId ) );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> searchUsers(String searchString, int limit ) throws SecurityException
    {
        return reviewMgr.findUsers( new User( searchString ), limit );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> assignedUsers( String roleName, int limit ) throws SecurityException
    {
        return reviewMgr.assignedUsers( new Role( roleName ), limit );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> authorizedRoles( String userId ) throws SecurityException
    {
        List<String> list = null;
        // This will check temporal constraints on User and Roles.
        Session session = createSession( new User( userId ), true );
        // Get the Set of authorized Roles.
        Set<String> authZRoleSet = accessMgr.authorizedRoles( session );
        
        // If User has authorized roles.
        if ( ( authZRoleSet != null ) && ( authZRoleSet.size() > 0 ) )
        {
            // Convert the Set into a List before returning:
            list = new ArrayList<String>( authZRoleSet );
        }
        
        return list;
    }

    /**
     * Utility to write any object into a Base64 string.  Used by this class to serialize {@link TcPrincipal} object to be returned by its toString method..
     */
    private String serialize( Object obj ) throws SecurityException
    {
        String szRetVal = null;
        
        if ( obj != null )
        {
            try
            {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                ObjectOutputStream so = new ObjectOutputStream( bo );
                so.writeObject( obj );
                so.flush();
                
                // This encoding induces a bijection between byte[] and String (unlike UTF-8)
                szRetVal = bo.toString( "ISO-8859-1" );
            }
            catch ( IOException ioe )
            {
                String error = "serialize caught IOException: " + ioe;
                throw new SecurityException( org.apache.directory.fortress.realm.GlobalIds.CONTEXT_SERIALIZATION_FAILED, error, ioe );
            }
        }
        
        return szRetVal;
    }

    /**
     * {@inheritDoc}
     */
    public Session deserialize( String str ) throws SecurityException
    {
        // deserialize the object
        try
        {
            // This encoding induces a bijection between byte[] and String (unlike UTF-8)
            byte b[] = str.getBytes("ISO-8859-1");
            ByteArrayInputStream bi = new ByteArrayInputStream(b);
            ObjectInputStream si = new ObjectInputStream(bi);
            return Session.class.cast(si.readObject());
        }
        catch (java.io.UnsupportedEncodingException e)
        {
            throw new SecurityException( org.apache.directory.fortress.realm.GlobalIds.CONTEXT_DESERIALIZATION_FAILED_UNSUPPORTED_ENCODING, "deserialize caught UnsupportedEncodingException:" + e, e );
        }
        catch (IOException e)
        {
            LOG.warn( "deserialize caught IOException:" + e);
            throw new SecurityException( org.apache.directory.fortress.realm.GlobalIds.CONTEXT_DESERIALIZATION_FAILED_IO, "deserialize caught IOException:" + e, e );
        }
        catch (ClassNotFoundException e)
        {
            LOG.warn( "deserialize caught ClassNotFoundException:" + e);
            throw new SecurityException( org.apache.directory.fortress.realm.GlobalIds.CONTEXT_DESERIALIZATION_FAILED_CLASS_NOT_FOUND, "deserialize caught ClassNotFoundException:" + e, e );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setContextId(String contextId)
    {
        this.contextId = contextId;
        accessMgr.setContextId( contextId );
        reviewMgr.setContextId( contextId );
    }
}