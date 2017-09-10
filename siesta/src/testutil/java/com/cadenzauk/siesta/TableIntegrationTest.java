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

import com.cadenzauk.core.RandomValues;
import com.cadenzauk.core.lang.UncheckedAutoCloseable;
import com.cadenzauk.core.testutil.TemporalTestUtil;
import com.cadenzauk.core.tuple.Tuple2;
import com.cadenzauk.core.tuple.Tuple3;
import com.cadenzauk.core.tuple.Tuple6;
import com.cadenzauk.core.tuple.Tuple7;
import com.cadenzauk.siesta.dialect.H2Dialect;
import com.cadenzauk.siesta.grammar.expression.LiteralExpression;
import com.cadenzauk.siesta.grammar.expression.TypedExpression;
import com.cadenzauk.siesta.grammar.expression.ValueExpression;
import com.cadenzauk.siesta.grammar.select.CommonTableExpression;
import com.cadenzauk.siesta.jdbc.JdbcSqlExecutor;
import com.cadenzauk.siesta.model.ManufacturerRow;
import com.cadenzauk.siesta.model.SalespersonRow;
import com.cadenzauk.siesta.model.TestRow;
import com.cadenzauk.siesta.model.WidgetRow;
import com.cadenzauk.siesta.model.WidgetViewRow;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.AssumptionViolatedException;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static com.cadenzauk.core.RandomValues.randomLocalDateTime;
import static com.cadenzauk.core.RandomValues.randomZonedDateTime;
import static com.cadenzauk.core.testutil.TemporalTestUtil.withTimeZone;
import static com.cadenzauk.siesta.grammar.expression.Aggregates.count;
import static com.cadenzauk.siesta.grammar.expression.Aggregates.countDistinct;
import static com.cadenzauk.siesta.grammar.expression.Aggregates.max;
import static com.cadenzauk.siesta.grammar.expression.Aggregates.min;
import static com.cadenzauk.siesta.grammar.expression.CoalesceFunction.coalesce;
import static com.cadenzauk.siesta.grammar.expression.DateFunctions.currentDate;
import static com.cadenzauk.siesta.grammar.expression.DateFunctions.currentTimestamp;
import static com.cadenzauk.siesta.grammar.expression.DateFunctions.currentTimestampLocal;
import static com.cadenzauk.siesta.grammar.expression.DateFunctions.day;
import static com.cadenzauk.siesta.grammar.expression.DateFunctions.hour;
import static com.cadenzauk.siesta.grammar.expression.DateFunctions.minute;
import static com.cadenzauk.siesta.grammar.expression.DateFunctions.month;
import static com.cadenzauk.siesta.grammar.expression.DateFunctions.second;
import static com.cadenzauk.siesta.grammar.expression.DateFunctions.year;
import static com.cadenzauk.siesta.grammar.expression.ExpressionBuilder.when;
import static com.cadenzauk.siesta.grammar.expression.StringFunctions.lower;
import static com.cadenzauk.siesta.grammar.expression.StringFunctions.upper;
import static com.cadenzauk.siesta.grammar.expression.TypedExpression.cast;
import static com.cadenzauk.siesta.grammar.expression.TypedExpression.literal;
import static com.cadenzauk.siesta.grammar.expression.TypedExpression.value;
import static com.cadenzauk.siesta.model.TestDatabase.testDatabase;
import static com.cadenzauk.siesta.model.TestDatabase.testDatabaseBuilder;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(JUnitParamsRunner.class)
public abstract class TableIntegrationTest extends IntegrationTest {
    @Test
    public void selectFromDatabaseOneTable() {
        Database database = testDatabase(dataSource, dialect);
        WidgetRow aWidget = WidgetRow.newBuilder()
            .widgetId(newId())
            .manufacturerId(newId())
            .name("Dodacky")
            .description(Optional.of("Thingamibob"))
            .build();
        database.insert(aWidget);

        Optional<WidgetRow> theSame = database.from(WidgetRow.class)
            .where(WidgetRow::widgetId).isEqualTo(aWidget.widgetId())
            .optional();

        assertThat(theSame, is(Optional.of(aWidget)));
    }

    @Test
    public void selectFromDatabaseTwoTables() {
        Database database = testDatabase(dataSource, dialect);
        long manufacturerId = newId();
        ManufacturerRow aManufacturer = ManufacturerRow.newBuilder()
            .manufacturerId(manufacturerId)
            .name(Optional.of("Makers"))
            .build();
        WidgetRow aWidget = WidgetRow.newBuilder()
            .widgetId(newId())
            .manufacturerId(manufacturerId)
            .name("Dodacky")
            .description(Optional.of("Thingamibob"))
            .build();
        database.insert(aManufacturer);
        database.insert(aWidget);

        Alias<WidgetRow> w = database.table(WidgetRow.class).as("w");
        Alias<ManufacturerRow> m = database.table(ManufacturerRow.class).as("m");
        Optional<WidgetRow> theSame = database.from(w)
            .join(m)
            .on(m, ManufacturerRow::manufacturerId).isEqualTo(w, WidgetRow::manufacturerId)
            .where(w, WidgetRow::name).isEqualTo("Dodacky")
            .and(w, WidgetRow::widgetId).isEqualTo(aWidget.widgetId())
            .optional()
            .map(Tuple2::item1);

        assertThat(theSame, is(Optional.of(aWidget)));
    }

