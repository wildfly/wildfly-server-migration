/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.migration.wfly.task.hostexclude;

import org.jboss.migration.core.jboss.HostExclude;
import org.jboss.migration.core.jboss.HostExcludes;
import org.jboss.migration.core.jboss.JBossExtensionNames;
import org.jboss.migration.wfly10.config.task.hostexclude.AddHostExcludes;

/**
 * @author emmartins
 */
public class WildFly37_0AddHostExcludes<S> extends AddHostExcludes<S> {

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

    public WildFly37_0AddHostExcludes() {
        super(HOST_EXCLUDES);
    }
}
