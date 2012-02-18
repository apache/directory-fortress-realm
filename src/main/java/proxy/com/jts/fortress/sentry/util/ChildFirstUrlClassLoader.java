/*
 * Copyright (c) 2009-2012. Joshua Tree Software, LLC.  All Rights Reserved.
 */

package com.jts.fortress.sentry.util;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * This class extends the standard Java URLClassloader and provides a child first lookup for system classpath and is used when
 * JBoss application server is in use.  The child first URLClassloader behaves as one would expect and always looks in child classloader
 * before going to parent to search.  This inversion is needed in JBoss to allow isolation of Fortress code from the server and application classpaths.
 *
 * @author smckinn
 * @created January 13, 2010
 */
public class ChildFirstUrlClassLoader extends URLClassLoader
{
    /**
     * Constructor for the ChildFirstUrlClassLoader object accepts an array of URLs and parent reference.
     *
     * @param urls   Contains a list of value Java classpath entries.
     * @param parent Reference to parent classloader.
     */
    public ChildFirstUrlClassLoader(URL[] urls, ClassLoader parent)
    {
        super(urls, parent);
    }


    /**
     * Constructor for the ChildFirstUrlClassLoader object accepts an array of URLs.
     *
     * @param urls   Contains a list of value Java classpath entries.
     */
    public ChildFirstUrlClassLoader(URL[] urls)
    {
        super(urls);
    }


    /**
     * Description of the Method
     *
     * @param name    Description of the Parameter
     * @param resolve Description of the Parameter
     * @return Description of the Return Value
     * @throws ClassNotFoundException Description of the Exception
     */
    protected Class loadClass(String name, boolean resolve)
        throws ClassNotFoundException
    {

        Class c = findLoadedClass(name);
        if (c == null)
        {
            try
            {
                c = findClass(name);
            }
            catch (ClassNotFoundException ignore)
            {

            }
            if (c == null)
            {
                if (getParent() != null)
                {
                    c = getParent().loadClass(name);
                }
                else
                {
                    c = getSystemClassLoader().loadClass(name);
                }
            }
        }
        if (resolve)
        {
            resolveClass(c);
        }

        return c;
    }


    /**
     * Gets the resource attribute of the ChildFirstUrlClassLoader object
     *
     * @param name Description of the Parameter
     * @return The resource value
     */
    public URL getResource(String name)
    {
        URL url;
        url = findResource(name);
        if (url == null)
        {
            if (getParent() != null)
            {
                url = getParent().getResource(name);
            }
            else
            {
                url = getSystemClassLoader().getResource(name);
            }
        }
        return url;
    }
}

