package org.jboss.migration.wfly.task.security;

public class LegacySecurityRealmSSLServerIdentity {

    private LegacySecurityRealmKeystore keystore;

    public LegacySecurityRealmKeystore getKeystore() {
        return keystore;
    }

    public void setKeystore(LegacySecurityRealmKeystore keystore) {
        this.keystore = keystore;
    }

    @Override
    public String toString() {
        return "LegacySecurityRealmSSLServerIdentity{" +
                "keystore=" + keystore +
                '}';
    }
}
