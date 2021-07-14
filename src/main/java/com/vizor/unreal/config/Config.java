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

import com.vizor.unreal.util.CliHandler.Parse;
import com.vizor.unreal.util.Misc;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.vizor.unreal.util.Misc.snakeCaseToCamelCase;
import static com.vizor.unreal.util.Misc.stringIsNullOrEmpty;
import static java.lang.Character.isDigit;
import static java.lang.Character.isLetter;
import static java.lang.String.valueOf;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.stream;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.logging.log4j.LogManager.getLogger;
import static org.apache.logging.log4j.LogManager.getRootLogger;


@SuppressWarnings("unused")
public final class Config
{
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface ConfigField {}

    private static final Logger log = getLogger(Config.class);
    private static final String configFileName = "config.yml";

    private static Config config = null;

    @ConfigField
    private String srcPath;
    
    @ConfigField
    private String includePath;

    @ConfigField
    private String dstPublicPath;

    @ConfigField
    private String dstPrivatePath;

    @ConfigField
    private String moduleName;

    @ConfigField
    private String precompiledHeader;

    @ConfigField
    private String wrappersPath;

    @ConfigField
    private String companyName;

    @ConfigField
    private String logLevel;

    @ConfigField
    private boolean noFork;


    public final String getSrcPath()
    {
        return srcPath;
    }

    public void setSrcPath(String srcPath)
    {
        this.srcPath = srcPath;
    }
    
    public final String getIncludePath()
    {
        return includePath;
    }

    public void setIncludePath(String includePath)
    {
        this.includePath = includePath;
    }

    public final String getDstPublicPath()
    {
        return dstPublicPath;
    }

    public void setDstPublicPath(String dstPath)
    {
        this.dstPublicPath = dstPath;
    }

    public final String getDstPrivatePath()
    {
        return dstPrivatePath;
    }

    public void setDstPrivatePath(String dstPath)
    {
        this.dstPrivatePath = dstPath;
    }

    public final String getModuleName()
    {
        return moduleName;
    }

    public void setModuleName(String moduleName)
    {
        this.moduleName = moduleName;
    }

    public final String getPrecompiledHeader()
    {
        return precompiledHeader;
    }

    public void setPrecompiledHeader(String precompiledHeader)
    {
        this.precompiledHeader = precompiledHeader;
    }

    public final String getWrappersPath()
    {
        return wrappersPath;
    }

    public void setWrappersPath(String wrappersPath)
    {
        this.wrappersPath = wrappersPath;
    }

    public final String getCompanyName()
    {
        return companyName;
    }

    public void setCompanyName(String companyName)
    {
        this.companyName = companyName;
    }

    public String getLogLevel()
    {
        return logLevel;
    }

    public void setLogLevel(String logLevel)
    {
        this.logLevel = logLevel;
    }

    public boolean isNoFork()
    {
        return noFork;
    }

    public void setNoFork(boolean noFork)
    {
        this.noFork = noFork;
    }

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

        // Only excelsior jet is now supported
        final boolean isJetCompiled = nonNull(System.getProperty("jet.exe.dir"));

        if (isInJar || isJetCompiled)
        {
            // If integrally compiled  -> the config must be loaded
            final Path configSearchFolder;

            if (isInJar)
                configSearchFolder = Paths.get(pathToJar).getParent();
            else
                configSearchFolder = Paths.get(System.getProperty("jet.exe.dir"));

            final Path pathToConfig = requireNonNull(configSearchFolder).resolve(configFileName);

            try
            {
                final FileInputStream fs = new FileInputStream(pathToConfig.toString());
                log.info("Override config found: {}", pathToConfig);

                return fs;
            }
            catch (FileNotFoundException t)
            {
                // Override config is mandatory if cornerstone is compiled into an executable
                log.fatal("Please, put your {} into {}", configFileName, configSearchFolder);
                System.exit(1);
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
            final Constructor constructor = new Constructor(Config.class);
            constructor.setPropertyUtils(new PropertyUtils(){
                @Override
                public Property getProperty(Class<?> type, String name)
                {
                    return super.getProperty(type, snakeCaseToCamelCase(name, false));
                }
            });

            config = new Yaml(constructor).loadAs(getConfigStream(), Config.class);
        }

        return config;
    }

    @Override
    public String toString()
    {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(FlowStyle.BLOCK);

        return new Yaml(options).dump(this);
    }

    public final boolean isLogLevelNotDefault()
    {
        return !stringIsNullOrEmpty(logLevel);
    }

    public final Level getLog4jLogLevel()
    {
        return stream(Level.values())
            .filter(l -> {
                final String currentLevel = l.name();
                return currentLevel.equalsIgnoreCase(logLevel);
            })
            .findFirst().orElse(getRootLogger().getLevel());
    }

    private void checkString(final String string, final String failMessage)
    {
        if (stringIsNullOrEmpty(string))
            throw new RuntimeException(failMessage);
    }

    public final void validate()
    {
        checkString(srcPath, "srcPath must not be null or empty");
        checkString(includePath, "includePath must not be null or empty");
        checkString(dstPublicPath, "dstPublicPath must not be null or empty");
        checkString(dstPrivatePath, "dstPrivatePath must not be null or empty");

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
            final List<String> availableOptions = Misc.getLowercaseLog4jLevels();

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
            if (!configField.isAnnotationPresent(ConfigField.class))
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

    public DestinationConfig getDstPath() {
        return new DestinationConfig(Paths.get(dstPublicPath), Paths.get(dstPrivatePath));
    }
}
