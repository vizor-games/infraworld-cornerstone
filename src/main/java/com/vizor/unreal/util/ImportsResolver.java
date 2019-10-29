package com.vizor.unreal.util;

import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import com.vizor.unreal.proto.ParseResult;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.vizor.unreal.util.Misc.reorder;

public class ImportsResolver
{
    private final Graph<ProtoFileElement> protoFileElementGraph;
    private final ParseResult parseResult;

    public ImportsResolver(final ParseResult parseResult)
    {
        this.parseResult = parseResult;
        this.protoFileElementGraph = new Graph<>(this.parseResult.getElements());
    }

    public List<ProtoFileElement> resolveImports()
    {
        final List<ProtoFileElement> vertices = protoFileElementGraph.getVertices();
        for (ProtoFileElement e : vertices)
        {
            final ArrayList<String> imports = new ArrayList<>();
            imports.addAll(e.publicImports());
            imports.addAll(e.imports());

            for (String imp : imports)
            {
                final Optional<ProtoFileElement> protoFileElement = parseResult.find(Paths.get(imp));
                if (protoFileElement.isPresent())
                    protoFileElementGraph.addEdge(protoFileElement.get(), e);
                else
                    throw new RuntimeException("Can not find file: " + imp);
            }
        }

        try
        {
            protoFileElementGraph.topologySort();
            reorder(vertices, protoFileElementGraph.getOrder());
        }
        catch (Graph.GraphHasCyclesException e)
        {
            throw new RuntimeException(e);
        }

        return vertices;
    }
}
