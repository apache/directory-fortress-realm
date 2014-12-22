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
package org.apache.directory.fortress.realm.tomcat;

import java.io.Serializable;
import java.security.Principal;
import java.util.HashMap;

/**
 * Contains the Fortress RBAC session that has been created on behalf of an end user who has
 * signed onto Tomcat system.  The session contains the User's active roles and other security attributes.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class TcPrincipal implements Principal, Serializable
{
    /** Default serialVersionUID */
    private static final long serialVersionUID = 1L;
    
    /** The key when we store a serialiazed version of a Session into the context */
    public static final String SERIALIZED = "SERIALIZED";
    
    /** The context storing the session */
    private HashMap<String, Object> context;
    
    /** The userId */
    private String name;


    /**
     * Constructor for the TcPrincipal object.  Accepts a HashMap which
     * contains the Fortress session.
     *
     * @param name    contains the userId of User who signed onto Tomcat.
     * @param context Instantiated HashMap that contains the User's Fortress session data.
     */
    public TcPrincipal( String name, HashMap<String, Object> context )
    {
        if ( ( context == null ) || ( name == null ) )
        {
            throw new IllegalArgumentException( TcPrincipal.class.getName() + " Null Map passed to constructor" );
        }
        
        this.context = context;
        this.name = name;
    }


    /**
     * Return the HashMap to the caller. This HashMap contains the User's Fortress session data.
     *
     * @return HashMap reference to security session data.
     */
    public final HashMap<String, Object> getContext()
    {
        return context;
    }


    /**
     * Return the userId of the end User who has signed onto Tomcat and is represented by this principal object.
     *
     * @return Contains the end userId.
     */
    public final String getName()
    {
        return name;
    }


    /**
     * Set a new HashMap reference into this Principal object.
     *
     * @param context HashMap reference to security session data.
     */
    public final void setContext( HashMap<String, Object> context )
    {
        this.context = context;
    }


    /**
     * This method returns a string containing the serialized instance of this object.
     *
     * @return Return this object in serialized format.
     */
    public final String toString()
    {
        String ser = null;
        HashMap<String, Object> context = getContext();
        
        if ( context != null )
        {
            ser = (String)context.get( SERIALIZED );
        }
        
        return ser;
    }
    

    /**
     * Determine if the caller supplied a reference to a security Principal that is equal to the current value.
     *
     * @param o Contains reference to the Principal.
     * @return true if the userId on both Principal objects is equal, false otherwise.
     */
    public final boolean equals( Object o )
    {
        if ( o == null )
        {
            return false;
        }
        
        if ( this == o )
        {
            return true;
        }
        
        if ( !( o instanceof TcPrincipal ) )
        {
            return false;
        }
        
        TcPrincipal that = ( TcPrincipal ) o;

        if ( this.getName().equals( that.getName() ) )
        {
            return true;
        }
        
        return false;
    }


    /**
     * Compute the hashcode for the current userId asserted into this Principal object.
     *
     * @return Description of the Return Value
     */
    public final int hashCode()
    {
        return name.hashCode();
    }
}

