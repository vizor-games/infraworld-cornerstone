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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

public abstract class CppRecord implements CtLeaf
{
    public enum Residence
    {
        /**
         * Declared in the .h file. Defined in the .h file.
         */
        Header,

        /**
         * Declared in the .cpp file. Defined in the .cpp file.
         */
        Cpp,

        /**
         * Declared in the .h file. Defined in the .cpp file.
         */
        Split
    }

    private final Map<CppAnnotation, String> annotations = new HashMap<>();

    private boolean annotationsEnabled = true;
    private Residence residence;

    public CppRecord()
    {
        this(Residence.Header);
    }

    public CppRecord(Residence residence)
    {
        this.residence = residence;
    }

    public final Map<CppAnnotation, String> getAnnotations()
    {
        // If annotations are ignored - always return an empty map.
        return annotationsEnabled ? unmodifiableMap(annotations) : emptyMap();
    }

    public final void addAnnotation(final Collection<CppAnnotation> newAnnotations)
    {
        ensureAnnotationsEnabled();
        newAnnotations.forEach(a -> addAnnotation(a, null));
    }

    public final void addAnnotation(final CppAnnotation... annotations)
    {
        ensureAnnotationsEnabled();
        stream(annotations).forEach(a -> addAnnotation(a, null));
    }

    public final void addAnnotation(final CppAnnotation annotation, final String value)
    {
        ensureAnnotationsEnabled();
        annotations.put(annotation, value);
    }

    public final boolean isAnnotationsEnabled()
    {
        return annotationsEnabled;
    }

    public final Residence getResidence()
    {
        return residence;
    }

    public void enableAnnotations(boolean enable)
    {
        this.annotationsEnabled = enable;
    }

    public void setResidence(Residence residence)
    {
        this.residence = residence;
    }

    private void ensureAnnotationsEnabled()
    {
        if (!annotationsEnabled)
            throw new RuntimeException("Can not add an annotation, you should call enableAnnotations() to do this");
    }
}
