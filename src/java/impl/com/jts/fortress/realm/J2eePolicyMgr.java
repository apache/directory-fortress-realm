/*
 * Copyright (c) 2009-2011. Joshua Tree Software, LLC.  All Rights Reserved.
 */

package com.jts.fortress.realm;

import java.util.List;
import java.security.Principal;

import com.jts.fortress.rbac.User;
import com.jts.fortress.rbac.Role;
import com.jts.fortress.SecurityException;
import com.jts.fortress.rbac.Session;
import com.jts.fortress.realm.tomcat.TcPrincipal;

/**
 * This interface is for components that use Websphere and Tomcat Container SPI's to provide
 * Java EE Security capabilities.  These APIs may be called by external programs as needed though the recommended
 * practice is to use Fortress Core APIs like {@link com.jts.fortress.AccessMgr} and {@link com.jts.fortress.ReviewMgr}.
 *
 * @author smckinn
 * @created January 13, 2010
 */
public interface J2eePolicyMgr
{
    /**
     * Perform user authentication and evaluate password policies.
     *
     * @param userId   Contains the userid of the user signing on.
     * @param password Contains the user's password.
     * @return boolean true if succeeds, false otherwise.
     * @throws com.jts.fortress.SecurityException
     *          in the event of data validation failure, security policy violation or DAO error.
     */
    public boolean authenticate(String userId, String password)
        throws SecurityException;


    /**
     * Perform user authentication {@link User#password} and role activations.<br />
     * This method must be called once per user prior to calling other methods within this class.
     * The successful result is {@link com.jts.fortress.rbac.Session} that contains target user's RBAC {@link User#roles} and Admin role {@link User#adminRoles}.<br />
     * In addition to checking user password validity it will apply configured password policy checks {@link com.jts.fortress.rbac.User#pwPolicy}..<br />
     * Method may also store parms passed in for audit trail {@link com.jts.fortress.FortEntity}.
     * <h4> This API will...</h4>
     * <ul>
     * <li> authenticate user password if trusted == false.
     * <li> perform <a href="http://www.openldap.org/">OpenLDAP</a> <a href="http://tools.ietf.org/html/draft-behera-ldap-password-policy-10/">password policy evaluation</a>, see {@link com.jts.fortress.pwpolicy.openldap.OLPWControlImpl#checkPasswordPolicy(com.unboundid.ldap.sdk.migrate.ldapjdk.LDAPConnection, boolean, com.jts.fortress.pwpolicy.PwMessage)}.
     * <li> fail for any user who is locked by OpenLDAP's policies {@link com.jts.fortress.rbac.User#isLocked()}, regardless of trusted flag being set as parm on API.
     * <li> evaluate temporal {@link com.jts.fortress.util.time.Constraint}(s) on {@link User}, {@link com.jts.fortress.rbac.UserRole} and {@link com.jts.fortress.arbac.UserAdminRole} entities.
     * <li> process selective role activations into User RBAC Session {@link User#roles}.
     * <li> check Dynamic Separation of Duties {@link com.jts.fortress.rbac.DSD#validate(com.jts.fortress.rbac.Session, com.jts.fortress.util.time.Constraint, com.jts.fortress.util.time.Time)} on {@link com.jts.fortress.rbac.User#roles}.
     * <li> process selective administrative role activations {@link User#adminRoles}.
     * <li> return a {@link com.jts.fortress.rbac.Session} containing {@link com.jts.fortress.rbac.Session#getUser()}, {@link com.jts.fortress.rbac.Session#getRoles()} and {@link com.jts.fortress.rbac.Session#getAdminRoles()} if everything checks out good.
     * <li> throw a checked exception that will be {@link com.jts.fortress.SecurityException} or its derivation.
     * <li> throw a {@link SecurityException} for system failures.
     * <li> throw a {@link com.jts.fortress.PasswordException} for authentication and password policy violations.
     * <li> throw a {@link com.jts.fortress.ValidationException} for data validation errors.
     * <li> throw a {@link com.jts.fortress.FinderException} if User id not found.
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
     * <li> {@link com.jts.fortress.rbac.User#password}
     * <li> {@link com.jts.fortress.rbac.User#roles} contains a list of RBAC role names authorized for user and targeted for activation within this session.  Default is all authorized RBAC roles will be activated into this Session.
     * <li> {@link com.jts.fortress.rbac.User#adminRoles} contains a list of Admin role names authorized for user and targeted for activation.  Default is all authorized ARBAC roles will be activated into this Session.
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
     * @param userId   maps to {@link com.jts.fortress.rbac.User#userId}.
     * @param password maps to {@link com.jts.fortress.rbac.User#password}.
     * @return TcPrincipal which contains the User's RBAC Session data formatted into a java.security.Principal that is used by Tomcat runtime.
     * @throws com.jts.fortress.SecurityException
     *          in the event of data validation failure, security policy violation or DAO error.
     */
    public TcPrincipal createSession(String userId, String password)
        throws SecurityException;


