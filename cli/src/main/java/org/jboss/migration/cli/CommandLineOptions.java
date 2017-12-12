/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.migration.cli;

import org.jboss.cli.commonscli.Option;
import org.jboss.cli.commonscli.Options;

/**
 * @author Ingo Weiss
 */
public class CommandLineOptions {
    private final Options options;

    public CommandLineOptions() {
        options = new Options();

        Option opt = Option.builder("e").longOpt(CommandLineConstants.ENVIRONMENT.getArgument()).argName("environment file")
                .desc(CommandLineConstants.ENVIRONMENT.getDescription()).hasArg(true).build();
        options.addOption(opt);

        opt = Option.builder("n").longOpt(CommandLineConstants.NON_INTERACTIVE.getArgument())
                .desc(CommandLineConstants.NON_INTERACTIVE.getDescription()).hasArg(false).build();
        options.addOption(opt);

        opt = Option.builder("s").longOpt(CommandLineConstants.SOURCE.getArgument()).argName("source")
                .desc(CommandLineConstants.SOURCE.getDescription()).hasArg(true).build();
        options.addOption(opt);

        opt = Option.builder("t").longOpt(CommandLineConstants.TARGET.getArgument()).argName("target")
                .desc(CommandLineConstants.TARGET.getDescription()).hasArg(true).build();
        options.addOption(opt);

        opt = Option.builder("h").longOpt(CommandLineConstants.HELP.getArgument()).argName("help")
                .desc(CommandLineConstants.HELP.getDescription()).hasArg(false).build();
        options.addOption(opt);

        opt = Option.builder("m").longOpt(CommandLineConstants.SILENT_MODE.getArgument()).argName("silent")
                .desc(CommandLineConstants.SILENT_MODE.getDescription()).hasArg(false).build();
        options.addOption(opt);
    }

    public Options getOptions() {
        return options;
    }
}
