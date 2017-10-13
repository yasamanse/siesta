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

package com.cadenzauk.core.util;

import com.cadenzauk.core.function.ThrowingFunction;
import com.cadenzauk.core.function.ThrowingSupplier;
import com.cadenzauk.core.stream.StreamUtil;
import com.google.common.base.Throwables;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract class Try<T> {
    public abstract boolean isSuccess();

    public abstract boolean isFailure();

    public abstract T orElseThrow();

    public abstract Throwable throwable();

    public abstract <U, E extends Throwable> Try<U> map(ThrowingFunction<? super T,? extends U,? extends E> function);

    public abstract <U, E extends Throwable> Try<U> flatMap(ThrowingFunction<? super T,Try<U>,? extends E> function);

    public abstract T orElse(T alternativeValue);

    public abstract T orElseGet(Supplier<T> supplier);

    public abstract <E extends Throwable> Try<T> orElseTry(ThrowingSupplier<T, ? extends E> supplier);

    public abstract <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X;

    public abstract Try<T> throwError();

    public abstract void ifSuccess(Consumer<? super T> consumer);

    public abstract void ifFailure(Consumer<? super Throwable> consumer);

    public abstract Optional<T> toOptional();

    public abstract Stream<T> stream();

    public static <T, E extends Throwable> Try<T> trySupply(ThrowingSupplier<? extends T,E> supplier) {
        Objects.requireNonNull(supplier);
        try {
            return success(supplier.get());
        } catch (Throwable e) {
            return failure(e);
        }
    }

    public static <T> Try<T> success(T value) {
        return new Success<>(value);
    }

    public static <T> Try<T> failure(Throwable e) {
        return new Failure<>(e);
    }

    private static final class Success<T> extends Try<T> {
        private final T value;

        public Success(T value) {

            this.value = value;
        }

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public boolean isFailure() {
            return false;
        }

        @Override
        public T orElseThrow() {
            return value;
        }

        @Override
        public Throwable throwable() {
            throw new IllegalStateException("Cannot get an throwable from a successful Try.");
        }

        @Override
        public <U, E extends Throwable> Try<U> map(ThrowingFunction<? super T,? extends U,? extends E> function) {
            Objects.requireNonNull(function);
            return Try.trySupply(() -> function.apply(value));
        }

        @Override
        public <U, E extends Throwable> Try<U> flatMap(ThrowingFunction<? super T,Try<U>,? extends E> function) {
            Objects.requireNonNull(function);
            try {
                return function.apply(value);
            } catch (Throwable e) {
                return failure(e);
            }
        }

        @Override
        public T orElse(T alternativeValue) {
            return value;
        }

        @Override
        public T orElseGet(Supplier<T> supplier) {
            return value;
        }

        @Override
        public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
            return value;
        }

        @Override
        public Try<T> throwError() {
            return this;
        }

        @Override
        public <E extends Throwable> Try<T> orElseTry(ThrowingSupplier<T,? extends E> supplier) {
            return this;
        }

        @Override
        public void ifSuccess(Consumer<? super T> consumer) {
            Objects.requireNonNull(consumer);
            consumer.accept(value);
        }

        @Override
        public void ifFailure(Consumer<? super Throwable> consumer) {
        }

        @Override
        public Optional<T> toOptional() {
            return Optional.ofNullable(value);
        }

        @Override
        public Stream<T> stream() {
            return StreamUtil.of(toOptional());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;

            if (o == null || getClass() != o.getClass()) return false;

            Success<?> success = (Success<?>) o;

            return new EqualsBuilder()
                .append(value, success.value)
                .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                .append(value)
                .toHashCode();
        }
    }

    private static final class Failure<T> extends Try<T> {
        private final Throwable throwable;

        public Failure(Throwable throwable) {
            this.throwable = throwable;
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public boolean isFailure() {
            return true;
        }

        @Override
        public T orElseThrow() {
            Throwables.throwIfUnchecked(throwable);
            throw new RuntimeException(throwable);
        }

        @Override
        public Throwable throwable() {
            return throwable;
        }

        @Override
        public <U, E extends Throwable> Try<U> map(ThrowingFunction<? super T,? extends U,? extends E> function) {
            return Try.failure(throwable);
        }

        @Override
        public <U, E extends Throwable> Try<U> flatMap(ThrowingFunction<? super T,Try<U>,? extends E> function) {
            return Try.failure(throwable);
        }

        @Override
        public T orElse(T alternativeValue) {
            return alternativeValue;
        }

        @Override
        public T orElseGet(Supplier<T> supplier) {
            Objects.requireNonNull(supplier);
            return supplier.get();
        }

        @Override
        public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
            Objects.requireNonNull(exceptionSupplier);
            throw exceptionSupplier.get();
        }

        @Override
        public Try<T> throwError() {
            OptionalUtil.as(Error.class, throwable)
                .ifPresent(e -> { throw e; });
            return this;
        }

        @Override
        public <E extends Throwable> Try<T> orElseTry(ThrowingSupplier<T,? extends E> supplier) {
            Objects.requireNonNull(supplier);
            return Try.trySupply(supplier);
        }

        @Override
        public void ifSuccess(Consumer<? super T> consumer) {
        }

        @Override
        public void ifFailure(Consumer<? super Throwable> consumer) {
            Objects.requireNonNull(consumer);
            consumer.accept(throwable);
        }

        @Override
        public Optional<T> toOptional() {
            return Optional.empty();
        }

        @Override
        public Stream<T> stream() {
            return Stream.empty();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;

            if (o == null || getClass() != o.getClass()) return false;

            Failure<?> failure = (Failure<?>) o;

            return new EqualsBuilder()
                .append(throwable.toString(), failure.throwable.toString())
                .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                .append(throwable.toString())
                .toHashCode();
        }
    }
}
