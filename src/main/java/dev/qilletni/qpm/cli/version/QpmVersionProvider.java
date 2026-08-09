package dev.qilletni.qpm.cli.version;

import picocli.CommandLine;

/**
 * Backs the {@code --version} option of {@code qpm}, printing this repository's own version
 * alongside the exact {@code qilletni-api}/{@code qilletni-pkgutil} versions it was built
 * against and the source commit (see {@link VersionInfo}).
 */
public class QpmVersionProvider implements CommandLine.IVersionProvider {

    @Override
    public String[] getVersion() {
        return formatLines(VersionInfo.load());
    }

    static String[] formatLines(VersionInfo info) {
        return new String[]{
                "qpm " + info.qpmVersion(),
                "dev.qilletni.api:qilletni-api " + info.apiVersion(),
                "dev.qilletni.pkgutil:qilletni-pkgutil " + info.pkgutilVersion(),
                "commit " + info.commit()
        };
    }
}
