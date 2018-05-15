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

public class CppPragma extends CppRecord
{
    private final String body;
    private final String comment;

    public CppPragma(Residence residence, final String body)
    {
        this(residence, body, "");
    }

    public CppPragma(Residence residence, final String body, final String comment)
    {
        super(residence);

        this.body = body;
        this.comment = comment;

//        setResidence(Residence.Cpp);
    }

    public String getBody()
    {
        return body;
    }

    public String getComment()
    {
        return comment;
    }

    @Override
    public CppPrinter accept(CppPrinter printer)
    {
        printer.visit(this);
        return printer;
    }

    @Override
    public String toString ()
    {
        return body + " //" + comment;
    }
}
