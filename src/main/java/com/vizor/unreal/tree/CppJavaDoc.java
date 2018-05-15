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
package com.vizor.unreal.tree;

import com.vizor.unreal.writer.CppPrinter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.join;
import static java.lang.System.lineSeparator;
import static java.util.Arrays.copyOfRange;
import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.nonNull;

public class CppJavaDoc implements CtLeaf
{
    private final List<String> linesDoc = new ArrayList<>();
    private final Map<String, String> paramsDoc = new HashMap<>();
    private String returnDoc = null;

    public void set(String s)
    {
        linesDoc.clear();
        paramsDoc.clear();
        returnDoc = null;

        stream(s.split(lineSeparator()))
            .map(String::trim)
            .forEach(line -> {
                if (line.startsWith("@"))
                {
                    final String[] words = line.split("\\s+");
                    final int numWords = words.length;

                    final String operator = words[0].substring(1);
                    switch (operator)
                    {
                        case "param":
                            final String key = (numWords > 1) ? words[1] : "";
                            final String value = (numWords > 2) ? join(" ", copyOfRange(words, 2, numWords)) : "";
                            paramsDoc.put(key, value);
                            break;
                        case "return":
                        case "returns":
                            returnDoc = (numWords > 1) ? join(" ", copyOfRange(words, 1, numWords)) : "";
                            break;
                        default:
                            throw new RuntimeException("Unrecognized operator directive '" + operator + "' in context '" + line + "'");
                    }
                }
                else
                {
                    linesDoc.add(line);
                }
            });
    }

    private List<String> getParamsAndReturn()
    {
        final List<String> lines = new ArrayList<>();

        // Add params
        paramsDoc.forEach((k, v) -> lines.add("@param " + k + " " + v));

        if (nonNull(returnDoc))
            lines.add("@return " + returnDoc);

        return lines;
    }

    public List<String> getLines()
    {
        final List<String> lines = new ArrayList<>(linesDoc);
        final List<String> paramsAndReturn = getParamsAndReturn();

        if (!paramsAndReturn.isEmpty())
        {
            lines.add("");
            lines.addAll(paramsAndReturn);
        }

        return unmodifiableList(lines);
    }

    @Override
    public CppPrinter accept(CppPrinter printer)
    {
        printer.visit(this);
        return printer;
    }
}