    @Test
    public void update() {
        Database database = testDatabase(dataSource, dialect);
        WidgetRow aWidget = WidgetRow.newBuilder()
            .widgetId(newId())
            .manufacturerId(newId())
            .name("Dodacky")
            .description(Optional.of("Thingamibob"))
            .build();
        database.insert(aWidget);

        int updated = database.update(WidgetRow.class)
            .set(WidgetRow::name).to("Sprocket")
            .set(WidgetRow::description).toNull()
            .where(WidgetRow::widgetId).isEqualTo(aWidget.widgetId())
            .execute();

        Optional<WidgetRow> sprocket = database.from(WidgetRow.class)
            .where(WidgetRow::widgetId).isEqualTo(aWidget.widgetId())
            .optional();
        assertThat(sprocket.map(WidgetRow::name), is(Optional.of("Sprocket")));
        assertThat(sprocket.flatMap(WidgetRow::description), is(Optional.empty()));
        assertThat(updated, is(1));
    }

    @Test
    public void selectRow() {
        Database database = testDatabase(dataSource, dialect);
        long manufacturerId = newId();
        ManufacturerRow aManufacturer = ManufacturerRow.newBuilder()
            .manufacturerId(manufacturerId)
            .name(Optional.of("Acclaimed Widgets"))
            .build();
        WidgetRow aWidget1 = WidgetRow.newBuilder()
            .widgetId(newId())
            .manufacturerId(manufacturerId)
            .name("Acclaimed 1")
            .build();
        WidgetRow aWidget2 = WidgetRow.newBuilder()
            .widgetId(newId())
            .manufacturerId(manufacturerId)
            .name("Acclaimed 2")
            .build();
        database.insert(aManufacturer);
        database.insert(aWidget1, aWidget2);

        List<WidgetRow> acclaimedWidgets1 = database
            .from(ManufacturerRow.class, "m")
            .join(WidgetRow.class, "w").on(WidgetRow::manufacturerId).isEqualTo(ManufacturerRow::manufacturerId)
            .select(WidgetRow.class)
            .where(ManufacturerRow::name).isEqualTo("Acclaimed Widgets")
            .orderBy(WidgetRow::name, Order.DESC)
            .list();

        List<Tuple2<String,WidgetRow>> acclaimedWidgets2 = database
            .from(ManufacturerRow.class, "m")
            .join(WidgetRow.class, "w").on(WidgetRow::manufacturerId).isEqualTo(ManufacturerRow::manufacturerId)
            .select(WidgetRow::name, "name")
            .comma(WidgetRow.class)
            .where(ManufacturerRow::name).isEqualTo("Acclaimed Widgets")
            .orderBy(WidgetRow::name, Order.ASC)
            .list();

        assertThat(acclaimedWidgets1, hasSize(2));
        assertThat(acclaimedWidgets1.get(0).name(), is("Acclaimed 2"));
        assertThat(acclaimedWidgets1.get(1).name(), is("Acclaimed 1"));

        assertThat(acclaimedWidgets2, hasSize(2));
        assertThat(acclaimedWidgets2.get(0).item1(), is("Acclaimed 1"));
        assertThat(acclaimedWidgets2.get(1).item1(), is("Acclaimed 2"));
        assertThat(acclaimedWidgets2.get(0).item2().name(), is("Acclaimed 1"));
        assertThat(acclaimedWidgets2.get(1).item2().name(), is("Acclaimed 2"));
    }

