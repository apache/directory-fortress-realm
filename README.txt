#
#   Licensed to the Apache Software Foundation (ASF) under one
#   or more contributor license agreements.  See the NOTICE file
#   distributed with this work for additional information
#   regarding copyright ownership.  The ASF licenses this file
#   to you under the Apache License, Version 2.0 (the
#   "License"); you may not use this file except in compliance
#   with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing,
#   software distributed under the License is distributed on an
#   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#   KIND, either express or implied.  See the License for the
#   specific language governing permissions and limitations
#   under the License.
#
#
#
___________________________________________________________________________________
###################################################################################
README for Fortress Realm Application Installation
Last updated: February 5, 2015
___________________________________________________________________________________
###################################################################################
# SECTION 0.  Prerequisites for Fortress Realm installation
###################################################################################
a. Internet access to retrieve source code from Apache Fortress Rest GIT and binary dependencies from online Maven repo.

b. Java SDK Version 7 or beyond installed to target environment

c. Apache Maven installed to target environment
_________________________________________________________________________________
###################################################################################
# SECTION 1:  Instructions to clone source from Fortress Realm Git Repo:
###################################################################################

a. Clone the directory-fortress-realm from apache git repo:
# git clone https://git-wip-us.apache.org/repos/asf/directory-fortress-realm.git

b. Change directory to package home:
# cd directory-fortress-realm/
___________________________________________________________________________________
###################################################################################
# SECTION 2:  Instructions to install Fortress Realm
###################################################################################

a. Open a command prompt on target machine in the root folder of the directory-fortress-realm package

b. Set java  and maven home

c. Run maven install:
# mvn clean install

e. Build the javadoc:
# mvn javadoc:javadoc

javadoc note: if using java 8, add this param to the pom.xml:
<plugin>
    ...
    <artifactId>maven-javadoc-plugin</artifactId>
    <configuration>
        <additionalparam>-Xdoclint:none</additionalparam>
        ...
    </configuration>
</plugin>

f. To view Fortress Realm Setup Notes for Tomcat Container point your browser here:
file:///[directory-fortress-realm]/target/site/apidocs/org/apache/directory/fortress/realm/tomcat/package-summary.html

(where [directory-fortress-realm] is location of current package)

___________________________________________________________________________________
###################################################################################
# SECTION 3:  Instructions to use Fortress Realm
###################################################################################

a. For a single web application context under Tomcat: REALM-CONTEXT-SETUP.txt

b. For all web application contexts under Tomcat: REALM-HOST-SETUP.txt