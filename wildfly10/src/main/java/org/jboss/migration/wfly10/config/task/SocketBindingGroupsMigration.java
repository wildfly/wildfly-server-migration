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

package org.jboss.migration.wfly10.config.task;

import org.jboss.migration.core.ServerMigrationTaskContext;
import org.jboss.migration.wfly10.config.management.SocketBindingGroupsManagement;

/**
 * Migration of socket binding groups.
 *  @author emmartins
 */
public class SocketBindingGroupsMigration<S> extends ResourcesMigration<S, SocketBindingGroupsManagement> {

    public static final String SOCKET_BINDING_GROUPS = "socket-binding-groups";

    protected SocketBindingGroupsMigration(Builder<S> builder) {
        super(builder);
    }

    public interface SubtaskFactory<S> extends ResourcesMigration.SubtaskFactory<S, SocketBindingGroupsManagement> {
    }

    public static class Builder<S> extends ResourcesMigration.Builder<Builder<S>, S, SocketBindingGroupsManagement> {
        public Builder() {
            super(SOCKET_BINDING_GROUPS);
            eventListener(new ResourcesMigration.EventListener() {
                @Override
                public void started(ServerMigrationTaskContext context) {
                    context.getLogger().infof("Socket binding groups migration starting...");
                }
                @Override
                public void done(ServerMigrationTaskContext context) {
                    context.getLogger().infof("Socket binding groups migration done.");
                }
            });
        }
        public Builder socketBindingGroupMigration(SocketBindingGroupMigration socketBindingGroupMigration) {
            return addSubtaskFactory(socketBindingGroupMigration);
        }
        @Override
        public SocketBindingGroupsMigration<S> build() {
            return new SocketBindingGroupsMigration(this);
        }
    }

    public static <S> SocketBindingGroupsMigration<S> from(SocketBindingGroupMigration<S> socketBindingGroupMigration) {
        return new Builder<S>().socketBindingGroupMigration(socketBindingGroupMigration).build();
    }
}