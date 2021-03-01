package com.vizor.unreal.util;

@FunctionalInterface
public interface TriFunction<T, U, V, R> {
    public R apply(T first, U second, V third);
}