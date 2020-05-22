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

# README for Apache Fortress Realm Host Setup
 * Version 2.0.5
 * Apache Fortress Realm Host System Architecture Diagram
 ![Apache Fortress Realm Host System Architecture](images/fortress-realm-host-system-arch.png "Apache Fortress Realm Host System Architecture")


-------------------------------------------------------------------------------
## Table of Contents

 * Document Overview
 * Tips for first-time users.
 * SECTION 1. Prerequisites.
 * SECTION 2. Prepare Tomcat for the Global Realm.
 * SECTION 3. Enable Tomcat Global Security.
 * SECTION 4. Enable Web App to use the Global Realm.
 * SECTION 5. Test with Tomcat Manager App (Optional).
 * SECTION 6. Common troubleshooting tips.
 * More on the Realm Proxy

___________________________________________________________________________________
## Document Overview

This document describes how to enable global security inside Tomcat to use the Fortress Realm.  To target setup for a single web application, w/out global security enabled, follow the instructions in the [REALM-CONTEXT-SETUP](./REALM-CONTEXT-SETUP.md) instead.

___________________________________________________________________________________
##  Tips for first-time users

 * For a tutorial on how to use Apache Fortress check out the quickstarts in directory-fortress-core package.
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
 * Java SDK 8++
 * Apache Tomcat7++
 * git
 * Apache Maven3++
 * Apache Fortress Core **Download & Install** in **FORTRESS_CORE_HOME** package **README.md**.
 * Apache Fortress Core **Options for using Apache Fortress and LDAP server** in **FORTRESS_CORE_HOME** package **README.md**.
 * Apache Fortress Realm **Download & Install** in this package's [README](README.md).

Everything else covered in steps that follow.  Tested on Debian, Centos & Windows machines.

-------------------------------------------------------------------------------
## SECTION 2. Prepare Tomcat for the Global Realm

1. Stage the Fortress Realm Proxy jar.

 Copy fortress-realm-proxy-[version].jar to TOMCAT_HOME/lib/ folder:

 ```
 cp $FORTRESS_REALM_HOME/proxy/fortress-realm-proxy-[version].jar $TOMCAT_HOME/lib
 ```

2. Configure the Fortress Realm for target LDAP server usage.

 Copy the **fortress.properties**, created during **FORTRESS_CORE_HOME** setup, to this package's conf folder.
 ```
 cp $FORTRESS_CORE_HOME/config/fortress.properties $FORTRESS_REALM_HOME/conf
 ```

3. Verify a match for target LDAP server coordinates.
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

4. Verify the configuration artifacts are present.
 ```
 x@machine:~/FORTRESS_REALM_HOME/conf$ ls -l
 ...
 -rwxrwxr-x 1 x y 5905 Jan 23 12:41 ehcache.xml
 -rw-rw-r-- 1 x y 1161 Jan 23 12:41 fortress.properties
 -rw-rw-r-- 1 x y 1235 Jan 23 12:41 log4j.properties
 ...
 ```

 *Fortress needs all three files.*

_________________________________________________________________________________
## SECTION 3. Enable Tomcat Global Security

1. Edit **TOMCAT_HOME**/conf/server.xml:
 ```
 vi $TOMCAT_HOME/conf/server.xml
 ```

2. Comment out entry to UserDatabase:
 ```
 <!--Resource name="UserDatabase" auth="Container"
    type="org.apache.catalina.UserDatabase"
    description="User database that can be updated and saved"
    factory="org.apache.catalina.users.MemoryUserDatabaseFactory"
    pathname="conf/tomcat-users.xml" /-->
 ```

3. Enable Tomcat global security to use the Fortress Realm.
 Add the following to the same file:
 ```
 <Realm className="org.apache.directory.fortress.realm.tomcat.Tc7AccessMgrProxy"
     debug="0"
     resourceName="UserDatabase"
	 containerType="Tomcat7"
     realmClasspath="$FORTRESS_REALM_HOME/conf:$FORTRESS_REALM_HOME/impl/target/fortress-realm-impl-uber-[version].jar"
     defaultRoles=""
     contextId="HOME"
	/>
 ```

 *Here the FORTRESS_REALM_HOME conf folder and implementation jar are being declared to Tomcat server.  Ensure that you stage these files outside of TOMCAT_HOME to ensure remaining clear of Tomcat's system classpath.*

4. Save and exit the editor.

5. Restart Tomcat server to enable changes to take effect.

