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

import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import com.vizor.unreal.writer.CppPrinter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.vizor.unreal.tree.CppType.Kind.Wildcard;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.nCopies;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

@SuppressWarnings("unused")
public class CppType implements CtLeaf
{
    private static final CppType wildcard = plain("?", Wildcard);

    public enum Passage
    {
        ByValue(""),
        ByRef("&"),
        ByPtr("*");

        private final String symbols;

        Passage(String symbols)
        {
            this.symbols = symbols;
        }

        public final boolean isDefault()
        {
            return (this == ByValue);
        }

        public final String getSymbols()
        {
            return symbols;
        }
    }

    public enum Kind
    {
        Primitive,
        Struct,
        Class,
        Enum,
        Wildcard;

        /**
         * Retrieves a C++ kind name, useful for imports or friend declarations.
         * Class -> 'class';
         * Enum -> 'enum';
         * Struct -> 'struct';
         * Primitive -> '' because primitives has no kind.
         * @return Name of the C++ kind for given instance.
         */
        public final String getCppKindName()
        {
            switch (this)
            {
                case Class:
                case Struct:
                case Enum:
                    return name().toLowerCase();
                default:
                    return "";
            }
        }
    }

    private final List<CppNamespace> namespaces = new ArrayList<>();

    private final String name;
    private final List<CppType> genericParams;
    private final Kind kind;

    /**
     * null if passage is {@link Passage#ByValue}, use {@link CppType#getUnderType()} instead if you want to get a
     * real underlying type.
     */
    private final CppType underType;
    private final Passage passage;

    private final boolean isConstant;
    private final boolean isVolatile;

    private Class<?> nativeClass = null;

    /** The hash code is being cached, because it is kinda hard to calculate **/
    private int hash;

    private CppType(String name, Kind kind)
    {
        this(name, kind, emptyList());
    }

    private CppType(String name, Kind kind, Collection<CppType> genericParams)
    {
        this(name, kind, genericParams, null, Passage.ByValue, false, false);
    }

    private CppType(String name, Kind kind, Collection<CppType> genericParams, CppType underType, Passage passage,
                    boolean isConstant, boolean isVolatile)
    {
        stream(Passage.values()).forEach(p -> {
            if (name.endsWith(p.name()))
                throw new RuntimeException("Incorrect type name, you should use 'make" + p.name() +
                    "()' to make a pointer/reference type instead");
        });

        this.name = name;
        this.kind = kind;

        this.genericParams = genericParams.isEmpty() ? emptyList() : new ArrayList<>(genericParams);

        this.underType = underType;
        this.passage = passage;
        this.isConstant = isConstant;
        this.isVolatile = isVolatile;
    }

    public final boolean isArray()
    {
        return hasNativeType() && nativeClass.isArray();
    }

    public final boolean isMap()
    {
        return hasNativeType() && nativeClass.isAssignableFrom(Map.class);
    }

    public final boolean isKindOf(final Kind kind)
    {
        return this.kind == kind;
    }

    public final boolean isWildcard()
    {
        return this == wildcard;
    }

    public final boolean isGeneric()
    {
        return !genericParams.isEmpty();
    }

    public final boolean isCompiledGeneric()
    {
        return isGeneric() && genericParams.stream().noneMatch(CppType::isWildcard);
    }

    public final boolean isWildcardGeneric()
    {
        return isGeneric() && genericParams.stream().filter(CppType::isWildcard).count() == genericParams.size();
    }

    public final boolean hasNativeType()
    {
        return nonNull(nativeClass);
    }

    public final boolean isA(final Class<?> clazz)
    {
        return hasNativeType() && nativeClass.isAssignableFrom(clazz);
    }

    public final boolean isConstant()
    {
        return isConstant;
    }

    public final boolean isVolatile()
    {
        return isVolatile;
    }

    public final void markAsNative(final Class<?> nativeClass)
    {
        if (isNull(nativeClass))
            throw new RuntimeException("Native class should not be null");

        if (nonNull(this.nativeClass) && !Objects.equals(this.nativeClass, nativeClass))
            throw new RuntimeException("Can not mark " + toString() + " as native " + nativeClass.getSimpleName() +
                ", because it was already marked as " + this.nativeClass.getSimpleName());

        this.nativeClass = nativeClass;
    }

    public final void markAsNativeArray()
    {
        markAsNative(Object[].class);
    }

    public String getName()
    {
        return name;
    }

    public List<CppType> getGenericParams()
    {
        // fast way for non-generic types
        if (genericParams.isEmpty())
            return emptyList();

        return unmodifiableList(genericParams);
    }

    public Set<CppType> getFlatGenericArguments()
    {
        if (!isGeneric())
            return emptySet();

        final Set<CppType> flatTypes = new HashSet<>();

        final List<CppType> upperLevel = new ArrayList<>(genericParams);
        final List<CppType> currentLevel = new ArrayList<>();

        while (!upperLevel.isEmpty())
        {
            flatTypes.addAll(upperLevel);

            upperLevel.forEach(t -> currentLevel.addAll(t.genericParams));

            upperLevel.clear();
            upperLevel.addAll(currentLevel);

            currentLevel.clear();
        }

        return flatTypes;
    }

    public final Kind getKind()
    {
        return kind;
    }

    public Passage getPassage()
    {
        return passage;
    }

