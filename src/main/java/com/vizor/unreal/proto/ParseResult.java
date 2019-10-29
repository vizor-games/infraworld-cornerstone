package com.vizor.unreal.proto;

import com.squareup.wire.schema.internal.parser.MessageElement;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
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

public final class ParseResult
{
    private final Map<Path, ProtoFileElement> parseTable = new HashMap<>();

    public ParseResult(final List<Path> protoFiles)
    {
        protoFiles.forEach(path -> parseTable.put(path, parseProto(path)));
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
