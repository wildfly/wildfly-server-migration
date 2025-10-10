package org.jboss.migration.wfly12.task.subsystem.infinispan;

import org.jboss.migration.wfly10.config.task.subsystem.infinispan.FixCacheModuleName;

/**
 * @author emmartins
 */
public class WildFly12_0FixHibernateCacheModuleName<S> extends FixCacheModuleName<S> {
    public WildFly12_0FixHibernateCacheModuleName() {
        super(CacheType.HIBERNATE,"org.infinispan.hibernate-cache");
    }
}
