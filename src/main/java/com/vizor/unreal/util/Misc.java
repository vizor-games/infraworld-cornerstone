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

import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.vizor.unreal.config.DestinationConfig;

import static java.lang.Character.isDigit;
import static java.lang.Character.isJavaIdentifierPart;
import static java.lang.Character.isLowerCase;
import static java.lang.Character.isSpaceChar;
import static java.lang.Character.isUpperCase;
import static java.lang.Character.isWhitespace;
import static java.lang.Character.toLowerCase;
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
     * Unfortunately, you can not calculate a real deadweight tonnage of a string.
     * But it is *slightly* less than {@link Integer#MAX_VALUE}
     * See for details: https://stackoverflow.com/questions/1179983/how-many-characters-can-a-java-string-have
     */
    private static final int MAX_CHARS_IN_STRING = Integer.MAX_VALUE - 10;

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
     *
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

        try
        {
            list.clear();
            list.addAll(copy);
        }
        catch (UnsupportedOperationException uoe)
        {
            throw new RuntimeException("Input list must be mutable in order to perform rearrange");
        }
    }

    /**
     * Transforms snake_case_string into camelCaseString.
     * @param snakeCaseString String in snake_case.
     *
     * @return Input snake_case string transformed to camelCase.
     */
    public static String snakeCaseToCamelCase(final String snakeCaseString)
    {
        return snakeCaseToCamelCase(snakeCaseString, true);
    }

    /**
     * Transforms snake_case_string into camelCaseString.
     * @param snakeCaseString String in snake_case
     * @param firstLetterIsCapital True to make the first letter of the output string capital. False to make it lowercase.
     *
     * @return Input 'snake_case_string' converted into 'camelCaseString'.
     */
    public static String snakeCaseToCamelCase(final String snakeCaseString, boolean firstLetterIsCapital)
    {
        final StringBuilder sb = new StringBuilder(snakeCaseString.length());

        // Split our snake case string by snake separator, simultaneously excluding empty strings
        // This is faster than more sophisticated regex.
        final String[] split = stream(snakeCaseString.split("_"))
                .filter(i -> i.length() != 0)
                .toArray(String[]::new);

        for (int i = 0; i < split.length; i++)
        {
            String s = split[i];
            final int wordLength = s.length();

            if (wordLength > 0)
            {
                // The first letter may be either lower or upper case, depending of 'firstLetterIsCapital' flag
                final char firstLetter;
                if ((i > 0) || firstLetterIsCapital)
                    firstLetter = toUpperCase(s.charAt(0));
                else
                    firstLetter = toLowerCase(s.charAt(0));

                sb.append(firstLetter);

                if (wordLength > 1)
                    sb.append(s.substring(1).toLowerCase());
            }
        }

        return sb.toString();
    }

    /**
     * Converts a space-separated string into camel case string.
     * @param spaceSeparatedString Space-separated string (words) to be converted into camel case string.
     *
     * @return Input 'words string' converted into 'CamelCaseString'.
     */
    public static String spaceSeparatedToCamelCase(final String spaceSeparatedString)
    {
        final int rawLength = spaceSeparatedString.length();

        if (rawLength > 0)
        {
            int numSpaces = 0;
            for (int i = 0; i < rawLength; i++)
            {
                numSpaces += isSpaceChar(spaceSeparatedString.charAt(i)) ? 1 : 0;
            }

            if (numSpaces < rawLength)
            {
                final StringBuilder sb = new StringBuilder();

                // 'true' or 'false' here determines whether the very first letter is upper- or lowercase
                boolean shouldBeUppercase = true;

                for (int i = 0; i < rawLength; i++)
                {
                    final char currentChar = spaceSeparatedString.charAt(i);

                    if (isSpaceChar(currentChar))
                    {
                        // Only reset to uppercase if there are some character within the StringBuilder
                        // (to prevent first character either upper- or lowercase, no matter of leading spaces count)
                        if (sb.length() > 0)
                            shouldBeUppercase = true;
                    }
                    else
                    {
                        if (shouldBeUppercase)
                        {
                            sb.append(toUpperCase(currentChar));
                            shouldBeUppercase = false;
                        }
                        else
                        {
                            sb.append(toLowerCase(currentChar));
                        }
                    }
                }

                return sb.toString();
            }
        }

        return "";
    }

    /**
     * Returns whether the string is null or empty.
     * @param string String to be check for emptiness.
     *
     * @return True if the string is null or contains no characters (is empty).
     */
    public static boolean stringIsNullOrEmpty(final String string)
    {
        return isNull(string) || string.isEmpty();
    }

    /**
     * Returns a string containing n number of tabs.
     * @param n Number of tabs to insert, must not be negative.
     *
     * @return String containing n tabs.
     */
    public static String nTabs(int n)
    {
        switch (n)
        {
            // slight optimization for special cases
            case 0:
                return "";
            case 1:
                return TAB;

            // slow path for natural numbers
            default:
                if (n < 0)
                    throw new IllegalArgumentException("n mustn't be negative, got " + n + " instead");

                final int actualNumChars = TAB.length() * n;

                // String mustn't exceed MAX_CHARS_IN_STRING, which is surprisingly lesser than max int value.K
                if (actualNumChars > MAX_CHARS_IN_STRING)
                {
                    throw new IllegalArgumentException("actual number of tabs mustn't exceed " + MAX_CHARS_IN_STRING +
                        ", got " + actualNumChars + " instead");
                }

                final StringBuilder sb = new StringBuilder(actualNumChars);

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

    public static List<Tuple<Path, DestinationConfig>> findFilesRecursively(final Path src, final DestinationConfig dst, final String extension)
    {
        final Predicate<String> endsWithIgnoreCase = str -> {
            final int sufLen = extension.length();
            return str.regionMatches(true, str.length() - sufLen, extension, 0, sufLen);
        };

        try {
            return walk(src)
                .filter(Files::isRegularFile)
                .filter(p -> endsWithIgnoreCase.test(p.toString()))
                .map(p -> { 
                    final Path relativeSourceFilePath = src.relativize(p.getParent());

					final DestinationConfig relativeDestinationConfig = 
						dst.append(relativeSourceFilePath);

                    return Tuple.of(p, relativeDestinationConfig);
                })
                .collect(toList());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves lowercase names of all supported log4j log levels sorted by its priority.
     * @see org.apache.logging.log4j.Level for details.
     *
     * @return A list of all supported log4j levels.
     */
    public static List<String> getLowercaseLog4jLevels()
    {
        return stream(Level.values())
            .sorted(Level::compareTo)
            .map(Level::name)
            .map(String::toLowerCase)
            .collect(toList());
    }
}
