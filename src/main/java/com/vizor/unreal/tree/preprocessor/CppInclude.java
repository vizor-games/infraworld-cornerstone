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
package com.vizor.unreal.tree.preprocessor;

import com.vizor.unreal.tree.CppRecord;
import com.vizor.unreal.writer.CppPrinter;

import java.text.MessageFormat;
import java.util.Objects;

public final class CppInclude extends CppRecord
{
    private final boolean isGlobal;
    private final String include;

    public CppInclude(Residence residence, String include)
    {
        this(residence, include, false);
    }

    public CppInclude(Residence residence, String include, boolean global)
    {
        // should tell the record about residency
        super(residence);

        if (include.isEmpty())
            throw new RuntimeException("'include' can not be empty");

        this.include = include;
        this.isGlobal = global;

        // annotations should be disabled for includes
        enableAnnotations(false);
    }

    public final String getFormatter()
    {
        return isGlobal ? "<{0}>" : "\"{0}\"";
    }

    public final String getInclude()
    {
        return include;
    }

    @Override
    public final CppPrinter accept(CppPrinter printer)
    {
        printer.visit(this);
        return printer;
    }

    @Override
    public void enableAnnotations(boolean enable)
    {
        if (enable)
            throw new RuntimeException(CppInclude.class.getName() + " can not enable annotations");

        super.enableAnnotations(false);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(isGlobal, include);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
            return true;

        if (obj instanceof CppInclude)
        {
            final CppInclude other = (CppInclude) obj;
            return (isGlobal == other.isGlobal) && Objects.equals(include, other.include);
        }

        return false;
    }

    @Override
    public String toString()
    {
        return MessageFormat.format(getFormatter(), include);
    }
}
