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

# README for Apache Fortress Realm Context Setup
 * Version 1.0-RC41
 * Apache Fortress Realm Context System Architecture Diagram
 ![Apache Fortress Realm Context System Architecture](images/fortress-realm-system-arch.png "Apache Fortress Realm Context System Architecture")

-------------------------------------------------------------------------------
## Table of Contents

 * Document Overview
 * Tips for first-time users.
 * SECTION 1. Prerequisites.
 * SECTION 2. Prepare the Fortress Realm.
 * SECTION 3. Enable Fortress Realm for Web context.
 * More on the Realm Proxy
___________________________________________________________________________________
## Document Overview

This document contains instructions to enable Apache Fortress Realm for a single Web app context running under Apache Tomcat.  To enable for all apps running, using Tomcat global security option, checkout: [REALM-HOST-SETUP](./REALM-HOST-SETUP.md).

___________________________________________________________________________________
##  Tips for first-time users

 * For a tutorial on how to use Apache Fortress check out the: [10 Minute Guide](http://directory.apache.org/fortress/gen-docs/latest/apidocs/org/apache/directory/fortress/core/doc-files/ten-minute-guide.html).
 * For a tutorial on how to enable a Web application to use Fortress, check out the: [README-ENABLE-FORTRESS](https://github.com/shawnmckinney/wicket-sample/blob/master/README-ENABLE-FORTRESS.md).
 * If you see **FORTRESS_CORE_HOME**, refer to the base package of [directory-fortress-core].
 * If you see **FORTRESS_REALM_HOME**, refer to this packages base folder.
 * If you see **TOMCAT_HOME**, refer to the location of that package's base folder.
 * Questions about this software package should be directed to its mailing list:
   * http://mail-archives.apache.org/mod_mbox/directory-fortress/

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
 * Apache Fortress Core and LDAP server installed per README located in **FORTRESS_CORE_HOME** package.
 * Apache Fortress Realm installed per this package's [README](README.md).

Everything else covered in steps that follow.  Tested on Debian, Centos & Windows machines.

-------------------------------------------------------------------------------
## SECTION 2. Prepare the Fortress Realm

1. Follow instructions in README.txt to build and install fortress realm component.

2. copy fortress-realm-proxy-[version].jar to TOMCAT_HOME/lib/

 ```
 cp FORTRESS_REALM_HOME/proxy/fortress-realm-proxy-[version].jar TOMCAT_HOME/lib
 ```

3. Restart tomcat server instance for changes to take effect.

-------------------------------------------------------------------------------
## SECTION 3. Enable Fortress Realm for Web context

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

 *Fortress Realm follows standard Java EE security semantics.*

4. Add the maven dependencies to the Web app.

 ```
 <dependency>
     <groupId>org.apache.directory</groupId>
     <artifactId>fortress-realm-impl</artifactId>
     <version>${project.version}</version>
     <classifier>classes</classifier>
  </dependency>
  ```

 *Where project.version contains target version, e.g. 1.0-RC41*

5. Add the fortress.properties file to the classpath of the Web app.

 *It contains the coordinates to the target LDAP server.*

 ```
 # This param tells fortress what type of ldap server in use:
 ldap.server.type=apacheds

 # ldap host name
 host=localhost

 # if ApacheDS is listening on
 port=10389

 # If ApacheDS, these credentials are used for read/write to fortress DIT
 admin.user=uid=admin,ou=system
 admin.pw=secret

 # This is min/max settings for admin pool connections:
 min.admin.conn=1
 max.admin.conn=10

 # This node contains more fortress properties stored on behalf of connecting LDAP clients:
 config.realm=DEFAULT
 config.root=ou=Config,dc=example,dc=com

 # Used by application security components:
 perms.cached=true

 # Fortress uses a cache:
 ehcache.config.file=ehcache.xml

 # Default for pool reconnect flag is false:
 enable.pool.reconnect=true
 ```

6. Add two other config files to classpath.

 ```
 cp $FORTRESS_REALM_HOME/conf/echcache.xml $MY_APP_HOME/src/main/resources
 cp $FORTRESS_REALM_HOME/conf/log4j.properties $MY_APP_HOME/src/main/resources
 ```

7. Verify the configuration artifacts are properly staged to your app.
 ```
 x@machine:~/MY_APP_HOME/src/main/resources$ ls -l
 ...
 -rwxrwxr-x 1 x y 5905 Jan 23 12:41 ehcache.xml
 -rw-rw-r-- 1 x y 1161 Jan 23 12:41 fortress.properties
 -rw-rw-r-- 1 x y 1235 Jan 23 12:41 log4j.properties
 ...
 ```
 *Fortress needs all three files.*

8. Redeploy web application to Tomcat.

9. Login to the web application.  Users that successfully authenticate and have activated role(s) listed in auth-constraints have access to all resources matching the url-pattern(s).

10. Verify that realm is operating properly per Tomcat server log:

 ```
 tail -f -n10000 $TOMCAT_HOME/logs/catalina.out
 ...
 org.apache.directory.fortress.realm.tomcat.Tc7AccessMgrProxy J2EE Tomcat7 policy agent initialization successful
 ...
 ```

11. You have enabled security for a single Web app running in Tomcat.  This will enforce declarative authentication and coarse-gained authorization (isUserInRole) checks.  For a look at how to apply more, check out [Apache Fortress Demo End-to-End Security Example](https://github.com/shawnmckinney/apache-fortress-demo).

Realm Usage Notes:
* This automatically enforces authentication and coarse-gained authorization (isUserInRole) checking.
* Repeat steps in this section for each additional app to use Java EE security enforcement.

## More on the Realm Proxy
The fortress realm proxy jar contains a *shim* that uses a URLClassLoader to reach its implementation libs. It prevents the realm impl libs, pulled in as dependency to your web app, from interfering with Tomcat's system classpath thus providing an error free deployment process w/out classloader issues. The realm proxy offers the flexibility for each web app to determine its own version/type of security realm to use, satisfying a variety of requirements related to web hosting and multitenancy.

___________________________________________________________________________________
#### END OF README-CONTEXT-SETUP.md