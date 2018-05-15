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

import com.vizor.unreal.util.Graph;
import com.vizor.unreal.util.Graph.GraphHasCyclesException;
import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.shuffle;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GraphTest
{
    @Test
    public void graphReduceTest()
    {
        final Graph<Integer> graph = new Graph<>(1, 2, 3);
        graph.addEdge(2, 1);
        graph.addEdge(1, 2);

        graph.removeUnreachableNodes();

        final List<Integer> expected = asList(1, 2);
        assertArrayEquals(graph.getVertices().toArray(), expected.toArray());

        // This graph has cross-refs
        assertTrue(graph.hasCycles());
    }

    @Test
    public void topologySortTest() throws GraphHasCyclesException
    {
        final List<Integer> givenVertices = asList(1, 2, 3, 4, 5);

        // Perform n times to ensure stability
        for (int i = 0; i < givenVertices.size(); i++)
        {
            // The order doesn't matter (have to prove it)
            shuffle(givenVertices);

            final Graph<Integer> graph = new Graph<>(givenVertices);

            graph.addEdge(2, 1);
            graph.addEdge(3, 2);
            graph.addEdge(4, 3);
            graph.addEdge(5, 4);

            // This graph has no cycles
            assertFalse(graph.hasCycles());

            final List<Integer> sort = graph.topologySort();
            assertArrayEquals(sort.toArray(), asList(5, 4, 3, 2, 1).toArray());

            final List<Integer> vertices = graph.getVertices();
            assertArrayEquals(vertices.toArray(), givenVertices.toArray());
        }
    }

    @Test(expected = GraphHasCyclesException.class)
    public void testTopologyCross() throws GraphHasCyclesException
    {
        final Graph<Integer> graph = new Graph<>(1, 2, 3, 4, 5);

        // 1 and 2 cross-reference each other
        graph.addEdge(2, 1);
        graph.addEdge(1, 2);

        // Should throw a GraphHasCyclesException exception
        graph.topologySort();
    }
}
