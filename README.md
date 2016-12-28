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

        unzip  jboss-server-migration-1.0.0.Beta1.zip


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
        INFO  [org.jboss.migration.core.logger] (main) SOURCE server name: EAP, version: 6.4.0.GA.
        INFO  [org.jboss.migration.core.logger] (main) TARGET server name: JBoss EAP, version: 7.0.0.GA.

        ----------------------------------------
        ----------------------------------------
        Server migration starting...
        INFO  [org.jboss.migration.core.ServerMigrationTask#3] (main) Retrieving source's standalone server config files...
        INFO  [org.jboss.migration.core.ServerMigrationTask#3] (main) /home/username/tools/jboss-eap-6.4/standalone/configuration/standalone_xml_history/standalone-full.initial.xml

        Migrate all configurations?
        yes/no?
    Enter `y` to continue with the migration.
5. The tool reports on the progress throughout the migration. You should see the following message when it completes.

        Migration Result: SUCCESS
6. Review the information in the output log. The log contains detailed information about the modified configuration files and subsystems.

   * Informational messages report information about the conversion of the named file, for example:

            INFO  [org.jboss.as.remoting] (Controller Boot Thread) WFLYRMT0024: The remoting subsystem is present but no io subsystem was found. An io subsystem was not required when remoting schema 'urn:jboss:domain:remoting:1.2' was current but now is, so a default subsystem is being added.
            INFO  [org.jboss.as.connector] (Controller Boot Thread) WFLYJCA0093: The 'enable' operation is deprecated. Use of the 'add' or 'remove' operations is preferred, or if required the 'write-attribute' operation can used to set the deprecated 'enabled' attribute
            INFO  [org.jboss.as.controller.management-deprecated] (Controller Boot Thread) WFLYCTL0028: Attribute 'default-clustered-sfsb-cache' in the resource at address '/subsystem=ejb3' is deprecated, and may be removed in future version. See the attribute description in the output of the read-resource-description operation to learn more about the deprecation.
            INFO  [org.jboss.as.controller.management-deprecated] (Controller Boot Thread) WFLYCTL0028: Attribute 'default-stack' in the resource at address '/subsystem=jgroups' is deprecated, and may be removed in future version. See the attribute description in the output of the read-resource-description operation to learn more about the deprecation.

   * Error messages appear in red and describe the problem encountered in the migration and often how to resolve it. This error requires action.

            ERROR [org.jboss.as.ejb3] (Controller Boot Thread) WFLYEJB0474: Attribute 'default-clustered-sfsb-cache' is not supported on current version servers; it is only allowed if its value matches 'default-sfsb-cache'. This attribute should be removed.

    _NOTE:_ If you have any deployments to your old server configuration, you may see error messages similar to the following.

          ERROR [org.jboss.as.server] (Controller Boot Thread) WFLYSRV0057: No deployment content with hash 029f689e292c7cbe680ad330edd50440da51d7a3 is available in the deployment content repository for deployment jboss-mail.war. Because this Host Controller is booting in ADMIN-ONLY mode, boot will be allowed to proceed to provide administrators an opportunity to correct this problem. If this Host Controller were not in ADMIN-ONLY mode this would be a fatal boot failure.
6. The messages are followed by a `Task Summary` report that summarizes the result of the migration of each task.

7. Review the updated files in target server installation directory. Note the original configuration and properties files are backed up and now have the suffix `.beforeMigration`.