    /**
     * Perform user authentication {@link User#password} and role activations.<br />
     * This method must be called once per user prior to calling other methods within this class.
     * The successful result is {@link com.jts.fortress.rbac.Session} that contains target user's RBAC {@link User#roles} and Admin role {@link User#adminRoles}.<br />
     * In addition to checking user password validity it will apply configured password policy checks {@link com.jts.fortress.rbac.User#pwPolicy}..<br />
     * Method may also store parms passed in for audit trail {@link com.jts.fortress.FortEntity}.
     * <h4> This API will...</h4>
     * <ul>
     * <li> authenticate user password if trusted == false.
     * <li> perform <a href="http://www.openldap.org/">OpenLDAP</a> <a href="http://tools.ietf.org/html/draft-behera-ldap-password-policy-10/">password policy evaluation</a>, see {@link com.jts.fortress.pwpolicy.openldap.OLPWControlImpl#checkPasswordPolicy(com.unboundid.ldap.sdk.migrate.ldapjdk.LDAPConnection, boolean, com.jts.fortress.pwpolicy.PwMessage)}.
     * <li> fail for any user who is locked by OpenLDAP's policies {@link com.jts.fortress.rbac.User#isLocked()}, regardless of trusted flag being set as parm on API.
     * <li> evaluate temporal {@link com.jts.fortress.util.time.Constraint}(s) on {@link User}, {@link com.jts.fortress.rbac.UserRole} and {@link com.jts.fortress.arbac.UserAdminRole} entities.
     * <li> process selective role activations into User RBAC Session {@link User#roles}.
     * <li> check Dynamic Separation of Duties {@link com.jts.fortress.rbac.DSD#validate(com.jts.fortress.rbac.Session, com.jts.fortress.util.time.Constraint, com.jts.fortress.util.time.Time)} on {@link com.jts.fortress.rbac.User#roles}.
     * <li> process selective administrative role activations {@link User#adminRoles}.
     * <li> return a {@link com.jts.fortress.rbac.Session} containing {@link com.jts.fortress.rbac.Session#getUser()}, {@link com.jts.fortress.rbac.Session#getRoles()} and {@link com.jts.fortress.rbac.Session#getAdminRoles()} if everything checks out good.
     * <li> throw a checked exception that will be {@link com.jts.fortress.SecurityException} or its derivation.
     * <li> throw a {@link SecurityException} for system failures.
     * <li> throw a {@link com.jts.fortress.PasswordException} for authentication and password policy violations.
     * <li> throw a {@link com.jts.fortress.ValidationException} for data validation errors.
     * <li> throw a {@link com.jts.fortress.FinderException} if User id not found.
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
     * <li> {@link com.jts.fortress.rbac.User#password}
     * <li> {@link com.jts.fortress.rbac.User#roles} contains a list of RBAC role names authorized for user and targeted for activation within this session.  Default is all authorized RBAC roles will be activated into this Session.
     * <li> {@link com.jts.fortress.rbac.User#adminRoles} contains a list of Admin role names authorized for user and targeted for activation.  Default is all authorized ARBAC roles will be activated into this Session.
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
     * @param user      Contains {@link User#userId}, {@link com.jts.fortress.rbac.User#password} (optional if {@code isTrusted} is 'true'), optional {@link User#roles}, optional {@link com.jts.fortress.rbac.User#adminRoles}
     * @param isTrusted if true password is not required.
     * @return Session object will contain authentication result code {@link com.jts.fortress.rbac.Session#errorId}, RBAC role activations {@link com.jts.fortress.rbac.Session#getRoles()}, Admin Role activations {@link com.jts.fortress.rbac.Session#getAdminRoles()},OpenLDAP pw policy codes {@link com.jts.fortress.rbac.Session#warningId}, {@link com.jts.fortress.rbac.Session#expirationSeconds}, {@link com.jts.fortress.rbac.Session#graceLogins} and more.
     * @throws com.jts.fortress.SecurityException
     *          in the event of data validation failure, security policy violation or DAO error.
     */
    public Session createSession(User user, boolean isTrusted)
        throws SecurityException;


