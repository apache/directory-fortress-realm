/*
 * Copyright (c) 2009-2012. Joshua Tree Software, LLC.  All Rights Reserved.
 */

package com.jts.fortress.sentry.util;

import java.util.logging.Logger;
import java.io.File;
import java.net.URL;
import java.util.StringTokenizer;

/**
 * This class interrogates the Java System for properties set on REALM_CLASSPATH by the
 * caller.  The system properties are converted into URL arrays for later URL classloader processing.
 *
 * @author smckinn
 * @created January 13, 2010
 */
public class CpUtil
{
    private static final String OCLS_NM = CpUtil.class.getName();
    private static Logger log = Logger.getLogger(OCLS_NM);
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
    public static URL[] parseRealmClasspath(String classpath)
    {
        URL urls[] = null;
        try
        {
            String slash;
            String filePrefix = FILE_PREFIX;
            String pathSeparator = System.getProperty(PATH_SEPARATOR);

            // Is this a Unix machine?
            if (pathSeparator.compareTo(UNIX_SEPARATOR) == 0)
            {
                // unix requires adding extra forward slash:
                filePrefix += UNIX_SLASH;
                slash = UNIX_SLASH;
            }
            else
            {
                slash = WIN_SLASH;
            }

            log.info(OCLS_NM + ".parseRealmClasspath <" + classpath + ">");
            StringTokenizer st = new StringTokenizer(classpath, pathSeparator, false);
            int size = st.countTokens();
            urls = new URL[size];
            for (int i = 0; i < size; i++)
            {
                String resource = st.nextToken();
                resource = getResource(resource, slash);
                String file = filePrefix + resource;
                urls[i] = new URL(file);
                log.info(OCLS_NM + ".parseRealmClasspath path" + i + " <" + urls[i] + ">");
            }
        }
        catch (java.net.MalformedURLException me)
        {
            String error = OCLS_NM + ".parseRealmClasspath caught MalformedURLException=" + me;
            log.severe(error);
            error = OCLS_NM + ".parseRealmClasspath check your Fortress REALM_CLASSPATH setting.";
            log.severe(error);
        }
        return urls;
    }


    /**
     * This method is used by Websphere Realm Proxy to retrieve REALM_CLASSPATH from the System.getProperty..
     *
     * @param cpProp contains paths separated by ':' or ';' depending if Unix or Win.
     * @return URL array containing the resource names that comprise the REALM_CLASSPATH.
     */
    public static URL[] getRealmClasspath(String cpProp)
    {
        URL urls[] = null;
        try
        {
            String slash;
            String filePrefix = FILE_PREFIX;
            String classpath = System.getProperty(cpProp);
            String pathSeparator = System.getProperty(PATH_SEPARATOR);
            // Is this a Unix machine?
            if (pathSeparator.compareTo(UNIX_SEPARATOR) == 0)
            {
                // unix requires adding extra forward slash:
                filePrefix += UNIX_SLASH;
                slash = UNIX_SLASH;
            }
            else
            {
                slash = WIN_SLASH;
            }
            log.info(OCLS_NM + ".parseRealmClasspath <" + classpath + ">");
            StringTokenizer st = new StringTokenizer(classpath, pathSeparator, false);
            int size = st.countTokens();
            urls = new URL[size];
            for (int i = 0; i < size; i++)
            {
                String resource = st.nextToken();
                resource = getResource(resource, slash);
                String file = filePrefix + resource;
                urls[i] = new URL(file);
                log.info(OCLS_NM + ".parseRealmClasspath path" + i + " <" + urls[i] + ">");
            }
        }
        catch (java.net.MalformedURLException me)
        {
            String error = OCLS_NM + ".parseRealmClasspath caught MalformedURLException=" + me;
            log.severe(error);
            error = OCLS_NM + ".parseRealmClasspath check your Fortress REALM_CLASSPATH setting.";
            log.severe(error);
        }
        return urls;
    }


    /**
     * Check to see if resource is a folder and if the trailing slash was left off add it.  Otherwise
     * the folder will not be added to REALM_CLASSPATH causing problems with Fortress finding its config folder.
     *
     * @param name
     * @param slash
     * @return
     */
    private static String getResource(String name, String slash)
    {
        File rFile = new File(name);
        if (rFile.isDirectory())
        {
            if (name.lastIndexOf(slash) != (name.length() - 1))
            {
                name += slash;
                log.info(OCLS_NM + ".getResource slash added to dir path");
            }
        }
        return name;
    }
}