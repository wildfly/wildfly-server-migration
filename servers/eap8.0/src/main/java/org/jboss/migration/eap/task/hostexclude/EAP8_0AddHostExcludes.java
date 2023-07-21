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
package org.jboss.migration.eap.task.hostexclude;

import org.jboss.migration.core.jboss.HostExclude;
import org.jboss.migration.core.jboss.HostExcludes;
import org.jboss.migration.core.jboss.JBossExtensionNames;
import org.jboss.migration.wfly10.config.task.hostexclude.AddHostExcludes;

/**
 * @author emmartins
 */
public class EAP8_0AddHostExcludes<S> extends AddHostExcludes<S> {

    private static final HostExcludes HOST_EXCLUDES = HostExcludes.builder()
            .hostExclude(HostExclude.builder()
                    .name("EAP74")
                    .release("EAP7.4")
                    .excludedExtension(JBossExtensionNames.CLUSTERING_EJB)
                    .excludedExtension(JBossExtensionNames.ELYTRON_OIDC_CLIENT)
            )
            .build();

    public EAP8_0AddHostExcludes() {
        super(HOST_EXCLUDES);
    }
}