    /**
     * Determine if given Role is contained within User's Tomcat Principal object.  This method does not need to hit
     * the ldap server as the User's activated Roles are loaded into {@link TcPrincipal#setContext(java.util.HashMap)}
     *
     * @param principal Contains User's Tomcat RBAC Session data that includes activated Roles.
     * @param roleName  Maps to {@link com.jts.fortress.rbac.Role#name}.
     * @return True if Role is found in TcPrincipal, false otherwise.
     * @throws com.jts.fortress.SecurityException
     *          data validation failure or system error..
     */
    public boolean hasRole(Principal principal, String roleName)
        throws SecurityException;


    /**
     * Method reads Role entity from the role container in directory.
     *
     * @param roleName maps to {@link Role#name}, to be read.
     * @return Role entity that corresponds with role name.
     * @throws com.jts.fortress.SecurityException
     *          will be thrown if role not found or system error occurs.
     */
    public Role readRole(String roleName)
        throws SecurityException;


    /**
     * Search for Roles assigned to given User.
     *
     * @param userId Maps to {@link com.jts.fortress.rbac.User#userId}.
     * @param limit  controls the size of ldap result set returned.
     * @return List of type String containing the {@link com.jts.fortress.rbac.Role#name} of all assigned Roles.
     * @throws com.jts.fortress.SecurityException
     *          in the event of data validation failure or DAO error.
     */
    public List<String> searchRoles(String userId, int limit)
        throws SecurityException;


    /**
     * Method returns matching User entity that is contained within the people container in the directory.
     *
     * @param userId maps to {@link User#userId} that matches record in the directory.  userId is globally unique in
     *               people container.
     * @return entity containing matching user data.
     * @throws SecurityException if record not found or system error occurs.
     */
    public User readUser(String userId)
        throws SecurityException;


    /**
     * Return a list of type String of all users in the people container that match the userId field passed in User entity.
     * This method is used by the Websphere realm component.  The max number of returned users may be set by the integer limit arg.
     *
     * @param userId contains all or some leading chars that correspond to users stored in the directory.
     * @param limit  integer value sets the max returned records.
     * @return List of type String containing matching userIds.
     * @throws SecurityException in the event of system error.
     */
    public List<String> searchUsers(String userId, int limit)
        throws SecurityException;


    /**
     * This function returns the set of users assigned to a given role. The function is valid if and
     * only if the role is a member of the ROLES data set.
     * The max number of users returned is constrained by limit argument.
     * This method is used by the Websphere realm component.  This method does NOT use hierarchical rbac.
     *
     * @param roleName maps to {@link Role#name} of Role entity assigned to user.
     * @param limit    integer value sets the max returned records.
     * @return List of type String containing userIds assigned to a particular role.
     * @throws com.jts.fortress.SecurityException
     *          in the event of data validation or system error.
     */
    public List<String> assignedUsers(String roleName, int limit)
        throws SecurityException;


    /**
     * This function returns the set of roles authorized for a given user. The function is valid if
     * and only if the user is a member of the USERS data set.
     *
     * @param userId maps to {@link User#userId} matching User entity stored in the directory.
     * @return Set of type String containing the roles assigned and roles inherited.
     * @throws SecurityException If user not found or system error occurs.
     */
    public List<String> authorizedRoles(String userId)
        throws SecurityException;
}

