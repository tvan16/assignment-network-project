package vn.ptit.p2p.cli.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import vn.ptit.p2p.cli.CliWiring;
import vn.ptit.p2p.common.Models.FileMetadata;
import vn.ptit.p2p.common.Models.Peer;
import vn.ptit.p2p.common.Utils;
import vn.ptit.p2p.control.ManifestStore;
import vn.ptit.p2p.control.PieceScheduler;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Command to download a file from the network
 */
@Command(
    name = "get",
    description = "Download a file from the P2P network",
    mixinStandardHelpOptions = true
)
public class GetCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(GetCommand.class);
    
    @Parameters(
        index = "0",
        description = "Hash of the file to download"
    )
    private String fileHash;
    
    @Option(
        names = {"-o", "--output"},
        description = "Output file path (default: downloads/<filename>)"
    )
    private File outputPath;
    
    @Override
    public Integer call() throws Exception {
        if (!Utils.isValidHash(fileHash)) {
            System.err.println("Error: Invalid file hash format");
            return 1;
        }
        
        System.out.println("Downloading file: " + fileHash);
        
        try {
            CliWiring wiring = CliWiring.getInstance();
            ManifestStore manifestStore = wiring.getManifestStore();
            
            // Check if we have the manifest
            if (!manifestStore.hasManifest(fileHash)) {
                System.err.println("Error: File not found in network");
                System.err.println("Make sure at least one peer is sharing this file");
                return 1;
            }
            
            // Get file metadata
            FileMetadata metadata = manifestStore.getManifest(fileHash);
            System.out.println("File: " + metadata.getFileName());
            System.out.println("Size: " + Utils.formatBytes(metadata.getFileSize()));
            System.out.println("Pieces: " + metadata.getPieceCount());
            
            // Find peers
            Set<String> peers = manifestStore.getPeersForFile(fileHash);
            System.out.println("Available peers: " + peers.size());
            
            if (peers.isEmpty()) {
                System.err.println("Error: No peers available for this file");
                return 1;
            }
            
            // Determine output path
            if (outputPath == null) {
                String downloadDir = wiring.getConfig().getString("storage.download_dir", "./downloads");
                outputPath = new File(downloadDir, metadata.getFileName());
            }
            
            System.out.println("Output: " + outputPath.getAbsolutePath());
            
            // Check for resume
            if (wiring.getResumeManager().hasResumeState(fileHash)) {
                System.out.println("Found partial download, resuming...");
            }
            
            // Get missing pieces
            List<Integer> missingPieces = manifestStore.getMissingPieces(fileHash);
            System.out.println("Pieces to download: " + missingPieces.size());
            
            // Start download (simplified - in real implementation would actually download)
            System.out.println("\nStarting download...");
            System.out.println("(Download implementation would happen here)");
            
            // Simulate progress
            System.out.println("Progress: 0%");
            System.out.println("Note: Full download implementation requires data transfer integration");
            
            return 0;
            
        } catch (Exception e) {
            logger.error("Error downloading file", e);
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}

