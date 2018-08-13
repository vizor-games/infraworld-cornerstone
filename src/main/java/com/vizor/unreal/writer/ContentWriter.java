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
package com.vizor.unreal.writer;

import com.vizor.unreal.util.Misc;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Character.isWhitespace;
import static java.lang.Math.max;
import static java.lang.String.join;
import static java.lang.System.lineSeparator;

final class ContentWriter
{
    private final List<String> lines = new ArrayList<>();
    private StringBuilder currentLine = new StringBuilder();

    private int numTabs = 0;

    void incTabs()
    {
        ++numTabs;
    }

    void decTabs()
    {
        numTabs = max(0, numTabs - 1);
    }

    void write(final String s)
    {
        currentLine.append(s);
    }

    void writeLine(final String s)
    {
        currentLine.append(s);
        newLine();
    }

    void backspace(final int times)
    {
        // Can not operate with negative indices
        if (times < 0)
            throw new IllegalArgumentException("Can not operate with negative index. Actual index: " + times);

        final int length = currentLine.length();

        // Trim trailing characters if number of backspaces is lesser than current string builder's length
        if (times < length)
            currentLine.delete(length - times, length);
        else
            currentLine.setLength(0);
    }

    void newLine()
    {
        trimTrailingSpaces(currentLine);

        if (currentLine.length() > 0)
        {
            for (int i = 0; i < numTabs; i++)
                currentLine.insert(0, Misc.TAB);

            lines.add(currentLine.toString());
            currentLine.setLength(0);
        }
        else
        {
            lines.add("");
        }
    }

    void removeLine()
    {
        if (!lines.isEmpty())
            lines.remove(lines.size() - 1);
    }

    void writeToFile(final String fileName)
    {
        try (final PrintWriter pw = new PrintWriter(fileName))
        {
            lines.forEach(pw::println);
            pw.println(currentLine.toString());
        }
        catch (IOException ex)
        {
            throw new RuntimeException(ex);
        }
    }

    private static void trimTrailingSpaces(final StringBuilder sb)
    {
        final int length = sb.length();

        for (int i = length - 1; (i >= 0) && isWhitespace(sb.charAt(i)); i--)
            sb.deleteCharAt(i);
    }

    @Override
    public String toString()
    {
        final List<String> list = new ArrayList<>(lines);
        lines.add(currentLine.toString());

        return !lines.isEmpty() ? join(lineSeparator(), list) : "";
    }
}
