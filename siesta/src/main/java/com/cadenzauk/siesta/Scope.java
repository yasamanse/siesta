/*
 * Copyright (c) 2017 Cadenza United Kingdom Limited
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.cadenzauk.siesta;

import com.cadenzauk.core.reflect.MethodInfo;
import com.cadenzauk.core.sql.RowMapper;
import com.cadenzauk.core.sql.RowMapperFactory;
import com.cadenzauk.core.stream.StreamUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.cadenzauk.core.lang.StringUtil.prepend;
import static java.util.stream.Collectors.toList;

public class Scope {
    private final Optional<Scope> outer;
    private final List<Alias<?>> aliases;
    private final Database database;
    private final AtomicLong labelCounter = new AtomicLong();

    public Scope(Database database, Alias<?>... aliases) {
        this.database = database;
        this.outer = Optional.empty();
        this.aliases = ImmutableList.copyOf(aliases);
    }

    private Scope(Scope outer, List<Alias<?>> aliases) {
        this.database = outer.database;
        this.outer = Optional.of(outer);
        this.aliases = ImmutableList.copyOf(aliases);
    }

    @Override
    public String toString() {
        return lines()
            .collect(Collectors.joining("\n"));
    }

    private Stream<String> lines() {
        return Stream.concat(
            aliases.stream().map(Alias::inFromClauseSql),
            StreamUtil.of(outer)
                .flatMap(s -> s.lines().map(prepend("  "))));
    }

    public Database database() {
        return database;
    }

    public Scope empty() {
        return new Scope(database());
    }

    public  Alias<?> findAlias(MethodInfo<?, ?> getter, Optional<String> requiredAlias) {
        Optional<Alias<?>> found = aliases.stream().flatMap(a -> a.as(getter, requiredAlias)).findFirst();
        return found
            .orElseGet(() -> outer.map(o -> o.findAlias(getter, requiredAlias))
                .orElseThrow(() -> new IllegalArgumentException("No such alias as " + requiredAlias + " in scope.")));
    }

    public <R> Alias<R> findAlias(Class<R> requiredRowClass, String requiredAlias) {
        Optional<Alias<R>> found = aliases.stream().flatMap(a -> a.as(requiredRowClass, requiredAlias)).findFirst();
        return found
            .orElseGet(() -> outer.map(o -> o.findAlias(requiredRowClass, requiredAlias))
                .orElseThrow(() -> new IllegalArgumentException("No such alias as " + requiredAlias + " in scope.")));
    }

    public <R> Alias<R> findAlias(Class<R> requiredRowClass) {
        List<Alias<R>> found = aliases.stream().flatMap(a -> a.as(requiredRowClass)).collect(toList());
        if (found.isEmpty()) {
            return outer.map(o -> o.findAlias(requiredRowClass))
                .orElseThrow(() -> new IllegalArgumentException("No alias for " + requiredRowClass + " in scope."));
        }
        if (found.size() > 1) {
            throw new IllegalArgumentException("More than one alias for " + requiredRowClass + " in scope.");
        }
        return found.get(0);
    }

    public <R> Scope plus(Alias<R> alias) {
        return new Scope(this, ImmutableList.of(alias));
    }

    public Scope plus(Scope inner) {
        return inner.outer
            .map(o -> new Scope(this.plus(o), inner.aliases))
            .orElseGet(() -> new Scope(this, inner.aliases));
    }

    public Dialect dialect() {
        return database().dialect();
    }

    public boolean isOutermost() {
        return !outer.isPresent();
    }

    public long newLabel() {
        return outer
            .map(Scope::newLabel)
            .orElseGet(labelCounter::incrementAndGet);
    }

    public Scope tracker(Alias<?> lookingFor, AtomicBoolean result) {
        return new Scope(this, ImmutableList.of()) {

            @Override
            public Alias<?> findAlias(MethodInfo<?,?> getter, Optional<String> requiredAlias) {
                Alias<?> found = super.findAlias(getter, requiredAlias);
                if (Objects.equals(lookingFor, found)) {
                    result.set(true);
                }
                return found;
            }

            @Override
            public <R> Alias<R> findAlias(Class<R> requiredRowClass, String requiredAlias) {
                Alias<R> found = super.findAlias(requiredRowClass, requiredAlias);
                if (Objects.equals(lookingFor, found)) {
                    result.set(true);
                }
                return found;
            }

            @Override
            public <R> Alias<R> findAlias(Class<R> requiredRowClass) {
                Alias<R> found = super.findAlias(requiredRowClass);
                if (Objects.equals(lookingFor, found)) {
                    result.set(true);
                }
                return found;
            }
        };
    }

    public static <S> BiFunction<Scope,String,RowMapper<S>> makeMapper(Class<S> resultClass) {
        return (scope, label) -> {
            final DataType<S> dataType = scope.database().getDataTypeOf(resultClass);
            return rs -> dataType.get(rs, label, scope.database()).orElse(null);
        };
    }

    public static <S> BiFunction<Scope,String,RowMapperFactory<S>> makeMapperFactory(Class<S> resultClass) {
        return (scope, defaultLabel) -> {
            final DataType<S> dataType = scope.database().getDataTypeOf(resultClass);
            return label -> rs -> dataType.get(rs, label.orElseGet(() -> defaultLabel), scope.database()).orElse(null);
        };
    }

    public static <S> BiFunction<Scope,String,RowMapper<S>> makeMapper(TypeToken<S> resultType) {
        return (scope, label) -> {
            final DataType<S> dataType = scope.database().getDataTypeOf(resultType);
            return rs -> dataType.get(rs, label, scope.database()).orElse(null);
        };
    }
}
