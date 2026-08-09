package dev.qilletni.qpm.cli.version;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VersionInfoTest {

    @Test
    void fromProperties_readsAllFields() {
        var properties = new Properties();
        properties.setProperty("qpm.version", "1.2.3");
        properties.setProperty("qilletni.api.version", "4.5.6");
        properties.setProperty("qilletni.pkgutil.version", "7.8.9");
        properties.setProperty("qpm.commit", "abc1234");

        var info = VersionInfo.fromProperties(properties);

        assertEquals("1.2.3", info.qpmVersion());
        assertEquals("4.5.6", info.apiVersion());
        assertEquals("7.8.9", info.pkgutilVersion());
        assertEquals("abc1234", info.commit());
    }

    @Test
    void fromProperties_missingFieldsFallBackToUnknown() {
        var info = VersionInfo.fromProperties(new Properties());

        assertEquals("unknown", info.qpmVersion());
        assertEquals("unknown", info.apiVersion());
        assertEquals("unknown", info.pkgutilVersion());
        assertEquals("unknown", info.commit());
    }

    @Test
    void fromProperties_partiallyMissingFieldsFallBackIndividually() {
        var properties = new Properties();
        properties.setProperty("qpm.version", "1.2.3");

        var info = VersionInfo.fromProperties(properties);

        assertEquals("1.2.3", info.qpmVersion());
        assertEquals("unknown", info.apiVersion());
        assertEquals("unknown", info.pkgutilVersion());
        assertEquals("unknown", info.commit());
    }
}
