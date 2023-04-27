/*
 * Copyright 2022 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.migration.eap;

import org.jboss.migration.wfly10.WildFlyServerMigrationProvider10;

/**
 * The interface that JBoss EAP 8.0 migration providers must implement. Such implementations are loaded through ServiceLoader framework, thus a service descriptor must be in classpath.
 * @author emmartins
 */
public interface EAPServerMigrationProvider8_0 extends WildFlyServerMigrationProvider10 {
}
