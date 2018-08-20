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

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

import static java.util.Objects.hash;

/**
 * A tuple is a data structure that has a specific number and sequence of elements.
 *
 * An example of a tuple is a data structure with two elements that is used to store an identifier such as a person's
 * name in the first element, and the year in the second element.
 *
 * @param <T> Type of first value of the tuple.
 * @param <U> Type of second value of the tuple.
 */
public final class Tuple<T, U> implements Serializable
{
    /**
     * Use serialVersionUID for interoperability.
     */
    private static final long serialVersionUID = 2407615415524525623L;

    private final T first;
    private final U second;

    private Tuple(T first, U second)
    {
        this.first = first;
        this.second = second;
    }

    /**
     * Get first value of the tuple.
     * @return Value of the tuple.
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public final T first()
    {
        return first;
    }

    /**
     * Get second value of the tuple.
     * @return Value of the tuple.
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public final U second()
    {
        return second;
    }

    /**
     * Get the optional first value of the tuple.
     * @return Optional value of the tuple.
     */
    @SuppressWarnings("unused")
    public final Optional<T> firstOptional()
    {
        return Optional.ofNullable(first);
    }

    /**
     * Get the optional second value of the tuple.
     * @return Optional value of the tuple.
     */
    @SuppressWarnings("unused")
    public final Optional<U> secondOptional()
    {
        return Optional.ofNullable(second);
    }

    /**
     * Inverts the tuple. {@link Tuple#second()} becomes {@link Tuple#first()} and vice versa.
     * @return a new inverted tuple.
     */
    @SuppressWarnings("unused")
    public final Tuple<U, T> rotate()
    {
        return of(second, first);
    }

    /**
     * Folds a whole tuple into the single value.
     * @param accumulator Fold accumulator.
     * @param <R> A generic type to be folded to.
     * @return A single value, depends of what the accumulator returns.
     */
    @SuppressWarnings("unused")
    public final <R> R reduce(BiFunction<? super T, ? super U, ? extends R> accumulator)
    {
        return accumulator.apply(first, second);
    }

    /**
     * Constructs an instance of tuple of two arguments.
     * @param t First value of the tuple.
     * @param u Second value of the tuple.
     *
     * @param <T> Type of first value of the tuple.
     * @param <U> Type of second value of the tuple.
     *
     * @return A newly-constructed instance of Tuple.
     */
    public static <T, U> Tuple<T, U> of(final T t, final U u)
    {
        return new Tuple<>(t, u);
    }

    /**
     * Constructs an instance of tuple of the other tuple.
     * @param t Instance of the tuple to be copied.
     *
     * @param <T> Type of first value of the tuple.
     * @param <U> Type of second value of the tuple.
     *
     * @return A newly-constructed instance of Tuple.
     */
    public static <T, U> Tuple<T, U> of (final Tuple<T, U> t)
    {
        return new Tuple<>(t.first, t.second);
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