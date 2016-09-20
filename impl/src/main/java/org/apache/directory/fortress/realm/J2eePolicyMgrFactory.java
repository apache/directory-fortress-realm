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

import org.apache.commons.lang.StringUtils;
import org.apache.directory.fortress.core.*;
import org.apache.directory.fortress.core.GlobalIds;
import org.apache.directory.fortress.core.SecurityException;
import org.apache.directory.fortress.core.impl.AdminMgrImpl;
import org.apache.directory.fortress.core.rest.AdminMgrRestImpl;
import org.apache.directory.fortress.core.util.ClassUtil;
import org.apache.directory.fortress.core.util.Config;
import org.apache.directory.fortress.core.util.VUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates an instance of the J2eePolicyMgr object.
 * <p/>
 * The default implementation class is specified as {@link #J2EE_POLICYMGR_DEFAULT_CLASS} but can be overridden by
 * adding the {@link #J2EE_POLICYMGR_IMPLEMENTATION} config property.
 * <p/>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class J2eePolicyMgrFactory
{
    // disallow construction
    private J2eePolicyMgrFactory()
    {
    }

    /**
     * When this optional tag, {@code realmImplementation}, is placed in Fortress properties, its class name will be the default {@code org.apache.directory.fortress.core.security.RealmMgr} instance used.
     */
    private static final String J2EE_POLICYMGR_IMPLEMENTATION = "realmImplementation";
    private static final String J2EE_POLICYMGR_DEFAULT_CLASS = J2eePolicyMgrImpl.class.getName();
    private static final String CLS_NM = J2eePolicyMgrFactory.class.getName();
    private static final Logger LOG = LoggerFactory.getLogger( CLS_NM );
    private static String j2eeClassName = Config.getInstance().getProperty( J2EE_POLICYMGR_IMPLEMENTATION );

    /**
     * Create and return a reference to {@link J2eePolicyMgr} object with HOME context.
     *
     * @return instance of {@link J2eePolicyMgr}.
     * @throws org.apache.directory.fortress.core.SecurityException in the event of failure during instantiation.
     */
    public static J2eePolicyMgr createInstance() throws SecurityException
    {
        return createInstance( GlobalIds.HOME );
    }


    /**
     * Create and return a reference to {@link J2eePolicyMgr} object with HOME context.
     *
     * @param contextId maps to sub-tree in DIT, for example ou=contextId, dc=example, dc=com.
     * @return instance of {@link J2eePolicyMgr}.
     * @throws SecurityException in the event of failure during instantiation.
     */
    public static J2eePolicyMgr createInstance(String contextId)
        throws SecurityException
    {
        VUtil.assertNotNull( contextId, GlobalErrIds.CONTEXT_NULL, CLS_NM + ".createInstance" );
        String j2eeClassName = Config.getInstance().getProperty( J2EE_POLICYMGR_IMPLEMENTATION );
        J2eePolicyMgr policyMgr;

        if ( StringUtils.isEmpty( j2eeClassName ) )
        {
            policyMgr = new J2eePolicyMgrImpl( );
        }
        else
        {
            policyMgr = ( J2eePolicyMgr ) ClassUtil.createInstance( J2EE_POLICYMGR_DEFAULT_CLASS );
        }

        policyMgr.setContextId( contextId );
        return policyMgr;
    }

}