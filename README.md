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

# README for Apache Fortress Realm
 * Version 1.0-RC41

-------------------------------------------------------------------------------
## Table of Contents

 * Document Overview
 * Tips for first-time users.
 * SECTION 1. Prerequisites.
 * SECTION 2. Download & Build.
 * SECTION 3. Instructions to Use

___________________________________________________________________________________
## Document Overview

This document contains instructions to download, build, and install Apache Fortress Realm component.

___________________________________________________________________________________
##  Tips for first-time users

 * For a tutorial on how to use Apache Fortress check out the: [10 Minute Guide](http://directory.apache.org/fortress/gen-docs/latest/apidocs/org/apache/directory/fortress/core/doc-files/ten-minute-guide.html).
 * For a tutorial on how to enable a Web application to use Fortress, check out the: [README-ENABLE-FORTRESS](https://github.com/shawnmckinney/wicket-sample/blob/master/README-ENABLE-FORTRESS.md).
 * Questions about this software package should be directed to its mailing list:
   * http://mail-archives.apache.org/mod_mbox/directory-fortress/

-------------------------------------------------------------------------------
## SECTION 1. Prerequisites

Minimum hardware requirements:
 * 2 Cores
 * 4GB RAM

Minimum software requirements:
 * Java SDK 7++
 * git
 * Apache Maven3++
 * Apache Fortress Core installed per README located in the same package.

Everything else covered in steps that follow.  Tested on Debian, Centos & Windows machines.

-------------------------------------------------------------------------------
## SECTION 2. Download & Build

1. Build the source.
 ```
 git clone https://git-wip-us.apache.org/repos/asf/directory-fortress-realm.git
 cd directory-fortress-realm
 mvn clean install
 ```

2. Now build the javadoc:

 ```
 mvn javadoc:javadoc
 ```

 If using java 8, add this param to the pom.xml:
 ```
 <plugin>
    ...
    <artifactId>maven-javadoc-plugin</artifactId>
    <configuration>
        <additionalparam>-Xdoclint:none</additionalparam>
        ...
    </configuration>
 </plugin>
 ```

3. View the generated document here: [./target/site/apidocs/overview-summary.html](./target/site/apidocs/overview-summary.html).

-------------------------------------------------------------------------------
## SECTION 3. Instructions to Use

There are two ways in which to use the Apache Fortress Realm.  The first, pulls extra dependencies into the Web app.  The second, externalizes the dependencies away from the Web app into a separate folder.

### Option 1. For Single Web Context
 *enable Java EE security for a single web app running under Tomcat*
 * Follow these instructions: [REALM-CONTEXT-SETUP](./REALM-CONTEXT-SETUP.md).

 ![Apache Fortress Realm System Architecture](images/fortress-realm-system-arch.png "Apache Fortress Realm System Architecture")

-------------------------------------------------------------------------------

### Option 2. For Global Security
 *enable Java EE security for all web apps running under Tomcat*
 * Follow these instructions: [REALM-HOST-SETUP](./REALM-HOST-SETUP.md).

  ![Apache Fortress Realm Host System Architecture](images/fortress-realm-host-system-arch.png "Apache Fortress Realm Host System Architecture")

___________________________________________________________________________________
#### END OF README