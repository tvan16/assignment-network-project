# 📚 P2P File Sharing System - Detailed Instructions

> Comprehensive guide for developers who want to understand, run, and contribute to the project.

---

## 📑 Table of Contents

1. [Introduction](#1-introduction)
2. [System Architecture](#2-system-architecture)
3. [Setup & Installation](#3-setup--installation)
4. [Module Details](#4-module-details)
5. [Protocol Specification](#5-protocol-specification)
6. [API Reference](#6-api-reference)
7. [Development Guide](#7-development-guide)
8. [Testing Strategy](#8-testing-strategy)
9. [Performance Tuning](#9-performance-tuning)
10. [Troubleshooting](#10-troubleshooting)
11. [FAQ](#11-faq)

---

## 1. Introduction

### 1.1 Project Overview

The P2P File Sharing System is a peer-to-peer file sharing system built entirely in Java using pure socket programming (without any existing P2P frameworks). The system is designed with a modular architecture consisting of 3 main components:

- **Member A (Discovery)**: Detect peers in the LAN network
- **Member B (Control)**: Coordinate and manage file transfers
- **Member C (Data)**: Perform high-speed data transmission

### 1.2 Key Features

#### Performance
- **Throughput**: ~80 MB/s on 1GbE LAN with UDP
- **Discovery Time**: ≤ 3 seconds in LAN
- **Resume**: Instant resume with 0% overhead
- **Concurrent**: Download from multiple peers simultaneously

#### Reliability
- **Loss Tolerance**: Works well with packet loss up to 30%
- **Auto Recovery**: Automatic retransmit or TCP fallback
- **Integrity**: SHA-256 verification for each piece and entire file
- **Checkpoint**: Automatic save/restore state

#### Scalability
- **Multi-peer**: Support multiple peers (tested up to 10)
- **Large Files**: Handle files up to several GB
- **Rate Control**: Token bucket per-peer and global

---

## 2. System Architecture

### 2.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    APPLICATION LAYER                             │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              CLI Interface (Picocli)                      │  │
│  │  Commands: share, get, stat                              │  │
│  └────────────┬─────────────────────────────────────────────┘  │
│               │                                                  │
├───────────────┼──────────────────────────────────────────────────┤
│               │            CONTROL LAYER                          │
│  ┌────────────▼───────────────────────────────────────────┐    │
│  │                    Controller                           │    │
│  │  - Message handling (OFFER, REQUEST, HAVE, NACK)      │    │
│  │  - Piece scheduling (Sequential/Rarest-first)          │    │
│  │  - Manifest management                                 │    │
│  │  - Resume management                                   │    │
│  └──┬─────────────────────────────────────────────────┬───┘    │
│     │                                                   │        │
├─────┼───────────────────────────────────────────────────┼────────┤
│     │            NETWORK LAYER                          │        │
│  ┌──▼──────────────────┐              ┌────────────────▼─────┐ │
│  │  Discovery Service  │              │    Data Service      │ │
│  │  (UDP Multicast)    │              │  (UDP/TCP Transfer)  │ │
│  │  - HELLO messages   │              │  - Sliding Window    │ │
│  │  - Peer registry    │              │  - NACK/SACK         │ │
│  │  - TCP handshake    │              │  - TCP Fallback      │ │
│  └─────────────────────┘              └──────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 Module Dependencies

```
cli
 ├─ control
 │   ├─ discovery
 │   │   └─ common
 │   ├─ data-api
 │   │   └─ common
 │   └─ common
 └─ data
     ├─ data-api
     │   └─ common
     └─ common
```

### 2.3 Thread Model

```
Main Thread
 │
 ├─ Discovery Thread
 │   ├─ Announce Thread (scheduled, 2s interval)
 │   ├─ Listen Thread (blocking UDP receive)
 │   └─ Cleanup Thread (scheduled, timeout check)
 │
 ├─ Control Server Thread Pool (10-50 threads)
 │   ├─ Accept Thread (blocking TCP accept)
 │   └─ Client Handler Threads (one per connection)
 │
 └─ Data Transfer Thread Pool
     ├─ UDP Sender Threads
     ├─ UDP Receiver Thread
     └─ TCP Fallback Threads
```

---

## 3. Setup & Installation

### 3.1 Prerequisites

#### System Requirements
- **OS**: Windows 10/11, Linux, macOS
- **RAM**: ≥ 2 GB
- **Disk**: ≥ 500 MB free space
- **Network**: LAN connection (WiFi or Ethernet)

#### Software Requirements
- **Java JDK**: 11 or higher ([Download](https://adoptium.net/))
- **Gradle**: 7.0+ (included via wrapper)
- **Git**: For cloning repository

#### Optional
- **IntelliJ IDEA**: 2023.2+ (recommended IDE)
- **Wireshark**: For network debugging
- **VisualVM**: For profiling

### 3.2 Installation Steps

#### Step 1: Clone Repository
```bash
git clone https://github.com/tvan16/assignment-network-project.git
cd assignment-network-project
```

#### Step 2: Verify Java Installation
```bash
java -version
# Expected output: java version "11.x.x" or higher

javac -version
# Expected output: javac 11.x.x or higher
```

#### Step 3: Build Project
```bash
cd source

# On Windows
gradlew.bat build

# On Linux/macOS
./gradlew build
```

Expected output:
```
BUILD SUCCESSFUL in 30s
12 actionable tasks: 12 executed
```

#### Step 4: Create Configuration
```bash
# Copy example config
cp resources/application.example.yml resources/application.yml

# Edit with your settings
# Windows: notepad resources/application.yml
# Linux: nano resources/application.yml
```

#### Step 5: Test Installation
```bash
# Run stat command
gradlew :cli:run --args="stat"
```

Expected output:
```
=== P2P Statistics ===

Peer ID: peer-xxx
Connected Peers: 0
Shared Files: 0
```

### 3.3 IntelliJ Setup

**Quick steps:**
1. `File` → `Open` → Select `source/` directory
2. Wait for Gradle import (~2 minutes)
3. `Run` → `Edit Configurations` → Add Application
4. Main class: `vn.ptit.p2p.cli.Main`
5. Run!

---

## 4. Module Details

### 4.1 Common Module

**Purpose**: Shared utilities and models for all modules

**Key Classes:**

#### Config.java
```java
// Load YAML configuration
Config config = new Config("application.yml");
int port = config.getInt("control.port", 7000);
String peerId = config.getString("peer.id", "default");
```

#### Hashing.java
```java
// Calculate SHA-256 hash
String fileHash = Hashing.hashFile(new File("test.txt"));
String pieceHash = Hashing.hashBytes(pieceData);

// Verify hash
boolean valid = Hashing.verify(data, expectedHash);
```

#### Models.java
```java
// Peer model
Peer peer = new Peer("peer-001", "PeerName", "192.168.1.100", 7000);
peer.setScore(95);  // Trust score 0-100

// File metadata
FileMetadata metadata = new FileMetadata(
    fileHash, fileName, fileSize, pieceSize, pieceCount, pieceHashes
);

// Piece model
Piece piece = new Piece(fileHash, pieceIndex, data, hash);
boolean verified = piece.verify();  // Check SHA-256
```

#### Utils.java
```java
// Format bytes
String readable = Utils.formatBytes(10485760);  // "10.0 MB"

// Format duration
String duration = Utils.formatDuration(125000);  // "2m 5s"

// Transfer rate
double rate = Utils.calculateTransferRate(bytes, millis);
String formatted = Utils.formatTransferRate(rate);  // "50.3 MB/s"

// Generate peer ID
String peerId = Utils.generatePeerId();  // UUID

// Get local IP
String ip = Utils.getLocalHostAddress();
```

---

### 4.2 Discovery Module

**Purpose**: Discover peers in LAN and initiate TCP connections

#### 4.2.1 DiscoveryService.java

**Configuration:**
```yaml
discovery:
  enabled: true
  port: 9090
  multicast_address: "239.255.0.1"
  announce_interval: 2      # seconds
  peer_timeout: 90          # seconds
```

**Message Format (HELLO):**
```json
{
  "type": "HELLO",
  "peer_id": "peer-001",
  "tcp_port": 7000,
  "timestamp": 1699366800000,
  "ttl": 1
}
```

**Usage:**
```java
DiscoveryService discovery = new DiscoveryService(
    peerRegistry,
    "peer-001",    // my peer ID
    9090,          // UDP port
    "239.255.0.1", // multicast address
    2,             // announce interval (seconds)
    90             // peer timeout (seconds)
);

discovery.start();

// Later...
discovery.stop();
```

**Anti-loop Logic:**
```java
private void handleMessage(String message, InetAddress address) {
    Map<String, Object> data = Json.fromJson(message, Map.class);
    String remotePeerId = (String) data.get("peer_id");
    
    // Don't add ourselves
    if (peerId.equals(remotePeerId)) {
        return;  // Ignore
    }
    
    // Debouncing: ignore if seen <500ms ago
    Long lastSeen = lastSeenDebounce.get(remotePeerId);
    if (lastSeen != null && 
        System.currentTimeMillis() - lastSeen < 500) {
        return;  // Too soon
    }
    
    // Process...
}
```

#### 4.2.2 PeerRegistry.java

**Purpose**: Maintain list of active peers

**API:**
```java
PeerRegistry registry = new PeerRegistry();

// Add peer
Peer peer = new Peer("peer-002", "Peer2", "192.168.1.100", 7000);
registry.addPeer(peer);

// Get peer
Peer found = registry.getPeer("peer-002");

// List all
Collection<Peer> all = registry.getAllPeers();

// Remove
registry.removePeer("peer-002");

// Listen to events
registry.addListener(new PeerRegistryListener() {
    @Override
    public void onPeerAdded(Peer peer) {
        System.out.println("New peer: " + peer.getId());
    }
    
    @Override
    public void onPeerRemoved(Peer peer) {
        System.out.println("Peer left: " + peer.getId());
    }
});
```

---

### 4.3 Control Module

**Purpose**: Orchestrate the entire file transfer flow

#### 4.3.1 Protocol Messages

**1. OFFER_FILE**
```json
{
  "message_type": "offer_file",
  "peer_id": "peer-001",
  "file_hash": "abc123...",
  "file_name": "document.pdf",
  "file_size": 10485760,
  "piece_size": 262144,
  "piece_count": 40,
  "piece_hashes": ["hash1", "hash2", ...],
  "timestamp": 1699366800000
}
```

**2. REQUEST_PIECES**
```json
{
  "message_type": "request_pieces",
  "peer_id": "peer-002",
  "file_hash": "abc123...",
  "pieces": [0, 2, 5, 7, 10],
  "timestamp": 1699366800000
}
```

**3. HAVE**
```json
{
  "message_type": "have",
  "peer_id": "peer-002",
  "file_hash": "abc123...",
  "pieces": [0, 1, 2],
  "timestamp": 1699366800000
}
```

**4. NACK**
```json
{
  "message_type": "nack",
  "peer_id": "peer-001",
  "request_type": "request_pieces",
  "file_hash": "abc123...",
  "reason": "busy",
  "message": "Too many active transfers",
  "timestamp": 1699366800000
}
```

**5. PING/PONG**
```json
{
  "message_type": "ping",
  "peer_id": "peer-001",
  "sequence": 42,
  "timestamp": 1699366800000
}
```

#### 4.3.2 Controller Flow

**Sharing a File:**
```java
// 1. Calculate hashes
String fileHash = Hashing.hashFile(file);
List<String> pieceHashes = calculatePieceHashes(file);

// 2. Create manifest
FileMetadata metadata = new FileMetadata(...);
manifestStore.addManifest(fileHash, metadata);

// 3. Mark all pieces available
for (int i = 0; i < pieceCount; i++) {
    manifestStore.markPieceAvailable(fileHash, i);
}

// 4. Announce to peers
Messages.OfferFile offer = new Messages.OfferFile();
offer.setFileHash(fileHash);
// ... set other fields

for (Peer peer : peerRegistry.getAllPeers()) {
    controlClient.sendOfferFile(peer, offer);
}
```

**Downloading a File:**
```java
// 1. Check manifest exists
if (!manifestStore.hasManifest(fileHash)) {
    throw new FileNotFoundException();
}

// 2. Check resume state
if (resumeManager.hasResumeState(fileHash)) {
    Map<String, Object> state = resumeManager.loadDownloadState(fileHash);
    BitSet downloaded = (BitSet) state.get("downloadedPieces");
    // Restore state...
}

// 3. Schedule pieces
List<Integer> pieces = pieceScheduler.getNextPieces(fileHash, 10);

// 4. Request from peers
Messages.RequestPieces request = new Messages.RequestPieces();
request.setFileHash(fileHash);
request.setPieces(pieces);

for (Peer peer : manifestStore.getPeersForFile(fileHash)) {
    controlClient.sendRequestPieces(peer, request);
}

// 5. Wait for pieces via Data Plane...

// 6. On piece received
controller.onPieceDone(fileHash, pieceId);

// 7. Verify and save
if (manifestStore.isFileComplete(fileHash)) {
    boolean verified = manifestStore.verifySha256(fileHash, data);
    if (verified) {
        saveToFile(data);
        resumeManager.deleteResumeState(fileHash);
    }
}
```

#### 4.3.3 Piece Scheduling Strategies

**Sequential Mode** (for streaming):
```java
scheduler.setScheduleMode(fileHash, ScheduleMode.SEQUENTIAL);
List<Integer> pieces = scheduler.getNextPieces(fileHash, 10);
// Result: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
```

**Rarest-First Mode** (for swarm):
```java
scheduler.setScheduleMode(fileHash, ScheduleMode.RAREST_FIRST);
List<Integer> pieces = scheduler.getNextPieces(fileHash, 10);
// Result: [42, 7, 88, 15, ...] (ordered by rarity)
```

---

### 4.4 Data Module

**Purpose**: High-performance data transfer with UDP/TCP

#### 4.4.1 UDP Packet Format

```
┌─────────────────────────────────────────────────┐
│              UDP Packet Structure                │
├─────────────────────────────────────────────────┤
│ Header (20 bytes)                               │
│  ├─ fileIdCrc    (4 bytes) - CRC32 of fileHash │
│  ├─ pieceId      (4 bytes) - Piece index       │
│  ├─ seq          (4 bytes) - Sequence number   │
│  ├─ total        (4 bytes) - Total sequences   │
│  └─ crc32        (4 bytes) - Payload CRC32     │
├─────────────────────────────────────────────────┤
│ Payload (≤1400 bytes)                           │
│  └─ Actual piece data                           │
└─────────────────────────────────────────────────┘
Total: ≤1420 bytes (fits in MTU 1500)
```

#### 4.4.2 Sliding Window Implementation

**Sender Side:**
```java
public class SlidingWindowSender {
    private static final int WINDOW_SIZE = 64;
    private static final int PACING_DELAY_US = 100;
    
    private int base = 0;         // First unacked seq
    private int nextSeqNum = 0;   // Next seq to send
    
    public void sendPieceWithWindow(Piece piece) {
        List<byte[]> sequences = splitIntoSequences(piece);
        
        while (base < sequences.size()) {
            // Send packets in window
            while (nextSeqNum < base + WINDOW_SIZE && 
                   nextSeqNum < sequences.size()) {
                
                sendPacket(sequences.get(nextSeqNum));
                nextSeqNum++;
                
                // Pacing delay
                LockSupport.parkNanos(PACING_DELAY_US * 1000);
            }
            
            // Wait for ACK or timeout
            waitForAck();
        }
    }
}
```

**Receiver Side:**
```java
public class SlidingWindowReceiver {
    private Map<Integer, BitSet> receivedSeqs;  // pieceId -> BitSet
    
    public void receivePacket(UdpPacket packet) {
        // Verify CRC32
        if (!verifyCrc32(packet)) {
            sendNack(packet.pieceId, packet.seq);
            return;
        }
        
        // Store sequence
        receivedSeqs.get(packet.pieceId).set(packet.seq);
        
        // Check if piece complete
        if (isPieceComplete(packet.pieceId, packet.total)) {
            byte[] pieceData = assemblePiece(packet.pieceId);
            
            // Verify piece SHA-256
            if (verifyPieceHash(pieceData)) {
                notifyController(packet.pieceId, pieceData);
            }
        } else {
            // Send SACK
            sendSack(packet.pieceId, receivedSeqs.get(packet.pieceId));
        }
    }
}
```

#### 4.4.3 Rate Control (Token Bucket)

```java
public class RateController {
    private final long maxBytesPerSecond;
    private final long bucketSize;
    private long tokens;
    private long lastRefill;
    
    public RateController(long maxBytesPerSecond) {
        this.maxBytesPerSecond = maxBytesPerSecond;
        this.bucketSize = maxBytesPerSecond * 2;  // 2 seconds worth
        this.tokens = bucketSize;
        this.lastRefill = System.nanoTime();
    }
    
    public void consume(int bytes) {
        while (!tryConsume(bytes)) {
            Thread.sleep(1);  // Wait for refill
        }
    }
    
    private synchronized boolean tryConsume(int bytes) {
        refillTokens();
        
        if (tokens >= bytes) {
            tokens -= bytes;
            return true;
        }
        return false;
    }
    
    private void refillTokens() {
        long now = System.nanoTime();
        long elapsed = now - lastRefill;
        long tokensToAdd = (elapsed * maxBytesPerSecond) / 1_000_000_000L;
        
        if (tokensToAdd > 0) {
            tokens = Math.min(bucketSize, tokens + tokensToAdd);
            lastRefill = now;
        }
    }
}
```

**Usage:**
```java
// Global rate limit: 10 MB/s
RateController global = new RateController(10 * 1024 * 1024);

// Per-peer rate limit: 1 MB/s
Map<String, RateController> perPeer = new HashMap<>();
perPeer.put(peerId, new RateController(1 * 1024 * 1024));

// Before sending packet
global.consume(packet.length);
perPeer.get(peerId).consume(packet.length);

// Now send packet
socket.send(packet);
```

---

## 5. Protocol Specification

### 5.1 NDJSON Format

**Definition**: Newline Delimited JSON - Each message is a JSON object ending with `\n`

**Example:**
```
{"message_type":"offer_file","peer_id":"A",...}\n
{"message_type":"request_pieces","pieces":[1,2,3]}\n
{"message_type":"have","pieces":[1,2,3]}\n
```

**Advantages:**
- ✅ Simple parsing (read until `\n`)
- ✅ Streaming friendly
- ✅ Self-delimited (no length prefix needed)
- ✅ Human readable (easy debugging)

**Implementation:**
```java
// Send
BufferedWriter writer = new BufferedWriter(
    new OutputStreamWriter(socket.getOutputStream(), UTF_8)
);
writer.write(Json.toJson(message));
writer.write('\n');  // Important!
writer.flush();

// Receive
BufferedReader reader = new BufferedReader(
    new InputStreamReader(socket.getInputStream(), UTF_8)
);
String line = reader.readLine();  // Blocks until \n
Message msg = Json.fromJson(line, Message.class);
```

### 5.2 Message Sequence Diagrams

**File Sharing Flow:**
```
Peer A                    Peer B
  │                          │
  │  1. OFFER_FILE           │
  │─────────────────────────>│
  │                          │
  │  2. REQUEST_PIECES       │
  │<─────────────────────────│
  │   [0, 1, 2, 3, 4]        │
  │                          │
  │  3. UDP Data (piece 0)   │
  │─────────────────────────>│
  │     (sliding window)     │
  │─────────────────────────>│
  │                          │
  │  4. HAVE [0]             │
  │<─────────────────────────│
  │                          │
  │  5. UDP Data (piece 1-4) │
  │─────────────────────────>│
  │                          │
  │  6. HAVE [0,1,2,3,4]     │
  │<─────────────────────────│
  │                          │
```

**Loss & Retransmit:**
```
Sender                    Receiver
  │                          │
  │  seq 0                   │
  │─────────────────────────>│ ✓
  │  seq 1                   │
  │─────────────────────────>│ ✓
  │  seq 2                   │
  │─────────X (lost)         │
  │  seq 3                   │
  │─────────────────────────>│ ✓
  │                          │
  │  NACK [2]                │
  │<─────────────────────────│
  │                          │
  │  seq 2 (retransmit)      │
  │─────────────────────────>│ ✓
  │                          │
  │  SACK [0,1,2,3]          │
  │<─────────────────────────│
  │                          │
```

### 5.3 State Machine

**Peer State:**
```
┌─────────────┐
│  INIT       │
└──────┬──────┘
       │ start()
       ▼
┌─────────────┐
│ DISCOVERING │◄─────┐
└──────┬──────┘      │
       │ peer found  │ peer lost
       ▼             │
┌─────────────┐      │
│  CONNECTED  │──────┘
└──────┬──────┘
       │ share/get
       ▼
┌─────────────┐
│TRANSFERRING │
└──────┬──────┘
       │ complete
       ▼
┌─────────────┐
│    IDLE     │
└─────────────┘
```

**Download State:**
```
┌─────────────┐
│   PENDING   │
└──────┬──────┘
       │ start
       ▼
┌─────────────┐
│ DOWNLOADING │◄─────┐
└──────┬──────┘      │
       │             │ retry
       ▼             │
┌─────────────┐      │
│  VERIFYING  │──────┘ fail
└──────┬──────┘
       │ success
       ▼
┌─────────────┐
│  COMPLETED  │
└─────────────┘
```

---

## 6. API Reference

### 6.1 Controller APIs

#### For Data Plane (Member C)

```java
/**
 * Send piece via Data Plane
 */
public void sendPiece(String fileHash, int pieceId, Peer peer);

/**
 * Request retransmit sequences
 */
public void retransmitSeq(String fileHash, int pieceId, List<Integer> seqList);

/**
 * Cancel transfer
 */
public void cancelTransfer(String fileHash);

/**
 * Fallback to TCP
 */
public void fallbackTcp(String fileHash, int pieceId, Peer peer);
```

#### Callbacks from Data Plane

```java
/**
 * Piece completed
 */
public void onPieceDone(String fileHash, int pieceId);

/**
 * High loss rate
 */
public void onLossAlert(String fileHash, int pieceId, double lossRate);

/**
 * CRC error
 */
public void onPieceCrcError(String fileHash, int pieceId);
```

### 6.2 ManifestStore APIs

```java
// Add manifest
void addManifest(String fileHash, FileMetadata metadata);

// Get manifest
FileMetadata getManifest(String fileHash);

// Mark piece available
void markPieceAvailable(String fileHash, int pieceIndex);

// Get missing pieces
List<Integer> getMissingPieces(String fileHash);

// Check complete
boolean isFileComplete(String fileHash);

// Verify SHA-256
boolean verifySha256(String fileHash, byte[] data);
```

### 6.3 PieceScheduler APIs

```java
// Set mode
void setScheduleMode(String fileHash, ScheduleMode mode);

// Get next pieces
List<Integer> getNextPieces(String fileHash, int count);

// Update peer pieces
void updatePeerPieces(String fileHash, String peerId, BitSet pieces);

// Mark completed/failed
void markPieceCompleted(String fileHash, int pieceIndex);
void markPieceFailed(String fileHash, int pieceIndex);
```

---

## 7. Development Guide

### 7.1 Code Style

#### Naming Conventions
- Classes: `PascalCase`
- Methods: `camelCase`
- Constants: `UPPER_SNAKE_CASE`
- Packages: `lowercase`

#### Logging
```java
// Use SLF4J
private static final Logger logger = LoggerFactory.getLogger(MyClass.class);

// Log levels
logger.trace("Very detailed");
logger.debug("Debug info");
logger.info("Important events");
logger.warn("Warning");
logger.error("Error", exception);
```

#### Error Handling
```java
// Throw specific exceptions
throw new FileNotFoundException("File not found: " + path);

// Catch and log
try {
    // risky operation
} catch (IOException e) {
    logger.error("Failed to read file", e);
    throw new RuntimeException("Failed to process", e);
}
```

### 7.2 Adding New Features

#### Example: Add new message type

**Step 1: Define message**
```java
// Messages.java
public static class MyNewMessage extends BaseMessage {
    private String myField;
    
    public MyNewMessage() {
        setMessageType("my_new_message");
    }
    
    // Getters/setters...
}
```

**Step 2: Create JSON schema**
```json
// resources/schemas/my_new_message.schema.json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "MyNewMessage",
  "type": "object",
  "properties": {
    "message_type": {
      "type": "string",
      "const": "my_new_message"
    },
    "my_field": {
      "type": "string"
    }
  },
  "required": ["message_type", "my_field"]
}
```

**Step 3: Handle in Controller**
```java
// Controller.java
public String handleMessage(String messageJson, Socket socket) {
    // ...
    switch (messageType) {
        case "my_new_message":
            return handleMyNewMessage(message);
        // ...
    }
}

private String handleMyNewMessage(Map<String, Object> message) {
    String myField = (String) message.get("my_field");
    // Process...
    return Json.toJson(Map.of("status", "ok"));
}
```

**Step 4: Write tests**
```java
@Test
public void testMyNewMessage() {
    MyNewMessage msg = new MyNewMessage();
    msg.setMyField("test");
    
    String json = Json.toJson(msg);
    String response = controller.handleMessage(json, mockSocket);
    
    assertNotNull(response);
}
```

---

## 8. Testing Strategy

### 8.1 Unit Tests

```bash
# Run all unit tests
./gradlew test

# Run specific test class
./gradlew test --tests ManifestStoreTest

# Run with coverage
./gradlew test jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

**Example Test:**
```java
@Test
public void testPieceScheduler_Sequential() {
    // Setup
    ManifestStore store = new ManifestStore();
    PieceScheduler scheduler = new PieceScheduler(store);
    
    FileMetadata metadata = createTestMetadata(10);  // 10 pieces
    store.addManifest("hash1", metadata);
    
    // Set sequential mode
    scheduler.setScheduleMode("hash1", ScheduleMode.SEQUENTIAL);
    
    // Test
    List<Integer> pieces = scheduler.getNextPieces("hash1", 5);
    
    // Assert
    assertEquals(Arrays.asList(0, 1, 2, 3, 4), pieces);
}
```

### 8.2 Integration Tests

**Test Scenario: End-to-end transfer**
```java
@Test
public void testEndToEndTransfer() throws Exception {
    // 1. Start 2 peers
    Peer peer1 = startPeer("peer-1", 7000);
    Peer peer2 = startPeer("peer-2", 7001);
    
    // 2. Wait for discovery
    Thread.sleep(3000);
    assertEquals(1, peer1.getConnectedPeers().size());
    assertEquals(1, peer2.getConnectedPeers().size());
    
    // 3. Share file on peer1
    File testFile = createTempFile(10 * 1024 * 1024);  // 10 MB
    String hash = peer1.shareFile(testFile);
    
    // 4. Download on peer2
    File downloaded = peer2.downloadFile(hash);
    
    // 5. Verify
    assertEquals(hash, Hashing.hashFile(downloaded));
    assertEquals(testFile.length(), downloaded.length());
    
    // 6. Cleanup
    peer1.stop();
    peer2.stop();
}
```

### 8.3 Performance Tests

```java
@Test
public void testThroughput() throws Exception {
    // Setup
    Peer sender = startPeer("sender", 7000);
    Peer receiver = startPeer("receiver", 7001);
    Thread.sleep(3000);  // Discovery
    
    // Create 100 MB file
    File largeFile = createTempFile(100 * 1024 * 1024);
    String hash = sender.shareFile(largeFile);
    
    // Measure
    long start = System.currentTimeMillis();
    File downloaded = receiver.downloadFile(hash);
    long elapsed = System.currentTimeMillis() - start;
    
    // Calculate throughput
    double throughputMBps = (100.0 / elapsed) * 1000;
    
    // Assert: Should be > 50 MB/s on LAN
    assertTrue(throughputMBps > 50, 
              "Throughput: " + throughputMBps + " MB/s");
}
```

---

## 9. Performance Tuning

### 9.1 UDP Window Size

```yaml
# Small files (<10 MB): smaller window
data:
  window_size: 32

# Large files (>100 MB): larger window
data:
  window_size: 128
```

### 9.2 Rate Limiting

```yaml
# High-speed LAN
data:
  max_bandwidth: 100000000  # 100 MB/s

# Limited connection
data:
  max_bandwidth: 10000000   # 10 MB/s
```

### 9.3 Thread Pool Tuning

```yaml
control:
  core_threads: 10
  max_threads: 50
  queue_size: 1000
```

### 9.4 JVM Options

```bash
# For large files
java -Xmx4g -Xms1g -jar app.jar

# For many concurrent connections
java -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -jar app.jar
```

---

## 10. Troubleshooting

### 10.1 Discovery Issues

**Problem**: Peers cannot discover each other

**Diagnosis:**
```bash
# Check multicast
netsh interface ipv4 show joins

# Check firewall
netsh advfirewall firewall show rule name=all | findstr 9090
```

**Solution:**
```yaml
# Enable broadcast fallback
discovery:
  use_broadcast_fallback: true
```

### 10.2 High Packet Loss

**Problem**: Loss rate >30%

**Diagnosis:**
- Check network quality
- Monitor with Wireshark
- Check logs for retransmit count

**Solution:**
```yaml
# Reduce window size
data:
  window_size: 32
  pacing_delay_us: 200  # Slower pacing
```

### 10.3 Memory Issues

**Problem**: OutOfMemoryError

**Diagnosis:**
```bash
# Monitor heap
jconsole
visualvm
```

**Solution:**
```bash
# Increase heap
-Xmx2g

# Limit concurrent transfers
data:
  max_concurrent_downloads: 2
```

---

## 11. FAQ

**Q: Why use NDJSON instead of length-prefixed format?**

A: NDJSON is simpler, easier to debug, and streaming-friendly. Length-prefixed requires knowing the length beforehand, which is more complex to implement.

**Q: Is UDP window size 64 appropriate?**

A: It depends on the network. Good LAN: 64-128. WiFi: 32-64. Can be tuned according to bandwidth-delay product.

**Q: Why is TCP fallback needed?**

A: UDP doesn't guarantee delivery. When loss >30%, UDP retransmit is inefficient. TCP fallback ensures reliability.

**Q: Does resume have overhead?**

A: Minimal. Only saves BitSet when piece is done. Load is instant when restarting.

**Q: Is NAT traversal supported?**

A: Not yet. Currently LAN only. Future: STUN/TURN.

**Q: Is encryption supported?**

A: Not yet. Future: TLS for TCP, DTLS for UDP.

---

## 📞 Support

If you encounter issues, check:
1. Logs in `logs/p2p.log`
2. GitHub Issues: https://github.com/tvan16/assignment-network-project/issues
3. Email: thevan@ptit.edu.vn

---

**Happy Coding! 🚀**
