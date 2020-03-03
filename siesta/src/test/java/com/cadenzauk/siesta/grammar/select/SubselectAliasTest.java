/*
 * Copyright (c) 2020 Cadenza United Kingdom Limited
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

package com.cadenzauk.siesta.grammar.select;

import co.unruly.matchers.OptionalMatchers;
import co.unruly.matchers.StreamMatchers;
import com.cadenzauk.core.reflect.MethodInfo;
import com.cadenzauk.core.sql.RowMapper;
import com.cadenzauk.core.sql.RowMapperFactory;
import com.cadenzauk.siesta.Alias;
import com.cadenzauk.siesta.Database;
import com.cadenzauk.siesta.ProjectionColumn;
import com.cadenzauk.siesta.Scope;
import com.cadenzauk.siesta.catalog.ForeignKeyReference;
import com.cadenzauk.siesta.model.ManufacturerRow;
import com.cadenzauk.siesta.model.WidgetRow;
import com.google.common.reflect.TypeToken;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.cadenzauk.core.testutil.FluentAssert.calling;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubselectAliasTest {
    @Mock
    private Select<WidgetRow> select;

    @Mock
    private Database database;

    @Mock
    private MethodInfo<WidgetRow,Long> methodInfo;

    @Mock
    private MethodInfo<ManufacturerRow,Long> manufacturerMethodInfo;

    @Mock
    private Alias<ManufacturerRow> manufacturerAlias;

    @Mock
    private ProjectionColumn<Long> projectionColumn;

    @Mock
    private Scope scope;

    @Mock
    private ProjectionColumn<?> column1;

    @Mock
    private ProjectionColumn<?> column2;

    @Mock
    private RowMapperFactory<WidgetRow> rowMapperFactory;

    @Mock
    private RowMapperFactory<Long> columnRowMapperFactory;

    @Mock
    private RowMapper<WidgetRow> rowMapper;

    @Test
    void type() {
        TypeToken<WidgetRow> expected = TypeToken.of(WidgetRow.class);
        when(select.type()).thenReturn(expected);
        SubselectAlias<WidgetRow> sut = new SubselectAlias<>(select, "foo");

        TypeToken<WidgetRow> result = sut.type();

        assertThat(result, sameInstance(expected));
    }

    @Test
    void database() {
        when(select.database()).thenReturn(database);
        SubselectAlias<WidgetRow> sut = new SubselectAlias<>(select, "bar");

        Database result = sut.database();

        assertThat(result, sameInstance(database));
    }

    @Test
    void findColumnDelegatesToSelect() {
        when(select.findColumn(methodInfo)).thenReturn(Optional.of(projectionColumn));
        SubselectAlias<WidgetRow> sut = new SubselectAlias<>(select, "ernie");

        Optional<ProjectionColumn<Long>> result = sut.findColumn(methodInfo);

        assertThat(result, is(Optional.of(projectionColumn)));
    }

    @Test
    void foreignKeyIsAlwaysEmpty() {
        SubselectAlias<WidgetRow> sut = new SubselectAlias<>(select, "ernie");

        Optional<ForeignKeyReference<WidgetRow,ManufacturerRow>> result = sut.foreignKey(manufacturerAlias, Optional.empty());

        assertThat(result, OptionalMatchers.empty());
    }

    @Test
    void projectionColumnsHaveTheRightColumnSql() {
        SubselectAlias<WidgetRow> sut = new SubselectAlias<>(select, "band");
        when(select.projectionColumns(scope)).thenReturn(Stream.of(column1, column2));
        when(column1.label()).thenReturn("axl");
        when(column2.label()).thenReturn("slash");

        Stream<ProjectionColumn<?>> result = sut.projectionColumns(scope);

        assertThat(result.map(ProjectionColumn::columnSql), StreamMatchers.contains("axl", "slash"));
    }

    @Test
    void projectionColumnsHaveTheRightLabels() {
        SubselectAlias<WidgetRow> sut = new SubselectAlias<>(select, "band");
        when(select.projectionColumns(scope)).thenReturn(Stream.of(column1, column2));
        when(column1.label()).thenReturn("duff");
        when(column2.label()).thenReturn("izzy");

        Stream<ProjectionColumn<?>> result = sut.projectionColumns(scope);

        assertThat(result.map(ProjectionColumn::label), StreamMatchers.contains("band_duff", "band_izzy"));
    }

    @Test
    void argsDelegatesToSelect() {
        SubselectAlias<WidgetRow> sut = new SubselectAlias<>(select, "triple");
        when(select.args(scope)).thenReturn(Stream.of('a', 2, 3.0));

        Stream<Object> result = sut.args(scope);

        assertThat(result, StreamMatchers.contains('a', 2, 3.0));
    }

    @Test
    void isDualIsAlwaysFalse() {
        SubselectAlias<WidgetRow> sut = new SubselectAlias<>(select, "triple");

        boolean result = sut.isDual();

        assertThat(result, is(false));
    }

    @Test
    void withoutAliasIsSqlFromSelect() {
        SubselectAlias<WidgetRow> sut = new SubselectAlias<>(select, "barney");
        when(select.sql()).thenReturn("select * from foo where bar = baz");

        String result = sut.withoutAlias();

        assertThat(result, is("select * from foo where bar = baz"));
    }

    @Test
    void inFromClauseSql() {
        SubselectAlias<WidgetRow> sut = new SubselectAlias<>(select, "barney");
        when(select.sql()).thenReturn("select * from foo where bar = baz");

        String result = sut.inFromClauseSql();

        assertThat(result, is("(select * from foo where bar = baz) barney"));
    }

    @Test
    void columnSql() {
        SubselectAlias<WidgetRow> sut = new SubselectAlias<>(select, "barney");
        when(select.findColumn(methodInfo)).thenReturn(Optional.of(projectionColumn));
        when(projectionColumn.label()).thenReturn("rubble");

        String result = sut.columnSql(methodInfo);

        assertThat(result, is("barney.rubble"));
    }

    @Test
    void columnSqlWithLabelGivenEmptyLabel() {
        SubselectAlias<WidgetRow> sut = new SubselectAlias<>(select, "fred");
        when(select.findColumn(methodInfo)).thenReturn(Optional.of(projectionColumn));
        when(projectionColumn.label()).thenReturn("flintstone");

        String result = sut.columnSqlWithLabel(methodInfo, Optional.empty());

        assertThat(result, is("fred.flintstone fred_flintstone"));
    }

    @Test
    void columnSqlWithLabelGivenNonEmptyLabel() {
        SubselectAlias<WidgetRow> sut = new SubselectAlias<>(select, "fred");
        when(select.findColumn(methodInfo)).thenReturn(Optional.of(projectionColumn));
        when(projectionColumn.label()).thenReturn("flintstone");

        String result = sut.columnSqlWithLabel(methodInfo, Optional.of("caveman"));

        assertThat(result, is("fred.flintstone caveman"));
    }

    @Test
    void inSelectClauseSqlGivenScope() {
        SubselectAlias<WidgetRow> sut = new SubselectAlias<>(select, "t");
        when(select.projectionColumns(scope)).thenReturn(Stream.of(column1, column2));
        when(column1.label()).thenReturn("foo_col1");
        when(column2.label()).thenReturn("foo_col2");

        String result = sut.inSelectClauseSql(scope);

        assertThat(result, is("t.foo_col1 t_foo_col1, t.foo_col2 t_foo_col2"));
    }

    @Test
    void inSelectClauseSqlGivenColumnName() {
        SubselectAlias<WidgetRow> sut = new SubselectAlias<>(select, "john");

        String result = sut.inSelectClauseSql("doe");

        assertThat(result, is("john.doe"));
    }

    @Test
    void inSelectClauseSqlGivenColumnNameAndEmptyLabel() {
        SubselectAlias<WidgetRow> sut = new SubselectAlias<>(select, "john");

        String result = sut.inSelectClauseSql("doe", Optional.empty());

        assertThat(result, is("john.doe john_doe"));
    }

    @Test
    void inSelectClauseSqlGivenColumnNameAndNonEmptyLabel() {
        SubselectAlias<WidgetRow> sut = new SubselectAlias<>(select, "john");

        String result = sut.inSelectClauseSql("doe", Optional.of("anon"));

        assertThat(result, is("john.doe anon"));
    }

    @Test
    void columnLabelPrefixIsAliasName() {
        SubselectAlias<WidgetRow> sut = new SubselectAlias<>(select, "foo");

        String result = sut.columnLabelPrefix();

        assertThat(result, is("foo"));
    }

    @Test
    void aliasName() {
        SubselectAlias<WidgetRow> sut = new SubselectAlias<>(select, "bar");

        Optional<String> result = sut.aliasName();

        assertThat(result, is(Optional.of("bar")));
    }

    @Test
    void rowMapperFactoryDelegatesToSelect() {
        SubselectAlias<WidgetRow> sut = new SubselectAlias<>(select, "s");
        when(select.rowMapperFactory()).thenReturn(rowMapperFactory);
        when(rowMapperFactory.rowMapper(Optional.of("bruce"))).thenReturn(rowMapper);

        RowMapper<WidgetRow> result = sut.rowMapperFactory().rowMapper(Optional.of("bruce"));

        verify(select, times(1)).rowMapperFactory();
        assertThat(result, sameInstance(rowMapper));
    }

    @Test
    void rowMapperFactoryForReturnsRowMapperFactoryThatUsesLabelIfGiven() {
        when(select.findColumn(methodInfo)).thenReturn(Optional.of(projectionColumn));
        when(projectionColumn.rowMapperFactory()).thenReturn(columnRowMapperFactory);
        SubselectAlias<WidgetRow> sut = new SubselectAlias<>(select, "s");

        RowMapperFactory<Long> result = sut.rowMapperFactoryFor(methodInfo, Optional.of("burt"));

        verifyZeroInteractions(columnRowMapperFactory);
        result.rowMapper(Optional.of("foo"));
        verify(columnRowMapperFactory).rowMapper(Optional.of("foo"));
    }

    @Test
    void rowMapperFactoryForReturnsRowMapperFactoryThatUsesDefaultLabelIfNoLabelGiven() {
        when(select.findColumn(methodInfo)).thenReturn(Optional.of(projectionColumn));
        when(projectionColumn.rowMapperFactory()).thenReturn(columnRowMapperFactory);
        SubselectAlias<WidgetRow> sut = new SubselectAlias<>(select, "s");

        RowMapperFactory<Long> result = sut.rowMapperFactoryFor(methodInfo, Optional.of("burt"));

        verifyZeroInteractions(columnRowMapperFactory);
        result.rowMapper(Optional.empty());
        verify(columnRowMapperFactory).rowMapper(Optional.of("burt"));
    }

    @Test
    void rowMapperFactoryForReturnsRowMapperFactoryThatUsesCalculatedLabelIfNoLabelOrDefaultGiven() {
        when(select.findColumn(methodInfo)).thenReturn(Optional.of(projectionColumn));
        when(projectionColumn.rowMapperFactory()).thenReturn(columnRowMapperFactory);
        when(projectionColumn.label()).thenReturn("col");
        SubselectAlias<WidgetRow> sut = new SubselectAlias<>(select, "s");

        RowMapperFactory<Long> result = sut.rowMapperFactoryFor(methodInfo, Optional.empty());

        verifyZeroInteractions(columnRowMapperFactory);
        result.rowMapper(Optional.empty());
        verify(columnRowMapperFactory).rowMapper(Optional.of("s_col"));
    }

    @Test
    void asMethodInfoFromSameClassWithRightNameReturnsAlias()  {
        when(select.projectionIncludes(methodInfo)).thenReturn(true);
        SubselectAlias<WidgetRow> sut = new SubselectAlias<>(select, "wilma");

        List<Alias<?>> result = sut.as(methodInfo, Optional.of("wilma")).collect(toList());

        assertThat(result, containsInAnyOrder(sut));
    }

    @Test
    void asMethodInfoForDifferentClassWithWrongNameThrowsException()  {
        when(select.projectionIncludes(manufacturerMethodInfo)).thenReturn(false);
        when(manufacturerMethodInfo.referringClass()).thenReturn(ManufacturerRow.class);
        when(select.type()).thenReturn(TypeToken.of(WidgetRow.class));
        SubselectAlias<WidgetRow> sut = new SubselectAlias<>(select, "wilma");

        calling(() -> sut.as(manufacturerMethodInfo, Optional.of("wilma")))
            .shouldThrow(IllegalArgumentException.class)
            .withMessage(is("Alias wilma is an alias for " +
                "com.cadenzauk.siesta.model.WidgetRow and not " +
                "class com.cadenzauk.siesta.model.ManufacturerRow"));
    }

    @Test
    void asMethodInfoForRightClassWithDifferentNameReturnsEmpty()  {
        SubselectAlias<WidgetRow> sut = new SubselectAlias<>(select, "wilma");

        List<Alias<?>> result = sut.as(methodInfo, Optional.of("dino")).collect(toList());

        assertThat(result, Matchers.empty());
    }

    @Test
    void asMethodInfoForDifferentClassWithDifferentNameReturnsEmpty()  {
        SubselectAlias<WidgetRow> sut = new SubselectAlias<>(select, "dino");

        List<Alias<?>> result = sut.as(manufacturerMethodInfo, Optional.of("burt")).collect(toList());

        assertThat(result, Matchers.empty());
    }

    @Test
    void asMethodInfoWithWrongClassReturnsEmpty()  {
        when(select.projectionIncludes(methodInfo)).thenReturn(false);
        SubselectAlias<WidgetRow> sut = new SubselectAlias<>(select, "dino");

        List<Alias<?>> result = sut.as(methodInfo, Optional.empty()).collect(toList());

        assertThat(result, Matchers.empty());
    }

    @Test
    void asMethodInfoWithSameClassReturnsAlias()  {
        when(select.projectionIncludes(methodInfo)).thenReturn(true);
        SubselectAlias<WidgetRow> sut = new SubselectAlias<>(select, "wilma");

        List<Alias<?>> result = sut.as(methodInfo, Optional.empty()).collect(toList());

        assertThat(result, containsInAnyOrder(sut));
    }
}