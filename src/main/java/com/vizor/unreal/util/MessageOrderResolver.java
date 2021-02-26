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

import com.vizor.unreal.tree.CppField;
import com.vizor.unreal.tree.CppStruct;
import com.vizor.unreal.tree.CppType;
import com.vizor.unreal.util.Graph.GraphHasCyclesException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MessageOrderResolver
{
    public int[] sortByInclusion(final List<CppStruct> structures)
    {
        final Set<CppType> cache = new HashSet<>(structures.size());
        final List<CppType> list = new ArrayList<>(structures.size());

        structures.forEach(cppStruct -> {
            cache.add(cppStruct.getType());
            list.add(cppStruct.getType());
        });

        final Graph<CppType> graph = new Graph<>(list);
        for (final CppStruct struct : structures)
        {
            for (final CppField field : struct.getFields())
            {
                final CppType fieldType = field.getType();

                // If a field type has a reference to this struct type - add it as edge
                if (cache.contains(fieldType))
                    graph.addEdge(fieldType, struct.getType());

                // It the field's type is generic class - perform the same inclusion check for all it's arguments
                // getFlatGenericArguments() returns an empty collection if it doesn't contain any generic arguments
                fieldType.getFlatGenericArguments().stream()
                    .filter(cache::contains)
                    .forEach(genericArg -> graph.addEdge(genericArg, struct.getType()));

                fieldType.getFlatVariantArguments().stream()
                        .filter(cache::contains)
                        .forEach(variantArg -> graph.addEdge(variantArg, struct.getType()));
            }
        }

        try
        {
            return graph.getOrder();
        }
        catch (GraphHasCyclesException e)
        {
            throw new RuntimeException(e);
        }
    }
}
