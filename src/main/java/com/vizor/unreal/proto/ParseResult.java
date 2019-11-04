package com.vizor.unreal.proto;

import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import com.squareup.wire.schema.internal.parser.TypeElement;
import com.vizor.unreal.util.Misc;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.squareup.wire.schema.Location.get;
import static com.squareup.wire.schema.internal.parser.ProtoParser.parse;
import static java.util.Objects.isNull;

public final class ParseResult
{
    private final Map<Path, ProtoFileElement> parseTable = new HashMap<>();
    private final Map<String, List<ProtoFileElement>> protoFileByPackage = new HashMap<>();

    public ParseResult(final List<Path> protoFiles)
    {
        protoFiles.forEach(path -> {
            final ProtoFileElement parsedProto = parseProto(path);

            parseTable.put(path, parsedProto);

            final String packageName = parsedProto.packageName();
            if (!packageName.isEmpty())
            {
                List<ProtoFileElement> protoFileElements = protoFileByPackage.get(packageName);
                if(isNull(protoFileElements))
                {
                    protoFileElements = new ArrayList<>();
                    protoFileElements.add(parsedProto);
                    protoFileByPackage.put(packageName, protoFileElements);
                }
                else
                {
                    protoFileElements.add(parsedProto);
                }
            }
        });
    }

    public Optional<ProtoFileElement> find(final Path path)
    {
        ProtoFileElement protoFileElement = parseTable.get(path);
        if (protoFileElement == null)
            for (final Path p : parseTable.keySet())
                if (p.endsWith(path))
                    return Optional.of(parseTable.get(p));

        return Optional.empty();
    }

    public Optional<TypeElement> lookupSymbol(final String packageName, final String symbol)
    {
        final List<ProtoFileElement> protoFilesInPackage = protoFileByPackage.get(packageName);
        if(isNull(protoFilesInPackage))
            return Optional.empty();

        for (final ProtoFileElement protoFileElement : protoFilesInPackage)
            for (final TypeElement type : protoFileElement.types())
                if (type.name().equals(symbol))
                    return Optional.of(type);

        return Optional.empty();
    }

    public List<ProtoFileElement> getElements()
    {
        return Collections.unmodifiableList(new ArrayList<>(parseTable.values()));
    }

    private ProtoFileElement parseProto(final Path path)
    {
        final String fileContent = Misc.readFileContent(path);
        return parse(get(path.toString()), fileContent);
    }
}
