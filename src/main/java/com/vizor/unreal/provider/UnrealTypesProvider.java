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

import com.vizor.unreal.tree.CppType;

import java.nio.ByteBuffer;
import java.util.Map;

import static com.vizor.unreal.tree.CppType.Kind.Primitive;
import static com.vizor.unreal.tree.CppType.Kind.Struct;
import static com.vizor.unreal.tree.CppType.plain;
import static com.vizor.unreal.tree.CppType.wildcardGeneric;
import static com.vizor.unreal.util.Misc.sanitizeVarName;

public final class UnrealTypesProvider extends TypesProvider
{
    @Override
    protected final CppType initArrayType()
    {
        final CppType arrayType = wildcardGeneric("TArray", Struct, 1);
        arrayType.markAsNativeArray();

        return arrayType;
    }

    @Override
    protected final void init()
    {
        register("byte", plain("uint8", Primitive), byte.class);
        register("int32", plain("int32", Primitive), int.class);
        register("int64", plain("int64", Primitive), long.class);
        register("float", plain("float", Primitive), float.class);
        register("bool", plain("bool", Primitive), boolean.class);
        register("void", plain("void", Primitive), void.class);

        registerAlias("int", "int32");
        registerAlias("uint32", "int32");
        registerAlias("uint64", "int64");

        register("string", plain("FString", Struct), String.class);
        register("map", wildcardGeneric("TMap", Struct, 2), Map.class);

        // 'bytes' -> TArray<uint8> (ByteBuffer because 'bytes' doesn't truly conforms to 'array').
        register("bytes", plain("FByteArray", Struct), ByteBuffer.class);

        register("oneof", plain("TVariant", Struct));
    }

    @Override
    public String fixFieldName(final String fieldName, final boolean isBoolean)
    {
        return sanitizeVarName(fieldName, isBoolean);
    }
}
