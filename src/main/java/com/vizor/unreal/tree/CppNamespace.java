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

import com.vizor.unreal.writer.CppPrinter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

public final class CppNamespace extends CppRecord
{
    private final String name;
    private final List<CppRecord> residents = new ArrayList<>();

    public CppNamespace(final String name)
    {
        this.name = name;
    }

    public void add(final CppRecord... residents)
    {
        this.residents.addAll(asList(residents));
    }

    public void add(final Collection<? extends CppRecord> residents)
    {
        this.residents.addAll(residents);
    }

    public List<CppRecord> getResidents()
    {
        return unmodifiableList(residents);
    }

    public String getName()
    {
        return name;
    }

    public boolean hasName()
    {
        return name != null;
    }

    @Override
    public CppPrinter accept(CppPrinter printer)
    {
        printer.visit(this);
        return printer;
    }

    @Override
    public void setResidence(Residence residence)
    {
        if (residence == Residence.Split)
            throw new RuntimeException("'" + residence + "' is not allowed for " + getClass().getSimpleName());

        super.setResidence(residence);

        residents.forEach(r -> r.setResidence(residence));
    }

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;

        if (obj == null)
            return false;

        if (obj instanceof CppNamespace)
            return name.equals(((CppNamespace) obj).name);

        return false;
    }
}
