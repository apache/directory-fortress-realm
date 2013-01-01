/*
 * Copyright (c) 2009-2013, JoshuaTree. All Rights Reserved.
 */

package com.jts.fortress.sentry.websphere;

import com.jts.fortress.sentry.util.CpUtil;

import com.ibm.websphere.security.CertificateMapFailedException;
import com.ibm.websphere.security.CertificateMapNotSupportedException;
import com.ibm.websphere.security.CustomRegistryException;
import com.ibm.websphere.security.EntryNotFoundException;
import com.ibm.websphere.security.NotImplementedException;
import com.ibm.websphere.security.PasswordCheckFailedException;
import com.ibm.websphere.security.Result;
import com.ibm.websphere.security.UserRegistry;

import java.security.cert.X509Certificate;
import java.net.URLClassLoader;
import java.util.List;
import java.util.logging.Logger;

/**
 * This class implements the Websphere UserRegistry class and provides Java EE security services within the Websphere container.
 * This class is a "proxy" for the {@code com.jts.fortress.sentry.websphere.WsAccessMgrImpl} class which isolates dependencies from the Websphere
 * runtime environment by loading the implementation on a URLClassLoader.
 *
 * @author Shawn McKinney
 */
public class WsAccessMgrProxy implements com.ibm.websphere.security.UserRegistry
{
    private UserRegistry realmImpl = null;
    private static String REALM_PROP_NAME = "REALM_CLASSPATH";
    private static String REALM_IMPL_PROP_NAME = "REALM_PROP";
    private static String DEFAULT_REALM_IMPLEMENTATION = "com.jts.fortress.sentry.websphere.WsAccessMgrImpl";
    private static Logger log = Logger.getLogger(WsAccessMgrProxy.class.getName());


    /**
     * This method will load the Fortress Websphere implementation on a URL classloader.  Methods on the implementation are
     * wrapped by methods on this class and are accessed via the {@code realmImpl} instance variable of this class.
     *
     * @param props Contains runtime properties of where on file system the Fortress runtime dependencies can be found.
     * @throws CustomRegistryException  In the event of system error.
     * @throws java.rmi.RemoteException In the event of RMI system error.
     */
    @Override
    public void initialize(java.util.Properties props)
        throws CustomRegistryException, java.rmi.RemoteException
    {
        try
        {
            log.info(WsAccessMgrProxy.class.getName() + ".initialize - Fortress UserRegistry");
            setUserRegistryClasspath(props);
            String realmClassname = getUserRegistryImplClassname(props);
            URLClassLoader ucl = new URLClassLoader(CpUtil.getRealmClasspath(REALM_PROP_NAME), this.getClass().getClassLoader());
            log.info(WsAccessMgrProxy.class.getName() + ".initialize - instantiate class: " + realmClassname);
            Class realmImpClass = ucl.loadClass(realmClassname);
            realmImpl = (UserRegistry) realmImpClass.newInstance();
            if (realmImpl != null)
            {
                log.info(WsAccessMgrProxy.class.getName() + ".initialize - Fortress UserRegistry initialized no errors.");
                realmImpl.initialize(props);
            }
            else
            {
                String error = WsAccessMgrProxy.class.getName() + ".initialize - Fortress UserRegistry error instantiating:" + realmClassname;
                log.severe(error);
                throw new CustomRegistryException(error);
            }
        }
        catch (java.lang.ClassNotFoundException e)
        {
            String error = WsAccessMgrProxy.class.getName() + ".initialize ClassNotFoundException=" + e;
            log.severe(error);
            e.printStackTrace();
            throw new CustomRegistryException(error);
        }
        catch (java.lang.InstantiationException e)
        {
            String error = WsAccessMgrProxy.class.getName() + ".initialize InstantiationException=" + e;
            log.severe(error);
            e.printStackTrace();
            throw new CustomRegistryException(error);
        }
        catch (java.lang.IllegalAccessException e)
        {
            String error = WsAccessMgrProxy.class.getName() + ".initialize IllegalAccessException=" + e;
            log.severe(error);
            e.printStackTrace();
            throw new CustomRegistryException(error);
        }
    }


    /**
     * Sets the userRegistryClasspath attribute of the WsAccessMgrProxy object
     *
     * @param props The new userRegistryClasspath value
     */
    private void setUserRegistryClasspath(java.util.Properties props)
    {
        // Get the value of REALM_CLASSPATH from Websphere WsAccessMgrImpl properties
        String realmClasspath = props.getProperty(REALM_PROP_NAME);
        if (realmClasspath != null && realmClasspath.compareTo("") != 0)
        {
            log.info(WsAccessMgrProxy.class.getName() + ".setRealmClasspath User Registry CpUtil=" + realmClasspath);
            System.setProperty(REALM_PROP_NAME, realmClasspath);
        }
    }


