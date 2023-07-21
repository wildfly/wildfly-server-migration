/*
 * Copyright 2015 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.migration.wfly10.dist.full;

import org.jboss.migration.core.ProductInfo;
import org.jboss.migration.core.env.MigrationEnvironment;
import org.jboss.migration.core.jboss.JBossExtensions;
import org.jboss.migration.core.jboss.JBossServer;
import org.jboss.migration.wfly10.WildFlyServer10;

import java.nio.file.Path;

/**
 * @author emmartins
 */
public class WildFlyFullServer10_0 extends WildFlyServer10 {

    public static final JBossServer.Extensions EXTENSIONS = JBossServer.Extensions.builder()
            .extension(JBossExtensions.BATCH_JBERET)
            .extension(JBossExtensions.BEAN_VALIDATION)
            .extension(JBossExtensions.CONNECTOR)
            .extension(JBossExtensions.DEPLOYMENT_SCANNER)
            .extension(JBossExtensions.EE)
            .extension(JBossExtensions.EJB3)
            .extension(JBossExtensions.IIOP_OPENJDK)
            .extension(JBossExtensions.INFINISPAN)
            .extension(JBossExtensions.IO)
            .extension(JBossExtensions.JACORB)
            .extension(JBossExtensions.JAXRS)
            .extension(JBossExtensions.JDR)
            .extension(JBossExtensions.JGROUPS)
            .extension(JBossExtensions.JMX)
            .extension(JBossExtensions.JPA)
            .extension(JBossExtensions.JSF)
            .extension(JBossExtensions.JSR77)
            .extension(JBossExtensions.KEYCLOAK)
            .extension(JBossExtensions.LOGGING)
            .extension(JBossExtensions.MAIL)
            .extension(JBossExtensions.MESSAGING)
            .extension(JBossExtensions.MESSAGING_ACTIVEMQ)
            .extension(JBossExtensions.MODCLUSTER)
            .extension(JBossExtensions.NAMING)
            .extension(JBossExtensions.POJO)
            .extension(JBossExtensions.PICKETLINK)
            .extension(JBossExtensions.REMOTING)
            .extension(JBossExtensions.REQUEST_CONTROLLER)
            .extension(JBossExtensions.SAR)
            .extension(JBossExtensions.SECURITY)
            .extension(JBossExtensions.SECURITY_MANAGER)
            .extension(JBossExtensions.SINGLETON)
            .extension(JBossExtensions.TRANSACTIONS)
            .extension(JBossExtensions.UNDERTOW)
            .extension(JBossExtensions.WEB)
            .extension(JBossExtensions.WEBSERVICES)
            .extension(JBossExtensions.WELD)
            .build();

    public WildFlyFullServer10_0(String migrationName, ProductInfo productInfo, Path baseDir, MigrationEnvironment migrationEnvironment) {
        super(migrationName, productInfo, baseDir, migrationEnvironment, EXTENSIONS);
    }
}
