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

/**
 * Names of JBoss Server's extensions.
 * @author emmartins
 */
public interface JBossExtensionNames {
    String BATCH = "org.wildfly.extension.batch";
    String BATCH_JBERET = "org.wildfly.extension.batch.jberet";
    String BEAN_VALIDATION = "org.wildfly.extension.bean-validation";
    String CLUSTERING_EJB = "org.wildfly.extension.clustering.ejb";
    String CLUSTERING_WEB = "org.wildfly.extension.clustering.web";
    String CMP = "org.jboss.as.cmp";
    String CONFIGADMIN = "org.jboss.as.configadmin";
    String CONNECTOR = "org.jboss.as.connector";
    String CORE_MANAGEMENT = "org.wildfly.extension.core-management";
    String DATASOURCES_AGROAL = "org.wildfly.extension.datasources-agroal";
    String DEPLOYMENT_SCANNER = "org.jboss.as.deployment-scanner";
    String DISCOVERY = "org.wildfly.extension.discovery";
    String EE = "org.jboss.as.ee";
    String EE_SECURITY = "org.wildfly.extension.ee-security";
    String EJB3 = "org.jboss.as.ejb3";
    String ELYTRON = "org.wildfly.extension.elytron";
    String ELYTRON_JAAS_REALM = "org.wildfly.extension.elytron.jaas-realm";
    String ELYTRON_OIDC_CLIENT = "org.wildfly.extension.elytron-oidc-client";
    String HEALTH = "org.wildfly.extension.health";
    String IIOP_OPENJDK = "org.wildfly.iiop-openjdk";
    String INFINISPAN = "org.jboss.as.clustering.infinispan";
    String IO = "org.wildfly.extension.io";
    String JACORB = "org.jboss.as.jacorb";
    String JAXR = "org.jboss.as.jaxr";
    String JAXRS = "org.jboss.as.jaxrs";
    String JDR = "org.jboss.as.jdr";
    String JGROUPS = "org.jboss.as.clustering.jgroups";
    String JMX = "org.jboss.as.jmx";
    String JPA = "org.jboss.as.jpa";
    String JSF = "org.jboss.as.jsf";
    String JSR77 = "org.jboss.as.jsr77";
    String KEYCLOAK = "org.keycloak.keycloak-adapter-subsystem";
    String KEYCLOAK_SAML = "org.keycloak.keycloak-saml-adapter-subsystem";
    String LOGGING = "org.jboss.as.logging";
    String MAIL = "org.jboss.as.mail";
    String METRICS = "org.wildfly.extension.metrics";
    String MESSAGING = "org.jboss.as.messaging";
    String MESSAGING_ACTIVEMQ = "org.wildfly.extension.messaging-activemq";
    String MICROMETER = "org.wildfly.extension.micrometer";
    String MICROPROFILE_CONFIG_SMALLRYE = "org.wildfly.extension.microprofile.config-smallrye";
    String MICROPROFILE_FAULT_TOLERANCE_SMALLRYE = "org.wildfly.extension.microprofile.fault-tolerance-smallrye";
    String MICROPROFILE_HEALTH_SMALLRYE = "org.wildfly.extension.microprofile.health-smallrye";
    String MICROPROFILE_JWT_SMALLRYE = "org.wildfly.extension.microprofile.jwt-smallrye";
    String MICROPROFILE_LRA_COORDINATOR = "org.wildfly.extension.microprofile.lra-coordinator";
    String MICROPROFILE_LRA_PARTICIPANT = "org.wildfly.extension.microprofile.lra-participant";
    String MICROPROFILE_METRICS_SMALLRYE = "org.wildfly.extension.microprofile.metrics-smallrye";
    String MICROPROFILE_OPENAPI_SMALLRYE = "org.wildfly.extension.microprofile.openapi-smallrye";
    String MICROPROFILE_OPENTRACING_SMALLRYE = "org.wildfly.extension.microprofile.opentracing-smallrye";
    String MICROPROFILE_REACTIVE_MESSAGING_SMALLRYE = "org.wildfly.extension.microprofile.reactive-messaging-smallrye";
    String MICROPROFILE_REACTIVE_STREAMS_OPERATORS_SMALLRYE = "org.wildfly.extension.microprofile.reactive-streams-operators-smallrye";
    String MICROPROFILE_TELEMETRY = "org.wildfly.extension.microprofile.telemetry";
    String MODCLUSTER = "org.jboss.as.modcluster";
    String MVC_KRAZO = "org.wildfly.extension.mvc-krazo";
    String NAMING = "org.jboss.as.naming";
    String OPENTELEMETRY = "org.wildfly.extension.opentelemetry";
    String OSGI = "org.jboss.as.osgi";
    String PICKETLINK = "org.wildfly.extension.picketlink";
    String POJO = "org.jboss.as.pojo";
    String REMOTING = "org.jboss.as.remoting";
    String REQUEST_CONTROLLER = "org.wildfly.extension.request-controller";
    String SAR = "org.jboss.as.sar";
    String SECURITY = "org.jboss.as.security";
    String SECURITY_MANAGER = "org.wildfly.extension.security.manager";
    String SINGLETON = "org.wildfly.extension.clustering.singleton";
    String THREADS = "org.jboss.as.threads";
    String TRANSACTIONS = "org.jboss.as.transactions";
    String UNDERTOW = "org.wildfly.extension.undertow";
    String WEB = "org.jboss.as.web";
    String WEBSERVICES = "org.jboss.as.webservices";
    String WELD = "org.jboss.as.weld";
}
