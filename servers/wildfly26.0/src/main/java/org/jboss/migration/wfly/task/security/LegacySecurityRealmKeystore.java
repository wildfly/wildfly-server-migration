package org.jboss.migration.wfly.task.security;

public class LegacySecurityRealmKeystore {

    private String provider = "JKS";
    private String path;
    private String relativeTo;
    private String alias;
    private String keyPassword;
    private String keyPasswordCredentialReference;
    private String generateSelfSignedCertificateHost;
    private String keystorePassword;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getRelativeTo() {
        return relativeTo;
    }

    public void setRelativeTo(String relativeTo) {
        this.relativeTo = relativeTo;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }

    public String getKeyPasswordCredentialReference() {
        return keyPasswordCredentialReference;
    }

    public void setKeyPasswordCredentialReference(String keyPasswordCredentialReference) {
        this.keyPasswordCredentialReference = keyPasswordCredentialReference;
    }

    public String getGenerateSelfSignedCertificateHost() {
        return generateSelfSignedCertificateHost;
    }

    public void setGenerateSelfSignedCertificateHost(String generateSelfSignedCertificateHost) {
        this.generateSelfSignedCertificateHost = generateSelfSignedCertificateHost;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    @Override
    public String toString() {
        return "Keystore{" +
                "provider='" + provider + '\'' +
                ", path='" + path + '\'' +
                ", relativeTo='" + relativeTo + '\'' +
                ", alias='" + alias + '\'' +
                ", keyPassword='" + keyPassword + '\'' +
                ", keyPasswordCredentialReference='" + keyPasswordCredentialReference + '\'' +
                ", generateSelfSignedCertificateHost='" + generateSelfSignedCertificateHost + '\'' +
                ", keystorePassword='" + keystorePassword + '\'' +
                '}';
    }
}