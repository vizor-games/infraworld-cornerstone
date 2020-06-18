package com.vizor.unreal.config;

import java.nio.file.Path;
import static java.nio.file.Paths.get;

public final class DestinationConfig
{
    public DestinationConfig(Path pathPublic, Path pathPrivate) {
        this.pathPublic = pathPublic;
        this.pathPrivate = pathPrivate;
    }
    
    public DestinationConfig append(final Path childPath)
    {
        return new DestinationConfig(
            get(pathPublic.toString(), childPath.toString()), 
            get(pathPrivate.toString(), childPath.toString())
        );
    }

    public DestinationConfig append(String childPathString) {
        return append(get(childPathString));
    }

    public Path pathPublic;
    public Path pathPrivate;
}