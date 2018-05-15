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

import com.squareup.wire.schema.internal.parser.RpcElement;
import com.squareup.wire.schema.internal.parser.ServiceElement;
import com.vizor.unreal.config.Config;
import com.vizor.unreal.provider.TypesProvider;
import com.vizor.unreal.tree.CppArgument;
import com.vizor.unreal.tree.CppClass;
import com.vizor.unreal.tree.CppDelegate;
import com.vizor.unreal.tree.CppField;
import com.vizor.unreal.tree.CppFunction;
import com.vizor.unreal.tree.CppType;
import com.vizor.unreal.util.Tuple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.vizor.unreal.tree.CppAnnotation.BlueprintAssignable;
import static com.vizor.unreal.tree.CppAnnotation.BlueprintCallable;
import static com.vizor.unreal.tree.CppAnnotation.Category;
import static com.vizor.unreal.tree.CppType.Kind.Class;
import static com.vizor.unreal.tree.CppType.Kind.Struct;
import static com.vizor.unreal.tree.CppType.plain;
import static com.vizor.unreal.tree.CppType.wildcardGeneric;
import static java.lang.String.join;
import static java.lang.System.lineSeparator;
import static java.text.MessageFormat.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

class ClientGenerator
{
    private static final String companyName = Config.get().getCompanyName();

    // URpcDispatcher is a parent type for all dispatchers
    private static final CppType parentType = plain("URpcClient", Class);

    // Frequently used string literals:
    private static final String rpcRequestsCategory = companyName + "|RPC Requests|";
    private static final String rpcResponsesCategory = companyName + "|RPC Responses|";
    private static final String dispatcherPrefix = "RpcClient";
    private static final String eventPrefix = "Event";
    private static final String eventTypePrefix = "F" + eventPrefix;

    static final String conduitName = "Conduit";
    static final String updateFunctionName = "HierarchicalUpdate";
    static final String initFunctionName = "HierarchicalInit";

    // Special structures, wrapping requests and responses:
    static final CppType reqWithCtx = wildcardGeneric("TRequestWithContext", Struct, 1);
    static final CppType rspWithSts = wildcardGeneric("TResponseWithStatus", Struct, 1);
    static final CppType conduitType = wildcardGeneric("TConduit", Struct, 2);
    static final CppArgument contextArg = new CppArgument(plain("FGrpcClientContext", Struct).makeRef(), "Context");

    private final ServiceElement service;
    private final CppType boolType;
    private final CppType voidType;

    // Bulk cache
    private final CppType clientType;
    private final CppType dispatcherType;
    private final Map<String, Tuple<CppType, CppType>> requestsResponses;

    private final List<CppField> conduits;
    private final List<Tuple<CppDelegate, CppField>> delegates;

    ClientGenerator(final ServiceElement service, final TypesProvider provider, final CppType clientType)
    {
        this.service = service;

        boolType = provider.getNative(boolean.class);
        voidType = provider.getNative(void.class);

        this.clientType = clientType;
        this.dispatcherType = plain("U" + service.name() + dispatcherPrefix, CppType.Kind.Class);

        final List<RpcElement> rpcs = service.rpcs();

        requestsResponses = new HashMap<>(rpcs.size());
        rpcs.forEach(r -> requestsResponses.put(r.name(),
            Tuple.of(
                provider.get(r.requestType()),
                provider.get(r.responseType())
            )
        ));

        conduits = genConduits();
        delegates = genDelegates();
    }

    CppClass genClientClass()
    {
        final List<CppFunction> methods = new ArrayList<>();
        methods.add(genInitialize());
        methods.add(genUpdate());
        methods.addAll(genProcedures());

        final List<CppField> fields = new ArrayList<>(genConduits());
        fields.addAll(delegates.stream().map(Tuple::second).collect(toList()));

        return new CppClass(dispatcherType, parentType, fields, methods);
    }

    final List<CppDelegate> getDelegates()
    {
        return delegates.stream().map(Tuple::first).collect(toList());
    }

    static String supressSuperString(final String functionName)
    {
        return "// No need to call Super::" + functionName + "(), it isn't required by design" + lineSeparator();
    }

    private List<CppField> genConduits()
    {
        return requestsResponses.entrySet().stream()
            .map(e -> {
                final CppType compiled = e.getValue().reduce((req, rsp) -> conduitType.makeGeneric(
                    reqWithCtx.makeGeneric(req),
                    rspWithSts.makeGeneric(rsp))
                );
                final CppField f = new CppField(compiled, e.getKey() + conduitName);
                f.enableAnnotations(false);

                return f;
            })
            .collect(toList());
    }