    private CppType makeHybrid(final List<CppType> genericParams,
                               final Passage passage,
                               final boolean isConstant,
                               final boolean isVolatile)
    {
        final CppType cppType = new CppType(name, kind, genericParams, getMostUnderType(), passage, isConstant, isVolatile);
        cppType.setNamespaces(getNamespaces());

        // If this is native type, mark compiled type as native too
        if (hasNativeType())
            cppType.markAsNative(nativeClass);

        return cppType;
    }

    public final CppType makeGeneric(final List<CppType> genericParams)
    {
        if (!genericParams.isEmpty())
        {
            if (!isGeneric())
                throw new RuntimeException(toString() + " is not a generic type");

            if (isCompiledGeneric())
                throw new RuntimeException(toString() + " is already a compiled generic type, must be a wildcard generic");

            if (genericParams.size() != this.genericParams.size())
                throw new RuntimeException(toString() + " can not fit such arguments: " + genericParams.toString());
        }

        return makeHybrid(genericParams, passage, isConstant, isVolatile);
    }

    public final CppType makeGeneric(final CppType... genericArguments)
    {
        return makeGeneric(asList(genericArguments));
    }

    public final CppType makeRef(final boolean isConstant, final boolean isVolatile)
    {
        return makeHybrid(genericParams, Passage.ByRef, isConstant, isVolatile);
    }

    public final CppType makeRef()
    {
        return makeHybrid(genericParams, Passage.ByRef, isConstant, isVolatile);
    }

    public final CppType makePtr(final boolean isConstant, final boolean isVolatile)
    {
        return makeHybrid(genericParams, Passage.ByPtr, isConstant, isVolatile);
    }

    public final CppType makePtr()
    {
        return makeHybrid(genericParams, Passage.ByPtr, isConstant, isVolatile);
    }

    public final CppType makeValue(final boolean isConstant, final boolean isVolatile)
    {
        return makeHybrid(genericParams, Passage.ByValue, isConstant, isVolatile);
    }

    public final CppType makeValue()
    {
        return makeHybrid(genericParams, Passage.ByValue, isConstant, isVolatile);
    }

    public final CppType makeConstant(final Passage passage)
    {
        return makeHybrid(genericParams, passage, true, isVolatile);
    }

    public final CppType makeVolatile(final Passage passage)
    {
        return makeHybrid(genericParams, passage, isConstant, true);
    }

    public final CppType makeConstant()
    {
        return makeHybrid(genericParams, passage, true, isVolatile);
    }

    public final CppType makeVolatile()
    {
        return makeHybrid(genericParams, passage, isConstant, true);
    }

    public final CppType getUnderType()
    {
        return isNull(underType) ? null : getMostUnderType();
    }

    private CppType getMostUnderType()
    {
        if (isConstant || isVolatile || !passage.isDefault())
            return requireNonNull(underType, "Hierarchy is broken").getMostUnderType();

        return this;
    }

    public final void setNamespaces(final CppNamespace... namespaces)
    {
        setNamespaces(asList(namespaces));
    }

    public final void setNamespaces(final List<CppNamespace> namespaces)
    {
        this.namespaces.clear();
        this.namespaces.addAll(namespaces);
    }

    public final List<CppNamespace> getNamespaces()
    {
        return unmodifiableList(namespaces);
    }

    public static CppType wildcardGeneric(final String name, final Kind kind, final int numParams)
    {
        return new CppType(name, kind, nCopies(numParams, wildcard));
    }

    public static CppType plain(String name, Kind kind)
    {
        return new CppType(name, kind);
    }

    @Override
    public CppPrinter accept(final CppPrinter printer)
    {
        printer.visit(this);
        return printer;
    }

    @Override
    public final String toString()
    {
        final StringBuilder sb = new StringBuilder();

        for (CppNamespace namespace : namespaces)
        {
            final String namespaceName = (namespace != null) ? namespace.getName() : null;
            if (namespaceName != null)
                sb.append(namespaceName).append("::");
        }


        sb.append(name);

        if (isGeneric())
            sb.append(genericParams.stream().map(CppType::toString).collect(joining(", ", "<", ">")));

        return sb.toString();
    }

    @Override
    public final int hashCode()
    {
        if (hash == 0)
        {
            final List<Object> hashes = new ArrayList<>();

            hashes.add(isConstant);
            hashes.add(isVolatile);
            hashes.add(kind);
            hashes.add(name);

            hashes.addAll(namespaces);
            hashes.add(nativeClass);

            hashes.add(passage);

            if (isGeneric())
                hashes.addAll(getFlatGenericArguments());

            // AbstractList's
            hash = hashes.hashCode();
        }

        return hash;
    }

    @Override
    public final boolean equals(final Object o)
    {
        if (this == o)
            return true;

        if (o instanceof CppType)
        {
            final CppType otherType = (CppType) o;

            return  (isConstant == otherType.isConstant) &&
                    (isVolatile == otherType.isVolatile) &&
                    Objects.equals(kind, otherType.kind) &&
                    Objects.equals(name, otherType.name) &&
                    Objects.equals(namespaces, otherType.namespaces) &&
                    Objects.equals(nativeClass, otherType.nativeClass) &&
                    Objects.equals(passage, otherType.passage) &&
                    (!isGeneric() || Objects.equals(getFlatGenericArguments(), otherType.getFlatGenericArguments()));
        }

        return false;
    }
}
