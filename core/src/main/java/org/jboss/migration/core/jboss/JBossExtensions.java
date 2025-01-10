/*
 * Copyright 2017 Red Hat, Inc.
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

/**
 * Prebuilt extensions supported by multiple jboss servers.
 * @author emmartins
 */
public interface JBossExtensions {

    Extension BATCH = Extension.builder()
            .module(JBossExtensionNames.BATCH)
            .subsystem(JBossSubsystemNames.BATCH)
            .build();

    Extension BATCH_JBERET = Extension.builder()
            .module(JBossExtensionNames.BATCH_JBERET)
            .subsystem(JBossSubsystemNames.BATCH_JBERET)
            .build();

    Extension BEAN_VALIDATION = Extension.builder()
            .module(JBossExtensionNames.BEAN_VALIDATION)
            .subsystem(JBossSubsystemNames.BEAN_VALIDATION)
            .build();

    Extension CLUSTERING_EJB = Extension.builder()
            .module(JBossExtensionNames.CLUSTERING_EJB)
            .subsystem(JBossSubsystemNames.DISTRIBUTABLE_EJB)
            .build();

    Extension CLUSTERING_WEB = Extension.builder()
            .module(JBossExtensionNames.CLUSTERING_WEB)
            .subsystem(JBossSubsystemNames.DISTRIBUTABLE_WEB)
            .build();

    Extension CMP = Extension.builder()
            .module(JBossExtensionNames.CMP)
            .subsystem(JBossSubsystemNames.CMP)
            .build();

    Extension CONFIGADMIN = Extension.builder()
            .module(JBossExtensionNames.CONFIGADMIN)
            .subsystem(JBossSubsystemNames.CONFIGADMIN)
            .build();

    Extension CONNECTOR = Extension.builder()
            .module(JBossExtensionNames.CONNECTOR)
            .subsystem(JBossSubsystemNames.DATASOURCES)
            .subsystem(JBossSubsystemNames.JCA)
            .subsystem(JBossSubsystemNames.RESOURCE_ADAPTERS)
            .build();

    Extension CORE_MANAGEMENT = Extension.builder()
            .module(JBossExtensionNames.CORE_MANAGEMENT)
            .subsystem(JBossSubsystemNames.CORE_MANAGEMENT)
            .build();

    Extension DATASOURCES_AGROAL = Extension.builder()
            .module(JBossExtensionNames.DATASOURCES_AGROAL)
            .subsystem(Subsystem.builder().name(JBossSubsystemNames.DATASOURCES_AGROAL).namespaceWithoutVersion("urn:wildfly:"+JBossSubsystemNames.DATASOURCES_AGROAL))
            .build();

    Extension DEPLOYMENT_SCANNER = Extension.builder()
            .module(JBossExtensionNames.DEPLOYMENT_SCANNER)
            .subsystem(JBossSubsystemNames.DEPLOYMENT_SCANNER)
            .build();

    Extension DISCOVERY = Extension.builder()
            .module(JBossExtensionNames.DISCOVERY)
            .subsystem(JBossSubsystemNames.DISCOVERY)
            .build();

    Extension EE = Extension.builder()
            .module(JBossExtensionNames.EE)
            .subsystem(JBossSubsystemNames.EE)
            .build();

    Extension EE_SECURITY = Extension.builder()
            .module(JBossExtensionNames.EE_SECURITY)
            .subsystem(JBossSubsystemNames.EE_SECURITY)
            .build();

    Extension EJB3 = Extension.builder()
            .module(JBossExtensionNames.EJB3)
            .subsystem(JBossSubsystemNames.EJB3)
            .build();

    Extension ELYTRON = Extension.builder()
            .module(JBossExtensionNames.ELYTRON)
            .subsystem(Subsystem.builder().name(JBossSubsystemNames.ELYTRON).namespaceWithoutVersion("urn:wildfly:"+JBossSubsystemNames.ELYTRON))
            .build();

    Extension ELYTRON_JAAS_REALM = Extension.builder()
            .module(JBossExtensionNames.ELYTRON_JAAS_REALM)
            .build();

    Extension ELYTRON_OIDC_CLIENT = Extension.builder()
            .module(JBossExtensionNames.ELYTRON_OIDC_CLIENT)
            .subsystem(Subsystem.builder().name(JBossSubsystemNames.ELYTRON_OIDC_CLIENT).namespaceWithoutVersion("urn:wildfly:"+JBossSubsystemNames.ELYTRON_OIDC_CLIENT))
            .build();

