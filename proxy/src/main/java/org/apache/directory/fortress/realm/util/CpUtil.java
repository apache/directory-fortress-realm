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
package org.apache.directory.fortress.realm.util;

import java.util.logging.Logger;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.StringTokenizer;

/**
 * This class interrogates the Java System for properties set on REALM_CLASSPATH by the
 * caller.  The system properties are converted into URL arrays for later URL classloader processing.
 *
 * @author Shawn McKinney
 */
public class CpUtil
{
    private static final String CLS_NM = CpUtil.class.getName();
    private static Logger LOG = Logger.getLogger(CLS_NM);
    private static final String PATH_SEPARATOR = "path.separator";
    private static final String FILE_PREFIX = "file:/";
    private static final String UNIX_SLASH = "/";
    private static final String WIN_SLASH = "\\";
    private static final String UNIX_SEPARATOR = ":";

    /**
     * This method is used by Tomcat Realm Proxy to retrieve REALM_CLASSPATH from the Server.xml config file.
     *
     * @param classpath String contains paths separated by ':' or ';' depending if Unix or Win.
     * @return URL array containing the resource names that comprise the REALM_CLASSPATH.
     */
    public static URL[] parseRealmClasspath( String classpath )
    {
        URL urls[] = null;
        
        try
        {
            String slash;
            String filePrefix = FILE_PREFIX;
            String pathSeparator = System.getProperty( PATH_SEPARATOR );

            // Is this a Unix machine?
            if ( UNIX_SEPARATOR.compareTo( pathSeparator ) == 0 )
            {
                // unix requires adding extra forward slash:
                filePrefix += UNIX_SLASH;
                slash = UNIX_SLASH;
            }
            else
            {
                slash = WIN_SLASH;
            }

            LOG.info(CLS_NM + ".parseRealmClasspath <" + classpath + ">");
            StringTokenizer st = new StringTokenizer( classpath, pathSeparator, false );
            int size = st.countTokens();
            urls = new URL[size];
            
            for ( int i = 0; i < size; i++ )
            {
                String resource = st.nextToken();
                resource = getResource( resource, slash );
                String file = filePrefix + resource;
                urls[i] = new URL( file );
                LOG.info( CLS_NM + ".parseRealmClasspath path" + i + " <" + urls[i] + ">" );
            }
        }
        catch ( MalformedURLException me )
        {
            String error = CLS_NM + ".parseRealmClasspath caught MalformedURLException=" + me;
            LOG.severe( error );
            error = CLS_NM + ".parseRealmClasspath check your Fortress REALM_CLASSPATH setting.";
            LOG.severe( error );
        }
        
        return urls;
    }


    /**
     * This method is used by Websphere Realm Proxy to retrieve REALM_CLASSPATH from the System.getProperty..
     *
     * @param cpProp contains paths separated by ':' or ';' depending if Unix or Win.
     * @return URL array containing the resource names that comprise the REALM_CLASSPATH.
     */
    public static URL[] getRealmClasspath( String cpProp )
    {
        URL urls[] = null;
        
        try
        {
            String slash;
            String filePrefix = FILE_PREFIX;
            String classpath = System.getProperty( cpProp );
            String pathSeparator = System.getProperty( PATH_SEPARATOR );

            if ( ( classpath == null ) || ( classpath.length() == 0 ) )
            {
                String error = CLS_NM + ".parseRealmClasspath invalid realm classpath detected";
                LOG.severe( error );
                throw new RuntimeException( error );
            }
            else if( ( pathSeparator == null ) || ( pathSeparator.length() == 0 ) )
            {
                String error = CLS_NM + ".parseRealmClasspath invalid classpath detected";
                LOG.severe( error );
                throw new RuntimeException( error );
            }

            // Is this a Unix machine?
            if ( pathSeparator.compareTo( UNIX_SEPARATOR ) == 0 )
            {
                // unix requires adding extra forward slash:
                filePrefix += UNIX_SLASH;
                slash = UNIX_SLASH;
            }
            else
            {
                slash = WIN_SLASH;
            }
            
            LOG.info( CLS_NM + ".parseRealmClasspath <" + classpath + ">" );
            StringTokenizer st = new StringTokenizer( classpath, pathSeparator, false );
            int size = st.countTokens();
            urls = new URL[size];
            
            for ( int i = 0; i < size; i++ )
            {
                String resource = st.nextToken();
                resource = getResource( resource, slash );
                String file = filePrefix + resource;
                urls[i] = new URL( file );
                LOG.info( CLS_NM + ".parseRealmClasspath path" + i + " <" + urls[i] + ">" );
            }
        }
        catch ( MalformedURLException me )
        {
            String error = CLS_NM + ".parseRealmClasspath caught MalformedURLException=" + me;
            LOG.severe( error );
            throw new RuntimeException( error );
        }
        
        return urls;
    }


    /**
     * Check to see if resource is a folder and if the trailing slash was left off add it.  Otherwise
     * the folder will not be added to REALM_CLASSPATH causing problems with Fortress finding its config folder.
     *
     * @param name
     * @param slash
     * @return String containing resource value
     */
    private static String getResource( String name, String slash )
    {
        File rFile = new File( name );
        
        if ( rFile.isDirectory() )
        {
            if ( name.lastIndexOf( slash ) != ( name.length() - 1 ) )
            {
                name += slash;
                LOG.info( CLS_NM + ".getResource slash added to dir path" );
            }
        }
        
        return name;
    }
}