    @Test
    public void selectIntoView() {
        Database database = testDatabase(dataSource, dialect);
        long manufacturerId = newId();
        ManufacturerRow aManufacturer = ManufacturerRow.newBuilder()
            .manufacturerId(manufacturerId)
            .name(Optional.of("Acme"))
            .build();
        WidgetRow aWidget = WidgetRow.newBuilder()
            .widgetId(newId())
            .manufacturerId(manufacturerId)
            .name("Gizmo")
            .description(Optional.of("Acme's Patent Gizmo"))
            .build();
        database.insert(aManufacturer);
        database.insert(aWidget);

        Optional<WidgetViewRow> gizmo = database
            .from(WidgetRow.class, "w")
            .leftJoin(ManufacturerRow.class, "m").on(ManufacturerRow::manufacturerId).isEqualTo(WidgetRow::manufacturerId)
            .selectInto(WidgetViewRow.class, "v")
            .with(WidgetRow::widgetId).as(WidgetViewRow::widgetId)
            .with(WidgetRow::name).as(WidgetViewRow::widgetName)
            .with(WidgetRow::description).as(WidgetViewRow::description)
            .with(WidgetRow::manufacturerId).as(WidgetViewRow::manufacturerId)
            .with(ManufacturerRow::name).as(WidgetViewRow::manufacturerName)
            .where(WidgetRow::widgetId).isEqualTo(aWidget.widgetId())
            .optional();

        assertThat(gizmo.map(WidgetViewRow::widgetName), is(Optional.of("Gizmo")));
        assertThat(gizmo.flatMap(WidgetViewRow::manufacturerName), is(Optional.of("Acme")));
        assertThat(gizmo.flatMap(WidgetViewRow::description), is(Optional.of("Acme's Patent Gizmo")));
    }

    @Test
    public void groupBy() {
        Database database = testDatabase(dataSource, dialect);
        long manufacturer1 = newId();
        long manufacturer2 = newId();
        WidgetRow aWidget1 = WidgetRow.newBuilder()
            .widgetId(newId())
            .manufacturerId(manufacturer1)
            .name("Grouper 1")
            .build();
        WidgetRow aWidget2 = WidgetRow.newBuilder()
            .widgetId(newId())
            .manufacturerId(manufacturer1)
            .name("Grouper 2")
            .build();
        WidgetRow aWidget3 = WidgetRow.newBuilder()
            .widgetId(newId())
            .manufacturerId(manufacturer2)
            .name("Grouper 3")
            .build();
        database.insert(aWidget1, aWidget2, aWidget3);

        List<Tuple3<Long,String,String>> result = database.from(WidgetRow.class)
            .select(WidgetRow::manufacturerId).comma(max(WidgetRow::name)).comma(min(WidgetRow::name))
            .where(WidgetRow::widgetId).isIn(aWidget1.widgetId(), aWidget2.widgetId(), aWidget3.widgetId())
            .groupBy(WidgetRow::manufacturerId)
            .orderBy(WidgetRow::manufacturerId, Order.DESC).then(max(WidgetRow::name)).then(min(WidgetRow::name))
            .list();

        assertThat(result.get(0).item1(), is(manufacturer2));
        assertThat(result.get(0).item2(), is("Grouper 3"));
        assertThat(result.get(0).item3(), is("Grouper 3"));
        assertThat(result.get(1).item1(), is(manufacturer1));
        assertThat(result.get(1).item2(), is("Grouper 2"));
        assertThat(result.get(1).item3(), is("Grouper 1"));
    }

    @Test
    public void countAndCountDistinct() {
        Database database = testDatabase(dataSource, dialect);
        long manufacturer1 = newId();
        long manufacturer2 = newId();
        WidgetRow aWidget1 = WidgetRow.newBuilder()
            .widgetId(newId())
            .manufacturerId(manufacturer1)
            .name("Gizmo")
            .build();
        WidgetRow aWidget2 = WidgetRow.newBuilder()
            .widgetId(newId())
            .manufacturerId(manufacturer1)
            .name("Gizmo")
            .build();
        WidgetRow aWidget3 = WidgetRow.newBuilder()
            .widgetId(newId())
            .manufacturerId(manufacturer2)
            .name("Gizmo 2")
            .build();
        database.insert(aWidget1, aWidget2, aWidget3);

        List<Tuple3<Long,Integer,Integer>> result = database.from(WidgetRow.class)
            .select(WidgetRow::manufacturerId).comma(countDistinct(WidgetRow::name)).comma(count())
            .where(WidgetRow::widgetId).isIn(aWidget1.widgetId(), aWidget2.widgetId(), aWidget3.widgetId())
            .groupBy(WidgetRow::manufacturerId)
            .orderBy(WidgetRow::manufacturerId, Order.ASC)
            .list();

        assertThat(result.get(0).item1(), is(manufacturer1));
        assertThat(result.get(0).item2(), is(1));
        assertThat(result.get(0).item3(), is(2));
        assertThat(result.get(1).item1(), is(manufacturer2));
        assertThat(result.get(1).item2(), is(1));
        assertThat(result.get(1).item3(), is(1));
    }

