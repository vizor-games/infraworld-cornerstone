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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static java.lang.Character.isDigit;
import static java.lang.Character.isJavaIdentifierPart;
import static java.lang.Character.isLowerCase;
import static java.lang.Character.isUpperCase;
import static java.lang.Character.isWhitespace;
import static java.lang.Character.toUpperCase;
import static java.nio.file.Files.walk;
import static java.nio.file.Paths.get;
import static java.util.Arrays.stream;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;

@SuppressWarnings("unused")
public class Misc
{
    public static final String TAB = "    ";

    /**
     * Returns an input string without whitespaces.
     *
     * @param str A string to remove whitespaces from.
     * @return String without whitespaces. Might be the same instance if the input strings contained no whitespaces.
     */
    public static String removeWhitespaces(final String str)
    {
        // Very fast
        if (str.isEmpty())
            return str;

        final int length = str.length();

        // Count whitespaces
        int numWhitespaces = 0;

        for (int i = 0; i < length; i++)
            numWhitespaces += isWhitespace(str.charAt(i)) ? 1 : 0;

        if (numWhitespaces > 0)
        {
            final int numCharactersRequired = length - numWhitespaces;

            if (numCharactersRequired > 0)
            {
                // 'slow path' - allocate a builder, then preserve all non-whitespace characters
                final StringBuilder sb = new StringBuilder(numCharactersRequired);
                for (int i = 0; i < length; i++)
                {
                    final char c = str.charAt(i);
                    if (!isWhitespace(c))
                        sb.append(c);
                }

                return sb.toString();
            }
            else
            {
                // string contained nothing but whitespaces - can return a pre-allocated instance of empty string.
                return "";
            }
        }
        else
        {
            // 'fase path' - source string contained no whitespaces, no need to allocate an additional string.
            return str;
        }
    }

    /**
     * 'almost' copied from UE4's FName::NameToDisplayString
     * @param displayName DisplayName of type.
     * @param isBoolean mark as boolean (adds 'b' prefix)
     * @return A sanitized variable name.
     */
    public static String sanitizeVarName(String displayName, boolean isBoolean)
    {
        if (!displayName.matches("^[A-Za-z_$](([A-Za-z0-9_$])+)?$"))
            throw new RuntimeException("'" + displayName + "' isn't a valid C++ name");

        boolean inRun = false;
        boolean wasSpace = false;
        boolean wasOpenParen = false;

        final int nameLength = displayName.length();
        final StringBuilder sb = new StringBuilder(nameLength);
        for (int i = 0; i < nameLength; ++i)
        {
            char ch = displayName.charAt(i);

            final boolean isLowerCase = isLowerCase(ch);
            final boolean isUpperCase = isUpperCase(ch);
            final boolean isDigit = isDigit(ch);
            final boolean isUnderscore = ch == '_';

            if ((isUpperCase || isDigit) && !inRun && !wasOpenParen)
            {
                if (!wasSpace && sb.length() > 0)
                {
                    sb.append(' ');
                    wasSpace = true;
                }
                inRun = true;
            }
            
            if (isLowerCase)
                inRun = false;
            
            if (isUnderscore)
            {
                ch = ' ';
                inRun = true;
            }
            
            if (sb.length() == 0)
                ch = toUpperCase(ch);
            else if (wasSpace || wasOpenParen)
                ch = toUpperCase(ch);

            wasSpace = ch == ' ';
            wasOpenParen = ch == '(';
            sb.append(ch);
        }

        // Remove all whitespaces
        final String name = spaceSeparatedToCamelCase(sb.toString());

        if (isBoolean)
        {
            // If this is boolean - add 'b' prefix
            return 'b' + name;
        }
        else
        {
            final char firstChar = sb.charAt(0);

            // If first char is digit or any symbol, restricted in cpp - should add a leading underscore
            return (isDigit(firstChar) || !isJavaIdentifierPart(firstChar)) ? ('_' + name) : name;
        }
    }

