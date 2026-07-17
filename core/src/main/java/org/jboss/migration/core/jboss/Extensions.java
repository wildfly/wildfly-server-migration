/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.core.jboss;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class Extensions {

    private final Map<String, Extension> extensionMap;

    protected Extensions(Builder builder) {
        this.extensionMap = Collections.unmodifiableMap(builder.extensionMap);
    }

    public Collection<Extension> getExtensions() {
        return extensionMap.values();
    }

    public Set<String> getExtensionModuleNames() {
        return extensionMap.keySet();
    }

    public Extension getExtension(String moduleName) {
        return extensionMap.get(moduleName);
    }

    public abstract static class Builder<T extends Builder<T>> {

        private final Map<String, Extension> extensionMap = new HashMap<>();

        protected abstract T getThis();

        public T extension(Extension extension) {
            this.extensionMap.put(extension.getModule(), extension);
            return getThis();
        }

        public T extension(Extension.Builder extensionBuilder) {
            return extension(extensionBuilder.build());
        }

        public T extensions(Extensions extensions) {
            this.extensionMap.putAll(extensions.extensionMap);
            return getThis();
        }

        public T extensions(Collection<Extension> extensions) {
            for (Extension extension : extensions) {
                extension(extension);
            }
            return getThis();
        }

        public T extensionsExcept(Extensions extensions, String... moduleNamesToExclude) {
            for (Extension extension : extensions.getExtensions()) {
                boolean exclude = false;
                if (moduleNamesToExclude != null) {
                    for (String moduleName : moduleNamesToExclude) {
                        if (moduleName.equals(extension.getModule())) {
                            exclude = true;
                            break;
                        }
                    }
                }
                if (!exclude) {
                    this.extensionMap.put(extension.getModule(), extension);
                }
            }
            return getThis();
        }

        public Extensions build() {
            return new Extensions(this);
        }
    }

    private static class DefaultBuilder extends Builder {
        @Override
        protected DefaultBuilder getThis() {
            return this;
        }
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }
}
