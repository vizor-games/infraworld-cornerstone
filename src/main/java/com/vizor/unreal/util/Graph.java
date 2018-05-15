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
package com.vizor.unreal.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

import static com.vizor.unreal.util.Misc.TAB;
import static com.vizor.unreal.util.Misc.reorder;
import static java.lang.String.valueOf;
import static java.lang.System.lineSeparator;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

public final class Graph<T>
{
    public static class GraphHasCyclesException extends Exception
    {
        private final List<?> crossReferencedObjects;

        GraphHasCyclesException(final List<?> crossReferencedObjects)
        {
            this.crossReferencedObjects = crossReferencedObjects;
        }

        @Override
        public String toString()
        {
            return "Following nodes cross-references each other: " +
                    crossReferencedObjects.stream().distinct().map(Object::toString).collect(joining(", ", "[", "]"));
        }
    }

    private final Map<T, Integer> vertices = new HashMap<>();
    private final List<List<Integer>> adjaency = new ArrayList<>();

    @SafeVarargs
    public Graph(T... items)
    {
        for (T item : items)
            this.add(item);
    }

    public Graph(final List<T> items)
    {
        items.forEach(this::add);
    }

    public final void addEdge(final T from, final T to)
    {
        final Integer a = vertices.getOrDefault(from, null);
        if (isNull(a))
            throw new RuntimeException("This graph does not contain from '" + Objects.toString(from) + "'");

        final Integer b = vertices.getOrDefault(to, null);
        if (isNull(b))
            throw new RuntimeException("This graph does not contain to '" + Objects.toString(to) + "'");

        adjaency.get(a).add(b);
    }

    /**
     * Returns a list of vertices in this graph.
     * This is stable. The vertices are ordered the same way as in array's creation.
     * @return An ordered list of vertices.
     */
    public final List<T> getVertices()
    {
        final int numVertices = vertices.size();
        final List<T> list = new ArrayList<>(numVertices);

        // Fill with nulls to ensure safety on set()
        for (int i = 0; i < numVertices; i++)
            list.add(null);

        // Add in the right order, as they were added in the constructor
        vertices.forEach((item, index) -> list.set(index, item));
        return list;
    }

    public final void removeUnreachableNodes()
    {
        removeUnreachableNodes(emptyList());
    }

    /**
     * Removes all vertices, that has no INCOMING edges.
     *
     * @param rootSet Set of root structures.
     */
    @SuppressWarnings("WeakerAccess")
    public void removeUnreachableNodes(final Collection<? extends T> rootSet)
    {
        // From root set remove everything that isn't contained in 'vertices'
        // This is done for error-proof.
        rootSet.removeIf(t -> !vertices.containsKey(t));

        final int[] numLinks = getWeights();

        // Increment every item, root set contains
        // (make them accessible)
        rootSet.forEach(t -> numLinks[vertices.get(t)]++);

        // Remove from vertices
        vertices.entrySet().removeIf(e -> numLinks[e.getValue()] == 0);

        // Remove from adjaencies
        int counter = 0;
        for (Iterator<?> it = adjaency.iterator(); it.hasNext(); it.next())
        {
            if (!vertices.containsValue(counter))
                it.remove();
        }
    }

    public List<T> topologySort() throws GraphHasCyclesException
    {
        final List<T> vertices = getVertices();

        reorder(vertices, getOrder());
        return vertices;
    }

    /**
     * Tries to order the graph and tells is it possible.
     *
     * @return True if the graph has cycles and can't be ordered yet. False otherwise.
     */
    public final boolean hasCycles()
    {
        try
        {
            getOrder();
            return false;
        }
        catch (GraphHasCyclesException ex)
        {
            return true;
        }
    }

    int[] getOrder() throws GraphHasCyclesException
    {
        final int[] weights = getWeights();

        final Queue<Integer> q = new LinkedList<>();
        for (int i = 0; i < weights.length; i++)
        {
            if (weights[i] == 0)
                q.add(i);
        }

        int cnt = 0;
        final List<Integer> topOrder = new ArrayList<>();

        while (!q.isEmpty())
        {
            final Integer u = q.poll();
            topOrder.add(u);

            for (final Integer node : adjaency.get(requireNonNull(u)))
            {
                if (--weights[node] == 0)
                    q.add(node);
            }
            cnt++;
        }

        if (cnt != weights.length)
        {
            // Copy cross-references in outCycles
            final List<T> vk = getVertices();
            final List<T> cycles = new ArrayList<>(weights.length);

            for (int i = 0; i < weights.length; i++)
            {
                if (weights[i] != 0)
                    cycles.add(vk.get(i));
            }

            throw new GraphHasCyclesException(cycles);
        }

        return topOrder.stream().mapToInt(Integer::intValue).toArray();
    }

    private void add(T item)
    {
        vertices.put(item, vertices.size());
        adjaency.add(new ArrayList<>(0));
    }

    private int[] getWeights()
    {
        final int[] indegrees = new int[adjaency.size()];

        for (List<Integer> anAdj : adjaency)
        {
            for (int node : anAdj)
                indegrees[node]++;
        }

        return indegrees;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();

        final List<T> vertices = getVertices();
        sb.append("Graph           :").append(hashCode()).append(lineSeparator());
        sb.append("Empty           :").append(vertices.isEmpty()).append(lineSeparator());
        sb.append("Num vertices    :").append(vertices.size()).append(lineSeparator());
        sb.append("Vertices (list) :").append(lineSeparator());

        for (int i = 0; i < vertices.size(); i++)
        {
            final T vertex = vertices.get(i);

            sb.append(i).append(") ").append(valueOf(vertex)).append(" (kind of ")
                    .append(vertex.getClass().getCanonicalName()).append(')').append(lineSeparator());

            // print references
            adjaency.get(i).forEach(refIndex -> {
                final T referent = vertices.get(refIndex);

                sb.append(TAB).append(" * referenced from ").append('[').append(refIndex).append("] ")
                        .append(valueOf(referent)).append(lineSeparator());
            });

        }

        try
        {
            final int[] order = getOrder();
            sb.append("Cyclic           :").append(false).append(lineSeparator());
            sb.append("Sorted (indices) :").append(Arrays.toString(order)).append(lineSeparator());
            sb.append("Sorted (list)    :").append(lineSeparator());

            final List<T> orderedVertices = new ArrayList<>(vertices);
            reorder(orderedVertices, order);

            for (int i = 0; i < orderedVertices.size(); i++)
            {
                final T vertex = orderedVertices.get(i);
                final int previousIndex = this.vertices.get(vertex);

                sb.append(i).append(") ").append("(was ").append(previousIndex).append(") ")
                        .append(valueOf(vertex)).append(lineSeparator());
            }
        }
        catch (GraphHasCyclesException ex)
        {
            sb.append("Cyclic           :").append(true).append(lineSeparator());
        }


        return sb.toString();
    }
}
