/*
 * Copyright (c) 2009-2011. Joshua Tree Software, LLC.  All Rights Reserved.
 */

package com.jts.fortress.realm.websphere;

import com.jts.fortress.SecurityException;
import com.jts.fortress.constants.GlobalErrIds;
import com.jts.fortress.rbac.User;
import com.jts.fortress.realm.J2eePolicyMgrFactory;
import com.jts.fortress.realm.J2eePolicyMgr;

import java.security.cert.X509Certificate;
import java.util.List;

import com.ibm.websphere.security.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * This class runs on a URL classloader and provides Fortress runtime security services for the Websphere container.
 *
 * @author smckinn
 * @created January 13, 2010
 */
public class WsAccessMgrImpl implements com.ibm.websphere.security.UserRegistry
{
    /**
     * Description of the Field
     */
    protected static String realmAgentName = "FortressUserRegistry";
    private static final String OCLS_NM = WsAccessMgrImpl.class.getName();
    private static final Logger log = Logger.getLogger(OCLS_NM);
    private static J2eePolicyMgr j2eeMgr;

    static
    {
        try
        {
            j2eeMgr = J2eePolicyMgrFactory.createInstance();
        }
        catch (SecurityException se)
        {
            String error = OCLS_NM + " static initializer caught SecurityException=" + se;
            log.fatal(error);
        }
    }

    /**
     * This method simply checks to ensure the j2eeMgr instance variable was correctly initialized.  If it
     * is not an exception is thrown.
     *
     * @param props Description of the Parameter
     * @throws CustomRegistryException Description of the Exception
     */
    public void initialize(java.util.Properties props)
        throws CustomRegistryException
    {
        if (j2eeMgr == null)
        {
            String error = WsAccessMgrImpl.class.getName() + ".initialize WsAccessMgrImpl failed to initialize";
            log.fatal(error);
            throw new CustomRegistryException(error);
        }
        log.info(OCLS_NM + ". J2EE policy agent initialization successful");
    }


    /**
     * Perform user authentication and evaluate password policies.
     *
     * @param userId   Contains the userid of the user signing on.
     * @param passwd Contains the user's password.
     * @return String containing the userId of the caller (if successful).
     * @throws PasswordCheckFailedException In the event the User's password check failed due to invalid credential or password policy violation.
     * @throws CustomRegistryException      In the event of system error.
     * @throws java.rmi.RemoteException     In the event of RMI system error.
     */
    public String checkPassword(String userId, String passwd)
        throws PasswordCheckFailedException,
        CustomRegistryException
    {
        if (log.isEnabledFor(Level.DEBUG))
        {
            log.debug(OCLS_NM + ".checkPassword(" + userId + ")");
        }
        try
        {
            boolean result = j2eeMgr.authenticate(userId, passwd.toCharArray());
            if (!result)
            {
                String info = OCLS_NM + ".checkPassword user <" + userId + "> authentication failed";
                log.info(info);
                throw new PasswordCheckFailedException(info);
            }
        }
        catch (SecurityException se)
        {
            String error = OCLS_NM + ".checkPassword caught SecurityException=" + se;
            log.error(error);
            throw new CustomRegistryException(error);
        }

        return userId;
    }


    /**
     * Description of the Method
     *
     * @param cert Description of the Parameter
     * @return Description of the Return
     *         Value
     * @throws CertificateMapNotSupportedException
     *                                       Description of the Exception
     * @throws CertificateMapFailedException Description of the Exception
     * @throws CustomRegistryException       Description of the Exception
     */
    public String mapCertificate(X509Certificate[] cert)
        throws CertificateMapNotSupportedException,
        CertificateMapFailedException, CustomRegistryException
    {
        if (log.isEnabledFor(Level.DEBUG))
        {
            log.debug(OCLS_NM + ".mapCertificate");
        }
        String name = null;
        try
        {
            X509Certificate cert1 = cert[0];
            name = cert1.getSubjectDN().getName();
        }
        catch (Exception ex)
        {
            String error = OCLS_NM + ".mapCertificate exception=" + ex;
            log.error(error);
            throw new CertificateMapNotSupportedException(ex.getMessage());
        }
        if (!isValidUser(name))
        {
            String warning = OCLS_NM + ".mapCertificate invalid user <" + name + ">";
            log.error(warning);
            throw new CustomRegistryException(name);
        }
        return name;
    }


    /**
     * Gets the realm attribute of the WsAccessMgrImpl object
     *
     * @return The realm value
     * @throws CustomRegistryException Description of the Exception
     */
    public String getRealm()
        throws CustomRegistryException
    {
        return realmAgentName;
    }


    /**
     * Gets the users attribute of the WsAccessMgrImpl object
     *
     * @param pattern Description of the Parameter
     * @param limit   Description of the Parameter
     * @return The users value
     * @throws CustomRegistryException Description of the Exception
     */
    public Result getUsers(String pattern, int limit)
        throws CustomRegistryException
    {
        Result result = new Result();
        List<String> users;
        if (log.isEnabledFor(Level.DEBUG))
        {
            log.debug(OCLS_NM + ".getUsers(" + pattern + ")");
        }
        try
        {
            users = j2eeMgr.searchUsers(pattern, limit);
        }
        catch (SecurityException se)
        {
            String error = OCLS_NM + ".getUsers caught caught SecurityException=" + se;
            log.error(error);
            throw new CustomRegistryException(error);
        }
        result.setList(users);
        return result;
    }


