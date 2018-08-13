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

import java.util.Collection;

import static java.lang.String.join;
import static java.util.Collections.emptyList;

public enum CppAnnotation
{
    Blueprintable(CppClass.class),
    BlueprintCallable(CppFunction.class),
    BlueprintType(CppStruct.class, CppClass.class),

    // Fields
    BlueprintReadOnly(CppField.class),
    BlueprintWriteOnly(CppField.class),
    BlueprintReadWrite(CppField.class),
    Transient(CppField.class),

    // Events
    BlueprintImplementableEvent,
    BlueprintNativeEvent,
    BlueprintAssignable,

    // Params
    ref(CppArgument.class),

    DisplayName(true, CppStruct.class, CppField.class, CppFunction.class, CppClass.class),
    ToolTip(true, CppStruct.class, CppField.class, CppFunction.class, CppClass.class),

    // Misc
    Category(CppRecord.class);

    public final boolean isMeta;
    private final Class<? extends CppRecord>[] targetClasses;

    @SafeVarargs
    CppAnnotation(Class<? extends CppRecord>... targetClasses)
    {
        this(false, targetClasses);
    }

    @SafeVarargs
    CppAnnotation(boolean isMeta, Class<? extends CppRecord>... targetClasses)
    {
        this.isMeta = isMeta;
        this.targetClasses = targetClasses;
    }

    private static String flattenProps(final Collection<String> nonMeta, final Collection<String> meta)
    {
        // if has some meta, wrap 'meta=(a, b, c)' and putTrough it to the end
        if (!meta.isEmpty())
            nonMeta.add("meta=(" + join(", ", meta) + ")");

        return join(", ", nonMeta);
    }

    public static String upropertyOf(final Collection<String> nonMeta, final Collection<String> meta)
    {
        return "UPROPERTY(" + flattenProps(nonMeta, meta) + ')';
    }

    public static String ustructOf(final Collection<String> nonMeta, final Collection<String> meta)
    {
        return "USTRUCT(" + flattenProps(nonMeta, meta) + ')';
    }

    public static String uclassOf(final Collection<String> nonMeta, final Collection<String> meta)
    {
        return "UCLASS(" + flattenProps(nonMeta, meta) + ')';
    }

    public static String uparamOf(final Collection<String> nonMeta, final Collection<String> meta)
    {
        return "UPARAM(" + flattenProps(nonMeta, meta) + ')';
    }

    public static String ufunctionOf(final Collection<String> nonMeta, final Collection<String> meta)
    {
        return "UFUNCTION(" + flattenProps(nonMeta, meta) + ')';
    }

    public static String uenumOf(final Collection<String> nonMeta, final Collection<String> meta)
    {
        return "UENUM(" + flattenProps(nonMeta, meta) + ')';
    }

    public static String umetaOf(final Collection<String> meta)
    {
        return "UMETA(" + flattenProps(meta, emptyList()) + ')';
    }
}