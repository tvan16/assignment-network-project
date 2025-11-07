package vn.ptit.p2p.control;

import vn.ptit.p2p.common.Json;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Codec for encoding/decoding NDJSON (Newline Delimited JSON) messages over TCP
 * Mỗi message là 1 dòng JSON kết thúc bằng \n
 */
public class TcpJsonCodec {
    
    /**
     * Send a message over a socket using NDJSON format
     */
    public static void sendMessage(Socket socket, Object message) throws IOException {
        String json = Json.toJson(message);
        
        // NDJSON: mỗi message trên 1 dòng, kết thúc bằng \n
        BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)
        );
        
        writer.write(json);
        writer.write('\n');  // Newline delimiter
        writer.flush();
    }
    
    /**
     * Receive a message from a socket using NDJSON format
     */
    public static String receiveMessage(Socket socket) throws IOException {
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
        );
        
        String line = reader.readLine();
        if (line == null) {
            throw new IOException("Connection closed");
        }
        
        // Kiểm tra độ dài message (max 1MB)
        if (line.length() > 1024 * 1024) {
            throw new IOException("Message too large: " + line.length());
        }
        
        return line.trim();
    }
    
    /**
     * Receive message with timeout
     */
    public static String receiveMessageWithTimeout(Socket socket, int timeoutMs) throws IOException {
        int originalTimeout = socket.getSoTimeout();
        try {
            socket.setSoTimeout(timeoutMs);
            return receiveMessage(socket);
        } finally {
            socket.setSoTimeout(originalTimeout);
        }
    }
    
    /**
     * Decode a JSON message to a specific type
     */
    public static <T> T decodeMessage(String json, Class<T> messageType) {
        return Json.fromJson(json, messageType);
    }
    
    /**
     * Send and wait for response
     */
    public static String sendAndReceive(Socket socket, Object message) throws IOException {
        sendMessage(socket, message);
        return receiveMessage(socket);
    }
    
    /**
     * Send and wait for response with timeout
     */
    public static String sendAndReceive(Socket socket, Object message, int timeoutMs) throws IOException {
        sendMessage(socket, message);
        return receiveMessageWithTimeout(socket, timeoutMs);
    }
}

