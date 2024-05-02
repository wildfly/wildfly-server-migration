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
 * The names of supported subsystems.
 * @author emmartins
 */
public interface JBossSubsystemNames {

    String BATCH = "batch";
    String BATCH_JBERET = "batch-jberet";
    String BEAN_VALIDATION = "bean-validation";
    String CMP = "core-management";
    String CONFIGADMIN = "configadmin";
    String CORE_MANAGEMENT = "core-management";
    String DATASOURCES = "datasources";
    String DATASOURCES_AGROAL = "datasources-agroal";
    String DEPLOYMENT_SCANNER = "deployment-scanner";
    String DISCOVERY = "discovery";
    String DISTRIBUTABLE_EJB = "distributable-ejb";
    String DISTRIBUTABLE_WEB = "distributable-web";
    String EE = "ee";
    String EE_SECURITY = "ee-security";
    String EJB3 = "ejb3";
    String ELYTRON = "elytron";
    String ELYTRON_OIDC_CLIENT = "elytron-oidc-client";
    String HEALTH = "health";
    String IIOP_OPENJDK = "iiop-openjdk";
    String INFINISPAN = "infinispan";
    String IO = "io";
    String JACORB = "jacorb";
    String JAXR = "jaxr";
    String JAXRS = "jaxrs";
    String JCA = "jca";
    String JDR = "jdr";
    String JGROUPS = "jgroups";
    String JMX = "jmx";
    String JPA = "jpa";
    String JSF = "jsf";
    String JSR77 = "jsr77";
    String KEYCLOAK = "keycloak";
    String LOGGING = "logging";
    String MAIL = "mail";
    String METRICS = "metrics";
    String MESSAGING = "messaging";
    String MESSAGING_ACTIVEMQ = "messaging-activemq";
    String MICROMETER = "micrometer";
    String MICROPROFILE_CONFIG_SMALLRYE = "microprofile-config-smallrye";
    String MICROPROFILE_FAULT_TOLERANCE_SMALLRYE = "microprofile-fault-tolerance-smallrye";
    String MICROPROFILE_HEALTH_SMALLRYE = "microprofile-health-smallrye";
    String MICROPROFILE_JWT_SMALLRYE = "microprofile-jwt-smallrye";
    String MICROPROFILE_LRA_COORDINATOR = "microprofile-lra-coordinator";
    String MICROPROFILE_LRA_PARTICIPANT = "microprofile-lra-participant";
    String MICROPROFILE_METRICS_SMALLRYE = "microprofile-metrics-smallrye";
    String MICROPROFILE_OPENAPI_SMALLRYE = "microprofile-openapi-smallrye";
    String MICROPROFILE_OPENTRACING_SMALLRYE = "microprofile-opentracing-smallrye";
    String MICROPROFILE_REACTIVE_MESSAGING_SMALLRYE = "microprofile-reactive-messaging-smallrye";
    String MICROPROFILE_REACTIVE_STREAMS_OPERATORS_SMALLRYE = "microprofile-reactive-streams-operators-smallrye";
    String MICROPROFILE_TELEMETRY = "microprofile-telemetry";

    String MODCLUSTER = "modcluster";
    String MVC_KRAZO = "mvc-krazo";
    String NAMING = "naming";
    String OPENTELEMETRY = "opentelemetry";
    String OSGI = "osgi";
    String PICKETLINK_FEDERATION = "picketlink-federation";
    String POJO = "pojo";
    String REMOTING = "remoting";
    String REQUEST_CONTROLLER = "request-controller";
    String RESOURCE_ADAPTERS = "resource-adapters";
    String SAR = "sar";
    String SECURITY = "security";
    String SECURITY_MANAGER = "security-manager";
    String SINGLETON = "singleton";
    String THREADS = "threads";
    String TRANSACTIONS = "transactions";
    String UNDERTOW = "undertow";
    String WEB = "web";
    String WEBSERVICES = "webservices";
    String WELD = "weld";

}
