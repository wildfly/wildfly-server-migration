package org.jboss.migration.wfly.task.subsystem.infinispan;

import org.jboss.migration.wfly10.config.task.subsystem.infinispan.FixCacheModuleName;

/**
 * @author istudens
 */
public class WildFly22_0FixHibernateCacheModuleName<S> extends FixCacheModuleName<S> {
    public WildFly22_0FixHibernateCacheModuleName() {
        super(CacheType.HIBERNATE, "org.infinispan.hibernate-cache", "modules");
    }
}
