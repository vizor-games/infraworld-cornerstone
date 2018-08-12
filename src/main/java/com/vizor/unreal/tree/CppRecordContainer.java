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

import java.util.List;

import static java.util.Arrays.asList;

public class CppRecordContainer extends CppRecord
{
    private final List<CppRecord> elements;

    public CppRecordContainer (final Residence residence, final CppRecord... records)
    {
        elements = asList(records);

        setResidence(residence);
    }

    @Override
    public void setResidence(final Residence residence)
    {
        super.setResidence(residence);

        elements.forEach(e -> e.setResidence(residence));
    }

    @Override
    public CppPrinter accept(final CppPrinter printer)
    {
        elements.forEach(e -> e.accept(printer));
        return printer;
    }
}
