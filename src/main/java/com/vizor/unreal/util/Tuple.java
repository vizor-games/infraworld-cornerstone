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
package com.vizor.unreal.util;

import java.util.Objects;
import java.util.function.BiFunction;

import static java.util.Objects.hash;

public final class Tuple<T, U>
{
    private final T first;
    private final U second;

    private Tuple(T first, U second)
    {
        this.first = first;
        this.second = second;
    }

    public final T first()
    {
        return first;
    }

    public final U second()
    {
        return second;
    }

    /**
     * Inverts the tuple. {@link Tuple#second()} becomes {@link Tuple#first()} and vice versa.
     * @return a new inverted tuple.
     */
    public final Tuple<U, T> invert()
    {
        return of(second, first);
    }

    /**
     * Folds a whole tuple into the single value.
     * @param accumulator Fold accumulator.
     * @param <R> A generic type to be folded to.
     * @return A single value, depends of what the accumulator returns.
     */
    public final <R> R reduce(BiFunction<? super T, ? super U, ? extends R> accumulator)
    {
        return accumulator.apply(first, second);
    }

    public static <T, U> Tuple<T, U> of(T t, U u)
    {
        return new Tuple<>(t, u);
    }

    @Override
    public final String toString()
    {
        return "[first: " + first.toString() + "second: " + second.toString() + "]";
    }

    @Override
    public int hashCode()
    {
        return hash(first, second);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
            return true;

        if (obj instanceof Tuple)
        {
            final Tuple<?, ?> to = (Tuple<?, ?>) obj;

            return Objects.equals(to.first, first) && Objects.equals(to.second, second);
        }

        return false;
    }
}