    @Test
    public void coalesceFunc() {
        Database database = testDatabase(dataSource, dialect);
        long manufacturer1 = newId();
        long manufacturer2 = newId();
        ManufacturerRow twoParts = ManufacturerRow.newBuilder()
            .manufacturerId(manufacturer1)
            .name(Optional.of("Two Parts"))
            .build();
        ManufacturerRow noParts = ManufacturerRow.newBuilder()
            .manufacturerId(manufacturer2)
            .name(Optional.of("No Parts"))
            .build();
        WidgetRow aWidget1 = WidgetRow.newBuilder()
            .widgetId(newId())
            .manufacturerId(manufacturer1)
            .name("Name 1")
            .build();
        WidgetRow aWidget2 = WidgetRow.newBuilder()
            .widgetId(newId())
            .manufacturerId(manufacturer1)
            .name("Name 2")
            .description(Optional.of("Description 2"))
            .build();
        database.insert(noParts);
        database.insert(twoParts);
        database.insert(aWidget1);
        database.insert(aWidget2);

        List<Tuple2<String,String>> result = database.from(ManufacturerRow.class, "m")
            .leftJoin(WidgetRow.class, "w").on(WidgetRow::manufacturerId).isEqualTo(ManufacturerRow::manufacturerId)
            .select(ManufacturerRow::name)
            .comma(coalesce(WidgetRow::description).orElse(WidgetRow::name).orElse("-- no parts --"))
            .where(ManufacturerRow::manufacturerId).isIn(manufacturer1, manufacturer2)
            .orderBy(ManufacturerRow::manufacturerId).then(WidgetRow::name)
            .list();

        assertThat(result, hasSize(3));
        assertThat(result.get(0).item1(), is("Two Parts"));
        assertThat(result.get(0).item2(), is("Name 1"));
        assertThat(result.get(1).item1(), is("Two Parts"));
        assertThat(result.get(1).item2(), is("Description 2"));
        assertThat(result.get(2).item1(), is("No Parts"));
        assertThat(result.get(2).item2(), is("-- no parts --"));
    }

    @Test
    public void leftJoinOfMissingIsNull() {
        Database database = testDatabase(dataSource, dialect);
        long manufacturer1 = newId();
        long manufacturer2 = newId();
        ManufacturerRow twoParts = ManufacturerRow.newBuilder()
            .manufacturerId(manufacturer1)
            .name(Optional.of("Has a Part"))
            .build();
        ManufacturerRow noParts = ManufacturerRow.newBuilder()
            .manufacturerId(manufacturer2)
            .name(Optional.of("Has No Parts"))
            .build();
        WidgetRow aWidget1 = WidgetRow.newBuilder()
            .widgetId(newId())
            .manufacturerId(manufacturer1)
            .name("Name 1")
            .build();
        database.insert(noParts, twoParts);
        database.insert(aWidget1);

        List<Tuple2<ManufacturerRow,WidgetRow>> result = database.from(ManufacturerRow.class, "m")
            .leftJoin(WidgetRow.class, "w").on(WidgetRow::manufacturerId).isEqualTo(ManufacturerRow::manufacturerId)
            .where(ManufacturerRow::manufacturerId).isIn(manufacturer1, manufacturer2)
            .orderBy(ManufacturerRow::manufacturerId)
            .list();

        assertThat(result, hasSize(2));
        assertThat(result.get(0).item1().name(), is(Optional.of("Has a Part")));
        assertThat(result.get(0).item2().name(), is("Name 1"));
        assertThat(result.get(1).item1().name(), is(Optional.of("Has No Parts")));
        assertThat(result.get(1).item2(), nullValue());
    }

    @Test
    public void delete() {
        Database database = testDatabase(dataSource, dialect);
        long manufacturer1 = newId();
        long manufacturer2 = newId();
        ManufacturerRow manufacturerRow1 = ManufacturerRow.newBuilder()
            .manufacturerId(manufacturer1)
            .name(Optional.of("tbc 1"))
            .build();
        ManufacturerRow manufacturerRow2 = ManufacturerRow.newBuilder()
            .manufacturerId(manufacturer2)
            .name(Optional.of("tbc 2"))
            .build();
        database.insert(manufacturerRow1, manufacturerRow2);
        assertThat(database.from(ManufacturerRow.class).select(count()).where(ManufacturerRow::name).isLike("tbc %").single(), is(2));

        int rowsDeleted = database.delete(ManufacturerRow.class).where(ManufacturerRow::name).isEqualTo("tbc 1").execute();

        assertThat(database.from(ManufacturerRow.class).select(count()).where(ManufacturerRow::name).isLike("tbc %").single(), is(1));
        assertThat(rowsDeleted, is(1));
    }

    @Test
    public void concatTest() {
        Database database = testDatabase(dataSource, dialect);
        SalespersonRow george = SalespersonRow.newBuilder()
            .salespersonId(newId())
            .firstName("George")
            .surname("Jetson")
            .build();
        database.insert(george);

        Alias<SalespersonRow> s = database.table(SalespersonRow.class).as("s");
        String name = database.from(s)
            .select(upper(s.column(SalespersonRow::firstName).concat(cast(" ").asVarchar(1)).concat(SalespersonRow::surname).concat(cast(1).asInteger())))
            .where(SalespersonRow::salespersonId).isEqualTo(george.salespersonId())
            .single();
        assertThat(name, is("GEORGE JETSON1"));
    }

