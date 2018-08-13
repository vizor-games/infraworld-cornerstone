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

import com.vizor.unreal.util.Misc;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.vizor.unreal.util.Misc.TAB;
import static com.vizor.unreal.util.Misc.removeWhitespaces;
import static com.vizor.unreal.util.Misc.reorder;
import static com.vizor.unreal.util.Misc.snakeCaseToCamelCase;
import static com.vizor.unreal.util.Misc.spaceSeparatedToCamelCase;
import static com.vizor.unreal.util.Misc.splitGeneric;
import static com.vizor.unreal.util.Misc.stringIsNullOrEmpty;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.Collections.shuffle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MiscTest
{
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
    public void testSnakeCaseToCamelCase()
    {
        // Digits
        assertEquals(snakeCaseToCamelCase("42"), "42");
        assertEquals(snakeCaseToCamelCase("_42__"), "42");
        assertEquals(snakeCaseToCamelCase("42_23"), "4223");

        // One word
        assertEquals(snakeCaseToCamelCase("hello"), "Hello");
        assertEquals(snakeCaseToCamelCase("Hello"), "Hello");
        assertEquals(snakeCaseToCamelCase("HELLO"), "Hello");

        assertEquals(snakeCaseToCamelCase("__hello"), "Hello");
        assertEquals(snakeCaseToCamelCase("hello__"), "Hello");
        assertEquals(snakeCaseToCamelCase("_hello_"), "Hello");

        // Two words
        assertEquals(snakeCaseToCamelCase("hello_world"), "HelloWorld");
        assertEquals(snakeCaseToCamelCase("HELLO_WORLD"), "HelloWorld");
        assertEquals(snakeCaseToCamelCase("Hello_World"), "HelloWorld");
        assertEquals(snakeCaseToCamelCase("heLLo_woRLd"), "HelloWorld");

        assertEquals(snakeCaseToCamelCase("__hello__world"), "HelloWorld");
        assertEquals(snakeCaseToCamelCase("Hello__World__"), "HelloWorld");
        assertEquals(snakeCaseToCamelCase("_heLLo__woRLd_"), "HelloWorld");

        // Two words (with digits)
        assertEquals(snakeCaseToCamelCase("42_hello_world"), "42HelloWorld");
        assertEquals(snakeCaseToCamelCase("hello_42_world"), "Hello42World");
        assertEquals(snakeCaseToCamelCase("hello_world_42"), "HelloWorld42");
    }

    @Test
    public void testSnakeCaseToCamelCaseLowercase()
    {
        // Digits
        assertEquals(snakeCaseToCamelCase("42", false), "42");
        assertEquals(snakeCaseToCamelCase("_42__", false), "42");
        assertEquals(snakeCaseToCamelCase("42_23", false), "4223");

        // One word
        assertEquals(snakeCaseToCamelCase("hello", false), "hello");
        assertEquals(snakeCaseToCamelCase("Hello", false), "hello");
        assertEquals(snakeCaseToCamelCase("HELLO", false), "hello");

        assertEquals(snakeCaseToCamelCase("__hello", false), "hello");
        assertEquals(snakeCaseToCamelCase("hello__", false), "hello");
        assertEquals(snakeCaseToCamelCase("_hello_", false), "hello");

        // Two words
        assertEquals(snakeCaseToCamelCase("hello_world", false), "helloWorld");
        assertEquals(snakeCaseToCamelCase("HELLO_WORLD", false), "helloWorld");
        assertEquals(snakeCaseToCamelCase("Hello_World", false), "helloWorld");
        assertEquals(snakeCaseToCamelCase("heLLo_woRLd", false), "helloWorld");

        assertEquals(snakeCaseToCamelCase("__hello__world", false), "helloWorld");
        assertEquals(snakeCaseToCamelCase("Hello__World__", false), "helloWorld");
        assertEquals(snakeCaseToCamelCase("_heLLo__woRLd_", false), "helloWorld");

        // Two words (with digits)
        assertEquals(snakeCaseToCamelCase("42_hello_world", false), "42HelloWorld");
        assertEquals(snakeCaseToCamelCase("hello_42_world", false), "hello42World");
        assertEquals(snakeCaseToCamelCase("hello_world_42", false), "helloWorld42");
    }

    @Test
    public void testSpaceSeparatedToCamelCase()
    {
        // Digits
        assertEquals(spaceSeparatedToCamelCase("42"), "42");
        assertEquals(spaceSeparatedToCamelCase(" 42  "), "42");
        assertEquals(spaceSeparatedToCamelCase("42 23"), "4223");

        // One word
        assertEquals(spaceSeparatedToCamelCase("hello"), "Hello");
        assertEquals(spaceSeparatedToCamelCase("Hello"), "Hello");
        assertEquals(spaceSeparatedToCamelCase("HELLO"), "Hello");

        assertEquals(spaceSeparatedToCamelCase("  hello"), "Hello");
        assertEquals(spaceSeparatedToCamelCase("hello  "), "Hello");
        assertEquals(spaceSeparatedToCamelCase(" hello "), "Hello");

        // Two words
        assertEquals(spaceSeparatedToCamelCase("hello world"), "HelloWorld");
        assertEquals(spaceSeparatedToCamelCase("HELLO WORLD"), "HelloWorld");
        assertEquals(spaceSeparatedToCamelCase("Hello World"), "HelloWorld");
        assertEquals(spaceSeparatedToCamelCase("heLLo woRLd"), "HelloWorld");

        assertEquals(spaceSeparatedToCamelCase("  hello  world"), "HelloWorld");
        assertEquals(spaceSeparatedToCamelCase("Hello  World  "), "HelloWorld");
        assertEquals(spaceSeparatedToCamelCase(" heLLo  woRLd "), "HelloWorld");

        // Two words (with digits)
        assertEquals(spaceSeparatedToCamelCase("42 hello world"), "42HelloWorld");
        assertEquals(spaceSeparatedToCamelCase("hello 42 world"), "Hello42World");
        assertEquals(spaceSeparatedToCamelCase("hello world 42"), "HelloWorld42");
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

    @Test
    public void testNTabs()
    {
        for (int n = 0; n < 100; n++)
        {
            final String nTabs = Misc.nTabs(n);

            assertEquals(nTabs.length(), TAB.length() * n);

            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < n; i++)
                sb.append(TAB);

            assertEquals(nTabs, sb.toString());
        }
    }
}
