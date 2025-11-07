package vn.ptit.p2p.common;

import org.yaml.snakeyaml.Yaml;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Configuration management for P2P application
 */
public class Config {
    private Map<String, Object> config;
    
    public Config(String configPath) throws IOException {
        try (InputStream input = new FileInputStream(configPath)) {
            Yaml yaml = new Yaml();
            config = yaml.load(input);
        }
    }
    
    @SuppressWarnings("unchecked")
    public <T> T get(String path, T defaultValue) {
        String[] keys = path.split("\\.");
        Object current = config;
        
        for (String key : keys) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(key);
                if (current == null) {
                    return defaultValue;
                }
            } else {
                return defaultValue;
            }
        }
        
        try {
            return (T) current;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }
    
    public String getString(String path, String defaultValue) {
        return get(path, defaultValue);
    }
    
    public Integer getInt(String path, Integer defaultValue) {
        return get(path, defaultValue);
    }
    
    public Boolean getBoolean(String path, Boolean defaultValue) {
        return get(path, defaultValue);
    }
    
    public Long getLong(String path, Long defaultValue) {
        return get(path, defaultValue);
    }
}