6. Verify it worked:

 ```
 tail -f -n10000 $TOMCAT_HOME/logs/catalina.out
 ...
 org.apache.directory.fortress.realm.tomcat.Tc7AccessMgrProxy J2EE Tomcat7 policy agent initialization successful
 ...
 ```

_________________________________________________________________________________
## SECTION 4. Enable Web App to use the Global Realm

1. Edit the web app's deployment descriptor:
 ```
 vi $MY_APP_HOME/src/main/webapp/WEB-INF/web.xml
 ```

2. Add security constraints to the file:
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

3. Redeploy web app to Tomcat.

4. Login to the web app.  Users that successfully authenticate and have role(s) listed in **auth-constraint** may access resources matching the **url-pattern**.

5. Verify that the fortress realm is operating properly by viewing the Tomcat server log:

 ```
 tail -f -n10000 $TOMCAT_HOME/logs/catalina.out
 ...
 org.apache.directory.fortress.realm.tomcat.Tc7AccessMgrProxy J2EE Tomcat7 policy agent initialization successful
 ...
 ```

Realm Usage Notes:
* Global security is now enabled under Tomcat.
* This automatically enforces authentication and coarse-gained authorization (isUserInRole) checking.
* Repeat steps in this section for each additional app to use Java EE security enforcement.

_________________________________________________________________________________
## SECTION 5. Test with Tomcat Manager App (Optional)

This section provides instructions for using the Tomcat Manager application to test Apache Fortress Realm.

1. Enable Tomcat Manager application. note: check the Tomcat documentation on how to do this.

2. Verify/enable role name. Edit **TOMCAT_HOME**/webapps/manager/WEB-INF/web.xml

 ```
 <!-- Security roles referenced by this web application --/>
 <security-role/>
     <description/>
     The role that is required to log in to the Manager Application
     </description/>
     <role-name/>manager</role-name/>
 </security-role/>
 ```

3. Add and assign that role to a user.

4. Test logon onto the Tomcat Manager app.

    Enter URL to manager web app:
    http://localhost:8080/manager/html

    Enter creds (tcmanager, password) into basic logon form
    Verify authentication/authorization success to web app.

_________________________________________________________________________________
## SECTION 6. Common troubleshooting tips

1. Server can't find config files (realmClasspath=**FORTRESS_REALM_HOME**/conf")

 ```
 Jul 15, 2011 8:21:16 PM us.jts.sentry.tomcat.Tc7AccessMgrProxy initialize
 INFO: us.jts.sentry.tomcat.Tc7AccessMgrProxy.initialize - instantiate policy agent name: us.jts.sentry.tomcat.TcAccessMgrImpl
 2011-07-15 20:21:17,053 (FATAL) us.jts.configuration.Config static init: Error, null configuration file: fortress.properties
 Jul 15, 2011 8:21:17 PM us.jts.sentry.tomcat.Tc7AccessMgrProxy startInternal
 SEVERE: us.jts.sentry.tomcat.Tc7AccessMgrProxy.startInternal caught Throwable=java.lang.ExceptionInInitializerError
 java.lang.ExceptionInInitializerError
        at us.jts.sentry.J2eePolicyMgrFactory.<clinit>(J2eePolicyMgrFactory.java:32)
        at us.jts.sentry.tomcat.TcAccessMgrImpl.<init>(TcAccessMgrImpl.java:35)
        at sun.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
        at sun.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:39)
        at sun.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:27)
        at java.lang.reflect.Constructor.newInstance(Constructor.java:513)
        at java.lang.Class.newInstance0(Class.java:355)
        at java.lang.Class.newInstance(Class.java:308)
        at us.jts.sentry.tomcat.Tc7AccessMgrProxy.initialize(Tc7AccessMgrProxy.java:112)
        at us.jts.sentry.tomcat.Tc7AccessMgrProxy.startInternal(Tc7AccessMgrProxy.java:236)
        at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:145)
        at org.apache.catalina.core.ContainerBase.startInternal(ContainerBase.java:1026)
        at org.apache.catalina.core.StandardEngine.startInternal(StandardEngine.java:291)
        at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:145)
        at org.apache.catalina.core.StandardService.startInternal(StandardService.java:443)
        at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:145)
        at org.apache.catalina.core.StandardServer.startInternal(StandardServer.java:727)
        at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:145)
        at org.apache.catalina.startup.Catalina.start(Catalina.java:620)
        at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
        at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
        at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
        at java.lang.reflect.Method.invoke(Method.java:597)
        at org.apache.catalina.startup.Bootstrap.start(Bootstrap.java:303)
        at org.apache.catalina.startup.Bootstrap.main(Bootstrap.java:431)
 Caused by: java.lang.RuntimeException: us.jts.configuration.Config static init: Error, null configuration file: fortress.properties
        at us.jts.configuration.Config.<clinit>(Config.java:51)
        ... 25 more
 ```

 ACTION: Ensure realm classpath points to Fortress configuration folder that contains fortress.properties config file.

