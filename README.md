JBoss Server Migration Tool
=================

The JBoss Server Migration Tool is a tool that migrates JBoss Application Servers. It reads the server configuration and properties files for a previous release of WildFly or JBoss EAP, referred to as the source server, and migrates them to the latest release or target server.

Note that the tool at present does not attempt to migrate modules added to the system, such as database driver modules. The target server will need manual modification to install any such modules.

System Requirements
------------

* [Java 8.0 (Java SDK 1.8)](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) or later
* [Apache Maven](http://maven.apache.org/download.cgi)


Build the Server Migration Tool
------------

1. Use the following command to build the tool from source:

        mvn clean install

2. Navigate to the `build/target/` directory and unzip the ` jboss-server-migration-VERSION_NUMBER.zip` file

        unzip  jboss-server-migration-1.0.0.CR1.zip


Run the Server Migration Tool
------------

1. Open a terminal and navigate to the `build/target/jboss-server-migration/` directory.
2. Run the following command.

        For Linux|Mac:   ./server-migration.sh --source SOURCE_SERVER_PATH --target TARGET_SERVER_PATH
        For Windows: server-migration.bat --source SOURCE_SERVER_PATH --target SOURCE_SERVER_PATH

    Replace `SOURCE_SERVER_PATH` with the path to previous version of the server installation that you want to migrate from, for example:  `${user.home}/jboss-eap-6.4/`

    Replace `TARGET_SERVER_PATH` with the path to current version of the server installation that you want the old configuration migrated to, for example:  `${user.home}/jboss-eap-7.0/`
3. When you execute the command, the tool displays a list of the files to be migrated and then prompts you whether you want to continue.

        ----------------------------------------
        ----  JBoss Server Migration Tool  -----
        ----------------------------------------

        Retrieving servers...
        [org.jboss.migration.core.logger] (main) SOURCE server name: EAP, version: 6.4.0.GA.
        [org.jboss.migration.core.logger] (main) TARGET server name: JBoss EAP, version: 7.0.0.GA.
        
        ----------------------------------------------------------
        ----------------------------------------------------------
        
        Server migration starting...
        
        Migrate the source's standalone server?
        yes/no?

    Type `y` or `yes` to continue with the migration.    
4. You are presented with information and prompts similar to the following.

        [org.jboss.migration.core.task.ServerMigrationTask#3] (main) Standalone server migration starting...
        
        [org.jboss.migration.core.task.ServerMigrationTask#5] (main) Retrieving source's standalone configurations...
        [org.jboss.migration.core.task.ServerMigrationTask#5] (main) /path_to_source_server/standalone/configuration/standalone-full-ha.xml
        [org.jboss.migration.core.task.ServerMigrationTask#5] (main) /path_to_source_server/standalone/standalone-full.xml
        [org.jboss.migration.core.task.ServerMigrationTask#5] (main) /path_to_source_server/standalone/standalone-ha.xml
        [org.jboss.migration.core.task.ServerMigrationTask#5] (main) /path_to_source_server/standalone/standalone-osgi.xml
        [org.jboss.migration.core.task.ServerMigrationTask#5] (main) /path_to_source_server/standalone/standalone.xml
        
        Migrate all configurations?
        yes/no?

    Type `y` or `yes` to continue with the migration of all of the standalone server configuration files. Type `n` or `no` to choose the select the files individually.    
5. You are presented with a long list of tasks that were completed for the standalone server migration, and the following prompt.

        INFO  [org.jboss.migration.core.task.ServerMigrationTask#3] (main) Standalone server migration done.
        
        Migrate the source's managed domain?
        yes/no? 
    Type `y` or `yes` to continue with the migration of the managed domain configuration files.    
6. You are presented with the list of available domain configuration files.

        INFO  [org.jboss.migration.core.task.ServerMigrationTask#323] (main) Domain migration starting...
        
        INFO  [org.jboss.migration.core.task.ServerMigrationTask#325] (main) Retrieving source's domain configurations...
        INFO  [org.jboss.migration.core.task.ServerMigrationTask#325] (main) .../configuration/domain.xml
        
        Migrate all configurations?
        yes/no? 

    Type `y` or `yes` to continue with the migration of all of the managed domain configuration files. Type `n` or `no` to choose the select the files individually.
7. You are presented with a long list of tasks that were completed for the managed domain migration, and the following prompt.

        INFO  [org.jboss.migration.core.task.ServerMigrationTask#475] (main) Retrieving source's host configurations...
        INFO  [org.jboss.migration.core.task.ServerMigrationTask#475] (main) .../domain/configuration/host-master.xml
        INFO  [org.jboss.migration.core.task.ServerMigrationTask#475] (main) .../configuration/host-slave.xml
        INFO  [org.jboss.migration.core.task.ServerMigrationTask#475] (main) .../configuration/host.xml
        
        Migrate all configurations?
        yes/no? 

    Type `y` or `yes` to continue with the migration of all of the displayed configuration files. Type `n` or `no` to choose the select the files individually.    
8. The messages are followed by a `Task Summary` report that summarizes the result of the migration of each task.

        ---------------------------------------------------------------------------------------------------
         Task Summary
        ---------------------------------------------------------------------------------------------------
        
         server ................................................................................... SUCCESS
          standalone .............................................................................. SUCCESS
           standalone-configurations .............................................................. SUCCESS
            standalone-configuration(source=.../standalone/configuration/standalone-full-ha.xml) .. SUCCESS
            standalone-configuration(source=.../standalone/configuration/standalone-full.xml) ..... SUCCESS
            standalone-configuration(source=.../standalone/configuration/standalone-ha.xml) ....... SUCCESS
            standalone-configuration(source=.../standalone/configuration/standalone-osgi.xml) ..... SUCCESS
            standalone-configuration(source=.../standalone/configuration/standalone.xml) .......... SUCCESS
          domain .................................................................................. SUCCESS
           domain-configurations .................................................................. SUCCESS
            domain-configuration(source=.../domain/configuration/domain.xml) ...................... SUCCESS
           host-configurations .................................................................... SUCCESS
            host-configuration(source=.../domain/configuration/host-master.xml) ................... SUCCESS
            host-configuration(source=.../domain/configuration/host-slave.xml) .................... SUCCESS
            host-configuration(source=.../domain/configuration/host.xml) .......................... SUCCESS           
9. You should see the following message when it completes.

        -------------------------
        Migration Result: SUCCESS
        -------------------------        
10. Review the information in the `output/` directory.
  * `migration.log`: The log contains detailed information about the modified configuration files and subsystems. For more information, see [Review the Migration Log](#review-the-migration-log).
  * `migration-report.html`: The HTML report is a nicely formatted report showing the detailed results of the migration. For more information, see [Review the Migration Report](#review-the-migration-report).
  * `migration-report.xml`: The XML file is used to format the HTML report. For more information, see [Review the Migration XML File](#review-the-migration-xml-file).
11. Review the updated files in target server installation directory. Note the original configuration and properties files are backed up and now have the suffix `.beforeMigration`.

<a name="review-the-migration-log"/>

Review the Migration Log
-----------------------

The migration log contains detailed information about the modified configuration files and subsystems.

* Informational messages about the tasks performed and migration results begin with `INFO`. Look for message codes beginning with `WFLY` for more detailed information about the migration results. Some informational messages might require action or review. The following are examples of informational messages.

        INFO  [org.jboss.as] (MSC service thread 2-8) WFLYSRV0049: JBoss EAP 7.0.0.GA (WildFly Core 2.1.2.Final-redhat-1) starting
        INFO  [org.jboss.as.remoting] (Controller Boot Thread) WFLYRMT0024: The remoting subsystem is present but no io subsystem was found. An io subsystem was not required when remoting schema 'urn:jboss:domain:remoting:1.2' was current but now is, so a default subsystem is being added.
        INFO  [org.jboss.as.connector] (Controller Boot Thread) WFLYJCA0093: The 'enable' operation is deprecated. Use of the 'add' or 'remove' operations is preferred, or if required the 'write-attribute' operation can used to set the deprecated 'enabled' attribute
        ...
        INFO  [org.jboss.as] (MSC service thread 9-6) WFLYSRV0049: JBoss EAP 7.0.0.GA (WildFly Core 2.1.2.Final-redhat-1) starting
        INFO  [org.jboss.as.controller.management-deprecated] (Controller Boot Thread) WFLYCTL0028: Attribute 'permgen-size' in the resource at address '/host=master/jvm=default' is deprecated, and may be removed in future version. See the attribute description in the output of the read-resource-description operation to learn more about the deprecation.
        INFO  [org.jboss.as.controller.management-deprecated] (Controller Boot Thread) WFLYCTL0028: Attribute 'max-permgen-size' in the resource at address '/host=master/jvm=default' is deprecated, and may be removed in future version. See the attribute description in the output of the read-resource-description operation to learn more about the deprecation.
        INFO  [org.jboss.as.controller.management-deprecated] (Controller Boot Thread) WFLYCTL0028: Attribute 'default-stack' in the resource at address '/profile=ha/subsystem=jgroups' is deprecated, and may be removed in future version. See the attribute description in the output of the read-resource-description operation to learn more about the deprecation.
        INFO  [org.jboss.as.controller.management-deprecated] (Controller Boot Thread) WFLYCTL0028: Attribute 'default-stack' in the resource at address '/profile=full-ha/subsystem=jgroups' is deprecated, and may be removed in future version. See the attribute description in the output of the read-resource-description operation to learn more about the deprecation.

* Error messages, which appear in red in the server console, describe the problem encountered in the migration and often how to resolve it. Most errors require action. The following are examples of an error messages.

        ERROR [org.jboss.as.ejb3] (Controller Boot Thread) WFLYEJB0474: Attribute 'default-clustered-sfsb-cache' is not supported on current version servers; it is only allowed if its value matches 'default-sfsb-cache'. This attribute should be removed.
        ...
        ERROR [org.jboss.as.controller] (main) WFLYCTL0369: Required capabilities are not available:
          org.wildfly.domain.server-group.main-server-group in context 'server-config'; There are no known registration points which can provide this capability.
          org.wildfly.domain.server-group.other-server-group in context 'server-config'; There are no known registration points which can provide this capability.
        ERROR [org.jboss.as.controller] (main) WFLYCTL0369: Required capabilities are not available:
          org.wildfly.domain.server-group.main-server-group in context 'server-config'; There are no known registration points which can provide this capability.
          org.wildfly.domain.server-group.other-server-group in context 'server-config'; There are no known registration points which can provide this capability.
        ERROR [org.jboss.as.controller] (main) WFLYCTL0369: Required capabilities are not available:
          org.wildfly.domain.server-group.main-server-group in context 'server-config'; There are no known registration points which can provide this capability.
          org.wildfly.domain.server-group.other-server-group in context 'server-config'; There are no known registration points which can provide this capability.

    _NOTE:_ If you have any deployments to your old server configuration, you may see error messages similar to the following.

        ERROR [org.jboss.as.server] (Controller Boot Thread) WFLYSRV0057: No deployment content with hash 029f689e292c7cbe680ad330edd50440da51d7a3 is available in the deployment content repository for deployment jboss-mail.war. Because this Host Controller is booting in ADMIN-ONLY mode, boot will be allowed to proceed to provide administrators an opportunity to correct this problem. If this Host Controller were not in ADMIN-ONLY mode this would be a fatal boot failure.

<a name="review-the-migration-report"/>

Review the Migration Report
------------
The `output/migration-report.html` HTML report file is a nicely formatted output detailing the results of the migration. It contains the following sections.

* _Summary_: This section displays the time of the migration, the source and target server releases and paths, and the result of the migration.
* _Environment_: This section describes the reporting environment, including the path of the migration tool.
* _Tasks_: This section summarizes the tasks that were run during the migration. It then provides the details of those tasks in a hierarchical, collapsible, easy to read format. Tasks are grouped by server, and within server, by server type and configuration.


<a name="review-the-migration--xml-file"/>

Review the Migration XML File
-----------------------------

The XML file is used to format the HTML report. This file can be imported into your favorite spreadsheet or other tool where you can manipulate and process the data.