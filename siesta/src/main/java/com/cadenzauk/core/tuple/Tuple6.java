/*
 * Copyright (c) 2017, 2018 Cadenza United Kingdom Limited
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

package com.cadenzauk.core.tuple;

import com.cadenzauk.core.function.Function6;
import com.google.common.reflect.TypeToken;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.function.Function;

public class Tuple6<T1, T2, T3, T4, T5, T6> implements Tuple {
    private final T1 item1;
    private final T2 item2;
    private final T3 item3;
    private final T4 item4;
    private final T5 item5;
    private final T6 item6;

    public Tuple6(T1 item1, T2 item2, T3 item3, T4 item4, T5 item5, T6 item6) {
        this.item1 = item1;
        this.item2 = item2;
        this.item3 = item3;
        this.item4 = item4;
        this.item5 = item5;
        this.item6 = item6;
    }

    @Override
    public String toString() {
        return "(" + item1 +
            ", " + item2 +
            ", " + item3 +
            ", " + item4 +
            ", " + item5 +
            ", " + item6 +
            ')';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Tuple6<?,?,?,?,?,?> tuple6 = (Tuple6<?,?,?,?,?,?>) o;

        return new EqualsBuilder()
            .append(item1, tuple6.item1)
            .append(item2, tuple6.item2)
            .append(item3, tuple6.item3)
            .append(item4, tuple6.item4)
            .append(item5, tuple6.item5)
            .append(item6, tuple6.item6)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(item1)
            .append(item2)
            .append(item3)
            .append(item4)
            .append(item5)
            .append(item6)
            .toHashCode();
    }

    public T1 item1() {
        return item1;
    }

    public T2 item2() {
        return item2;
    }

    public T3 item3() {
        return item3;
    }

    public T4 item4() {
        return item4;
    }

    public T5 item5() {
        return item5;
    }

    public T6 item6() {
        return item6;
    }

    public <T> T map(Function6<? super T1,? super T2,? super T3,? super T4,? super T5,? super T6,? extends T> function) {
        return function.apply(item1, item2, item3, item4, item5, item6);
    }

    public <T> Tuple6<T,T2,T3,T4,T5,T6> map1(Function<? super T1, ? extends T> function) {
        return Tuple.of(
            function.apply(item1),
            item2,
            item3,
            item4,
            item5,
            item6
        );
    }

    public <T> Tuple6<T1,T,T3,T4,T5,T6> map2(Function<? super T2, ? extends T> function) {
        return Tuple.of(
            item1,
            function.apply(item2),
            item3,
            item4,
            item5,
            item6
        );
    }

    public <T> Tuple6<T1,T2,T,T4,T5,T6> map3(Function<? super T3, ? extends T> function) {
        return Tuple.of(
            item1,
            item2,
            function.apply(item3),
            item4,
            item5,
            item6
        );
    }

    public <T> Tuple6<T1,T2,T3,T,T5,T6> map4(Function<? super T4, ? extends T> function) {
        return Tuple.of(
            item1,
            item2,
            item3,
            function.apply(item4),
            item5,
            item6
        );
    }

    public <T> Tuple6<T1,T2,T3,T4,T,T6> map5(Function<? super T5, ? extends T> function) {
        return Tuple.of(
            item1,
            item2,
            item3,
            item4,
            function.apply(item5),
            item6
        );
    }

    public <T> Tuple6<T1,T2,T3,T4,T5,T> map6(Function<? super T6, ? extends T> function) {
        return Tuple.of(
            item1,
            item2,
            item3,
            item4,
            item5,
            function.apply(item6)
        );
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, T5, T6> TypeToken<T1> type1(TypeToken<Tuple6<T1,T2,T3,T4,T5,T6>> type) {
        return (TypeToken<T1>) type.resolveType(Tuple6.class.getTypeParameters()[0]);
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, T5, T6> TypeToken<T2> type2(TypeToken<Tuple6<T1,T2,T3,T4,T5,T6>> type) {
        return (TypeToken<T2>) type.resolveType(Tuple6.class.getTypeParameters()[1]);
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, T5, T6> TypeToken<T3> type3(TypeToken<Tuple6<T1,T2,T3,T4,T5,T6>> type) {
        return (TypeToken<T3>) type.resolveType(Tuple6.class.getTypeParameters()[2]);
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, T5, T6> TypeToken<T4> type4(TypeToken<Tuple6<T1,T2,T3,T4,T5,T6>> type) {
        return (TypeToken<T4>) type.resolveType(Tuple6.class.getTypeParameters()[3]);
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, T5, T6> TypeToken<T5> type5(TypeToken<Tuple6<T1,T2,T3,T4,T5,T6>> type) {
        return (TypeToken<T5>) type.resolveType(Tuple6.class.getTypeParameters()[4]);
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, T5, T6> TypeToken<T6> type6(TypeToken<Tuple6<T1,T2,T3,T4,T5,T6>> type) {
        return (TypeToken<T6>) type.resolveType(Tuple6.class.getTypeParameters()[5]);
    }
}
