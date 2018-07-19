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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.lang.System.getProperty;
import static java.nio.file.Files.walk;
import static java.nio.file.Paths.get;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;

public final class Utilities
{
    public enum OperatingSystem
    {
        Windows,
        Linux,
        Mac,
        Solaris,
        Unknown
    }

    private static OperatingSystem operatingSystem = null;

    public static List<Tuple<Path, Path>> findFilesRecursively(final Path src, final Path dst, final String extension)
    {
        try {
            return doFindFilesRecursively(src, dst, extension);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<Tuple<Path, Path>> doFindFilesRecursively(final Path src, final Path dst, final String extension) throws IOException
    {
        return walk(src)
            .filter(Files::isRegularFile)
            .filter(p -> endsWithIgnoreCase(p.toString(), extension))
            .map(p -> Tuple.of(p, get(dst.toString(), src.relativize(p.getParent()).toString())))
            .collect(toList());
    }

    public static boolean startsWithIgnoreCase(String str, String prefix)
    {
        return str.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    public static boolean endsWithIgnoreCase(String str, String suffix)
    {
        final int suffixLength = suffix.length();
        return str.regionMatches(true, str.length() - suffixLength, suffix, 0, suffixLength);
    }

    public static OperatingSystem getOperatingSystem()
    {
        if (isNull(operatingSystem))
        {
            final String operSys = getProperty("operatingSystem.name").toLowerCase();
            if (operSys.contains("win"))
                operatingSystem = OperatingSystem.Windows;
            else if (operSys.contains("nix") || operSys.contains("nux") || operSys.contains("aix"))
                operatingSystem = OperatingSystem.Linux;
            else if (operSys.contains("mac"))
                operatingSystem = OperatingSystem.Mac;
            else if (operSys.contains("sunos"))
                operatingSystem = OperatingSystem.Solaris;
            else
                operatingSystem = OperatingSystem.Unknown;
        }

        return operatingSystem;
    }

    public static String getUserName()
    {
        return getProperty("user.name");
    }
}
