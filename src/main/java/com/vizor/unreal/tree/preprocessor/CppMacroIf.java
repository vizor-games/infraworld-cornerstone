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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

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
    }

    private static Branch DUMMY_BRANCH = new Branch("NO_CONDITION", emptyList()) {
        @Override
        public boolean isEmpty()
        {
            return true;
        }
    };

    private final Branch ifBranch;
    private Branch elseBranch = DUMMY_BRANCH;

    private List<Branch> elifBranches = new ArrayList<>(0);

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
        elifBranches.add(new Branch(condition, asList(records)));

        return this;
    }


    public final Branch getIfBranch()
    {
        return ifBranch;
    }

    public final Branch getElseBranch()
    {
        return elseBranch;
    }

    public final List<Branch> getElifBranches()
    {
        return unmodifiableList(elifBranches);
    }

    @Override
    public CppPrinter accept(CppPrinter printer)
    {
        printer.visit(this);
        return printer;
    }
}