    Extension HEALTH = Extension.builder()
            .module(JBossExtensionNames.HEALTH)
            .subsystem(Subsystem.builder().name(JBossSubsystemNames.HEALTH).namespaceWithoutVersion("urn:wildfly:"+JBossSubsystemNames.HEALTH))
            .build();

    Extension IIOP_OPENJDK = Extension.builder()
            .module(JBossExtensionNames.IIOP_OPENJDK)
            .subsystem(JBossSubsystemNames.IIOP_OPENJDK)
            .build();

    Extension INFINISPAN = Extension.builder()
            .module(JBossExtensionNames.INFINISPAN)
            .subsystem(JBossSubsystemNames.INFINISPAN)
            .build();

    Extension IO = Extension.builder()
            .module(JBossExtensionNames.IO)
            .subsystem(JBossSubsystemNames.IO)
            .build();

    Extension JACORB = Extension.builder()
            .module(JBossExtensionNames.JACORB)
            .subsystem(JBossSubsystemNames.JACORB)
            .build();

    Extension JAKARTA_DATA = Extension.builder()
            .module(JBossExtensionNames.JAKARTA_DATA)
            .subsystem(Subsystem.builder().name(JBossSubsystemNames.JAKARTA_DATA).namespaceWithoutVersion("urn:wildfly:"+JBossSubsystemNames.JAKARTA_DATA))
            .build();

    Extension JAXR = Extension.builder()
            .module(JBossExtensionNames.JAXR)
            .subsystem(JBossSubsystemNames.JAXR)
            .build();

    Extension JAXRS = Extension.builder()
            .module(JBossExtensionNames.JAXRS)
            .subsystem(JBossSubsystemNames.JAXRS)
            .build();

    Extension JDR = Extension.builder()
            .module(JBossExtensionNames.JDR)
            .subsystem(JBossSubsystemNames.JDR)
            .build();

    Extension JGROUPS = Extension.builder()
            .module(JBossExtensionNames.JGROUPS)
            .subsystem(JBossSubsystemNames.JGROUPS)
            .build();

    Extension JMX = Extension.builder()
            .module(JBossExtensionNames.JMX)
            .subsystem(JBossSubsystemNames.JMX)
            .build();

    Extension JPA = Extension.builder()
            .module(JBossExtensionNames.JPA)
            .subsystem(JBossSubsystemNames.JPA)
            .build();

    Extension JSF = Extension.builder()
            .module(JBossExtensionNames.JSF)
            .subsystem(JBossSubsystemNames.JSF)
            .build();

    Extension JSR77 =Extension.builder()
            .module(JBossExtensionNames.JSR77)
            .subsystem(JBossSubsystemNames.JSR77)
            .build();

    Extension KEYCLOAK = Extension.builder()
            .module(JBossExtensionNames.KEYCLOAK)
            .subsystem(JBossSubsystemNames.KEYCLOAK)
            .build();

    Extension LOGGING = Extension.builder()
            .module(JBossExtensionNames.LOGGING)
            .subsystem(JBossSubsystemNames.LOGGING)
            .build();

    Extension MAIL = Extension.builder()
            .module(JBossExtensionNames.MAIL)
            .subsystem(JBossSubsystemNames.MAIL)
            .build();

    Extension METRICS = Extension.builder()
            .module(JBossExtensionNames.METRICS)
            .subsystem(Subsystem.builder().name(JBossSubsystemNames.METRICS).namespaceWithoutVersion("urn:wildfly:"+JBossSubsystemNames.METRICS))
            .build();

    Extension MESSAGING = Extension.builder()
            .module(JBossExtensionNames.MESSAGING)
            .subsystem(JBossSubsystemNames.MESSAGING)
            .build();

    Extension MESSAGING_ACTIVEMQ = Extension.builder()
            .module(JBossExtensionNames.MESSAGING_ACTIVEMQ)
            .subsystem(JBossSubsystemNames.MESSAGING_ACTIVEMQ)
            .build();

    Extension MICROMETER = Extension.builder()
            .module(JBossExtensionNames.MICROMETER)
            .subsystem(Subsystem.builder().name(JBossSubsystemNames.MICROMETER).namespaceWithoutVersion("urn:wildfly:"+JBossSubsystemNames.MICROMETER))
            .build();

    Extension MICROPROFILE_CONFIG_SMALLRYE = Extension.builder()
            .module(JBossExtensionNames.MICROPROFILE_CONFIG_SMALLRYE)
            .subsystem(Subsystem.builder().name(JBossSubsystemNames.MICROPROFILE_CONFIG_SMALLRYE).namespaceWithoutVersion("urn:wildfly:"+JBossSubsystemNames.MICROPROFILE_CONFIG_SMALLRYE))
            .build();