    /**
     * Gets the userRegistryImplClassname attribute of the WsAccessMgrProxy
     * object
     *
     * @param props Description of the Parameter
     * @return The userRegistryImplClassname value
     */
    private String getUserRegistryImplClassname(java.util.Properties props)
    {
        // Get the value of REALM_IMPLEMENTATION from Websphere custom realm properties
        String realmClassname = props.getProperty(REALM_IMPL_PROP_NAME);
        if (realmClassname == null || realmClassname.compareTo("") == 0)
        {
            // Can't find on custom realm properties so check for a system property
            realmClassname = System.getProperty(REALM_IMPL_PROP_NAME);
            if (realmClassname != null && realmClassname.compareTo("") != 0)
            {
                log.warning(WsAccessMgrProxy.class.getName() + ".getRealmImplClassname - REALM_IMPLEMENTATION System Prop=" + realmClassname);
            }
            else
            {
                // Can't find the value anywhere else so set to default.
                realmClassname = DEFAULT_REALM_IMPLEMENTATION;
                log.warning(WsAccessMgrProxy.class.getName() + ".getRealmImplClassname - REALM_IMPLEMENTATION default=" + realmClassname);
            }
        }
        else
        {
            log.info(WsAccessMgrProxy.class.getName() + ".getRealmImplClassname - REALM_IMPLEMENTATION WsAccessMgrImpl Prop=" + realmClassname);
        }
        return realmClassname;
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
    @Override
    public String checkPassword(String userId, String passwd)
        throws PasswordCheckFailedException, CustomRegistryException, java.rmi.RemoteException
    {
        return realmImpl.checkPassword(userId, passwd);
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
     * @throws java.rmi.RemoteException      Description of the Exception
     */
    @Override
    public String mapCertificate(X509Certificate[] cert)
        throws CertificateMapNotSupportedException,
        CertificateMapFailedException, CustomRegistryException, java.rmi.RemoteException
    {
        return realmImpl.mapCertificate(cert);
    }


    /**
     * Gets the realm attribute of the WsAccessMgrProxy object
     *
     * @return The realm value
     * @throws CustomRegistryException  Description of the Exception
     * @throws java.rmi.RemoteException Description of the Exception
     */
    @Override
    public String getRealm()
        throws CustomRegistryException, java.rmi.RemoteException
    {
        return realmImpl.getRealm();
    }


    /**
     * Gets the users attribute of the WsAccessMgrProxy object
     *
     * @param pattern Description of the Parameter
     * @param limit   Description of the Parameter
     * @return The users value
     * @throws CustomRegistryException  Description of the Exception
     * @throws java.rmi.RemoteException Description of the Exception
     */
    @Override
    public Result getUsers(String pattern, int limit)
        throws CustomRegistryException, java.rmi.RemoteException
    {
        return realmImpl.getUsers(pattern, limit);
    }


    /**
     * Gets the usersForGroup attribute of the WsAccessMgrProxy object
     *
     * @param groupSecurityName Description of the Parameter
     * @param limit             Description of the Parameter
     * @return The usersForGroup value
     * @throws NotImplementedException Description of the Exception
     * @throws EntryNotFoundException  Description of the Exception
     * @throws CustomRegistryException Description of the Exception
     */
    @Override
    public Result getUsersForGroup(String groupSecurityName, int limit)
        throws NotImplementedException,
        EntryNotFoundException,
        CustomRegistryException
    {
        return getUsersForGroup(groupSecurityName, limit);
    }


    /**
     * Gets the userDisplayName attribute of the WsAccessMgrProxy object
     *
     * @param userName Description of the Parameter
     * @return The userDisplayName value
     * @throws CustomRegistryException  Description of the Exception
     * @throws EntryNotFoundException   Description of the Exception
     * @throws java.rmi.RemoteException Description of the Exception
     */
    @Override
    public String getUserDisplayName(String userName)
        throws CustomRegistryException, EntryNotFoundException, java.rmi.RemoteException
    {
        return realmImpl.getUserDisplayName(userName);
    }


    /**
     * Gets the uniqueUserId attribute of the WsAccessMgrProxy object
     *
     * @param userName Description of the Parameter
     * @return The uniqueUserId value
     * @throws CustomRegistryException  Description of the Exception
     * @throws EntryNotFoundException   Description of the Exception
     * @throws java.rmi.RemoteException Description of the Exception
     */
    @Override
    public String getUniqueUserId(String userName)
        throws CustomRegistryException, EntryNotFoundException, java.rmi.RemoteException
    {
        return realmImpl.getUniqueUserId(userName);
    }


    /**
     * Gets the userSecurityName attribute of the WsAccessMgrProxy object
     *
     * @param uniqueUserId Description of the Parameter
     * @return The userSecurityName value
     * @throws CustomRegistryException  Description of the Exception
     * @throws EntryNotFoundException   Description of the Exception
     * @throws java.rmi.RemoteException Description of the Exception
     */
    @Override
    public String getUserSecurityName(String uniqueUserId)
        throws CustomRegistryException, EntryNotFoundException, java.rmi.RemoteException
    {
        return realmImpl.getUserSecurityName(uniqueUserId);
    }


    /**
     * Gets the validUser attribute of the WsAccessMgrProxy object
     *
     * @param userName Description of the Parameter
     * @return The validUser value
     * @throws CustomRegistryException  Description of the Exception
     * @throws java.rmi.RemoteException Description of the Exception
     */
    @Override
    public boolean isValidUser(String userName)
        throws CustomRegistryException, java.rmi.RemoteException
    {
        return realmImpl.isValidUser(userName);
    }


    /**
     * Gets the groups attribute of the WsAccessMgrProxy object
     *
     * @param pattern Description of the Parameter
     * @param limit   Description of the Parameter
     * @return The groups value
     * @throws CustomRegistryException  Description of the Exception
     * @throws java.rmi.RemoteException Description of the Exception
     */
    @Override
    public Result getGroups(String pattern, int limit)
        throws CustomRegistryException, java.rmi.RemoteException
    {
        return realmImpl.getGroups(pattern, limit);
    }


    /**
     * Gets the groupsForUser attribute of the WsAccessMgrProxy object
     *
     * @param userName Description of the Parameter
     * @return The groupsForUser value
     * @throws CustomRegistryException  Description of the Exception
     * @throws EntryNotFoundException   Description of the Exception
     * @throws java.rmi.RemoteException Description of the Exception
     */
    @Override
    public List getGroupsForUser(String userName)
        throws CustomRegistryException, EntryNotFoundException, java.rmi.RemoteException
    {
        return realmImpl.getGroupsForUser(userName);
    }


    /**
     * Gets the groupDisplayName attribute of the WsAccessMgrProxy object
     *
     * @param groupName Description of the Parameter
     * @return The groupDisplayName value
     * @throws CustomRegistryException  Description of the Exception
     * @throws EntryNotFoundException   Description of the Exception
     * @throws java.rmi.RemoteException Description of the Exception
     */
    @Override
    public String getGroupDisplayName(String groupName)
        throws CustomRegistryException, EntryNotFoundException, java.rmi.RemoteException
    {
        return realmImpl.getGroupDisplayName(groupName);
    }


    /**
     * Gets the uniqueGroupId attribute of the WsAccessMgrProxy object
     *
     * @param groupName Description of the Parameter
     * @return The uniqueGroupId value
     * @throws CustomRegistryException  Description of the Exception
     * @throws EntryNotFoundException   Description of the Exception
     * @throws java.rmi.RemoteException Description of the Exception
     */
    @Override
    public String getUniqueGroupId(String groupName)
        throws CustomRegistryException, EntryNotFoundException, java.rmi.RemoteException
    {
        return realmImpl.getUniqueGroupId(groupName);
    }


    /**
     * Gets the uniqueGroupIds attribute of the WsAccessMgrProxy object
     *
     * @param uniqueUserId Description of the Parameter
     * @return The uniqueGroupIds value
     * @throws CustomRegistryException  Description of the Exception
     * @throws EntryNotFoundException   Description of the Exception
     * @throws java.rmi.RemoteException Description of the Exception
     */
    @Override
    public List getUniqueGroupIds(String uniqueUserId)
        throws CustomRegistryException, EntryNotFoundException, java.rmi.RemoteException
    {
        return realmImpl.getUniqueGroupIds(uniqueUserId);
    }


    /**
     * Gets the groupSecurityName attribute of the WsAccessMgrProxy object
     *
     * @param uniqueGroupId Description of the Parameter
     * @return The groupSecurityName value
     * @throws CustomRegistryException  Description of the Exception
     * @throws EntryNotFoundException   Description of the Exception
     * @throws java.rmi.RemoteException Description of the Exception
     */
    @Override
    public String getGroupSecurityName(String uniqueGroupId)
        throws CustomRegistryException, EntryNotFoundException, java.rmi.RemoteException
    {
        return realmImpl.getGroupSecurityName(uniqueGroupId);
    }


    /**
     * Gets the validGroup attribute of the WsAccessMgrProxy object
     *
     * @param groupName Description of the Parameter
     * @return The validGroup value
     * @throws CustomRegistryException  Description of the Exception
     * @throws java.rmi.RemoteException Description of the Exception
     */
    @Override
    public boolean isValidGroup(String groupName)
        throws CustomRegistryException, java.rmi.RemoteException
    {
        return realmImpl.isValidGroup(groupName);
    }


    /**
     * Description of the Method
     *
     * @param userSecurityName Description of the Parameter
     * @return Description of the Return Value
     * @throws CustomRegistryException  Description of the Exception
     * @throws NotImplementedException  Description of the Exception
     * @throws EntryNotFoundException   Description of the Exception
     * @throws java.rmi.RemoteException Description of the Exception
     */
    @Override
	public com.ibm.websphere.security.cred.WSCredential createCredential(String userSecurityName)
		throws CustomRegistryException, NotImplementedException, EntryNotFoundException, java.rmi.RemoteException
	{
		return realmImpl.createCredential(userSecurityName);
	}
}