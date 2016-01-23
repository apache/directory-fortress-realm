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

# Apache Fortress Realm Host Setup

This document describes how to enable Fortress Realm to provide security for all apps running inside a Tomcat server virtual host.  To target setup for a single web application, and not the entire container, follow the Tomcat instructions in the [REALM-CONTEXT-SETUP](./REALM-CONTEXT-SETUP.md).

-------------------------------------------------------------------------------
## Table of Contents

 * SECTION 1. Prerequisites.
 * SECTION 2. Prepare Machine.
 * SECTION 3. Enable Tomcat Realm for Host.
 * SECTION 4. Test with Tomcat Manager App (Optional).
 * SECTION 5. Common troubleshooting tips.

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

3. Configure the Fortress Realm for target LDAP server.

 Copy the fortress.properties, created during [directory-fortress-core] setup, to this package's config folder.
 ```
 cp [directory-fortress-core]/config/fortress.properties [directory-fortress-realm]/conf
 ```

 Where [directory-fortress-core] is base folder of the fortress core source package and [directory-fortress-realm] is this package's home folder.

4. Restart Tomcat server for changes to take effect.

_________________________________________________________________________________
## SECTION 3. Enable Tomcat Realm for Host

1. Edit TOMCAT_HOME/conf/server.xml

2. Comment out entry to UserDatabase:
 ```
 <!--Resource name="UserDatabase" auth="Container"
    type="org.apache.catalina.UserDatabase"
    description="User database that can be updated and saved"
    factory="org.apache.catalina.users.MemoryUserDatabaseFactory"
    pathname="conf/tomcat-users.xml" /-->
 ```

3. Add the following to the same file:
 ```
 <Realm className="org.apache.directory.fortress.realm.tomcat.Tc7AccessMgrProxy"
     debug="0"
     resourceName="UserDatabase"
	 containerType="Tomcat7"
     realmClasspath="[directory-fortress-realm]/conf:[directory-fortress-realm]/impl/target/fortress-realm-impl-uber-[version].jar"
     defaultRoles=""
	/>
 ```

 Where [directory-fortress-realm] is base folder of this package.

4. restart tomcat

5. view the server logs to ensure there are no errors.

6. verify that fortress realm started successfully by viewing following message in catalina.log:
 ```
 org.apache.directory.fortress.realm.tomcat.Tc7AccessMgrProxy J2EE Tomcat7 policy agent initialization successful
 ```
_________________________________________________________________________________
## SECTION 4. Test with Tomcat Manager App (Optional)

This section provides instructions for using the Tomcat Manager application to test Apache Fortress Realm.

1. Enable Tomcat Manager application. note: check the Tomcat documentation on how to do this.

2. Verify/enable role name. Edit TOMCAT_HOME/webapps/manager/WEB-INF/web.xml

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
## SECTION 5. Common troubleshooting tips

1. Server can't find config files (realmClasspath="[directory-fortress-realm]/conf")

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

3. Server can't find binaries (realmClasspath="[directory-fortress-realm]/impl/target/fortress-realm-impl-uber-[version].jar")
 ```
 Apr 22, 2011 10:22:25 PM org.apache.directory.fortress.realm.tomcat.Tc7AccessMgrProxy initialize
 SEVERE: Fortress Tomcat Realm.initialize java.lang.ClassNotFoundException=java.lang.ClassNotFoundException: org.apache.directory.fortress.realm.tomcat.TcAccessMgrImpl
 Apr 22, 2011 10:22:25 PM org.apache.directory.fortress.realm.tomcat.TcAccessMgrProxy start
 SEVERE: Fortress Tomcat Realm.start caught Exception=java.lang.RuntimeException: Fortress Tomcat Realm.initialize java.lang.ClassNotFoundException=java.lang.ClassNotFoundException: org.apache.directory.fortress.realm.tomcat.TcAccessMgrImpl
 java.lang.RuntimeException: Fortress Tomcat Realm.initialize java.lang.ClassNotFoundException=java.lang.ClassNotFoundException: org.apache.directory.fortress.realm.tomcat.TcAccessMgrImpl
        at org.apache.directory.fortress.realm.tomcat.TcAccessMgrProxy.initialize(TcAccessMgrProxy.java:118)
 ```

 ACTION: Ensure realm classpath points fortress-realm-impl-uber jar, i.e. [fortress-realm]/proxy/target/fortress-realm-impl-uber-[version].jar.

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

___________________________________________________________________________________
#### END OF README-HOST-SETUP.md