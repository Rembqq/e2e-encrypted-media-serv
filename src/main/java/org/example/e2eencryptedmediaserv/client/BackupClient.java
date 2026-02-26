package org.example.e2eencryptedmediaserv.client;

import org.example.e2eencryptedmediaserv.client.commands.UploadCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "backup",
        mixinStandardHelpOptions = true,
        version = "0.1.0",
        description = "E2E Encrypted Backup CLI",
        subcommands = {UploadCommand.class}
)

public class BackupClient {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new BackupClient()).execute(args);
        System.exit(exitCode);
    }
}



