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

import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import com.squareup.wire.schema.internal.parser.RpcElement;
import com.squareup.wire.schema.internal.parser.ServiceElement;
import com.vizor.unreal.provider.TypesProvider;
import com.vizor.unreal.tree.CppArgument;
import com.vizor.unreal.tree.CppClass;
import com.vizor.unreal.tree.CppField;
import com.vizor.unreal.tree.CppFunction;
import com.vizor.unreal.tree.CppNamespace;
import com.vizor.unreal.tree.CppType;

import java.util.ArrayList;
import java.util.List;

import static com.vizor.unreal.convert.ClientGenerator.conduitName;
import static com.vizor.unreal.convert.ClientGenerator.conduitType;
import static com.vizor.unreal.convert.ClientGenerator.contextArg;
import static com.vizor.unreal.convert.ClientGenerator.initFunctionName;
import static com.vizor.unreal.convert.ClientGenerator.reqWithCtx;
import static com.vizor.unreal.convert.ClientGenerator.rspWithSts;
import static com.vizor.unreal.convert.ClientGenerator.supressSuperString;
import static com.vizor.unreal.convert.ClientGenerator.updateFunctionName;
import static com.vizor.unreal.tree.CppRecord.Residence.Cpp;
import static com.vizor.unreal.tree.CppType.Kind.Class;
import static com.vizor.unreal.tree.CppType.Kind.Struct;
import static com.vizor.unreal.tree.CppType.plain;
import static com.vizor.unreal.tree.CppType.wildcardGeneric;
import static java.lang.String.join;
import static java.lang.System.lineSeparator;
import static java.text.MessageFormat.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

class ClientWorkerGenerator
{
    private static final CppType parentType = plain("RpcClientWorker", Class);
    private static final CppType wildcardUniquePtr = wildcardGeneric("unique_ptr", Struct, 1);

    static
    {
        wildcardUniquePtr.setNamespaces(new CppNamespace("std"));
    }

    private final List<ServiceElement> services;
    private final TypesProvider provider;
    private final ProtoFileElement parse;
    private final CppType voidType;
    private final CppType boolType;

    ClientWorkerGenerator(List<ServiceElement> services, TypesProvider provider, ProtoFileElement parse)
    {
        this.services = services;
        this.provider = provider;
        this.voidType = provider.getNative(void.class);
        this.boolType = provider.getNative(boolean.class);
        this.parse = parse;
    }

    List<CppClass> genClientClass()
    {
        return services.stream().map(this::genSingle).collect(toList());
    }

    private CppClass genSingle(final ServiceElement service)
    {
        final CppType classType = plain(service.name() + "RpcClientWorker", Class);

        final List<CppField> cppFields = extractConduits(service);
        final List<CppField> fields = new ArrayList<>(cppFields);

        // 0 - Request Type
        // 1 - Response Type
        // 2 - UE Response Generic Type
        // 3 - UE Response Type
        // 4 - Package Name
        final String rpcMethodBody = join(lineSeparator(), asList(
            "{4}::{0} ClientRequest(casts::Proto_Cast<{4}::{0}>(Request));",
            "",
            "grpc::ClientContext ClientContext;",
            "casts::CastClientContext(Context, ClientContext);",
            "",
            "grpc::CompletionQueue Queue;",
            "grpc::Status Status;",
            "",
            "std::unique_ptr<grpc::ClientAsyncResponseReader<{4}::{1}>> Rpc(Stub->Async{5}(&ClientContext, ClientRequest, &Queue));",
            "",
            "{4}::{1} Response;",
            "Rpc->Finish(&Response, &Status, (void*)1);",
            "",
            "void* got_tag;",
            "bool ok = false;",
            "",
            "GPR_ASSERT(Queue.Next(&got_tag, &ok));",
            "GPR_ASSERT(got_tag == (void*)1);",
            "GPR_ASSERT(ok);",
            "",
            "FGrpcStatus GrpcStatus;",
            "",
            "casts::CastStatus(Status, GrpcStatus);",
            "{2} Result(casts::Proto_Cast<{3}>(Response), GrpcStatus);",
            "",
            "return Result;"
        ));

        final List<CppFunction> methods = extractFunctions(service);

        for (int i = 0; i < methods.size(); i++)
        {
            final RpcElement rpc = service.rpcs().get(i);
            final CppFunction function = methods.get(i);

            final CppType response = provider.get(rpc.responseType());
            final CppType responseWithStatus = rspWithSts.makeGeneric(response);

            function.setBody(format(rpcMethodBody, rpc.requestType(), rpc.responseType(), responseWithStatus.toString(),
                    response, parse.packageName(), function.getName()));
        }

        methods.add(createStubInitializer(service, fields));
        methods.add(createUpdate(service, fields));
        fields.add(createStub(service));

        final CppClass clientClass = new CppClass(classType, parentType, fields, methods);

        clientClass.setResidence(Cpp);
        clientClass.enableAnnotations(false);

        return clientClass;
    }

