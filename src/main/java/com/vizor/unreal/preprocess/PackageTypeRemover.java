package com.vizor.unreal.preprocess;

import com.google.common.collect.ImmutableList;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import com.squareup.wire.schema.internal.parser.RpcElement;
import com.squareup.wire.schema.internal.parser.ServiceElement;
import com.vizor.unreal.convert.Converter;

import java.util.ArrayList;

public class PackageTypeRemover implements Preprocessor
{
    @Override
    public ProtoFileElement process(ProtoFileElement e)
    {
        final ArrayList<String> imports = new ArrayList<>();
        imports.addAll(e.imports());
        imports.addAll(e.publicImports());

        final ProtoFileElement.Builder protoFileElementBuilder = ProtoFileElement.builder(e.location());
        final ArrayList<ServiceElement> newServiceElementList = new ArrayList<>();

        for(final ServiceElement se : e.services())
        {
            final ServiceElement.Builder serviceElementBuilder = ServiceElement.builder(se.location());
            serviceElementBuilder
                    .documentation(se.documentation())
                    .name(se.name())
                    .options(se.options());

            final ArrayList<RpcElement> newRpcList = new ArrayList<>();
            for (final RpcElement rpc : se.rpcs())
            {
                final RpcElement.Builder rpcElementBuilder = RpcElement.builder(rpc.location());
                rpcElementBuilder
                        .requestStreaming(rpc.requestStreaming())
                        .documentation(rpc.documentation())
                        .options(rpc.options())
                        .name(rpc.name())
                        .responseStreaming(rpc.responseStreaming())
                        .requestType(rpc.requestType())
                        .responseType(rpc.responseType());

                for (final String s : imports)
                {
                    final String imp = s.replace(".proto", "");

                    final String packagePrefix = imp + ".";

                     if (rpc.requestType().contains(imp))
                         rpcElementBuilder.requestType(rpc.requestType().replace(packagePrefix, ""));
                     if (rpc.responseType().contains(imp))
                         rpcElementBuilder.responseType(rpc.responseType().replace(packagePrefix, ""));
                }
                newRpcList.add(rpcElementBuilder.build());
            }

            serviceElementBuilder.rpcs(ImmutableList.copyOf(newRpcList));
            newServiceElementList.add(serviceElementBuilder.build());
        }

        return protoFileElementBuilder
                .syntax(e.syntax())
                .services(ImmutableList.copyOf(newServiceElementList))
                .types(e.types())
                .imports(e.imports())
                .publicImports(e.publicImports())
                .packageName(e.packageName())
                .extendDeclarations(e.extendDeclarations())
                .build();
    }
}
