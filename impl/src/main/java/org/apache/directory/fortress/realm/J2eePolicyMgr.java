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

import java.util.List;
import java.security.Principal;

import org.apache.directory.fortress.core.rbac.User;
import org.apache.directory.fortress.core.rbac.Role;
import org.apache.directory.fortress.core.SecurityException;
import org.apache.directory.fortress.core.rbac.Session;
import org.apache.directory.fortress.realm.tomcat.TcPrincipal;

/**
 * This interface is for components that use Websphere and Tomcat Container SPI's to provide
 * Java EE Security capabilities.  These APIs may be called by external programs as needed though the recommended
 * practice is to use Fortress Core APIs like {@link org.apache.directory.fortress.core.AccessMgr} and {@link org.apache.directory.fortress.core.ReviewMgr}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface J2eePolicyMgr
{
    /**
     * Perform user authentication and evaluate password policies.
     *
     * @param userId   Contains the userid of the user signing on.
     * @param password Contains the user's password.
     * @return boolean true if succeeds, false otherwise.
     * @throws org.apache.directory.fortress.core.SecurityException
     *          in the event of data validation failure, security policy violation or DAO error.
     */
    public boolean authenticate( String userId, char[] password ) throws SecurityException;


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
     * @return TcPrincipal which contains the User's RBAC Session data formatted into a java.security.Principal that is used by Tomcat runtime.
     * @throws org.apache.directory.fortress.core.SecurityException
     *          in the event of data validation failure, security policy violation or DAO error.
     */
    public TcPrincipal createSession( String userId, char[] password ) throws SecurityException;


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
    public TcPrincipal createSession( String userId, char[] password, List<String> roles ) throws SecurityException;


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
     * @param user      Contains {@link User#userId}, {@link org.apache.directory.fortress.core.rbac.User#password} (optional if {@code isTrusted} is 'true'), optional {@link User#roles}, optional {@link org.apache.directory.fortress.core.rbac.User#adminRoles}
     * @param isTrusted if true password is not required.
     * @return Session object will contain authentication result code {@link org.apache.directory.fortress.core.rbac.Session#errorId}, RBAC role activations {@link org.apache.directory.fortress.core.rbac.Session#getRoles()}, Admin Role activations {@link org.apache.directory.fortress.core.rbac.Session#getAdminRoles()},OpenLDAP pw policy codes {@link org.apache.directory.fortress.core.rbac.Session#warningId}, {@link org.apache.directory.fortress.core.rbac.Session#expirationSeconds}, {@link org.apache.directory.fortress.core.rbac.Session#graceLogins} and more.
     * @throws org.apache.directory.fortress.core.SecurityException
     *          in the event of data validation failure, security policy violation or DAO error.
     */
    public Session createSession( User user, boolean isTrusted ) throws SecurityException;


    /**
     * Determine if given Role is contained within User's Tomcat Principal object.  This method does not need to hit
     * the ldap server as the User's activated Roles are loaded into {@link org.apache.directory.fortress.realm.tomcat.TcPrincipal#setContext(java.util.HashMap)}
     *
     * @param principal Contains User's Tomcat RBAC Session data that includes activated Roles.
     * @param roleName  Maps to {@link org.apache.directory.fortress.core.rbac.Role#name}.
     * @return True if Role is found in TcPrincipal, false otherwise.
     * @throws org.apache.directory.fortress.core.SecurityException
     *          data validation failure or system error..
     */
    public boolean hasRole( Principal principal, String roleName ) throws SecurityException;


    /**
     * Method reads Role entity from the role container in directory.
     *
     * @param roleName maps to {@link Role#name}, to be read.
     * @return Role entity that corresponds with role name.
     * @throws org.apache.directory.fortress.core.SecurityException
     *          will be thrown if role not found or system error occurs.
     */
    public Role readRole( String roleName ) throws SecurityException;


    /**
     * Search for Roles assigned to given User.
     *
     * @param userId Maps to {@link org.apache.directory.fortress.core.rbac.User#userId}.
     * @param limit  controls the size of ldap result set returned.
     * @return List of type String containing the {@link org.apache.directory.fortress.core.rbac.Role#name} of all assigned Roles.
     * @throws org.apache.directory.fortress.core.SecurityException
     *          in the event of data validation failure or DAO error.
     */
    public List<String> searchRoles( String userId, int limit ) throws SecurityException;


    /**
     * Method returns matching User entity that is contained within the people container in the directory.
     *
     * @param userId maps to {@link User#userId} that matches record in the directory.  userId is globally unique in
     *               people container.
     * @return entity containing matching user data.
     * @throws SecurityException if record not found or system error occurs.
     */
    public User readUser( String userId ) throws SecurityException;


    /**
     * Return a list of type String of all users in the people container that match the userId field passed in User entity.
     * This method is used by the Websphere sentry component.  The max number of returned users may be set by the integer limit arg.
     *
     * @param userId contains all or some leading chars that correspond to users stored in the directory.
     * @param limit  integer value sets the max returned records.
     * @return List of type String containing matching userIds.
     * @throws SecurityException in the event of system error.
     */
    public List<String> searchUsers( String userId, int limit ) throws SecurityException;


    /**
     * This function returns the set of users assigned to a given role. The function is valid if and
     * only if the role is a member of the ROLES data set.
     * The max number of users returned is constrained by limit argument.
     * This method is used by the Websphere sentry component.  This method does NOT use hierarchical rbac.
     *
     * @param roleName maps to {@link Role#name} of Role entity assigned to user.
     * @param limit    integer value sets the max returned records.
     * @return List of type String containing userIds assigned to a particular role.
     * @throws org.apache.directory.fortress.core.SecurityException
     *          in the event of data validation or system error.
     */
    public List<String> assignedUsers( String roleName, int limit ) throws SecurityException;


    /**
     * This function returns the set of roles authorized for a given user. The function is valid if
     * and only if the user is a member of the USERS data set.
     *
     * @param userId maps to {@link User#userId} matching User entity stored in the directory.
     * @return Set of type String containing the roles assigned and roles inherited.
     * @throws SecurityException If user not found or system error occurs.
     */
    public List<String> authorizedRoles( String userId ) throws SecurityException;


    /**
     * This utility method can deserialize java.security.Principal to Fortress RBAC session object.
     *
     * @param str contains String to deserialize
     * @return deserialization target object
     */
    public Session deserialize( String str ) throws SecurityException;
}

