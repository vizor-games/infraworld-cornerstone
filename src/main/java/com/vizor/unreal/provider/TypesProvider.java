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
import com.vizor.unreal.util.Misc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.vizor.unreal.util.Misc.splitGeneric;
import static java.text.MessageFormat.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toList;

public abstract class TypesProvider
{
    private static final Pattern typePattern = compile("^([A-Za-z][A-Za-z0-9_.]+)(<(.+)>)?$");

    private final Map<String, CppType> types = new HashMap<>();
    private final Map<String, CppType> compiledGenerics = new HashMap<>();

    private final CppType arrayType;

    TypesProvider()
    {
        arrayType = requireNonNull(initArrayType(), "An array type should be initialized, but method "
                + getClass().getCanonicalName() + ".initArrayType() returned null");

        init();
    }

    /**
     * Returns a type from caches
     * @param type Input type name for a certain
     * @return A type, corresponding
     */
    public final CppType get(final String type)
    {
        final Matcher matcher = typePattern.matcher(Misc.removeWhitespaces(type));

        if (!matcher.matches())
            throw new RuntimeException("'" + type + "' doesn't look like a valid type name");

        final int groupCount = matcher.groupCount();
        if (groupCount != 3)
            throw new RuntimeException("Unexpected number of groups: " + groupCount);

        final String typeName = matcher.group(1);
        final String genericParams = matcher.group(3);

        if (nonNull(genericParams))
        {
            if (!compiledGenerics.containsKey(type))
            {
                compiledGenerics.put(type, getGeneric(typeName, splitGeneric(genericParams).stream()
                    .map(this::get)
                    .collect(toList()))
                );
            }

            return compiledGenerics.get(type);
        }

        return getPlainType(typeName);
    }

    public final CppType getNative(final Class<?> clazz)
    {
        return types.values().stream()
            .filter(t -> t.hasNativeType() && t.isA(clazz))
            .findFirst()
            .orElseThrow(() -> new RuntimeException(TypesProvider.this.getClass().getSimpleName() +
                    "doesn't have a native mapping for " + clazz.getSimpleName()));
    }

    private CppType getBestType(final String typeName)
    {
        {
            final CppType foundType = types.get(typeName);

            if (!isNull(foundType))
            {
                return foundType;
            }
        }

        final Pattern typePattern = compile("^(.*\\.)?" + typeName);

        List<Map.Entry<String, CppType>> possibleTypeEntries = types.entrySet()
            .stream()
            .filter(entry->typePattern.matcher(entry.getKey()).matches())
            .collect(Collectors.toList());

        if (possibleTypeEntries.size() > 1)
        {
            throw new RuntimeException(format("Cannot determine best possible type for {} out of {} options", typeName, possibleTypeEntries.size()));
        }

        if (possibleTypeEntries.size() == 0)
        {
            return null;
        }

        return possibleTypeEntries.get(0).getValue();
    }

    private CppType getPlainType(final String typeName)
    {
        final CppType foundType = getBestType(typeName);
        if (isNull(foundType))
            throw new RuntimeException("Can't get a corresponding C++ type for " + typeName);

        // If we've got there, an output type must not be a wildcard generic, but compiled generics are allowed.
        if (foundType.isWildcardGeneric())
            throw new RuntimeException(typeName + " was expected to be a compiled types, but it is actually a wildcard generic"
                    + foundType.toString());

        return foundType;
    }

    public final void register(final String protoType, final CppType cppType)
    {
        register(protoType, cppType, null);
    }

    final void register(final String protoType, final CppType cppType, final Class<?> nativeType)
    {
        final CppType previous = types.put(protoType, cppType);
        if (nonNull(previous))
            throw new RuntimeException("Type association '" + protoType + "' -> '" + previous.getName() +
                    "' is already defined");

        if (nonNull(nativeType))
            cppType.markAsNative(nativeType);
    }

    final void registerAlias(final String protoType, final String cppTypeName)
    {
        if (isNull(protoType) || isNull(cppTypeName))
            throw new RuntimeException("Neither protoType nor cppTypeName shouldn't be null");

        final CppType cppType = types.getOrDefault(cppTypeName, null);
        if (isNull(cppType))
            throw new RuntimeException("Can't putTrough an alias '" + protoType + "' to '" + cppTypeName + "' because the source type name '" +
                    cppTypeName + "' not found");

        register(protoType, cppType);
    }

    public final CppType arrayOf(CppType arrayType)
    {
        return this.arrayType.makeGeneric(arrayType);
    }

    // Overridable methods

    /**
     * Initializes an array type. Been called internally before {@link #init()}
     * @return an array type.
     */
    protected abstract CppType initArrayType();

    /**
     * Init all using types. Been called internally after {@link #initArrayType()}.
     */
    protected abstract void init();

    public abstract String fixFieldName(final String fieldName, final boolean isBoolean);

    private CppType getGeneric(final String typeName, final List<CppType> genericArguments)
    {
        if (genericArguments.isEmpty())
        {
            final String message = format("genericArguments wasn't expected to be empty. Weren't you" +
                            " looking for {0}.getPlainType({1})?", getClass().getSimpleName(), typeName);
            throw new IllegalArgumentException(message);
        }

        final CppType foundType = types.get(typeName);
        if (isNull(foundType))
        {
            final String message = format("{0} doesn't have a corresponding C++ type for {1}",
                    getClass().getSimpleName(), typeName);
            throw new RuntimeException(message);
        }

        if (!foundType.isGeneric())
        {
            final String message = format("{0} was expected to be a generic type, but it is actually a plain" +
                            " (non-generic) type: {1}", typeName, foundType.toString());
            throw new RuntimeException(message);
        }

        if (!foundType.isWildcardGeneric())
        {
            final String message = format("{0} was expected to be a wildcard generic type, but it is actually" +
                    " a compiled generic type: {1}", typeName, foundType.toString());
            throw new RuntimeException(message);
        }

        final int expectedNumArgs = foundType.getGenericParams().size();
        final int gotNumArgs = genericArguments.size();
        if (expectedNumArgs != gotNumArgs)
        {
            final String message = format("{0} needs {1} generic arguments to be compiled, got {2} generic arguments",
                    foundType.toString(), expectedNumArgs, gotNumArgs);
            throw new RuntimeException(message);
        }

        return foundType.makeGeneric(genericArguments);
    }
}
