package vn.ptit.p2p.control;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.*;

/**
 * Server for handling incoming control connections with back-pressure & keepalive
 */
public class ControlServer {
    private static final Logger logger = LoggerFactory.getLogger(ControlServer.class);
    private static final int SO_TIMEOUT_MS = 30000;  // 30s read timeout
    private static final int BACKLOG = 50;
    private static final int MAX_QUEUE_SIZE = 1000;  // Back-pressure queue limit
    
    private final ControlConfig config;
    private final Controller controller;
    private ServerSocket serverSocket;
    private volatile boolean running;
    private ExecutorService executor;
    private BlockingQueue<Runnable> taskQueue;
    
    public ControlServer(ControlConfig config, Controller controller) {
        this.config = config;
        this.controller = controller;
        
        // Back-pressure: sử dụng bounded queue
        this.taskQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
        this.executor = new ThreadPoolExecutor(
            10,  // core threads
            50,  // max threads
            60L, TimeUnit.SECONDS,
            taskQueue,
            new ThreadPoolExecutor.CallerRunsPolicy()  // Back-pressure: caller runs khi queue đầy
        );
    }
    
    /**
     * Start the control server
     */
    public void start() throws IOException {
        serverSocket = new ServerSocket(config.getPort(), BACKLOG);
        running = true;
        
        Thread acceptThread = new Thread(this::acceptLoop);
        acceptThread.setDaemon(true);
        acceptThread.start();
        
        logger.info("Control server started on port {} (SO_TIMEOUT={}ms, backlog={})", 
                   config.getPort(), SO_TIMEOUT_MS, BACKLOG);
    }
    
    /**
     * Stop the control server
     */
    public void stop() {
        running = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                logger.error("Error closing server socket", e);
            }
        }
        executor.shutdown();
        logger.info("Control server stopped");
    }
    
    /**
     * Accept incoming connections
     */
    private void acceptLoop() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                
                // Cấu hình TCP socket
                configureSocket(clientSocket);
                
                logger.debug("Accepted connection from {}", clientSocket.getRemoteSocketAddress());
                
                // Submit vào executor với back-pressure
                try {
                    executor.submit(() -> handleClient(clientSocket));
                } catch (RejectedExecutionException e) {
                    logger.warn("Connection rejected due to overload from {}", 
                               clientSocket.getRemoteSocketAddress());
                    clientSocket.close();
                }
            } catch (IOException e) {
                if (running) {
                    logger.error("Error accepting connection", e);
                }
            }
        }
    }
    
    /**
     * Cấu hình socket với SO_TIMEOUT và TCP keepalive
     */
    private void configureSocket(Socket socket) throws SocketException {
        // SO_TIMEOUT: timeout cho read operations
        socket.setSoTimeout(SO_TIMEOUT_MS);
        
        // TCP Keepalive: phát hiện connection chết
        socket.setKeepAlive(true);
        
        // TCP_NODELAY: tắt Nagle algorithm cho low latency
        socket.setTcpNoDelay(true);
        
        // SO_LINGER: đóng connection gracefully
        socket.setSoLinger(true, 5);
    }
    
    /**
     * Handle a client connection
     */
    private void handleClient(Socket socket) {
        String remoteAddr = socket.getRemoteSocketAddress().toString();
        logger.info("Handling client connection: {}", remoteAddr);
        
        try {
            while (!socket.isClosed() && running) {
                try {
                    String messageJson = TcpJsonCodec.receiveMessage(socket);
                    
                    // Dispatch to controller
                    String response = controller.handleMessage(messageJson, socket);
                    
                    if (response != null) {
                        TcpJsonCodec.sendMessage(socket, response);
                    }
                } catch (java.net.SocketTimeoutException e) {
                    // Timeout - kiểm tra keepalive
                    logger.debug("Socket timeout for {}, sending keepalive check", remoteAddr);
                    // Client sẽ tự động gửi PING nếu cần
                    continue;
                }
            }
        } catch (IOException e) {
            logger.debug("Client connection closed: {} - {}", remoteAddr, e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                logger.debug("Error closing socket", e);
            }
        }
    }
    
    /**
     * Get current queue size (for monitoring)
     */
    public int getQueueSize() {
        return taskQueue.size();
    }
    
    /**
     * Check if server is overloaded
     */
    public boolean isOverloaded() {
        return taskQueue.size() > MAX_QUEUE_SIZE * 0.8;  // 80% threshold
    }
}

