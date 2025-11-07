package vn.ptit.p2p.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import vn.ptit.p2p.cli.commands.GetCommand;
import vn.ptit.p2p.cli.commands.ShareCommand;
import vn.ptit.p2p.cli.commands.StatCommand;

/**
 * Main entry point for P2P CLI application
 */
@Command(
    name = "p2p",
    version = "1.0.0",
    description = "P2P File Sharing Application",
    mixinStandardHelpOptions = true,
    subcommands = {
        ShareCommand.class,
        GetCommand.class,
        StatCommand.class
    }
)
public class Main implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    public static void main(String[] args) {
        try {
            // Initialize application wiring
            CliWiring wiring = CliWiring.getInstance();
            wiring.initialize();
            
            // Execute command
            int exitCode = new CommandLine(new Main())
                .setExecutionStrategy(new CommandLine.RunLast())
                .execute(args);
            
            // Shutdown
            wiring.shutdown();
            System.exit(exitCode);
            
        } catch (Exception e) {
            logger.error("Application error", e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
    
    @Override
    public void run() {
        System.out.println("P2P File Sharing Application");
        System.out.println("Use --help for available commands");
    }
}

