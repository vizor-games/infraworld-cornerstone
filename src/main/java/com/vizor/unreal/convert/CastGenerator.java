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
package com.vizor.unreal.convert;

import com.vizor.unreal.tree.CppArgument;
import com.vizor.unreal.tree.CppField;
import com.vizor.unreal.tree.CppFunction;
import com.vizor.unreal.tree.CppNamespace;
import com.vizor.unreal.tree.CppStruct;
import com.vizor.unreal.tree.CppType;
import com.vizor.unreal.util.Tuple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.function.BiFunction;

import static com.vizor.unreal.tree.CppRecord.Residence.Cpp;
import static com.vizor.unreal.tree.CppType.Kind.Enum;
import static com.vizor.unreal.tree.CppType.Kind.Struct;
import static java.lang.System.lineSeparator;
import static java.text.MessageFormat.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;

class CastGenerator
{
    private static final boolean generateCastComments = true;
    private static final String typeCastHint = "// {0}::{1} <- {2}::{3}" + lineSeparator();
    private static final String dataTypeHint = "// {0} ({1}) <- {2} ({3})" + lineSeparator();

    private static final String inputItemName = "InItem";
    private static final String outputItemName = "OutItem";

    private static final Logger log = LogManager.getLogger(CastGenerator.class);

    public enum CastMethod
    {
        PtrArrayCast,
        ArrayCast,
        MapCast,
        EnumCast,
        Cast;

        private final String castMethodName;

        CastMethod()
        {
            castMethodName = "Proto_" + name();
        }

        public final String getMethodName()
        {
            return castMethodName;
        }
    }

    /**
     * Generates casts from CPP to UE and backwards.
     *
     * @param structures tuples for type cast.
     *                   First should be a PROTO (C++) structure
     *                   Second should be a UE4 structure
     * @return A namespace where casts reside.
     */
    final CppNamespace genCasts(final List<Tuple<CppStruct, CppStruct>> structures)
    {
        final CppNamespace ns = new CppNamespace("casts");

        structures.forEach(t -> {
            final CppStruct cppStruct = t.first();
            final CppStruct ueStruct = t.second();

            ns.add(generateCast(cppStruct, ueStruct, this::generateProtoToUeCast));
            ns.add(generateCast(ueStruct, cppStruct, this::generateUeToProtoCast));
        });

        ns.setResidence(Cpp);
        return ns;
    }

    private CppFunction generateCast(final CppStruct inStruct, final CppStruct outStruct,
                                     final BiFunction<CppField, CppField, String> genFunction)
    {
        final CppType inType = inStruct.getType();
        final CppType outType = outStruct.getType();

        final List<CppField> cppFields = inStruct.getFields();
        final List<CppField> ueFields = outStruct.getFields();

        final StringBuilder body = new StringBuilder();
        body.append(outType.toString()).append(' ').append(outputItemName).append(';');
        body.append(lineSeparator());

        if (generateCastComments)
            body.append(lineSeparator());

        for (int i = 0; i < cppFields.size(); i++)
        {
            final CppField firstField = cppFields.get(i);
            final CppField secondField = ueFields.get(i);

            final CppType firstType = firstField.getType();
            final CppType secondType = secondField.getType();

            if (generateCastComments)
            {
                body.append(format(typeCastHint, outType.getName(), secondField.getName(), inType.getName(),
                        firstField.getName()));
                body.append(format(dataTypeHint, secondType.getKind(), secondType.toString(), firstType.getKind(),
                        firstType.toString()));
            }

            body.append(genFunction.apply(firstField, secondField));

            if (generateCastComments)
                body.append(lineSeparator());

            body.append(lineSeparator());
        }

        body.append("return ").append(outputItemName).append(';');

        final CastMethod castMethod = getCastMethod(inType);
        final CppFunction castFunction = new CppFunction(castMethod.getMethodName(), outType,
                singletonList(new CppArgument(inType.makeRef(), inputItemName)), emptyList());

        castFunction.setBody(body.toString());
        castFunction.setInlineModifier(CppFunction.InlineModifier.ForceInline);
        castFunction.enableAnnotations(false);

        return castFunction;
    }

