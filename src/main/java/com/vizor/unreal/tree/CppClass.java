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

import static com.vizor.unreal.tree.CppRecord.Residence.Split;
import static java.util.Collections.unmodifiableList;

public class CppClass extends CppStruct
{
    private final CppType superType;
    private final List<CppFunction> methods;

    public CppClass(final CppType type, final CppType superType, final List<CppField> fields, final List<CppFunction> methods)
    {
        super(type, fields);

        this.superType = superType;
        this.methods = unmodifiableList(methods);

        // Set each method's declaring class to this.
        this.methods.forEach(m -> m.declaringItem = this);

        // 'Split' is default residence type for all classes.
        // Assuming methods and
        setResidence(Split);
    }

    public final List<CppFunction> getMethods()
    {
        return methods;
    }

    public final CppType getSuperType()
    {
        return superType;
    }

    @Override
    public final void setResidence(final Residence residence)
    {
        super.setResidence(residence);
        methods.forEach(m -> m.setResidence(residence));
    }

    @Override
    public final CppPrinter accept(final CppPrinter printer)
    {
        printer.visit(this);
        return printer;
    }
}
