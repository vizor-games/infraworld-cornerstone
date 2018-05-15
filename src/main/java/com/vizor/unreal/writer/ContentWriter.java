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
import java.util.regex.Pattern;

import static java.lang.Character.isWhitespace;
import static java.lang.Math.max;
import static java.lang.String.join;
import static java.lang.System.lineSeparator;

public final class ContentWriter
{
    private static final Pattern spaceFinder = Pattern.compile("\\s+");

    private final List<String> lines = new ArrayList<>();
    private StringBuilder currentLine = new StringBuilder();

    private int numTabs = 0;

    public void incTabs()
    {
        ++numTabs;
    }

    public void decTabs()
    {
        numTabs = max(0, numTabs - 1);
    }

    public void write(String s)
    {
        currentLine.append(s);
    }

    public void writeLine(String s)
    {
        currentLine.append(s);
        newLine();
    }

    public void backspace()
    {
        final int length = currentLine.length();
        if (length > 0)
            currentLine.deleteCharAt(length - 1);
    }

    public void backspace(int times)
    {
        for (int i = 0; i < times; i++)
            backspace();
    }

    public void newLine()
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

    public void removeLine()
    {
        if (!lines.isEmpty())
            lines.remove(lines.size() - 1);
    }

    public void writeToFile(String fileName)
    {
        try (PrintWriter pw = new PrintWriter(fileName))
        {
            lines.forEach(pw::println);
            pw.println(currentLine.toString());
        }
        catch (IOException ex)
        {
            throw new RuntimeException(ex);
        }
    }

    private static void trimTrailingSpaces(StringBuilder sb)
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

        return !lines.isEmpty() ? join(lineSeparator(), list) : "<<< empty >>>";
    }
}
