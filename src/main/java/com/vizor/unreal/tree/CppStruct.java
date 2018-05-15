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
import java.util.List;

import static java.util.Collections.unmodifiableList;

public class CppStruct extends CppRecord
{
    private final CppType type;
    private final List<CppType> friendDeclarations = new ArrayList<>();
    private final List<CppField> fields = new ArrayList<>();

    public final CppJavaDoc javaDoc = new CppJavaDoc();

    public CppStruct(CppType type, List<CppField> fields)
    {
        this.type = type;
        this.fields.addAll(fields);
    }

    public final CppType getType()
    {
        return type;
    }

    public final List<CppField> getFields()
    {
        return unmodifiableList(fields);
    }

    public final void addFriendDeclaration(CppType friend)
    {
        friendDeclarations.add(friend);
    }

    public final List<CppType> getFriendDeclarations()
    {
        return unmodifiableList(friendDeclarations);
    }

    @Override
    public CppPrinter accept(CppPrinter printer)
    {
        printer.visit(this);
        return printer;
    }

    @Override
    public void setResidence(Residence residence)
    {
        super.setResidence(residence);
        fields.forEach(f -> f.setResidence(residence));
    }

    @Override
    public String toString()
    {
        return type.toString();
    }
}
