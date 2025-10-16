package org.jboss.migration.wfly10.config.task.subsystem.infinispan;

/**
 * @author emmartins
 */
public class WildFly10_0FixHibernateCacheModuleName<S> extends FixCacheModuleName<S> {
    public WildFly10_0FixHibernateCacheModuleName() {
        super(CacheType.HIBERNATE, "org.hibernate.infinispan");
    }
}