    @Test
    @Parameters(value = {"America/Anchorage", "America/Sao_Paulo", "UTC", "Europe/London", "Africa/Johannesburg", "Pacific/Chatham"})
    public void currentDateTest(String timeZone) {
        Database database = testDatabase(dataSource, dialect);
        try (UncheckedAutoCloseable ignored = TemporalTestUtil.withTimeZone(timeZone)) {
            LocalDate before = LocalDate.now(database.databaseTimeZone());
            LocalDate now = database
                .select(currentDate())
                .single();
            LocalDate after = LocalDate.now(database.databaseTimeZone());

            System.out.printf("%s <= %s <= %s%n", before, now, after);
            assertThat(before.isAfter(now), is(false));
            assertThat(now.isAfter(after), is(false));
        }
    }

    @Test
    @Parameters(value = {"America/Anchorage", "America/Sao_Paulo", "UTC", "Europe/London", "Africa/Johannesburg", "Pacific/Chatham"})
    public void currentTimestampLocalTest(String timeZone) {
        Database database = testDatabase(dataSource, dialect);
        try (UncheckedAutoCloseable ignored = TemporalTestUtil.withTimeZone(timeZone)) {
            LocalDateTime before = LocalDateTime.now(database.databaseTimeZone()).minusSeconds(10);
            LocalDateTime now = database
                .select(currentTimestampLocal())
                .single();
            LocalDateTime after = LocalDateTime.now(database.databaseTimeZone()).plusSeconds(10);

            System.out.printf("%s <= %s <= %s%n", before, now, after);
            assertThat(before.isAfter(now), is(false));
            assertThat(now.isAfter(after), is(false));
        }
    }

    @Test
    @Parameters(value = {"America/Anchorage", "America/Sao_Paulo", "UTC", "Europe/London", "Africa/Johannesburg", "Pacific/Chatham"})
    public void currentTimestampTest(String timeZone) {
        Database database = testDatabase(dataSource, dialect);
        try (UncheckedAutoCloseable ignored = TemporalTestUtil.withTimeZone(timeZone)) {
            ZonedDateTime before = ZonedDateTime.now(ZoneId.of("UTC")).minusSeconds(10);
            ZonedDateTime now = database
                .select(currentTimestamp())
                .single();
            ZonedDateTime after = ZonedDateTime.now(ZoneId.of("UTC")).plusSeconds(10);

            System.out.printf("%s <= %s <= %s%n", before, now, after);
            assertThat(before.isAfter(now), is(false));
            assertThat(now.isAfter(after), is(false));
        }
    }

    @Test
    public void selectIn() {
        Database database = testDatabase(dataSource, dialect);
        SalespersonRow fred = SalespersonRow.newBuilder()
            .salespersonId(newId())
            .firstName("Fred")
            .surname("Smith")
            .build();
        SalespersonRow bruce = SalespersonRow.newBuilder()
            .salespersonId(newId())
            .firstName("Bruce")
            .surname("Smith")
            .build();
        database.insert(fred, bruce);

        List<SalespersonRow> smiths1 = database.from(SalespersonRow.class)
            .where(SalespersonRow::salespersonId).isIn(
                database.from(SalespersonRow.class, "s")
                    .select(SalespersonRow::salespersonId)
                    .where(SalespersonRow::surname).isEqualTo("Smith")
            )
            .orderBy(SalespersonRow::firstName)
            .list();
        List<SalespersonRow> smiths2 = database.from(SalespersonRow.class)
            .where(SalespersonRow::salespersonId).isNotIn(
                database.from(SalespersonRow.class, "s")
                    .select(SalespersonRow::salespersonId)
                    .where(SalespersonRow::surname).isNotEqualTo("Smith")
            )
            .orderBy(SalespersonRow::firstName, Order.DESC)
            .list();

        assertThat(smiths1, hasSize(2));
        assertThat(smiths1.get(0).firstName(), is("Bruce"));
        assertThat(smiths1.get(1).firstName(), is("Fred"));
        assertThat(smiths2, hasSize(2));
        assertThat(smiths2.get(0).firstName(), is("Fred"));
        assertThat(smiths2.get(1).firstName(), is("Bruce"));
    }

