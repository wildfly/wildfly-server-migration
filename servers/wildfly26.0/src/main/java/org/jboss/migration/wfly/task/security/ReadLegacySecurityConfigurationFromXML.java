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

package org.jboss.migration.wfly.task.security;

import org.jboss.logging.Logger;
import org.jboss.migration.core.ServerMigrationFailureException;
import org.jboss.migration.core.jboss.JBossServer;
import org.jboss.migration.core.jboss.JBossServerConfiguration;
import org.jboss.migration.core.task.ServerMigrationTask;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.core.task.TaskContext;
import org.jboss.migration.core.task.component.SimpleComponentTask;
import org.jboss.migration.core.util.xml.XMLFileProcessor;
import org.jboss.migration.core.util.xml.XMLFiles;
import org.jboss.migration.wfly10.config.task.ServerConfigurationMigration;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.Iterator;
import java.util.Objects;

import static org.jboss.migration.core.task.component.TaskSkipPolicy.skipIfDefaultTaskSkipPropertyIsSet;

/**
 * @author emmartins
 */
public class ReadLegacySecurityConfigurationFromXML<S extends JBossServer<S>> implements ServerConfigurationMigration.XMLConfigurationSubtaskFactory<JBossServerConfiguration<S>> {

    public static final String TASK_NAME = "security.read-legacy-security-configuration";

    public static final String ALIAS = "alias";
    public static final String ALLOWED_USERS = "allowed-users";

    public static final String AUTH_MODULE = "auth-module";

    public static final String AUTHENTICATION = "authentication";
    public static final String AUTHENTICATION_JASPI = "authentication-jaspi";

    public static final String AUTHORIZATION = "authorization";
    private static final String ENGINE = "engine";
    public static final String GENERATE_SELF_SIGNED_CERTIFICATE_HOST = "generate-self-signed-certificate-host";
    public static final String HTTP_INTERFACE = "http-interface";
    public static final String KEY_PASSWORD = "key-password";
    public static final String KEYSTORE = "keystore";
    public static final String KEYSTORE_PASSWORD = "keystore-password";
    public static final String LOCAL = "local";
    public static final String LOGIN_MODULE = "login-module";
    public static final String LOGIN_MODULE_STACK = "login-module-stack";
    public static final String MANAGEMENT = "management";
    public static final String MANAGEMENT_INTERFACES = "management-interfaces";
    public static final String MODULE_OPTION = "module-option";
    public static final String NATIVE_INTERFACE = "native-interface";
    public static final String NATIVE_REMOTING_INTERFACE = "native-remoting-interface";
    public static final String PATH = "path";
    public static final String PLAIN_TEXT = "plain-text";
    public static final String POLICY_MODULE = "policy-module";
    public static final String PROFILE = "profile";
    public static final String PROFILES = "profiles";
    public static final String PROPERTIES = "properties";
    public static final String RELATIVE_TO = "relative-to";
    private static final String SECRET = "secret";
    public static final String SECURITY_DOMAIN = "security-domain";
    public static final String SECURITY_DOMAINS = "security-domains";
    public static final String SECURITY_REALM = "security-realm";
    public static final String SECURITY_REALMS = "security-realms";
    public static final String SERVER_IDENTITIES = "server-identities";
    public static final String SSL = "ssl";
    public static final String SUBSYSTEM = "subsystem";

    private static final String DEFAULT_USER = "default-user";
    private static final String SKIP_GROUP_LOADING = "skip-group-loading";
    public static final String DOMAIN_CONTROLLER = "domain-controller";
    public static final String REMOTE = "remote";

    private final LegacySecurityConfigurations legacySecurityConfigurations;

    public ReadLegacySecurityConfigurationFromXML(LegacySecurityConfigurations legacySecurityConfigurations) {
        this.legacySecurityConfigurations = legacySecurityConfigurations;
    }

    @Override
    public ServerMigrationTask getTask(final JBossServerConfiguration<S> source, final JBossServerConfiguration targetConfigurationPath) {
        return new SimpleComponentTask.Builder()
                .name(TASK_NAME)
                .skipPolicy(skipIfDefaultTaskSkipPropertyIsSet())
                .runnable(context -> {
                    final Logger logger = context.getLogger();
                    logger.debug("Retrieving legacy security XML configuration...");
                    final LegacySecurityConfiguration legacySecurityConfiguration = processXMLConfiguration(source, targetConfigurationPath, context);
                    ServerMigrationTaskResult taskResult = legacySecurityConfiguration != null ? ServerMigrationTaskResult.SUCCESS : ServerMigrationTaskResult.SKIPPED;
                    if (taskResult.getStatus() == ServerMigrationTaskResult.Status.SKIPPED) {
                        logger.debug("No legacy security XML configuration found.");
                    } else {
                        legacySecurityConfigurations.getSecurityConfigurations().putIfAbsent(legacySecurityConfiguration.getTargetConfiguration().getPath().toString(), legacySecurityConfiguration);
                        context.getLogger().debugf("Legacy security XML configuration: %s",legacySecurityConfiguration);
                        context.getLogger().infof("Legacy security XML configuration retrieved.");
                    }
                    return taskResult;
                })
                .build();
    }