    Extension MICROPROFILE_FAULT_TOLERANCE_SMALLRYE = Extension.builder()
            .module(JBossExtensionNames.MICROPROFILE_FAULT_TOLERANCE_SMALLRYE)
            .subsystem(Subsystem.builder().name(JBossSubsystemNames.MICROPROFILE_FAULT_TOLERANCE_SMALLRYE).namespaceWithoutVersion("urn:wildfly:"+JBossSubsystemNames.MICROPROFILE_FAULT_TOLERANCE_SMALLRYE))
            .build();

    Extension MICROPROFILE_HEALTH_SMALLRYE = Extension.builder()
            .module(JBossExtensionNames.MICROPROFILE_HEALTH_SMALLRYE)
            .subsystem(Subsystem.builder().name(JBossSubsystemNames.MICROPROFILE_HEALTH_SMALLRYE).namespaceWithoutVersion("urn:wildfly:"+JBossSubsystemNames.MICROPROFILE_HEALTH_SMALLRYE))
            .build();

    Extension MICROPROFILE_JWT_SMALLRYE = Extension.builder()
            .module(JBossExtensionNames.MICROPROFILE_JWT_SMALLRYE)
            .subsystem(Subsystem.builder().name(JBossSubsystemNames.MICROPROFILE_JWT_SMALLRYE).namespaceWithoutVersion("urn:wildfly:"+JBossSubsystemNames.MICROPROFILE_JWT_SMALLRYE))
            .build();

    Extension MICROPROFILE_LRA_COORDINATOR = Extension.builder()
            .module(JBossExtensionNames.MICROPROFILE_LRA_COORDINATOR)
            .subsystem(Subsystem.builder().name(JBossSubsystemNames.MICROPROFILE_LRA_COORDINATOR).namespaceWithoutVersion("urn:wildfly:"+JBossSubsystemNames.MICROPROFILE_LRA_COORDINATOR))
            .build();

    Extension MICROPROFILE_LRA_PARTICIPANT = Extension.builder()
            .module(JBossExtensionNames.MICROPROFILE_LRA_PARTICIPANT)
            .subsystem(Subsystem.builder().name(JBossSubsystemNames.MICROPROFILE_LRA_PARTICIPANT).namespaceWithoutVersion("urn:wildfly:"+JBossSubsystemNames.MICROPROFILE_LRA_PARTICIPANT))
            .build();

    Extension MICROPROFILE_METRICS_SMALLRYE = Extension.builder()
            .module(JBossExtensionNames.MICROPROFILE_METRICS_SMALLRYE)
            .subsystem(Subsystem.builder().name(JBossSubsystemNames.MICROPROFILE_METRICS_SMALLRYE).namespaceWithoutVersion("urn:wildfly:"+JBossSubsystemNames.MICROPROFILE_METRICS_SMALLRYE))
            .build();

    Extension MICROPROFILE_OPENAPI_SMALLRYE = Extension.builder()
            .module(JBossExtensionNames.MICROPROFILE_OPENAPI_SMALLRYE)
            .subsystem(Subsystem.builder().name(JBossSubsystemNames.MICROPROFILE_OPENAPI_SMALLRYE).namespaceWithoutVersion("urn:wildfly:"+JBossSubsystemNames.MICROPROFILE_OPENAPI_SMALLRYE))
            .build();

    Extension MICROPROFILE_OPENTRACING_SMALLRYE = Extension.builder()
            .module(JBossExtensionNames.MICROPROFILE_OPENTRACING_SMALLRYE)
            .subsystem(Subsystem.builder().name(JBossSubsystemNames.MICROPROFILE_OPENTRACING_SMALLRYE).namespaceWithoutVersion("urn:wildfly:"+JBossSubsystemNames.MICROPROFILE_OPENTRACING_SMALLRYE))
            .build();

    Extension MICROPROFILE_REACTIVE_MESSAGING_SMALLRYE = Extension.builder()
            .module(JBossExtensionNames.MICROPROFILE_REACTIVE_MESSAGING_SMALLRYE)
            .subsystem(Subsystem.builder().name(JBossSubsystemNames.MICROPROFILE_REACTIVE_MESSAGING_SMALLRYE).namespaceWithoutVersion("urn:wildfly:"+JBossSubsystemNames.MICROPROFILE_REACTIVE_MESSAGING_SMALLRYE))
            .build();

