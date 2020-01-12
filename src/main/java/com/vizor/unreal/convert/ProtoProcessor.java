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

import com.squareup.wire.schema.internal.parser.EnumConstantElement;
import com.squareup.wire.schema.internal.parser.EnumElement;
import com.squareup.wire.schema.internal.parser.FieldElement;
import com.squareup.wire.schema.internal.parser.MessageElement;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import com.squareup.wire.schema.internal.parser.ServiceElement;
import com.squareup.wire.schema.internal.parser.TypeElement;
import com.vizor.unreal.config.Config;
import com.vizor.unreal.provider.ProtoTypesProvider;
import com.vizor.unreal.provider.TypesProvider;
import com.vizor.unreal.provider.UnrealTypesProvider;
import com.vizor.unreal.tree.CppAnnotation;
import com.vizor.unreal.tree.CppClass;
import com.vizor.unreal.tree.CppDelegate;
import com.vizor.unreal.tree.CppEnum;
import com.vizor.unreal.tree.CppField;
import com.vizor.unreal.tree.CppNamespace;
import com.vizor.unreal.tree.CppRecord;
import com.vizor.unreal.tree.CppStruct;
import com.vizor.unreal.tree.CppType;
import com.vizor.unreal.tree.preprocessor.CppInclude;
import com.vizor.unreal.tree.preprocessor.CppMacroIf;
import com.vizor.unreal.tree.preprocessor.CppPragma;
import com.vizor.unreal.util.MessageOrderResolver;
import com.vizor.unreal.util.Tuple;
import com.vizor.unreal.writer.CppPrinter;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.squareup.wire.schema.Field.Label.REPEATED;
import static com.vizor.unreal.tree.CppAnnotation.BlueprintReadWrite;
import static com.vizor.unreal.tree.CppAnnotation.BlueprintType;
import static com.vizor.unreal.tree.CppAnnotation.DisplayName;
import static com.vizor.unreal.tree.CppAnnotation.Transient;
import static com.vizor.unreal.tree.CppRecord.Residence.Cpp;
import static com.vizor.unreal.tree.CppRecord.Residence.Header;
import static com.vizor.unreal.tree.CppType.Kind.Enum;
import static com.vizor.unreal.tree.CppType.Kind.Struct;
import static com.vizor.unreal.tree.CppType.plain;
import static com.vizor.unreal.util.Misc.reorder;
import static com.vizor.unreal.util.Misc.snakeCaseToCamelCase;
import static com.vizor.unreal.util.Misc.stringIsNullOrEmpty;
import static com.vizor.unreal.util.Tuple.of;
import static java.io.File.separator;
import static java.lang.String.join;
import static java.nio.file.Paths.get;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.apache.logging.log4j.LogManager.getLogger;

class ProtoProcessorArgs
{
	ProtoProcessorArgs(final ProtoFileElement parse, final Path pathToProto,
	final Path pathToConverted, final String moduleName)
	{
		this.parse = requireNonNull(parse);
		this.pathToProto = requireNonNull(pathToProto);
		this.pathToConverted = requireNonNull(pathToConverted);
		this.moduleName = requireNonNull(moduleName);

		this.wrapperName = removeExtension(pathToProto.toFile().getName());

		this.className = snakeCaseToCamelCase(wrapperName);

//        if (parse.packageName() == null)
//            throw new RuntimeException("package filed in proto file is required for cornerstone");

		this.packageNamespace = new CppNamespace(parse.packageName());
	}

	final ProtoFileElement parse;
	final Path pathToProto;
	final Path pathToConverted;
	final String moduleName;

	final String wrapperName;

	final String className;
	final CppNamespace packageNamespace;
}

class ProtoProcessor implements Runnable
{
    private static final Logger log = getLogger(ProtoProcessor.class);

	private final ProtoProcessorArgs args;

    private final TypesProvider ueProvider = new UnrealTypesProvider();
    private final TypesProvider protoProvider = new ProtoTypesProvider();

	private final List<ProtoProcessorArgs> otherProcessorArgs;

	ProtoProcessor(ProtoProcessorArgs args, List<ProtoProcessorArgs> otherProcessorArgs) {
		this.args = args;
		this.otherProcessorArgs = otherProcessorArgs;
				
	}
	
	private Stream<ProtoProcessorArgs> GatherImportedProtos(final ProtoProcessorArgs proto, final List<ProtoProcessorArgs> otherProtos)
	{
		return otherProtos.stream()
			.filter(otherProto -> proto.parse.imports().contains(otherProto.pathToProto.toString()));
	}

	private Stream<ProtoProcessorArgs> GatherImportedProtosDeep(final ProtoProcessorArgs proto, final List<ProtoProcessorArgs> otherProtos)
	{
		final Stream<ProtoProcessorArgs> importedProtos = GatherImportedProtos(proto, otherProtos);

		return Stream.concat(Stream.of(proto), importedProtos.flatMap(importedProto->GatherImportedProtosDeep(importedProto, otherProtos)));
	}

