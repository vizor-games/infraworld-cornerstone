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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static com.vizor.unreal.util.Misc.TAB;
import static java.lang.System.lineSeparator;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.joining;

public class CppMacroIf extends CppRecord implements PreprocessorStatement
{
    public static class Branch
    {
        private final String condition;
        private final List<CppRecord> records;

        private Branch(final String condition, final List<CppRecord> records)
        {
            this.condition = condition;
            this.records = records;
        }

        public final String getCondition()
        {
            return condition;
        }

        public final List<CppRecord> getRecords()
        {
            return records;
        }

        public boolean isEmpty()
        {
            return false;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(condition, records);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == this)
                return true;

            if (obj instanceof Branch)
            {
                final Branch other = (Branch) obj;
                return Objects.equals(condition, other.condition) && Objects.equals(records, other.records);
            }

            return false;
        }
    }

    private static Branch DUMMY_BRANCH = new Branch("NO_CONDITION", emptyList()) {
        @Override
        public boolean isEmpty()
        {
            return true;
        }
    };

    private final Branch ifBranch;
    private List<Branch> elseIfBranches = new ArrayList<>(0);
    private Branch elseBranch = DUMMY_BRANCH;

    public CppMacroIf(final Residence residence, final String condition, final CppRecord... records)
    {
        super(residence);

        ifBranch = new Branch(condition, asList(records));
    }

    public final CppMacroIf orElse(final CppRecord... records)
    {
        elseBranch = new Branch("NO_CONDITION", asList(records));

        return this;
    }

    public final CppMacroIf elseIf(final String condition, final CppRecord... records)
    {
        elseIfBranches.add(new Branch(condition, asList(records)));

        return this;
    }

    public final Branch getIfBranch()
    {
        return ifBranch;
    }

    public final List<Branch> getElseIfBranches()
    {
        return unmodifiableList(elseIfBranches);
    }

    public final Branch getElseBranch()
    {
        return elseBranch;
    }

    @Override
    public CppPrinter accept(CppPrinter printer)
    {
        printer.visit(this);
        return printer;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(ifBranch, elseIfBranches, elseBranch);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
            return true;

        if (obj instanceof CppMacroIf)
        {
            final CppMacroIf other = (CppMacroIf) obj;

            return Objects.equals(ifBranch, other.ifBranch)
                && Objects.equals(elseIfBranches, other.elseIfBranches)
                && Objects.equals(elseBranch, other.elseBranch);
        }

        return false;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();

        final Function<List<?>, String> squasher = rs -> rs.stream().map(r -> TAB + r).collect(joining(lineSeparator()));

        sb.append("#if ").append(ifBranch.condition).append(lineSeparator());
        sb.append(squasher.apply(ifBranch.records));

        elseIfBranches.forEach(elseIfBranch -> {
            sb.append("#elif ").append(elseIfBranch.condition).append(lineSeparator());
            sb.append(squasher.apply(elseIfBranch.records));
        });

        sb.append("#else").append(lineSeparator());
        sb.append(squasher.apply(elseBranch.records));

        return sb.toString();
    }
}
