package vn.ptit.p2p.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.ptit.p2p.common.Config;
import vn.ptit.p2p.common.Utils;
import vn.ptit.p2p.control.*;
import vn.ptit.p2p.data.DataService;
import vn.ptit.p2p.discovery.DiscoveryService;
import vn.ptit.p2p.discovery.PeerRegistry;

import java.io.File;

/**
 * Dependency injection and wiring for CLI application
 */
public class CliWiring {
    private static final Logger logger = LoggerFactory.getLogger(CliWiring.class);
    private static CliWiring instance;
    
    private Config config;
    private String peerId;
    private PeerRegistry peerRegistry;
    private DiscoveryService discoveryService;
    private DataService dataService;
    private ManifestStore manifestStore;
    private PieceScheduler pieceScheduler;
    private ResumeManager resumeManager;
    private Controller controller;
    
    private CliWiring() {}
    
    public static synchronized CliWiring getInstance() {
        if (instance == null) {
            instance = new CliWiring();
        }
        return instance;
    }
    
    /**
     * Initialize all components
     */
    public void initialize() throws Exception {
        logger.info("Initializing P2P application");
        
        // Load configuration
        String configPath = System.getProperty("config.path", "resources/application.yml");
        File configFile = new File(configPath);
        
        if (!configFile.exists()) {
            logger.warn("Config file not found, using defaults");
            config = createDefaultConfig();
        } else {
            config = new Config(configPath);
        }
        
        // Generate peer ID
        peerId = config.getString("peer.id", Utils.generatePeerId());
        logger.info("Peer ID: {}", peerId);
        
        // Initialize components
        peerRegistry = new PeerRegistry();
        
        // Discovery service
        if (config.getBoolean("discovery.enabled", true)) {
            discoveryService = new DiscoveryService(
                peerRegistry,
                peerId,
                config.getInt("discovery.port", 9090),
                config.getString("discovery.multicast_address", "239.255.0.1"),
                config.getInt("discovery.announce_interval", 30),
                config.getInt("discovery.peer_timeout", 90)
            );
            discoveryService.start();
        }
        
        // Data service
        dataService = new DataService();
        dataService.startListening(config.getInt("data.udp_port", 7070));
        
        // Control components
        manifestStore = new ManifestStore();
        pieceScheduler = new PieceScheduler(manifestStore);
        resumeManager = new ResumeManager(config.getString("storage.temp_dir", "./temp"));
        
        // Controller
        controller = new Controller(peerId, manifestStore, pieceScheduler, resumeManager);
        ControlConfig controlConfig = ControlConfig.builder()
            .port(config.getInt("control.port", 8081))
            .maxMessageSize(config.getInt("control.max_message_size", 65536))
            .keepaliveInterval(config.getInt("control.keepalive_interval", 15))
            .requestTimeout(config.getInt("control.request_timeout", 10))
            .build();
        controller.initialize(controlConfig);
        
        // Ensure directories exist
        Utils.ensureDirectoryExists(config.getString("storage.download_dir", "./downloads"));
        Utils.ensureDirectoryExists(config.getString("storage.shared_dir", "./shared"));
        Utils.ensureDirectoryExists(config.getString("storage.temp_dir", "./temp"));
        
        logger.info("Initialization complete");
    }
    
    /**
     * Shutdown all components
     */
    public void shutdown() {
        logger.info("Shutting down P2P application");
        
        if (controller != null) {
            controller.shutdown();
        }
        
        if (dataService != null) {
            dataService.stopListening();
        }
        
        if (discoveryService != null) {
            discoveryService.stop();
        }
        
        logger.info("Shutdown complete");
    }
    
    private Config createDefaultConfig() {
        // In a real implementation, this would create a proper default config
        // For now, return a mock that uses defaults
        return new Config() {
            @Override
            public <T> T get(String path, T defaultValue) {
                return defaultValue;
            }
        };
    }
    
    // Getters for components
    public Config getConfig() { return config; }
    public String getPeerId() { return peerId; }
    public PeerRegistry getPeerRegistry() { return peerRegistry; }
    public DataService getDataService() { return dataService; }
    public ManifestStore getManifestStore() { return manifestStore; }
    public PieceScheduler getPieceScheduler() { return pieceScheduler; }
    public ResumeManager getResumeManager() { return resumeManager; }
    public Controller getController() { return controller; }
}