    protected LegacySecurityConfiguration processXMLConfiguration(final JBossServerConfiguration<S> source, final JBossServerConfiguration targetConfigurationPath, final TaskContext context) {
        final LegacySecurityConfiguration legacySecurityConfiguration = new LegacySecurityConfiguration(targetConfigurationPath);
        // setup and run the xml processor
        final XMLFileProcessor xmlFileProcessor = (startElement, xmlEventReader) -> {
            try {
                while (xmlEventReader.hasNext()) {
                    XMLEvent xmlEvent = xmlEventReader.nextEvent();
                    if (xmlEvent.isStartElement()) {
                        final StartElement element = xmlEvent.asStartElement();
                        final String elementLocalName = element.getName().getLocalPart();
                        if (elementLocalName.equals(MANAGEMENT)) {
                            processElementManagement(element, xmlEventReader, legacySecurityConfiguration, context);
                        } else if (elementLocalName.equals(PROFILES)) {
                            processElementProfiles(element, xmlEventReader, legacySecurityConfiguration, context);
                        } else if (elementLocalName.equals(PROFILE)) {
                            processElementProfile(element, xmlEventReader, legacySecurityConfiguration, context);
                        } else if (elementLocalName.equals(DOMAIN_CONTROLLER)) {
                            processElementDomainController(element, xmlEventReader, legacySecurityConfiguration, context);
                        } else {
                            // ignore element
                            skipElement(element, xmlEventReader);
                        }
                    } else if (xmlEvent.isEndElement()) {
                        break;
                    }
                }
            } catch (Throwable e) {
                throw new ServerMigrationFailureException(e);
            }
        };
        XMLFiles.process(targetConfigurationPath.getPath(), xmlFileProcessor);
        return legacySecurityConfiguration;
    }