    @Test
    public void caseExpression() {
        Database database = testDatabase(dataSource, dialect);
        long manufacturerId = newId();
        WidgetRow doohicky = WidgetRow.newBuilder()
            .widgetId(newId())
            .name("Doohicky")
            .manufacturerId(manufacturerId)
            .build();
        WidgetRow doohickie = WidgetRow.newBuilder()
            .widgetId(newId())
            .name("Doohickie")
            .manufacturerId(manufacturerId)
            .build();
        WidgetRow doodad = WidgetRow.newBuilder()
            .widgetId(newId())
            .name("Doodad")
            .manufacturerId(manufacturerId)
            .build();
        WidgetRow doofer = WidgetRow.newBuilder()
            .widgetId(newId())
            .name("Doofer")
            .manufacturerId(manufacturerId)
            .build();
        database.insert(doohicky, doohickie, doodad, doofer);

        List<String> result = database.from(WidgetRow.class)
            .select(
                when(WidgetRow::name).isLike("Doohick%").then(lower(WidgetRow::name))
                    .when(WidgetRow::name).isEqualTo(value("Doodad")).then(upper(WidgetRow::name))
                    .orElse(WidgetRow::name))
            .where(WidgetRow::manufacturerId).isEqualTo(manufacturerId)
            .orderBy(WidgetRow::widgetId)
            .list();

        assertThat(result.get(0), is("doohicky"));
        assertThat(result.get(1), is("doohickie"));
        assertThat(result.get(2), is("DOODAD"));
        assertThat(result.get(3), is("Doofer"));
    }

    @SuppressWarnings("unused")
    private Object[] parametersForLiteral() {
        return new Object[]{
            new BigDecimal("5001.12"),
            (byte) 0xff,
            new byte[]{(byte) 0xff, 0x1, 0x7f},
            3.1415,
            2.71f,
            LocalDate.of(2014, 9, 12),
            LocalDateTime.of(2017, 1, 2, 3, 4, 5, 6000000),
            9223372036854775807L,
            (short) 32767,
            "Abc'def\"g hi",
        };
    }

    @Test
    @Parameters(method = "parametersForLiteral")
    public <T> void literalTest(T value) {
        Database database = testDatabase(dataSource, dialect);

        T result = database.select(literal(value)).single();

        assertThat(result, is(value));
    }

    @SuppressWarnings("unused")
    private Object[][] parametersForZonedDateTimeLiteral() {
        return new Object[][]{
            {"UTC", ZonedDateTime.of(2001, 6, 7, 1, 2, 3, 4000000, ZoneId.of("UTC"))},
            {"Europe/Berlin", ZonedDateTime.of(2002, 6, 7, 1, 2, 3, 4000000, ZoneId.of("UTC"))},
            {"UTC", ZonedDateTime.of(2003, 6, 7, 1, 2, 3, 4000000, ZoneId.of("America/Anchorage"))},
            {"America/New_York", ZonedDateTime.of(2004, 6, 7, 1, 2, 3, 4000000, ZoneId.of("America/Anchorage"))},
            {"America/Anchorage", ZonedDateTime.of(2005, 6, 7, 1, 2, 3, 4000000, ZoneId.of("America/Anchorage"))}
        };
    }

    @Test
    @Parameters(method = "parametersForZonedDateTimeLiteral")
    public void zonedDateTimeLiteral(String timeZone, ZonedDateTime value) {
        Database database = testDatabase(dataSource, dialect);
        try (UncheckedAutoCloseable ignored = withTimeZone(timeZone)) {
            ZonedDateTime expected = value.withZoneSameInstant(ZoneId.of("UTC"));

            ZonedDateTime result = database.select(literal(value)).single();

            assertThat(result, is(expected));
        }
    }

    @SuppressWarnings("unused")
    private Object[][] parametersForLocalDateTimeLiteral() {
        return new Object[][]{
            {"UTC", LocalDateTime.of(2017, 7, 6, 5, 4, 3, 2000000)},
            {"Europe/Paris", LocalDateTime.of(2017, 7, 6, 5, 4, 3, 2000000)},
            {"Pacific/Chatham", LocalDateTime.of(2017, 7, 6, 5, 4, 3, 2000000)}
        };
    }

    @Test
    @Parameters(method = "parametersForLocalDateTimeLiteral")
    public void localDateTimeLiteral(String timeZone, LocalDateTime value) {
        Database database = testDatabase(dataSource, dialect);
        try (UncheckedAutoCloseable ignored = withTimeZone(timeZone)) {
            LocalDateTime expected = value;

            LocalDateTime result = database.select(literal(value)).single();

            assertThat(result, is(expected));
        }
    }

    @Test
    @Parameters(value = {"America/Anchorage", "America/Brazilia", "UTC", "Europe/London", "Africa/Johannesburg", "Pacific/Chatham"})
    public void canRoundTripLocalDateTimes(String timeZone) {
        Database database = testDatabase(dataSource, dialect);
        try (UncheckedAutoCloseable ignored = withTimeZone(timeZone)) {
            LocalDateTime expected = randomLocalDateTime();
            TestRow input = new TestRow(expected);

            database.insert(input);
            LocalDateTime result = database
                .from(TestRow.class)
                .select(TestRow::localDateTimeOpt)
                .where(TestRow::guid).isEqualTo(input.guid())
                .single();

            assertThat(result, is(expected));
        }
    }