	private void GatherTypes(final ProtoProcessorArgs proto, final List<ProtoProcessorArgs> otherProtos, TypesProvider ueProvider, TypesProvider protoProvider)
	{
		final Stream<ProtoProcessorArgs> importedProtos = GatherImportedProtosDeep(proto, otherProtos);

		importedProtos.forEach(
			importedProto -> importedProto.parse.types().forEach(
				typeElement ->
				{
					ueProvider.register(typeElement.name(), ueNamedType(importedProto.className, typeElement));
					protoProvider.register(typeElement.name(), cppNamedType(typeElement));
				}
			)
		);

		// return importedProtos.collect(Collectors.toList());
	}

    @Override
    public void run()
    {
        final List<ServiceElement> services = args.parse.services();

		GatherTypes(args, otherProcessorArgs, ueProvider, protoProvider);

		

        final List<Tuple<CppStruct, CppStruct>> castAssociations = new ArrayList<>();
        final List<CppStruct> unrealStructures = new ArrayList<>();

        final List<CppEnum> ueEnums = new ArrayList<>();

        // At this moment, we have all types registered in both type providers
        for (final TypeElement s : args.parse.types())
        {
            if (s instanceof MessageElement)
            {
                final MessageElement messageElement = (MessageElement) s;

                final CppStruct ueStruct = extractStruct(ueProvider, messageElement);
                final CppStruct protoStruct = extractStruct(protoProvider, messageElement);

                log.debug("Found type cast {} -> {}", ueStruct.getType(), protoStruct.getType());

                castAssociations.add(of(protoStruct, ueStruct));
                unrealStructures.add(ueStruct);
            }
            else if (s instanceof EnumElement)
            {
                ueEnums.add(extractEnum(ueProvider, (EnumElement) s));
            }
            else
            {
                throw new RuntimeException("Unknown type: '" + s.getClass().getName() + "'");
            }
        }

        // Topologically sort structures
        final MessageOrderResolver resolver = new MessageOrderResolver();
        final int[] indices = resolver.sortByInclusion(unrealStructures);

        // Then reorder data types
        reorder(unrealStructures, indices);
        reorder(castAssociations, indices);

        final CppNamespace casts = new CastGenerator().genCasts(castAssociations);

        log.debug("Found structures (sorted): {}", () ->
            unrealStructures.stream().map(s -> s.getType().getName()).collect(joining(", ", "[", "]")
        ));

        // Generate RPC workers
        final ClientWorkerGenerator clientWorkerGenerator = new ClientWorkerGenerator(services, ueProvider, args.parse);
        final List<CppClass> workers = clientWorkerGenerator.genClientClass();

        // Generate RPC clients
        final List<CppClass> clients = new ArrayList<>(services.size());
        final List<CppDelegate> dispatchers = new ArrayList<>(services.size());

        for (int i = 0; i < services.size(); i++)
        {
            final ServiceElement service = services.get(i);
            final CppClass worker = workers.get(i);

            final ClientGenerator cg = new ClientGenerator(service, ueProvider, worker.getType());

            clients.add(cg.genClientClass());
            dispatchers.addAll(cg.getDelegates());
        }

        final String pathToProtoStr = removeExtension(args.pathToProto.toString());
        final String outDirPath = join(separator, args.pathToConverted.toString(), pathToProtoStr);

        // Should create an output directories if does not exit.
        @SuppressWarnings("unused")
        final boolean ignore = get(outDirPath).toFile().mkdirs();

        final List<CppInclude> headerIncludes = new ArrayList<>(asList(
            // header
            new CppInclude(Header, "CoreMinimal.h"),
            new CppInclude(Header, "Conduit.h"),
            new CppInclude(Header, "GenUtils.h"),
            new CppInclude(Header, "RpcClient.h")
		));

		headerIncludes.addAll(GatherImportedProtos(args, otherProcessorArgs).map(
			importedProto -> {
				
				final String pathToImportedProtoStr = removeExtension(importedProto.pathToProto.toString());
				final String importedHeaderPath = join(separator, importedProto.pathToConverted.toString(), pathToImportedProtoStr, importedProto.className + ".h");
				return new CppInclude(Header, importedHeaderPath);
			}
		).collect(Collectors.toList()));

		if (clients.size() > 0 || unrealStructures.size() > 0 || ueEnums.size() > 0)
		{
			headerIncludes.add(
				new CppInclude(Header, args.className + ".generated.h")
			);
		}
		
        final Config config = Config.get();

        // TODO: Fix paths
        final String generatedIncludeName = join("/", config.getWrappersPath(),
                removeExtension(pathToProtoStr), args.wrapperName);

        // code. mutable to allow
        final List<CppRecord> cppIncludes = new ArrayList<>(asList(
            new CppInclude(Cpp, args.className + ".h"),
            new CppInclude(Cpp, "RpcClientWorker.h"),
            new CppInclude(Cpp, "CastUtils.h"),

            new CppInclude(Cpp, "GrpcIncludesBegin.h"),

            new CppInclude(Cpp, "grpc/support/log.h", true),
            new CppInclude(Cpp, "grpc++/channel.h", true),

            new CppInclude(Cpp, generatedIncludeName + ".pb.hpp", false),
            new CppInclude(Cpp, generatedIncludeName + ".grpc.pb.hpp", false),
            new CppInclude(Cpp, "ChannelProvider.h", false),

            new CppInclude(Cpp, "GrpcIncludesEnd.h")
        ));

        if (!stringIsNullOrEmpty(config.getPrecompiledHeader()))
            cppIncludes.add(0, new CppInclude(Cpp, config.getPrecompiledHeader(), false));

        final Path outFilePath = get(outDirPath, args.className);

        try (final CppPrinter p = new CppPrinter(outFilePath, args.moduleName.toUpperCase()))
        {
            headerIncludes.forEach(i -> i.accept(p));
            p.newLine();

            cppIncludes.forEach(i -> i.accept(p));
            p.newLine();

            // Write enums and structs
            p.writeInlineComment("Enums:");
            ueEnums.forEach(e -> e.accept(p).newLine());

            p.writeInlineComment("Structures:");
            unrealStructures.forEach(s -> s.accept(p).newLine());

            p.writeInlineComment("Forward class definitions (for delegates)");
            clients.forEach(c -> p.write("class ").write(c.getType().toString()).writeLine(";"));
            p.newLine();

            p.writeInlineComment("Dispatcher delegates");
            dispatchers.forEach(d -> d.accept(p).newLine());
            p.newLine();

            // Write casts to the CPP file
            casts.accept(p).newLine();

            // Workers are being written to the *.cpp file, have to write them before
            workers.forEach(c -> c.accept(p).newLine());

            clients.forEach(w -> w.accept(p).newLine());
        }
    }

