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

import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

public final class CppEnum extends CppRecord
{
    private final CppType type;
    private final CppJavaDoc javaDoc = new CppJavaDoc();

    private final List<CppEnumElement> cppEnumElements;

    public CppEnum(final CppType type, final Map<String, Integer> enumElements)
    {
        final List<CppEnumElement> cppEnumElements = enumElements.entrySet().stream()
                .map(e -> new CppEnumElement(e.getKey(), e.getValue()))
                .collect(toList());

        this.type = type;
        this.cppEnumElements = unmodifiableList(cppEnumElements);
    }

    public final List<CppEnumElement> getCppEnumElements()
    {
        return cppEnumElements;
    }

    public final CppType getType()
    {
        return type;
    }

    public final CppJavaDoc getJavaDoc()
    {
        return javaDoc;
    }

    @Override
    public final CppPrinter accept(CppPrinter printer)
    {
        printer.visit(this);
        return printer;
    }
}
