/*
 * Copyright 2015 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.migration.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.ParseException;
import org.jboss.migration.cli.logger.CommandLineMigrationLogger;
import org.jboss.migration.core.MigrationData;
import org.jboss.migration.core.ServerMigration;
import org.jboss.migration.core.env.MigrationEnvironment;
import org.jboss.migration.core.env.SystemEnvironment;
import org.jboss.migration.core.logger.ServerMigrationLogger;
import org.jboss.migration.core.report.HtmlReportWriter;
import org.jboss.migration.core.report.XmlReportWriter;
import org.jboss.migration.core.task.ServerMigrationTaskResult;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * The command line tool to migrate a WildFly server.
 *
 * @author Eduardo Martins
 */
public class CommandLineServerMigration {
    // Capture System.out and System.err before they are redirected by STDIO
    private static final PrintStream STDOUT = System.out;
    private static final PrintStream STDERR = System.err;
    private static final CommandLineParser cmdLineParser = new DefaultParser();
    private static final CommandLineOptions cmdOptions = new CommandLineOptions();

    private CommandLineServerMigration() {
    }

    /**
     * The main method.
     *
     * @param args the command-line arguments
     */
    public static void main(String[] args) {

        CommandLine cmdLine;

        try {
            cmdLine = cmdLineParser.parse(cmdOptions.getOptions(), args);

            if (cmdLine.hasOption(CommandLineConstants.HELP.getArgument())) {
                help();
            }

            if (!cmdLine.hasOption(CommandLineConstants.SOURCE.getArgument())) {
                throw new MissingOptionException("Missing required option: " + CommandLineConstants.SOURCE.getArgument());
            }

            if (!cmdLine.hasOption(CommandLineConstants.TARGET.getArgument())) {
                throw new MissingOptionException("Missing required option: " + CommandLineConstants.TARGET.getArgument());
            }

            Path source = resolvePath(cmdLine.getOptionValue(CommandLineConstants.SOURCE.getArgument()));
            Path target = resolvePath(cmdLine.getOptionValue(CommandLineConstants.TARGET.getArgument()));
            Path environment = cmdLine.hasOption(CommandLineConstants.ENVIRONMENT.getArgument()) ? resolvePath(cmdLine.getOptionValue(CommandLineConstants.ENVIRONMENT.getArgument())) : null;
            boolean interactive = !cmdLine.hasOption(CommandLineConstants.INTERACTIVE.getArgument()) || Boolean.parseBoolean(cmdLine.getOptionValue(CommandLineConstants.INTERACTIVE.getArgument()));

            if (!cmdLine.getArgList().isEmpty()) {
                System.out.printf("Argument(s), %s, will be discarded.\n", cmdLine.getArgList());
            }

            final String baseDir = SystemEnvironment.INSTANCE.getPropertyAsString(EnvironmentProperties.BASE_DIR);
            if (baseDir == null) {
                throw new RuntimeException("system environment does not specifies the tool's base dir");
            }
            final Path baseDirPath = Paths.get(baseDir);
            final Path configDirPath = baseDirPath.resolve("configuration");
            final Path reportsDirPath = baseDirPath.resolve("reports");

            // setup user environment
            final MigrationEnvironment userEnvironment = new MigrationEnvironment();
            final Path configDirEnvironment = configDirPath.resolve("environment.properties");
            if (Files.exists(configDirEnvironment)) {
                userEnvironment.setProperties(loadProperties(configDirEnvironment));
            }
            if (environment != null) {
                userEnvironment.setProperties(loadProperties(environment));
            }

            // run migration
            final MigrationData migrationData = new ServerMigration()
                    .from(source)
                    .to(target)
                    .interactive(interactive)
                    .userEnvironment(userEnvironment)
                    .run();

            // write reports
            final String htmlReportFileName = userEnvironment.getPropertyAsString(EnvironmentProperties.REPORT_HTML_FILE_NAME);
            final String xmlReportFileName = userEnvironment.getPropertyAsString(EnvironmentProperties.REPORT_XML_FILE_NAME);
            if (htmlReportFileName != null) {
                try {
                    final String htmlReportTemplateFileName = userEnvironment.getPropertyAsString(EnvironmentProperties.REPORT_HTML_TEMPLATE_FILE_NAME, "migration-report-template.html");
                    final Path htmlReportTemplatePath = configDirPath.resolve(htmlReportTemplateFileName);
                    HtmlReportWriter.INSTANCE.toPath(reportsDirPath.resolve(htmlReportFileName), migrationData, HtmlReportWriter.ReportTemplate.from(htmlReportTemplatePath));
                } catch (Throwable e) {
                    ServerMigrationLogger.ROOT_LOGGER.error("HTML Report write failed", e);
                }
            }
            if (xmlReportFileName != null) {
                try {
                    XmlReportWriter.INSTANCE.writeContent(reportsDirPath.resolve(xmlReportFileName).toFile(), migrationData);
                } catch (Throwable e) {
                    ServerMigrationLogger.ROOT_LOGGER.error("XML Report write failed", e);
                }
            }
            if (migrationData.getRootTask().getResult().getStatus() == ServerMigrationTaskResult.Status.FAIL) {
                System.exit(1);
            }
        } catch (ParseException pex) {
            System.err.println(pex.getLocalizedMessage());
            help();
        } catch (Throwable t) {
            abort(t);
        }
    }

    private static void help() {
        PrintWriter pw = new PrintWriter(System.out);
        HelpFormatter help = new HelpFormatter();
        pw.println( CommandLineMigrationLogger.ROOT_LOGGER.helpHeader());
        help.printHelp(pw, 1024, CommandLineMigrationLogger.ROOT_LOGGER.argUsage("jboss-server-migration"), "", cmdOptions.getOptions(), help.getLeftPadding(), help.getDescPadding(), "", true);
        pw.flush();
        abort(null);
    }

    private static Properties loadProperties(Path propertiesFilePath) throws IOException {
        final Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(propertiesFilePath)) {
            properties.load(inputStream);
        }
        return properties;
    }

    private static void abort(Throwable t) {
        try {
            if (t != null) {
                t.printStackTrace(STDERR);
            }
        } finally {
            System.exit(1);
        }
    }

    private static Path resolvePath(String s) throws IllegalArgumentException {
        Path path = Paths.get(s).normalize();
        Path absolutePath = path.isAbsolute() ? path : Paths.get(System.getProperty("user.dir")).resolve(path);
        if (!Files.exists(absolutePath)) {
            throw new IllegalArgumentException("File "+absolutePath+" does not exists.");
        } else {
            return absolutePath;
        }
    }
}
