package vn.ptit.p2p.cli.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import vn.ptit.p2p.cli.CliWiring;
import vn.ptit.p2p.common.Models.Peer;
import vn.ptit.p2p.common.Utils;
import vn.ptit.p2p.dataapi.DataApi;
import vn.ptit.p2p.discovery.PeerRegistry;

import java.util.Collection;
import java.util.concurrent.Callable;

/**
 * Command to display statistics and peer information
 */
@Command(
    name = "stat",
    description = "Display statistics and peer information",
    mixinStandardHelpOptions = true
)
public class StatCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(StatCommand.class);
    
    @Override
    public Integer call() throws Exception {
        try {
            CliWiring wiring = CliWiring.getInstance();
            
            System.out.println("=== P2P Statistics ===\n");
            
            // Peer information
            System.out.println("Peer ID: " + wiring.getPeerId());
            
            // Connected peers
            PeerRegistry peerRegistry = wiring.getPeerRegistry();
            Collection<Peer> peers = peerRegistry.getAllPeers();
            System.out.println("\nConnected Peers: " + peers.size());
            
            if (!peers.isEmpty()) {
                System.out.println("\nPeer List:");
                for (Peer peer : peers) {
                    System.out.printf("  - %s (%s:%d)%n", 
                        peer.getId(), peer.getHost(), peer.getPort());
                }
            }
            
            // Shared files
            int sharedFiles = wiring.getManifestStore().getAllFileHashes().size();
            System.out.println("\nShared Files: " + sharedFiles);
            
            // Data transfer statistics
            DataApi.DataTransferStats stats = wiring.getDataService().getStats();
            System.out.println("\n=== Transfer Statistics ===");
            System.out.println("Uploaded: " + Utils.formatBytes(stats.getBytesSent()));
            System.out.println("Downloaded: " + Utils.formatBytes(stats.getBytesReceived()));
            System.out.println("Active Uploads: " + stats.getActiveSends());
            System.out.println("Active Downloads: " + stats.getActiveReceives());
            System.out.println("Total Uploads: " + stats.getTotalSends());
            System.out.println("Total Downloads: " + stats.getTotalReceives());
            System.out.println("Failed Uploads: " + stats.getFailedSends());
            System.out.println("Failed Downloads: " + stats.getFailedReceives());
            
            // Resumable downloads
            int resumableCount = wiring.getResumeManager().getResumableDownloads().size();
            if (resumableCount > 0) {
                System.out.println("\nResumable Downloads: " + resumableCount);
            }
            
            return 0;
            
        } catch (Exception e) {
            logger.error("Error displaying statistics", e);
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}

