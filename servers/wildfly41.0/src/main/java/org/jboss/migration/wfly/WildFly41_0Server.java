/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.wfly;

/**
 * The WildFly 41.x {@link org.jboss.migration.core.Server}, which introduces Legacy distributions.
 * @author emmartins
 */
public interface WildFly41_0Server {

    /**
     * The type of WildFly 41.x server technology.
     */
    enum TechnologyType { LEGACY, STANDARD, PREVIEW }

    /**
     * The type of WildFly 41.x server distribution.
     */
    enum DistributionType { EE_DIST, DIST}

    /**
     *
     * @return the server's technology type
     */
    TechnologyType getTechnologyType();

    /**
     *
     * @return the server's distribution type
     */
    DistributionType getDistributionType();
}