    private List<Tuple<CppDelegate, CppField>> genDelegates()
    {
        // two named arguments
        final CppArgument dispatcherArg = new CppArgument(dispatcherType.makePtr(), dispatcherPrefix);
        final CppArgument statusArg = new CppArgument(plain("FGrpcStatus", Struct), "Status");

        return requestsResponses.entrySet().stream()
            .map(e -> {
                final CppArgument responseArg = e.getValue().reduce(($, rsp) -> new CppArgument(rsp.makeRef(), "Response"));
                final CppType eventType = plain(eventTypePrefix + e.getKey() + service.name(), Struct);

                return Tuple.of(
                    new CppDelegate(eventType, asList(dispatcherArg, responseArg, statusArg)),
                    new CppField(eventType, eventPrefix + e.getKey())
                );
            })
            .peek(t -> {
                // should add an UE-specific annotations to these events
                t.second().addAnnotation(BlueprintAssignable);
                t.second().addAnnotation(Category, rpcResponsesCategory + service.name());
            })
            .collect(toList());
    }

    private CppFunction genInitialize()
    {
        final StringBuilder sb = new StringBuilder(supressSuperString(initFunctionName));
        final String cName = clientType.getName();

        final String workerVariableName = "Worker";

        sb.append(cName).append("* const ").append(workerVariableName).append(" = new ").append(cName).append("();");
        sb.append(lineSeparator()).append(lineSeparator());

        conduits.forEach(f -> {
            sb.append(workerVariableName).append("->").append(f.getName()).append(" = &");

            sb.append(f.getName()).append(';').append(lineSeparator());
            sb.append(f.getName()).append('.').append("AcquireRequestsProducer();");

            sb.append(lineSeparator()).append(lineSeparator());
        });

        sb.append("InnerWorker = TUniquePtr<RpcClientWorker>(").append(workerVariableName).append(");");
        sb.append(lineSeparator()).append(lineSeparator());

        final CppFunction init = new CppFunction(initFunctionName, voidType);

        init.isOverride = true;
        init.setBody(sb.toString());
        init.enableAnnotations(false);

        return init;
    }

    private CppFunction genUpdate()
    {
        final String dequeuePattern = join(lineSeparator(), asList(
            "if (!{0}.IsEmpty())",
            "'{'",
            "    {1} ResponseWithStatus;",
            "    while ({0}.Dequeue(ResponseWithStatus))",
            "        {2}.Broadcast(",
            "            this,",
            "            ResponseWithStatus.Response,",
            "            ResponseWithStatus.Status",
            "        );",
            "'}'"
        ));

        final StringBuilder sb = new StringBuilder(supressSuperString(updateFunctionName));
        for (int i = 0; i < conduits.size(); i++)
        {
            final Tuple<CppDelegate, CppField> delegate = delegates.get(i);
            final CppField conduit = conduits.get(i);

            final String dequeue = delegate.reduce((d, f) -> {
                final List<CppType> genericParams = conduit.getType().getGenericParams();
                final CppType requestWithContext = genericParams.get(1);

                return format(dequeuePattern, conduit.getName(), requestWithContext.toString(), f.getName());
            });

            sb.append(dequeue).append(lineSeparator()).append(lineSeparator());
        }

        final CppFunction update = new CppFunction(updateFunctionName, voidType);
        update.isOverride = true;
        update.enableAnnotations(false);
        update.setBody(sb.toString());

        return update;
    }

    private List<CppFunction> genProcedures()
    {
        final String pattern = join(lineSeparator(), asList(
            "if (!CanSendRequests())",
            "    return false;",
            "",
            "{0}Conduit.Enqueue(TRequestWithContext$New(Request, Context));",
            "return true;"
        ));

        final CppArgument contextArg = new CppArgument(plain("FGrpcClientContext", Struct).makeRef(), "Context");

        return requestsResponses.entrySet().stream()
            .map(e -> {
                final CppArgument requestArg = e.getValue().reduce((r, $) -> new CppArgument(r, "Request"));
                final CppFunction method = new CppFunction(e.getKey(), boolType, asList(requestArg, contextArg));

                method.setBody(format(pattern, e.getKey()));
                method.addAnnotation(BlueprintCallable);
                method.addAnnotation(Category, rpcRequestsCategory + service.name());

                return method;
            })
            .collect(toList());
    }
}
