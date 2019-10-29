package com.vizor.unreal.proto;

import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import com.squareup.wire.schema.internal.parser.RpcElement;
import com.squareup.wire.schema.internal.parser.ServiceElement;
import com.squareup.wire.schema.internal.parser.TypeElement;
import com.vizor.unreal.util.Tuple;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.isNull;

public final class Linker
{
    final ParseResult parseResult;

    // FIXME:
    // Symbol -> ProtoFileElement cache
    private static final Map<String, Tuple<ProtoFileElement, TypeElement>> symbolsCache = new HashMap<>();

    public Linker(final ParseResult parseResult)
    {
        this.parseResult = parseResult;
    }

    private Optional<ProtoFileElement> assertProtoFileParsed(String root)
    {
        final Optional<ProtoFileElement> importedProtoFileElementOptional = parseResult.find(new File(root + ".proto").toPath());
        if (!importedProtoFileElementOptional.isPresent())
            throw new RuntimeException("Can't find:" + "\"" + root + "\"" + " " + "in parse table!");
        return importedProtoFileElementOptional;
    }

    private Tuple<ProtoFileElement, TypeElement> tryFindSymbol(final String symbol, final String root)
    {
        final Optional<ProtoFileElement> importedProtoFileElementOptional = assertProtoFileParsed(root);

        return tryFindSymbol(symbol, importedProtoFileElementOptional.get());
    }

    private Tuple<ProtoFileElement, TypeElement> tryFindSymbol(final String symbol, final ProtoFileElement root)
    {
        // check symbol in cache
        final Tuple<ProtoFileElement, TypeElement> cachedSymbol = symbolsCache.get(symbol);
        if(!isNull(cachedSymbol))
            return cachedSymbol;

        // try find symbol transitively
        for (final String imp : root.publicImports())
        {
            final Optional<ProtoFileElement> importedProtoFileElementOptional = assertProtoFileParsed(imp);

            final ProtoFileElement importedProtoFileElement = importedProtoFileElementOptional.get();
            for (final TypeElement typeElement : importedProtoFileElement.types())
            {
                if (typeElement.name().equals(symbol))
                {
                    final Tuple<ProtoFileElement, TypeElement> tuple = Tuple.of(importedProtoFileElement, typeElement);

                    // cache symbol for future usage
                    symbolsCache.put(symbol, tuple);

                    return tuple;
                }
            }

            tryFindSymbol(symbol, importedProtoFileElement);
        }

        return Tuple.of(null, null);
    }

    private Optional<Tuple<String, String>> parseImportedSymbol(final String importStatement)
    {
        final String[] split = importStatement.split(".");
        if(split.length != 2)
            return Optional.empty();
        else
            return Optional.of(Tuple.of(split[0], split[1]));
    }

    public Map<ProtoFileElement, Map<ProtoFileElement, Set<TypeElement>>> resolveSymbols()
    {
        final Map<ProtoFileElement, Map<ProtoFileElement, Set<TypeElement>>> result = new HashMap<>();

        // walk through all parsed proto files
        for (final ProtoFileElement currentProtoFileElement : parseResult.getElements())
        {
            final Map<ProtoFileElement, Set<TypeElement>> importedSymbolsTable = new HashMap<>();
            result.put(currentProtoFileElement, importedSymbolsTable);

            // find all symbols which externally imported in the current document
            for (final ServiceElement serviceElement : currentProtoFileElement.services())
            {
                for (final RpcElement rpcElement : serviceElement.rpcs())
                {
                    final ArrayList<String> types = new ArrayList<>();
                    types.add(rpcElement.requestType());
                    types.add(rpcElement.responseType());

                    types.forEach(type -> {
                        parseImportedSymbol(type).ifPresent(t -> {
                            final Tuple<ProtoFileElement, TypeElement> tuple = tryFindSymbol(t.second(), t.first());

                            if (tuple.firstOptional().isPresent() && tuple.secondOptional().isPresent())
                            {
                                Set<TypeElement> externalSymbols;
                                if (importedSymbolsTable.containsKey(tuple.first()))
                                    externalSymbols = importedSymbolsTable.get(tuple.first());
                                else
                                {
                                    externalSymbols = new HashSet<>();
                                    importedSymbolsTable.put(tuple.first(), externalSymbols);
                                }

                                externalSymbols.add(tuple.second());
                            }
                            else
                                throw new RuntimeException("Unresolved external symbol:" + "\"" + tuple.second() + "\"");
                        });
                    });
                }
            }
        }
        return result;
    }
}
