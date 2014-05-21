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

package org.openldap.sentry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.security.Principal;
import java.util.Set;

import org.openldap.fortress.GlobalIds;
import org.openldap.fortress.ReviewMgr;
import org.openldap.fortress.ReviewMgrFactory;
import org.openldap.fortress.AccessMgr;
import org.openldap.fortress.AccessMgrFactory;
import org.openldap.fortress.SecurityException;
import org.openldap.fortress.GlobalErrIds;
import org.openldap.fortress.rbac.User;
import org.openldap.fortress.rbac.Role;
import org.openldap.fortress.rbac.Session;
import org.openldap.sentry.tomcat.TcPrincipal;
import org.openldap.fortress.util.attr.VUtil;
import org.openldap.fortress.util.time.CUtil;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * This class is for components that use Websphere and Tomcat Container SPI's to provide
 * Java EE Security capabilities.  These APIs may be called by external programs as needed though the recommended
 * practice is to use Fortress Core APIs like {@link org.openldap.fortress.AccessMgr} and {@link org.openldap.fortress.ReviewMgr}.
 *
 * @author Shawn McKinney
 */
public class J2eePolicyMgrImpl implements J2eePolicyMgr
{
    private static final String CLS_NM = J2eePolicyMgrImpl.class.getName();
    private static final Logger log = Logger.getLogger( CLS_NM );
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
            log.info( J2eePolicyMgrImpl.class.getName() + " - Initialized successfully" );
        }
        catch ( SecurityException se )
        {
            String error = CLS_NM + " caught SecurityException=" + se;
            log.fatal( error );
        }
    }


    /**
     * Perform user authentication and evaluate password policies.
     *
     * @param userId   Contains the userid of the user signing on.
     * @param password Contains the user's password.
     * @return boolean true if succeeds, false otherwise.
     * @throws org.openldap.fortress.SecurityException
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
            if ( log.isEnabledFor( Level.DEBUG ) )
            {
                log.debug( CLS_NM + ".authenticate userId [" + userId + "] successful" );
            }
        }
        else
        {
            if ( log.isEnabledFor( Level.DEBUG ) )
            {
                log.debug( CLS_NM + ".authenticate userId [" + userId + "] failed" );
            }
        }

        return result;
    }


    /**
     * Perform user authentication {@link org.openldap.fortress.rbac.User#password} and role activations.<br />
     * This method must be called once per user prior to calling other methods within this class.
     * The successful result is {@link org.openldap.fortress.rbac.Session} that contains target user's RBAC {@link
     * User#roles} and Admin role {@link User#adminRoles}.<br />
     * In addition to checking user password validity it will apply configured password policy checks {@link org.openldap
     * .fortress.rbac.User#pwPolicy}..<br />
     * Method may also store parms passed in for audit trail {@link org.openldap.fortress.rbac.FortEntity}.
     * <h4> This API will...</h4>
     * <ul>
     * <li> authenticate user password if trusted == false.
     * <li> perform <a href="http://www.openldap.org/">OpenLDAP</a> <a href="http://tools.ietf
     * .org/html/draft-behera-ldap-password-policy-10/">password policy evaluation</a>.
     * <li> fail for any user who is locked by OpenLDAP's policies {@link org.openldap.fortress.rbac.User#isLocked()},
     * regardless of trusted flag being set as parm on API.
     * <li> evaluate temporal {@link org.openldap.fortress.util.time.Constraint}(s) on {@link org.openldap.fortress.rbac.User},
     * {@link org.openldap.fortress.rbac.UserRole} and {@link org.openldap.fortress.rbac.UserAdminRole} entities.
     * <li> process selective role activations into User RBAC Session {@link User#roles}.
     * <li> check Dynamic Separation of Duties {@link org.openldap.fortress.rbac.DSDChecker#validate(org.openldap.fortress.rbac.Session,
     * org.openldap.fortress.util.time.Constraint, org.openldap.fortress.util.time.Time)} on {@link org.openldap.fortress
     * .rbac.User#roles}.
     * <li> process selective administrative role activations {@link User#adminRoles}.
     * <li> return a {@link org.openldap.fortress.rbac.Session} containing {@link org.openldap.fortress.rbac.Session#getUser()},
     * {@link org.openldap.fortress.rbac.Session#getRoles()} and {@link org.openldap.fortress.rbac.Session#getAdminRoles()} if
     * everything checks out good.
     * <li> throw a checked exception that will be {@link org.openldap.fortress.SecurityException} or its derivation.
     * <li> throw a {@link SecurityException} for system failures.
     * <li> throw a {@link org.openldap.fortress.PasswordException} for authentication and password policy violations.
     * <li> throw a {@link org.openldap.fortress.ValidationException} for data validation errors.
     * <li> throw a {@link org.openldap.fortress.FinderException} if User id not found.
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
     * <li> {@link org.openldap.fortress.rbac.User#userId} - required
     * <li> {@link org.openldap.fortress.rbac.User#password}
     * <li> {@link org.openldap.fortress.rbac.User#roles} contains a list of RBAC role names authorized for user and
     * targeted for activation within this session.  Default is all authorized RBAC roles will be activated into this
     * Session.
     * <li> {@link org.openldap.fortress.rbac.User#adminRoles} contains a list of Admin role names authorized for user and
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
     * see {@link org.openldap.fortress.rbac.User#setRole(String)}.
     * </ul>
     * </p>
     *
     * @param userId   maps to {@link org.openldap.fortress.rbac.User#userId}.
     * @param password maps to {@link org.openldap.fortress.rbac.User#password}.
     * @return TcPrincipal which contains the User's RBAC Session data formatted into a java.security.Principal that
     * is used by Tomcat runtime.
     * @throws org.openldap.fortress.SecurityException
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
     * The successful result is {@link org.openldap.fortress.rbac.Session} that contains target user's RBAC {@link User#roles} and Admin role {@link User#adminRoles}.<br />
     * In addition to checking user password validity it will apply configured password policy checks {@link org.openldap.fortress.rbac.User#pwPolicy}..<br />
     * Method may also store parms passed in for audit trail {@link org.openldap.fortress.rbac.FortEntity}.
     * <h4> This API will...</h4>
     * <ul>
     * <li> authenticate user password if trusted == false.
     * <li> perform <a href="http://www.openldap.org/">OpenLDAP</a> <a href="http://tools.ietf.org/html/draft-behera-ldap-password-policy-10/">password policy evaluation</a>.
     * <li> fail for any user who is locked by OpenLDAP's policies {@link org.openldap.fortress.rbac.User#isLocked()}, regardless of trusted flag being set as parm on API.
     * <li> evaluate temporal {@link org.openldap.fortress.util.time.Constraint}(s) on {@link User}, {@link org.openldap.fortress.rbac.UserRole} and {@link org.openldap.fortress.rbac.UserAdminRole} entities.
     * <li> process selective role activations into User RBAC Session {@link User#roles}.
     * <li> check Dynamic Separation of Duties {@link org.openldap.fortress.rbac.DSDChecker#validate(org.openldap.fortress.rbac.Session, org.openldap.fortress.util.time.Constraint, org.openldap.fortress.util.time.Time)} on {@link org.openldap.fortress.rbac.User#roles}.
     * <li> process selective administrative role activations {@link User#adminRoles}.
     * <li> return a {@link org.openldap.fortress.rbac.Session} containing {@link org.openldap.fortress.rbac.Session#getUser()}, {@link org.openldap.fortress.rbac.Session#getRoles()} and {@link org.openldap.fortress.rbac.Session#getAdminRoles()} if everything checks out good.
     * <li> throw a checked exception that will be {@link org.openldap.fortress.SecurityException} or its derivation.
     * <li> throw a {@link SecurityException} for system failures.
     * <li> throw a {@link org.openldap.fortress.PasswordException} for authentication and password policy violations.
     * <li> throw a {@link org.openldap.fortress.ValidationException} for data validation errors.
     * <li> throw a {@link org.openldap.fortress.FinderException} if User id not found.
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
     * <li> {@link org.openldap.fortress.rbac.User#password}
     * <li> {@link org.openldap.fortress.rbac.User#roles} contains a list of RBAC role names authorized for user and targeted for activation within this session.  Default is all authorized RBAC roles will be activated into this Session.
     * <li> {@link org.openldap.fortress.rbac.User#adminRoles} contains a list of Admin role names authorized for user and targeted for activation.  Default is all authorized ARBAC roles will be activated into this Session.
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
     * @param userId   maps to {@link org.openldap.fortress.rbac.User#userId}.
     * @param password maps to {@link org.openldap.fortress.rbac.User#password}.
     * @param roles constains list of role names to activate.
     * @return TcPrincipal which contains the User's RBAC Session data formatted into a java.security.Principal that is used by Tomcat runtime.
     * @throws org.openldap.fortress.SecurityException
     *          in the event of data validation failure, security policy violation or DAO error.
     */
    public TcPrincipal createSession(String userId, char[] password, List<String> roles)
        throws SecurityException
    {
        User user = new User( userId, password );
        // Load the passed in role list into list of User requested roles:
        if(VUtil.isNotNullOrEmpty( roles ))
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
        if ( log.isEnabledFor( Level.DEBUG ) )
        {
            log.debug( CLS_NM + ".createSession userId [" + user.getUserId() + "] successful" );
        }
        HashMap context = new HashMap<String, Session>();
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
     * Perform user authentication {@link org.openldap.fortress.rbac.User#password} and role activations.<br />
     * This method must be called once per user prior to calling other methods within this class.
     * The successful result is {@link org.openldap.fortress.rbac.Session} that contains target user's RBAC {@link
     * User#roles} and Admin role {@link User#adminRoles}.<br />
     * In addition to checking user password validity it will apply configured password policy checks {@link org.openldap
     * .fortress.rbac.User#pwPolicy}..<br />
     * Method may also store parms passed in for audit trail {@link org.openldap.fortress.rbac.FortEntity}.
     * <h4> This API will...</h4>
     * <ul>
     * <li> authenticate user password if trusted == false.
     * <li> perform <a href="http://www.openldap.org/">OpenLDAP</a> <a href="http://tools.ietf
     * .org/html/draft-behera-ldap-password-policy-10/">password policy evaluation</a>.
     * <li> fail for any user who is locked by OpenLDAP's policies {@link org.openldap.fortress.rbac.User#isLocked()},
     * regardless of trusted flag being set as parm on API.
     * <li> evaluate temporal {@link org.openldap.fortress.util.time.Constraint}(s) on {@link org.openldap.fortress.rbac.User},
     * {@link org.openldap.fortress.rbac.UserRole} and {@link org.openldap.fortress.rbac.UserAdminRole} entities.
     * <li> process selective role activations into User RBAC Session {@link User#roles}.
     * <li> check Dynamic Separation of Duties {@link org.openldap.fortress.rbac.DSDChecker#validate(org.openldap.fortress.rbac.Session,
     * org.openldap.fortress.util.time.Constraint, org.openldap.fortress.util.time.Time)} on {@link org.openldap.fortress
     * .rbac.User#roles}.
     * <li> process selective administrative role activations {@link User#adminRoles}.
     * <li> return a {@link org.openldap.fortress.rbac.Session} containing {@link org.openldap.fortress.rbac.Session#getUser()},
     * {@link org.openldap.fortress.rbac.Session#getRoles()} and {@link org.openldap.fortress.rbac.Session#getAdminRoles()} if
     * everything checks out good.
     * <li> throw a checked exception that will be {@link org.openldap.fortress.SecurityException} or its derivation.
     * <li> throw a {@link SecurityException} for system failures.
     * <li> throw a {@link org.openldap.fortress.PasswordException} for authentication and password policy violations.
     * <li> throw a {@link org.openldap.fortress.ValidationException} for data validation errors.
     * <li> throw a {@link org.openldap.fortress.FinderException} if User id not found.
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
     * <li> {@link org.openldap.fortress.rbac.User#userId} - required
     * <li> {@link org.openldap.fortress.rbac.User#password}
     * <li> {@link org.openldap.fortress.rbac.User#roles} contains a list of RBAC role names authorized for user and
     * targeted for activation within this session.  Default is all authorized RBAC roles will be activated into this
     * Session.
     * <li> {@link org.openldap.fortress.rbac.User#adminRoles} contains a list of Admin role names authorized for user and
     * targeted for activation.  Default is all authorized ARBAC roles will be activated into this Session.
     * <li> {@link org.openldap.fortress.rbac.User#props} collection of name value pairs collected on behalf of User during
     * signon.  For example hostname:myservername or ip:192.168.1.99
     * </ul>
     * <h4>
     * Notes:
     * </h4>
     * <ul>
     * <li> roles that violate Dynamic Separation of Duty Relationships will not be activated into session.
     * <li> role activations will proceed in same order as supplied to User entity setter,
     * see {@link org.openldap.fortress.rbac.User#setRole(String)}.
     * </ul>
     * </p>
     *
     * @param user      Contains {@link org.openldap.fortress.rbac.User#userId}, {@link org.openldap.fortress.rbac.User#password}
     *                  (optional if {@code isTrusted} is 'true'), optional {@link org.openldap.fortress.rbac.User#roles},
     *                  optional {@link org.openldap.fortress.rbac.User#adminRoles}
     * @param isTrusted if true password is not required.
     * @return Session object will contain authentication result code {@link org.openldap.fortress.rbac.Session#errorId},
     * RBAC role activations {@link org.openldap.fortress.rbac.Session#getRoles()}, Admin Role activations {@link org.openldap
     * .fortress.rbac.Session#getAdminRoles()},OpenLDAP pw policy codes {@link org.openldap.fortress.rbac
     * .Session#warningId}, {@link org.openldap.fortress.rbac.Session#expirationSeconds},
     * {@link org.openldap.fortress.rbac.Session#graceLogins} and more.
     * @throws org.openldap.fortress.SecurityException
     *          in the event of data validation failure, security policy violation or DAO error.
     */
    @Override
    public Session createSession( User user, boolean isTrusted ) throws SecurityException
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( CLS_NM + ".createSession userId [" + user.getUserId() + "] " );
        }
        return accessMgr.createSession( user, isTrusted );
    }


    /**
     * Determine if given Role is contained within User's Tomcat Principal object.  This method does not need to hit
     * the ldap server as the User's activated Roles are loaded into {@link org.openldap.sentry.tomcat
     * .TcPrincipal#setContext(java.util.HashMap)}
     *
     * @param principal Contains User's Tomcat RBAC Session data that includes activated Roles.
     * @param roleName  Maps to {@link org.openldap.fortress.rbac.Role#name}.
     * @return True if Role is found in TcPrincipal, false otherwise.
     * @throws org.openldap.fortress.SecurityException
     *          data validation failure or system error..
     */
    @Override
    public boolean hasRole( Principal principal, String roleName ) throws SecurityException
    {
        String fullMethodName = CLS_NM + ".hasRole";
        if ( log.isDebugEnabled() )
        {
            log.debug( fullMethodName + " userId [" + principal.getName() + "] role [" + roleName + "]" );
        }

        // Fail closed
        boolean result = false;

        // Principal must contain a HashMap that contains a Fortress session object.
        HashMap<String, Session> context = ( ( TcPrincipal ) principal ).getContext();
        VUtil.assertNotNull( context, GlobalErrIds.SESS_CTXT_NULL, fullMethodName );

        // This Map must contain a Fortress Session:
        Session session = context.get( SESSION );
        VUtil.assertNotNull( session, GlobalErrIds.USER_SESS_NULL, fullMethodName );

        // Check User temporal constraints in the Session:
        CUtil.validateConstraints( session, CUtil.ConstraintType.USER, false );
        // Check Roles temporal constraints; don't check DSD:
        CUtil.validateConstraints( session, CUtil.ConstraintType.ROLE, false );
        // Get the set of authorized roles from the Session:
        Set<String> authZRoles = accessMgr.authorizedRoles( session );
        if ( authZRoles != null && authZRoles.size() > 0 )
        {
            // Does the set of authorized roles contain a name matched to the one passed in?
            if ( authZRoles.contains( roleName ) )
            {
                // Yes, we have a match.
                if ( log.isEnabledFor( Level.DEBUG ) )
                {
                    log.debug( fullMethodName + " userId [" + principal.getName() + "] role [" + roleName + "] " +
                        "successful" );
                }
                result = true;
            }
            else
            {
                if ( log.isEnabledFor( Level.DEBUG ) )
                {
                    // User is not authorized in their Session..
                    log.debug( fullMethodName + " userId [" + principal.getName() + "] is not authorized role [" +
                        roleName + "]" );
                }
            }
        }
        else
        {
            // User does not have any authorized Roles in their Session..
            log.info( fullMethodName + " userId [" + principal.getName() + "], role [" + roleName + "], has no authorized roles" );
        }
        return result;
    }


    /**
     * Method reads Role entity from the role container in directory.
     *
     * @param roleName maps to {@link org.openldap.fortress.rbac.Role#name}, to be read.
     * @return Role entity that corresponds with role name.
     * @throws org.openldap.fortress.SecurityException
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
     * @param searchString Maps to {@link org.openldap.fortress.rbac.User#userId}.
     * @param limit        controls the size of ldap result set returned.
     * @return List of type String containing the {@link org.openldap.fortress.rbac.Role#name} of all assigned Roles.
     * @throws org.openldap.fortress.SecurityException
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
     * @param userId maps to {@link org.openldap.fortress.rbac.User#userId} that matches record in the directory.  userId
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
     * @param roleName maps to {@link org.openldap.fortress.rbac.Role#name} of Role entity assigned to user.
     * @param limit    integer value sets the max returned records.
     * @return List of type String containing userIds assigned to a particular role.
     * @throws org.openldap.fortress.SecurityException
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
     * @param userId maps to {@link org.openldap.fortress.rbac.User#userId} matching User entity stored in the directory.
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
        if ( authZRoleSet != null && authZRoleSet.size() > 0 )
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
        if( obj != null )
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

