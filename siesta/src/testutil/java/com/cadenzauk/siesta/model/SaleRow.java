/*
 * Copyright (c) 2019 Cadenza United Kingdom Limited
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
package com.cadenzauk.siesta.model;

import javax.persistence.Table;
import java.math.BigDecimal;

@Table(name = "SALE", schema = "SIESTA")
public class SaleRow {
    private final long salespersonId;
    private final long widgetId;
    private final long quantity;
    private final BigDecimal price;

    private SaleRow(Builder builder) {
        salespersonId = builder.salespersonId;
        widgetId = builder.widgetId;
        quantity = builder.quantity;
        price = builder.price;
    }

    public long salespersonId() {
        return salespersonId;
    }

    public long widgetId() {
        return widgetId;
    }

    public long quantity() {
        return quantity;
    }

    public BigDecimal price() {
        return price;
    }

    public static Builder newBuilder() {
        return new Builder();
    }
    public static final class Builder {
        private long salespersonId;
        private long widgetId;
        private long quantity;
        private BigDecimal price;

        private Builder() {
        }

        public Builder salespersonId(long val) {
            salespersonId = val;
            return this;
        }

        public Builder widgetId(long val) {
            widgetId = val;
            return this;
        }

        public Builder quantity(long val) {
            quantity = val;
            return this;
        }

        public Builder price(BigDecimal val) {
            price = val;
            return this;
        }

        public SaleRow build() {
            return new SaleRow(this);
        }
    }
}
