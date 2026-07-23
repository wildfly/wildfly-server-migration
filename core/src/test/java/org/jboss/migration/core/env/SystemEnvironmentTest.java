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

import org.junit.jupiter.api.AfterAll;

public class SystemEnvironmentTest extends AbstractEnvironmentTest<SystemEnvironment> {
    public SystemEnvironmentTest() {
        super(createSystemEnvironment());
    }

    private static SystemEnvironment createSystemEnvironment() {
        System.setProperty("jboss.server.migration.empty.property", "");
        System.setProperty("jboss.server.migration.simple.property", "foobar");
        System.setProperty("jboss.server.migration.complex.property", "foo,bar");

        // this could use SystemEnvironment.INSTANCE and rely on lazy class loading,
        // but noone can guarantee that there won't be any test running before that causes
        // the class to load before this test
        return new SystemEnvironment();
    }

    @AfterAll
    public static void cleanSystemProperties() {
        System.clearProperty("jboss.server.migration.empty.property");
        System.clearProperty("jboss.server.migration.simple.property");
        System.clearProperty("jboss.server.migration.complex.property");
    }
}
