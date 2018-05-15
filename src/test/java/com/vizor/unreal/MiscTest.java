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

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.vizor.unreal.util.Misc.enumToString;
import static com.vizor.unreal.util.Misc.lineWiseIndent;
import static com.vizor.unreal.util.Misc.lineWiseUnindent;
import static com.vizor.unreal.util.Misc.removeWhitespaces;
import static com.vizor.unreal.util.Misc.reorder;
import static com.vizor.unreal.util.Misc.rotateMap;
import static com.vizor.unreal.util.Misc.splitGeneric;
import static com.vizor.unreal.util.Misc.stringIsNullOrEmpty;
import static com.vizor.unreal.util.Misc.toCamelCase;
import static java.lang.Integer.valueOf;
import static java.lang.Math.abs;
import static java.lang.String.join;
import static java.lang.System.lineSeparator;
import static java.util.Arrays.asList;
import static java.util.Collections.shuffle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MiscTest
{
    @SuppressWarnings("unused")
    private enum TestEnum
    {
        A,
        Bb,
        Ccc,
        Dddd,
        Eeeee,
        Ffffff,
    }

    @Test
    public void testEnumToString()
    {
        assertEquals(enumToString(TestEnum.class), "A, Bb, Ccc, Dddd, Eeeee, Ffffff");
    }

    @Test
    public void testIndentUnindent()
    {
        // Number of attemps
        final int numAttempts = 100;

        final String src = join(lineSeparator(), asList(
            "Hello",
            "World"
        ));

        final Random random = new Random();

        // Since src wasn't indented, unindenting it will cause no effect
        random.ints().limit(numAttempts).forEach(randInt -> assertEquals(src, lineWiseUnindent(src, abs(randInt))));

        // Indent and then unindent, then compare to the source
        for (int i = 0; i < numAttempts; i++)
        {
            final int numTabs = random.nextInt(1000);

            final String indented = lineWiseIndent(src, numTabs);
            final String unindented = lineWiseUnindent(indented, numTabs);

            assertEquals(src, unindented);
        }
    }

    @Test
    public void testSplitGeneric()
    {
        final List<String> items = new ArrayList<>(asList(
            "String",
            "Integer",
            "Character",
            "Set<String>",
            "Set<Integer>",
            "Set<Character>",
            "List<String>",
            "List<Integer>",
            "List<Character>",
            "Map<String,String>",
            "Map<String,Integer>",
            "Map<Integer,String>",
            "Map<Integer,Integer>",
            "Map<String,Character>",
            "Map<Character,Integer>",
            "Map<Integer,Character>",
            "Map<Integer,Integer>"
        ));

        final Random random = new Random();

        for (int i = 0; i < 1000; i++)
        {
            shuffle(items);

            // Take first [0, size) items
            final List<String> initial = items.subList(0, random.nextInt(items.size()));

            final String joined = join(",", initial);
            assertEquals(initial, splitGeneric(joined));
        }
    }

    @Test
    public void testRemoveWhitespaces()
    {
        assertTrue(removeWhitespaces("    ").isEmpty());
        assertEquals(removeWhitespaces("    "), "");

        assertEquals(removeWhitespaces("    Hello"), "Hello");
        assertEquals(removeWhitespaces("Hello    "), "Hello");
        assertEquals(removeWhitespaces("  Hello  "), "Hello");

        assertEquals(removeWhitespaces("H e l l o"), "Hello");
    }

    @Test
    public void testReorder()
    {
        // integers should be a mutable array
        final List<Integer> integers = new ArrayList<>();
        integers.add(10);
        integers.add(20);
        integers.add(30);

        reorder(integers, new int[]{0, 1, 2, 2, 1, 0});

        assertEquals(integers, asList(10, 20, 30, 30, 20, 10));
    }

    @Test
    public void testRotateMap()
    {
        final HashMap<Integer, String> map = new HashMap<>();
        map.put(0, "Zero");
        map.put(1, "One");
        map.put(2, "Two");
        map.put(3, "Three");
        map.put(4, "Four");

        final Map<String, Integer> rot = rotateMap(map);

        assertEquals(rot.get("Zero"), valueOf(0));
        assertEquals(rot.get("One"), valueOf(1));
        assertEquals(rot.get("Two"), valueOf(2));
        assertEquals(rot.get("Three"), valueOf(3));
        assertEquals(rot.get("Four"), valueOf(4));

        // Rotate againg and compare with previous
        assertEquals(map, rotateMap(rot));
    }

    @Test
    public void testToCamelCase()
    {
        // Digits
        assertEquals(toCamelCase("42"), "42");
        assertEquals(toCamelCase("_42__"), "42");
        assertEquals(toCamelCase("42_23"), "4223");

        // One word
        assertEquals(toCamelCase("hello"), "Hello");
        assertEquals(toCamelCase("Hello"), "Hello");
        assertEquals(toCamelCase("HELLO"), "Hello");

        assertEquals(toCamelCase("__hello"), "Hello");
        assertEquals(toCamelCase("hello__"), "Hello");
        assertEquals(toCamelCase("_hello_"), "Hello");

        // Two words
        assertEquals(toCamelCase("hello_world"), "HelloWorld");
        assertEquals(toCamelCase("HELLO_WORLD"), "HelloWorld");
        assertEquals(toCamelCase("Hello_World"), "HelloWorld");
        assertEquals(toCamelCase("heLLo_woRLd"), "HelloWorld");

        assertEquals(toCamelCase("__hello__world"), "HelloWorld");
        assertEquals(toCamelCase("Hello__World__"), "HelloWorld");
        assertEquals(toCamelCase("_heLLo__woRLd_"), "HelloWorld");

        // Two words (with digits)
        assertEquals(toCamelCase("42_hello_world"), "42HelloWorld");
        assertEquals(toCamelCase("hello_42_world"), "Hello42World");
        assertEquals(toCamelCase("hello_world_42"), "HelloWorld42");
    }

    @Test
    @SuppressWarnings("StringBufferReplaceableByString")
    public void testStringIsNullOrEmpty()
    {
        assertTrue(stringIsNullOrEmpty(null));
        assertTrue(stringIsNullOrEmpty(""));

        assertTrue(stringIsNullOrEmpty(new StringBuilder().toString()));

        assertFalse(stringIsNullOrEmpty("null"));
        assertFalse(stringIsNullOrEmpty("0"));
        assertFalse(stringIsNullOrEmpty("zero"));
    }
}
