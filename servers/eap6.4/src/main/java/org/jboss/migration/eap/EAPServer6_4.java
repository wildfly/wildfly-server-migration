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
package org.jboss.migration.eap;

import org.jboss.migration.core.ProductInfo;
import org.jboss.migration.core.env.MigrationEnvironment;
import org.jboss.migration.core.jboss.Extensions;
import org.jboss.migration.core.jboss.JBossExtensions;
import org.jboss.migration.core.jboss.JBossServer;

import java.nio.file.Path;

/**
 * The EAP 6.4 {@link org.jboss.migration.core.Server}
 * @author emmartins
 */
public class EAPServer6_4 extends JBossServer<EAPServer6_4> {

    public static final Extensions EXTENSIONS = Extensions.builder()
            .extension(JBossExtensions.CMP)
            .extension(JBossExtensions.CONFIGADMIN)
            .extension(JBossExtensions.CONNECTOR)
            .extension(JBossExtensions.DEPLOYMENT_SCANNER)
            .extension(JBossExtensions.EE)
            .extension(JBossExtensions.EJB3)
            .extension(JBossExtensions.INFINISPAN)
            .extension(JBossExtensions.JACORB)
            .extension(JBossExtensions.JAXR)
            .extension(JBossExtensions.JAXRS)
            .extension(JBossExtensions.JDR)
            .extension(JBossExtensions.JGROUPS)
            .extension(JBossExtensions.JMX)
            .extension(JBossExtensions.JPA)
            .extension(JBossExtensions.JSF)
            .extension(JBossExtensions.JSR77)
            .extension(JBossExtensions.LOGGING)
            .extension(JBossExtensions.MAIL)
            .extension(JBossExtensions.MESSAGING)
            .extension(JBossExtensions.MODCLUSTER)
            .extension(JBossExtensions.NAMING)
            .extension(JBossExtensions.OSGI)
            .extension(JBossExtensions.POJO)
            .extension(JBossExtensions.REMOTING)
            .extension(JBossExtensions.SAR)
            .extension(JBossExtensions.SECURITY)
            .extension(JBossExtensions.THREADS)
            .extension(JBossExtensions.TRANSACTIONS)
            .extension(JBossExtensions.WEB)
            .extension(JBossExtensions.WEBSERVICES)
            .extension(JBossExtensions.WELD)
            .build();

    public EAPServer6_4(String migrationName, ProductInfo productInfo, Path baseDir, MigrationEnvironment migrationEnvironment) {
        super(migrationName, productInfo, baseDir, migrationEnvironment);
    }
}
