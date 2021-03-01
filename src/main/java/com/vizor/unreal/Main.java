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
package com.vizor.unreal;

import com.vizor.unreal.config.Config;
import com.vizor.unreal.config.DestinationConfig;
import com.vizor.unreal.convert.Converter;
import com.vizor.unreal.util.CliHandler;
import com.vizor.unreal.util.CliHandler.Parse;
import com.vizor.unreal.util.Tuple;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.List;

import static com.vizor.unreal.util.Misc.findFilesRecursively;
import static com.vizor.unreal.util.Misc.stringIsNullOrEmpty;
import static java.lang.Math.round;
import static java.lang.Runtime.getRuntime;
import static java.lang.System.nanoTime;
import static java.nio.file.Paths.get;
import static org.apache.logging.log4j.LogManager.getLogger;
import static org.apache.logging.log4j.core.config.Configurator.setLevel;



public class Main
{
    private static final Logger log = getLogger(Main.class);

    public static void main(final String[] args)
    {
        launch(args);
    }

    private static void launch(String[] args)
    {
        final Parse cliParse = new CliHandler(args).getParse();

        // Will be ran after jvm was exited
        getRuntime().addShutdownHook(new Thread(() -> log.info("Shutting javaVM down...")));

        // Load and validate config
        final Config config = Config.get();
        config.validate();

        config.patchWithCliOptions(cliParse);
        config.validate();

        // Override log level if has such option
        if (config.isLogLevelNotDefault())
        {
            final Level previousLevel = log.getLevel();

            final String packageName = Main.class.getPackage().getName();
            final Level logLevel = config.getLog4jLogLevel();

            setLevel(packageName, logLevel);
            log.debug("Globally changed {} log level from {} to {}", packageName, previousLevel.name(), logLevel.name());
        }

        final Path srcPath = get(config.getSrcPath());
        final DestinationConfig dstPath = config.getDstPath();

        final Converter converter = new Converter(config.getModuleName());

        if (!srcPath.toFile().isDirectory())
            if(!srcPath.toFile().mkdirs())
                throw new IllegalArgumentException("Source folder '" + srcPath + "' does not exist, or isn't a directory");

        if (!dstPath.pathPublic.toFile().isDirectory())
            if(!dstPath.pathPublic.toFile().mkdirs())
                throw new IllegalArgumentException("Destination Public folder '" + dstPath.pathPublic + "' does not exist, or isn't a directory");

        if (!dstPath.pathPrivate.toFile().isDirectory())
            if(!dstPath.pathPrivate.toFile().mkdirs())
                throw new IllegalArgumentException("Destination Private folder '" + dstPath.pathPrivate + "' does not exist, or isn't a directory");

        log.info("Running cornerstone...");
        log.info("Logging level: {}", log.getLevel().toString());
        log.info("Source path: '{}'", srcPath);
        log.info("Destination path: '{}'", dstPath);

        if (!stringIsNullOrEmpty(config.getModuleName()))
        {
            log.info("Module name: '{}' (thus API is {}_API)", config.getModuleName(),
                    config.getModuleName().toUpperCase());
        }
        else
        {
            log.info("Module name is empty (thus API won't be used)");
        }

        log.info("Company name: {}", config.getCompanyName());
        log.info("Wrappers path: %INCLUDE_DIR%/{}", config.getWrappersPath());

        launchSingle(srcPath, dstPath, converter);
    }

    private static void launchSingle(final Path srcPath, final DestinationConfig dstPath, final Converter converter)
    {
        final long start = nanoTime();

        final List<Tuple<Path, DestinationConfig>> paths = findFilesRecursively(srcPath, dstPath, "proto");

        // Display how many proto file(s) pending processed
        log.info("Running converter, {} proto-files pending processed.", paths.size());
        converter.convert(srcPath, paths);

        final float elapsed = (float) round((double) (nanoTime() - start) / 1000000.0) / 1000.0f;
        log.info("All done in {} seconds. Shutting converter down...", elapsed);
    }
}
