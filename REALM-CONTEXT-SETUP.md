   Licensed to the Apache Software Foundation (ASF) under one
   or more contributor license agreements.  See the NOTICE file
   distributed with this work for additional information
   regarding copyright ownership.  The ASF licenses this file
   to you under the Apache License, Version 2.0 (the
   "License"); you may not use this file except in compliance
   with the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing,
   software distributed under the License is distributed on an
   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
   KIND, either express or implied.  See the License for the
   specific language governing permissions and limitations
   under the License.

# Apache Fortress Realm Context Setup

 Apache Fortress Realm Version 1.0-RC41 System Architecture Diagram
 ![Apache Fortress Realm System Architecture](images/fortress-realm-system-arch.png "Apache Fortress Realm System Architecture")

This document contains instructions to enable Apache Fortress Realm for a single Web context under Apache Tomcat.  To target enablement for all apps running under the container, checkout: [REALM-HOST-SETUP](./REALM-HOST-SETUP.md).

-------------------------------------------------------------------------------
## Table of Contents

 * SECTION 1. Prerequisites.
 * SECTION 2. Prepare Machine.
 * SECTION 3. Enable Tomcat Realm for Web context.

-------------------------------------------------------------------------------
## SECTION 1. Prerequisites

Minimum hardware requirements:
 * 2 Cores
 * 4GB RAM

Minimum software requirements:
 * Java SDK 7++
 * Apache Tomcat7++
 * git
 * Apache Maven3++
 * Apache Fortress Core installed per README located in that package.
 * LDAP server setup and configured per Apache Fortress Core README.

Everything else covered in steps that follow.  Tested on Debian, Centos & Windows machines.

-------------------------------------------------------------------------------
## SECTION 2. Prepare Machine.

1. Follow instructions in README.txt to build and install fortress realm component.

2. copy fortress-realm-proxy-[version].jar to TOMCAT_HOME/lib/

 ```
 cp [directory-fortress-realm]/proxy/fortress-realm-proxy-[version].jar TOMCAT_HOME/lib
 ```

 Where [directory-fortress-realm] is base folder of this source package.

3. Restart tomcat server instance for changes to take effect.

-------------------------------------------------------------------------------
## SECTION 3. Enable Tomcat Realm for Web context

1. Add a context.xml file to the META-INF folder of target web app.

2. Add the following:
 ```
 <Context path="/myappcontext" reloadable="true">
    <Realm className="org.apache.directory.fortress.realm.tomcat.Tc7AccessMgrProxy"
           debug="0"
           resourceName="UserDatabase"
           defaultRoles=""
           containerType="TomcatContext"
           realmClasspath=""
            />
 </Context>
 ```

 Where *myappcontext* is the web context for your web application.

3. Add security constraints to target web.xml:
 ```
  ...
  <security-constraint>
      <display-name>Commander Security Constraint</display-name>
      <web-resource-collection>
          <web-resource-name>Protected Area</web-resource-name>
          <!-- Define the context-relative URL(s) to be protected -->
          <url-pattern>/*</url-pattern>
      </web-resource-collection>
      <auth-constraint>
          <!-- Anyone with one of the listed roles may access this area -->
          <role-name>MY_ROLE_NAME</role-name>
          ...
      </auth-constraint>
  </security-constraint>

  <!-- Example of HTTP Basic Authentication Setup. -->
  <login-config>
      <auth-method>BASIC</auth-method>
      <realm-name>FortressSecurityRealm</realm-name>
  </login-config>

  <!-- Security roles referenced by this web application -->
  <security-role>
      <role-name>MY_ROLE_NAME</role-name>
  </security-role>
  ...
 ```

 * Note:  Fortress Realm follows standard Java EE security semantics.  The above is a very simple example. For more info on how Java EE security is used: http://docs.oracle.com/javaee/6/tutorial/doc/bnbwj.html

4. Redeploy web application to Tomcat.

5. Login to the web application.  Users that successfully authenticate and have activated role(s) listed in auth-constraints have access to all resources matching the url-pattern(s).

6. View the Tomcat server logs to ensure there are no errors.

7. Verify that fortress realm is operating properly by viewing the following in catalina.log:

 ```
 org.apache.directory.fortress.realm.tomcat.Tc7AccessMgrProxy J2EE Tomcat7 policy agent initialization successful
 ```
___________________________________________________________________________________
#### END OF README-CONTEXT-SETUP.md