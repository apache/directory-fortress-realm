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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.security.Principal;
import java.util.Set;

import org.apache.directory.fortress.core.GlobalIds;
import org.apache.directory.fortress.core.ReviewMgr;
import org.apache.directory.fortress.core.ReviewMgrFactory;
import org.apache.directory.fortress.core.AccessMgr;
import org.apache.directory.fortress.core.AccessMgrFactory;
import org.apache.directory.fortress.core.SecurityException;
import org.apache.directory.fortress.core.GlobalErrIds;
import org.apache.directory.fortress.core.rbac.User;
import org.apache.directory.fortress.core.rbac.Role;
import org.apache.directory.fortress.core.rbac.Session;
import org.apache.directory.fortress.realm.tomcat.TcPrincipal;
import org.apache.directory.fortress.core.util.attr.VUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is for components that use Websphere and Tomcat Container SPI's to provide
 * Java EE Security capabilities.  These APIs may be called by external programs as needed though the recommended
 * practice is to use Fortress Core APIs like {@link org.apache.directory.fortress.core.AccessMgr} and {@link org.apache.directory.fortress.core.ReviewMgr}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class J2eePolicyMgrImpl implements J2eePolicyMgr
{
    private static final String CLS_NM = J2eePolicyMgrImpl.class.getName();
    private static final Logger LOG = LoggerFactory.getLogger( CLS_NM );
    private static AccessMgr accessMgr;
    private static ReviewMgr reviewMgr;
    private static final String SESSION = "session";
    private static int CONTEXT_SERIALIZATION_FAILED = 102;

    static
    {
        try
        {
            accessMgr = AccessMgrFactory.createInstance( GlobalIds.HOME );
            reviewMgr = ReviewMgrFactory.createInstance( GlobalIds.HOME );
            LOG.info( "{} - Initialized successfully", CLS_NM );
        }
        catch ( SecurityException se )
        {
            LOG.error( "{} caught SecurityException={}", CLS_NM, se );
        }
    }


    /**
     * Perform user authentication and evaluate password policies.
     *
     * @param userId   Contains the userid of the user signing on.
     * @param password Contains the user's password.
     * @return boolean true if succeeds, false otherwise.
     * @throws org.apache.directory.fortress.core.SecurityException
     *          in the event of data validation failure, security policy violation or DAO error.
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
     * Perform user authentication {@link org.apache.directory.fortress.core.rbac.User#password} and role activations.<br />
     * This method must be called once per user prior to calling other methods within this class.
     * The successful result is {@link org.apache.directory.fortress.core.rbac.Session} that contains target user's RBAC {@link
     * User#roles} and Admin role {@link User#adminRoles}.<br />
     * In addition to checking user password validity it will apply configured password policy checks {@link org.openldap
     * .fortress.rbac.User#pwPolicy}..<br />
     * Method may also store parms passed in for audit trail {@link org.apache.directory.fortress.core.rbac.FortEntity}.
     * <h4> This API will...</h4>
     * <ul>
     * <li> authenticate user password if trusted == false.
     * <li> perform <a href="http://www.openldap.org/">OpenLDAP</a> <a href="http://tools.ietf
     * .org/html/draft-behera-ldap-password-policy-10/">password policy evaluation</a>.
     * <li> fail for any user who is locked by OpenLDAP's policies {@link org.apache.directory.fortress.core.rbac.User#isLocked()},
     * regardless of trusted flag being set as parm on API.
     * <li> evaluate temporal {@link org.apache.directory.fortress.core.util.time.Constraint}(s) on {@link org.apache.directory.fortress.core.rbac.User},
     * {@link org.apache.directory.fortress.core.rbac.UserRole} and {@link org.apache.directory.fortress.core.rbac.UserAdminRole} entities.
     * <li> process selective role activations into User RBAC Session {@link User#roles}.
     * <li> check Dynamic Separation of Duties {@link org.apache.directory.fortress.core.rbac.DSDChecker#validate(org.apache.directory.fortress.core.rbac.Session,
     * org.apache.directory.fortress.core.util.time.Constraint, org.apache.directory.fortress.core.util.time.Time)} on {@link org.apache.directory.fortress.core
     * .rbac.User#roles}.
     * <li> process selective administrative role activations {@link User#adminRoles}.
     * <li> return a {@link org.apache.directory.fortress.core.rbac.Session} containing {@link org.apache.directory.fortress.core.rbac.Session#getUser()},
     * {@link org.apache.directory.fortress.core.rbac.Session#getRoles()} and {@link org.apache.directory.fortress.core.rbac.Session#getAdminRoles()} if
     * everything checks out good.
     * <li> throw a checked exception that will be {@link org.apache.directory.fortress.core.SecurityException} or its derivation.
     * <li> throw a {@link SecurityException} for system failures.
     * <li> throw a {@link org.apache.directory.fortress.core.PasswordException} for authentication and password policy violations.
     * <li> throw a {@link org.apache.directory.fortress.core.ValidationException} for data validation errors.
     * <li> throw a {@link org.apache.directory.fortress.core.FinderException} if User id not found.
     * </ul>
     * <h4>
     * The function is valid if and only if:
     * </h4>
     * <ul>
     * <li> the user is a member of the USERS data set
     * <li> the password is supplied (unless trusted).
     * <li> the (optional) active role set is a subset of the roles authorized for that user.
     * </ul>
     * <h4>
     * The following attributes may be set when calling this method
     * </h4>
     * <ul>
     * <li> {@link org.apache.directory.fortress.core.rbac.User#userId} - required
     * <li> {@link org.apache.directory.fortress.core.rbac.User#password}
     * <li> {@link org.apache.directory.fortress.core.rbac.User#roles} contains a list of RBAC role names authorized for user and
     * targeted for activation within this session.  Default is all authorized RBAC roles will be activated into this
     * Session.
     * <li> {@link org.apache.directory.fortress.core.rbac.User#adminRoles} contains a list of Admin role names authorized for user and
     * targeted for activation.  Default is all authorized ARBAC roles will be activated into this Session.
     * <li> {@link User#props} collection of name value pairs collected on behalf of User during signon.  For example
     * hostname:myservername or ip:192.168.1.99
     * </ul>
     * <h4>
     * Notes:
     * </h4>
     * <ul>
     * <li> roles that violate Dynamic Separation of Duty Relationships will not be activated into session.
     * <li> role activations will proceed in same order as supplied to User entity setter,
     * see {@link org.apache.directory.fortress.core.rbac.User#setRole(String)}.
     * </ul>
     * </p>
     *
     * @param userId   maps to {@link org.apache.directory.fortress.core.rbac.User#userId}.
     * @param password maps to {@link org.apache.directory.fortress.core.rbac.User#password}.
     * @return TcPrincipal which contains the User's RBAC Session data formatted into a java.security.Principal that
     * is used by Tomcat runtime.
     * @throws org.apache.directory.fortress.core.SecurityException
     *          in the event of data validation failure, security policy violation or DAO error.
     */
    @Override
    public TcPrincipal createSession( String userId, char[] password ) throws SecurityException
    {
        User user = new User( userId, password );
        
        return createSession( user );
    }


    /**
     * Perform user authentication {@link User#password} and role activations.<br />
     * This method must be called once per user prior to calling other methods within this class.
     * The successful result is {@link org.apache.directory.fortress.core.rbac.Session} that contains target user's RBAC {@link User#roles} and Admin role {@link User#adminRoles}.<br />
     * In addition to checking user password validity it will apply configured password policy checks {@link org.apache.directory.fortress.core.rbac.User#pwPolicy}..<br />
     * Method may also store parms passed in for audit trail {@link org.apache.directory.fortress.core.rbac.FortEntity}.
     * <h4> This API will...</h4>
     * <ul>
     * <li> authenticate user password if trusted == false.
     * <li> perform <a href="http://www.openldap.org/">OpenLDAP</a> <a href="http://tools.ietf.org/html/draft-behera-ldap-password-policy-10/">password policy evaluation</a>.
     * <li> fail for any user who is locked by OpenLDAP's policies {@link org.apache.directory.fortress.core.rbac.User#isLocked()}, regardless of trusted flag being set as parm on API.
     * <li> evaluate temporal {@link org.apache.directory.fortress.core.util.time.Constraint}(s) on {@link User}, {@link org.apache.directory.fortress.core.rbac.UserRole} and {@link org.apache.directory.fortress.core.rbac.UserAdminRole} entities.
     * <li> process selective role activations into User RBAC Session {@link User#roles}.
     * <li> check Dynamic Separation of Duties {@link org.apache.directory.fortress.core.rbac.DSDChecker#validate(org.apache.directory.fortress.core.rbac.Session, org.apache.directory.fortress.core.util.time.Constraint, org.apache.directory.fortress.core.util.time.Time)} on {@link org.apache.directory.fortress.core.rbac.User#roles}.
     * <li> process selective administrative role activations {@link User#adminRoles}.
     * <li> return a {@link org.apache.directory.fortress.core.rbac.Session} containing {@link org.apache.directory.fortress.core.rbac.Session#getUser()}, {@link org.apache.directory.fortress.core.rbac.Session#getRoles()} and {@link org.apache.directory.fortress.core.rbac.Session#getAdminRoles()} if everything checks out good.
     * <li> throw a checked exception that will be {@link org.apache.directory.fortress.core.SecurityException} or its derivation.
     * <li> throw a {@link SecurityException} for system failures.
     * <li> throw a {@link org.apache.directory.fortress.core.PasswordException} for authentication and password policy violations.
     * <li> throw a {@link org.apache.directory.fortress.core.ValidationException} for data validation errors.
     * <li> throw a {@link org.apache.directory.fortress.core.FinderException} if User id not found.
     * </ul>
     * <h4>
     * The function is valid if and only if:
     * </h4>
     * <ul>
     * <li> the user is a member of the USERS data set
     * <li> the password is supplied (unless trusted).
     * <li> the (optional) active role set is a subset of the roles authorized for that user.
     * </ul>
     * <h4>
     * The following attributes may be set when calling this method
     * </h4>
     * <ul>
     * <li> {@link User#userId} - required
     * <li> {@link org.apache.directory.fortress.core.rbac.User#password}
     * <li> {@link org.apache.directory.fortress.core.rbac.User#roles} contains a list of RBAC role names authorized for user and targeted for activation within this session.  Default is all authorized RBAC roles will be activated into this Session.
     * <li> {@link org.apache.directory.fortress.core.rbac.User#adminRoles} contains a list of Admin role names authorized for user and targeted for activation.  Default is all authorized ARBAC roles will be activated into this Session.
     * <li> {@link User#props} collection of name value pairs collected on behalf of User during signon.  For example hostname:myservername or ip:192.168.1.99
     * </ul>
     * <h4>
     * Notes:
     * </h4>
     * <ul>
     * <li> roles that violate Dynamic Separation of Duty Relationships will not be activated into session.
     * <li> role activations will proceed in same order as supplied to User entity setter, see {@link User#setRole(String)}.
     * </ul>
     * </p>
     *
     * @param userId   maps to {@link org.apache.directory.fortress.core.rbac.User#userId}.
     * @param password maps to {@link org.apache.directory.fortress.core.rbac.User#password}.
     * @param roles constains list of role names to activate.
     * @return TcPrincipal which contains the User's RBAC Session data formatted into a java.security.Principal that is used by Tomcat runtime.
     * @throws org.apache.directory.fortress.core.SecurityException
     *          in the event of data validation failure, security policy violation or DAO error.
     */
    public TcPrincipal createSession( String userId, char[] password, List<String> roles ) throws SecurityException
    {
        User user = new User( userId, password );
        
        // Load the passed in role list into list of User requested roles:
        if ( VUtil.isNotNullOrEmpty( roles ) )
        {
            for(String role : roles)
            {
                user.setRole( role );
            }
        }
        
        return createSession( user );
    }


    /**
     * Utility function to call Fortress createSession, build the principal on behalf of caller.
     *
     * @param user
     * @return
     * @throws SecurityException
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
     * Perform user authentication {@link org.apache.directory.fortress.core.rbac.User#password} and role activations.<br />
     * This method must be called once per user prior to calling other methods within this class.
     * The successful result is {@link org.apache.directory.fortress.core.rbac.Session} that contains target user's RBAC {@link
     * User#roles} and Admin role {@link User#adminRoles}.<br />
     * In addition to checking user password validity it will apply configured password policy checks {@link org.openldap
     * .fortress.rbac.User#pwPolicy}..<br />
     * Method may also store parms passed in for audit trail {@link org.apache.directory.fortress.core.rbac.FortEntity}.
     * <h4> This API will...</h4>
     * <ul>
     * <li> authenticate user password if trusted == false.
     * <li> perform <a href="http://www.openldap.org/">OpenLDAP</a> <a href="http://tools.ietf
     * .org/html/draft-behera-ldap-password-policy-10/">password policy evaluation</a>.
     * <li> fail for any user who is locked by OpenLDAP's policies {@link org.apache.directory.fortress.core.rbac.User#isLocked()},
     * regardless of trusted flag being set as parm on API.
     * <li> evaluate temporal {@link org.apache.directory.fortress.core.util.time.Constraint}(s) on {@link org.apache.directory.fortress.core.rbac.User},
     * {@link org.apache.directory.fortress.core.rbac.UserRole} and {@link org.apache.directory.fortress.core.rbac.UserAdminRole} entities.
     * <li> process selective role activations into User RBAC Session {@link User#roles}.
     * <li> check Dynamic Separation of Duties {@link org.apache.directory.fortress.core.rbac.DSDChecker#validate(org.apache.directory.fortress.core.rbac.Session,
     * org.apache.directory.fortress.core.util.time.Constraint, org.apache.directory.fortress.core.util.time.Time)} on {@link org.apache.directory.fortress.core
     * .rbac.User#roles}.
     * <li> process selective administrative role activations {@link User#adminRoles}.
     * <li> return a {@link org.apache.directory.fortress.core.rbac.Session} containing {@link org.apache.directory.fortress.core.rbac.Session#getUser()},
     * {@link org.apache.directory.fortress.core.rbac.Session#getRoles()} and {@link org.apache.directory.fortress.core.rbac.Session#getAdminRoles()} if
     * everything checks out good.
     * <li> throw a checked exception that will be {@link org.apache.directory.fortress.core.SecurityException} or its derivation.
     * <li> throw a {@link SecurityException} for system failures.
     * <li> throw a {@link org.apache.directory.fortress.core.PasswordException} for authentication and password policy violations.
     * <li> throw a {@link org.apache.directory.fortress.core.ValidationException} for data validation errors.
     * <li> throw a {@link org.apache.directory.fortress.core.FinderException} if User id not found.
     * </ul>
     * <h4>
     * The function is valid if and only if:
     * </h4>
     * <ul>
     * <li> the user is a member of the USERS data set
     * <li> the password is supplied (unless trusted).
     * <li> the (optional) active role set is a subset of the roles authorized for that user.
     * </ul>
     * <h4>
     * The following attributes may be set when calling this method
     * </h4>
     * <ul>
     * <li> {@link org.apache.directory.fortress.core.rbac.User#userId} - required
     * <li> {@link org.apache.directory.fortress.core.rbac.User#password}
     * <li> {@link org.apache.directory.fortress.core.rbac.User#roles} contains a list of RBAC role names authorized for user and
     * targeted for activation within this session.  Default is all authorized RBAC roles will be activated into this
     * Session.
     * <li> {@link org.apache.directory.fortress.core.rbac.User#adminRoles} contains a list of Admin role names authorized for user and
     * targeted for activation.  Default is all authorized ARBAC roles will be activated into this Session.
     * <li> {@link org.apache.directory.fortress.core.rbac.User#props} collection of name value pairs collected on behalf of User during
     * signon.  For example hostname:myservername or ip:192.168.1.99
     * </ul>
     * <h4>
     * Notes:
     * </h4>
     * <ul>
     * <li> roles that violate Dynamic Separation of Duty Relationships will not be activated into session.
     * <li> role activations will proceed in same order as supplied to User entity setter,
     * see {@link org.apache.directory.fortress.core.rbac.User#setRole(String)}.
     * </ul>
     * </p>
     *
     * @param user      Contains {@link org.apache.directory.fortress.core.rbac.User#userId}, {@link org.apache.directory.fortress.core.rbac.User#password}
     *                  (optional if {@code isTrusted} is 'true'), optional {@link org.apache.directory.fortress.core.rbac.User#roles},
     *                  optional {@link org.apache.directory.fortress.core.rbac.User#adminRoles}
     * @param isTrusted if true password is not required.
     * @return Session object will contain authentication result code {@link org.apache.directory.fortress.core.rbac.Session#errorId},
     * RBAC role activations {@link org.apache.directory.fortress.core.rbac.Session#getRoles()}, Admin Role activations {@link org.openldap
     * .fortress.rbac.Session#getAdminRoles()},OpenLDAP pw policy codes {@link org.apache.directory.fortress.core.rbac
     * .Session#warningId}, {@link org.apache.directory.fortress.core.rbac.Session#expirationSeconds},
     * {@link org.apache.directory.fortress.core.rbac.Session#graceLogins} and more.
     * @throws org.apache.directory.fortress.core.SecurityException
     *          in the event of data validation failure, security policy violation or DAO error.
     */
    @Override
    public Session createSession( User user, boolean isTrusted ) throws SecurityException
    {
        LOG.debug( "{}.createSession userId [{}], isTrusted [{}]", CLS_NM, user.getUserId(), isTrusted );
        
        return accessMgr.createSession( user, isTrusted );
    }


    /**
     * Determine if given Role is contained within User's Tomcat Principal object.  This method does not need to hit
     * the ldap server as the User's activated Roles are loaded into {@link org.apache.directory.fortress.realm.tomcat
     * .TcPrincipal#setContext(java.util.HashMap)}
     *
     * @param principal Contains User's Tomcat RBAC Session data that includes activated Roles.
     * @param roleName  Maps to {@link org.apache.directory.fortress.core.rbac.Role#name}.
     * @return True if Role is found in TcPrincipal, false otherwise.
     * @throws org.apache.directory.fortress.core.SecurityException
     *          data validation failure or system error..
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
     * Method reads Role entity from the role container in directory.
     *
     * @param roleName maps to {@link org.apache.directory.fortress.core.rbac.Role#name}, to be read.
     * @return Role entity that corresponds with role name.
     * @throws org.apache.directory.fortress.core.SecurityException
     *          will be thrown if role not found or system error occurs.
     */
    @Override
    public Role readRole( String roleName ) throws SecurityException
    {
        return reviewMgr.readRole( new Role( roleName ) );
    }


    /**
     * Search for Roles assigned to given User.
     *
     * @param searchString Maps to {@link org.apache.directory.fortress.core.rbac.User#userId}.
     * @param limit        controls the size of ldap result set returned.
     * @return List of type String containing the {@link org.apache.directory.fortress.core.rbac.Role#name} of all assigned Roles.
     * @throws org.apache.directory.fortress.core.SecurityException
     *          in the event of data validation failure or DAO error.
     */
    @Override
    public List<String> searchRoles( String searchString, int limit ) throws SecurityException
    {
        return reviewMgr.findRoles( searchString, limit );
    }


    /**
     * Method returns matching User entity that is contained within the people container in the directory.
     *
     * @param userId maps to {@link org.apache.directory.fortress.core.rbac.User#userId} that matches record in the directory.  userId
     *               is globally unique in
     *               people container.
     * @return entity containing matching user data.
     * @throws SecurityException if record not found or system error occurs.
     */
    @Override
    public User readUser( String userId ) throws SecurityException
    {
        return reviewMgr.readUser( new User( userId ) );
    }


    /**
     * Return a list of type String of all users in the people container that match the userId field passed in User
     * entity.
     * This method is used by the Websphere sentry component.  The max number of returned users may be set by the
     * integer limit arg.
     *
     * @param searchString contains all or some leading chars that correspond to users stored in the directory.
     * @param limit        integer value sets the max returned records.
     * @return List of type String containing matching userIds.
     * @throws SecurityException in the event of system error.
     */
    @Override
    public List<String> searchUsers( String searchString, int limit ) throws SecurityException
    {
        return reviewMgr.findUsers( new User( searchString ), limit );
    }


    /**
     * This function returns the set of users assigned to a given role. The function is valid if and
     * only if the role is a member of the ROLES data set.
     * The max number of users returned is constrained by limit argument.
     * This method is used by the Websphere sentry component.  This method does NOT use hierarchical rbac.
     *
     * @param roleName maps to {@link org.apache.directory.fortress.core.rbac.Role#name} of Role entity assigned to user.
     * @param limit    integer value sets the max returned records.
     * @return List of type String containing userIds assigned to a particular role.
     * @throws org.apache.directory.fortress.core.SecurityException
     *          in the event of data validation or system error.
     */
    @Override
    public List<String> assignedUsers( String roleName, int limit ) throws SecurityException
    {
        return reviewMgr.assignedUsers( new Role( roleName ), limit );
    }


    /**
     * This function returns the set of roles authorized for a given user. The function is valid if
     * and only if the user is a member of the USERS data set.
     *
     * @param userId maps to {@link org.apache.directory.fortress.core.rbac.User#userId} matching User entity stored in the directory.
     * @return Set of type String containing the roles assigned and roles inherited.
     * @throws SecurityException If user not found or system error occurs.
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
                throw new SecurityException(CONTEXT_SERIALIZATION_FAILED, error);
            }
        }
        
        return szRetVal;
    }
}
