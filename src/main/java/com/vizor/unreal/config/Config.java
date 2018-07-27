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
package com.vizor.unreal.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.vizor.unreal.util.CliHandler.Parse;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.vizor.unreal.util.Misc.stringIsNullOrEmpty;
import static java.lang.Character.isDigit;
import static java.lang.Character.isLetter;
import static java.lang.String.valueOf;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.stream;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.logging.log4j.LogManager.getLogger;
import static org.apache.logging.log4j.LogManager.getRootLogger;

public final class Config
{
    private static final Logger log = getLogger(Config.class);

    private static final String configFileName = "config.yml";

    private static Config config = null;

    @JsonProperty(value = "src_path", required = true)
    private String srcPath;

    @JsonProperty(value = "dst_path", required = true)
    private String dstPath;

    @JsonProperty(value = "module_name", required = true)
    private String moduleName;

    @JsonProperty(value = "precompiled_header", required = true)
    private String precompiledHeader;

    @JsonProperty("wrappers_path")
    private String wrappersPath;

    @JsonProperty("company_name")
    private String companyName;

    /**
     * @see org.apache.logging.log4j.Level
     */
    @JsonProperty("log_level")
    private String logLevel;

    @JsonProperty(value = "no_fork", defaultValue = "false")
    private boolean noFork;


    /**
     * If we're not in jar -> loads the config ONLY from the 'resources' directory.
     * If we're in jar -> try to load config from the file near the jar.
     *
     * @return Opened {@link InputStream to the config file}
     */
    private static InputStream getConfigStream()
    {
        String pathToJar = Config.class.getProtectionDomain().getCodeSource().getLocation().getPath();

        // Cut first slash - need on Windows
        if (System.getProperty("os.name").toLowerCase().contains("windows"))
            pathToJar = pathToJar.substring(1);

        final boolean isInJar = !pathToJar.endsWith("/") && pathToJar.endsWith(".jar");

        if (isInJar)
        {
            // If in jar -> the config must be loaded
            final String pathToConfigFolder = pathToJar.substring(0, pathToJar.lastIndexOf("/"));
            final String pathToConfig = Paths.get(pathToConfigFolder, configFileName).toString();

            try
            {
                final FileInputStream fs = new FileInputStream(pathToConfig);
                log.info("Override config found: {}", pathToConfig);

                return fs;
            }
            catch (final Throwable t)
            {
                log.info("Override config not found (might be placed as {}), got {}",
                        pathToConfig, t.toString());
            }
        }

        final ClassLoader cl = currentThread().getContextClassLoader();
        final InputStream rs = cl.getResourceAsStream(configFileName);

        if (isNull(rs))
            throw new RuntimeException("Unable to open the config from the resource folder: " + configFileName);

        return rs;
    }

    public static Config get()
    {
        if (isNull(config))
        {
            final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

            try (final InputStream is = getConfigStream())
            {
                config = mapper.readValue(is, Config.class);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        return config;
    }

    @Override
    public String toString()
    {
        final ObjectMapper mapper = new ObjectMapper();
        try
        {
            return mapper.writeValueAsString(this);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public final String getSrcPath()
    {
        return srcPath;
    }

    public final String getDstPath()
    {
        return dstPath;
    }

    public final String getModuleName()
    {
        return moduleName;
    }

    public final String getPrecompiledHeader()
    {
        return precompiledHeader;
    }

    public final String getWrappersPath()
    {
        return wrappersPath;
    }

    public final String getCompanyName()
    {
        return companyName;
    }

    public final boolean isLogLevelNotDefault()
    {
        return !stringIsNullOrEmpty(logLevel);
    }

    public final Level getLogLevel()
    {
        return stream(Level.values())
            .filter(l -> {
                final String currentLevel = l.name();
                return currentLevel.equalsIgnoreCase(logLevel);
            })
            .findFirst().orElse(getRootLogger().getLevel());
    }

    public boolean canNoFork()
    {
        return noFork;
    }

    private void checkString(final String string, final String failMessage)
    {
        if (stringIsNullOrEmpty(string))
            throw new RuntimeException(failMessage);
    }

    public final void validate()
    {
        checkString(srcPath, "srcPath must not be null or empty");
        checkString(dstPath, "dstPath must not be null or empty");

        for (int i = 0; i < moduleName.length(); i++)
        {
            final char c = moduleName.charAt(i);
            if (!isDigit(c) && !isLetter(c))
            {
                throw new RuntimeException("module_name, which is '" + moduleName +
                        "' must contain only digits or letters");
            }
        }

        checkString(wrappersPath, "wrappers_path must not be null or empty");
        checkString(companyName, "company_name must not be null or empty");

        for (int i = 0; i < companyName.length(); i++)
        {
            if (companyName.charAt(i) == '|')
                throw new RuntimeException("company_name, which is '" + companyName + "' mustn't contain '|'");
        }

        if (!stringIsNullOrEmpty(logLevel))
        {
            final List<String> availableOptions = stream(Level.values())
                .map(Level::name)
                .map(String::toLowerCase)
                .collect(toList());

            if (!availableOptions.contains(logLevel))
                throw new RuntimeException("Selected option '" + logLevel + "' not found among available options: " +
                    availableOptions.toString());
        }
    }

    public final void patchWithCliOptions(final Parse cliParse)
    {
        final Class<?> cliParseClass = cliParse.getClass();
        final Class<?> configClass = getClass();

        final Field[] declaredFields = cliParseClass.getDeclaredFields();
        final Map<String, Field> cliFields = stream(declaredFields).collect(toMap(Field::getName, identity()));

        for (final Field configField : configClass.getDeclaredFields())
        {
            // Skip fields not annotated as json props
            if (!configField.isAnnotationPresent(JsonProperty.class))
                continue;

            final Field cliField = cliFields.get(configField.getName());

            // If field was found
            if (nonNull(cliField))
            {
                final boolean configFieldWasAccessible = configField.isAccessible();
                if (!configFieldWasAccessible)
                    configField.setAccessible(true);

                final boolean cliFieldWasAccessible = cliField.isAccessible();
                if (!cliFieldWasAccessible)
                    cliField.setAccessible(true);

                try
                {
                    final Object cliValue = cliField.get(cliParse);
                    final Object configValue = configField.get(Config.this);

                    // Don't replace if replacement value is null or values are equal.
                    if (nonNull(cliValue) && !Objects.equals(cliValue, configValue))
                    {
                        log.info("Replacing '{}.{} = {}' with '{}.{} = {}'",
                                configClass.getName(), configField.getName(), valueOf(configValue),
                                cliParseClass.getName(), cliField.getName(), valueOf(cliValue));

                        configField.set(this, cliValue);
                    }
                }
                catch (IllegalAccessException e)
                {
                    throw new RuntimeException(e);
                }

                if (!configFieldWasAccessible)
                    configField.setAccessible(false);

                if (!cliFieldWasAccessible)
                    cliField.setAccessible(false);
            }
            else
            {
                log.debug("Field named '{}' wasn't found among {}", configField.getName(),
                        stream(declaredFields).map(df -> "'" + df.getName() + "'").collect(toList()));
            }
        }
    }
}