    Extension MICROPROFILE_REACTIVE_STREAMS_OPERATORS_SMALLRYE = Extension.builder()
            .module(JBossExtensionNames.MICROPROFILE_REACTIVE_STREAMS_OPERATORS_SMALLRYE)
            .subsystem(Subsystem.builder().name(JBossSubsystemNames.MICROPROFILE_REACTIVE_STREAMS_OPERATORS_SMALLRYE).namespaceWithoutVersion("urn:wildfly:"+JBossSubsystemNames.MICROPROFILE_REACTIVE_STREAMS_OPERATORS_SMALLRYE))
            .build();

    Extension MICROPROFILE_TELEMETRY = Extension.builder()
            .module(JBossExtensionNames.MICROPROFILE_TELEMETRY)
            .subsystem(Subsystem.builder().name(JBossSubsystemNames.MICROPROFILE_TELEMETRY).namespaceWithoutVersion("urn:wildfly:"+JBossSubsystemNames.MICROPROFILE_TELEMETRY))
            .build();

    Extension MODCLUSTER = Extension.builder()
            .module(JBossExtensionNames.MODCLUSTER)
            .subsystem(JBossSubsystemNames.MODCLUSTER)
            .build();

    Extension MVC_KRAZO = Extension.builder()
            .module(JBossExtensionNames.MVC_KRAZO)
            .subsystem(JBossSubsystemNames.MVC_KRAZO)
            .build();

    Extension NAMING = Extension.builder()
            .module(JBossExtensionNames.NAMING)
            .subsystem(JBossSubsystemNames.NAMING)
            .build();

    Extension OPENTELEMETRY = Extension.builder()
            .module(JBossExtensionNames.OPENTELEMETRY)
            .subsystem(Subsystem.builder().name(JBossSubsystemNames.OPENTELEMETRY).namespaceWithoutVersion("urn:wildfly:"+JBossSubsystemNames.OPENTELEMETRY))
            .build();

    Extension OSGI = Extension.builder()
            .module(JBossExtensionNames.OSGI)
            .subsystem(JBossSubsystemNames.OSGI)
            .build();

    Extension PICKETLINK = Extension.builder()
            .module(JBossExtensionNames.PICKETLINK)
            .subsystem(JBossSubsystemNames.PICKETLINK_FEDERATION)
            .build();

    Extension POJO = Extension.builder()
            .module(JBossExtensionNames.POJO)
            .subsystem(JBossSubsystemNames.POJO)
            .build();

    Extension REQUEST_CONTROLLER = Extension.builder()
            .module(JBossExtensionNames.REQUEST_CONTROLLER)
            .subsystem(JBossSubsystemNames.REQUEST_CONTROLLER)
            .build();

    Extension REMOTING = Extension.builder()
            .module(JBossExtensionNames.REMOTING)
            .subsystem(JBossSubsystemNames.REMOTING)
            .build();

    Extension SAR = Extension.builder()
            .module(JBossExtensionNames.SAR)
            .subsystem(JBossSubsystemNames.SAR)
            .build();

    Extension SECURITY = Extension.builder()
            .module(JBossExtensionNames.SECURITY)
            .subsystem(JBossSubsystemNames.SECURITY)
            .build();

    Extension SECURITY_MANAGER = Extension.builder()
            .module(JBossExtensionNames.SECURITY_MANAGER)
            .subsystem(JBossSubsystemNames.SECURITY_MANAGER)
            .build();

    Extension SINGLETON = Extension.builder()
            .module(JBossExtensionNames.SINGLETON)
            .subsystem(JBossSubsystemNames.SINGLETON)
            .build();

    Extension TRANSACTIONS = Extension.builder()
            .module(JBossExtensionNames.TRANSACTIONS)
            .subsystem(JBossSubsystemNames.TRANSACTIONS)
            .build();

    Extension THREADS = Extension.builder()
            .module(JBossExtensionNames.THREADS)
            .subsystem(JBossSubsystemNames.THREADS)
            .build();

    Extension UNDERTOW = Extension.builder()
            .module(JBossExtensionNames.UNDERTOW)
            .subsystem(JBossSubsystemNames.UNDERTOW)
            .build();

    Extension WEB = Extension.builder()
            .module(JBossExtensionNames.WEB)
            .subsystem(JBossSubsystemNames.WEB)
            .build();

    Extension WEBSERVICES = Extension.builder()
            .module(JBossExtensionNames.WEBSERVICES)
            .subsystem(JBossSubsystemNames.WEBSERVICES)
            .build();

    Extension WELD = Extension.builder()
            .module(JBossExtensionNames.WELD)
            .subsystem(JBossSubsystemNames.WELD)
            .build();














}
