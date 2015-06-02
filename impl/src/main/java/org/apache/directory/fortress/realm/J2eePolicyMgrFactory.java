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

import org.apache.directory.fortress.core.util.Config;
import org.apache.directory.fortress.core.SecurityException;
import org.apache.directory.fortress.core.GlobalErrIds;
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
    private static String j2eeClassName = Config.getProperty( J2EE_POLICYMGR_IMPLEMENTATION );

    /**
     * Create and return a reference to {@link J2eePolicyMgr} object.
     *
     * @return instance of {@link J2eePolicyMgr}.
     * @throws org.apache.directory.fortress.core.SecurityException in the event of failure during instantiation.
     */
    public static J2eePolicyMgr createInstance() throws SecurityException
    {
        J2eePolicyMgr realmMgr;
        
        try
        {
            if ( ( j2eeClassName == null ) || ( j2eeClassName.length() == 0 ) )
            {
                j2eeClassName = J2EE_POLICYMGR_DEFAULT_CLASS;
                LOG.debug( "{}.createInstance [{}], not found.", CLS_NM, J2EE_POLICYMGR_IMPLEMENTATION );
                LOG.debug( "{}.createInstance use default [{}], not found.", CLS_NM, J2EE_POLICYMGR_DEFAULT_CLASS );
            }
            
            realmMgr = (J2eePolicyMgr) Class.forName( j2eeClassName ).newInstance();
        }
        catch ( ClassNotFoundException e )
        {
            String error = CLS_NM + ".createInstance caught java.lang.ClassNotFoundException=" + e;
            throw new SecurityException( GlobalErrIds.FT_MGR_CLASS_NOT_FOUND, error, e );
        }
        catch ( InstantiationException e )
        {
            String error = CLS_NM + ".createInstance caught java.lang.InstantiationException=" + e;
            throw new SecurityException( GlobalErrIds.FT_MGR_INST_EXCEPTION, error, e );
        }
        catch ( IllegalAccessException e )
        {
            String error = CLS_NM + ".createInstance caught java.lang.IllegalAccessException=" + e;
            LOG.error( error );
            throw new SecurityException( GlobalErrIds.FT_MGR_ILLEGAL_ACCESS, error, e );
        }
        
        return realmMgr;
    }
}