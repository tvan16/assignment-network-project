package vn.ptit.p2p.control;

/**
 * Configuration for control protocol
 */
public class ControlConfig {
    private final int port;
    private final int maxMessageSize;
    private final int keepaliveInterval;
    private final int requestTimeout;
    
    public ControlConfig(int port, int maxMessageSize, int keepaliveInterval, int requestTimeout) {
        this.port = port;
        this.maxMessageSize = maxMessageSize;
        this.keepaliveInterval = keepaliveInterval;
        this.requestTimeout = requestTimeout;
    }
    
    public int getPort() { return port; }
    public int getMaxMessageSize() { return maxMessageSize; }
    public int getKeepaliveInterval() { return keepaliveInterval; }
    public int getRequestTimeout() { return requestTimeout; }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private int port = 7000;  // TCP Control port theo yêu cầu
        private int maxMessageSize = 65536;
        private int keepaliveInterval = 15;
        private int requestTimeout = 10;
        
        public Builder port(int port) {
            this.port = port;
            return this;
        }
        
        public Builder maxMessageSize(int size) {
            this.maxMessageSize = size;
            return this;
        }
        
        public Builder keepaliveInterval(int interval) {
            this.keepaliveInterval = interval;
            return this;
        }
        
        public Builder requestTimeout(int timeout) {
            this.requestTimeout = timeout;
            return this;
        }
        
        public ControlConfig build() {
            return new ControlConfig(port, maxMessageSize, keepaliveInterval, requestTimeout);
        }
    }
}