    private CppFunction createStubInitializer(final ServiceElement service, final List<CppField> fields)
    {
        final CppFunction initStub = new CppFunction(initFunctionName, boolType);

        final StringBuilder sb = new StringBuilder(supressSuperString(initFunctionName));

        sb.append("std::shared_ptr<grpc::Channel> Channel = channel::CreateChannel(this);")
                .append(lineSeparator());

        sb.append("if (!Channel.get())").append(lineSeparator());
        sb.append("    return false;").append(lineSeparator()).append(lineSeparator());

        final String initStubPattern = "Stub = {0}::{1}::NewStub(Channel);";
        final String acquireProducerPattern = "{0}->AcquireResponsesProducer();";

        sb.append(format(initStubPattern, parse.packageName(), service.name()))
                .append(lineSeparator()).append(lineSeparator());

        // Acquire all required conduits
        fields.forEach(a -> sb.append(format(acquireProducerPattern, a.getName())).append(lineSeparator()));

        sb.append(lineSeparator()).append("return true;");

        initStub.setBody(sb.toString());
        initStub.isOverride = true;
        initStub.enableAnnotations(false);

        return initStub;
    }

    private CppFunction createUpdate(final ServiceElement service, final List<CppField> fields)
    {
        final CppFunction update = new CppFunction(updateFunctionName, voidType);
        update.enableAnnotations(false);
        update.isOverride = true;

        // @see https://stackoverflow.com/questions/1187093/can-i-escape-braces-in-a-java-messageformat
        final String dequeuePattern = join(lineSeparator(), asList(
            "if (!{0}->IsEmpty())",
            "'{'",
            "    {1} WrappedRequest;",
            "    {0}->Dequeue(WrappedRequest);",
            "",
            "    const {2}& WrappedResponse = ",
            "        {3}(WrappedRequest.Request, WrappedRequest.Context);",
            "    {0}->Enqueue(WrappedResponse);",
            "'}'"
        ));

        final StringBuilder sb = new StringBuilder(supressSuperString(updateFunctionName));

        for (int i = 0; i < fields.size(); i++)
        {
            final RpcElement rpc = service.rpcs().get(i);
            final CppField field = fields.get(i);
            final List<CppType> genericParams = field.getType().getGenericParams();

            sb.append(format(dequeuePattern, field.getName(),
                genericParams.get(0),
                genericParams.get(1),
                rpc.name()
            )).append(lineSeparator()).append(lineSeparator());
        }

        update.setBody(sb.toString());
        return update;
    }

    private CppField createStub(final ServiceElement service)
    {
        final CppType plain = plain(parse.packageName() + "::" + service.name() + "::Stub", Struct);
        final CppType stubPtr = wildcardUniquePtr.makeGeneric(plain);
        final CppField stub = new CppField(stubPtr, "Stub");

        stub.enableAnnotations(false);
        return stub;
    }

    private List<CppField> extractConduits(final ServiceElement service)
    {
        return service.rpcs().stream()
            .map(rpc -> {
                // Extract conduits (bidirectional queues)
                final CppType compiledGenericConduit = conduitType.makeGeneric(
                    reqWithCtx.makeGeneric(provider.get(rpc.requestType())),
                    rspWithSts.makeGeneric(provider.get(rpc.responseType()))
                );

                final CppField conduit = new CppField(compiledGenericConduit.makePtr(), rpc.name() + conduitName);
                conduit.enableAnnotations(false);

                return conduit;
            })
            .collect(toList());
    }

    private List<CppFunction> extractFunctions(final ServiceElement service)
    {
        return service.rpcs().stream()
            .map(rpc -> {
                final CppType request = provider.get(rpc.requestType());
                final CppType response = provider.get(rpc.responseType());

                final CppArgument requestArg = new CppArgument(request.makeRef().makeConstant(), "Request");
                final CppType responseType = rspWithSts.makeGeneric(response);

                final CppFunction method = new CppFunction(rpc.name(), responseType, asList(requestArg, contextArg));

                if (!rpc.documentation().isEmpty())
                    method.getJavaDoc().set(rpc.documentation());

                method.enableAnnotations(false);
                return method;
            })
            .collect(toList());
    }

}
