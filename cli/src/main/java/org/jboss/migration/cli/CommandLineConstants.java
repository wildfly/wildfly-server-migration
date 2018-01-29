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

import org.jboss.migration.cli.logger.CommandLineMigrationLogger;

/**
 * @author emmartins
 */
public enum CommandLineConstants {
   ENVIRONMENT("environment", CommandLineMigrationLogger.ROOT_LOGGER.argEnvironment()),
   NON_INTERACTIVE("non-interactive", CommandLineMigrationLogger.ROOT_LOGGER.argInteractive()),
   SOURCE("source", CommandLineMigrationLogger.ROOT_LOGGER.argSource()),
   TARGET("target", CommandLineMigrationLogger.ROOT_LOGGER.argTarget()),
   HELP("help", CommandLineMigrationLogger.ROOT_LOGGER.argUsage("jboss-server-migration")),
   SILENT_MODE("silent", CommandLineMigrationLogger.ROOT_LOGGER.argSilent());

   private final String argument;
   private final String description;

   CommandLineConstants(String argument, String description) {
      this.argument = argument;
      this.description = description;
   }

   public String getArgument() {
      return this.argument;
   }

   public String getDescription() {
      return this.description;
   }
}
