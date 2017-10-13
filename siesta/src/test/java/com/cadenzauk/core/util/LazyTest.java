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
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static com.cadenzauk.core.testutil.FluentAssert.calling;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

class LazyTest {
    @Test
    void getSuccess() {
        Lazy<String> sut = new Lazy<>(() -> "Hello World");

        Try<String> result = sut.get();

        assertThat(result, is(Try.success("Hello World")));
    }

    @Test
    void getFailure() {
        Lazy<String> sut = new Lazy<>(() -> {
            throw new RuntimeException("Epic fail.");
        });

        Try<String> result = sut.get();

        assertThat(result, is(Try.failure(new RuntimeException("Epic fail."))));
    }

    @SuppressWarnings("unchecked")
    @Test
    void getOnlyOnceIfSuccessful() {
        ThrowingSupplier<String,RuntimeException> supplier = Mockito.mock(ThrowingSupplier.class);
        when(supplier.get()).thenReturn("Listen very carefully, I shall say this only once.");
        Lazy<String> sut = new Lazy<>(supplier);

        Try<String> result1 = sut.get();
        Try<String> result2 = sut.get();

        assertThat(result1, is(Try.success("Listen very carefully, I shall say this only once.")));
        assertThat(result2, is(Try.success("Listen very carefully, I shall say this only once.")));
        verify(supplier, times(1)).get();
    }

    @SuppressWarnings("unchecked")
    @Test
    void getOnlyOnceIfFailed() {
        ThrowingSupplier<String,RuntimeException> supplier = Mockito.mock(ThrowingSupplier.class);
        when(supplier.get()).thenThrow(new IllegalArgumentException("Bad argument."));
        Lazy<String> sut = new Lazy<>(supplier);

        Try<String> result1 = sut.get();
        Try<String> result2 = sut.get();

        assertThat(result1, is(Try.failure(new IllegalArgumentException("Bad argument."))));
        assertThat(result2, is(Try.failure(new IllegalArgumentException("Bad argument."))));
        verify(supplier, times(1)).get();
    }

    @Test
    void orElseThrowSuccess() {
        Lazy<String> sut = new Lazy<>(() -> "Hello World");

        String result = sut.orElseThrow();

        assertThat(result, is("Hello World"));
    }

    @Test
    void orElseThrowFailure() {
        Lazy<String> sut = new Lazy<>(() -> {
            throw new RuntimeException("Oops!");
        });

        calling(sut::orElseThrow)
            .shouldThrow(RuntimeException.class)
            .withMessage("Oops!");
    }

    @SuppressWarnings("unchecked")
    @Test
    void orElseThrowOnlyOnceIfSuccessful() {
        ThrowingSupplier<String,RuntimeException> supplier = Mockito.mock(ThrowingSupplier.class);
        when(supplier.get()).thenReturn("Listen very carefully, I shall say this only once.");
        Lazy<String> sut = new Lazy<>(supplier);

        String result1 = sut.orElseThrow();
        String result2 = sut.orElseThrow();

        assertThat(result1, is("Listen very carefully, I shall say this only once."));
        assertThat(result2, is("Listen very carefully, I shall say this only once."));
        verify(supplier, times(1)).get();
    }

    @SuppressWarnings("unchecked")
    @Test
    void orElseThrowOnlyOnceIfFailed() {
        ThrowingSupplier<String,RuntimeException> supplier = Mockito.mock(ThrowingSupplier.class);
        when(supplier.get()).thenThrow(new IllegalArgumentException("Bad argument."));
        Lazy<String> sut = new Lazy<>(supplier);

        calling(sut::orElseThrow)
            .shouldThrow(IllegalArgumentException.class)
            .withMessage("Bad argument.");
        calling(sut::orElseThrow)
            .shouldThrow(IllegalArgumentException.class)
            .withMessage("Bad argument.");

        verify(supplier, times(1)).get();
    }

    @Test
    void isKnownWhenNot() {
        Lazy<String> sut = new Lazy<>(() -> "Good boy");

        assertThat(sut.isKnown(), is(false));
        assertThat(sut.isKnownSuccess(), is(false));
        assertThat(sut.isKnownFailure(), is(false));
    }

    @Test
    void isKnownWhenSuccess() {
        Lazy<String> sut = new Lazy<>(() -> "Good boy");

        sut.get();

        assertThat(sut.isKnown(), is(true));
        assertThat(sut.isKnownSuccess(), is(true));
        assertThat(sut.isKnownFailure(), is(false));
    }

    @Test
    void isKnownWhenFailure() {
        Lazy<String> sut = new Lazy<>(() -> {
            throw new ArithmeticException("Bad boy");
        });

        sut.get();

        assertThat(sut.isKnown(), is(true));
        assertThat(sut.isKnownSuccess(), is(false));
        assertThat(sut.isKnownFailure(), is(true));
    }

    @Test
    void mapSuccessSuccess() {
        Lazy<String> sut = new Lazy<>(() -> "Hello");

        Lazy<String> map = sut.map(s -> s + " World!");

        assertThat(map.orElseThrow(), is("Hello World!"));
    }