    private String generateProtoToUeCast(CppField inField, CppField outField)
    {
        final CppType inType = inField.getType();
        final CppType outType = outField.getType();

        final List<CppType> params = outType.getGenericParams();
        final CastMethod castMethod = getCastMethod(inType);

        if (inType.isMap())
        {
            if (params.size() != 2)
            {
                throw new RuntimeException("For a map " + inType.getName() +
                        " type, the number of args should be 2, not: " + params.size());
            }

            final String castPattern =  outputItemName + ".{0} = " + castMethod.getMethodName() + "<{1}>(Item.{2}());";
            return format(castPattern, outField.getName(), params.stream().map(CppType::toString).collect(joining(", ")),
                    inField.getName());
        }
        else
        {
            final String castedTypename = outType.isArray() ?
                    params.stream().map(CppType::toString).collect(joining(", ")) : outType.toString();

            final String pattern = outputItemName + ".{0} = " + castMethod.getMethodName() + "<{1}>(" + inputItemName + ".{2}());";

            return format(pattern, outField.getName(), castedTypename, inField.getName());
        }
    }

    private String generateUeToProtoCast(CppField inField, CppField outField)
    {
        final CppType inType = inField.getType();
        final CppType outType = outField.getType();

        final List<CppType> params = outType.getGenericParams();
        final String paramsArgs = params.stream().map(CppType::toString).collect(joining(", "));

        final CastMethod castMethod = getCastMethod(inType);
        final String completeCast;

        if (inType.isMap())
        {
            if (params.size() != 2)
            {
                throw new RuntimeException("For a map " + inType.getName() +
                        " type, the number of args should be 2, not: " + params.size());
            }

            final String declarationName = "CastedMap_" + outField.getName();

            @SuppressWarnings("StringBufferReplaceableByString")
            final StringBuilder sb = new StringBuilder();

            sb.append("const ").append(outType.toString()).append("& ").append(declarationName).append(" = ");
            sb.append(castMethod.getMethodName()).append('<').append(paramsArgs).append('>');
            sb.append('(').append(inputItemName).append('.').append(inField.getName()).append(");");
            sb.append(lineSeparator());

            sb.append(outputItemName).append(".mutable_").append(outField.getName()).append("()->insert(");
            sb.append(declarationName).append(".begin(), ").append(declarationName).append(".end());");

            completeCast = sb.toString();
        }
        else if (inType.isArray())
        {
            final String pattern = outputItemName + ".mutable_{0}()->CopyFrom(" + castMethod.getMethodName() + "<{1}>(" + inputItemName + ".{2}));";
            completeCast =  format(pattern, outField.getName(), paramsArgs, inField.getName());
        }
        else
        {
            // If receiver type is a structure, but not string, we should set the casted structure via
            // 'set_allocated_...()', not just 'set_...()' method.
            if (outType.isKindOf(Struct) && !outType.isA(String.class))
            {
                final StringBuilder sb = new StringBuilder();

                // Create a temporary variable and assign the result to it.
                final String declarationName = "CastedStruct_" + outField.getName();

                sb.append(outType.toString()).append(" ").append(declarationName).append(" = ");
                sb.append(castMethod.getMethodName()).append('<').append(outType.toString()).append('>');
                sb.append('(').append(inputItemName).append('.').append(inField.getName()).append(");");
                sb.append(lineSeparator());

                if (generateCastComments)
                {
                    sb.append("// ! Need to instantiate a new ").append(outType.toString())
                            .append(" to be possessed by the outer item");
                    sb.append(lineSeparator());
                }

                // Assign a temp variable to the field via 'set_allocated_...()'
                sb.append(outputItemName).append(".set_allocated_").append(outField.getName());
                sb.append("(new ").append(outType.toString()).append('(').append(declarationName).append("));");

                completeCast =  sb.toString();
            }
            else
            {
                final String pattern = outputItemName + ".set_{0}(" + castMethod.getMethodName() + "<{1}>(" + inputItemName + ".{2}));";
                completeCast =  format(pattern, outField.getName(), outType.toString(), inField.getName());
            }
        }

        log.debug("Generated cast: {}", completeCast);
        return completeCast;
    }

    private CastMethod getCastMethod(CppType typeToCast)
    {
        if (typeToCast.isArray())
        {
            final CppType arrayGenericType = typeToCast.getGenericParams().get(0);

            if (arrayGenericType.isKindOf(Struct))
                return CastMethod.PtrArrayCast;
            else
                return CastMethod.ArrayCast;

        }
        else if (typeToCast.isMap())
        {
            return CastMethod.MapCast;
        }
        else if (typeToCast.isKindOf(Enum))
        {
            return CastMethod.EnumCast;
        }
        else
        {
            return CastMethod.Cast;
        }
    }
}