2. Server can't find proxy jar (Realm className="org.apache.directory.fortress.realm.tomcat.Tc7AccessMgrProxy")
 ```
 INFO: The APR based Apache Tomcat Native library which allows optimal performance in production environments was not found on the java.library.path...
 Apr 22, 2011 10:24:04 PM org.apache.tomcat.util.digester.Digester startElement
 SEVERE: Begin event threw exception
 java.lang.ClassNotFoundException: org.apache.directory.fortress.realm.tomcat.Tc7AccessMgrProxy
 ```

 ACTION: Ensure fortress-realm-proxy jar copied to TOMCAT_HOME/lib folder.

3. Server can't find binaries (realmClasspath="**FORTRESS_REALM_HOME**/impl/target/fortress-realm-impl-uber-[version].jar")
 ```
 Apr 22, 2011 10:22:25 PM org.apache.directory.fortress.realm.tomcat.Tc7AccessMgrProxy initialize
 SEVERE: Fortress Tomcat Realm.initialize java.lang.ClassNotFoundException=java.lang.ClassNotFoundException: org.apache.directory.fortress.realm.tomcat.TcAccessMgrImpl
 Apr 22, 2011 10:22:25 PM org.apache.directory.fortress.realm.tomcat.TcAccessMgrProxy start
 SEVERE: Fortress Tomcat Realm.start caught Exception=java.lang.RuntimeException: Fortress Tomcat Realm.initialize java.lang.ClassNotFoundException=java.lang.ClassNotFoundException: org.apache.directory.fortress.realm.tomcat.TcAccessMgrImpl
 java.lang.RuntimeException: Fortress Tomcat Realm.initialize java.lang.ClassNotFoundException=java.lang.ClassNotFoundException: org.apache.directory.fortress.realm.tomcat.TcAccessMgrImpl
        at org.apache.directory.fortress.realm.tomcat.TcAccessMgrProxy.initialize(TcAccessMgrProxy.java:118)
 ```

 ACTION: Ensure realm classpath points fortress-realm-impl-uber jar, i.e. **FORTRESS_REALM_HOME**/impl/target/fortress-realm-impl-uber-[version].jar.

4. Incompatible Tomcat Proxy jar loaded for Tomcat 6 and before.

 The Fortress Realm requires Tomcat version's 7 and beyond.  If you are running Tomcat 4, 5 or 6 and see error that looks like this:

 ```
 SEVERE: An exception or error occurred in the container during the request processing
 java.lang.RuntimeException: org.apache.directory.fortress.realm.tomcat.Tc7AccessMgrProxyauthenticate detected Fortress Tomcat7 Realm not initialized correctly.  Check your Fortress Realm configuration
        at org.apache.directory.fortress.realm.tomcat.Tc7AccessMgrProxy.authenticate(Tc7AccessMgrProxy.java:161)
        at org.apache.catalina.authenticator.FormAuthenticator.authenticate(FormAuthenticator.java:259)
        at org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:449)
        at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:127)
        at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:102)
        at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:109)
        at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:298)
        at org.apache.coyote.http11.Http11Processor.process(Http11Processor.java:852)
        at org.apache.coyote.http11.Http11Protocol$Http11ConnectionHandler.process(Http11Protocol.java:588)
        at org.apache.tomcat.util.net.JIoEndpoint$Worker.run(JIoEndpoint.java:489)
        at java.lang.Thread.run(Thread.java:662)
 ```

 ACTION: Install and use Tomcat version 7 and later in your target machine.

## More on the Realm Proxy
The fortress realm proxy jar contains a *shim* that uses a URLClassLoader to reach its implementation libs. It prevents the realm impl libs, contained within this package, from interfering with Tomcat's system classpath thus providing an error free deployment process w/out classloader issues. The realm proxy offers the flexibility for each web app to determine its own version/type of security realm to use, satisfying a variety of requirements related to web hosting and multitenancy.

___________________________________________________________________________________
#### END OF README-HOST-SETUP.md