    /**
     * Input string should be line "int, map<string, string>", not map<int, map<string, string>>
     * this method will split generic arguments, no matter whether they are generics too, or not
     * to do this, it counts angular brackets and splits generic args.
     * @param sourceString string containing comma-separated generic arguments.
     * @return list of generic arguments names.
     */
    public static List<String> splitGeneric(String sourceString)
    {
        final String str = removeWhitespaces(sourceString);
        final List<String> strings = new ArrayList<>();

        int indexOfBegin = 0;
        int numBrackets = 0;

        for (int i = 0; i < str.length(); i++)
        {
            switch (str.charAt(i))
            {
                case '<':
                    ++numBrackets;
                    break;
                case '>':
                    --numBrackets;
                    break;
                case ',':
                    if ((numBrackets == 0) && (i != indexOfBegin))
                    {
                        // String.substring(indexOfBegin, i) i - exclusive.
                        strings.add(str.substring(indexOfBegin, i));
                        indexOfBegin = i + 1;
                    }
                    break;
            }
        }

        if (numBrackets > 0)
            throw new RuntimeException("Missing '>' in '" + sourceString + "'");
        else if (numBrackets < 0)
            throw new RuntimeException("Missing '<' in '" + sourceString + "'");

        if (indexOfBegin != str.length())
            strings.add(str.substring(indexOfBegin));

        return strings;
    }

    /**
     * Rearranges the order of list elements according to indices.
     * So if the list contained ['foo', 'bar', 'baz']
     * And the array of indices is [3, 2, 1, 1, 2, 3]
     * Then the resulting array will be ['baz', 'bar', 'foo', 'foo', 'bar', 'baz']
     *
     * @param list List of the initial elements.
     * @param indices An order, which will be used to reorder elements
     */
    public static <T> void reorder(final List<T> list, final int[] indices)
    {
        final List<T> copy = stream(indices).mapToObj(list::get).collect(toList());

        list.clear();
        list.addAll(copy);
    }

    public static <T, U> Map<U, T> rotateMap(final Map<T, U> map)
    {
        final HashMap<U, T> result = new HashMap<>(map.size());
        map.forEach((t, u) -> result.put(u, t));

        return result;
    }

    public static String snakeCaseToCamelCase(final String snakeCaseString)
    {
        final StringBuilder sb = new StringBuilder(snakeCaseString.length());

        for (final String s : snakeCaseString.split("_"))
        {
            final int wordLength = s.length();

            if (wordLength > 0)
            {
                sb.append(toUpperCase(s.charAt(0)));

                if (wordLength > 1)
                    sb.append(s.substring(1).toLowerCase());
            }
        }

        return sb.toString();
    }

    public static String spaceSeparatedToCamelCase(final String spaceSeparatedString)
    {
        final StringBuilder sb = new StringBuilder(spaceSeparatedString.length());

        // No need to use a slower regex (such as '\\s+'), cause empty words will be wiped out anyway.
        for (final String s : spaceSeparatedString.split(" "))
        {
            final int wordLength = s.length();

            if (wordLength > 0)
            {
                sb.append(toUpperCase(s.charAt(0)));

                if (wordLength > 1)
                    sb.append(s.substring(1).toLowerCase());
            }
        }

        return sb.toString();
    }

    public static boolean stringIsNullOrEmpty(final String string)
    {
        return isNull(string) || string.isEmpty();
    }

    /**
     * Returns a string containing n number of tabs.
     * @param n Number of tabs to insert, must not be negative.
     * @return String containing n tabs.
     */
    public static String nTabs(int n)
    {
        switch (n)
        {
            case 0:
                return "";
            case 1:
                return TAB;
            default:
                if (n < 0)
                    throw new IllegalArgumentException("n mustn't be negative, got " + n + " instead");

                final StringBuilder sb = new StringBuilder(TAB.length() * n);

                for (int i = 0; i < n; i++)
                    sb.append(TAB);

                return sb.toString();
        }
    }

    public static List<Tuple<Path, Path>> findFilesRecursively(final Path src, final Path dst, final String extension)
    {
        final Predicate<String> endsWithIgnoreCase = str -> {
            final int sufLen = extension.length();
            return str.regionMatches(true, str.length() - sufLen, extension, 0, sufLen);
        };

        try {
            return walk(src)
                .filter(Files::isRegularFile)
                .filter(p -> endsWithIgnoreCase.test(p.toString()))
                .map(p -> Tuple.of(p, get(dst.toString(), src.relativize(p.getParent()).toString())))
                .collect(toList());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