    @Test
    @Parameters(value = {"America/Anchorage", "America/Sao_Paulo", "UTC", "Europe/London", "Africa/Johannesburg", "Pacific/Chatham"})
    public void canRoundTripZonedDateTimes(String timeZone) {
        Database database = testDatabase(dataSource, dialect);
        try (UncheckedAutoCloseable ignored = withTimeZone(timeZone)) {
            ZonedDateTime expected = randomZonedDateTime(ZoneId.of("UTC"));
            TestRow input = new TestRow(expected);

            database.insert(input);
            ZonedDateTime result = database
                .from(TestRow.class)
                .select(TestRow::utcDateTimeOpt)
                .where(TestRow::guid).isEqualTo(input.guid())
                .single();

            assertThat(result, is(expected));
        }
    }

    @Test
    public void localDatePartFuncs() {
        LocalDate date = RandomValues.randomLocalDate();
        Database database = testDatabase(dataSource, dialect);

        Tuple3<Integer,Integer,Integer> result = database
            .select(year(LiteralExpression.of(date)))
            .comma(month(ValueExpression.of(date)))
            .comma(day(LiteralExpression.of(date)))
            .single();

        assertThat(result.item1(), is(date.getYear()));
        assertThat(result.item2(), is(date.getMonthValue()));
        assertThat(result.item3(), is(date.getDayOfMonth()));
    }

    @Test
    public void localDateTimePartFuncs() {
        LocalDateTime dateTime = RandomValues.randomLocalDateTime();
        Database database = testDatabase(dataSource, dialect);

        Tuple6<Integer,Integer,Integer,Integer,Integer,Integer> result = database
            .select(year(LiteralExpression.of(dateTime)))
            .comma(month(ValueExpression.of(dateTime)))
            .comma(day(LiteralExpression.of(dateTime)))
            .comma(hour(ValueExpression.of(dateTime)))
            .comma(minute(LiteralExpression.of(dateTime)))
            .comma(second(ValueExpression.of(dateTime)))
            .single();

        assertThat(result.item1(), is(dateTime.getYear()));
        assertThat(result.item2(), is(dateTime.getMonthValue()));
        assertThat(result.item3(), is(dateTime.getDayOfMonth()));
        assertThat(result.item4(), is(dateTime.getHour()));
        assertThat(result.item5(), is(dateTime.getMinute()));
        assertThat(result.item6(), is(dateTime.getSecond()));
    }

    @Test
    @Parameters(value = {"America/Anchorage", "America/Sao_Paulo", "UTC", "Europe/London", "Africa/Johannesburg", "Pacific/Chatham"})
    public void zonedDateTimePartFuncs(String timeZone) {
        Database database = testDatabaseBuilder(dialect)
            .defaultSqlExecutor(JdbcSqlExecutor.of(dataSource))
            .databaseTimeZone(ZoneId.of(timeZone))
            .build();
        ZonedDateTime dateTime = RandomValues.randomZonedDateTime(database.databaseTimeZone());

        Tuple7<Integer,Integer,Integer,Integer,Integer,Integer,Integer> result = database
            .select(year(LiteralExpression.of(dateTime)))
            .comma(month(ValueExpression.of(dateTime)))
            .comma(day(LiteralExpression.of(dateTime)))
            .comma(hour(ValueExpression.of(dateTime)))
            .comma(hour(LiteralExpression.of(dateTime)))
            .comma(minute(LiteralExpression.of(dateTime)))
            .comma(second(ValueExpression.of(dateTime)))
            .single();

        assertThat(result.item1(), is(dateTime.getYear()));
        assertThat(result.item2(), is(dateTime.getMonthValue()));
        assertThat(result.item3(), is(dateTime.getDayOfMonth()));
        assertThat(result.item4(), is(dateTime.getHour()));
        assertThat(result.item5(), is(dateTime.getHour()));
        assertThat(result.item6(), is(dateTime.getMinute()));
        assertThat(result.item7(), is(dateTime.getSecond()));
    }

    @Test
    public void unionTest() {
        Database database = testDatabase(dataSource, dialect);

        List<Integer> result = database.select(literal(3))
            .union(database.select(literal(1)))
            .union(database.select(literal(1))
                .where(literal(4)).isEqualTo(literal(5)))
            .union(database.select(literal(2)))
            .unionAll(database.select(literal(2)))
            .orderBy(1)
            .list();

        assertThat(result, hasSize(4));
        assertThat(result.get(0), is(1));
        assertThat(result.get(1), is(2));
        assertThat(result.get(2), is(2));
        assertThat(result.get(3), is(3));
    }