    /**
     * Gets the usersForGroup attribute of the WsAccessMgrImpl object
     *
     * @param groupSecurityName Description of the Parameter
     * @param limit             Description of the Parameter
     * @return The usersForGroup value
     * @throws CustomRegistryException Description of the Exception
     */
    public Result getUsersForGroup(String groupSecurityName, int limit)
        throws CustomRegistryException
    {
        Result result = new Result();
        List<String> users;
        if (log.isEnabledFor(Level.DEBUG))
        {
            log.debug(OCLS_NM + ".getUsersForGroup(" + groupSecurityName + ")");
        }
        try
        {
            users = j2eeMgr.assignedUsers(groupSecurityName, limit);
        }
        catch (SecurityException se)
        {
            String error = OCLS_NM + ".getUsersForGroup caught SecurityException=" + se;
            log.error(error);
            throw new CustomRegistryException(error);
        }
        result.setList(users);
        return result;
    }


    /**
     * Gets the userDisplayName attribute of the WsAccessMgrImpl object
     *
     * @param userName Description of the Parameter
     * @return The userDisplayName value
     * @throws CustomRegistryException Description of the Exception
     * @throws EntryNotFoundException  Description of the Exception
     */
    public String getUserDisplayName(String userName)
        throws CustomRegistryException,
        EntryNotFoundException
    {
        if (log.isEnabledFor(Level.DEBUG))
        {
            log.debug(OCLS_NM + ".getUserDisplayName(" + userName + ")");
        }
        return userName;
    }


    /**
     * Gets the uniqueUserId attribute of the WsAccessMgrImpl object
     *
     * @param userName Description of the Parameter
     * @return The uniqueUserId value
     * @throws CustomRegistryException Description of the Exception
     * @throws EntryNotFoundException  Description of the Exception
     */
    public String getUniqueUserId(String userName)
        throws CustomRegistryException,
        EntryNotFoundException
    {
        if (log.isEnabledFor(Level.DEBUG))
        {
            log.debug(OCLS_NM + ".getUniqueUserId(" + userName + ")");
        }
        return userName;
    }


    /**
     * Gets the userSecurityName attribute of the WsAccessMgrImpl object
     *
     * @param uniqueUserId Description of the Parameter
     * @return The userSecurityName value
     * @throws CustomRegistryException Description of the Exception
     * @throws EntryNotFoundException  Description of the Exception
     */
    public String getUserSecurityName(String uniqueUserId)
        throws CustomRegistryException,
        EntryNotFoundException
    {
        if (log.isEnabledFor(Level.DEBUG))
        {
            log.debug(OCLS_NM + ".getUserSecurityName(" + uniqueUserId + ")");
        }
        return uniqueUserId;
    }


    /**
     * Gets the validUser attribute of the WsAccessMgrImpl object
     *
     * @param userName Description of the Parameter
     * @return The validUser value
     * @throws CustomRegistryException Description of the Exception
     */
    public boolean isValidUser(String userName)
        throws CustomRegistryException
    {
        if (log.isEnabledFor(Level.DEBUG))
        {
            log.debug(OCLS_NM + ".isValidUser(" + userName + ")");
        }
        boolean bIsValid = false;
        try
        {
            User user = j2eeMgr.readUser(userName);
            if (user != null)
            {
                bIsValid = true;
            }
        }
        catch (SecurityException se)
        {
            if (se.getErrorId() == GlobalErrIds.USER_NOT_FOUND)
            {
                log.info(OCLS_NM + ".isValidUser detected invalid user <" + userName + ">");
            }
            else
            {
                String error = OCLS_NM + ".isValidUser() caught SecurityException=" + se;
                log.error(error);
                throw new CustomRegistryException(error);
            }
        }
        return bIsValid;
    }


    /**
     * Gets the groups attribute of the WsAccessMgrImpl object
     *
     * @param pattern Description of the Parameter
     * @param limit   Description of the Parameter
     * @return The groups value
     * @throws CustomRegistryException Description of the Exception
     */
    public Result getGroups(String pattern, int limit)
        throws CustomRegistryException
    {
        List<String> roles;
        Result result = new Result();
        if (log.isEnabledFor(Level.DEBUG))
        {
            log.debug(OCLS_NM + ".getGroups(" + pattern + ")");
        }
        try
        {
            roles = j2eeMgr.searchRoles(pattern, limit);
        }
        catch (SecurityException se)
        {
            String error = OCLS_NM + ".getGroups caught SecurityException=" + se;
            log.error(error);
            throw new CustomRegistryException(error);
        }
        result.setList(roles);
        return result;
    }


