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
package com.vizor.unreal;

import com.vizor.unreal.tree.CppNamespace;
import com.vizor.unreal.tree.CppType;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;

import static com.vizor.unreal.tree.CppType.Passage.ByPtr;
import static com.vizor.unreal.tree.CppType.Passage.ByRef;
import static com.vizor.unreal.tree.CppType.Passage.ByValue;
import static com.vizor.unreal.tree.CppType.plain;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TypeTest
{
    private final CppType stringType = plain("FString", CppType.Kind.Struct);
    private final CppType intType = plain("Integer", CppType.Kind.Struct);

    @Test
    public void structTest()
    {
    }

    @Test
    public void wildcardGenericTest()
    {
        final CppType wildcardGeneric = CppType.wildcardGeneric("map", CppType.Kind.Struct, 2);

        final List<CppType> genericArguments = wildcardGeneric.getGenericParams();
        assertEquals(genericArguments.getClass(), unmodifiableList(emptyList()).getClass());
        assertEquals(2, genericArguments.size());

        // Check whether is generic
        assertTrue(wildcardGeneric.isGeneric());
        assertTrue(wildcardGeneric.isWildcardGeneric());
        assertFalse(wildcardGeneric.isCompiledGeneric());

        // Check whether all args are wildcards
        wildcardGeneric.getGenericParams().forEach(s -> assertTrue(s.isWildcard()));
    }

    @Test
    public void namespacedGenericTest()
    {
        final List<CppNamespace> namespaces = singletonList(new CppNamespace("std"));

        final CppType wildcardPtr = CppType.wildcardGeneric("shared_ptr", CppType.Kind.Struct, 1);
        wildcardPtr.setNamespaces(namespaces);
        assertEquals(namespaces, wildcardPtr.getNamespaces());

        final CppType intPtr = wildcardPtr.makeGeneric(plain("int32", CppType.Kind.Primitive));
        assertEquals(namespaces, intPtr.getNamespaces());
    }

    @Test
    public void compiledGenericTest()
    {
        final CppType int32Type = plain("int32", CppType.Kind.Primitive);

        final CppType wildcardGeneric = CppType.wildcardGeneric("map", CppType.Kind.Struct, 2);
        final CppType compiledGeneric = wildcardGeneric.makeGeneric(stringType, int32Type);

        // Check number of arguments
        final List<CppType> genericArguments = compiledGeneric.getGenericParams();
        assertEquals(genericArguments.getClass(), unmodifiableList(emptyList()).getClass());
        assertEquals(2, genericArguments.size());

        // Check whether is generic
        assertTrue(compiledGeneric.isGeneric());
        assertTrue(compiledGeneric.isCompiledGeneric());
        assertFalse(compiledGeneric.isWildcardGeneric());

        // Check whether all our types are contained
        assertArrayEquals(genericArguments.toArray(), new CppType[]{stringType, int32Type});
    }

    @Test
    public void makeTest()
    {
        final CppType constString = stringType.makeConstant();
        final CppType constRefString = constString.makeRef();
        final CppType volatilePtrString = stringType.makePtr(false, true);

        // Check passage
        assertEquals(stringType.getPassage(), ByValue);
        assertEquals(constString.getPassage(), ByValue);
        assertEquals(constRefString.getPassage(), ByRef);
        assertEquals(volatilePtrString.getPassage(), CppType.Passage.ByPtr);

        // All derivative's underlying type should be 'string type'
        assertNull(stringType.getUnderType());
        assertEquals(constString.getUnderType(), stringType);
        assertEquals(constRefString.getUnderType(), stringType);
    }

    @Test
    public void refPassageTest()
    {
        final CppType stringRef = stringType.makeRef();

        assertEquals(stringRef.getPassage(), ByRef);
        assertNotEquals(stringRef.getPassage(), ByPtr);
        assertNotEquals(stringRef.getPassage(), ByValue);
    }

    @Test
    public void ptrPassageTest()
    {
        final CppType ptr = stringType.makePtr();

        assertNotEquals(ptr.getPassage(), ByRef);
        assertEquals(ptr.getPassage(), ByPtr);
        assertNotEquals(ptr.getPassage(), ByValue);
    }

    @Test
    public void valuePassageTest()
    {
        final CppType stringValue = stringType.makeValue();

        // must be a new instance
        assertNotSame(stringType, stringValue);

        assertNotEquals(stringValue.getPassage(), ByRef);
        assertNotEquals(stringValue.getPassage(), ByPtr);
        assertEquals(stringValue.getPassage(), ByValue);
    }

    @Test
    public void cvQualifierTest()
    {
        final CppType constString = stringType.makeConstant();
        final CppType constRefString = constString.makeRef();
        final CppType volatilePtrString = stringType.makePtr(false, true);

        // Check is constant and is volatile
        assertFalse(stringType.isConstant());
        assertFalse(stringType.isVolatile());
        assertTrue(constString.isConstant());
        assertFalse(constString.isVolatile());
        assertTrue(constRefString.isConstant());
        assertFalse(constRefString.isVolatile());
        assertFalse(volatilePtrString.isConstant());
        assertTrue(volatilePtrString.isVolatile());
    }

    @Test
    public void equalityTest()
    {
        final String integerTypeName = "Integer";

        final CppType integerPrimitive1 = plain(integerTypeName, CppType.Kind.Primitive);
        final CppType integerPrimitive2 = plain(integerTypeName, CppType.Kind.Primitive);

        final CppType integerClass1 = plain(integerTypeName, CppType.Kind.Class);
        final CppType integerClass2 = plain(integerTypeName, CppType.Kind.Class);

        // 'equals()' is overloaded. thus different instances must be equal.
        assertEquals(integerPrimitive1, integerPrimitive2);
        assertEquals(integerClass1, integerClass2);

        // if 'kind' was changed - must not be equals
        assertNotEquals(integerPrimitive1, integerClass1);
        assertNotEquals(integerClass1, integerPrimitive1);

        // if 'passage' was changed - mustn't be equals
        assertNotEquals(integerPrimitive1, integerPrimitive1.makeRef());
        assertNotEquals(integerPrimitive1, integerPrimitive1.makePtr());

        // but should be equals for changed, but identical passage
        assertEquals(integerPrimitive1.makeRef(), integerPrimitive2.makeRef());
        assertEquals(integerPrimitive1.makePtr(), integerPrimitive2.makePtr());
    }

    @Test
    public void flatGenericArgumentsTest()
    {
        final CppType list = CppType.wildcardGeneric("List", CppType.Kind.Class, 1);
        final CppType map = CppType.wildcardGeneric("Map", CppType.Kind.Class, 2);

        final CppType intList = list.makeGeneric(intType);
        final CppType stringList = list.makeGeneric(stringType);

        final CppType intToStringList = map.makeGeneric(intType, stringList);
        final CppType stringToIntList = map.makeGeneric(stringType, intList);

        final CppType complexMap = map.makeGeneric(intToStringList, stringToIntList);

        assertEquals(complexMap.getFlatGenericArguments(), new HashSet<>(asList(
            intType,
            stringType,
            intList,
            stringList,
            intToStringList,
            stringToIntList
        )));
    }
}
