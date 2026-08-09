package dev.qilletni.qpm.cli.version;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class QpmVersionProviderTest {

    @Test
    void formatLines_includesOwnAndUpstreamVersionsAndCommit() {
        var info = new VersionInfo("1.2.3", "4.5.6", "7.8.9", "abc1234");

        var lines = QpmVersionProvider.formatLines(info);

        assertArrayEquals(new String[]{
                "qpm 1.2.3",
                "dev.qilletni.api:qilletni-api 4.5.6",
                "dev.qilletni.pkgutil:qilletni-pkgutil 7.8.9",
                "commit abc1234"
        }, lines);
    }
}