    @Test
    public void fetchFirst() {
        Database database = testDatabase(dataSource, dialect);
        Tuple2<Long,Long> inserted = insertSalespeople(database, 10);

        List<SalespersonRow> fullList = database.from(SalespersonRow.class)
            .where(SalespersonRow::salespersonId).isBetween(inserted.item1()).and(inserted.item2())
            .list();
        List<SalespersonRow> reducedList = database.from(SalespersonRow.class)
            .where(SalespersonRow::salespersonId).isBetween(inserted.item1()).and(inserted.item2())
            .fetchFirst(5)
            .list();

        assertThat(fullList, hasSize(10));
        assertThat(reducedList, hasSize(5));
    }

    @Test
    public void commonTableExpression() {
        if (dialect instanceof H2Dialect) {
            throw new AssumptionViolatedException("H2 is buggy");
        }
        Database database = testDatabase(dataSource, dialect);
        Tuple2<Long,Long> inserted = insertSalespeople(database, 10);

        CommonTableExpression<SalespersonRow> first5 = database.with("first5")
            .as(
                database.from(SalespersonRow.class)
                    .where(SalespersonRow::salespersonId).isBetween(inserted.item1()).and(inserted.item2())
                    .orderBy(SalespersonRow::salespersonId)
                    .fetchFirst(5)
            );
        CommonTableExpression<SalespersonRow> last2 = database.with("last2")
            .as(
                database.from(first5, "f5")
                    .orderBy(SalespersonRow::salespersonId, Order.DESC)
                    .fetchFirst(2)
            );
        List<SalespersonRow> lastTwo = database.from(last2, "l2").list();

        assertThat(lastTwo, hasSize(2));
        assertThat(lastTwo.get(0).salespersonId(), is(inserted.item1() + 4));
        assertThat(lastTwo.get(1).salespersonId(), is(inserted.item1() + 3));
    }

    public <T> Object[] castExpressionTestCase(TypedExpression<T> castExpr, T expectedResult) {
        return new Object[]{castExpr, expectedResult};
    }

    public Object[][] parametersForCastExpression() {
        return new Object[][]{
            castExpressionTestCase(cast(134).asChar(3), "134"),
            castExpressionTestCase(cast(126).asTinyInteger(), (byte) 126),
            castExpressionTestCase(cast(32767L).asSmallInteger(), (short) 32767),
            castExpressionTestCase(cast(123456L).asInteger(), 123456),
            castExpressionTestCase(cast("789101112").asBigInteger(), 789101112L),
            castExpressionTestCase(cast(literal(LocalDateTime.of(2017, 1, 3, 12, 0, 1))).asDate(), LocalDate.of(2017, 1, 3)),
            castExpressionTestCase(cast(literal(LocalDate.of(2017, 1, 3))).asTimestamp(), LocalDateTime.of(2017, 1, 3, 0, 0, 0)),
            castExpressionTestCase(cast(literal(LocalDate.of(2017, 1, 3))).asTimestamp(6), LocalDateTime.of(2017, 1, 3, 0, 0, 0)),
            castExpressionTestCase(cast(LocalDate.of(2017, 1, 3)).asVarchar(20), "2017-01-03"),
            castExpressionTestCase(cast(literal(1234)).asDoublePrecision(), 1234.0),
            castExpressionTestCase(cast(literal(1234)).asReal(), 1234.0f),
        };
    }

    @Test
    @Parameters
    public <T> void castExpression(TypedExpression<T> castExpr, T expectedResult) {
        Database database = testDatabase(dataSource, dialect);

        T result = database.select(castExpr).single();

        assertThat(result, is(expectedResult));
    }

    @Test
    public void sequence() {
        Database database = testDatabase(dataSource, dialect);
        Sequence<Integer> widgetSeq = database.sequence("widget_seq");
        long salespersonId = newId();
        int seq1 = widgetSeq.single();
        SalespersonRow salesPerson = SalespersonRow.newBuilder()
            .salespersonId(salespersonId)
            .firstName("Terry")
            .surname("McCann")
            .numberOfSales(seq1)
            .build();
        database.insert(salesPerson);

        database.update(SalespersonRow.class)
            .set(SalespersonRow::numberOfSales).to(widgetSeq.nextVal())
            .where(SalespersonRow::salespersonId).isEqualTo(literal(salespersonId))
            .execute();

        int seq2 = database.from(SalespersonRow.class)
            .select(SalespersonRow::numberOfSales)
            .where(SalespersonRow::salespersonId).isEqualTo(literal(salespersonId))
            .single();

        assertThat(seq2, greaterThan(seq1));
    }
}