package vn.ptit.p2p.cli.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import vn.ptit.p2p.cli.CliWiring;
import vn.ptit.p2p.common.Hashing;
import vn.ptit.p2p.common.Models.FileMetadata;
import vn.ptit.p2p.common.Utils;
import vn.ptit.p2p.control.ManifestStore;
import vn.ptit.p2p.control.Messages;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Command to share a file with the network
 */
@Command(
    name = "share",
    description = "Share a file with the P2P network",
    mixinStandardHelpOptions = true
)
public class ShareCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(ShareCommand.class);
    
    @Parameters(
        index = "0",
        description = "Path to the file to share"
    )
    private File file;
    
    @Override
    public Integer call() throws Exception {
        if (!file.exists()) {
            System.err.println("Error: File not found: " + file.getAbsolutePath());
            return 1;
        }
        
        if (!file.isFile()) {
            System.err.println("Error: Not a file: " + file.getAbsolutePath());
            return 1;
        }
        
        System.out.println("Sharing file: " + file.getName());
        System.out.println("Size: " + Utils.formatBytes(file.length()));
        
        try {
            // Calculate file hash
            System.out.print("Calculating file hash... ");
            String fileHash = Hashing.hashFile(file);
            System.out.println("done");
            System.out.println("File hash: " + fileHash);
            
            // Get configuration
            CliWiring wiring = CliWiring.getInstance();
            int pieceSize = wiring.getConfig().getInt("data.piece_size", 262144);
            
            // Calculate piece information
            int pieceCount = (int) Math.ceil((double) file.length() / pieceSize);
            System.out.println("Piece size: " + Utils.formatBytes(pieceSize));
            System.out.println("Pieces: " + pieceCount);
            
            // Calculate piece hashes
            System.out.print("Calculating piece hashes... ");
            List<String> pieceHashes = calculatePieceHashes(file, pieceSize, pieceCount);
            System.out.println("done");
            
            // Create file metadata
            FileMetadata metadata = new FileMetadata(
                fileHash,
                file.getName(),
                file.length(),
                pieceSize,
                pieceCount,
                pieceHashes
            );
            
            // Add to manifest store
            ManifestStore manifestStore = wiring.getManifestStore();
            manifestStore.addManifest(fileHash, metadata);
            
            // Mark all pieces as available (we have the complete file)
            for (int i = 0; i < pieceCount; i++) {
                manifestStore.markPieceAvailable(fileHash, i);
            }
            
            System.out.println("\nFile is now being shared!");
            System.out.println("Other peers can download using hash: " + fileHash);
            
            return 0;
            
        } catch (Exception e) {
            logger.error("Error sharing file", e);
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
    
    private List<String> calculatePieceHashes(File file, int pieceSize, int pieceCount) throws IOException {
        List<String> hashes = new ArrayList<>();
        
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[pieceSize];
            
            for (int i = 0; i < pieceCount; i++) {
                int bytesRead = fis.read(buffer);
                if (bytesRead > 0) {
                    byte[] pieceData = new byte[bytesRead];
                    System.arraycopy(buffer, 0, pieceData, 0, bytesRead);
                    String hash = Hashing.hashBytes(pieceData);
                    hashes.add(hash);
                }
            }
        }
        
        return hashes;
    }
}

