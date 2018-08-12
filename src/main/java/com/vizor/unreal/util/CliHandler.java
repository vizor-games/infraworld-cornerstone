/*
 * Copyright 2018 Vizor Games LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.vizor.unreal.util;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import java.io.PrintWriter;

import static com.vizor.unreal.util.Misc.getLowercaseLog4jLevels;
import static java.lang.System.exit;
import static java.lang.System.lineSeparator;
import static java.util.Arrays.asList;

public final class CliHandler
{
    // Being used in reflection, thus can not be deleted.
    @SuppressWarnings("unused")
    public static class Parse
    {
        private String srcPath;
        private String dstPath;
        private String moduleName;
        private String logLevel;
    }

    private static Option srcOption = new Option("s", "src_path", true, "Proto files root folder");
    private static Option dstOption = new Option("d", "dst_path", true, "Cpp generated files destination folder");
    private static Option moduleNameOption = new Option("m", "module_name", true, "UE 4 API/Plugin name");
    private static Option logLevelOption = new Option("l", "log_level", true, "Override log level. Available options are: " +
        getLowercaseLog4jLevels().toString());
    private static Option helpOption = new Option("h", "help", false, "Print this help message");
    private static Option creditsOption = new Option("credits", false, "Print the creators of Cornerstone");

    private final Parse parse;

    public CliHandler(String... args)
    {
        final Options options = new Options();
        options.addOption(srcOption)
               .addOption(dstOption)
               .addOption(moduleNameOption)
               .addOption(logLevelOption)
               .addOption(helpOption)
               .addOption(creditsOption);
        try
        {
            final CommandLine cmd = new PosixParser().parse(options, args);

            parse = new Parse();
            {
                parse.srcPath = cmd.getOptionValue(srcOption.getOpt());
                parse.dstPath = cmd.getOptionValue(dstOption.getOpt());
                parse.moduleName = cmd.getOptionValue(moduleNameOption.getOpt());
                parse.logLevel = cmd.getOptionValue(logLevelOption.getOpt());
            }

            if (cmd.hasOption(helpOption.getOpt()))
                printHelp(options, "Available commands:");

            if (cmd.hasOption(creditsOption.getOpt()))
            {
                printHelp(options, String.join(lineSeparator(), asList(
                    "* CREDITS *",
                    "",
                    "Design:",
                    " - Raman Chakhouski",
                    "",
                    "General programming:",
                    " - Raman Chakhouski",
                    " - Nikita Miroshnichenko",
                    ""
                )));
            }
        }
        catch (final ParseException e)
        {
            printHelp(options, e.getMessage());

            // Should never reach this point, because printHelp causes exit()
            throw new RuntimeException(e);
        }
    }

    private void printHelp(Options options, String extra)
    {
        final String commandLineSyntax = "java -jar cornerstone.jar";
        final PrintWriter writer = new PrintWriter(System.err);
        final HelpFormatter helpFormatter = new HelpFormatter();

        helpFormatter.printHelp(writer, 128, commandLineSyntax, extra, options, 3, 5,
                "", true);
        writer.flush();
        exit(1);
    }

    public final Parse getParse()
    {
        return parse;
    }
}
