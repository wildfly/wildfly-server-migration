/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.migration.core.env;

import org.jboss.migration.core.ServerMigrationFailureException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class AbstractEnvironmentTest<T extends Environment> {
    protected final T env;

    /**
     * The environment {@code env}, which will be tested, must:
     *
     * <ul>
     *     <li>have an empty property called {@code empty.property}</li>
     *     <li>have a property called {@code simple.property} that has a value of {@code foobar}</li>
     *     <li>have a property called {@code complex.property} that has a value of {@code foo,bar}</li>
     *     <li><b>not</b> have a property called {@code missing.property}</li>
     *     <li><b>not</b> have any property prefixed {@code test.property}</li>
     * </ul>
     */
    protected AbstractEnvironmentTest(T env) {
        this.env = env;
    }

    @Test
    public void getMissingProperty() {
        assertNull(env.getPropertyAsBoolean("missing.property"));
        assertNull(env.getPropertyAsBoolean("missing.property", null));
        assertNull(env.getPropertyAsString("missing.property"));
        assertNull(env.getPropertyAsString("missing.property", null));
        assertEquals("foobar", env.getPropertyAsString("missing.property", "foobar"));
        assertNull(env.getPropertyAsList("missing.property"));
        assertEquals(listOf("foobar"), env.getPropertyAsList("missing.property", listOf("foobar")));

        assertThrows(ServerMigrationFailureException.class,
                () -> env.requirePropertyAsBoolean("missing.property"));
        assertThrows(ServerMigrationFailureException.class,
                () -> env.requirePropertyAsString("missing.property", false));
        assertThrows(ServerMigrationFailureException.class,
                () -> env.requirePropertyAsList("missing.property", false));
    }

    @Test
    public void getEmptyProperty() {
        assertNull(env.getPropertyAsBoolean("empty.property"));
        assertNull(env.getPropertyAsBoolean("empty.property", null));
        assertEquals("", env.getPropertyAsString("empty.property"));
        assertEquals("", env.getPropertyAsString("empty.property", "foobar"));
        assertEquals(listOf(), env.getPropertyAsList("empty.property"));
        assertEquals(listOf(), env.getPropertyAsList("empty.property", listOf("foobar")));

        assertEquals("", env.requirePropertyAsString("empty.property", false));
        assertEquals(listOf(), env.requirePropertyAsList("empty.property", false));

        assertThrows(ServerMigrationFailureException.class,
                () -> env.requirePropertyAsBoolean("empty.property"));
        assertThrows(ServerMigrationFailureException.class,
                () -> env.requirePropertyAsString("empty.property", true));
        assertThrows(ServerMigrationFailureException.class,
                () -> env.requirePropertyAsList("empty.property", true));
    }

    @Test
    public void getSimpleProperty() {
        assertFalse(env.getPropertyAsBoolean("simple.property"));
        assertFalse(env.getPropertyAsBoolean("simple.property", null));
        assertEquals("foobar", env.getPropertyAsString("simple.property"));
        assertEquals("foobar", env.getPropertyAsString("simple.property", "quux"));
        assertEquals(listOf("foobar"), env.getPropertyAsList("simple.property"));
        assertEquals(listOf("foobar"), env.getPropertyAsList("simple.property", listOf("quux")));

        assertFalse(env.requirePropertyAsBoolean("simple.property"));
        assertEquals("foobar", env.requirePropertyAsString("simple.property", false));
        assertEquals("foobar", env.requirePropertyAsString("simple.property", true));
        assertEquals(listOf("foobar"), env.requirePropertyAsList("simple.property", false));
        assertEquals(listOf("foobar"), env.requirePropertyAsList("simple.property", true));
    }

    @Test
    public void getComplexProperty() {
        assertFalse(env.getPropertyAsBoolean("complex.property"));
        assertFalse(env.getPropertyAsBoolean("complex.property", null));
        assertEquals("foo,bar", env.getPropertyAsString("complex.property"));
        assertEquals("foo,bar", env.getPropertyAsString("complex.property", "quux"));
        assertEquals(listOf("foo", "bar"), env.getPropertyAsList("complex.property"));
        assertEquals(listOf("foo", "bar"), env.getPropertyAsList("complex.property", listOf("quux")));

        assertFalse(env.requirePropertyAsBoolean("complex.property"));
        assertEquals("foo,bar", env.requirePropertyAsString("complex.property", false));
        assertEquals("foo,bar", env.requirePropertyAsString("complex.property", true));
        assertEquals(listOf("foo", "bar"), env.requirePropertyAsList("complex.property", false));
        assertEquals(listOf("foo", "bar"), env.requirePropertyAsList("complex.property", true));
    }

    // relies on JUnit always creating new test instance for each @Test method
    @Test
    public void getWhatPropertiesWereRead() {
        assertTrue(env.getPropertyNamesReaded().isEmpty());

        env.getPropertyAsString("missing.property");

        assertTrue(env.getPropertyNamesReaded().isEmpty());

        env.getPropertyAsString("empty.property");

        assertTrue(env.getPropertyNamesReaded().containsAll(listOf("empty.property")));

        env.getPropertyAsString("simple.property");

        assertTrue(env.getPropertyNamesReaded().containsAll(listOf("empty.property", "simple.property")));

        env.getPropertyAsString("complex.property");

        assertTrue(env.getPropertyNamesReaded().containsAll(
                listOf("empty.property", "simple.property", "complex.property")));
    }

    @Test
    public void setProperty() {
        assertNull(env.getPropertyAsString("test.property"));

        String old = env.setProperty("test.property", "abcd");
        assertNull(old);
        assertEquals("abcd", env.getPropertyAsString("test.property"));

        old = env.setProperty("test.property", null);
        assertEquals("abcd", old);
        assertNull(env.getPropertyAsString("test.property"));
    }

    @Test
    public void setProperties() {
        assertNull(env.getPropertyAsString("test.property.1"));
        assertNull(env.getPropertyAsString("test.property.2"));

        Properties props = new Properties();
        env.setProperties(props);

        assertNull(env.getPropertyAsString("test.property.1"));
        assertNull(env.getPropertyAsString("test.property.2"));

        props = new Properties();
        props.setProperty("test.property.1", "abcd");
        props.setProperty("test.property.2", "xyzw");
        env.setProperties(props);

        assertEquals("abcd", env.getPropertyAsString("test.property.1"));
        assertEquals("xyzw", env.getPropertyAsString("test.property.2"));

        props = new Properties();
        env.setProperties(props);

        assertEquals("abcd", env.getPropertyAsString("test.property.1"));
        assertEquals("xyzw", env.getPropertyAsString("test.property.2"));
    }

    private static List<String> listOf(String... strings) {
        return Arrays.asList(strings);
    }
}
