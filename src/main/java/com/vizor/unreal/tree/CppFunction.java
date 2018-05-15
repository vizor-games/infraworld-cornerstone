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
import java.util.Collection;
import java.util.List;

import static java.lang.System.lineSeparator;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.isNull;

public class CppFunction extends CppRecord
{
    public enum InlineModifier
    {
        NoInline(""),
        Inline("inline"),
        ForceInline("FORCEINLINE");

        public final String name;

        InlineModifier(String name)
        {
            this.name = name;
        }
    }

    // Basic parameters
    private final String name;
    private final CppType returnType;
    private final List<CppArgument> arguments;
    private InlineModifier inlineModifier;

    // ! If generic params is 'null' - the function will be non-generic
    // ! If generic params is an empty list - it will be template<>
    // ! If generic params is non-empty list such as [a, b, c] - it will be template<a, b, c>
    private final List<CppType> genericParams;

    // Body (lines) of the method
    private final List<String> body = new ArrayList<>();

    // Setting with 'CppClass'
    CppClass declaringItem;

    public boolean isVirtual = false;
    public boolean isConst = false;
    public boolean isOverride = false;
    public boolean isStatic = false;

    private final CppJavaDoc javaDoc = new CppJavaDoc();

    public CppFunction(final String name, final CppType returnType)
    {
        this (name, returnType, emptyList());
    }

    public CppFunction(final String name, final CppType returnType, final List<CppArgument> args)
    {
        this (name, returnType, args, null);
    }

    public CppFunction(final String name, final CppType returnType, final List<CppArgument> args, final List<CppType> genericParams)
    {
        this.name = name;
        this.returnType = returnType;
        this.arguments = unmodifiableList(args);
        this.genericParams = isNull(genericParams) ? null : unmodifiableList(genericParams);
        inlineModifier = InlineModifier.NoInline;
    }

    public final CppType getReturnType()
    {
        return returnType;
    }

    public final Collection<CppArgument> getArguments()
    {
        return arguments;
    }

    // generic parameters
    public final List<CppType> getGenericParams()
    {
        return genericParams;
    }

    public final String getName()
    {
        return name;
    }

    public final void setBody(String body)
    {
        this.body.clear();
        this.body.addAll(asList(body.split(lineSeparator())));
    }

    public final List<String> getBody()
    {
        return unmodifiableList(body);
    }

    public final CppClass getDeclaringItem()
    {
        return declaringItem;
    }

    public final CppJavaDoc getJavaDoc()
    {
        return javaDoc;
    }

    public final InlineModifier getInlineModifier()
    {
        return inlineModifier;
    }

    public void setInlineModifier(InlineModifier inlineModifier)
    {
        this.inlineModifier = inlineModifier;
    }

    @Override
    public final CppPrinter accept(CppPrinter printer)
    {
        printer.visit(this);
        return printer;
    }
}
