/*
 * Copyright 2024 Red Hat, Inc.
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
package org.jboss.migration.wfly.task.hostexclude;

import org.jboss.migration.core.jboss.HostExclude;
import org.jboss.migration.core.jboss.HostExcludes;
import org.jboss.migration.core.jboss.JBossExtensionNames;
import org.jboss.migration.wfly10.config.task.hostexclude.AddHostExcludes;

/**
 * @author emmartins
 */
public class WildFly35_0AddHostExcludes<S> extends AddHostExcludes<S> {

    private static final HostExcludes HOST_EXCLUDES = HostExcludes.builder()
            .hostExclude(HostExclude.builder()
                    .name("WildFly23.0")
                    .release("WildFly23.0")
                    .excludedExtension(JBossExtensionNames.CLUSTERING_EJB)
                    .excludedExtension(JBossExtensionNames.ELYTRON_OIDC_CLIENT)
                    .excludedExtension(JBossExtensionNames.JAKARTA_DATA)
                    .excludedExtension(JBossExtensionNames.OPENTELEMETRY)
                    .excludedExtension(JBossExtensionNames.MICROMETER)
                    .excludedExtension(JBossExtensionNames.MICROPROFILE_LRA_PARTICIPANT)
                    .excludedExtension(JBossExtensionNames.MICROPROFILE_LRA_COORDINATOR)
                    .excludedExtension(JBossExtensionNames.MICROPROFILE_TELEMETRY)
                    .excludedExtension(JBossExtensionNames.MVC_KRAZO)
            )
            .hostExclude(HostExclude.builder()
                    .name("WildFly24.0")
                    .release("WildFly24.0")
                    .excludedExtension(JBossExtensionNames.CLUSTERING_EJB)
                    .excludedExtension(JBossExtensionNames.ELYTRON_OIDC_CLIENT)
                    .excludedExtension(JBossExtensionNames.JAKARTA_DATA)
                    .excludedExtension(JBossExtensionNames.OPENTELEMETRY)
                    .excludedExtension(JBossExtensionNames.MICROMETER)
                    .excludedExtension(JBossExtensionNames.MICROPROFILE_LRA_PARTICIPANT)
                    .excludedExtension(JBossExtensionNames.MICROPROFILE_LRA_COORDINATOR)
                    .excludedExtension(JBossExtensionNames.MICROPROFILE_TELEMETRY)
                    .excludedExtension(JBossExtensionNames.MVC_KRAZO)
            )
            .hostExclude(HostExclude.builder()
                    .name("WildFly25.0")
                    .release("WildFly25.0")
                    .excludedExtension(JBossExtensionNames.CLUSTERING_EJB)
                    .excludedExtension(JBossExtensionNames.JAKARTA_DATA)
                    .excludedExtension(JBossExtensionNames.MICROMETER)
                    .excludedExtension(JBossExtensionNames.MICROPROFILE_LRA_PARTICIPANT)
                    .excludedExtension(JBossExtensionNames.MICROPROFILE_LRA_COORDINATOR)
                    .excludedExtension(JBossExtensionNames.MICROPROFILE_TELEMETRY)
                    .excludedExtension(JBossExtensionNames.MVC_KRAZO)
            )
            .hostExclude(HostExclude.builder()
                    .name("WildFly26.0")
                    .release("WildFly26.0")
                    .excludedExtension(JBossExtensionNames.CLUSTERING_EJB)
                    .excludedExtension(JBossExtensionNames.JAKARTA_DATA)
                    .excludedExtension(JBossExtensionNames.MICROMETER)
                    .excludedExtension(JBossExtensionNames.MICROPROFILE_LRA_PARTICIPANT)
                    .excludedExtension(JBossExtensionNames.MICROPROFILE_LRA_COORDINATOR)
                    .excludedExtension(JBossExtensionNames.MICROPROFILE_TELEMETRY)
                    .excludedExtension(JBossExtensionNames.MVC_KRAZO)
            )
            .hostExclude(HostExclude.builder()
                    .name("WildFly27.0")
                    .release("WildFly27.0")
                    .excludedExtension(JBossExtensionNames.JAKARTA_DATA)
                    .excludedExtension(JBossExtensionNames.MICROMETER)
                    .excludedExtension(JBossExtensionNames.MICROPROFILE_LRA_PARTICIPANT)
                    .excludedExtension(JBossExtensionNames.MICROPROFILE_LRA_COORDINATOR)
                    .excludedExtension(JBossExtensionNames.MICROPROFILE_TELEMETRY)
                    .excludedExtension(JBossExtensionNames.MVC_KRAZO)
            )
            .hostExclude(HostExclude.builder()
                    .name("WildFly28.0")
                    .release("WildFly28.0")
                    .excludedExtension(JBossExtensionNames.JAKARTA_DATA)
                    .excludedExtension(JBossExtensionNames.MVC_KRAZO)
            )
            .hostExclude(HostExclude.builder()
                    .name("WildFly29.0")
                    .release("WildFly29.0")
                    .excludedExtension(JBossExtensionNames.JAKARTA_DATA)
                    .excludedExtension(JBossExtensionNames.MVC_KRAZO)
            )
            .hostExclude(HostExclude.builder()
                    .name("WildFly30.0")
                    .release("WildFly30.0")
                    .excludedExtension(JBossExtensionNames.JAKARTA_DATA)
                    .excludedExtension(JBossExtensionNames.MVC_KRAZO)
            )
            .hostExclude(HostExclude.builder()
                    .name("WildFly31.0")
                    .release("WildFly31.0")
                    .excludedExtension(JBossExtensionNames.JAKARTA_DATA)
                    .excludedExtension(JBossExtensionNames.MVC_KRAZO)
            )
            .hostExclude(HostExclude.builder()
                    .name("WildFly32.0")
                    .release("WildFly32.0")
                    .excludedExtension(JBossExtensionNames.JAKARTA_DATA)
            )
            .hostExclude(HostExclude.builder()
                    .name("WildFly33.0")
                    .release("WildFly33.0")
                    .excludedExtension(JBossExtensionNames.JAKARTA_DATA)
            )
            .hostExclude(HostExclude.builder()
                    .name("WildFly34.0")
                    .release("WildFly34.0")
                    .excludedExtension(JBossExtensionNames.JAKARTA_DATA)
            )
            .build();

    public WildFly35_0AddHostExcludes() {
        super(HOST_EXCLUDES);
    }
}
