JBoss Server Migration Tool
=================

The JBoss Server Migration Tool is a tool that migrates JBoss Application Servers. It reads the server configurations files for a previous release of WildFly or JBoss EAP, referred to as the source server, and migrates them to the latest release or target server. Besides the configurations, the tool is also capable of migrating modules, deployments and other resources found in the source server.

System Requirements
------------

* [Java 8.0 (Java SDK 1.8)](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) or later
* [Apache Maven](http://maven.apache.org/download.cgi)

Build the Server Migration Tool
------------

1. Use the following command to build the tool from source:

        mvn clean install

Run the Server Migration Tool (Standalone Build)
------------

1. Navigate to the `dist/standalone/target/` directory and unzip the ` jboss-server-migration-VERSION_NUMBER.zip` file

        unzip jboss-server-migration-1.8.0.Final-SNAPSHOT.zip
2. Open a terminal and navigate to the `dist/standalone/target/jboss-server-migration/` directory.
3. Run the following command.

        For Linux|Mac:   ./jboss-server-migration.sh -s SOURCE_SERVER_PATH -t TARGET_SERVER_PATH
        For Windows: jboss-server-migration.bat -s SOURCE_SERVER_PATH -t TARGET_SERVER_PATH

    Replace `SOURCE_SERVER_PATH` with the path to previous version of the server installation that you want to migrate from, for example:  `${user.home}/wildfly-11.0.0.Final/`

    Replace `TARGET_SERVER_PATH` with the path to current version of the server installation that you want the old configuration migrated to, for example:  `${user.home}/wildfly-16.0.0.Final/`
3. When you execute the command, the tool identifies the source and target servers from provided paths, and starts the server migration.
        ----------------------------------------------------------
        ----  JBoss Server Migration Tool  -----------------------
        ----------------------------------------------------------
        
        Retrieving servers...
         INFO  SOURCE server name: WildFly Full, version: 11.0.0.Final.
         INFO  TARGET server name: WildFly Full, version: 18.0.0.Final.
        
        ----------------------------------------------------------
        ----------------------------------------------------------
        
        Server migration starting...
4. Each server migration consists of several migration tasks, which may require or not user interaction.
        INFO  --- Migrating modules requested by environment...
        
        INFO  Module cmtool.module1:main migrated.
        
        INFO  --- Migrating standalone server...
        
        INFO  No source's standalone content found to migrate.
        
        INFO  Source's standalone configurations found: [standalone.xml]
        
        INFO  Migrating standalone configuration standalone.xml...
        INFO  Subsystem infinispan updated.
        INFO  Subsystem undertow updated.
        INFO  Security realms migrated.
        INFO  Non-persistent deployments found in standalone/deployments: [cmtool-helloworld5.war, cmtool-helloworld6.war]
        INFO  Non-persistent deployment cmtool-helloworld5.war migrated.
        INFO  Non-persistent deployment cmtool-helloworld6.war migrated.
        
        INFO  --- Migrating managed domain...
        
        INFO  No source's domain content found to migrate.
        
        INFO  Source's domain configurations found: [domain.xml]
        
        INFO  Migrating domain configuration domain.xml...
        INFO  Subsystem infinispan updated.
        INFO  Subsystem undertow updated.
        
        INFO  Source's host configurations found: [host.xml]
        
        INFO  Migrating host configuration host.xml...
        INFO  Migrating host master...
        INFO  Security realms migrated.
8. Once the server migration is done a `Task Summary` report will be shown, that summarizes the results of migration tasks.
        Server migration done.
        
         INFO  
        ----------------------------------------------------------------------------------------------
         Task Summary
        ----------------------------------------------------------------------------------------------
        
        server .............................................................................. SUCCESS
         modules.migrate-modules-requested-by-user .......................................... SUCCESS
          modules.migrate-modules-requested-by-user.migrate-module(id=cmtool.module1:main) .. SUCCESS
         standalone ......................................................................... SUCCESS
          standalone-configurations ......................................................... SUCCESS
           standalone-configuration(source=standalone.xml) .................................. SUCCESS
         domain ............................................................................. SUCCESS
          domain-configurations ............................................................. SUCCESS
           domain-configuration(source=domain.xml) .......................................... SUCCESS
          host-configurations ............................................................... SUCCESS
           host-configuration(source=host.xml) .............................................. SUCCESS
9. You should see the following message when it completes.
        ----------------------------------------------------------------------------------------------
         Migration Result: SUCCESS
        ----------------------------------------------------------------------------------------------
10. Additional reports may be found in the `reports/` directory.
  * `migration-report.html`: The HTML report is a nicely formatted report showing the detailed results of the migration.
  * `migration-report.xml`: The XML file includes all migration data collected.
11. File log(s) may be found in the `logs/` directory.
  * `migration.log`: The log contains detailed information about the server migration execution.
  
Development
------------

* [Issue Tracker](https://issues.jboss.org/browse/CMTOOL)