    protected void processElementManagement(StartElement startElement, XMLEventReader xmlEventReader, LegacySecurityConfiguration legacySecurityConfiguration, final TaskContext context) throws XMLStreamException {
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = xmlEventReader.nextEvent();
            if (xmlEvent.isStartElement()) {
                final StartElement element = xmlEvent.asStartElement();
                final String elementLocalName = element.getName().getLocalPart();
                if (elementLocalName.equals(SECURITY_REALMS)) {
                    processElementSecurityRealms(element, xmlEventReader, legacySecurityConfiguration, context);
                } else if (elementLocalName.equals(MANAGEMENT_INTERFACES)) {
                    processElementManagementInterfaces(element, xmlEventReader, legacySecurityConfiguration, context);
                } else {
                    // ignore element
                    skipElement(element, xmlEventReader);
                }
            } else if (xmlEvent.isEndElement()) {
                break;
            }
        }
    }

    private void processElementProfiles(StartElement startElement, XMLEventReader xmlEventReader, LegacySecurityConfiguration legacySecurityConfiguration, final TaskContext context) throws XMLStreamException {
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = xmlEventReader.nextEvent();
            if (xmlEvent.isStartElement()) {
                final StartElement element = xmlEvent.asStartElement();
                final String elementLocalName = element.getName().getLocalPart();
                if (elementLocalName.equals(PROFILE)) {
                    processElementProfile(element, xmlEventReader, legacySecurityConfiguration, context);
                } else {
                    // ignore element
                    skipElement(element, xmlEventReader);
                }
            } else if (xmlEvent.isEndElement()) {
                break;
            }
        }
    }

    private void processElementManagementInterfaces(StartElement startElement, XMLEventReader xmlEventReader, LegacySecurityConfiguration legacySecurityConfiguration, final TaskContext context) throws XMLStreamException {
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = xmlEventReader.nextEvent();
            if (xmlEvent.isStartElement()) {
                final StartElement element = xmlEvent.asStartElement();
                final String elementLocalName = element.getName().getLocalPart();
                Attribute securityRealmAttr = element.getAttributeByName(new QName("security-realm"));
                if (securityRealmAttr != null) {
                    final LegacySecuredManagementInterface<S> securedManagementInterface = new LegacySecuredManagementInterface<>(elementLocalName, securityRealmAttr.getValue());
                    legacySecurityConfiguration.getSecuredManagementInterfaces().add(securedManagementInterface);
                    context.getLogger().warnf("Management Interface %s is secured by legacy security realm %s, and its configuration will be changed to use the target's server Elytron matching functionally configured by default, which may require additional Elytron manual configuration.", securedManagementInterface.getName(), securedManagementInterface.getSecurityRealm());
                }
                skipElement(startElement, xmlEventReader);
           } else if (xmlEvent.isEndElement()) {
                break;
            }
        }
    }

    private void processElementSecurityRealms(StartElement startElement, XMLEventReader xmlEventReader, LegacySecurityConfiguration legacySecurityConfiguration, final TaskContext context) throws XMLStreamException {
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = xmlEventReader.nextEvent();
            if (xmlEvent.isStartElement()) {
                final StartElement element = xmlEvent.asStartElement();
                final String elementLocalName = element.getName().getLocalPart();
                if (elementLocalName.equals(SECURITY_REALM)) {
                    processElementSecurityRealm(element, xmlEventReader, legacySecurityConfiguration, context);
                } else {
                    // ignore element
                    skipElement(element, xmlEventReader);
                }
            } else if (xmlEvent.isEndElement()) {
                break;
            }
        }
    }

    protected void processElementSecurityRealm(final StartElement startElement, XMLEventReader xmlEventReader, LegacySecurityConfiguration legacySecurityConfiguration, final TaskContext context) throws XMLStreamException {
        final String securityRealmName = startElement.getAttributeByName(new QName("name")).getValue();
        final LegacySecurityRealm securityRealm = new LegacySecurityRealm(securityRealmName);
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = xmlEventReader.nextEvent();
            if (xmlEvent.isStartElement()) {
                final StartElement element = xmlEvent.asStartElement();
                final String elementLocalName = element.getName().getLocalPart();
                if (elementLocalName.equals(SERVER_IDENTITIES)) {
                    processElementServerIdentities(element, xmlEventReader, securityRealm, context);
                } else if (elementLocalName.equals(AUTHENTICATION)) {
                    processElementAuthentication(element, xmlEventReader, securityRealm, context);
                } else if (elementLocalName.equals(AUTHORIZATION)) {
                    processElementAuthorization(element, xmlEventReader, securityRealm, context);
                } else {
                    skipElement(element, xmlEventReader);
                    // fail the migration
                    //throw new UnsupportedOperationException("Legacy security realm element "+elementLocalName+" is not supported, please refer to this specific Migration documentation in the Tool's User Guide for more information");
                }
            } else if (xmlEvent.isEndElement()) {
                break;
            }
        }
        if (legacySecurityConfiguration.getLegacySecurityRealms().putIfAbsent(securityRealm.getName(), securityRealm) != null) {
            throw new IllegalStateException("security realm "+securityRealm+" already added to the legacy security configuration");
        } else {
            context.getLogger().debugf("Legacy security realm %s found.",securityRealm.getName());
        }
    }

    protected void processElementServerIdentities(final StartElement startElement, XMLEventReader xmlEventReader, LegacySecurityRealm securityRealm, final TaskContext context) throws XMLStreamException {
        LegacySecurityRealm.ServerIdentities serverIdentities = new LegacySecurityRealm.ServerIdentities();
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = xmlEventReader.nextEvent();
            if (xmlEvent.isStartElement()) {
                final StartElement element = xmlEvent.asStartElement();
                final String elementLocalName = element.getName().getLocalPart();
                if (elementLocalName.equals(SSL)) {
                    processElementServerIdentitiesSSL(element, xmlEventReader, serverIdentities, context);
                } else {
                    //context.getLogger().warnf("Legacy security realm's %s server identity found. Please note that the migration for such server identity is not supported and will be ignored.", elementLocalName);
                    skipElement(element, xmlEventReader);
                }
            } else if (xmlEvent.isEndElement()) {
                break;
            }
        }
        securityRealm.setServerIdentities(serverIdentities);
    }

    protected void processElementServerIdentitiesSSL(final StartElement startElement, XMLEventReader xmlEventReader, LegacySecurityRealm.ServerIdentities serverIdentities, final TaskContext context) throws XMLStreamException {
        LegacySecurityRealmSSLServerIdentity serverIdentity = new LegacySecurityRealmSSLServerIdentity();
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = xmlEventReader.nextEvent();
            if (xmlEvent.isStartElement()) {
                final StartElement element = xmlEvent.asStartElement();
                final String elementLocalName = element.getName().getLocalPart();
                if (elementLocalName.equals(KEYSTORE)) {
                    processElementServerIdentitiesSSLKeystore(element, xmlEventReader, serverIdentity, context);
                } else {
                    //context.getLogger().warnf("Legacy security realm SSL element %s found. Please note that the migration for such element is not available and will be ignored, which may result on an invalid/different migrated SSL configuration.", elementLocalName);
                    skipElement(element, xmlEventReader);
                }
            } else if (xmlEvent.isEndElement()) {
                break;
            }
        }
        serverIdentities.setSsl(serverIdentity);
    }

    protected void processElementServerIdentitiesSSLKeystore(final StartElement startElement, XMLEventReader xmlEventReader, LegacySecurityRealmSSLServerIdentity serverIdentity, final TaskContext context) throws XMLStreamException {
        LegacySecurityRealmKeystore keystore = new LegacySecurityRealmKeystore();
        final Iterator<Attribute> iterator = startElement.getAttributes();
        while(iterator.hasNext()) {
            Attribute attribute = iterator.next();
            if (attribute.getName().getLocalPart().equals(PATH)) {
                keystore.setPath(attribute.getValue());
            } else if (attribute.getName().getLocalPart().equals(RELATIVE_TO)) {
                keystore.setRelativeTo(attribute.getValue());
            } else if (attribute.getName().getLocalPart().equals(KEYSTORE_PASSWORD)) {
                keystore.setKeystorePassword(attribute.getValue());
            } else if (attribute.getName().getLocalPart().equals(ALIAS)) {
                keystore.setAlias(attribute.getValue());
            } else if (attribute.getName().getLocalPart().equals(KEY_PASSWORD)) {
                keystore.setKeyPassword(attribute.getValue());
            } else if (attribute.getName().getLocalPart().equals(GENERATE_SELF_SIGNED_CERTIFICATE_HOST)) {
                keystore.setGenerateSelfSignedCertificateHost(attribute.getValue());
            } else {
                //context.getLogger().debugf("Skipping unexpected attribute of server identities ssl keystore: %s",attribute.getName().getLocalPart());
            }
        }
        skipElement(startElement, xmlEventReader);
        serverIdentity.setKeystore(keystore);
    }

    protected void processElementAuthentication(final StartElement startElement, XMLEventReader xmlEventReader, LegacySecurityRealm securityRealm, final TaskContext context) throws XMLStreamException {
        LegacySecurityRealm.Authentication authentication = new LegacySecurityRealm.Authentication();
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = xmlEventReader.nextEvent();
            if (xmlEvent.isStartElement()) {
                final StartElement element = xmlEvent.asStartElement();
                final String elementLocalName = element.getName().getLocalPart();
                if (elementLocalName.equals(LOCAL)) {
                    processElementLocal(element, xmlEventReader, authentication, context);
                } else if (elementLocalName.equals(PROPERTIES)) {
                    authentication.setProperties(processElementProperties(element, xmlEventReader, context));
                } else {
                    skipElement(element, xmlEventReader);
                    // fail the migration
                    // throw new UnsupportedOperationException("Legacy security realm authentication element "+elementLocalName+" is not supported, please refer to this specific Migration documentation in the Tool's User Guide for more information");
                }
            } else if (xmlEvent.isEndElement()) {
                break;
            }
        }
        securityRealm.setAuthentication(authentication);
    }

    protected void processElementLocal(final StartElement startElement, XMLEventReader xmlEventReader, LegacySecurityRealm.Authentication authentication, final TaskContext context) throws XMLStreamException {
        LegacySecurityRealm.Local local = new LegacySecurityRealm.Local();
        final Iterator<Attribute> iterator = startElement.getAttributes();
        while(iterator.hasNext()) {
            Attribute attribute = iterator.next();
            if (attribute.getName().getLocalPart().equals(DEFAULT_USER)) {
                local.setDefaultUser(attribute.getValue());
            } else if (attribute.getName().getLocalPart().equals(SKIP_GROUP_LOADING)) {
                local.setSkipGroupLoading(Boolean.valueOf(attribute.getValue()));
            } else if (attribute.getName().getLocalPart().equals(ALLOWED_USERS)) {
                local.setAllowedUsers(attribute.getValue());
            } else {
                context.getLogger().debugf("Skipping unexpected attribute of authentication's local: %s",attribute.getName().getLocalPart());
            }
        }
        skipElement(startElement, xmlEventReader);
        authentication.setLocal(local);
    }

    protected LegacySecurityRealm.Properties processElementProperties(final StartElement startElement, XMLEventReader xmlEventReader, final TaskContext context) throws XMLStreamException {
        LegacySecurityRealm.Properties properties = new LegacySecurityRealm.Properties();
        final Iterator<Attribute> iterator = startElement.getAttributes();
        while(iterator.hasNext()) {
            Attribute attribute = iterator.next();
            if (attribute.getName().getLocalPart().equals(PATH)) {
                properties.setPath(attribute.getValue());
            } else if (attribute.getName().getLocalPart().equals(RELATIVE_TO)) {
                properties.setRelativeTo(attribute.getValue());
            } else if (attribute.getName().getLocalPart().equals(PLAIN_TEXT)) {
                properties.setPlainText(Boolean.valueOf(attribute.getValue()));
            } else {
                context.getLogger().debugf("Skipping unexpected attribute of properties: %s",attribute.getName().getLocalPart());
            }
        }
        skipElement(startElement, xmlEventReader);
        return properties;
    }

    protected void processElementAuthorization(final StartElement startElement, XMLEventReader xmlEventReader, LegacySecurityRealm securityRealm, final TaskContext context) throws XMLStreamException {
        LegacySecurityRealm.Authorization authorization = new LegacySecurityRealm.Authorization();
        Attribute mapGroupsToRolesAttr = startElement.getAttributeByName(new QName("map-groups-to-roles"));
        if (mapGroupsToRolesAttr != null) {
            authorization.setMapGroupsToRoles(Boolean.valueOf(mapGroupsToRolesAttr.getValue()));
        }
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = xmlEventReader.nextEvent();
            if (xmlEvent.isStartElement()) {
                final StartElement element = xmlEvent.asStartElement();
                final String elementLocalName = element.getName().getLocalPart();
                if (elementLocalName.equals(PROPERTIES)) {
                    authorization.setProperties(processElementProperties(element, xmlEventReader, context));
                } else {
                    skipElement(element, xmlEventReader);
                    // fail the migration
                    // throw new UnsupportedOperationException("Legacy security realm authorization's element "+elementLocalName+" is not supported, please refer to this specific Migration documentation in the Tool's User Guide for more information");
                }
            } else if (xmlEvent.isEndElement()) {
                break;
            }
        }
        securityRealm.setAuthorization(authorization);
    }

    protected void processElementProfile(final StartElement startElement, XMLEventReader xmlEventReader, LegacySecurityConfiguration legacySecurityConfiguration, final TaskContext context) throws XMLStreamException {
        final Attribute profileNameAttribute = startElement.getAttributeByName(new QName("name"));
        final String profileName = profileNameAttribute != null ? profileNameAttribute.getValue() : null;
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = xmlEventReader.nextEvent();
            if (xmlEvent.isStartElement()) {
                final StartElement element = xmlEvent.asStartElement();
                if (element.getName().getLocalPart().equals(SUBSYSTEM) && element.getName().getNamespaceURI().startsWith("urn:jboss:domain:security:")) {
                    processElementSecuritySubsystem(element, xmlEventReader, legacySecurityConfiguration, profileName, context);
                } else {
                    // ignore
                    skipElement(element, xmlEventReader);
                }
            } else if (xmlEvent.isEndElement()) {
                break;
            }
        }
    }

    protected void processElementSecuritySubsystem(StartElement startElement, XMLEventReader xmlEventReader, LegacySecurityConfiguration legacySecurityConfiguration, final String profileName, final TaskContext context) throws XMLStreamException {
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = xmlEventReader.nextEvent();
            if (xmlEvent.isStartElement()) {
                final StartElement element = xmlEvent.asStartElement();
                final String elementLocalName = element.getName().getLocalPart();
                if (elementLocalName.equals(SECURITY_DOMAINS)) {
                    processElementSecurityDomains(element, xmlEventReader, legacySecurityConfiguration, profileName, context);
                } else {
                    // ignore element
                    skipElement(element, xmlEventReader);
                }
            } else if (xmlEvent.isEndElement()) {
                break;
            }
        }
    }

    protected void processElementSecurityDomains(StartElement startElement, XMLEventReader xmlEventReader, LegacySecurityConfiguration legacySecurityConfiguration, final String profileName, final TaskContext context) throws XMLStreamException {
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = xmlEventReader.nextEvent();
            if (xmlEvent.isStartElement()) {
                final StartElement element = xmlEvent.asStartElement();
                final String elementLocalName = element.getName().getLocalPart();
                if (elementLocalName.equals(SECURITY_DOMAIN)) {
                    processElementSecurityDomain(element, xmlEventReader, legacySecurityConfiguration, profileName, context);
                } else {
                    // ignore element
                    skipElement(element, xmlEventReader);
                }
            } else if (xmlEvent.isEndElement()) {
                break;
            }
        }
    }

    protected void processElementSecurityDomain(StartElement startElement, XMLEventReader xmlEventReader, LegacySecurityConfiguration legacySecurityConfiguration, final String profileName, final TaskContext context) throws XMLStreamException {
        final String securityDomainName = startElement.getAttributeByName(new QName("name")).getValue();
        final LegacySecurityDomain securityDomain = new LegacySecurityDomain(securityDomainName, profileName);
        final Attribute securityDomainCacheTypeAttr = startElement.getAttributeByName(new QName("cache-type"));
        if (securityDomainCacheTypeAttr != null) {
            securityDomain.setCacheType(securityDomainCacheTypeAttr.getValue());
        }
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = xmlEventReader.nextEvent();
            if (xmlEvent.isStartElement()) {
                final StartElement element = xmlEvent.asStartElement();
                final String elementLocalName = element.getName().getLocalPart();
                if (elementLocalName.equals(AUTHENTICATION)) {
                    processElementSecurityDomainAuthentication(element, xmlEventReader, securityDomain, context);
                } else if (elementLocalName.equals(AUTHENTICATION_JASPI)) {
                    //processElementSecurityDomainAuthenticationJaspi(element, xmlEventReader, securityDomain, context);
                    //context.getLogger().warnf("Migration of legacy security domain %s's authentication-jaspi %s is not supported and will be ignored.", securityDomain.getName(), (profileName != null ? ", on profile "+profileName+"," : ""));
                    skipElement(element, xmlEventReader);
                } else if (elementLocalName.equals(AUTHORIZATION)) {
                    //processElementSecurityDomainAuthorization(element, xmlEventReader, securityDomain, context);
                    //context.getLogger().warnf("Migration of legacy security domain %s's authorization%s is not supported and will be ignored.", securityDomain.getName(), (profileName != null ? ", on profile "+profileName+"," : ""));
                    skipElement(element, xmlEventReader);
                } else {
                    skipElement(element, xmlEventReader);
                    // fail the migration
                    // throw new UnsupportedOperationException("Legacy security domain element "+elementLocalName+" is not supported, please refer to this specific Migration Task documentation in the Tool's User Guide for more information");
                }
            } else if (xmlEvent.isEndElement()) {
                break;
            }
        }
        legacySecurityConfiguration.getLegacySecurityDomains().add(securityDomain);
        if (securityDomain.getProfile() == null) {
            context.getLogger().debugf("Legacy security domain %s found.", securityDomain.getName());
        } else {
            context.getLogger().debugf("Legacy security domain %s found on profile %s.", securityDomain.getName(), securityDomain.getProfile());
        }
    }

    protected void processElementSecurityDomainAuthentication(final StartElement startElement, XMLEventReader xmlEventReader, LegacySecurityDomain securityDomain, final TaskContext context) throws XMLStreamException {
        LegacySecurityDomain.Authentication authentication = new LegacySecurityDomain.Authentication();
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = xmlEventReader.nextEvent();
            if (xmlEvent.isStartElement()) {
                final StartElement element = xmlEvent.asStartElement();
                final String elementLocalName = element.getName().getLocalPart();
                if (elementLocalName.equals(LOGIN_MODULE)) {
                    final LegacySecurityDomain.LoginModule loginModule = processElementLoginModule(element, xmlEventReader, context);
                    if (loginModule != null) {
                        authentication.getLoginModules().add(loginModule);
                    }
                } else {
                    skipElement(element, xmlEventReader);
                    // fail the migration
                    // throw new UnsupportedOperationException("Legacy security domain authentication element "+elementLocalName+" is not supported, please refer to this specific Migration documentation in the Tool's User Guide for more information");
                }
            } else if (xmlEvent.isEndElement()) {
                break;
            }
        }
        securityDomain.setAuthentication(authentication);
    }

    protected LegacySecurityDomain.LoginModule processElementLoginModule(final StartElement startElement, XMLEventReader xmlEventReader, final TaskContext context) throws XMLStreamException {
        final LegacySecurityDomain.LoginModule module = new LegacySecurityDomain.LoginModule();
        processElementModule(startElement, xmlEventReader, module, context);
        Objects.requireNonNull(module.getCode());
        Objects.requireNonNull(module.getFlag());
        if (LegacySecurityDomain.SUPPORTED_LOGIN_MODULE_CODES.contains(module.getCode())) {
            return module;
        } else {
            // unsupported login module
            //context.getLogger().warnf("Legacy security domain's login-module with code %s found. Please note that the migration for such element is not available and will be ignored, which may result on an invalid/different migrated security domain configuration.", module.getCode());
            return null;
        }
    }

    protected void processElementModule(final StartElement startElement, XMLEventReader xmlEventReader, LegacySecurityDomain.Module module, final TaskContext context) throws XMLStreamException {
        final Attribute codeAttribute = startElement.getAttributeByName(new QName("code"));
        if (codeAttribute != null) {
            module.setCode(codeAttribute.getValue());
        }
        final Attribute flagAttribute = startElement.getAttributeByName(new QName("flag"));
        if (flagAttribute != null) {
            module.setFlag(flagAttribute.getValue());
        }
        final Attribute nameAttribute = startElement.getAttributeByName(new QName("name"));
        if (nameAttribute != null) {
            module.setName(nameAttribute.getValue());
        }
        final Attribute moduleAttribute = startElement.getAttributeByName(new QName("module"));
        if (moduleAttribute != null) {
            module.setName(moduleAttribute.getValue());
        }
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = xmlEventReader.nextEvent();
            if (xmlEvent.isStartElement()) {
                final StartElement element = xmlEvent.asStartElement();
                final String elementLocalName = element.getName().getLocalPart();
                if (elementLocalName.equals(MODULE_OPTION)) {
                    processElementModuleOption(element, xmlEventReader, module, context);
                } else {
                    skipElement(element, xmlEventReader);
                    // fail the migration
                    // throw new UnsupportedOperationException("Legacy security domain authentication element "+elementLocalName+" is not supported, please refer to this specific Migration documentation in the Tool's User Guide for more information");
                }
            } else if (xmlEvent.isEndElement()) {
                break;
            }
        }
    }
    protected void processElementModuleOption(final StartElement startElement, XMLEventReader xmlEventReader, final LegacySecurityDomain.Module module, final TaskContext context) throws XMLStreamException {
        final String name = startElement.getAttributeByName(new QName("name")).getValue();
        final String value = startElement.getAttributeByName(new QName("value")).getValue();
        module.getModuleOptions().put(name, value);
        skipElement(startElement, xmlEventReader);
    }

    protected void processElementSecurityDomainAuthorization(final StartElement startElement, XMLEventReader xmlEventReader, LegacySecurityDomain securityDomain, final TaskContext context) throws XMLStreamException {
        LegacySecurityDomain.Authorization authorization = new LegacySecurityDomain.Authorization();
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = xmlEventReader.nextEvent();
            if (xmlEvent.isStartElement()) {
                final StartElement element = xmlEvent.asStartElement();
                final String elementLocalName = element.getName().getLocalPart();
                if (elementLocalName.equals(POLICY_MODULE)) {
                    processElementPolicyModule(element, xmlEventReader, authorization, context);
                } else {
                    skipElement(element, xmlEventReader);
                    // fail the migration
                    //throw new UnsupportedOperationException("Legacy security domain authorization element "+elementLocalName+" is not supported, please refer to this specific Migration documentation in the Tool's User Guide for more information");
                }
            } else if (xmlEvent.isEndElement()) {
                break;
            }
        }
        securityDomain.setAuthorization(authorization);
    }

    protected void processElementPolicyModule(final StartElement startElement, XMLEventReader xmlEventReader, LegacySecurityDomain.Authorization authorization, final TaskContext context) throws XMLStreamException {
        final LegacySecurityDomain.PolicyModule module = new LegacySecurityDomain.PolicyModule();
        processElementModule(startElement, xmlEventReader, module, context);
        Objects.requireNonNull(module.getCode());
        Objects.requireNonNull(module.getFlag());
        authorization.getPolicyModules().add(module);
    }

    protected void processElementSecurityDomainAuthenticationJaspi(final StartElement startElement, XMLEventReader xmlEventReader, LegacySecurityDomain securityDomain, final TaskContext context) throws XMLStreamException {
        LegacySecurityDomain.AuthenticationJaspi authentication = new LegacySecurityDomain.AuthenticationJaspi();
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = xmlEventReader.nextEvent();
            if (xmlEvent.isStartElement()) {
                final StartElement element = xmlEvent.asStartElement();
                final String elementLocalName = element.getName().getLocalPart();
                if (elementLocalName.equals(LOGIN_MODULE_STACK)) {
                    processElementLoginModuleStack(element, xmlEventReader, authentication, context);
                } else if (elementLocalName.equals(AUTH_MODULE)) {
                    processElementAuthModule(element, xmlEventReader, authentication, context);
                } else {
                    skipElement(element, xmlEventReader);
                    // fail the migration
                    //throw new UnsupportedOperationException("Legacy security domain authentication element "+elementLocalName+" is not supported, please refer to this specific Migration documentation in the Tool's User Guide for more information");
                }
            } else if (xmlEvent.isEndElement()) {
                break;
            }
        }
        securityDomain.setAuthenticationJaspi(authentication);
    }

    protected void processElementLoginModuleStack(final StartElement startElement, XMLEventReader xmlEventReader, LegacySecurityDomain.AuthenticationJaspi authentication, final TaskContext context) throws XMLStreamException {
        final String name = startElement.getAttributeByName(new QName("name")).getValue();
        LegacySecurityDomain.LoginModuleStack loginModuleStack = new LegacySecurityDomain.LoginModuleStack(name);
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = xmlEventReader.nextEvent();
            if (xmlEvent.isStartElement()) {
                final StartElement element = xmlEvent.asStartElement();
                final String elementLocalName = element.getName().getLocalPart();
                if (elementLocalName.equals(LOGIN_MODULE)) {
                    loginModuleStack.getLoginModules().add(processElementLoginModule(element, xmlEventReader, context));
                } else {
                    skipElement(element, xmlEventReader);
                    // fail the migration
                    //throw new UnsupportedOperationException("Legacy security domain authentication-jaspi login module stack element "+elementLocalName+" is not supported, please refer to this specific Migration documentation in the Tool's User Guide for more information");
                }
            } else if (xmlEvent.isEndElement()) {
                break;
            }
        }
        authentication.getLoginModulesStacks().add(loginModuleStack);
    }

    protected void processElementAuthModule(final StartElement startElement, XMLEventReader xmlEventReader, LegacySecurityDomain.AuthenticationJaspi authentication, final TaskContext context) throws XMLStreamException {
        final LegacySecurityDomain.AuthModule module = new LegacySecurityDomain.AuthModule();
        final Attribute loginModuleStackRefAttribute = startElement.getAttributeByName(new QName("login-module-stack-ref"));
        if (loginModuleStackRefAttribute != null) {
            module.setLoginModuleStackRef(loginModuleStackRefAttribute.getValue());
        }
        processElementModule(startElement, xmlEventReader, module, context);
        Objects.requireNonNull(module.getCode());
        authentication.getAuthModules().add(module);
    }

    protected void processElementDomainController(StartElement startElement, XMLEventReader xmlEventReader, LegacySecurityConfiguration legacySecurityConfiguration, final TaskContext context) throws XMLStreamException {
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = xmlEventReader.nextEvent();
            if (xmlEvent.isStartElement()) {
                final StartElement element = xmlEvent.asStartElement();
                final String elementLocalName = element.getName().getLocalPart();
                if (elementLocalName.equals(REMOTE)) {
                    processElementRemote(element, xmlEventReader, legacySecurityConfiguration, context);
                } else {
                    // ignore element
                    skipElement(element, xmlEventReader);
                }
            } else if (xmlEvent.isEndElement()) {
                break;
            }
        }
    }

    protected void processElementRemote(final StartElement startElement, XMLEventReader xmlEventReader, LegacySecurityConfiguration configuration, TaskContext context) throws XMLStreamException {
        final Attribute securityRealmAttribute = startElement.getAttributeByName(new QName(SECURITY_REALM));
        if (securityRealmAttribute != null) {
            final String securityRealm = securityRealmAttribute.getValue();
            if (!securityRealm.isEmpty()) {
                configuration.setDomainControllerRemoteSecurityRealm(securityRealm);
            }
        }
        skipElement(startElement, xmlEventReader);
    }

    protected void skipElement(StartElement startElement, XMLEventReader xmlEventReader) throws XMLStreamException {
        int endElementsLeft = 1;
        do {
            XMLEvent xmlEvent = xmlEventReader.nextEvent();
            if (xmlEvent.isStartElement()) {
                endElementsLeft++;
            }
            else if(xmlEvent.isEndElement()){
                endElementsLeft--;
            }
        } while (xmlEventReader.hasNext() && endElementsLeft > 0);
    }
}