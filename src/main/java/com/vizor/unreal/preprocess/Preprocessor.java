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
package com.vizor.unreal.preprocess;

import com.squareup.wire.schema.internal.parser.ProtoFileElement;

/**
 * Using preprocessor is a short and easy way to define operations, which had to be performed over
 * retrieved instances of {@link com.squareup.wire.schema.internal.parser.ProtoFileElement}
 *
 * NOTE: Since preprocessors are being instantiated with reflection, they needs a public default
 * constructor. Please consider not to execute any non-trivial logic within the constructor.
 */
public interface Preprocessor
{
    /**
     * Processes a {@link com.squareup.wire.schema.internal.parser.ProtoFileElement} a custom way to modify it's
     * internal structure. Should return a modified (or the same) ProtoFileElement.
     *
     * @param e A ProtoFileElement to be processed.
     * @return A list of ProtoFileElement, got after processing.
     */
    ProtoFileElement process(final ProtoFileElement e);
}
