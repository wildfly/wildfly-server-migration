/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.wfly;

import org.jboss.migration.wfly10.WildFlyServerMigrationProvider10;

/**
 * The interface that WildFly 38.0 specific migration providers must implement. Such implementations are loaded through ServiceLoader framework, thus a service descriptor must be in classpath.
 * @author emmartins
 */
public interface WildFly38_0ServerMigrationProvider extends WildFlyServerMigrationProvider10 {
}