    @Test
    void mapSuccessFailure() {
        Lazy<String> sut = new Lazy<>(() -> "Hello");

        Lazy<String> map = sut.map(s -> {
            throw new IllegalArgumentException("Too unfriendly.");
        });

        calling(map::orElseThrow)
            .shouldThrow(IllegalArgumentException.class)
            .withMessage("Too unfriendly.");
    }

    @Test
    void mapFailure() {
        Lazy<String> sut = new Lazy<>(() -> {
            throw new UnsupportedOperationException("No greetings for you.");
        });

        Lazy<String> map = sut.map(s -> s + " World!");

        calling(map::orElseThrow)
            .shouldThrow(UnsupportedOperationException.class)
            .withMessage("No greetings for you.");
    }

    @SuppressWarnings("unchecked")
    @Test
    void mapEachOnlyOnceIfSuccessful() {
        ThrowingSupplier<String,RuntimeException> supplier = Mockito.mock(ThrowingSupplier.class);
        ThrowingFunction<String,String,RuntimeException> mapFunction = Mockito.mock(ThrowingFunction.class);
        when(supplier.get()).thenReturn("Listen very carefully, ...");
        when(mapFunction.apply("Listen very carefully, ...")).thenReturn("I shall say this only once.");
        Lazy<String> sut = new Lazy<>(supplier);
        Lazy<String> map = sut.map(mapFunction);

        verifyZeroInteractions(supplier, mapFunction);

        Try<String> result1 = map.get();
        Try<String> result2 = map.get();

        assertThat(result1, is(Try.success("I shall say this only once.")));
        assertThat(result2, is(Try.success("I shall say this only once.")));
        verify(supplier, times(1)).get();
        verify(mapFunction, times(1)).apply("Listen very carefully, ...");
        verifyNoMoreInteractions(supplier, mapFunction);
    }

    @SuppressWarnings("unchecked")
    @Test
    void mapNotCalledOnFailure() {
        ThrowingSupplier<String,RuntimeException> supplier = Mockito.mock(ThrowingSupplier.class);
        ThrowingFunction<String,String,RuntimeException> mapFunction = Mockito.mock(ThrowingFunction.class);
        when(supplier.get()).thenThrow(new RuntimeException("Allo Allo"));
        Lazy<String> sut = new Lazy<>(supplier);
        Lazy<String> map = sut.map(mapFunction);

        verifyZeroInteractions(supplier, mapFunction);

        Try<String> result1 = map.get();
        Try<String> result2 = map.get();

        assertThat(result1, is(Try.failure(new RuntimeException("Allo Allo"))));
        assertThat(result2, is(Try.failure(new RuntimeException("Allo Allo"))));
        verify(supplier, times(1)).get();
        verifyNoMoreInteractions(supplier, mapFunction);
    }

    @SuppressWarnings("unchecked")
    @Test
    void flatMapEachOnlyOnceIfSuccessful() {
        ThrowingSupplier<String,RuntimeException> supplier = Mockito.mock(ThrowingSupplier.class);
        ThrowingFunction<String,Lazy<String>,RuntimeException> mapFunction = Mockito.mock(ThrowingFunction.class);
        when(supplier.get()).thenReturn("Listen very carefully, ...");
        when(mapFunction.apply("Listen very carefully, ...")).thenReturn(new Lazy<>(() -> "I shall say this only once."));
        Lazy<String> sut = new Lazy<>(supplier);
        Lazy<String> map = sut.flatMap(mapFunction);

        verifyZeroInteractions(supplier, mapFunction);

        Try<String> result1 = map.get();
        Try<String> result2 = map.get();

        assertThat(result1, is(Try.success("I shall say this only once.")));
        assertThat(result2, is(Try.success("I shall say this only once.")));
        verify(supplier, times(1)).get();
        verify(mapFunction, times(1)).apply("Listen very carefully, ...");
        verifyNoMoreInteractions(supplier, mapFunction);
    }

    @SuppressWarnings("unchecked")
    @Test
    void flatMapNotCalledOnFailure() {
        ThrowingSupplier<String,RuntimeException> supplier = Mockito.mock(ThrowingSupplier.class);
        ThrowingFunction<String,Lazy<String>,RuntimeException> mapFunction = Mockito.mock(ThrowingFunction.class);
        when(supplier.get()).thenThrow(new RuntimeException("Allo Allo"));
        Lazy<String> sut = new Lazy<>(supplier);
        Lazy<String> map = sut.flatMap(mapFunction);

        verifyZeroInteractions(supplier, mapFunction);

        Try<String> result1 = map.get();
        Try<String> result2 = map.get();

        assertThat(result1, is(Try.failure(new RuntimeException("Allo Allo"))));
        assertThat(result2, is(Try.failure(new RuntimeException("Allo Allo"))));
        verify(supplier, times(1)).get();
        verifyNoMoreInteractions(supplier, mapFunction);
    }

}