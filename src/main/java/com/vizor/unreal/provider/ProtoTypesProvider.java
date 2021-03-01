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
package com.vizor.unreal.provider;

import com.vizor.unreal.tree.CppNamespace;
import com.vizor.unreal.tree.CppType;
import com.vizor.unreal.tree.CppType.Kind;

import java.util.List;
import java.util.Map;

import static com.vizor.unreal.tree.CppType.Kind.Primitive;
import static com.vizor.unreal.tree.CppType.Kind.Struct;
import static com.vizor.unreal.tree.CppType.plain;
import static com.vizor.unreal.tree.CppType.wildcardGeneric;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

@SuppressWarnings("SameParameterValue")
public final class ProtoTypesProvider extends TypesProvider
{
    private static final List<CppNamespace> protobufNamespace = asList(
        new CppNamespace("google"),
        new CppNamespace("protobuf")
    );

    private static final List<CppNamespace> stdNs = singletonList(
        new CppNamespace("std")
    );

    private static CppType plainNs(final String name, final Kind kind, final List<CppNamespace> namespaces)
    {
        final CppType plainType = plain(name, kind);
        plainType.setNamespaces(namespaces);

        return plainType;
    }

    private static CppType genericNs(final String name, final Kind kind, final int numParams, final List<CppNamespace> namespaces)
    {
        final CppType wildcardGenericType = wildcardGeneric(name, kind, numParams);
        wildcardGenericType.setNamespaces(namespaces);

        return wildcardGenericType;
    }

    @Override
    protected final CppType initArrayType()
    {
        final CppType arrayType = genericNs("RepeatedField", Struct, 1, protobufNamespace);
        arrayType.markAsNativeArray();

        return arrayType;
    }

    @Override
    protected final void init()
    {
        register("byte", plain("unsigned char", Primitive), byte.class);
        register("int32", plainNs("int32", Primitive, protobufNamespace), int.class);
        register("int64", plainNs("int64", Primitive, protobufNamespace), long.class);
        register("float", plain("float", Primitive), float.class);
        register("bool", plain("bool", Primitive), boolean.class);
        register("void", plain("void", Primitive), void.class);

        register("uint32", plainNs("uint32", Primitive, protobufNamespace));
        register("uint64", plainNs("uint64", Primitive, protobufNamespace));

        register("string", plainNs("string", Struct, stdNs), String.class);
        register("map", genericNs("Map", Struct, 2, protobufNamespace), Map.class);

        register("oneof", plainNs("variant", Struct, protobufNamespace));

        // because in protobuf (in C++) 'bytes' is actually an std::string :)
        registerAlias("bytes", "string");
    }

    @Override
    public String fixFieldName(final String fieldName, final boolean isBoolean)
    {
        // in google protobuf all variables are in lower case
        return fieldName.toLowerCase();
    }
}
