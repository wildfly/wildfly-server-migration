package org.jboss.migration.core.ts.jboss;

import org.jboss.migration.core.jboss.Extension;
import org.jboss.migration.core.jboss.Subsystem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author emmartins
 */
public class ExtensionsAndSubsystemsTest {

    @Test
    public void testBuilders() {

        final String e1Module = "extension1";

        final String e1s1SubsystemName = getSubsystemName(e1Module,1);
        final String e1s2SubsystemName = getSubsystemName(e1Module,2);
        final String e1s3SubsystemName = getSubsystemName(e1Module,3);
        final String e1s3SubsystemNamespaceWithoutVersion = "urn:cmtool:domain:"+e1s3SubsystemName;
        final String e1s4SubsystemName = getSubsystemName(e1Module,4);
        final String e1s4SubsystemNamespaceWithoutVersion = "urn:cmtool:domain:"+e1s4SubsystemName;

        final Extension e1 = Extension.builder()
                .module(e1Module)
                .subsystem(e1s1SubsystemName)
                .subsystem(Subsystem.builder().name(e1s2SubsystemName))
                .subsystem(Subsystem.builder().name(e1s3SubsystemName).namespaceWithoutVersion(e1s3SubsystemNamespaceWithoutVersion))
                .subsystem(Subsystem.builder().namespaceWithoutVersion(e1s4SubsystemNamespaceWithoutVersion).name(e1s4SubsystemName))
                .build();

        // validate extension
        assertNotNull(e1);
        assertEquals(e1.getModule(), e1Module);
        assertEquals(e1.getSubsystemNames().size(), 4);
        assertEquals(e1.getSubsystems().size(), 4);

        // validate subsystem 1
        final Subsystem e1s1 = validateAndGetSubsystem(e1, e1s1SubsystemName);

        // validate subsystem 2
        final Subsystem e1s2 = validateAndGetSubsystem(e1, e1s2SubsystemName);

        // validate subsystem 3
        final Subsystem e1s3 = validateAndGetSubsystem(e1, e1s3SubsystemName);
        assertEquals(e1s3.getNamespaceWithoutVersion(), e1s3SubsystemNamespaceWithoutVersion);

        // validate subsystem 4
        final Subsystem e1s4 = validateAndGetSubsystem(e1, e1s4SubsystemName);
        assertEquals(e1s4.getNamespaceWithoutVersion(), e1s4SubsystemNamespaceWithoutVersion);
    }

    private String getSubsystemName(String extensionModule, int subsystemNumber) {
        return extensionModule + "-subsystem" + subsystemNumber;
    }

    private Subsystem validateAndGetSubsystem(Extension e, String subsystemName) {
        Subsystem s = e.getSubsystem(subsystemName);
        assertNotNull(s);
        assertEquals(e, s.getExtension());
        assertTrue(e.getSubsystemNames().contains(subsystemName));
        assertTrue(e.getSubsystems().contains(s));
        return s;
    }
}
