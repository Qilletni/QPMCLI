package dev.qilletni.qpm.cli;

import dev.qilletni.qpm.cli.commands.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

@CommandLine.Command(
    name = "qpm",
    version = "v1.0.0-SNAPSHOT",
    description = "Qilletni Package Manager - A CLI tool for managing Qilletni packages",
    subcommands = {
        LoginCommand.class,
        PublishCommand.class,
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

    @CommandLine.Option(names = {"-v", "--version"}, versionHelp = true, description = "Display the version")
    private boolean versionRequested = false;

    public static void main(String[] args) {
        var application = new QilletniPackageManagerApplication();
        CommandLine cmd = new CommandLine(application);

        // Show help if no arguments provided
        if (args.length == 0) {
            cmd.usage(System.out);
            System.exit(0);
        }

        // Execute command and exit with appropriate status code
        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }
}
