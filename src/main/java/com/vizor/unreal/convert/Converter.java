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
package com.vizor.unreal.convert;

import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import com.vizor.unreal.config.Config;
import com.vizor.unreal.config.DestinationConfig;
import com.vizor.unreal.preprocess.NestedTypesRemover;
import com.vizor.unreal.preprocess.Preprocessor;
import com.vizor.unreal.util.Tuple;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.squareup.wire.schema.Location.get;
import static com.squareup.wire.schema.internal.parser.ProtoParser.parse;
import static java.lang.String.join;
import static java.lang.System.lineSeparator;
import static java.nio.file.Files.readAllLines;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.logging.log4j.LogManager.getLogger;

public class Converter
{
    private static final Logger log = getLogger(Converter.class);

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    private static final List<Class<? extends Preprocessor>> preprocessorClasses = asList(
        NestedTypesRemover.class

        // Add new ones if you want to...
    );

    private final String moduleName;

    public Converter(final String moduleName)
    {
        this.moduleName = moduleName;
    }

    public void convert(final Path srcPath, final List<Tuple<Path, DestinationConfig>> paths)
    {
        List<ProtoProcessorArgs> args = new ArrayList<>();

        Stream<Tuple<Path, DestinationConfig>> pathsStream = paths.stream();

        pathsStream.forEach(pathPair -> {
            final Path pathToProto = pathPair.first();
            final DestinationConfig pathToConverted = pathPair.second();
            
            String fileContent = null;
            
            try 
            {
                fileContent = join(lineSeparator(), readAllLines(pathPair.first()));
            }
            catch (IOException ex)
            {
                throw new RuntimeException(ex);
            }

            final List<ProtoProcessorArgs> protoArgs = preProcess(parse(get(pathToProto.toString()), fileContent))
                .stream()
                .map(
                    protoFile -> 
                    {
                        final Path relativePath = srcPath.relativize(pathToProto); 
                        return new ProtoProcessorArgs(protoFile, relativePath, pathToConverted, moduleName);
                    })
                .collect(Collectors.toList());

            args.addAll(protoArgs);
        });

        Stream<ProtoProcessorArgs> argsStream = args.stream();

        if (!Config.get().isNoFork())
        {
            argsStream = argsStream.parallel();
        }

        argsStream.forEach(arg -> {
            log.info("Converting {}", arg.pathToProto);
            new ProtoProcessor(arg, args).run();
        });
    }

    private List<ProtoFileElement> preProcess(ProtoFileElement element)
    {
        final List<ProtoFileElement> elements = new ArrayList<>();
        elements.add(element);

        try
        {
            for (final Class<? extends Preprocessor> c : preprocessorClasses)
            {
                final Preprocessor p = c.cast(c.newInstance());

                // note that each processor outputs a set of ProtoFileElements's
                // which should be processed independent of each other.
                final List<ProtoFileElement> processed = elements.stream()
                    .peek(e -> log.debug("Processing '{}' with '{}'", e.packageName(), p.getClass().getSimpleName()))
                    .map(p::process)
                    .collect(toList());

                elements.clear();
                elements.addAll(processed);
            }
        }
        catch (Throwable e)
        {
            throw new RuntimeException(e);
        }

        return elements;
    }
}
