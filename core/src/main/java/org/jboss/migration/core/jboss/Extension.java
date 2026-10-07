/*
 * Copyright 2016 Red Hat, Inc.
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
package org.jboss.migration.core.jboss;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

/**
 * @author emmartins
 */
public class Extension {

    private final String module;
    private final boolean legacy;
    protected final Map<String, Subsystem> subsystems;

    protected Extension(Builder builder) {
        this.module = builder.module;
        if (module == null) {
            throw new IllegalArgumentException();
        }
        this.legacy = builder.legacy;
        final Stream<Subsystem.Builder> subsystemBuilderStream = builder.subsystems.stream();
        subsystems = Collections.unmodifiableMap(subsystemBuilderStream
                .map(subsystemBuilder -> subsystemBuilder.extension(this).build())
                .collect(toMap(Subsystem::getName, Function.identity())));
    }

    public String getModule() {
        return module;
    }

    public boolean isLegacy() {
        return legacy;
    }

    public Collection<Subsystem> getSubsystems() {
        return subsystems.values();
    }

    public Set<String> getSubsystemNames() {
        return subsystems.keySet();
    }

    public Subsystem getSubsystem(String subsystemName) {
        return subsystems.get(subsystemName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Extension that = (Extension) o;
        return module.equals(that.module);
    }

    @Override
    public int hashCode() {
        return module.hashCode();
    }

    @Override
    public String toString() {
        return module;
    }

    public abstract static class Builder<T extends Builder<T>> {

        private final List<Subsystem.Builder> subsystems = new ArrayList<>();
        private String module;
        private boolean legacy;

        protected abstract T getThis();

        public T module(String module) {
            this.module = module;
            return getThis();
        }

        public T legacy(boolean legacy) {
            this.legacy = legacy;
            return getThis();
        }

        public T subsystem(String subsystemName) {
            return subsystem(Subsystem.builder().name(subsystemName));
        }

        public T subsystem(Subsystem.Builder subsystemBuilder) {
            this.subsystems.add(subsystemBuilder);
            return getThis();
        }

        public Extension build() {
            return new Extension(this);
        }
    }

    private static class DefaultBuilder extends Builder<DefaultBuilder> {
        @Override
        protected DefaultBuilder getThis() {
            return this;
        }
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }
}
