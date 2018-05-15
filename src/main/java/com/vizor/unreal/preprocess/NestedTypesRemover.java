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
package com.vizor.unreal.preprocess;

import com.squareup.wire.schema.internal.parser.EnumElement;
import com.squareup.wire.schema.internal.parser.FieldElement;
import com.squareup.wire.schema.internal.parser.MessageElement;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import com.squareup.wire.schema.internal.parser.TypeElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.squareup.wire.schema.internal.parser.ProtoFileElement.builder;
import static java.util.stream.Collectors.toList;

public class NestedTypesRemover implements Preprocessor
{
    private Map<String, String> renames = new HashMap<>();

    private String getRenamedName(String prefix, TypeElement te)
    {
        final String previousName = te.name();
        final String renamedName = prefix + '_' + previousName;

        renames.put(previousName, renamedName);
        return renamedName;
    }

    private TypeElement renameMessageElement(String prefix, MessageElement me)
    {
        return MessageElement.builder(me.location())
            .name(getRenamedName(prefix, me))
            .options(me.options())
            .oneOfs(me.oneOfs())
            .reserveds(me.reserveds())
            .location(me.location())
            .groups(me.groups())
            .fields(me.fields())
            .extensions(me.extensions())
            .documentation(me.documentation())
            .build();
    }

    private TypeElement renameEnumElement(String prefix, EnumElement ee)
    {
        return EnumElement.builder(ee.location())
            .name(getRenamedName(prefix, ee))
            .constants(ee.constants())
            .documentation(ee.documentation())
            .location(ee.location())
            .options(ee.options())
            .build();
    }

    private void collectNestedTypes(String prefix, TypeElement te, List<TypeElement> types)
    {
        if (!prefix.isEmpty())
        {
            if (te instanceof MessageElement)
                types.add(renameMessageElement(prefix, (MessageElement)te));
            else if (te instanceof EnumElement)
                types.add(renameEnumElement(prefix, (EnumElement)te));
            else
                throw new RuntimeException("Unknown TypeElement: " + te.toString());
        }
        else
        {
            types.add(te);
        }

        for (TypeElement ne : te.nestedTypes())
            collectNestedTypes(te.name(), ne, types);
    }



    @Override
    public ProtoFileElement process(ProtoFileElement e)
    {
        final List<TypeElement> outTypes = new ArrayList<>();
        e.types().forEach(t -> collectNestedTypes("", t, outTypes));

        final List<TypeElement> typeElements = outTypes.stream()
            .map(t -> {
                if (t instanceof MessageElement)
                {
                    final MessageElement me = (MessageElement) t;

                    return MessageElement.builder(me.location())
                        .name(me.name())
                        .options(me.options())
                        .oneOfs(me.oneOfs())
                        .reserveds(me.reserveds())
                        .location(me.location())
                        .groups(me.groups())
                        .extensions(me.extensions())
                        .documentation(me.documentation())
                        .fields(copyOf(me.fields().stream()
                            .map(f -> {
                                if (renames.containsKey(f.type()))
                                    return FieldElement.builder(f.location())
                                        .type(renames.get(f.type()))
                                        .tag(f.tag())
                                        .options(f.options())
                                        .name(f.name())
                                        .location(f.location())
                                        .label(f.label())
                                        .documentation(f.documentation())
                                        .defaultValue(f.defaultValue())
                                        .build();
                                return f;
                            })
                            .collect(toList())
                        ))
                        .build();
                }
                return t;
            })
            .collect(toList());

        return builder(e.location())
            .location(e.location())
            .packageName(e.packageName())
            .syntax(e.syntax())
            .imports(e.imports())
            .publicImports(e.publicImports())
            .types(copyOf(typeElements))
            .services(e.services())
            .extendDeclarations(e.extendDeclarations())
            .options(e.options())
            .build();
    }
}