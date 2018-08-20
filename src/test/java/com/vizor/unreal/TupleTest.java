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

import com.vizor.unreal.util.Tuple;
import org.junit.Test;

import java.util.Random;

import static java.lang.Integer.valueOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TupleTest
{
    @Test
    public void testConstruction()
    {
        final Tuple<Integer, String> t = Tuple.of(1, "Hello");

        assertEquals(t.first(), valueOf(1));
        assertEquals(t.second(), "Hello");
    }

    @Test
    public void testInvert()
    {
        final Tuple<String, Integer> t = Tuple.of(1, "Hello").rotate();

        assertEquals(t.first(), "Hello");
        assertEquals(t.second(), valueOf(1));
    }

    @Test
    public void testReduce()
    {
        final Tuple<Integer, String> t = Tuple.of(1, "Hello");

        assertEquals(t.reduce((f, s) -> (f + 1) + s), "2Hello");
    }

    @Test
    public void testObject()
    {
        final Random r = new Random();

        // An instance should be equal to self
        final Tuple<String, String> t = Tuple.of("1", "2");
        assertEquals(t, t);

        // Different instances should be equal if their content is equal
        for (int i = 0; i < 100; i++)
        {
            final int f = r.nextInt();
            final String s = String.valueOf(r.nextInt());

            final Tuple<Integer, String> t1 = Tuple.of(f, s);
            final Tuple<Integer, String> t2 = Tuple.of(f, s);

            // equals() is overloaded, different instances should be equals
            assertEquals(t1, t2);

            // as well as their hash code
            assertEquals(t1.hashCode(), t2.hashCode());

            // toString() representation should be stable
            assertEquals(t1.toString(), t2.toString());
        }

        // And should not be equal if their content is different
        final Tuple<String, String> ne1 = Tuple.of("1", "2");
        final Tuple<String, String> ne2 = Tuple.of("2", "1");

        assertNotEquals(ne1, ne2);
        assertNotEquals(ne1, "12");
    }
}
