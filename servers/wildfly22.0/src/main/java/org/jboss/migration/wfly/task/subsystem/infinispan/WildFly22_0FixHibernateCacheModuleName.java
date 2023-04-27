package org.jboss.migration.wfly.task.subsystem.infinispan;

import org.jboss.migration.wfly10.config.task.subsystem.infinispan.FixHibernateCacheModuleName;

/**
 * @author istudens
 */
public class WildFly22_0FixHibernateCacheModuleName<S> extends FixHibernateCacheModuleName<S> {
    public WildFly22_0FixHibernateCacheModuleName() {
        super("org.infinispan.hibernate-cache", "modules");
    }
}
