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
package com.vizor.unreal.writer.annotation;

import com.vizor.unreal.tree.CppAnnotation;
import com.vizor.unreal.tree.CppArgument;
import com.vizor.unreal.tree.CppClass;
import com.vizor.unreal.tree.CppEnum;
import com.vizor.unreal.tree.CppField;
import com.vizor.unreal.tree.CppFunction;
import com.vizor.unreal.tree.CppRecord;
import com.vizor.unreal.tree.CppStruct;
import com.vizor.unreal.writer.CppPrinter;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static com.vizor.unreal.util.Misc.stringIsNullOrEmpty;
import static java.util.Comparator.comparingInt;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;

public class UEDecoratorWriter extends DummyDecoratorWriter
{
    private static final Map<Class<?>, BiFunction<Collection<String>, Collection<String>, String>> annotators = new HashMap<>();

    static {
        annotators.put(CppEnum.class, CppAnnotation::uenumOf);
        annotators.put(CppClass.class, CppAnnotation::uclassOf);
        annotators.put(CppStruct.class, CppAnnotation::ustructOf);
        annotators.put(CppField.class, CppAnnotation::upropertyOf);
        annotators.put(CppArgument.class, CppAnnotation::uparamOf);
        annotators.put(CppFunction.class, CppAnnotation::ufunctionOf);
    }

    private final String apiName;

    public UEDecoratorWriter(String apiName)
    {
        this.apiName = apiName;
    }

    private List<String> getAnnotations(Map<CppAnnotation, String> annotations, boolean metaAnnotations)
    {
        return annotations.entrySet().stream()
                .filter(kv -> kv.getKey().isMeta == metaAnnotations)
                .sorted(comparingInt(o -> o.getKey().ordinal()))
                .map(kv -> {
                    final String key = kv.getKey().name();
                    if (nonNull(kv.getValue()))
                        return key + "=\"" + kv.getValue() + "\"";

                    return key;
                })
                .collect(toList());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void writeAnnotations(CppPrinter p, CppRecord e)
    {
        if (isNull(p))
            throw new RuntimeException("A printer you've just passed is null");

        if (nonNull(e))
        {
            final Class<?> clazz = e.getClass();

            final Map<CppAnnotation, String> annotations = e.getAnnotations();
            final List<String> nonMeta = getAnnotations(annotations, false);
            final List<String> meta = getAnnotations(annotations, true);

            if (annotators.containsKey(clazz))
                p.writeLine(annotators.get(clazz).apply(nonMeta, meta));
            else
                throw new RuntimeException("Don't know how to annotate " + clazz.getSimpleName());
        }
    }

    @Override
    public void writeApi(CppPrinter printer)
    {
        if (!stringIsNullOrEmpty(apiName))
            printer.write(apiName).write("_API").write(" ");
    }

    @Override
    public void writeGeneratedBody(CppPrinter printer, CppRecord element)
    {
        final Class<?> clazz = element.getClass();

        // One should not use GENERATED_UCLASS_BODY, because it cause 'unresolved external symbol issue'
        if (clazz.equals(CppStruct.class))
            printer.writeLine("GENERATED_USTRUCT_BODY()");
//        else if (clazz.equals(CppClass.class))
//            printer.writeLine("GENERATED_UCLASS_BODY()");
        else
            printer.writeLine("GENERATED_BODY()");
    }
}