    private CppStruct extractStruct(final TypesProvider provider, final MessageElement me)
    {
        final CppType type = provider.get(me.name());

        final List<CppAnnotation> fieldAnnotations = new ArrayList<>();

        fieldAnnotations.add(Transient);
        fieldAnnotations.add(BlueprintReadWrite);

        final List<CppField> fields = new ArrayList<>();
        for (final FieldElement fe : me.fields())
        {
            // Get the type
            final CppType ueType = provider.get(fe.type());

            // If the field is repeated - make a TArray<?> of type.
            final CppField field;
            if (fe.label() == REPEATED)
            {
                final CppType ueArrayType = provider.arrayOf(ueType);
                field = new CppField(ueArrayType, provider.fixFieldName(fe.name(), false));
            }
            else
            {
                final String fieldName = provider.fixFieldName(fe.name(), ueType.isA(boolean.class));
                field = new CppField(ueType, fieldName);
            }

            // Add docs if has any
            final String sourceDoc = fe.documentation();
            if (!sourceDoc.isEmpty())
                field.javaDoc.set(sourceDoc);

            field.addAnnotation(fieldAnnotations);
            fields.add(field);
        }

        final CppStruct struct = new CppStruct(type, fields);

        struct.addAnnotation(DisplayName, args.className + " " + me.name());

        if (!me.documentation().isEmpty())
            struct.javaDoc.set(me.documentation());

        struct.addAnnotation(BlueprintType);

        struct.setResidence(Header);
        return struct;
    }

    private CppEnum extractEnum(final TypesProvider provider, final EnumElement ee)
    {
        final CppEnum cppEnum = new CppEnum(provider.get(ee.name()), ee.constants().stream()
                .collect(toMap(m -> provider.fixFieldName(m.name(), false), EnumConstantElement::tag)));

        if (!ee.documentation().isEmpty())
            cppEnum.getJavaDoc().set(ee.documentation());

        cppEnum.addAnnotation(BlueprintType);
        cppEnum.addAnnotation(DisplayName, args.className + " " + ee.name());

        cppEnum.setResidence(Header);
        return cppEnum;
    }

    private CppType ueNamedType(final String serviceName, final TypeElement el)
    {
        if (el instanceof MessageElement)
            return plain("F" + serviceName + "_" + el.name(), Struct);
        else if (el instanceof EnumElement)
            return plain("E" + serviceName + "_" + el.name(), Enum);
        else
            throw new RuntimeException("Unknown type: '" + el.getClass().getName() + "'");
    }

    private CppType cppNamedType(final TypeElement el)
    {
        if (el instanceof MessageElement)
        {
            final CppType mt = plain(el.name(), Struct);

            if (args.packageNamespace.hasName())
                mt.setNamespaces(args.packageNamespace);

            return mt;
        }
        else if (el instanceof EnumElement)
        {
            final CppType et = plain(el.name(), Enum);

            if (args.packageNamespace.hasName())
                et.setNamespaces(args.packageNamespace);

            return et;
        }
        else
        {
            throw new RuntimeException("Unknown type: '" + el.getClass().getName() + "'");
        }
    }
}