    /**
     * Gets the groupsForUser attribute of the WsAccessMgrImpl object
     *
     * @param userName Description of the Parameter
     * @return The groupsForUser value
     * @throws CustomRegistryException Description of the Exception
     * @throws EntryNotFoundException  Description of the Exception
     */
    public List getGroupsForUser(String userName)
        throws CustomRegistryException,
        EntryNotFoundException
    {
        if (log.isEnabledFor(Level.DEBUG))
        {
            log.debug(OCLS_NM + ".getUserRoles(" + userName + ")");
        }
        List<String> roles;
        try
        {
            roles = j2eeMgr.authorizedRoles(userName);
        }
        catch (SecurityException se)
        {
            if (!isValidUser(userName))
            {
                String error = OCLS_NM + ".getGroupsForUser detected invalid user <" + userName + ">";
                log.warn(error);
                throw new EntryNotFoundException(userName);
            }
            else
            {
                String error = OCLS_NM + ".getUserRoles caught SecurityException=" + se;
                log.error(error);
                throw new CustomRegistryException(error);
            }
        }
        return roles;
    }


    /**
     * Gets the groupDisplayName attribute of the WsAccessMgrImpl object
     *
     * @param groupName Description of the Parameter
     * @return The groupDisplayName value
     * @throws CustomRegistryException Description of the Exception
     * @throws EntryNotFoundException  Description of the Exception
     */
    public String getGroupDisplayName(String groupName)
        throws CustomRegistryException,
        EntryNotFoundException
    {
        if (log.isEnabledFor(Level.DEBUG))
        {
            log.debug(OCLS_NM + ".getGroupDisplayName(" + groupName + ")");
        }
        return groupName;
    }


    /**
     * Gets the uniqueGroupId attribute of the WsAccessMgrImpl object
     *
     * @param groupName Description of the Parameter
     * @return The uniqueGroupId value
     * @throws CustomRegistryException Description of the Exception
     * @throws EntryNotFoundException  Description of the Exception
     */
    public String getUniqueGroupId(String groupName)
        throws CustomRegistryException,
        EntryNotFoundException
    {
        if (log.isEnabledFor(Level.DEBUG))
        {
            log.debug(OCLS_NM + ".getUniqueGroupId(" + groupName + ")");
        }
        return groupName;
    }


    /**
     * Gets the uniqueGroupIds attribute of the WsAccessMgrImpl object
     *
     * @param uniqueUserId Description of the Parameter
     * @return The uniqueGroupIds value
     * @throws CustomRegistryException Description of the Exception
     * @throws EntryNotFoundException  Description of the Exception
     */
    public List getUniqueGroupIds(String uniqueUserId)
        throws CustomRegistryException,
        EntryNotFoundException
    {
        if (log.isEnabledFor(Level.DEBUG))
        {
            log.debug(OCLS_NM + ".getUniqueGroupIds(" + uniqueUserId + ")");
        }
        return this.getGroupsForUser(uniqueUserId);
    }


    /**
     * Gets the groupSecurityName attribute of the WsAccessMgrImpl object
     *
     * @param uniqueGroupId Description of the Parameter
     * @return The groupSecurityName value
     * @throws CustomRegistryException Description of the Exception
     * @throws EntryNotFoundException  Description of the Exception
     */
    public String getGroupSecurityName(String uniqueGroupId)
        throws CustomRegistryException,
        EntryNotFoundException
    {
        if (log.isEnabledFor(Level.DEBUG))
        {
            log.debug(OCLS_NM + ".getGroupSecurityName(" + uniqueGroupId + ")");
        }
        return uniqueGroupId;
    }


    /**
     * Gets the validGroup attribute of the WsAccessMgrImpl object
     *
     * @param groupName Description of the Parameter
     * @return The validGroup value
     * @throws CustomRegistryException Description of the Exception
     */
    public boolean isValidGroup(String groupName)
        throws CustomRegistryException
    {
        boolean bIsValid = false;
        if (log.isEnabledFor(Level.DEBUG))
        {
            log.debug(OCLS_NM + ".isValidGroup <" + groupName + ">");
        }
        try
        {
            j2eeMgr.readRole(groupName);
            bIsValid = true;
        }
        catch (SecurityException se)
        {
            if (se.getErrorId() == GlobalErrIds.ROLE_NOT_FOUND)
            {
                log.info(OCLS_NM + ".isValidGroup invalid group name: " + groupName);
            }
            else
            {
                String error = OCLS_NM + ".isValidRole role <" + groupName + "> caught SecurityException=" + se;
                log.error(error);
                throw new CustomRegistryException(error);
            }
        }
        return bIsValid;
    }


    /**
     * Description of the Method
     *
     * @param userSecurityName Description of the Parameter
     * @return Description of the Return Value
     * @throws CustomRegistryException Description of the Exception
     * @throws NotImplementedException Description of the Exception
     * @throws EntryNotFoundException  Description of the Exception
     */
	public com.ibm.websphere.security.cred.WSCredential createCredential(String userSecurityName)
		throws CustomRegistryException, NotImplementedException, EntryNotFoundException
	{
		return null;
	}
}