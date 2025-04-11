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
package org.jboss.migration.core.logger;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.migration.core.ProductInfo;

import java.lang.invoke.MethodHandles;

import static org.jboss.logging.Logger.Level.INFO;

/**
 * The server migration's core logger.
 * @author emmartins
 */
@MessageLogger(projectCode = "WFCMTOOL")
public interface ServerMigrationLogger extends BasicLogger {

    /**
     * the root logger instance
     */
    ServerMigrationLogger ROOT_LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), ServerMigrationLogger.class, ServerMigrationLogger.class.getPackage().getName());

    /**
     * Creates an exception indicating that no java.io.Console is available.
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 1, value = "No java.io.Console available to interact with user.")
    IllegalStateException noConsoleAvailable();

    /**
     * The long value a user would enter to indicate 'yes'
     *
     * This String should be the lower case representation in the respective locale.
     *
     * @return The value a user would enter to indicate 'yes'.
     */
    @Message(id = Message.NONE, value = "yes")
    String yes();

    /**
     * The short value a user would enter to indicate 'yes'
     *
     * If no short value is available for a specific translation then only the long value will be accepted.
     *
     * This String should be the lower case representation in the respective locale.
     *
     * @return The short value a user would enter to indicate 'yes'.
     */
    @Message(id = Message.NONE, value = "y")
    String shortYes();

    /**
     * The long value a user would enter to indicate 'no'
     *
     * This String should be the lower case representation in the respective locale.
     *
     * @return The value a user would enter to indicate 'no'.
     */
    @Message(id = Message.NONE, value = "no")
    String no();

    /**
     * The short value a user would enter to indicate 'no'
     *
     * If no short value is available for a specific translation then only the long value will be accepted.
     *
     * This String should be the lower case representation in the respective locale.
     *
     * @return The short value a user would enter to indicate 'no'.
     */
    @Message(id = Message.NONE, value = "n")
    String shortNo();

    /**
     * The option a user would choose to indicate an option not listed
     *
     * This String should be the lower case representation in the respective locale.
     *
     * @return the option a user would choose to indicate an option not listed.
     */
    @Message(id = Message.NONE, value = "other")
    String other();

    /**
     * The error message if the confirmation response is invalid.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = 2, value = "Invalid response. (Valid responses are %s and %s)")
    String invalidResponse(String firstValues, String secondValues);

    /**
     * The error message if the confirmation response is invalid.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = 3, value = "Invalid empty response.")
    String invalidEmptyResponse();

    /**
     * The error message header.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = Message.NONE, value = "Error")
    String errorHeader();

    /**
     * Simple yes/no prompt.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = Message.NONE, value = "yes/no?")
    String yesNo();

    /**
     * Prompt reading an other choice.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = Message.NONE, value = "Other choice?")
    String otherChoice();

    /**
     * Creates an exception indicating that the target server does not supports migration from the source server.
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 4, value = "Server name = %s, version = %s does not support migration from server name = %s, version = %s.")
    IllegalArgumentException doesNotSupportsMigration(String targetName, String targetVersion, String sourceName, String sourceVersion);

    /**
     * Creates an exception indicating that a server base dir is not set.
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 5, value = "Migration %s server base dir not set.")
    IllegalStateException serverBaseDirNotSet(String serverName);

    /**
     * Creates an exception indicating that no server was retrieved from a base dir.
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 6, value = "Failed to retrieve server %s, from base dir %s.")
    IllegalArgumentException failedToRetrieveServerFromBaseDir(String serverName, String baseDir);

    /**
     * Logs a msg with the server's product info.
     */
    @LogMessage(level = INFO)
    @Message(id = Message.NONE, value = "%s server %s.")
    void serverProductInfo(String name, ProductInfo productInfo);
}
