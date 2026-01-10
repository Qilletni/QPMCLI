package dev.qilletni.qpm.cli;

import dev.qilletni.qpm.cli.commands.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.Arrays;
import java.util.stream.Collectors;

@CommandLine.Command(
    name = "qpm",
    version = "v1.0.0-SNAPSHOT",
    description = "Qilletni Package Manager - A CLI tool for managing Qilletni packages",
    subcommands = {
        LoginCommand.class,
        PublishCommand.class,
        PublishLocalCommand.class,
        InstallCommand.class,
        VerifyCommand.class,
        ListCommand.class,
        DeleteCommand.class,
        CommandLine.HelpCommand.class
    }
)
public class QilletniPackageManagerApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(QilletniPackageManagerApplication.class);

    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "Display a help message")
    private boolean helpRequested = false;

    @CommandLine.Option(names = {"--version"}, versionHelp = true, description = "Display the version")
    private boolean versionRequested = false;

    @CommandLine.Option(names = {"-v", "--verbose"}, description = "Enable verbose logging output")
    public void setVerbose(boolean verbose) {
        QilletniPackageManagerApplication.verbose = verbose;
        if (verbose) {
            System.setProperty("VERBOSE", "DEBUG");
            // Reconfigure log4j2 to pick up the new system property
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            context.reconfigure();
        }
    }

    public static void main(String[] args) {
        var application = new QilletniPackageManagerApplication();
        var cmd = new CommandLine(application);

        if (args.length == 0) {
            cmd.usage(System.out);
            System.exit(0);
        }

        LOGGER.info("Executing command with args:  {}", Arrays.stream(args).map("'%s'"::formatted).collect(Collectors.joining(" ")));

        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }

    private static boolean verbose = false;

    public static boolean isVerbose() {
        return verbose;
    }
}
